<script setup>
import { computed, ref } from 'vue'

<<<<<<< Updated upstream
const appMode = ref('login')
=======
const appMode = ref(getAccessToken() ? 'scada' : 'login')
const SCADA_EXTERNAL_URL = 'http://192.168.0.100:11005/?Pro=ksj_260430#%EC%98%88%EC%8B%9C1'
>>>>>>> Stashed changes
const activePage = ref('facility')
const nowLabel = '2025.05.16 (금) 14:30'

const sites = [
  '기아 광명',
  '기아 광주',
  '기아 화성',
  '현대 울산',
  '현대 아산',
  '현대 전주',
]

const navItems = [
  { id: 'facility', label: '설비별 조회', icon: '▦' },
  { id: 'peak', label: '피크 전력', icon: '▥' },
  { id: 'utility', label: '가스 · 용수', icon: '◒' },
  { id: 'esg', label: 'ESG 평가 지표', icon: '◎' },
  { id: 'users', label: '사용자 관리', icon: '☷' },
]

const pageMeta = {
  facility: {
    title: '설비별 사용량 조회',
    description: '조건에 따라 설비별 에너지 사용량과 이력을 조회합니다.',
  },
  peak: {
    title: '전력 가동률 현황',
    description: '실시간 전력 사용량과 15분 단위 피크 전력 지표를 확인합니다.',
  },
  utility: {
    title: '가스 · 용수 모니터링',
    description: '가스와 용수의 상태를 통합 조회하는 사용량 모니터링 화면입니다.',
  },
  esg: {
    title: 'ESG 평가 지표',
    description: '전력·가스·용수·태양광·피크 전력 데이터를 기반으로 6개 사업장의 자체 ESG 등급을 조회합니다.',
  },
  users: {
    title: '사용자 관리',
    description: '시스템 사용자를 등록하고 조회·관리합니다.',
  },
}

const activeMeta = computed(() => pageMeta[activePage.value])

const energyCards = [
  { label: '전기 사용량', value: '12,450', unit: 'kWh', change: '▼ 6.2%', tone: 'electric' },
  { label: '가스 사용량', value: '2,850', unit: 'Nm³', change: '▲ 8.1%', tone: 'gas' },
  { label: '용수 사용량', value: '128.6', unit: 'm³', change: '▼ 2.7%', tone: 'water' },
  { label: '태양광 발전량', value: '3,420', unit: 'kWh', change: '▲ 15.3%', tone: 'solar' },
]

const scadaAlerts = [
  ['피크 전력 주의', '현재 전력 1,320 kW · 피크 기준 1,400 kW'],
  ['계측기 상태', '전체 24대 중 24대 정상 수신'],
  ['태양광 데이터', '금일 누적 3,420 kWh'],
  ['데이터 수집 상태', '수집률 100%'],
]

const scadaLogs = [
  ['10:30:00', '태양광 데이터 수신 정상'],
  ['10:25:00', '피크 전력 주의 발생'],
  ['10:20:00', '계측기 상태 정상'],
  ['10:15:00', '가스 사용량 전월 대비 +8.1%'],
]

const siteSummary = [
  { name: '기아 광명', energy: '11,980', peak: '주의', esg: 'AA' },
  { name: '기아 광주', energy: '10,860', peak: '정상', esg: 'A' },
  { name: '기아 화성', energy: '12,450', peak: '주의', esg: 'AAA' },
  { name: '현대 울산', energy: '13,280', peak: '경고', esg: 'A' },
  { name: '현대 아산', energy: '9,760', peak: '정상', esg: 'BBB' },
  { name: '현대 전주', energy: '8,940', peak: '정상', esg: 'BB' },
]

const dailyUsage = [
  { date: '05/10 (토)', value: 2350 },
  { date: '05/11 (일)', value: 2180 },
  { date: '05/12 (월)', value: 2560 },
  { date: '05/13 (화)', value: 2910 },
  { date: '05/14 (수)', value: 2420 },
  { date: '05/15 (목)', value: 2680 },
  { date: '05/16 (금)', value: 2740 },
]

