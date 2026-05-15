<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { Activity, Bolt, Gauge, History, ListOrdered, Search, Zap } from 'lucide-vue-next'
import { api, clearTokens, getAccessToken, saveTokens } from './api'

const appMode = ref(getAccessToken() ? 'scada' : 'login')
const SCADA_EXTERNAL_URL = 'http://192.168.0.100:11005/?Pro=ksj_260430#%EC%98%88%EC%8B%9C1'
const activePage = ref('facility')
const loading = ref(false)
const errorMessage = ref('')
const selectedPlantId = ref(null)
const selectedFacilityId = ref(null)
const selectedEnergyType = ref('ELECTRICITY')
const selectedDateFrom = ref('')
const selectedDateTo = ref('')
const selectedPeakDate = ref(formatDateInput(new Date()))
const syncingSelection = ref(false)
let energySocket = null
let energySocketReconnectTimer = null
let peakRefreshTimer = null
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
  peakDashboard: null,
  facilityDetail: null,
  alarms: [],
  esgScores: [],
  users: [],
})

const energyTypeOptions = [
  { value: 'ELECTRICITY', label: '전기', unit: 'kWh', tone: 'electric', iconPath: 'M13 2 4 14h7l-2 8 9-13h-7l2-7z' },
  { value: 'GAS', label: '가스', unit: 'm3', tone: 'gas', iconPath: 'M12 22c-3.6 0-6-2.4-6-5.8 0-2.7 1.8-4.7 3.4-6.4 1.4-1.5 2.7-3 2.7-5.8 3.4 2.3 6 5.7 6 9.5 0 .5-.1 1-.2 1.4.7-.5 1.3-1.2 1.7-2.2.9 4.8-1.8 9.3-7.6 9.3z' },
  { value: 'WATER', label: '용수', unit: 'ton', tone: 'water', iconPath: 'M12 22a7 7 0 0 1-7-7c0-4.9 7-13 7-13s7 8.1 7 13a7 7 0 0 1-7 7z' },
  { value: 'SOLAR', label: '태양광', unit: 'kWh', tone: 'solar', iconPath: 'M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12zM12 1v3M12 20v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M1 12h3M20 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1' },
]

const equipmentIconPath = 'M3 21V9l6 3V9l6 3V5h6v16H3zM7 17h2M12 17h2M17 17h2M17 9h2'
const trendIconPath = 'M4 16l5-5 4 4 7-8M14 7h6v6'
const storageIconPath = 'M5 6c0-1.7 3.1-3 7-3s7 1.3 7 3-3.1 3-7 3-7-1.3-7-3zM5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6'

const navItems = [
  { id: 'facility', label: '설비 조회', icon: 'F' },
  { id: 'peak', label: '피크 전력', icon: 'P' },
  { id: 'utility', label: '가스/용수', icon: 'U' },
  { id: 'esg', label: 'ESG 평가', icon: 'E' },
  { id: 'users', label: '사용자 관리', icon: 'M' },
  { id: 'alarms', label: '알람', icon: 'A' },
]

const validRoutes = ['facility', 'peak', 'utility', 'esg', 'users', 'alarms']

const activeMeta = computed(() => {
  const meta = {
    facility: ['설비별 에너지 현황', '사업장, 설비, 에너지 요약, 최신 수집값을 확인합니다.'],
    peak: ['피크 전력 현황', '피크 전력과 발생 알람을 확인합니다.'],
    utility: ['가스/용수 모니터링', '가스와 용수 사용량을 설비 단위로 확인합니다.'],
    esg: ['ESG 평가 지표', '사업장별 ESG 점수를 확인합니다.'],
    users: ['사용자 관리', '사용자 계정과 권한 상태를 확인합니다.'],
    alarms: ['알람 관리', '발생 알람과 처리 상태를 확인합니다.'],
  }
  const [title, description] = meta[activePage.value] || meta.facility
  return { title, description }
})

