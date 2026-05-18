<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  Activity,
  AlertTriangle,
  Bolt,
  Cloud,
  Droplets,
  Factory,
  Flame,
  Gauge,
  History,
  Leaf,
  ListOrdered,
  RadioReceiver,
  Search,
  SunMedium,
  Zap,
} from 'lucide-vue-next'
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
const selectedUtilityDate = ref(formatDateInput(new Date()))
const selectedEsgFrom = ref(formatDateInput(new Date()))
const selectedEsgTo = ref(formatDateInput(new Date()))
const syncingSelection = ref(false)
let energySocket = null
let energySocketReconnectTimer = null
let peakRefreshTimer = null
let utilityRefreshTimer = null
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
  utilityDashboard: null,
  esgDashboard: null,
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

function utilityBarHeight(value, maxValue) {
  return Math.max(8, Math.round((Number(value || 0) / maxValue) * 100))
}

function meterTypeLabel(type) {
  const labels = {
    GAS: '가스',
    WATER: '용수',
  }
  return labels[type] || type || '-'
}

function meterStatusLabel(status) {
  const labels = {
    NORMAL: '정상',
    DELAY: '지연',
    NONE: '미수신',
  }
  return labels[status] || status || '-'
}

function meterStatusClass(status) {
  if (status === 'NORMAL') {
    return 'ok'
  }
  if (status === 'DELAY') {
    return 'warn'
  }
  return 'danger'
}

function utilityPatternStyle(rate, tone) {
  const normalizedRate = Math.max(0, Math.min(Number(rate || 0), 100))
  const opacity = 0.1 + normalizedRate / 100 * 0.82
  const color = tone === 'gas' ? '255, 138, 0' : '0, 184, 217'
  const border = tone === 'gas' ? '255, 138, 0' : '0, 184, 217'
  return {
    '--pattern-color': color,
    '--pattern-border': border,
    '--pattern-opacity': opacity.toFixed(2),
    '--pattern-shadow': `${Math.round(normalizedRate / 100 * 18)}px`,
  }
}

function esgMetricIcon(key) {
  return {
    carbon: Cloud,
    water: Droplets,
    solar: SunMedium,
    peak: Gauge,
    electricity: Bolt,
    gas: Flame,
  }[key] || Activity
}

function esgMetricTone(key) {
  return {
    carbon: 'carbon',
    water: 'water',
    solar: 'solar',
    peak: 'peak',
    electricity: 'electric',
    gas: 'gas',
  }[key] || 'carbon'
}

function esgTrendClass(value, inverse = false) {
  const numericValue = Number(value || 0)
  if (numericValue === 0) {
    return 'flat'
  }
  const good = inverse ? numericValue < 0 : numericValue > 0
  return good ? 'down' : 'up'
}

function esgPlantMapStyle(index) {
  const position = esgMapPositions[index % esgMapPositions.length]
  return {
    left: `${position.left}%`,
    top: `${position.top}%`,
  }
}

