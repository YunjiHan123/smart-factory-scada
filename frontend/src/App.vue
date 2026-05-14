<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { api, clearTokens, getAccessToken, saveTokens } from './api'

const appMode = ref(getAccessToken() ? 'scada' : 'login')
const activePage = ref('facility')
const loading = ref(false)
const errorMessage = ref('')
const selectedPlantId = ref(null)
const selectedFacilityId = ref(null)
const syncingSelection = ref(false)
let energySocket = null
let energySocketReconnectTimer = null
const nowLabel = computed(() =>
  new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date()),
)

const loginForm = reactive({
  email: 'admin@scada.com',
  password: 'Password123!',
})

const state = reactive({
  me: null,
  overview: null,
  plants: [],
  facilities: [],
  summaries: [],
  measurements: [],
  latestEnergy: null,
  alarms: [],
  esgScores: [],
  users: [],
})

const navItems = [
  { id: 'facility', label: '설비 조회', icon: 'F' },
  { id: 'peak', label: '피크 전력', icon: 'P' },
  { id: 'utility', label: '가스/용수', icon: 'U' },
  { id: 'esg', label: 'ESG 평가', icon: 'E' },
  { id: 'users', label: '사용자 관리', icon: 'M' },
  { id: 'alarms', label: '알람', icon: 'A' },
]

const activeMeta = computed(() => {
  const meta = {
    facility: ['설비별 에너지 현황', '사업장, 설비, 에너지 요약, 최신 수집값을 확인합니다.'],
    peak: ['피크 전력 현황', '피크 전력과 발생 알람을 확인합니다.'],
    utility: ['가스/용수 모니터링', '가스와 용수 사용량을 설비 단위로 확인합니다.'],
    esg: ['ESG 평가 지표', '사업장별 ESG 점수를 확인합니다.'],
    users: ['사용자 관리', '사용자 계정과 권한 상태를 확인합니다.'],
    alarms: ['알람 관리', '발생 알람과 처리 상태를 확인합니다.'],
  }
  const [title, description] = meta[activePage.value]
  return { title, description }
})

const selectedPlant = computed(() => state.plants.find((plant) => plant.id === selectedPlantId.value))
const selectedFacility = computed(() => state.facilities.find((facility) => facility.id === selectedFacilityId.value))
const latestSummary = computed(() => state.overview?.latestEnergySummary || state.summaries.at(-1) || null)
const latestEsg = computed(() => state.overview?.latestEsgScore || state.esgScores[0] || null)
const recentSummaries = computed(() => state.summaries.slice(-8))
const latestMeasuredAt = computed(() => metricValue(state.latestEnergy, 'measuredAt', 'measured_at'))

function metricValue(source, camelKey, snakeKey = camelKey) {
  return source?.[camelKey] ?? source?.[snakeKey]
}

const summaryChartRows = computed(() => {
  const rows = recentSummaries.value.slice(-7).map((summary) => ({
    id: summary.id,
    label: formatDateTime(summary.summaryAt).slice(5, 16),
    value: Number(metricValue(summary, 'electricityKwh', 'electricity_kwh') || 0),
    live: false,
  }))

  if (state.latestEnergy) {
    rows.push({
      id: 'live-latest',
      label: '현재',
      value: Number(metricValue(state.latestEnergy, 'electricityKwh', 'electricity_kwh') || 0),
      live: true,
      measuredAt: latestMeasuredAt.value,
    })
  }

  return rows
})

function chartHeight(row) {
  if (row.live) {
    return Math.max(12, Math.min(row.value / 12, 100))
  }
  return Math.max(12, Math.min(row.value / 1200, 100))
}

const energyCards = computed(() => {
  const source = state.latestEnergy || latestSummary.value || {}
  return [
    ['전기 사용량', metricValue(source, 'electricityKwh', 'electricity_kwh'), 'kWh', 'electric'],
    ['가스 사용량', metricValue(source, 'gasM3', 'gas_m3'), 'm3', 'gas'],
    ['용수 사용량', metricValue(source, 'waterTon', 'water_ton'), 'ton', 'water'],
    ['태양광 발전량', metricValue(source, 'solarKwh', 'solar_kwh'), 'kWh', 'solar'],
  ].map(([label, value, unit, tone]) => ({
    label,
    value: formatNumber(value),
    unit,
    tone,
  }))
})