const usageRows = [
  ['2025-05-16 (금)', '설비-01', '전력계-01', '전기', '2,740', '+60', '+2.24%', '2025-05-16 14:30', '정상'],
  ['2025-05-15 (목)', '설비-01', '전력계-01', '전기', '2,680', '+260', '+10.74%', '2025-05-15 14:30', '정상'],
  ['2025-05-14 (수)', '설비-01', '전력계-01', '전기', '2,420', '-490', '-16.83%', '2025-05-14 14:30', '정상'],
  ['2025-05-13 (화)', '설비-01', '전력계-01', '전기', '2,910', '+350', '+13.67%', '2025-05-13 14:30', '정상'],
  ['2025-05-12 (월)', '설비-01', '전력계-01', '전기', '2,560', '+380', '+17.44%', '2025-05-12 14:30', '정상'],
  ['2025-05-11 (일)', '설비-01', '전력계-01', '전기', '2,180', '-170', '-7.24%', '2025-05-11 14:30', '정상'],
]

const peakHistory = [
  ['2025-05-26 14:15', '1,380', '94%', '15분', '-'],
  ['2025-05-22 13:45', '1,410', '101%', '30분', '피크 초과'],
  ['2025-05-21 14:00', '1,380', '99%', '15분', '-'],
  ['2025-05-15 13:30', '1,360', '97%', '15분', '-'],
]

const meterRows = [
  ['계측기-01', '가스', '125.4', 'Nm³/h', '정상', '2025-05-26 10:30:05'],
  ['계측기-02', '가스', '98.7', 'Nm³/h', '정상', '2025-05-26 10:30:03'],
  ['계측기-03', '용수', '8.6', 'm³/h', '정상', '2025-05-26 10:30:04'],
  ['계측기-04', '용수', '12.3', 'm³/h', '정상', '2025-05-26 10:30:02'],
  ['계측기-05', '용수', '6.4', 'm³/h', '주의', '2025-05-26 10:30:01'],
]

const esgSites = [
  { name: '기아 화성', grade: 'AAA', score: 8.74, diff: '+0.52' },
  { name: '기아 광명', grade: 'AA', score: 8.02, diff: '+0.31' },
  { name: '현대 울산', grade: 'A', score: 7.06, diff: '+0.18' },
  { name: '기아 광주', grade: 'A', score: 6.88, diff: '+0.07' },
  { name: '현대 아산', grade: 'BBB', score: 5.48, diff: '-0.12' },
  { name: '현대 전주', grade: 'BB', score: 4.22, diff: '-0.35' },
]

const users = [
  ['admin', '관리자', '최고관리자', '본사', 'admin@abc.com', '활성', '2025-05-16 09:12'],
  ['KM2018', '홍길동', '운영자', '기아 광명', 'km2018@abc.com', '정상', '2025-05-16 08:55'],
  ['HS1022', '김나리', '운영자', '기아 화성', 'hs1022@abc.com', '정상', '2025-05-15 18:20'],
  ['AS3301', '이지훈', '조회자', '현대 아산', 'as3301@abc.com', '비활성', '2025-05-10 14:31'],
  ['GJ2044', '박서연', '운영자', '기아 광주', 'gj2044@abc.com', '정상', '2025-05-16 07:44'],
]

const login = () => {
  appMode.value = 'scada'
}

const goDetail = (page = 'facility') => {
  activePage.value = page
  appMode.value = 'detail'
}

const goScada = () => {
  appMode.value = 'scada'
}
<<<<<<< Updated upstream
=======

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
>>>>>>> Stashed changes
</script>