const selectedPlant = computed(() => state.plants.find((plant) => plant.id === selectedPlantId.value))
const selectedFacility = computed(() => state.facilities.find((facility) => facility.id === selectedFacilityId.value))
const latestSummary = computed(() => state.overview?.latestEnergySummary || state.summaries.at(-1) || null)
const latestEsg = computed(() => state.overview?.latestEsgScore || state.esgScores[0] || null)
const recentSummaries = computed(() => state.summaries.slice(-8))
const latestMeasuredAt = computed(() => metricValue(state.latestEnergy, 'measuredAt', 'measured_at'))
const selectedEnergyMeta = computed(
  () => energyTypeOptions.find((option) => option.value === selectedEnergyType.value) || energyTypeOptions[0],
)
const facilityDetailChart = computed(() => state.facilityDetail?.chart || [])
const facilityDetailLogs = computed(() => state.facilityDetail?.logs || [])
const facilityDetailMaxUsage = computed(() =>
  Math.max(1, ...facilityDetailChart.value.map((point) => Number(point.usage || 0))),
)
const facilityTodayUsage = computed(() => state.facilityDetail?.todayUsage ?? 0)
const facilityChangeAmount = computed(() => state.facilityDetail?.changeAmount ?? 0)
const facilityChangeRate = computed(() => state.facilityDetail?.changeRate ?? 0)

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

function facilityBarHeight(point) {
  const value = Number(point?.usage || 0)
  return Math.max(8, Math.round((value / facilityDetailMaxUsage.value) * 100))
}

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return value.slice(0, 10)
}