const peakUsageRate = computed(() => {
  const peak = Number(
    metricValue(state.latestEnergy, 'peakKw', 'peak_kw') ?? metricValue(latestSummary.value, 'peakKw', 'peak_kw') ?? 0,
  )
  return peak ? Math.min(Math.round((peak / 1400) * 100), 999) : 0
})

const alarmCount = computed(() => state.overview?.occurredAlarmCount ?? state.alarms.length)

function formatNumber(value, digits = 1) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return Number(value).toLocaleString('ko-KR', {
    maximumFractionDigits: digits,
  })
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

function statusLabel(status) {
  const labels = {
    ACTIVE: '활성',
    INACTIVE: '비활성',
    LOCKED: '잠금',
    RUNNING: '운전',
    STOPPED: '정지',
    MAINTENANCE: '점검',
    OCCURRED: '발생',
    RESOLVED: '처리',
  }
  return labels[status] || status || '-'
}

async function run(task) {
  loading.value = true
  errorMessage.value = ''
  try {
    await task()
  } catch (error) {
    errorMessage.value = error.message
    if (error.message.includes('인증') || error.message.includes('Unauthorized')) {
      clearTokens()
      appMode.value = 'login'
    }
  } finally {
    loading.value = false
  }
}

async function login() {
  await run(async () => {
    const response = await api.login(loginForm)
    saveTokens(response)
    state.me = response
    appMode.value = 'scada'
    await loadInitial()
  })
}

async function logout() {
  await run(async () => {
    try {
      await api.logout()
    } finally {
      stopEnergyWebSocket()
      clearTokens()
      appMode.value = 'login'
    }
  })
}

async function loadInitial() {
  await run(async () => {
    const [me, plants, esgScores] = await Promise.all([api.me(), api.plants(), api.esgScores({})])
    state.me = me
    state.plants = plants
    state.esgScores = esgScores
    syncingSelection.value = true
    selectedPlantId.value = selectedPlantId.value || me.plantId || plants[0]?.id || null
    syncingSelection.value = false
    await loadPlantData()
  })
}

async function loadPlantData() {
  if (!selectedPlantId.value) {
    return
  }

  const [overview, facilities, alarms, users] = await Promise.all([
    api.dashboard(selectedPlantId.value),
    api.facilities(selectedPlantId.value),
    api.alarms({ plantId: selectedPlantId.value, limit: 20 }),
    api.users({ page: 0, size: 20 }),
  ])

  state.overview = overview
  state.facilities = facilities
  state.alarms = alarms
  state.users = users.items || []
  syncingSelection.value = true
  selectedFacilityId.value = facilities[0]?.id || null
  syncingSelection.value = false

  await loadEnergyData()
}

async function loadEnergyData() {
  if (!selectedPlantId.value) {
    return
  }

  const summaryParams = {
    plantId: selectedPlantId.value,
    facilityId: selectedFacilityId.value || undefined,
  }
  const measurementParams = {
    plantId: selectedPlantId.value,
    facilityId: selectedFacilityId.value || undefined,
    limit: 20,
  }

  const [summaries, measurements, latestEnergy] = await Promise.all([
    api.energySummaries(summaryParams),
    api.energyMeasurements(measurementParams),
    selectedFacilityId.value
      ? api.latestEnergy(selectedPlantId.value, selectedFacilityId.value).catch(() => null)
      : Promise.resolve(null),
  ])

  state.summaries = summaries
  state.measurements = measurements
  state.latestEnergy = latestEnergy
  startEnergyWebSocket()
}

async function loadLatestEnergy() {
  if (!selectedPlantId.value || !selectedFacilityId.value || appMode.value === 'login') {
    state.latestEnergy = null
    return
  }

  state.latestEnergy = await api.latestEnergy(selectedPlantId.value, selectedFacilityId.value).catch(() => null)
}

function energyWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/energy`
}

function startEnergyWebSocket() {
  if (appMode.value === 'login') {
    return
  }

  if (energySocket && energySocket.readyState <= WebSocket.OPEN) {
    return
  }

  window.clearTimeout(energySocketReconnectTimer)
  energySocket = new WebSocket(energyWebSocketUrl())

  energySocket.onmessage = (event) => {
    let message
    try {
      message = JSON.parse(event.data)
    } catch {
      return
    }

    const plantId = Number(metricValue(message, 'plantId', 'plant_id'))
    const facilityId = Number(metricValue(message, 'facilityId', 'facility_id'))

    if (plantId === Number(selectedPlantId.value) && facilityId === Number(selectedFacilityId.value)) {
      state.latestEnergy = message
    }
  }

  energySocket.onclose = () => {
    energySocket = null
    if (appMode.value !== 'login') {
      energySocketReconnectTimer = window.setTimeout(startEnergyWebSocket, 2000)
    }
  }

  energySocket.onerror = () => {
    energySocket?.close()
  }
}

function stopEnergyWebSocket() {
  window.clearTimeout(energySocketReconnectTimer)
  energySocketReconnectTimer = null

  if (energySocket) {
    const socket = energySocket
    energySocket = null
    socket.onclose = null
    socket.close()
  }
}

async function refreshData() {
  await run(loadPlantData)
}

async function resolveAlarm(alarmId) {
  await run(async () => {
    await api.resolveAlarm(alarmId)
    state.alarms = await api.alarms({ plantId: selectedPlantId.value, limit: 20 })
  })
}

watch(selectedPlantId, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadPlantData)
  }
})

watch(selectedFacilityId, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadEnergyData)
  }
})

onMounted(() => {
  if (getAccessToken()) {
    loadInitial()
  }
})

onUnmounted(stopEnergyWebSocket)
</script>

<template>
  <main v-if="appMode === 'login'" class="login-shell">
    <section class="login-page">
      <article class="login-visual">
        <p>Smart Factory SCADA</p>
        <h2>에너지 데이터와 ESG 지표를 한 화면에서 확인합니다</h2>
        <div class="login-lines"></div>
      </article>
      <form class="login-card" @submit.prevent="login">
        <h2>로그인</h2>
        <label>이메일<input v-model="loginForm.email" type="email" autocomplete="username" /></label>
        <label>비밀번호<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '로그인 중' : '로그인' }}
        </button>
      </form>
    </section>
  </main>

  <main v-else-if="appMode === 'scada'" class="scada-dashboard">
    <header class="scada-top">
      <div>
        <p>Web SCADA</p>
        <h1>에너지 종합 현황</h1>
      </div>
      <div class="scada-top-actions">
        <span>{{ nowLabel }}</span>
        <button class="ghost-button" type="button" @click="logout">로그아웃</button>
        <button class="primary-button" type="button" @click="appMode = 'detail'">상세 화면</button>
      </div>
    </header>

    <section class="scada-filter">
      <label>
        사업장
        <select v-model.number="selectedPlantId">
          <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
        </select>
      </label>
      <label>
        설비
        <select v-model.number="selectedFacilityId">
          <option :value="null">전체 설비</option>
          <option v-for="facility in state.facilities" :key="facility.id" :value="facility.id">
            {{ facility.name }}
          </option>
        </select>
      </label>
      <label>
        수집 기준
        <input value="Redis 최신값 + DB 요약" readonly />
      </label>
      <button class="primary-button compact" type="button" @click="refreshData">새로고침</button>
    </section>

    <p v-if="errorMessage" class="api-error">{{ errorMessage }}</p>

    <section class="scada-energy-grid">
      <article v-for="card in energyCards" :key="card.label" :class="['scada-energy-card', card.tone]">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }} <small>{{ card.unit }}</small></strong>
        <p>{{ state.latestEnergy ? `실시간 수신 ${formatDateTime(latestMeasuredAt).slice(11)}` : 'DB 요약값' }}</p>
      </article>
    </section>

    <section class="scada-main-grid">
      <article class="scada-panel wide">
        <div class="panel-title inline">
          <h2>최근 에너지 요약 추이</h2>
          <span class="live-pill">{{ state.latestEnergy ? '현재값 반영' : 'API 연동' }}</span>
        </div>
        <div class="summary-chart">
          <div
            v-for="summary in summaryChartRows"
            :key="summary.id"
            :class="['summary-bar', { live: summary.live }]"
          >
            <i :style="{ height: `${chartHeight(summary)}%` }"></i>
            <span>{{ summary.label }}</span>
            <b v-if="summary.live">{{ formatNumber(summary.value) }}</b>
          </div>
        </div>
      </article>

      <aside class="scada-panel">
        <h2>운영 요약</h2>
        <div class="alert-list">
          <article>
            <b>{{ selectedPlant?.name || '-' }}</b>
            <span>설비 {{ state.facilities.length }}개, 발생 알람 {{ alarmCount }}건</span>
          </article>
          <article>
            <b>피크 전력</b>
            <span>
              {{
                formatNumber(
                  metricValue(state.latestEnergy, 'peakKw', 'peak_kw') ??
                    metricValue(latestSummary, 'peakKw', 'peak_kw'),
                )
              }}
              kW / {{ peakUsageRate }}%
            </span>
          </article>
          <article>
            <b>ESG 등급</b>
            <span>{{ latestEsg?.grade || '-' }} / {{ formatNumber(latestEsg?.totalScore) }}점</span>
          </article>
        </div>
        <h2 class="section-gap">최근 알람</h2>
        <ul class="log-list">
          <li v-for="alarm in state.alarms.slice(0, 5)" :key="alarm.id">
            <span>{{ formatDateTime(alarm.occurredAt).slice(11) }}</span>{{ alarm.message }}
          </li>
        </ul>
      </aside>
    </section>

    <section class="scada-main-grid lower">
      <article class="scada-panel wide">
        <h2>사업장 ESG 순위</h2>
        <div class="site-summary-list">
          <article v-for="score in state.esgScores" :key="score.id">
            <b>{{ score.plantName }}</b>
            <span>{{ formatNumber(score.totalScore) }}점</span>
            <em>{{ score.grade }}</em>
            <strong>{{ score.targetMonth }}</strong>
          </article>
        </div>
      </article>
      <aside class="scada-panel">
        <h2>선택 설비</h2>
        <div class="site-summary-list">
          <article v-for="facility in state.facilities" :key="facility.id">
            <b>{{ facility.name }}</b>
            <span>{{ facility.facilityType }}</span>
            <em :class="{ warn: facility.status !== 'RUNNING' }">{{ statusLabel(facility.status) }}</em>
          </article>
        </div>
      </aside>
    </section>
  </main>

  <main v-else class="detail-shell">
    <aside class="sidebar">
      <button class="logo-button" type="button" @click="appMode = 'scada'">
        <span class="logo-symbol">SF</span>
        <b>SCADA</b>
      </button>
      <nav class="side-nav" aria-label="상세 화면">
        <button
          v-for="item in navItems"
          :key="item.id"
          :class="{ active: activePage === item.id }"
          type="button"
          @click="activePage = item.id"
        >
          <span>{{ item.icon }}</span>{{ item.label }}
        </button>
      </nav>
      <div class="side-footer">
        <button type="button">
          <span class="avatar">{{ state.me?.name?.slice(0, 1) || 'U' }}</span>
          <span><b>{{ state.me?.name || '사용자' }}</b>{{ state.me?.role || '-' }}</span>
        </button>
        <button class="collapse-button" type="button" @click="appMode = 'scada'">대시보드</button>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ activeMeta.title }}</h1>
          <p>{{ activeMeta.description }}</p>
        </div>
        <div class="topbar-tools">
          <span class="clock">{{ nowLabel }}</span>
          <button class="icon-button" type="button" @click="refreshData">R</button>
        </div>
      </header>

      <section class="filter-card">
        <label>
          사업장
          <select v-model.number="selectedPlantId">
            <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
          </select>
        </label>
        <label>
          설비
          <select v-model.number="selectedFacilityId">
            <option :value="null">전체 설비</option>
            <option v-for="facility in state.facilities" :key="facility.id" :value="facility.id">
              {{ facility.name }}
            </option>
          </select>
        </label>
        <label>
          데이터 해상도
          <input value="최근 요약" readonly />
        </label>
      </section>

      <p v-if="errorMessage" class="api-error">{{ errorMessage }}</p>

      <section v-if="activePage === 'facility'" class="page-stack">
        <div class="kpi-grid">
          <article v-for="card in energyCards" :key="card.label" :class="['kpi-card', card.tone]">
            <span>{{ card.label }}</span><b>{{ card.value }} <small>{{ card.unit }}</small></b>
            <p>{{ selectedFacility?.name || '전체 설비' }}</p>
          </article>
        </div>
        <article class="panel table-panel">
          <h2>측정 이력</h2>
          <table>
            <thead>
              <tr><th>측정 시각</th><th>설비 ID</th><th>전기</th><th>가스</th><th>용수</th><th>태양광</th><th>피크</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in state.measurements" :key="row.id">
                <td>{{ formatDateTime(row.measuredAt) }}</td>
                <td>{{ row.facilityId }}</td>
                <td>{{ formatNumber(row.electricityKwh) }}</td>
                <td>{{ formatNumber(row.gasM3) }}</td>
                <td>{{ formatNumber(row.waterTon) }}</td>
                <td>{{ formatNumber(row.solarKwh) }}</td>
                <td>{{ formatNumber(row.peakKw) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'peak'" class="page-stack">
        <div class="kpi-grid">
          <article class="kpi-card">
            <span>현재 피크</span>
            <b>
              {{
                formatNumber(
                  metricValue(state.latestEnergy, 'peakKw', 'peak_kw') ??
                    metricValue(latestSummary, 'peakKw', 'peak_kw'),
                )
              }}
              <small>kW</small>
            </b>
            <p>피크 기준 1,400 kW</p>
          </article>
          <article class="kpi-card cyan"><span>사용률</span><b>{{ peakUsageRate }}<small>%</small></b><p>요약 데이터 기준</p></article>
          <article class="kpi-card purple"><span>알람</span><b>{{ alarmCount }}<small>건</small></b><p>발생 상태</p></article>
          <article class="kpi-card green"><span>요약 건수</span><b>{{ state.summaries.length }}<small>건</small></b><p>선택 조건 기준</p></article>
        </div>
      </section>

      <section v-else-if="activePage === 'utility'" class="page-stack">
        <article class="panel table-panel">
          <h2>가스/용수 요약</h2>
          <table>
            <thead><tr><th>요약 시각</th><th>설비 ID</th><th>가스 m3</th><th>용수 ton</th><th>탄소배출</th></tr></thead>
            <tbody>
              <tr v-for="row in state.summaries" :key="row.id">
                <td>{{ formatDateTime(row.summaryAt) }}</td>
                <td>{{ row.facilityId || '사업장 합계' }}</td>
                <td>{{ formatNumber(row.gasM3) }}</td>
                <td>{{ formatNumber(row.waterTon) }}</td>
                <td>{{ formatNumber(row.carbonEmission) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'esg'" class="page-stack">
        <article class="panel table-panel">
          <h2>ESG 점수</h2>
          <table>
            <thead><tr><th>사업장</th><th>월</th><th>전기</th><th>가스</th><th>용수</th><th>피크</th><th>총점</th><th>등급</th></tr></thead>
            <tbody>
              <tr v-for="score in state.esgScores" :key="score.id">
                <td>{{ score.plantName }}</td>
                <td>{{ score.targetMonth }}</td>
                <td>{{ formatNumber(score.electricityScore) }}</td>
                <td>{{ formatNumber(score.gasScore) }}</td>
                <td>{{ formatNumber(score.waterScore) }}</td>
                <td>{{ formatNumber(score.peakScore) }}</td>
                <td>{{ formatNumber(score.totalScore) }}</td>
                <td><b>{{ score.grade }}</b></td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'users'" class="page-stack">
        <article class="panel table-panel">
          <h2>사용자 목록</h2>
          <table>
            <thead><tr><th>ID</th><th>이름</th><th>이메일</th><th>권한</th><th>사업장 ID</th><th>상태</th><th>최근 로그인</th></tr></thead>
            <tbody>
              <tr v-for="user in state.users" :key="user.userId">
                <td>{{ user.userId }}</td>
                <td>{{ user.name }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.role }}</td>
                <td>{{ user.plantId || '-' }}</td>
                <td><span :class="['badge', user.status === 'ACTIVE' ? 'ok' : 'warn']">{{ statusLabel(user.status) }}</span></td>
                <td>{{ formatDateTime(user.lastLoginAt) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else class="page-stack">
        <article class="panel table-panel">
          <h2>알람 목록</h2>
          <table>
            <thead><tr><th>발생 시각</th><th>설비</th><th>레벨</th><th>메시지</th><th>값</th><th>기준</th><th>상태</th><th>처리</th></tr></thead>
            <tbody>
              <tr v-for="alarm in state.alarms" :key="alarm.id">
                <td>{{ formatDateTime(alarm.occurredAt) }}</td>
                <td>{{ alarm.facilityName }}</td>
                <td>{{ alarm.alarmLevel }}</td>
                <td>{{ alarm.message }}</td>
                <td>{{ formatNumber(alarm.value) }}</td>
                <td>{{ formatNumber(alarm.thresholdValue) }}</td>
                <td><span :class="['badge', alarm.status === 'RESOLVED' ? 'ok' : 'warn']">{{ statusLabel(alarm.status) }}</span></td>
                <td><button class="light-button" type="button" :disabled="alarm.status === 'RESOLVED'" @click="resolveAlarm(alarm.id)">처리</button></td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>
    </section>
  </main>
</template>