function esgBarHeight(score) {
  return `${Math.max(8, Math.min(Number(score || 0) * 10, 100))}%`
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
const utilityMetrics = computed(() => state.utilityDashboard?.metrics || {})
const utilityHourlyUsage = computed(() =>
  (state.utilityDashboard?.hourlyUsage || []).map((point) => ({
    measuredAt: point.measuredAt || point.measured_at,
    gasUsageM3: Number(point.gasUsageM3 || point.gas_usage_m3 || 0),
    waterUsageTon: Number(point.waterUsageTon || point.water_usage_ton || 0),
  })),
)
const utilityMeterStatuses = computed(() => state.utilityDashboard?.meterStatuses || [])
const utilityPatterns = computed(() => {
  const patternsByDate = new Map(
    (state.utilityDashboard?.patterns || []).map((pattern) => [
      formatDate(pattern.usageDate || pattern.usage_date),
      {
        usageDate: pattern.usageDate || pattern.usage_date,
        gasUsageM3: Number(pattern.gasUsageM3 || pattern.gas_usage_m3 || 0),
        waterUsageTon: Number(pattern.waterUsageTon || pattern.water_usage_ton || 0),
        gasUsageRate: Number(pattern.gasUsageRate || pattern.gas_usage_rate || 0),
        waterUsageRate: Number(pattern.waterUsageRate || pattern.water_usage_rate || 0),
      },
    ]),
  )
  const endDate = new Date(`${selectedUtilityDate.value || formatDateInput(new Date())}T00:00:00`)
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(endDate)
    date.setDate(endDate.getDate() - 6 + index)
    const dateKey = formatDateInput(date)
    return (
      patternsByDate.get(dateKey) || {
        usageDate: dateKey,
        gasUsageM3: 0,
        waterUsageTon: 0,
        gasUsageRate: 0,
        waterUsageRate: 0,
      }
    )
  })
})
const utilityGasMax = computed(() => Math.max(1, ...utilityHourlyUsage.value.map((point) => point.gasUsageM3)))
const utilityWaterMax = computed(() => Math.max(1, ...utilityHourlyUsage.value.map((point) => point.waterUsageTon)))
const esgDashboard = computed(() => state.esgDashboard || {})
const esgPlants = computed(() => esgDashboard.value.plants || [])
const selectedEsgPlant = computed(
  () =>
    esgPlants.value.find((plant) => Number(plant.plantId) === Number(selectedPlantId.value)) ||
    esgDashboard.value.selectedPlant ||
    esgPlants.value[0] ||
    null,
)
const esgMetrics = computed(() => {
  const plant = selectedEsgPlant.value
  if (!plant) {
    return []
  }
  return [
    {
      key: 'carbon',
      label: '탄소배출',
      score: plant.carbonScore,
      unit: 'tCO2e',
      value: plant.carbonEmission,
      changeRate: plant.changeRate,
      sourceLabel: '전력/가스 사용량',
    },
    {
      key: 'water',
      label: '용수사용',
      score: plant.waterScore,
      unit: 'ton',
      value: plant.waterUsage,
      changeRate: plant.waterChangeRate,
      sourceLabel: '용수 계측값',
    },
    {
      key: 'solar',
      label: '태양광발전',
      score: plant.solarScore,
      unit: 'kWh',
      value: plant.solarGeneration,
      changeRate: plant.solarChangeRate,
      sourceLabel: '태양광 계측값',
    },
    {
      key: 'peak',
      label: '피크전력',
      score: plant.peakScore,
      unit: 'kW',
      value: plant.intervalMaxPeakKw,
      changeRate: plant.peakReductionRate,
      sourceLabel: '15분 최대전력',
    },
    {
      key: 'electricity',
      label: '전력사용',
      score: plant.electricityScore,
      unit: 'kWh',
      value: plant.electricityKwh,
      changeRate: 0,
      sourceLabel: '전력 계측값',
    },
    {
      key: 'gas',
      label: '가스사용',
      score: plant.gasScore,
      unit: 'm3',
      value: plant.gasM3,
      changeRate: 0,
      sourceLabel: '가스 계측값',
    },
  ]
})
const esgAlerts = computed(() => esgDashboard.value.alerts || [])
const esgLogic = computed(() => esgDashboard.value.logic || {})
const esgCompareLabels = ['탄소', '용수', '태양광', '피크']
const esgCompareRows = computed(() =>
  esgPlants.value.map((plant) => ({
    plantId: plant.plantId,
    plantName: plant.plantName,
    scores: [plant.carbonScore, plant.waterScore, plant.solarScore, plant.peakScore].map((score) => Number(score || 0)),
  })),
)
const esgMapPositions = [
  { left: 24, top: 20 },
  { left: 58, top: 26 },
  { left: 42, top: 44 },
  { left: 70, top: 54 },
  { left: 31, top: 66 },
  { left: 56, top: 74 },
]

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
    if (error.status === 401 || error.message.includes('인증') || error.message.includes('Unauthorized')) {
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
    const [me, plants] = await Promise.all([api.me(), api.plants()])
    state.me = me
    state.plants = plants
    syncingSelection.value = true
    selectedPlantId.value = selectedPlantId.value || me.plantId || plants[0]?.id || null
    syncingSelection.value = false
    if (appMode.value === 'detail' && activePage.value !== 'facility') {
      await loadActivePageData()
      return
    }
    await loadPlantData()
  })
}

async function loadPlantData() {
  if (!selectedPlantId.value) {
    return
  }

  const [overview, facilities] = await Promise.all([
    api.dashboard(selectedPlantId.value),
    api.facilities(selectedPlantId.value),
  ])

  state.overview = overview
  state.facilities = facilities
  syncingSelection.value = true
  selectedFacilityId.value = facilities[0]?.id || null
  syncingSelection.value = false

  await loadActivePageData()
}