<template>
  <main v-if="appMode === 'login'" class="login-shell">
    <section class="login-page">
      <article class="login-visual">
        <p>생성형 AI 기반</p>
        <h2>ESG 에너지 분석 평가 플랫폼</h2>
        <div class="login-lines"></div>
      </article>
      <article class="login-card">
        <h2>로그인</h2>
        <label>사용자 ID<input placeholder="사용자 ID를 입력하세요" /></label>
        <label>패스워드<input placeholder="패스워드를 입력하세요" type="password" /></label>
        <label class="check-line"><input type="checkbox" /> 아이디 저장</label>
        <button class="primary-button" type="button" @click="login">로그인</button>
      </article>
    </section>
  </main>

  <main v-else-if="appMode === 'scada'" class="scada-dashboard">
    <header class="scada-top">
      <div>
        <p>Web SCADA · SMWP</p>
        <h1>에너지 종합 현황</h1>
      </div>
      <div class="scada-top-actions">
        <span>{{ nowLabel }}</span>
        <button class="ghost-button" type="button" @click="appMode = 'login'">로그아웃</button>
        <button class="primary-button" type="button" @click="goDetail()">상세 화면</button>
      </div>
    </header>

    <section class="scada-filter">
      <label>
        사업장
        <select>
          <option>기아 화성</option>
          <option v-for="site in sites" :key="site">{{ site }}</option>
        </select>
      </label>
      <label>
        조회일
        <input value="2025-05-26" readonly />
      </label>
      <label>
        자동 갱신
        <select>
          <option>ON</option>
          <option>OFF</option>
        </select>
      </label>
    </section>

    <section class="scada-energy-grid">
      <article v-for="card in energyCards" :key="card.label" :class="['scada-energy-card', card.tone]">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }} <small>{{ card.unit }}</small></strong>
        <p>전일 대비 {{ card.change }}</p>
      </article>
    </section>

    <section class="scada-main-grid">
      <article class="scada-panel wide">
        <div class="panel-title inline">
          <h2>시간대별 통합 에너지 사용 추이</h2>
          <span class="live-pill">실시간</span>
        </div>
        <svg class="scada-line-chart" viewBox="0 0 900 360" role="img" aria-label="시간대별 통합 에너지 사용 추이">
          <g class="scada-grid-lines">
            <line x1="50" y1="60" x2="860" y2="60" />
            <line x1="50" y1="130" x2="860" y2="130" />
            <line x1="50" y1="200" x2="860" y2="200" />
            <line x1="50" y1="270" x2="860" y2="270" />
          </g>
          <polyline class="scada-line blue-line" points="50,240 130,252 210,230 290,170 370,105 450,82 530,70 610,82 690,116 770,178 860,226" />
          <polyline class="scada-line orange-line" points="50,290 130,282 210,260 290,232 370,214 450,204 530,190 610,178 690,198 770,216 860,236" />
          <polyline class="scada-line cyan-line" points="50,310 130,306 210,302 290,292 370,282 450,274 530,270 610,276 690,284 770,296 860,306" />
          <polyline class="scada-line green-line" points="50,318 130,315 210,306 290,270 370,220 450,205 530,198 610,214 690,252 770,286 860,310" />
          <g class="scada-axis">
            <text x="50" y="330">00:00</text>
            <text x="250" y="330">06:00</text>
            <text x="450" y="330">12:00</text>
            <text x="650" y="330">18:00</text>
            <text x="850" y="330">24:00</text>
          </g>
        </svg>
      </article>

      <aside class="scada-panel">
        <h2>실시간 알림</h2>
        <div class="alert-list">
          <article v-for="alert in scadaAlerts" :key="alert[0]">
            <b>{{ alert[0] }}</b>
            <span>{{ alert[1] }}</span>
          </article>
        </div>
        <h2 class="section-gap">최근 알림 로그</h2>
        <ul class="log-list">
          <li v-for="log in scadaLogs" :key="log[0]"><span>{{ log[0] }}</span>{{ log[1] }}</li>
        </ul>
      </aside>
    </section>

    <section class="scada-main-grid lower">
      <article class="scada-panel wide">
        <h2>에너지 종류별 금일 / 전월 대비 사용량 비교</h2>
        <div class="scada-bars">
          <div><span>전기</span><i style="height: 68%"></i><b>▼ 6.2%</b></div>
          <div><span>가스</span><i class="orange" style="height: 58%"></i><b>▲ 8.0%</b></div>
          <div><span>용수</span><i class="cyan" style="height: 50%"></i><b>▼ 2.7%</b></div>
          <div><span>태양광</span><i class="green" style="height: 74%"></i><b>▲ 15.2%</b></div>
        </div>
      </article>
      <aside class="scada-panel">
        <h2>6개 사업장 요약</h2>
        <div class="site-summary-list">
          <article v-for="site in siteSummary" :key="site.name">
            <b>{{ site.name }}</b>
            <span>{{ site.energy }} kWh</span>
            <em :class="{ warn: site.peak !== '정상' }">{{ site.peak }}</em>
            <strong>{{ site.esg }}</strong>
          </article>
        </div>
      </aside>
    </section>
  </main>

  <main v-else class="detail-shell">
    <aside class="sidebar">
      <button class="logo-button" type="button" @click="goScada">
        <span class="logo-symbol">EE</span>
        <b>EcoEnergy</b>
      </button>

      <nav class="side-nav" aria-label="상세 화면 네비게이션">
        <button
          v-for="item in navItems"
          :key="item.id"
          :class="{ active: activePage === item.id }"
          type="button"
          @click="activePage = item.id"
        >
          <span>{{ item.icon }}</span>
          {{ item.label }}
        </button>
      </nav>

      <div class="side-footer">
        <button type="button" @click="appMode = 'login'">
          <span class="avatar">A</span>
          <span>
            <b>기아 화성</b>
            관리자
          </span>
        </button>
        <button class="collapse-button" type="button" @click="goScada">←</button>
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
          <span class="icon-button">!</span>
          <span class="icon-button">?</span>
        </div>
      </header>

      <section class="filter-card">
        <label>
          사업장
          <select>
            <option v-for="site in sites" :key="site">{{ site }}</option>
          </select>
        </label>
        <label v-if="activePage === 'facility'">
          설비
          <select>
            <option>설비-01 (제작기-01)</option>
            <option>계측기-01</option>
            <option>태양광 설비-01</option>
          </select>
        </label>
        <label v-if="activePage !== 'users'">
          기간
          <input value="2025-05-10 ~ 2025-05-16" readonly />
        </label>
        <button class="icon-action" type="button">↻</button>
        <button class="primary-button compact" type="button">조회</button>
      </section>

      <section v-if="activePage === 'facility'" class="page-stack">
        <div class="two-column">
          <article class="panel chart-panel">
            <div class="panel-title">
              <div>
                <h2>일별 사용량 추이 <small>(전기)</small></h2>
                <p>단위: kWh</p>
              </div>
              <div class="segmented">
                <button class="active" type="button">일</button>
                <button type="button">주</button>
                <button type="button">월</button>
              </div>
            </div>
            <svg class="line-chart" viewBox="0 0 760 280" role="img" aria-label="일별 사용량 추이 차트">
              <g class="chart-grid">
                <line x1="40" y1="40" x2="720" y2="40" />
                <line x1="40" y1="100" x2="720" y2="100" />
                <line x1="40" y1="160" x2="720" y2="160" />
                <line x1="40" y1="220" x2="720" y2="220" />
              </g>
              <polyline class="area-line" points="80,154 180,166 280,132 380,108 480,152 580,126 680,120" />
              <polygon class="area-fill" points="80,154 180,166 280,132 380,108 480,152 580,126 680,120 680,235 80,235" />
              <g v-for="(item, index) in dailyUsage" :key="item.date">
                <circle class="point" :cx="80 + index * 100" :cy="[154,166,132,108,152,126,120][index]" r="6" />
                <text class="point-label" :x="80 + index * 100" :y="[154,166,132,108,152,126,120][index] - 18">{{ item.value.toLocaleString() }}</text>
                <text class="axis-label" :x="80 + index * 100" y="265">{{ item.date }}</text>
              </g>
            </svg>
          </article>

          <article class="panel selected-panel">
            <div class="panel-title inline">
              <h2>선택 설비 정보</h2>
              <button class="light-button" type="button">설비 상세보기 ›</button>
            </div>
            <div class="equipment-head">
              <span class="equipment-icon">▦</span>
              <div>
                <h3>설비-01 (제작기-01)</h3>
                <p>기아 화성 · 생산동 2층</p>
              </div>
              <span class="badge ok">운전 중</span>
            </div>
            <div class="info-grid">
              <div><span>현재 상태</span><b class="green">운전 중</b><small>정상 가동 중</small></div>
              <div><span>오늘 사용량</span><b>2,740</b><small>kWh</small></div>
              <div><span>전일 대비</span><b class="red">+60</b><small>kWh</small></div>
              <div><span>기간 총 사용량</span><b>17,840</b><small>kWh</small></div>
              <div><span>평균 일 사용량</span><b>2,549</b><small>kWh</small></div>
              <div><span>수집 상태</span><b class="green">정상</b><small>최근 수집 14:30</small></div>
            </div>
          </article>
        </div>

        <article class="panel table-panel">
          <div class="panel-title inline">
            <h2>사용량 이력</h2>
            <div class="table-actions">
              <button class="light-button" type="button">엑셀 다운로드</button>
              <button class="light-button" type="button">10개씩 ˅</button>
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>일자</th><th>설비명</th><th>계측기/장치</th><th>에너지원</th><th>사용량 (kWh)</th><th>전일 대비</th><th>전일 대비 (%)</th><th>수집 시간</th><th>상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in usageRows" :key="row[0]">
                <td v-for="(cell, index) in row" :key="index" :class="{ red: String(cell).startsWith('+'), blue: String(cell).startsWith('-') }">
                  <span v-if="index === 8" class="badge ok">{{ cell }}</span>
                  <template v-else>{{ cell }}</template>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'peak'" class="page-stack">
        <div class="kpi-grid">
          <article class="kpi-card"><span>현재 전력</span><b>1,320 <small>kW</small></b><p>전일 동시간 대비 ▼ 5.4%</p></article>
          <article class="kpi-card cyan"><span>피크 사용률</span><b>94<small>%</small></b><p>피크 기준 1,400 kW</p></article>
          <article class="kpi-card"><span>15분 평균 전력</span><b>1,245 <small>kW</small></b><p>전일 동시간 대비 ▼ 3.7%</p></article>
          <article class="kpi-card purple"><span>15분 최대 전력</span><b>1,380 <small>kW</small></b><p>발생 시각 14:15</p></article>
        </div>
        <div class="two-column peak-layout">
          <article class="panel gauge-panel">
            <h2>피크 사용률</h2>
            <div class="gauge">
              <div class="needle"></div>
              <strong>94%</strong>
              <span>피크 기준 1,400 kW</span>
            </div>
          </article>
          <article class="panel">
            <h2>금일 피크 전력 추이</h2>
            <svg class="line-chart compact-chart" viewBox="0 0 760 250">
              <g class="chart-grid">
                <line x1="40" y1="60" x2="720" y2="60" />
                <line x1="40" y1="120" x2="720" y2="120" />
                <line x1="40" y1="180" x2="720" y2="180" />
              </g>
              <line class="limit-line" x1="40" y1="82" x2="720" y2="82" />
              <polyline class="area-line orange" points="40,176 120,170 200,160 280,125 360,96 440,68 520,94 600,130 720,160" />
              <polyline class="area-line" points="40,188 120,182 200,170 280,140 360,112 440,88 520,112 600,148 720,176" />
            </svg>
          </article>
        </div>
        <article class="panel table-panel">
          <h2>피크 발생 이력</h2>
          <table>
            <thead><tr><th>발생 일시</th><th>피크 전력</th><th>피크 사용률</th><th>지속 시간</th><th>비고</th></tr></thead>
            <tbody>
              <tr v-for="row in peakHistory" :key="row[0]"><td v-for="cell in row" :key="cell">{{ cell }}</td></tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'utility'" class="page-stack">
        <div class="kpi-grid">
          <article class="kpi-card gas"><span>가스 금일 사용량</span><b>2,850 <small>Nm³</small></b><p>전일 대비 ▲ 8.1%</p></article>
          <article class="kpi-card gas"><span>가스 적산량</span><b>125,680 <small>Nm³</small></b><p>전일 대비 ▲ 6.4%</p></article>
          <article class="kpi-card cyan"><span>용수 금일 사용량</span><b>128.6 <small>m³</small></b><p>전일 대비 ▼ 2.7%</p></article>
          <article class="kpi-card cyan"><span>용수 적산량</span><b>5,482.1 <small>m³</small></b><p>전일 대비 ▲ 3.2%</p></article>
        </div>
        <div class="two-column">
          <article class="panel">
            <h2>가스 시간대별 사용량</h2>
            <div class="bar-chart orange-bars">
              <span v-for="height in [18,22,30,42,55,68,76,88,72,70,78,90,84,72,62,48,38,30]" :key="height" :style="{ height: `${height}%` }"></span>
            </div>
          </article>
          <article class="panel">
            <h2>용수 시간대별 사용량</h2>
            <div class="bar-chart cyan-bars">
              <span v-for="height in [12,16,25,32,42,50,58,66,72,69,74,78,70,62,50,35,24,16]" :key="height" :style="{ height: `${height}%` }"></span>
            </div>
          </article>
        </div>
        <article class="panel table-panel">
          <h2>계측기 상태</h2>
          <table>
            <thead><tr><th>계측기명</th><th>구분</th><th>현재값</th><th>단위</th><th>상태</th><th>최근 수신</th></tr></thead>
            <tbody>
              <tr v-for="row in meterRows" :key="row[0]">
                <td v-for="(cell, index) in row" :key="index">
                  <span v-if="index === 4" :class="['badge', cell === '주의' ? 'warn' : 'ok']">{{ cell }}</span>
                  <template v-else>{{ cell }}</template>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else-if="activePage === 'esg'" class="page-stack">
        <div class="kpi-grid">
          <article class="kpi-card green"><span>환경 종합 등급</span><b>AAA <small>8.74/10</small></b><p>매우 우수</p></article>
          <article class="kpi-card"><span>온실가스 배출</span><b>6.9 <small>/10</small></b><p>전월 대비 ▲ 0.4</p></article>
          <article class="kpi-card cyan"><span>용수 사용 효율</span><b>7.4 <small>/10</small></b><p>전월 대비 ▲ 0.2</p></article>
          <article class="kpi-card purple"><span>피크전력 점수</span><b>6.3 <small>/10</small></b><p>전월 대비 ▼ 0.1</p></article>
        </div>
        <div class="two-column esg-layout">
          <article class="panel map-card">
            <h2>사업장 환경종합 등급</h2>
            <div class="korea-map">
              <span class="pin p1">기아 광명<br><b>AA</b></span>
              <span class="pin p2">기아 화성<br><b>AAA</b></span>
              <span class="pin p3">현대 울산<br><b>A</b></span>
              <span class="pin p4">현대 아산<br><b>BBB</b></span>
              <span class="pin p5">기아 광주<br><b>A</b></span>
              <span class="pin p6">현대 전주<br><b>BB</b></span>
            </div>
          </article>
          <article class="panel table-panel">
            <h2>사업장 ESG 등급 순위</h2>
            <table>
              <thead><tr><th>순위</th><th>사업장</th><th>등급</th><th>점수</th><th>전월 대비</th></tr></thead>
              <tbody>
                <tr v-for="(site, index) in esgSites" :key="site.name">
                  <td>{{ index + 1 }}</td><td>{{ site.name }}</td><td><b>{{ site.grade }}</b></td><td>{{ site.score }}</td><td :class="{ red: site.diff.startsWith('+'), blue: site.diff.startsWith('-') }">{{ site.diff }}</td>
                </tr>
              </tbody>
            </table>
          </article>
        </div>
      </section>

      <section v-else class="page-stack">
        <div class="kpi-grid">
          <article class="kpi-card"><span>전체 사용자</span><b>24<small>명</small></b><p>등록 계정 기준</p></article>
          <article class="kpi-card green"><span>활성 사용자</span><b>20<small>명</small></b><p>최근 30일 로그인</p></article>
          <article class="kpi-card purple"><span>금일 로그인</span><b>18<small>건</small></b><p>정상 로그인 기준</p></article>
          <article class="kpi-card danger"><span>잠금 계정</span><b>2<small>건</small></b><p>확인 필요</p></article>
        </div>
        <div class="two-column user-layout">
          <article class="panel table-panel">
            <div class="panel-title inline">
              <h2>사용자 목록</h2>
              <input class="search-input" placeholder="검색어 입력" />
            </div>
            <table>
              <thead><tr><th>사용자 ID</th><th>사용자명</th><th>권한</th><th>소속공장</th><th>이메일</th><th>상태</th><th>최종 로그인</th></tr></thead>
              <tbody>
                <tr v-for="row in users" :key="row[0]">
                  <td v-for="(cell, index) in row" :key="index">
                    <span v-if="index === 5" :class="['badge', cell === '비활성' ? 'warn' : 'ok']">{{ cell }}</span>
                    <template v-else>{{ cell }}</template>
                  </td>
                </tr>
              </tbody>
            </table>
          </article>
          <article class="panel form-panel">
            <h2>사용자 등록 / 상세 정보</h2>
            <label>사용자 ID<input value="영문, 숫자 4~20자" /></label>
            <label>비밀번호<input value="영문/숫자/특수문자 조합" /></label>
            <label>사용자명<input value="사용자명을 입력하세요" /></label>
            <label>권한<select><option>운영자</option><option>관리자</option><option>조회자</option></select></label>
            <div class="form-actions">
              <button class="primary-button compact" type="button">등록</button>
              <button class="light-button" type="button">수정</button>
              <button class="danger-button" type="button">삭제</button>
            </div>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>