function formatChartDate(value) {
  const date = formatDate(value)
  return date === '-' ? '-' : date.slice(5)
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function trendClass(value) {
  const numericValue = Number(value || 0)
  if (numericValue > 0) {
    return 'up'
  }
  if (numericValue < 0) {
    return 'down'
  }
  return 'flat'
}

function trendPrefix(value) {
  return Number(value || 0) > 0 ? '+' : ''
}

function applySummaryDateRange(summaries) {
  if (!summaries.length || selectedDateFrom.value || selectedDateTo.value) {
    return
  }
  const dates = summaries
    .map((summary) => formatDate(summary.summaryAt))
    .filter((date) => date !== '-')
  const latestDate = dates.at(-1)
  if (!latestDate) {
    return
  }
  const startDate = new Date(`${latestDate}T00:00:00`)
  startDate.setDate(startDate.getDate() - 6)
  syncingSelection.value = true
  selectedDateFrom.value = formatDateInput(startDate)
  selectedDateTo.value = latestDate
  syncingSelection.value = false
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
const peakMetrics = computed(() => state.peakDashboard?.metrics || {})
const peakTrend = computed(() => state.peakDashboard?.trend || [])
const peakRanking = computed(() => state.peakDashboard?.facilityRanking || [])
const peakHistory = computed(() => state.peakDashboard?.history || [])
const peakThresholdKw = computed(() => Number(peakMetrics.value.thresholdKw || 1400))
const peakGaugeRate = computed(() => Math.min(Number(peakMetrics.value.peakUsageRate || 0), 125))
const peakGaugeStyle = computed(() => ({ '--peak-rate': `${Math.min(peakGaugeRate.value, 100)}%` }))
const peakTrendMax = computed(() =>
  Math.max(
    peakThresholdKw.value,
    1,
    ...peakTrend.value.map((point) => Number(point.maxKw || point.max_kw || 0)),
    ...peakTrend.value.map((point) => Number(point.averageKw || point.average_kw || 0)),
  ),
)
const peakTrendPoints = computed(() =>
  peakTrend.value.map((point) => ({
    measuredAt: point.measuredAt || point.measured_at,
    averageKw: Number(point.averageKw || point.average_kw || 0),
    maxKw: Number(point.maxKw || point.max_kw || 0),
  })),
)
const peakAveragePath = computed(() => linePath(peakTrendPoints.value, 'averageKw', peakTrendMax.value))
const peakMaxPath = computed(() => linePath(peakTrendPoints.value, 'maxKw', peakTrendMax.value))
const peakHistoryRows = computed(() =>
  peakHistory.value.map((row, index) => ({
    ...row,
    rank: index + 1,
    measuredAt: row.measuredAt || row.measured_at,
    peakKw: row.peakKw || row.peak_kw,
    peakUsageRate: row.peakUsageRate || row.peak_usage_rate,
    durationMinutes: row.durationMinutes || row.duration_minutes,
    thresholdKw: row.thresholdKw || row.threshold_kw,
  })),
)

function routeTo(hash) {
  if (hash === '/scada') {
    window.location.href = SCADA_EXTERNAL_URL
    return
  }
  if (window.location.hash === `#${hash}`) {
    applyRoute()
    return
  }
  window.location.hash = hash
}

function applyRoute() {
  const route = window.location.hash.replace(/^#/, '') || (getAccessToken() ? '/scada' : '/login')

  if (route === '/login') {
    appMode.value = 'login'
    return
  }

  if (route === '/scada') {
    window.location.href = SCADA_EXTERNAL_URL
    return
  }

  const detailMatch = route.match(/^\/detail\/([^/]+)$/)
  if (detailMatch && validRoutes.includes(detailMatch[1])) {
    activePage.value = detailMatch[1]
    appMode.value = 'detail'
    return
  }

  routeTo(getAccessToken() ? '/scada' : '/login')
}

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

function formatTime(value) {
  const formatted = formatDateTime(value)
  return formatted === '-' ? '-' : formatted.slice(11, 16)
}

function linePath(points, key, maxValue) {
  if (!points.length) {
    return ''
  }
  const width = 720
  const height = 220
  const gap = points.length <= 1 ? width : width / (points.length - 1)
  return points
    .map((point, index) => {
      const x = Math.round(index * gap)
      const ratio = Math.min(Number(point[key] || 0) / maxValue, 1)
      const y = Math.round(height - ratio * (height - 24) - 12)
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`
    })
    .join(' ')
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
      routeTo('/login')
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
    routeTo('/scada')
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
      routeTo('/login')
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
  await loadPeakDashboard()
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
  applySummaryDateRange(summaries)
  await loadFacilityDetail()
  startEnergyWebSocket()
}

async function loadFacilityDetail() {
  if (!selectedPlantId.value || !selectedFacilityId.value) {
    state.facilityDetail = null
    return
  }

  state.facilityDetail = await api.energyFacilityDetail({
    plantId: selectedPlantId.value,
    facilityId: selectedFacilityId.value,
    energyType: selectedEnergyType.value,
    from: selectedDateFrom.value || undefined,
    to: selectedDateTo.value || undefined,
  })
}

async function loadPeakDashboard() {
  if (!selectedPlantId.value) {
    state.peakDashboard = null
    return
  }

  state.peakDashboard = await api.peakDashboard({
    plantId: selectedPlantId.value,
    date: selectedPeakDate.value || undefined,
  })
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
    if (plantId === Number(selectedPlantId.value) && activePage.value === 'peak') {
      schedulePeakRefresh()
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

function schedulePeakRefresh() {
  window.clearTimeout(peakRefreshTimer)
  peakRefreshTimer = window.setTimeout(() => {
    loadPeakDashboard().catch(() => {})
  }, 500)
}

async function resolveAlarm(alarmId) {
  await run(async () => {
    await api.resolveAlarm(alarmId)
    state.alarms = await api.alarms({ plantId: selectedPlantId.value, limit: 20 })
  })
}

function goScada() {
  routeTo('/scada')
}

function goDetail(page = 'facility') {
  routeTo(`/detail/${page}`)
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

watch(selectedEnergyType, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadFacilityDetail)
  }
})

watch([selectedDateFrom, selectedDateTo], () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadFacilityDetail)
  }
})

watch(selectedPeakDate, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadPeakDashboard)
  }
})

onMounted(() => {
  applyRoute()
  window.addEventListener('hashchange', applyRoute)

  if (getAccessToken()) {
    loadInitial()
  }
})

onUnmounted(() => {
  stopEnergyWebSocket()
  window.clearTimeout(peakRefreshTimer)
  window.removeEventListener('hashchange', applyRoute)
})

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
        <button class="primary-button" type="button" @click="goDetail()">상세 화면</button>
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
        <input value="DB 최신값 + DB 요약" readonly />
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
      <button class="logo-button" type="button" @click="goScada">
        <span class="logo-symbol">SF</span>
        <b>SCADA</b>
      </button>
      <nav class="side-nav" aria-label="상세 화면">
        <button
          v-for="item in navItems"
          :key="item.id"
          :class="{ active: activePage === item.id }"
          type="button"
          @click="goDetail(item.id)"
        >
          <span>{{ item.icon }}</span>{{ item.label }}
        </button>
      </nav>
      <div class="side-footer">
        <button type="button">
          <span class="avatar">{{ state.me?.name?.slice(0, 1) || 'U' }}</span>
          <span><b>{{ state.me?.name || '사용자' }}</b>{{ state.me?.role || '-' }}</span>
        </button>
        <button class="collapse-button" type="button" @click="goScada">대시보드</button>
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
        <label v-if="activePage !== 'peak'">
          설비
          <select v-model.number="selectedFacilityId">
            <option :value="null">전체 설비</option>
            <option v-for="facility in state.facilities" :key="facility.id" :value="facility.id">
              {{ facility.name }}
            </option>
          </select>
        </label>
        <label v-if="activePage !== 'peak'">
          에너지 종류
          <select v-model="selectedEnergyType">
            <option v-for="option in energyTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label v-if="activePage !== 'peak'">
          시작일
          <input v-model="selectedDateFrom" type="date" />
        </label>
        <label v-if="activePage !== 'peak'">
          종료일
          <input v-model="selectedDateTo" type="date" />
        </label>
      </section>

      <p v-if="errorMessage" class="api-error">{{ errorMessage }}</p>

      <section v-if="activePage === 'facility'" class="page-stack">
        <section class="facility-status-layout">
          <article class="panel facility-chart-panel">
            <div class="panel-title inline">
              <h2>설비 사용량 추이 <small>({{ selectedEnergyMeta.label }})</small></h2>
              <span class="live-pill">{{ state.facilityDetail ? '상세 API 연동' : '데이터 대기' }}</span>
            </div>
            <div class="facility-bar-chart" :class="selectedEnergyMeta.tone">
              <div v-for="point in facilityDetailChart" :key="point.date" class="facility-bar">
                <b>{{ formatNumber(point.usage, 0) }}</b>
                <i :style="{ height: `${facilityBarHeight(point)}%` }"></i>
                <span>{{ formatChartDate(point.date) }}</span>
              </div>
            </div>
          </article>

          <aside class="panel facility-detail-card" :class="selectedEnergyMeta.tone">
            <div class="facility-card-head">
              <span class="facility-device-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path :d="equipmentIconPath"></path>
                </svg>
              </span>
              <div>
                <h2>{{ state.facilityDetail?.facilityName || selectedFacility?.name || '-' }}</h2>
                <p>{{ state.facilityDetail?.facilityType || selectedFacility?.facilityType || '-' }}</p>
              </div>
              <em :class="{ warn: state.facilityDetail?.facilityStatus !== 'RUNNING' }">
                {{ statusLabel(state.facilityDetail?.facilityStatus || selectedFacility?.status) }}
              </em>
            </div>

            <div class="facility-stat-list">
              <article>
                <span class="round-icon energy">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path :d="selectedEnergyMeta.iconPath"></path>
                  </svg>
                </span>
                <div>
                  <p>금일 사용량</p>
                  <strong>{{ formatNumber(facilityTodayUsage) }} <small>{{ state.facilityDetail?.unit || selectedEnergyMeta.unit }}</small></strong>
                </div>
              </article>
              <article>
                <span class="round-icon trend">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path :d="trendIconPath"></path>
                  </svg>
                </span>
                <div>
                  <p>전일 대비</p>
                  <strong :class="trendClass(facilityChangeRate)">
                    {{ trendPrefix(facilityChangeRate) }}{{ formatNumber(facilityChangeRate) }}<small>%</small>
                  </strong>
                  <small>{{ trendPrefix(facilityChangeAmount) }}{{ formatNumber(facilityChangeAmount) }} {{ state.facilityDetail?.unit || selectedEnergyMeta.unit }}</small>
                </div>
              </article>
              <article>
                <span class="round-icon storage">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path :d="storageIconPath"></path>
                  </svg>
                </span>
                <div>
                  <p>데이터 수집 상태</p>
                  <strong>정상</strong>
                  <small>최근 수집 {{ formatDateTime(state.facilityDetail?.latestMeasuredAt) }}</small>
                </div>
              </article>
            </div>
          </aside>
        </section>

        <article class="panel table-panel facility-log-panel">
          <div class="panel-title inline">
            <h2>사용 로그</h2>
            <span>{{ state.facilityDetail?.facilityName || selectedFacility?.name || '전체 설비' }}</span>
          </div>
          <table>
            <thead>
              <tr><th>일자</th><th>설비</th><th>에너지 종류</th><th>사용량</th><th>전일 대비</th><th>증감률</th><th>수집 시간</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in facilityDetailLogs" :key="row.measuredAt">
                <td>{{ formatDate(row.measuredAt) }}</td>
                <td>{{ state.facilityDetail?.facilityName || selectedFacility?.name || '-' }}</td>
                <td>{{ selectedEnergyMeta.label }}</td>
                <td>{{ formatNumber(row.usage) }} {{ state.facilityDetail?.unit || selectedEnergyMeta.unit }}</td>
                <td :class="trendClass(row.changeAmount)">
                  {{ trendPrefix(row.changeAmount) }}{{ formatNumber(row.changeAmount) }}
                </td>
                <td :class="trendClass(row.changeRate)">
                  {{ trendPrefix(row.changeRate) }}{{ formatNumber(row.changeRate) }}%
                </td>
                <td>{{ formatDateTime(row.measuredAt) }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'peak'" class="page-stack peak-monitor-page">
        <section class="peak-filter-row">
          <label>
            조회일
            <input v-model="selectedPeakDate" type="date" />
          </label>
          <button class="primary-button compact" type="button" @click="run(loadPeakDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">{{ state.latestEnergy ? '실시간 수신 중' : '금일 데이터 조회' }}</span>
        </section>

        <section class="peak-kpi-grid">
          <article class="peak-kpi-card">
            <span class="peak-card-icon blue"><Zap :size="22" /></span>
            <div>
              <p>현재 전력 사용량</p>
              <b>{{ formatNumber(peakMetrics.currentKw) }}<small> kW</small></b>
              <em>측정 {{ formatTime(peakMetrics.measuredAt) }}</em>
            </div>
          </article>
          <article class="peak-kpi-card">
            <span class="peak-card-icon cyan"><Gauge :size="22" /></span>
            <div>
              <p>피크 사용률</p>
              <b>{{ formatNumber(peakMetrics.peakUsageRate) }}<small>%</small></b>
              <em>피크 기준 {{ formatNumber(peakMetrics.thresholdKw, 0) }} kW</em>
            </div>
          </article>
          <article class="peak-kpi-card">
            <span class="peak-card-icon green"><Activity :size="22" /></span>
            <div>
              <p>15분 평균 전력</p>
              <b>{{ formatNumber(peakMetrics.intervalAverageKw) }}<small> kW</small></b>
              <em>구간 {{ formatTime(peakMetrics.intervalAt) }}</em>
            </div>
          </article>
          <article class="peak-kpi-card">
            <span class="peak-card-icon purple"><Bolt :size="22" /></span>
            <div>
              <p>15분 최대 전력</p>
              <b>{{ formatNumber(peakMetrics.intervalMaxKw) }}<small> kW</small></b>
              <em>금일 집계 기준</em>
            </div>
          </article>
        </section>

        <section class="peak-content-grid">
          <article class="panel peak-gauge-panel">
            <div class="panel-title inline">
              <h2>피크 사용률 현황</h2>
              <span>단위: %</span>
            </div>
            <div class="peak-gauge" :style="peakGaugeStyle">
              <div>
                <strong>{{ formatNumber(peakMetrics.peakUsageRate) }}%</strong>
                <span>{{ formatNumber(peakMetrics.currentKw) }} / {{ formatNumber(peakMetrics.thresholdKw, 0) }} kW</span>
              </div>
            </div>
            <div class="peak-scale">
              <span>0</span><span>50</span><span>100</span><span>125</span>
            </div>
          </article>

          <article class="panel peak-chart-panel">
            <div class="panel-title inline">
              <h2>금일 피크 전력 추이</h2>
              <div class="peak-legend">
                <span><i class="avg"></i>15분 평균</span>
                <span><i class="max"></i>15분 최대</span>
                <span><i class="limit"></i>피크 기준</span>
              </div>
            </div>
            <svg class="peak-line-chart" viewBox="0 0 720 240" role="img" aria-label="금일 피크 전력 추이">
              <g class="peak-grid-lines">
                <line v-for="row in 5" :key="row" x1="0" x2="720" :y1="row * 44" :y2="row * 44" />
              </g>
              <line
                class="peak-threshold-line"
                x1="0"
                x2="720"
                :y1="220 - Math.min(peakThresholdKw / peakTrendMax, 1) * 196"
                :y2="220 - Math.min(peakThresholdKw / peakTrendMax, 1) * 196"
              />
              <path v-if="peakAveragePath" class="peak-line avg" :d="peakAveragePath" />
              <path v-if="peakMaxPath" class="peak-line max" :d="peakMaxPath" />
            </svg>
            <div class="peak-chart-axis">
              <span>{{ formatTime(peakTrendPoints[0]?.measuredAt) }}</span>
              <span>{{ formatNumber(peakTrendMax, 0) }} kW</span>
              <span>{{ formatTime(peakTrendPoints.at(-1)?.measuredAt) }}</span>
            </div>
          </article>

          <article class="panel peak-ranking-panel">
            <div class="panel-title inline">
              <h2>설비별 전력 사용 순위</h2>
              <ListOrdered :size="20" />
            </div>
            <div class="peak-ranking-list">
              <article v-for="(item, index) in peakRanking" :key="item.facilityId || index">
                <b>{{ index + 1 }}</b>
                <div>
                  <strong>{{ item.facilityName || `설비 ${item.facilityId}` }}</strong>
                  <span>{{ formatNumber(item.usageKwh) }} kWh · 피크 {{ formatNumber(item.peakKw) }} kW</span>
                  <i :style="{ width: `${Math.min(Number(item.shareRate || 0), 100)}%` }"></i>
                </div>
                <em>{{ formatNumber(item.shareRate) }}%</em>
              </article>
            </div>
          </article>
        </section>

        <article class="panel table-panel peak-history-panel">
          <div class="panel-title inline">
            <h2>피크 이력</h2>
            <History :size="20" />
          </div>
          <table>
            <thead>
              <tr>
                <th>순번</th>
                <th>발생 일시</th>
                <th>피크 전력</th>
                <th>피크 사용률</th>
                <th>지속 시간</th>
                <th>기준 전력</th>
                <th>비고</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in peakHistoryRows" :key="`${row.measuredAt}-${row.rank}`">
                <td>{{ row.rank }}</td>
                <td>{{ formatDateTime(row.measuredAt) }}</td>
                <td>{{ formatNumber(row.peakKw) }} kW</td>
                <td :class="{ up: Number(row.peakUsageRate || 0) >= 100 }">{{ formatNumber(row.peakUsageRate) }}%</td>
                <td>{{ row.durationMinutes || 15 }}분</td>
                <td>{{ formatNumber(row.thresholdKw, 0) }} kW</td>
                <td>{{ row.exceeded ? '피크 초과' : '-' }}</td>
              </tr>
              <tr v-if="!peakHistoryRows.length">
                <td colspan="7">조회된 피크 이력이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </article>
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