async function loadActivePageData() {
  if (appMode.value === 'login' || !selectedPlantId.value) {
    return
  }

  if (appMode.value === 'scada') {
    await loadOverviewEnergyData()
    return
  }

  if (activePage.value === 'facility') {
    await loadEnergyData()
    return
  }

  if (activePage.value === 'peak') {
    await loadPeakDashboard()
    return
  }

  if (activePage.value === 'utility') {
    await loadUtilityDashboard()
    return
  }

  if (activePage.value === 'esg') {
    await loadEsgDashboard()
    return
  }

  if (activePage.value === 'users') {
    const users = await api.users({ page: 0, size: 20 })
    state.users = users.items || []
    return
  }

  if (activePage.value === 'alarms') {
    state.alarms = await api.alarms({ plantId: selectedPlantId.value, limit: 20 })
  }
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

async function loadOverviewEnergyData() {
  if (!selectedPlantId.value) {
    return
  }

  const summaryParams = {
    plantId: selectedPlantId.value,
    facilityId: selectedFacilityId.value || undefined,
  }

  const [summaries, latestEnergy] = await Promise.all([
    api.energySummaries(summaryParams),
    selectedFacilityId.value
      ? api.latestEnergy(selectedPlantId.value, selectedFacilityId.value).catch(() => null)
      : Promise.resolve(null),
  ])

  state.summaries = summaries
  state.measurements = []
  state.latestEnergy = latestEnergy
  applySummaryDateRange(summaries)
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
    ...dateRangeParams(selectedDateFrom.value, selectedDateTo.value),
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

async function loadUtilityDashboard() {
  if (!selectedPlantId.value) {
    state.utilityDashboard = null
    return
  }

  state.utilityDashboard = await api.utilityDashboard({
    plantId: selectedPlantId.value,
    date: selectedUtilityDate.value || undefined,
  })
}

async function loadEsgDashboard() {
  state.esgDashboard = await api.esgEnvironmentDashboard({
    plantId: selectedPlantId.value || undefined,
    ...dateRangeParams(selectedEsgFrom.value, selectedEsgTo.value),
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
  const configuredBaseUrl = import.meta.env.VITE_WS_BASE_URL || import.meta.env.VITE_API_BASE_URL || ''
  if (configuredBaseUrl) {
    const url = new URL(configuredBaseUrl, window.location.origin)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = '/ws/energy'
    url.search = ''
    url.hash = ''
    return url.toString()
  }

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
    if (plantId === Number(selectedPlantId.value) && activePage.value === 'utility') {
      scheduleUtilityRefresh()
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
  if (appMode.value === 'detail' && activePage.value !== 'facility') {
    await run(loadActivePageData)
    return
  }
  await run(loadPlantData)
}

function schedulePeakRefresh() {
  window.clearTimeout(peakRefreshTimer)
  peakRefreshTimer = window.setTimeout(() => {
    loadPeakDashboard().catch(() => {})
  }, 500)
}

function scheduleUtilityRefresh() {
  window.clearTimeout(utilityRefreshTimer)
  utilityRefreshTimer = window.setTimeout(() => {
    loadUtilityDashboard().catch(() => {})
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

function dateRangeParams(from, to) {
  if (from && to && from > to) {
    return { from: to, to: from }
  }
  return {
    from: from || undefined,
    to: to || undefined,
  }
}

watch(selectedPlantId, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    if (activePage.value === 'esg') {
      if (!esgPlants.value.length) {
        run(loadEsgDashboard)
      }
      return
    }
    if (appMode.value === 'detail' && activePage.value !== 'facility') {
      run(loadActivePageData)
      return
    }
    run(loadPlantData)
  }
})

watch(activePage, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadActivePageData)
  }
})

watch(selectedFacilityId, () => {
  if (appMode.value !== 'login' && !syncingSelection.value) {
    run(loadActivePageData)
  }
})

watch(selectedEnergyType, () => {
  if (appMode.value !== 'login' && activePage.value === 'facility' && !syncingSelection.value) {
    run(loadFacilityDetail)
  }
})

watch([selectedDateFrom, selectedDateTo], () => {
  if (appMode.value !== 'login' && activePage.value === 'facility' && !syncingSelection.value) {
    run(loadFacilityDetail)
  }
})

watch(selectedPeakDate, () => {
  if (appMode.value !== 'login' && activePage.value === 'peak' && !syncingSelection.value) {
    run(loadPeakDashboard)
  }
})

watch(selectedUtilityDate, () => {
  if (appMode.value !== 'login' && activePage.value === 'utility' && !syncingSelection.value) {
    run(loadUtilityDashboard)
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
  window.clearTimeout(utilityRefreshTimer)
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

      <section v-if="activePage !== 'esg'" class="filter-card">
        <label>
          사업장
          <select v-model.number="selectedPlantId">
            <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
          </select>
        </label>
        <label v-if="!['peak', 'utility'].includes(activePage)">
          설비
          <select v-model.number="selectedFacilityId">
            <option :value="null">전체 설비</option>
            <option v-for="facility in state.facilities" :key="facility.id" :value="facility.id">
              {{ facility.name }}
            </option>
          </select>
        </label>
        <label v-if="!['peak', 'utility'].includes(activePage)">
          에너지 종류
          <select v-model="selectedEnergyType">
            <option v-for="option in energyTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label v-if="!['peak', 'utility'].includes(activePage)">
          시작일
          <input v-model="selectedDateFrom" type="date" />
        </label>
        <label v-if="!['peak', 'utility'].includes(activePage)">
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

      <section v-else-if="activePage === 'utility'" class="page-stack utility-monitor-page">
        <section class="utility-filter-row">
          <label>
            조회일
            <input v-model="selectedUtilityDate" type="date" />
          </label>
          <button class="primary-button compact" type="button" @click="run(loadUtilityDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">{{ state.latestEnergy ? '실시간 수신 중' : '금일 데이터 조회' }}</span>
        </section>

        <section class="utility-kpi-grid">
          <article class="utility-kpi-card gas">
            <span class="utility-card-icon"><Flame :size="23" /></span>
            <div>
              <p>가스 금일 사용량</p>
              <b>{{ formatNumber(utilityMetrics.gasUsageM3) }}<small> m3</small></b>
              <em :class="trendClass(utilityMetrics.gasChangeRate)">
                전일 대비 {{ trendPrefix(utilityMetrics.gasChangeRate) }}{{ formatNumber(utilityMetrics.gasChangeRate) }}%
              </em>
            </div>
          </article>
          <article class="utility-kpi-card gas">
            <span class="utility-card-icon"><Gauge :size="23" /></span>
            <div>
              <p>가스 적산량</p>
              <b>{{ formatNumber(utilityMetrics.gasTotalM3) }}<small> m3</small></b>
              <em>최신 누적 계량값</em>
            </div>
          </article>
          <article class="utility-kpi-card water">
            <span class="utility-card-icon"><Droplets :size="23" /></span>
            <div>
              <p>용수 금일 사용량</p>
              <b>{{ formatNumber(utilityMetrics.waterUsageTon) }}<small> ton</small></b>
              <em :class="trendClass(utilityMetrics.waterChangeRate)">
                전일 대비 {{ trendPrefix(utilityMetrics.waterChangeRate) }}{{ formatNumber(utilityMetrics.waterChangeRate) }}%
              </em>
            </div>
          </article>
          <article class="utility-kpi-card water">
            <span class="utility-card-icon"><Gauge :size="23" /></span>
            <div>
              <p>용수 적산량</p>
              <b>{{ formatNumber(utilityMetrics.waterTotalTon) }}<small> ton</small></b>
              <em>최신 누적 계량값</em>
            </div>
          </article>
        </section>

        <section class="utility-chart-grid">
          <article class="panel utility-chart-panel gas">
            <div class="panel-title inline">
              <h2>가스 시간대별 사용량</h2>
              <span>단위: m3</span>
            </div>
            <div class="utility-bar-chart">
              <div v-for="point in utilityHourlyUsage" :key="`gas-${point.measuredAt}`" class="utility-bar">
                <b>{{ formatNumber(point.gasUsageM3) }}</b>
                <i :style="{ height: `${utilityBarHeight(point.gasUsageM3, utilityGasMax)}%` }"></i>
                <span>{{ formatTime(point.measuredAt) }}</span>
              </div>
            </div>
          </article>

          <article class="panel utility-chart-panel water">
            <div class="panel-title inline">
              <h2>용수 시간대별 사용량</h2>
              <span>단위: ton</span>
            </div>
            <div class="utility-bar-chart">
              <div v-for="point in utilityHourlyUsage" :key="`water-${point.measuredAt}`" class="utility-bar">
                <b>{{ formatNumber(point.waterUsageTon) }}</b>
                <i :style="{ height: `${utilityBarHeight(point.waterUsageTon, utilityWaterMax)}%` }"></i>
                <span>{{ formatTime(point.measuredAt) }}</span>
              </div>
            </div>
          </article>
        </section>

        <section class="utility-bottom-grid">
          <article class="panel table-panel utility-meter-panel">
            <div class="panel-title inline">
              <h2>계측기별 상태 조회</h2>
              <RadioReceiver :size="20" />
            </div>
            <table>
              <thead>
                <tr>
                  <th>계측기명</th>
                  <th>구분</th>
                  <th>현재값</th>
                  <th>단위</th>
                  <th>최근 수신</th>
                  <th>통신 상태</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="meter in utilityMeterStatuses" :key="`${meter.meterType}-${meter.facilityId}`">
                  <td>{{ meter.meterName || meter.facilityName }}</td>
                  <td :class="meter.meterType === 'GAS' ? 'utility-gas-text' : 'utility-water-text'">
                    {{ meterTypeLabel(meter.meterType) }}
                  </td>
                  <td>{{ formatNumber(meter.currentValue) }}</td>
                  <td>{{ meter.unit }}</td>
                  <td>{{ formatDateTime(meter.lastReceivedAt) }}</td>
                  <td>
                    <span :class="['badge', meterStatusClass(meter.communicationStatus)]">
                      {{ meterStatusLabel(meter.communicationStatus) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </article>

          <article class="panel utility-pattern-panel">
            <div class="panel-title inline">
              <h2>사용 패턴 분석</h2>
              <span>최근 7일</span>
            </div>
            <div class="utility-pattern-grid">
              <span class="utility-pattern-label gas"><Flame :size="18" />가스</span>
              <div
                v-for="pattern in utilityPatterns"
                :key="`gas-pattern-${pattern.usageDate}`"
                class="utility-pattern-cell gas"
                :style="utilityPatternStyle(pattern.gasUsageRate, 'gas')"
              >
                <b>{{ formatDate(pattern.usageDate).slice(5) }}</b>
                <strong>{{ formatNumber(pattern.gasUsageRate, 0) }}%</strong>
              </div>
              <span class="utility-pattern-label water"><Droplets :size="18" />용수</span>
              <div
                v-for="pattern in utilityPatterns"
                :key="`water-pattern-${pattern.usageDate}`"
                class="utility-pattern-cell water"
                :style="utilityPatternStyle(pattern.waterUsageRate, 'water')"
              >
                <b>{{ formatDate(pattern.usageDate).slice(5) }}</b>
                <strong>{{ formatNumber(pattern.waterUsageRate, 0) }}%</strong>
              </div>
            </div>
            <div class="utility-pattern-summary">
              <span>낮음</span>
              <i></i>
              <span>높음</span>
            </div>
          </article>
        </section>
      </section>

      <section v-else-if="activePage === 'esg'" class="page-stack esg-performance-page">
        <section class="esg-filter-row">
          <label>
            사업장
            <select v-model.number="selectedPlantId">
              <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
            </select>
          </label>
          <label>
            시작일
            <input v-model="selectedEsgFrom" type="date" />
          </label>
          <label>
            종료일
            <input v-model="selectedEsgTo" type="date" />
          </label>
          <button class="primary-button compact" type="button" @click="run(loadEsgDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">내부 환경 점수 0-10</span>
        </section>

        <section class="esg-kpi-grid">
          <article class="esg-grade-card">
            <span><Leaf :size="24" /></span>
            <div>
              <p>환경 종합 등급</p>
              <b>{{ selectedEsgPlant?.grade || '-' }}</b>
              <em>{{ formatNumber(selectedEsgPlant?.totalScore) }}/10</em>
            </div>
          </article>
          <article v-for="metric in esgMetrics.slice(0, 4)" :key="metric.key" :class="['esg-kpi-card', esgMetricTone(metric.key)]">
            <span><component :is="esgMetricIcon(metric.key)" :size="22" /></span>
            <div>
              <p>{{ metric.label }}</p>
              <b>{{ formatNumber(metric.score) }}<small>/10</small></b>
              <em :class="esgTrendClass(metric.changeRate, metric.key !== 'solar')">
                전월 대비 {{ trendPrefix(metric.changeRate) }}{{ formatNumber(metric.changeRate) }}%
              </em>
            </div>
          </article>
        </section>

        <section class="esg-main-grid">
          <article class="panel esg-map-panel">
            <div class="panel-title inline">
              <h2>사업장 환경 등급 현황</h2>
              <span>{{ selectedEsgFrom }} - {{ selectedEsgTo }}</span>
            </div>
            <div class="esg-map">
              <div
                v-for="(plant, index) in esgPlants"
                :key="plant.plantId"
                class="esg-map-pin"
                :class="{ active: plant.plantId === selectedEsgPlant?.plantId }"
                :style="esgPlantMapStyle(index)"
              >
                <Factory :size="17" />
                <strong>{{ plant.grade }}</strong>
                <span>{{ plant.plantName }}</span>
                <b>{{ formatNumber(plant.totalScore) }}</b>
              </div>
            </div>
          </article>

          <article class="panel esg-ranking-panel">
            <div class="panel-title inline">
              <h2>사업장 ESG 등급 순위</h2>
              <ListOrdered :size="20" />
            </div>
            <div class="esg-ranking-list">
              <button
                v-for="plant in esgPlants"
                :key="plant.plantId"
                type="button"
                :class="{ active: plant.plantId === selectedEsgPlant?.plantId }"
                @click="selectedPlantId = plant.plantId"
              >
                <b>{{ plant.rank }}</b>
                <span>{{ plant.plantName }}</span>
                <strong>{{ plant.grade }}</strong>
                <em>{{ formatNumber(plant.totalScore) }}</em>
                <i :class="trendClass(plant.changeRate)">
                  {{ trendPrefix(plant.changeRate) }}{{ formatNumber(plant.changeRate) }}%
                </i>
              </button>
            </div>
          </article>
        </section>

        <section class="esg-bottom-grid">
          <article class="panel esg-compare-panel">
            <div class="panel-title inline">
              <h2>핵심 환경 항목 비교</h2>
              <span>0-10 점수</span>
            </div>
            <div class="esg-grouped-chart">
              <div v-for="plant in esgCompareRows" :key="plant.plantId" class="esg-chart-group">
                <div class="esg-chart-bars">
                  <i
                    v-for="(score, index) in plant.scores"
                    :key="`${plant.plantId}-${index}`"
                    :class="`tone-${index}`"
                    :style="{ height: esgBarHeight(score) }"
                  ></i>
                </div>
                <span>{{ plant.plantName }}</span>
              </div>
            </div>
            <div class="esg-chart-legend">
              <span v-for="(label, index) in esgCompareLabels" :key="label"><i :class="`tone-${index}`"></i>{{ label }}</span>
            </div>
          </article>

          <article class="panel esg-detail-panel">
            <div class="panel-title inline">
              <h2>선택 사업장 상세 분석</h2>
              <span>{{ selectedEsgPlant?.plantName || '-' }}</span>
            </div>
            <div class="esg-detail-list">
              <article v-for="metric in esgMetrics" :key="`detail-${metric.key}`">
                <span :class="esgMetricTone(metric.key)"><component :is="esgMetricIcon(metric.key)" :size="18" /></span>
                <div>
                  <strong>{{ metric.label }}</strong>
                  <small>{{ metric.sourceLabel }}</small>
                </div>
                <b>{{ formatNumber(metric.value) }} {{ metric.unit }}</b>
                <em>{{ formatNumber(metric.score) }}/10</em>
              </article>
            </div>
          </article>
        </section>

        <section class="esg-bottom-grid">
          <article class="panel esg-alert-panel">
            <div class="panel-title inline">
              <h2>운영 알림</h2>
              <AlertTriangle :size="20" />
            </div>
            <div class="esg-alert-list">
              <article v-for="alert in esgAlerts" :key="`${alert.plantId}-${alert.title}`" :class="alert.level">
                <b>{{ alert.title }}</b>
                <span>{{ alert.plantName }} · {{ alert.message }}</span>
              </article>
              <article v-if="!esgAlerts.length" class="ok">
                <b>특이 알림 없음</b>
                <span>급증, 피크 초과, 데이터 누락 조건이 감지되지 않았습니다.</span>
              </article>
            </div>
          </article>

          <article class="panel esg-logic-panel">
            <div class="panel-title inline">
              <h2>등급 산정 로직</h2>
              <span>내부 기준</span>
            </div>
            <div class="esg-logic-list">
              <p>{{ esgLogic.normalization }}</p>
              <p>{{ esgLogic.weight }}</p>
              <p>{{ esgLogic.gradeMapping }}</p>
            </div>
          </article>
        </section>
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
