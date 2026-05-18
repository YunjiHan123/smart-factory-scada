<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  Activity,
  AlertTriangle,
  Bolt,
  Bot,
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
  Send,
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
const selectedFacilityLine = ref('PRESS')
const selectedFacilityDate = ref(formatDateInput(new Date()))
const selectedEnergyType = ref('ELECTRICITY')
const selectedDateFrom = ref('')
const selectedDateTo = ref('')
const selectedPeakDate = ref(formatDateInput(new Date()))
const selectedPeakPeriod = ref('DAY')
const activePeakView = ref('comparison')
const selectedUtilityDate = ref(formatDateInput(new Date()))
const selectedUtilityPeriod = ref('DAY')
const activeUtilityView = ref('comparison')
const utilityMeterSearch = ref('')
const utilityTooltip = ref(null)
const chatbotQuestion = ref('')
const chatbotSending = ref(false)
const selectedEsgMonth = ref(formatMonthInput(new Date()))
const selectedEsgFrom = ref(formatMonthStartInput(new Date()))
const selectedEsgTo = ref(formatMonthEndInput(new Date()))
const syncingSelection = ref(false)
const realtimeNow = ref(Date.now())
let energySocket = null
let energySocketReconnectTimer = null
let energyPollTimer = null
let realtimeTickTimer = null
let latestEnergyPollInFlight = false
let latestEnergyPollRetryAt = 0
let latestEnergyPollFailureCount = 0
let peakRefreshTimer = null
let utilityRefreshTimer = null
let lastPeakRefreshAt = 0
let lastUtilityRefreshAt = 0
let lastAlarmRefreshAt = 0
const LIVE_SERIES_LIMIT = 120
const LIVE_STALE_MS = 5000
const LIVE_POLL_MS = 1000
const LIVE_DASHBOARD_REFRESH_MS = 10000
const liveEnergyByFacility = reactive(new Map())
const liveEnergyBaselineByFacility = reactive(new Map())
const liveEnergySeriesByFacility = reactive(new Map())
const liveEnergySeenAtByFacility = reactive(new Map())
const nowLabel = computed(() =>
  new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(realtimeNow.value)),
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
  facilityLineUsages: [],
  alarms: [],
  esgScores: [],
  users: [],
  chatbotMessages: [],
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
  { id: 'chatbot', label: 'AI 상담', icon: 'AI' },
  { id: 'users', label: '사용자 관리', icon: 'M' },
  { id: 'alarms', label: '알람', icon: 'A' },
]

const validRoutes = ['facility', 'peak', 'utility', 'esg', 'chatbot', 'users', 'alarms']
const facilityLineOptions = [
  { value: 'PRESS', label: '프레스' },
  { value: 'BODY', label: '차체' },
  { value: 'ASSEMBLY', label: '의장' },
  { value: 'PAINT', label: '도장' },
]
const peakPeriodOptions = [
  { value: 'DAY', label: '일' },
  { value: 'WEEK', label: '주' },
  { value: 'MONTH', label: '월' },
]
const peakViewOptions = [
  { value: 'comparison', label: '공장별 비교' },
  { value: 'detail', label: '공장 상세 추이' },
  { value: 'anomaly', label: '이상 날짜' },
]
const utilityPeriodOptions = peakPeriodOptions
const utilityViewOptions = [
  { value: 'comparison', label: '공장별 비교' },
  { value: 'detail', label: '공장 상세 추이' },
  { value: 'meters', label: '계측기 상태' },
]
const equipmentProcessNames = {
  1: '메인 프레스기',
  2: '블랭킹 프레스',
  3: '코일 피더',
  4: '금형 교환기',
  5: '유압 펌프',
  6: '배출 컨베이어',
  7: '용접 로봇',
  8: '지그 시스템',
  9: '이송 컨베이어',
  10: '스폿 용접기',
  11: '검사 카메라',
  12: '보정 장치',
  13: '조립 로봇',
  14: '체결 토크 장치',
  15: '부품 공급 장치',
  16: '도어 장착 장치',
  17: '시트 장착 리프트',
  18: '최종 검사기',
  19: '전처리 탱크',
  20: '분사 로봇',
  21: '건조 오븐',
  22: '배기 처리 설비',
  23: '도료 공급 펌프',
  24: '도막 검사기',
}

const activeMeta = computed(() => {
  const meta = {
    facility: ['설비별 에너지 현황', '사업장, 설비 라인, 조회일 기준의 전기 사용량을 확인합니다.'],
    peak: ['피크 전력 현황', '사업장별 피크 전력과 기준 초과 이력을 확인합니다.'],
    utility: ['가스/용수 모니터링', '가스와 용수 사용량을 시간대와 계측기 단위로 확인합니다.'],
    esg: ['ESG 평가 지표', '사업장별 월간 ESG 점수와 상세 지표를 확인합니다.'],
    chatbot: ['AI 상담', '선택한 사업장의 에너지, ESG, 알람 데이터를 기준으로 질문합니다.'],
    users: ['사용자 관리', '사용자 계정과 권한 상태를 확인합니다.'],
    alarms: ['알람 관리', '발생 알람과 처리 상태를 확인합니다.'],
  }
  const [title, description] = meta[activePage.value] || meta.facility
  return { title, description }
})

const selectedPlant = computed(() =>
  state.plants.find((plant) => Number(plant.id) === Number(selectedPlantId.value)),
)
const chatbotMessagesChronological = computed(() => [...state.chatbotMessages].reverse())
const chatbotSuggestedQuestions = [
  '오늘 전력 사용량과 ESG 상태를 요약해줘',
  '피크 전력 위험이 있는지 알려줘',
  '용수와 가스 사용량에서 확인할 점을 알려줘',
]
const selectedFacility = computed(() =>
  state.facilities.find((facility) => Number(facility.id) === Number(selectedFacilityId.value)),
)
const selectedFacilityLineMeta = computed(
  () => facilityLineOptions.find((option) => option.value === selectedFacilityLine.value) || facilityLineOptions[0],
)
const latestSummary = computed(() => state.overview?.latestEnergySummary || state.summaries.at(-1) || null)
const latestEsg = computed(() => state.overview?.latestEsgScore || state.esgScores[0] || null)
const recentSummaries = computed(() => state.summaries.slice(-8))
const liveEnergyEntries = computed(() => Array.from(liveEnergyByFacility.values()))
const selectedFacilityLiveEnergy = computed(() => {
  if (!selectedPlantId.value || !selectedFacilityId.value) {
    return null
  }
  return liveEnergyByFacility.get(energyKey(selectedPlantId.value, selectedFacilityId.value)) || null
})
const selectedPlantLiveEnergy = computed(() => {
  const plantId = Number(selectedPlantId.value)
  if (!Number.isFinite(plantId)) {
    return null
  }

  const rows = liveEnergyEntries.value.filter((row) =>
    Number(row.plantId) === plantId && isLineFacilityId(row.facilityId, plantId),
  )
  if (!rows.length) {
    return null
  }

  const latestRow = rows.reduce((latest, row) => {
    const latestTime = Date.parse(latest?.measuredAt || '')
    const rowTime = Date.parse(row.measuredAt || '')
    return rowTime > latestTime ? row : latest
  }, rows[0])

  return rows.reduce(
    (sum, row) => ({
      plantId,
      facilityId: null,
      measuredAt: latestRow?.measuredAt || sum.measuredAt,
      electricityKwh: sum.electricityKwh + metricNumber(row, 'electricityKwh', 'electricity_kwh'),
      gasM3: sum.gasM3 + metricNumber(row, 'gasM3', 'gas_m3'),
      waterTon: sum.waterTon + metricNumber(row, 'waterTon', 'water_ton'),
      solarKwh: sum.solarKwh + metricNumber(row, 'solarKwh', 'solar_kwh'),
      peakKw: sum.peakKw + metricNumber(row, 'peakKw', 'peak_kw'),
    }),
    {
      plantId,
      facilityId: null,
      measuredAt: latestRow?.measuredAt || null,
      electricityKwh: 0,
      gasM3: 0,
      waterTon: 0,
      solarKwh: 0,
      peakKw: 0,
    },
  )
})
const selectedPlantDailyLiveUsage = computed(() => {
  const plantId = Number(selectedPlantId.value)
  if (!Number.isFinite(plantId)) {
    return { electricityKwh: 0, gasM3: 0, waterTon: 0, solarKwh: 0 }
  }

  return liveEnergyEntries.value
    .filter((row) => Number(row.plantId) === plantId && isLineFacilityId(row.facilityId, plantId))
    .reduce(
      (sum, row) => {
        const baseline = liveEnergyBaselineByFacility.get(energyKey(row.plantId, row.facilityId))
        return {
          electricityKwh: sum.electricityKwh + Number(dailyLiveUsage(row, baseline, 'electricityKwh', 'electricity_kwh') || 0),
          gasM3: sum.gasM3 + Number(dailyLiveUsage(row, baseline, 'gasM3', 'gas_m3') || 0),
          waterTon: sum.waterTon + Number(dailyLiveUsage(row, baseline, 'waterTon', 'water_ton') || 0),
          solarKwh: sum.solarKwh + Number(dailyLiveUsage(row, baseline, 'solarKwh', 'solar_kwh') || 0),
        }
      },
      { electricityKwh: 0, gasM3: 0, waterTon: 0, solarKwh: 0 },
    )
})
const selectedPlantRealtimeElapsedSeconds = computed(() => {
  const plantId = Number(selectedPlantId.value)
  if (!Number.isFinite(plantId)) {
    return 0
  }

  const seenAtValues = liveEnergyEntries.value
    .filter((row) => Number(row.plantId) === plantId && isLineFacilityId(row.facilityId, plantId))
    .map((row) => liveEnergySeenAtByFacility.get(energyKey(row.plantId, row.facilityId)))
    .filter((seenAt) => Number.isFinite(seenAt))
  if (!seenAtValues.length) {
    return 0
  }

  return Math.max(0, (realtimeNow.value - Math.max(...seenAtValues)) / 1000)
})
const selectedLiveEnergy = computed(() =>
  selectedFacilityId.value ? selectedFacilityLiveEnergy.value : selectedPlantLiveEnergy.value,
)
const liveEnergySource = computed(() => selectedLiveEnergy.value || state.latestEnergy || latestSummary.value || null)
const liveEnergyFresh = computed(() => {
  const measuredAt = metricValue(liveEnergySource.value, 'measuredAt', 'measured_at')
  return measuredAt ? Date.now() - Date.parse(measuredAt) <= LIVE_STALE_MS : false
})
const latestMeasuredAt = computed(() => metricValue(liveEnergySource.value, 'measuredAt', 'measured_at'))
const selectedEnergyMeta = computed(
  () => energyTypeOptions.find((option) => option.value === selectedEnergyType.value) || energyTypeOptions[0],
)
const selectedEnergyMetricKeys = computed(() => energyMetricKeys(selectedEnergyType.value))
const selectedEnergyPrecision = computed(() => {
  if (selectedEnergyType.value === 'GAS' || selectedEnergyType.value === 'WATER') {
    return 2
  }
  return 1
})
const isAdminUser = computed(() => state.me?.role === 'ADMIN')
const canViewAllPlants = computed(() => isAdminUser.value)
const assignedPlantId = computed(() => {
  const plantId = Number(state.me?.plantId)
  return Number.isFinite(plantId) && plantId > 0 ? plantId : null
})
const selectedFacilityDateIsToday = computed(() => selectedFacilityDate.value === formatDateInput(new Date()))
const selectedPeakDateIsToday = computed(() => selectedPeakDate.value === formatDateInput(new Date()))
const facilityEquipmentCards = computed(() => {
  const cards = state.facilityLineUsages.length
    ? state.facilityLineUsages
    : state.facilities
    .filter((facility) => facility.facilityType === selectedFacilityLine.value && Number(facility.id) >= 10000)
    .map((facility) => ({
      facilityId: facility.id,
      facilityName: facility.name,
      facilityType: facility.facilityType,
      facilityStatus: facility.status,
      usageDate: selectedFacilityDate.value,
      todayUsageKwh: 0,
      yesterdayUsageKwh: 0,
      monthlyAverageKwh: 0,
      todayVsYesterdayRate: 0,
      todayVsMonthlyAverageRate: 0,
      latestMeasuredAt: null,
    }))

  if (!selectedFacilityDateIsToday.value) {
    return cards
  }

  return cards.map((facility) => {
    const key = energyKey(selectedPlantId.value, facility.facilityId)
    const live = liveEnergyByFacility.get(key)
    if (!live) {
      return facility
    }

    const liveUsage = dailyLiveUsage(
      live,
      liveEnergyBaselineByFacility.get(key),
      selectedEnergyMetricKeys.value.camel,
      selectedEnergyMetricKeys.value.snake,
    )
    const realtimeUsage = estimatedRealtimeUsage(facility, live)
    const displayUsage = Number(facility.todayUsageKwh || 0) + Number(liveUsage || 0) + realtimeUsage
    const monthlyAverage = Number(facility.monthlyAverageKwh || 0)
    return {
      ...facility,
      todayUsageKwh: displayUsage,
      todayVsMonthlyAverageRate: calculateChangeRate(displayUsage, monthlyAverage),
      latestMeasuredAt: live.measuredAt || facility.latestMeasuredAt,
    }
  })
})
const selectedFacilityUsage = computed(() =>
  facilityEquipmentCards.value.find((facility) => Number(facility.facilityId) === Number(selectedFacilityId.value)) ||
  null,
)
const facilitySummaryChart = computed(() =>
  state.summaries.slice(-7).map((summary) => ({
    date: formatDate(summary.summaryAt),
    summaryAt: summary.summaryAt,
    usage: usageValueForEnergyType(summary, selectedEnergyType.value) || 0,
  })),
)
const facilityDetailChart = computed(() =>
  state.facilityDetail?.chart?.length ? state.facilityDetail.chart : facilitySummaryChart.value,
)
const facilityDetailLogs = computed(() => {
  if (state.facilityDetail?.logs?.length) {
    return state.facilityDetail.logs
  }

  return facilityDetailChart.value
    .map((point, index, rows) => {
      const previousUsage = Number(rows[index - 1]?.usage || 0)
      const usage = Number(point.usage || 0)
      const changeAmount = usage - previousUsage
      return {
        measuredAt: point.summaryAt || point.date,
        usage,
        changeAmount,
        changeRate: calculateChangeRate(usage, previousUsage),
      }
    })
    .reverse()
})
const facilityDetailMaxUsage = computed(() =>
  Math.max(1, ...facilityDetailChart.value.map((point) => Number(point.usage || 0))),
)
const facilityTodayUsage = computed(() => {
  return selectedFacilityUsage.value?.todayUsageKwh ?? state.facilityDetail?.todayUsage ?? facilityDetailChart.value.at(-1)?.usage ?? 0
})
const facilityChangeAmount = computed(() =>
  state.facilityDetail?.changeAmount ??
  Number(selectedFacilityUsage.value?.todayUsageKwh || 0) - Number(selectedFacilityUsage.value?.yesterdayUsageKwh || 0),
)
const facilityChangeRate = computed(() =>
  state.facilityDetail?.changeRate ?? selectedFacilityUsage.value?.todayVsYesterdayRate ?? facilityDetailLogs.value[0]?.changeRate ?? 0,
)

function metricValue(source, camelKey, snakeKey = camelKey) {
  return source?.[camelKey] ?? source?.[snakeKey]
}

function metricNumber(source, camelKey, snakeKey = camelKey) {
  const value = metricValue(source, camelKey, snakeKey)
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function energyKey(plantId, facilityId) {
  return `${Number(plantId)}:${Number(facilityId)}`
}

function dailyLiveUsage(live, baseline, camelKey, snakeKey = camelKey) {
  if (!live || !baseline) {
    return null
  }
  const liveValue = metricNumber(live, camelKey, snakeKey)
  const baselineValue = metricNumber(baseline, camelKey, snakeKey)
  return Math.max(0, liveValue - baselineValue)
}

function estimatedRealtimeUsage(facility, live) {
  if (!selectedFacilityDateIsToday.value || !live) {
    return 0
  }

  const key = energyKey(selectedPlantId.value, facility.facilityId)
  const seenAt = liveEnergySeenAtByFacility.get(key)
  if (!seenAt) {
    return 0
  }

  const elapsedSeconds = Math.max(0, (realtimeNow.value - seenAt) / 1000)
  if (!elapsedSeconds) {
    return 0
  }

  const averagePerSecond = Number(facility.monthlyAverageKwh || 0) / (10 * 60 * 60)
  const sequence = Number(facility.facilityId || 0) % 10000
  const factor = 0.82 + ((sequence % 6) * 0.08)
  const minimumRates = {
    ELECTRICITY: 0.08,
    GAS: 0.08,
    WATER: 0.0035,
    SOLAR: 0.12,
  }
  let rate = Math.max(averagePerSecond * factor, minimumRates[selectedEnergyType.value] || 0.04)
  if (selectedEnergyType.value === 'SOLAR') {
    const hour = new Date(realtimeNow.value).getHours()
    if (hour < 7 || hour > 18) {
      rate = 0
    }
  }
  return elapsedSeconds * rate
}

function energyMetricKeys(energyType) {
  const values = {
    ELECTRICITY: { camel: 'electricityKwh', snake: 'electricity_kwh' },
    GAS: { camel: 'gasM3', snake: 'gas_m3' },
    WATER: { camel: 'waterTon', snake: 'water_ton' },
    SOLAR: { camel: 'solarKwh', snake: 'solar_kwh' },
  }
  return values[energyType] || values.ELECTRICITY
}

function markLatestEnergyPollSuccess() {
  latestEnergyPollFailureCount = 0
  latestEnergyPollRetryAt = 0
}

function markLatestEnergyPollFailure() {
  latestEnergyPollFailureCount += 1
  const retryDelay = Math.min(15000, 2000 * latestEnergyPollFailureCount)
  latestEnergyPollRetryAt = Date.now() + retryDelay
}

function allLineFacilityIds(plantId = selectedPlantId.value) {
  const numericPlantId = Number(plantId)
  if (!Number.isFinite(numericPlantId) || numericPlantId <= 0) {
    return []
  }
  return Array.from({ length: 24 }, (_, index) => (numericPlantId * 10000) + index + 1)
}

function isLineFacilityId(facilityId, plantId = selectedPlantId.value) {
  const numericFacilityId = Number(facilityId)
  const numericPlantId = Number(plantId)
  if (!Number.isFinite(numericFacilityId) || numericFacilityId < 10000) {
    return false
  }
  const sequence = numericFacilityId % 10000
  return (
    sequence >= 1 &&
    sequence <= 24 &&
    (!Number.isFinite(numericPlantId) || Math.floor(numericFacilityId / 10000) === numericPlantId)
  )
}

function normalizeEnergyMessage(source) {
  if (!source) {
    return null
  }

  const plantId = Number(metricValue(source, 'plantId', 'plant_id'))
  const facilityId = Number(metricValue(source, 'facilityId', 'facility_id'))
  const measuredAt = metricValue(source, 'measuredAt', 'measured_at')

  if (!Number.isFinite(plantId) || !Number.isFinite(facilityId) || !measuredAt) {
    return null
  }

  return {
    plantId,
    facilityId,
    measuredAt,
    electricityKwh: metricNumber(source, 'electricityKwh', 'electricity_kwh'),
    gasM3: metricNumber(source, 'gasM3', 'gas_m3'),
    waterTon: metricNumber(source, 'waterTon', 'water_ton'),
    solarKwh: metricNumber(source, 'solarKwh', 'solar_kwh'),
    peakKw: metricNumber(source, 'peakKw', 'peak_kw'),
  }
}

function rememberEnergyMessage(source) {
  const message = normalizeEnergyMessage(source)
  if (!message) {
    return null
  }

  const key = energyKey(message.plantId, message.facilityId)
  if (formatDate(message.measuredAt) === formatDateInput(new Date())) {
    const baseline = liveEnergyBaselineByFacility.get(key)
    if (!baseline || formatDate(baseline.measuredAt) !== formatDateInput(new Date())) {
      liveEnergyBaselineByFacility.set(key, message)
    }
  }

  const previous = liveEnergyByFacility.get(key)
  const previousTime = Date.parse(previous?.measuredAt || '')
  const nextTime = Date.parse(message.measuredAt)
  if (Number.isFinite(previousTime) && Number.isFinite(nextTime) && nextTime < previousTime) {
    return previous
  }

  if (!previous || !Number.isFinite(previousTime) || !Number.isFinite(nextTime) || nextTime > previousTime) {
    liveEnergySeenAtByFacility.set(key, realtimeNow.value)
  }

  liveEnergyByFacility.set(key, message)

  const series = liveEnergySeriesByFacility.get(key) || []
  series.push(message)
  if (series.length > LIVE_SERIES_LIMIT) {
    series.splice(0, series.length - LIVE_SERIES_LIMIT)
  }
  liveEnergySeriesByFacility.set(key, series)

  return message
}

function usageValueForEnergyType(source, energyType) {
  if (!source) {
    return null
  }
  const values = {
    ELECTRICITY: metricNumber(source, 'electricityKwh', 'electricity_kwh'),
    GAS: metricNumber(source, 'gasM3', 'gas_m3'),
    WATER: metricNumber(source, 'waterTon', 'water_ton'),
    SOLAR: metricNumber(source, 'solarKwh', 'solar_kwh'),
  }
  return values[energyType] ?? null
}

function calculateChangeRate(currentUsage, previousUsage) {
  const current = Number(currentUsage || 0)
  const previous = Number(previousUsage || 0)
  return previous === 0 ? 0 : ((current - previous) / previous) * 100
}

const summaryChartRows = computed(() => {
  const rows = recentSummaries.value.slice(-7).map((summary) => ({
    id: summary.id,
    label: formatDateTime(summary.summaryAt).slice(5, 16),
    value: Number(metricValue(summary, 'electricityKwh', 'electricity_kwh') || 0),
    live: false,
  }))

  if (liveEnergySource.value) {
    rows.push({
      id: 'live-latest',
      label: '현재',
      value: metricNumber(liveEnergySource.value, 'electricityKwh', 'electricity_kwh'),
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

function formatDateTimeInput(date) {
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${formatDateInput(date)}T${hours}:${minutes}:${seconds}`
}

function endOfFacilityQueryDate() {
  return selectedFacilityDateIsToday.value ? formatDateTimeInput(new Date()) : `${selectedFacilityDate.value}T23:59:59`
}

function formatMonthInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function formatMonthStartInput(date) {
  return `${formatMonthInput(date)}-01`
}

function formatMonthEndInput(date) {
  const monthEnd = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  return formatDateInput(monthEnd)
}

function monthRange(month) {
  const [year, monthIndex] = String(month || formatMonthInput(new Date())).split('-').map(Number)
  const start = new Date(year, monthIndex - 1, 1)
  const end = new Date(year, monthIndex, 0)
  return {
    from: formatDateInput(start),
    to: formatDateInput(end),
  }
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
  if (!selectedUtilityDateIsToday.value) {
    return '-'
  }
  const labels = {
    NORMAL: '정상',
    DELAY: '지연',
    NONE: '미수신',
  }
  return labels[status] || status || '-'
}

function meterStatusClass(status) {
  if (!selectedUtilityDateIsToday.value) {
    return 'neutral'
  }
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
  const source = liveEnergySource.value || {}
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
    metricValue(liveEnergySource.value, 'peakKw', 'peak_kw') ?? metricValue(latestSummary.value, 'peakKw', 'peak_kw') ?? 0,
  )
  return peak ? Math.min(Math.round((peak / 1400) * 100), 999) : 0
})

const alarmCount = computed(() => state.overview?.occurredAlarmCount ?? state.alarms.length)
const peakMetrics = computed(() => {
  const metrics = state.peakDashboard?.metrics || {}
  const live = selectedPlantLiveEnergy.value
  if (!live || selectedPeakPeriod.value !== 'DAY' || !selectedPeakDateIsToday.value) {
    return metrics
  }

  const elapsedSeconds = selectedPlantRealtimeElapsedSeconds.value
  const currentKw = metricNumber(live, 'peakKw', 'peak_kw') + (elapsedSeconds * 0.8)
  const thresholdKw = Number(metrics.thresholdKw || 1400)
  const intervalPoints = peakTrendPoints.value
  const currentIntervalAt = live.measuredAt
  const recentCutoff = Date.parse(currentIntervalAt || '') - 15 * 60 * 1000
  const recentRows = liveEnergyEntries.value.filter((row) => {
    const measuredAt = Date.parse(row.measuredAt || '')
    return Number(row.plantId) === Number(selectedPlantId.value) && measuredAt >= recentCutoff
  })
  const recentPeaks = recentRows.map((row) => metricNumber(row, 'peakKw', 'peak_kw')).filter((value) => value > 0)
  const intervalAverageKw = recentPeaks.length
    ? recentPeaks.reduce((sum, value) => sum + value, 0) / recentPeaks.length
    : metrics.intervalAverageKw
  const intervalMaxKw = recentPeaks.length
    ? Math.max(...recentPeaks)
    : metrics.intervalMaxKw
  return {
    ...metrics,
    currentKw,
    peakUsageRate: thresholdKw ? Math.min((currentKw / thresholdKw) * 100, 999) : 0,
    intervalAverageKw,
    intervalMaxKw,
    intervalAt: currentIntervalAt || intervalPoints.at(-1)?.measuredAt || metrics.intervalAt,
    measuredAt: live.measuredAt,
  }
})
const peakTrend = computed(() => state.peakDashboard?.trend || [])
const peakRanking = computed(() => state.peakDashboard?.facilityRanking || [])
const peakHistory = computed(() => state.peakDashboard?.history || [])
const peakPlantComparison = computed(() => state.peakDashboard?.plantComparison || [])
const peakPeriodLabel = computed(
  () => peakPeriodOptions.find((option) => option.value === selectedPeakPeriod.value)?.label || '일',
)
const peakPeriodRangeLabel = computed(() => {
  const from = state.peakDashboard?.periodFrom || state.peakDashboard?.period_from || selectedPeakDate.value
  const to = state.peakDashboard?.periodTo || state.peakDashboard?.period_to || selectedPeakDate.value
  return from === to ? formatDate(from) : `${formatDate(from)} - ${formatDate(to)}`
})
const peakComparisonSubtitle = computed(() => `${peakPeriodRangeLabel.value} · 단위: kW`)
const peakTrendTitle = computed(() => `${peakPeriodLabel.value} 단위 피크 전력 추이`)
const peakPreviousPeriodLabel = computed(() =>
  selectedPeakPeriod.value === 'DAY' ? '전일 평균 대비' : '직전 기간 평균 대비',
)
const peakLivePillLabel = computed(() =>
  selectedPeakPeriod.value === 'DAY' && selectedPeakDateIsToday.value && liveEnergyFresh.value
    ? '실시간 수신 중'
    : `${peakPeriodLabel.value} 단위 데이터`,
)
const peakComparisonMax = computed(() =>
  Math.max(1, ...peakPlantComparison.value.map((plant) => Number(plant.periodPeakKw || plant.period_peak_kw || 0))),
)
const selectedPeakPlantComparison = computed(() =>
  peakPlantComparison.value.find((plant) => Number(plant.plantId || plant.plant_id) === Number(selectedPlantId.value)) || null,
)
const peakComparisonRows = computed(() =>
  peakPlantComparison.value
    .map((plant, index) => {
      const peakKw = Number(plant.periodPeakKw || plant.period_peak_kw || 0)
      const thresholdKw = Number(plant.thresholdKw || plant.threshold_kw || 0)
      return {
        plantId: plant.plantId || plant.plant_id,
        plantName: plant.plantName || plant.plant_name || `공장 ${plant.plantId || plant.plant_id}`,
        rank: plant.rank || plant.plant_rank || index + 1,
        periodPeakKw: peakKw,
        periodAverageKw: Number(plant.periodAverageKw || plant.period_average_kw || 0),
        thresholdKw,
        peakUsageRate: Number(plant.peakUsageRate || plant.peak_usage_rate || 0),
        exceeded: plant.exceeded === true || plant.exceeded === 1 || plant.exceeded === 'true',
        barWidth: `${Math.max(6, Math.round((peakKw / peakComparisonMax.value) * 100))}%`,
        active: Number(plant.plantId || plant.plant_id) === Number(selectedPlantId.value),
      }
    })
    .sort((a, b) => a.rank - b.rank),
)
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
  })).filter((row) => Number(row.peakUsageRate || 0) > 100),
)
const utilityMetrics = computed(() => {
  const metrics = state.utilityDashboard?.metrics || {}
  if (!selectedUtilityDateIsToday.value || selectedUtilityPeriod.value !== 'DAY') {
    return metrics
  }

  const liveUsage = selectedPlantDailyLiveUsage.value
  const elapsedSeconds = selectedPlantRealtimeElapsedSeconds.value
  const baseGasUsageM3 = Number(metrics.gasUsageM3 || metrics.gas_usage_m3 || 0)
  const baseWaterUsageTon = Number(metrics.waterUsageTon || metrics.water_usage_ton || 0)
  const gasUsageM3 = baseGasUsageM3 + liveUsage.gasM3 + (elapsedSeconds * 0.08)
  const waterUsageTon = baseWaterUsageTon + liveUsage.waterTon + (elapsedSeconds * 0.0035)
  return {
    ...metrics,
    gasUsageM3,
    gasTotalM3: Math.max(Number(metrics.gasTotalM3 || metrics.gas_total_m3 || 0), gasUsageM3),
    waterUsageTon,
    waterTotalTon: Math.max(Number(metrics.waterTotalTon || metrics.water_total_ton || 0), waterUsageTon),
  }
})
const utilityPeriodLabel = computed(
  () => utilityPeriodOptions.find((option) => option.value === selectedUtilityPeriod.value)?.label || '일',
)
const utilityPeriodRangeLabel = computed(() => {
  const from = state.utilityDashboard?.periodFrom || state.utilityDashboard?.period_from || selectedUtilityDate.value
  const to = state.utilityDashboard?.periodTo || state.utilityDashboard?.period_to || selectedUtilityDate.value
  return from === to ? formatDate(from) : `${formatDate(from)} - ${formatDate(to)}`
})
const utilityPatternRangeLabel = computed(() =>
  selectedUtilityPeriod.value === 'DAY' ? '최근 7일' : utilityPeriodRangeLabel.value,
)
const utilityPreviousPeriodLabel = computed(() =>
  selectedUtilityPeriod.value === 'DAY' ? '전일 대비' : '직전 기간 대비',
)
const utilityLivePillLabel = computed(() =>
  selectedUtilityPeriod.value === 'DAY' && selectedUtilityDateIsToday.value && liveEnergyFresh.value
    ? '실시간 수신 중'
    : `${utilityPeriodLabel.value} 단위 데이터`,
)
const utilityDashboardPeriodUsage = computed(() => state.utilityDashboard?.periodUsage || state.utilityDashboard?.period_usage || [])
const utilityHourlyUsage = computed(() => {
  if (selectedUtilityPeriod.value !== 'DAY') {
    return utilityDashboardPeriodUsage.value.map((point) => ({
      measuredAt: point.measuredAt || point.measured_at,
      gasUsageM3: Number(point.gasUsageM3 || point.gas_usage_m3 || 0),
      waterUsageTon: Number(point.waterUsageTon || point.water_usage_ton || 0),
    }))
  }

  return Array.from({ length: 24 }, (_, hour) => {
    const measuredAt = `${selectedUtilityDate.value}T${String(hour).padStart(2, '0')}:00:00`
    const source = (state.utilityDashboard?.hourlyUsage || []).find((point) => {
      const value = point.measuredAt || point.measured_at
      return formatDate(value) === selectedUtilityDate.value && Number(formatTime(value).slice(0, 2)) === hour
    })
    return {
      measuredAt,
      gasUsageM3: Number(source?.gasUsageM3 || source?.gas_usage_m3 || 0),
      waterUsageTon: Number(source?.waterUsageTon || source?.water_usage_ton || 0),
    }
  })
})
const utilityMeterStatuses = computed(() =>
  (state.utilityDashboard?.meterStatuses || [])
    .filter((meter) => {
      const facilityId = Number(meter.facilityId || meter.facility_id)
      const sequence = facilityId % 10000
      return (
        facilityId >= 10000 &&
        Math.floor(facilityId / 10000) === Number(selectedPlantId.value) &&
        sequence >= 1 &&
        sequence <= 24
      )
    })
    .map((meter) => {
      const facilityId = Number(meter.facilityId || meter.facility_id)
      const meterType = meter.meterType || meter.meter_type
      return {
        ...meter,
        facilityId,
        facilityName: meter.facilityName || meter.facility_name,
        meterType,
        currentValue: metricNumber(meter, 'currentValue', 'current_value'),
        lastReceivedAt: metricValue(meter, 'lastReceivedAt', 'last_received_at'),
        communicationStatus: metricValue(meter, 'communicationStatus', 'communication_status'),
        meterName: `${facilityCode({ facilityId })} ${meterTypeLabel(meterType)} 계측기`,
      }
    }),
)
const selectedUtilityDateIsToday = computed(() => selectedUtilityDate.value === formatDateInput(new Date()))
const utilityPlantComparison = computed(() => state.utilityDashboard?.plantComparison || state.utilityDashboard?.plant_comparison || [])
const utilityGasComparisonMax = computed(() =>
  Math.max(1, ...utilityPlantComparison.value.map((plant) => Number(plant.gasUsageM3 || plant.gas_usage_m3 || 0))),
)
const utilityWaterComparisonMax = computed(() =>
  Math.max(1, ...utilityPlantComparison.value.map((plant) => Number(plant.waterUsageTon || plant.water_usage_ton || 0))),
)
const selectedUtilityPlantComparison = computed(() =>
  utilityPlantComparison.value.find((plant) => Number(plant.plantId || plant.plant_id) === Number(selectedPlantId.value)) || null,
)
const utilityComparisonRows = computed(() =>
  utilityPlantComparison.value
    .map((plant) => {
      const gasUsage = Number(plant.gasUsageM3 || plant.gas_usage_m3 || 0)
      const waterUsage = Number(plant.waterUsageTon || plant.water_usage_ton || 0)
      return {
        plantId: plant.plantId || plant.plant_id,
        plantName: plant.plantName || plant.plant_name || `공장 ${plant.plantId || plant.plant_id}`,
        gasUsageM3: gasUsage,
        waterUsageTon: waterUsage,
        gasShareRate: Number(plant.gasShareRate || plant.gas_share_rate || 0),
        waterShareRate: Number(plant.waterShareRate || plant.water_share_rate || 0),
        gasRank: Number(plant.gasRank || plant.gas_rank || 0),
        waterRank: Number(plant.waterRank || plant.water_rank || 0),
        gasBarWidth: `${Math.max(6, Math.round((gasUsage / utilityGasComparisonMax.value) * 100))}%`,
        waterBarWidth: `${Math.max(6, Math.round((waterUsage / utilityWaterComparisonMax.value) * 100))}%`,
        active: Number(plant.plantId || plant.plant_id) === Number(selectedPlantId.value),
      }
    })
    .sort((a, b) => a.gasRank - b.gasRank),
)
const filteredUtilityMeterStatuses = computed(() => {
  const keyword = utilityMeterSearch.value.trim().toLowerCase()
  if (!keyword) {
    return utilityMeterStatuses.value
  }
  return utilityMeterStatuses.value.filter((meter) =>
    [
      meter.meterName,
      meter.facilityName,
      meter.meterType,
      meter.communicationStatus,
    ].some((value) => String(value || '').toLowerCase().includes(keyword)),
  )
})
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
  const periodFrom = state.utilityDashboard?.periodFrom || state.utilityDashboard?.period_from || selectedUtilityDate.value
  const periodTo = state.utilityDashboard?.periodTo || state.utilityDashboard?.period_to || selectedUtilityDate.value
  const startDate = new Date(`${periodFrom || selectedUtilityDate.value || formatDateInput(new Date())}T00:00:00`)
  const endDate = new Date(`${periodTo || selectedUtilityDate.value || formatDateInput(new Date())}T00:00:00`)
  if (selectedUtilityPeriod.value === 'DAY') {
    startDate.setDate(endDate.getDate() - 6)
  }
  const dayCount = Math.max(1, Math.round((endDate - startDate) / (24 * 60 * 60 * 1000)) + 1)
  return Array.from({ length: Math.min(dayCount, 31) }, (_, index) => {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)
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
const utilityTrendTitle = computed(() =>
  selectedUtilityPeriod.value === 'DAY' ? '시간대별 사용량' : `${utilityPeriodLabel.value} 단위 사용량 추이`,
)
const utilityGasPoints = computed(() => utilityLinePoints(utilityHourlyUsage.value, 'gasUsageM3', utilityGasMax.value))
const utilityWaterPoints = computed(() =>
  utilityLinePoints(utilityHourlyUsage.value, 'waterUsageTon', utilityWaterMax.value),
)
const utilityGasLinePath = computed(() => utilityPath(utilityGasPoints.value))
const utilityWaterLinePath = computed(() => utilityPath(utilityWaterPoints.value))
const utilityGasAreaPath = computed(() => utilityAreaPath(utilityGasPoints.value))
const utilityWaterAreaPath = computed(() => utilityAreaPath(utilityWaterPoints.value))
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

function buildScadaExternalUrl(userName) {
  const url = new URL(SCADA_EXTERNAL_URL)
  if (userName) {
    url.searchParams.set('userName', userName)
  }
  return url.toString()
}

async function redirectToScada() {
  let userName = state.me?.name

  if (!userName && getAccessToken()) {
    try {
      const me = await api.me()
      state.me = me
      userName = me.name
    } catch {
      userName = ''
    }
  }

  window.location.href = buildScadaExternalUrl(userName)
}

function routeTo(hash) {
  if (hash === '/scada') {
    redirectToScada()
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

  if (!getAccessToken()) {
    routeTo('/login')
    return
  }

  if (route === '/scada') {
    redirectToScada()
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

function peakPointLabel(value) {
  if (selectedPeakPeriod.value === 'DAY') {
    return formatTime(value)
  }
  const date = formatDate(value)
  return date === '-' ? '-' : date.slice(5)
}

function utilityPointLabel(value) {
  if (selectedUtilityPeriod.value === 'DAY') {
    const time = formatTime(value)
    return time === '-' ? '-' : `${time.slice(0, 2)}시`
  }
  const date = formatDate(value)
  return date === '-' ? '-' : date.slice(5)
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

function utilityLinePoints(points, key, maxValue) {
  const width = 720
  const height = 220
  const paddingX = 26
  const plotWidth = width - paddingX * 2
  const gap = points.length <= 1 ? plotWidth : plotWidth / (points.length - 1)

  return points.map((point, index) => {
    const value = Number(point[key] || 0)
    const ratio = Math.min(value / Math.max(maxValue, 1), 1)
    return {
      ...point,
      chartValue: value,
      x: Math.round(paddingX + index * gap),
      y: Math.round(height - ratio * (height - 34) - 14),
    }
  })
}

function utilityPath(points) {
  return points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')
}

function utilityAreaPath(points) {
  if (!points.length) {
    return ''
  }
  return `${utilityPath(points)} L ${points.at(-1).x} 220 L ${points[0].x} 220 Z`
}

function showUtilityTooltip(event, point, chart, label, unit) {
  utilityTooltip.value = {
    chart,
    label,
    time: formatTime(point.measuredAt),
    value: formatNumber(point.chartValue),
    unit,
    x: 0,
    y: 0,
  }
  moveUtilityTooltip(event)
}

function moveUtilityTooltip(event) {
  if (!utilityTooltip.value) {
    return
  }
  const chart = event.currentTarget.closest('.utility-line-chart')
  const rect = chart?.getBoundingClientRect()
  if (!rect) {
    return
  }
  utilityTooltip.value = {
    ...utilityTooltip.value,
    x: Math.min(Math.max(event.clientX - rect.left + 14, 12), rect.width - 144),
    y: Math.max(event.clientY - rect.top - 48, 10),
  }
}

function hideUtilityTooltip() {
  utilityTooltip.value = null
}

function facilitySequence(facility) {
  const id = Number(facility?.facilityId ?? facility?.id)
  if (!Number.isFinite(id)) {
    return null
  }
  return id >= 10000 ? id % 10000 : id % 100
}

function facilityCode(facility) {
  const sequence = facilitySequence(facility)
  if (!sequence) {
    return facility?.facilityName || facility?.name || '-'
  }
  return `F-${String(sequence).padStart(3, '0')}`
}

function facilityProcessName(facility) {
  const sequence = facilitySequence(facility)
  return equipmentProcessNames[sequence] || facility?.facilityName || facility?.name || selectedFacilityLineMeta.value.label
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
    if (
      error.status === 401 ||
      error.status === 403 ||
      error.message.includes('인증') ||
      error.message.includes('Unauthorized')
    ) {
      stopEnergyWebSocket()
      stopEnergyPolling()
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
      stopEnergyPolling()
      clearTokens()
      routeTo('/login')
    }
  })
}

async function loadInitial() {
  await run(async () => {
    const [me, plantsResponse] = await Promise.all([api.me(), api.plants()])
    const plants = Array.isArray(plantsResponse) ? plantsResponse : []
    state.me = me
    state.plants = plants

    const currentPlant = plants.find((plant) => Number(plant.id) === Number(selectedPlantId.value))
    const userPlant = plants.find((plant) => Number(plant.id) === Number(me.plantId))
    const fallbackPlantId = me.role === 'ADMIN'
      ? currentPlant?.id ?? plants[0]?.id ?? null
      : userPlant?.id ?? plants[0]?.id ?? null

    syncingSelection.value = true
    selectedPlantId.value = me.role === 'ADMIN' ? fallbackPlantId : userPlant?.id ?? fallbackPlantId
    syncingSelection.value = false

    if (!selectedPlantId.value) {
      state.facilities = []
      state.summaries = []
      state.measurements = []
      state.latestEnergy = null
      state.facilityDetail = null
      return
    }

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
  selectedFacilityId.value = null
  syncingSelection.value = false

  await loadActivePageData()
}

async function loadActivePageData() {
  if (appMode.value === 'login') {
    return
  }

  if (!selectedPlantId.value && activePage.value !== 'alarms') {
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

  if (activePage.value === 'chatbot') {
    await loadChatbotMessages()
    return
  }

  if (activePage.value === 'users') {
    const users = await api.users({ page: 0, size: 20 })
    state.users = users.items || []
    return
  }

  if (activePage.value === 'alarms') {
    state.alarms = await api.alarms({ plantId: selectedPlantId.value || undefined, limit: 100 })
  }
}

async function loadChatbotMessages() {
  if (!selectedPlantId.value) {
    state.chatbotMessages = []
    return
  }

  state.chatbotMessages = await api.chatbotMessages({
    plantId: selectedPlantId.value,
    limit: 20,
  })
}

async function submitChatbotQuestion() {
  const question = chatbotQuestion.value.trim()
  if (!question || chatbotSending.value || !selectedPlantId.value) {
    return
  }

  chatbotSending.value = true
  try {
    await run(async () => {
      const response = await api.askChatbot({
        plantId: selectedPlantId.value,
        question,
      })
      state.chatbotMessages = [
        response,
        ...state.chatbotMessages.filter((message) => Number(message.id) !== Number(response.id)),
      ].slice(0, 20)
      chatbotQuestion.value = ''
    })
  } finally {
    chatbotSending.value = false
  }
}

function useSuggestedQuestion(question) {
  chatbotQuestion.value = question
}

async function loadEnergyData() {
  if (!selectedPlantId.value) {
    return
  }

  selectedDateFrom.value = selectedFacilityDate.value
  selectedDateTo.value = selectedFacilityDate.value

  const summaryParams = {
    plantId: selectedPlantId.value,
    summaryType: 'DAILY',
    from: `${selectedFacilityDate.value}T00:00:00`,
    to: endOfFacilityQueryDate(),
  }
  const measurementParams = {
    plantId: selectedPlantId.value,
    facilityId: selectedFacilityId.value || undefined,
    from: `${selectedFacilityDate.value}T00:00:00`,
    to: endOfFacilityQueryDate(),
    limit: 20,
  }

  const [summaries, measurements, latestEnergy, facilityLineUsages] = await Promise.all([
    api.energySummaries(summaryParams),
    api.energyMeasurements(measurementParams),
    selectedFacilityId.value
      ? api.latestEnergy(selectedPlantId.value, selectedFacilityId.value).catch(() => null)
      : Promise.resolve(null),
    api.energyFacilityLine({
      plantId: selectedPlantId.value,
      facilityType: selectedFacilityLine.value,
      energyType: selectedEnergyType.value,
      date: selectedFacilityDate.value,
    }).catch(() => []),
  ])

  state.summaries = summaries
  state.measurements = measurements
  state.latestEnergy = rememberEnergyMessage(latestEnergy) || latestEnergy
  state.facilityLineUsages = facilityLineUsages
  if (selectedFacilityDateIsToday.value) {
    await preloadFacilityLineLiveEnergy(facilityLineUsages)
  }
  await loadFacilityDetail()
  startEnergyWebSocket()
  startEnergyPolling()
}

async function preloadFacilityLineLiveEnergy(facilityLineUsages = []) {
  if (!selectedPlantId.value || !facilityLineUsages.length) {
    return
  }

  const facilityIds = new Set(facilityLineUsages.map((facility) => Number(facility.facilityId)))
  const rows = await api.energyMeasurements({
    plantId: selectedPlantId.value,
    from: `${selectedFacilityDate.value}T00:00:00`,
    to: endOfFacilityQueryDate(),
    limit: 500,
  }).catch(() => [])

  const rowsByFacility = new Map()
  rows
    .map(normalizeEnergyMessage)
    .filter((message) => message && facilityIds.has(Number(message.facilityId)))
    .forEach((message) => {
      const key = energyKey(message.plantId, message.facilityId)
      const list = rowsByFacility.get(key) || []
      list.push(message)
      rowsByFacility.set(key, list)
    })

  facilityLineUsages.forEach((facility) => {
    const key = energyKey(selectedPlantId.value, facility.facilityId)
    const list = rowsByFacility.get(key) || []
    const latest = list[0] || null
    const baseline = latest
    if (!liveEnergyBaselineByFacility.has(key) && baseline) {
      liveEnergyBaselineByFacility.set(key, baseline)
    }
    rememberEnergyMessage(latest)
  })
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
  state.latestEnergy = rememberEnergyMessage(latestEnergy) || latestEnergy
  applySummaryDateRange(summaries)
  startEnergyWebSocket()
  startEnergyPolling()
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
    period: selectedPeakPeriod.value,
  })
  startEnergyWebSocket()
  startEnergyPolling()
}

async function loadUtilityDashboard() {
  if (!selectedPlantId.value) {
    state.utilityDashboard = null
    return
  }

  state.utilityDashboard = await api.utilityDashboard({
    plantId: selectedPlantId.value,
    date: selectedUtilityDate.value || undefined,
    period: selectedUtilityPeriod.value,
  })
  startEnergyWebSocket()
  startEnergyPolling()
}

async function loadEsgDashboard() {
  const range = monthRange(selectedEsgMonth.value)
  selectedEsgFrom.value = range.from
  selectedEsgTo.value = range.to
  state.esgDashboard = await api.esgEnvironmentDashboard({
    plantId: selectedPlantId.value || undefined,
    ...dateRangeParams(range.from, range.to),
  })
}

async function loadLatestEnergy() {
  if (!selectedPlantId.value || appMode.value === 'login') {
    state.latestEnergy = null
    return
  }
  if (Date.now() < latestEnergyPollRetryAt) {
    return
  }

  if (activePage.value === 'facility' && selectedFacilityDateIsToday.value) {
    const visibleFacilityIds = new Set(
      (state.facilityLineUsages.length
        ? state.facilityLineUsages.map((facility) => Number(facility.facilityId))
        : allLineFacilityIds(selectedPlantId.value))
    )
    let rows = []
    try {
      rows = await api.energyMeasurements({
        plantId: selectedPlantId.value,
        from: `${formatDateInput(new Date())}T00:00:00`,
        to: formatDateTimeInput(new Date()),
        limit: 2000,
      })
      markLatestEnergyPollSuccess()
    } catch {
      markLatestEnergyPollFailure()
      return
    }
    const seenFacilityIds = new Set()
    rows
      .map(normalizeEnergyMessage)
      .filter((message) => message && visibleFacilityIds.has(Number(message.facilityId)))
      .forEach((message) => {
        if (seenFacilityIds.has(message.facilityId)) {
          return
        }
        seenFacilityIds.add(message.facilityId)
        rememberEnergyMessage(message)
      })
    state.latestEnergy = selectedLiveEnergy.value || Array.from(liveEnergyByFacility.values()).find((message) =>
      visibleFacilityIds.has(Number(message.facilityId))
    ) || null
    return
  }

  if (selectedFacilityId.value) {
    let latestEnergy = null
    try {
      latestEnergy = await api.latestEnergy(selectedPlantId.value, selectedFacilityId.value)
      markLatestEnergyPollSuccess()
    } catch {
      markLatestEnergyPollFailure()
      return
    }
    state.latestEnergy = rememberEnergyMessage(latestEnergy) || latestEnergy
    return
  }

  let rows = []
  try {
    rows = await api.energyMeasurements({
      plantId: selectedPlantId.value,
      from: `${formatDateInput(new Date())}T00:00:00`,
      to: formatDateTimeInput(new Date()),
      limit: 500,
    })
    markLatestEnergyPollSuccess()
  } catch {
    markLatestEnergyPollFailure()
    return
  }
  const seenFacilityIds = new Set()
  rows
    .map(normalizeEnergyMessage)
    .filter((message) => message && isLineFacilityId(message.facilityId, selectedPlantId.value))
    .forEach((message) => {
      if (seenFacilityIds.has(message.facilityId)) {
        return
      }
      seenFacilityIds.add(message.facilityId)
      rememberEnergyMessage(message)
    })
  state.latestEnergy = selectedLiveEnergy.value || null
  if (activePage.value === 'peak') {
    schedulePeakRefresh()
  }
  if (activePage.value === 'utility') {
    scheduleUtilityRefresh()
  }
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

    const normalizedMessage = rememberEnergyMessage(message)
    if (!normalizedMessage) {
      return
    }

    const plantId = Number(normalizedMessage.plantId)
    const facilityId = Number(normalizedMessage.facilityId)

    if (plantId === Number(selectedPlantId.value)) {
      if (!selectedFacilityId.value || facilityId === Number(selectedFacilityId.value)) {
        state.latestEnergy = selectedLiveEnergy.value || normalizedMessage
      }
    }
    if (plantId === Number(selectedPlantId.value) && activePage.value === 'peak') {
      schedulePeakRefresh()
    }
    if (plantId === Number(selectedPlantId.value) && activePage.value === 'utility') {
      scheduleUtilityRefresh()
    }
    if (activePage.value === 'alarms' && (!selectedPlantId.value || plantId === Number(selectedPlantId.value))) {
      scheduleAlarmRefresh()
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

function startEnergyPolling() {
  if (energyPollTimer || appMode.value === 'login') {
    return
  }

  energyPollTimer = window.setInterval(async () => {
    if (latestEnergyPollInFlight || appMode.value === 'login') {
      return
    }

    latestEnergyPollInFlight = true
    try {
      await loadLatestEnergy()
    } finally {
      latestEnergyPollInFlight = false
    }
  }, LIVE_POLL_MS)
}

function stopEnergyPolling() {
  window.clearInterval(energyPollTimer)
  energyPollTimer = null
  latestEnergyPollInFlight = false
  latestEnergyPollRetryAt = 0
  latestEnergyPollFailureCount = 0
}

async function refreshData() {
  if (appMode.value === 'detail' && activePage.value !== 'facility') {
    await run(loadActivePageData)
    return
  }
  await run(loadPlantData)
}

function schedulePeakRefresh() {
  if (peakRefreshTimer || Date.now() - lastPeakRefreshAt < LIVE_DASHBOARD_REFRESH_MS) {
    return
  }
  window.clearTimeout(peakRefreshTimer)
  peakRefreshTimer = window.setTimeout(() => {
    lastPeakRefreshAt = Date.now()
    peakRefreshTimer = null
    loadPeakDashboard().catch(() => {})
  }, 1000)
}

function scheduleUtilityRefresh() {
  if (utilityRefreshTimer || Date.now() - lastUtilityRefreshAt < LIVE_DASHBOARD_REFRESH_MS) {
    return
  }
  window.clearTimeout(utilityRefreshTimer)
  utilityRefreshTimer = window.setTimeout(() => {
    lastUtilityRefreshAt = Date.now()
    utilityRefreshTimer = null
    loadUtilityDashboard().catch(() => {})
  }, 1000)
}

function scheduleAlarmRefresh() {
  if (Date.now() - lastAlarmRefreshAt < 5000) {
    return
  }
  lastAlarmRefreshAt = Date.now()
  api.alarms({ plantId: selectedPlantId.value || undefined, limit: 100 })
    .then((alarms) => {
      state.alarms = alarms
    })
    .catch(() => {})
}

async function resolveAlarm(alarmId) {
  await run(async () => {
    await api.resolveAlarm(alarmId)
    state.alarms = await api.alarms({ plantId: selectedPlantId.value || undefined, limit: 100 })
  })
}

async function deleteAlarm(alarmId) {
  await run(async () => {
    await api.deleteAlarm(alarmId)
    state.alarms = await api.alarms({ plantId: selectedPlantId.value || undefined, limit: 100 })
  })
}

function goScada() {
  routeTo('/scada')
}

function goDetail(page = 'facility') {
  routeTo(`/detail/${page}`)
}

function selectFacilityCard(facilityId) {
  selectedFacilityId.value = facilityId
}

function canAccessPlant(plantId) {
  if (!plantId) {
    return canViewAllPlants.value
  }
  return isAdminUser.value || !assignedPlantId.value || Number(plantId) === Number(assignedPlantId.value)
}

function enforcePlantAccess(plantId) {
  if (canAccessPlant(plantId)) {
    return true
  }

  errorMessage.value = '해당 사업장에 대한 권한이 없습니다.'
  syncingSelection.value = true
  selectedPlantId.value = assignedPlantId.value
  syncingSelection.value = false
  return false
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
    if (!enforcePlantAccess(selectedPlantId.value)) {
      return
    }
    if (!selectedPlantId.value && activePage.value === 'alarms') {
      run(loadActivePageData)
      return
    }
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
    run(loadFacilityDetail)
  }
})

watch([selectedFacilityLine, selectedFacilityDate], () => {
  if (appMode.value !== 'login' && activePage.value === 'facility' && !syncingSelection.value) {
    selectedFacilityId.value = null
    state.facilityDetail = null
    run(loadEnergyData)
  }
})

watch(selectedEnergyType, () => {
  if (appMode.value !== 'login' && activePage.value === 'facility' && !syncingSelection.value) {
    run(loadEnergyData)
  }
})

watch(selectedPeakDate, () => {
  if (appMode.value !== 'login' && activePage.value === 'peak' && !syncingSelection.value) {
    run(loadPeakDashboard)
  }
})

watch(selectedPeakPeriod, () => {
  if (appMode.value !== 'login' && activePage.value === 'peak' && !syncingSelection.value) {
    run(loadPeakDashboard)
  }
})

watch(selectedUtilityDate, () => {
  if (appMode.value !== 'login' && activePage.value === 'utility' && !syncingSelection.value) {
    run(loadUtilityDashboard)
  }
})

watch(selectedUtilityPeriod, () => {
  if (appMode.value !== 'login' && activePage.value === 'utility' && !syncingSelection.value) {
    run(loadUtilityDashboard)
  }
})

watch(selectedEsgMonth, () => {
  if (appMode.value !== 'login' && activePage.value === 'esg' && !syncingSelection.value) {
    run(loadEsgDashboard)
  }
})

onMounted(() => {
  applyRoute()
  window.addEventListener('hashchange', applyRoute)
  realtimeTickTimer = window.setInterval(() => {
    realtimeNow.value = Date.now()
  }, 1000)

  if (getAccessToken()) {
    loadInitial()
  }
})

onUnmounted(() => {
  stopEnergyWebSocket()
  stopEnergyPolling()
  window.clearInterval(realtimeTickTimer)
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
        <h2>에너지 데이터와 ESG 지표를 한 화면에서 확인합니다.</h2>
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
        <p>{{ liveEnergyFresh ? `실시간 수신 ${formatDateTime(latestMeasuredAt).slice(11)}` : 'DB 요약값' }}</p>
      </article>
    </section>

    <section class="scada-main-grid">
      <article class="scada-panel wide">
        <div class="panel-title inline">
          <h2>최근 에너지 요약 추이</h2>
          <span class="live-pill">{{ liveEnergyFresh ? '현재값 반영' : 'API 연동' }}</span>
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
                  metricValue(liveEnergySource, 'peakKw', 'peak_kw') ??
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
            <option v-if="activePage === 'alarms' && canViewAllPlants" :value="null">전체 사업장</option>
            <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
          </select>
        </label>
        <label v-if="activePage === 'facility'">
          설비 라인
          <select v-model="selectedFacilityLine">
            <option v-for="line in facilityLineOptions" :key="line.value" :value="line.value">
              {{ line.label }}
            </option>
          </select>
        </label>
        <label v-if="activePage === 'facility'">
          에너지 종류
          <select v-model="selectedEnergyType">
            <option v-for="energy in energyTypeOptions" :key="energy.value" :value="energy.value">
              {{ energy.label }}
            </option>
          </select>
        </label>
        <label v-if="activePage === 'facility'">
          조회일
          <input v-model="selectedFacilityDate" type="date" />
        </label>
      </section>

      <p v-if="errorMessage" class="api-error">{{ errorMessage }}</p>

      <section v-if="activePage === 'facility'" class="page-stack facility-monitor-page">
        <section class="facility-status-layout facility-card-layout">
          <article class="panel facility-chart-panel facility-equipment-panel">
            <div class="panel-title inline">
              <h2>설비 사용량 추이 <small>({{ selectedFacilityLineMeta.label }} / {{ selectedEnergyMeta.label }})</small></h2>
              <span class="live-pill">{{ selectedFacilityDateIsToday && liveEnergyFresh ? '실시간 반영' : '일별 데이터' }}</span>
            </div>
            <div class="facility-equipment-grid">
              <button
                v-for="facility in facilityEquipmentCards"
                :key="facility.facilityId"
                type="button"
                :class="['facility-equipment-card', { active: Number(selectedFacilityId) === Number(facility.facilityId) }]"
                @click="selectFacilityCard(facility.facilityId)"
              >
                <span class="facility-equipment-icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path :d="equipmentIconPath"></path>
                  </svg>
                </span>
                <strong>{{ facilityCode(facility) }}</strong>
                <small>{{ facilityProcessName(facility) }}</small>
                <b>{{ formatNumber(facility.todayUsageKwh, selectedEnergyPrecision) }} <em>{{ selectedEnergyMeta.unit }}</em></b>
                <i :class="trendClass(facility.todayVsMonthlyAverageRate)">
                  월 평균 {{ trendPrefix(facility.todayVsMonthlyAverageRate) }}{{ formatNumber(facility.todayVsMonthlyAverageRate) }}%
                </i>
                <span class="facility-month-average">월 평균 {{ formatNumber(facility.monthlyAverageKwh, selectedEnergyPrecision) }} {{ selectedEnergyMeta.unit }}</span>
              </button>
              <article v-if="!facilityEquipmentCards.length" class="facility-empty-card">
                선택한 라인에 표시할 설비 데이터가 없습니다.
              </article>
            </div>
          </article>

          <aside class="panel facility-detail-card" :class="selectedEnergyMeta.tone">
            <template v-if="selectedFacilityUsage">
              <div class="facility-card-head">
                <span class="facility-device-icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path :d="equipmentIconPath"></path>
                  </svg>
                </span>
                <div>
                  <h2>{{ facilityCode(selectedFacilityUsage) }}</h2>
                  <p>{{ facilityProcessName(selectedFacilityUsage) }}</p>
                </div>
                <em v-if="selectedFacilityDateIsToday" :class="{ warn: selectedFacilityUsage.facilityStatus !== 'RUNNING' }">
                  {{ statusLabel(selectedFacilityUsage.facilityStatus) }}
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
                    <strong>{{ formatNumber(facilityTodayUsage, selectedEnergyPrecision) }} <small>{{ selectedEnergyMeta.unit }}</small></strong>
                  </div>
                </article>
                <article>
                  <span class="round-icon trend">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path :d="trendIconPath"></path>
                    </svg>
                  </span>
                  <div>
                    <p>전일 사용량</p>
                    <strong>{{ formatNumber(selectedFacilityUsage.yesterdayUsageKwh, selectedEnergyPrecision) }} <small>{{ selectedEnergyMeta.unit }}</small></strong>
                    <small :class="trendClass(selectedFacilityUsage.todayVsYesterdayRate)">
                      전일 대비 {{ trendPrefix(selectedFacilityUsage.todayVsYesterdayRate) }}{{ formatNumber(selectedFacilityUsage.todayVsYesterdayRate) }}%
                    </small>
                  </div>
                </article>
                <article>
                  <span class="round-icon storage">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path :d="storageIconPath"></path>
                    </svg>
                  </span>
                  <div>
                    <p>월 평균 사용량</p>
                    <strong>{{ formatNumber(selectedFacilityUsage.monthlyAverageKwh, selectedEnergyPrecision) }} <small>{{ selectedEnergyMeta.unit }}</small></strong>
                    <small :class="trendClass(selectedFacilityUsage.todayVsMonthlyAverageRate)">
                      월 평균 대비 {{ trendPrefix(selectedFacilityUsage.todayVsMonthlyAverageRate) }}{{ formatNumber(selectedFacilityUsage.todayVsMonthlyAverageRate) }}%
                    </small>
                  </div>
                </article>
              </div>
            </template>
            <div v-else class="facility-detail-empty">
              <span class="facility-device-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path :d="equipmentIconPath"></path>
                </svg>
              </span>
              <strong>설비를 선택하세요</strong>
              <p>왼쪽 설비 카드 중 하나를 클릭하면 설비 정보가 표시됩니다.</p>
            </div>
          </aside>
        </section>
      </section>
      <section v-else-if="activePage === 'peak'" class="page-stack peak-monitor-page">
        <section class="peak-filter-row">
          <label>
            조회일
            <input v-model="selectedPeakDate" type="date" />
          </label>
          <div class="peak-period-control">
            <span>집계 단위</span>
            <div class="segmented">
              <button
                v-for="option in peakPeriodOptions"
                :key="option.value"
                type="button"
                :class="{ active: selectedPeakPeriod === option.value }"
                @click="selectedPeakPeriod = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <button class="primary-button compact" type="button" @click="run(loadPeakDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">{{ peakLivePillLabel }}</span>
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
              <p>{{ selectedPeakPeriod === 'DAY' ? '15분 평균 전력' : `${peakPeriodLabel} 평균 전력` }}</p>
              <b>{{ formatNumber(peakMetrics.intervalAverageKw) }}<small> kW</small></b>
              <em>{{ selectedPeakPeriod === 'DAY' ? `구간 ${formatTime(peakMetrics.intervalAt)}` : peakPeriodRangeLabel }}</em>
            </div>
          </article>
          <article class="peak-kpi-card">
            <span class="peak-card-icon purple"><Bolt :size="22" /></span>
            <div>
              <p>{{ selectedPeakPeriod === 'DAY' ? '15분 최대 전력' : `${peakPeriodLabel} 최대 전력` }}</p>
              <b>{{ formatNumber(peakMetrics.intervalMaxKw) }}<small> kW</small></b>
              <em>{{ peakPreviousPeriodLabel }} {{ formatNumber(peakMetrics.previousDayAverageRate) }}%</em>
            </div>
          </article>
        </section>

        <section class="peak-view-tabs" aria-label="피크 전력 보기">
          <button
            v-for="option in peakViewOptions"
            :key="option.value"
            type="button"
            :class="{ active: activePeakView === option.value }"
            @click="activePeakView = option.value"
          >
            {{ option.label }}
          </button>
        </section>

        <section v-if="activePeakView === 'comparison'" class="peak-comparison-grid">
          <article class="panel peak-comparison-panel">
            <div class="panel-title inline">
              <h2>공장별 피크 전력 비교</h2>
              <span>{{ peakComparisonSubtitle }}</span>
            </div>
            <div class="peak-plant-bars">
              <button
                v-for="plant in peakComparisonRows"
                :key="plant.plantId"
                type="button"
                :class="{ active: plant.active, danger: plant.exceeded }"
                @click="selectedPlantId = plant.plantId"
              >
                <b>{{ plant.rank }}</b>
                <span>{{ plant.plantName }}</span>
                <i><em :style="{ width: plant.barWidth }"></em></i>
                <strong>{{ formatNumber(plant.periodPeakKw, 0) }} kW</strong>
                <small>{{ formatNumber(plant.peakUsageRate) }}%</small>
              </button>
            </div>
          </article>

          <article class="panel peak-selected-plant-panel">
            <div class="panel-title inline">
              <h2>선택 공장 요약</h2>
              <Factory :size="20" />
            </div>
            <div class="peak-selected-summary">
              <strong>{{ selectedPeakPlantComparison?.plantName || selectedPlant?.name || '-' }}</strong>
              <b>{{ formatNumber(selectedPeakPlantComparison?.periodPeakKw, 0) }}<small> kW</small></b>
              <span>평균 {{ formatNumber(selectedPeakPlantComparison?.periodAverageKw, 0) }} kW</span>
              <span>기준 {{ formatNumber(selectedPeakPlantComparison?.thresholdKw, 0) }} kW</span>
              <em :class="{ up: Number(selectedPeakPlantComparison?.peakUsageRate || 0) >= 100 }">
                사용률 {{ formatNumber(selectedPeakPlantComparison?.peakUsageRate) }}%
              </em>
            </div>
          </article>
        </section>

        <section v-else-if="activePeakView === 'detail'" class="peak-content-grid">
          <article class="panel peak-gauge-panel">
            <div class="panel-title inline">
              <h2>피크 전력 현황</h2>
              <span>단위: kW</span>
            </div>
            <div class="peak-gauge" :style="peakGaugeStyle">
              <div>
                <strong>{{ formatNumber(peakMetrics.currentKw, 0) }} kW</strong>
                <span>사용률 {{ formatNumber(peakMetrics.peakUsageRate) }}% / 기준 {{ formatNumber(peakMetrics.thresholdKw, 0) }} kW</span>
              </div>
            </div>
            <div class="peak-scale">
              <span>0</span><span>50</span><span>100</span><span>125</span>
            </div>
          </article>

          <article class="panel peak-chart-panel">
            <div class="panel-title inline">
              <h2>{{ peakTrendTitle }}</h2>
              <div class="peak-legend">
                <span><i class="avg"></i>{{ selectedPeakPeriod === 'DAY' ? '15분 평균' : '일 평균' }}</span>
                <span><i class="max"></i>{{ selectedPeakPeriod === 'DAY' ? '15분 최대' : '일 최대' }}</span>
                <span><i class="limit"></i>피크 기준</span>
              </div>
            </div>
            <svg class="peak-line-chart" viewBox="0 0 720 240" role="img" :aria-label="peakTrendTitle">
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
              <span>{{ peakPointLabel(peakTrendPoints[0]?.measuredAt) }}</span>
              <span>{{ formatNumber(peakTrendMax, 0) }} kW</span>
              <span>{{ peakPointLabel(peakTrendPoints.at(-1)?.measuredAt) }}</span>
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

        <article v-else-if="activePeakView === 'anomaly'" class="panel table-panel peak-history-panel">
          <div class="panel-title inline">
            <h2>기준 초과 날짜</h2>
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
                <td colspan="7">{{ peakPeriodRangeLabel }} 기간에 피크 초과 이력이 없습니다.</td>
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
          <div class="utility-period-control">
            <span>집계 단위</span>
            <div class="segmented">
              <button
                v-for="option in utilityPeriodOptions"
                :key="option.value"
                type="button"
                :class="{ active: selectedUtilityPeriod === option.value }"
                @click="selectedUtilityPeriod = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <button class="primary-button compact" type="button" @click="run(loadUtilityDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">{{ utilityLivePillLabel }}</span>
        </section>

        <section class="utility-kpi-grid">
          <article class="utility-kpi-card gas">
            <span class="utility-card-icon"><Flame :size="23" /></span>
            <div>
              <p>가스 {{ utilityPeriodLabel }} 사용량</p>
              <b>{{ formatNumber(utilityMetrics.gasUsageM3) }}<small> m3</small></b>
              <em :class="trendClass(utilityMetrics.gasChangeRate)">
                {{ utilityPreviousPeriodLabel }} {{ trendPrefix(utilityMetrics.gasChangeRate) }}{{ formatNumber(utilityMetrics.gasChangeRate) }}%
              </em>
            </div>
          </article>
          <article class="utility-kpi-card gas">
            <span class="utility-card-icon"><Gauge :size="23" /></span>
            <div>
              <p>가스 적산량</p>
              <b>{{ formatNumber(utilityMetrics.gasTotalM3) }}<small> m3</small></b>
              <em>이번 달 누적 사용량</em>
            </div>
          </article>
          <article class="utility-kpi-card water">
            <span class="utility-card-icon"><Droplets :size="23" /></span>
            <div>
              <p>용수 {{ utilityPeriodLabel }} 사용량</p>
              <b>{{ formatNumber(utilityMetrics.waterUsageTon) }}<small> ton</small></b>
              <em :class="trendClass(utilityMetrics.waterChangeRate)">
                {{ utilityPreviousPeriodLabel }} {{ trendPrefix(utilityMetrics.waterChangeRate) }}{{ formatNumber(utilityMetrics.waterChangeRate) }}%
              </em>
            </div>
          </article>
          <article class="utility-kpi-card water">
            <span class="utility-card-icon"><Gauge :size="23" /></span>
            <div>
              <p>용수 적산량</p>
              <b>{{ formatNumber(utilityMetrics.waterTotalTon) }}<small> ton</small></b>
              <em>이번 달 누적 사용량</em>
            </div>
          </article>
        </section>

        <section class="utility-view-tabs" aria-label="가스 용수 보기">
          <button
            v-for="option in utilityViewOptions"
            :key="option.value"
            type="button"
            :class="{ active: activeUtilityView === option.value }"
            @click="activeUtilityView = option.value"
          >
            {{ option.label }}
          </button>
        </section>

        <section v-if="activeUtilityView === 'comparison'" class="utility-comparison-grid">
          <article class="panel utility-comparison-panel">
            <div class="panel-title inline">
              <h2>공장별 가스/용수 사용량 비교</h2>
              <span>{{ utilityPeriodRangeLabel }}</span>
            </div>
            <div class="utility-plant-bars">
              <button
                v-for="plant in utilityComparisonRows"
                :key="plant.plantId"
                type="button"
                :class="{ active: plant.active }"
                @click="selectedPlantId = plant.plantId"
              >
                <span>{{ plant.plantName }}</span>
                <div>
                  <i class="gas"><em :style="{ width: plant.gasBarWidth }"></em></i>
                  <strong>{{ formatNumber(plant.gasUsageM3, 0) }} m3</strong>
                </div>
                <div>
                  <i class="water"><em :style="{ width: plant.waterBarWidth }"></em></i>
                  <strong>{{ formatNumber(plant.waterUsageTon, 0) }} ton</strong>
                </div>
              </button>
            </div>
          </article>

          <article class="panel utility-selected-plant-panel">
            <div class="panel-title inline">
              <h2>선택 공장 요약</h2>
              <Factory :size="20" />
            </div>
            <div class="utility-selected-summary">
              <strong>{{ selectedUtilityPlantComparison?.plantName || selectedPlant?.name || '-' }}</strong>
              <article>
                <span class="gas"><Flame :size="18" /> 가스</span>
                <b>{{ formatNumber(selectedUtilityPlantComparison?.gasUsageM3, 0) }}<small> m3</small></b>
                <em>전체 비중 {{ formatNumber(selectedUtilityPlantComparison?.gasShareRate) }}%</em>
              </article>
              <article>
                <span class="water"><Droplets :size="18" /> 용수</span>
                <b>{{ formatNumber(selectedUtilityPlantComparison?.waterUsageTon, 0) }}<small> ton</small></b>
                <em>전체 비중 {{ formatNumber(selectedUtilityPlantComparison?.waterShareRate) }}%</em>
              </article>
            </div>
          </article>
        </section>

        <section v-else-if="activeUtilityView === 'detail'" class="utility-chart-grid">
          <article class="panel utility-chart-panel gas">
            <div class="panel-title inline">
              <h2>가스 {{ utilityTrendTitle }}</h2>
              <span>단위: m3 · {{ utilityPeriodRangeLabel }}</span>
            </div>
            <div class="utility-line-chart gas">
              <svg viewBox="0 0 720 260" role="img" :aria-label="`가스 ${utilityTrendTitle}`">
                <defs>
                  <linearGradient id="gasLineFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stop-color="#f97316" stop-opacity="0.28" />
                    <stop offset="100%" stop-color="#f97316" stop-opacity="0.03" />
                  </linearGradient>
                </defs>
                <g class="utility-grid-lines">
                  <line v-for="row in 5" :key="`gas-grid-${row}`" x1="26" x2="694" :y1="row * 40" :y2="row * 40" />
                </g>
                <path class="utility-area gas" :d="utilityGasAreaPath" />
                <path class="utility-line gas" :d="utilityGasLinePath" />
                <g
                  v-for="point in utilityGasPoints"
                  :key="`gas-point-${point.measuredAt}`"
                  @mouseenter="showUtilityTooltip($event, point, 'gas', '가스 사용량', 'm3')"
                  @mousemove="moveUtilityTooltip"
                  @mouseleave="hideUtilityTooltip"
                >
                  <circle class="utility-dot gas" :cx="point.x" :cy="point.y" r="4"></circle>
                </g>
                <g class="utility-x-axis">
                  <template v-for="(point, index) in utilityGasPoints" :key="`gas-axis-${point.measuredAt}`">
                    <text v-if="index % 2 === 0 || index === utilityGasPoints.length - 1" :x="point.x" y="246">
                      {{ utilityPointLabel(point.measuredAt) }}
                    </text>
                  </template>
                </g>
              </svg>
              <div
                v-if="utilityTooltip?.chart === 'gas'"
                class="utility-chart-tooltip"
                :style="{ left: `${utilityTooltip.x}px`, top: `${utilityTooltip.y}px` }"
              >
                <b>{{ utilityTooltip.label }}</b>
                <span>{{ utilityTooltip.time }} · {{ utilityTooltip.value }} {{ utilityTooltip.unit }}</span>
              </div>
            </div>
          </article>

          <article class="panel utility-chart-panel water">
            <div class="panel-title inline">
              <h2>용수 {{ utilityTrendTitle }}</h2>
              <span>단위: ton · {{ utilityPeriodRangeLabel }}</span>
            </div>
            <div class="utility-line-chart water">
              <svg viewBox="0 0 720 260" role="img" :aria-label="`용수 ${utilityTrendTitle}`">
                <defs>
                  <linearGradient id="waterLineFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stop-color="#06b6d4" stop-opacity="0.28" />
                    <stop offset="100%" stop-color="#06b6d4" stop-opacity="0.03" />
                  </linearGradient>
                </defs>
                <g class="utility-grid-lines">
                  <line v-for="row in 5" :key="`water-grid-${row}`" x1="26" x2="694" :y1="row * 40" :y2="row * 40" />
                </g>
                <path class="utility-area water" :d="utilityWaterAreaPath" />
                <path class="utility-line water" :d="utilityWaterLinePath" />
                <g
                  v-for="point in utilityWaterPoints"
                  :key="`water-point-${point.measuredAt}`"
                  @mouseenter="showUtilityTooltip($event, point, 'water', '용수 사용량', 'ton')"
                  @mousemove="moveUtilityTooltip"
                  @mouseleave="hideUtilityTooltip"
                >
                  <circle class="utility-dot water" :cx="point.x" :cy="point.y" r="4"></circle>
                </g>
                <g class="utility-x-axis">
                  <template v-for="(point, index) in utilityWaterPoints" :key="`water-axis-${point.measuredAt}`">
                    <text v-if="index % 2 === 0 || index === utilityWaterPoints.length - 1" :x="point.x" y="246">
                      {{ utilityPointLabel(point.measuredAt) }}
                    </text>
                  </template>
                </g>
              </svg>
              <div
                v-if="utilityTooltip?.chart === 'water'"
                class="utility-chart-tooltip"
                :style="{ left: `${utilityTooltip.x}px`, top: `${utilityTooltip.y}px` }"
              >
                <b>{{ utilityTooltip.label }}</b>
                <span>{{ utilityTooltip.time }} · {{ utilityTooltip.value }} {{ utilityTooltip.unit }}</span>
              </div>
            </div>
          </article>
        </section>

        <section v-else-if="activeUtilityView === 'meters'" class="utility-bottom-grid">
          <article class="panel table-panel utility-meter-panel">
            <div class="panel-title inline">
              <h2>계측기 상태 조회</h2>
              <RadioReceiver :size="20" />
            </div>
            <div class="utility-meter-toolbar">
              <input v-model="utilityMeterSearch" type="search" placeholder="계측기명, 설비명, 상태 검색" />
              <span>{{ filteredUtilityMeterStatuses.length }} / {{ utilityMeterStatuses.length }}</span>
            </div>
            <div class="utility-meter-scroll">
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
                  <tr v-for="meter in filteredUtilityMeterStatuses" :key="`${meter.meterType}-${meter.facilityId}`">
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
                  <tr v-if="!filteredUtilityMeterStatuses.length">
                    <td colspan="6">검색 결과가 없습니다.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>

          <article class="panel utility-pattern-panel">
            <div class="panel-title inline">
              <h2>사용 패턴 분석</h2>
              <span>{{ utilityPatternRangeLabel }}</span>
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
            조회월
            <input v-model="selectedEsgMonth" type="month" />
          </label>
          <button class="primary-button compact" type="button" @click="run(loadEsgDashboard)">
            <Search :size="17" /> 조회
          </button>
          <span class="live-pill">월간 환경 점수 0-10</span>
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
              <h2>통합 환경 항목 비교</h2>
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
      </section>

      <section v-else-if="activePage === 'chatbot'" class="page-stack chatbot-page">
        <section class="chatbot-layout">
          <article class="panel chatbot-conversation-panel">
            <div class="panel-title inline">
              <h2>AI 상담 대화</h2>
              <span>{{ selectedPlant?.name || '-' }}</span>
            </div>

            <div class="chatbot-message-list">
              <article v-for="message in chatbotMessagesChronological" :key="message.id" class="chatbot-message">
                <div class="chatbot-bubble user">
                  <span>질문</span>
                  <p>{{ message.question }}</p>
                </div>
                <div class="chatbot-bubble assistant">
                  <span>
                    <Bot :size="16" />
                    {{ message.plantName || selectedPlant?.name || 'SCADA AI' }}
                    <small>{{ formatDateTime(message.createdAt) }}</small>
                  </span>
                  <p>{{ message.answer }}</p>
                </div>
              </article>

              <article v-if="!chatbotMessagesChronological.length" class="chatbot-empty">
                <Bot :size="36" />
                <strong>아직 질문이 없습니다</strong>
                <p>사업장을 선택한 뒤 에너지 사용량, ESG 점수, 알람 상태를 질문해 보세요.</p>
              </article>
            </div>

            <form class="chatbot-input-row" @submit.prevent="submitChatbotQuestion">
              <textarea
                v-model="chatbotQuestion"
                rows="3"
                placeholder="예: 오늘 전력 사용량과 ESG 상태를 요약해줘"
                :disabled="chatbotSending || !selectedPlantId"
              ></textarea>
              <button
                class="primary-button compact"
                type="submit"
                :disabled="chatbotSending || !chatbotQuestion.trim() || !selectedPlantId"
              >
                <Send :size="17" /> {{ chatbotSending ? '전송 중' : '전송' }}
              </button>
            </form>
          </article>

          <aside class="panel chatbot-guide-panel">
            <div class="chatbot-guide-icon">
              <Bot :size="30" />
            </div>
            <h2>질문 예시</h2>
            <p>선택한 사업장의 최신 집계 데이터를 기준으로 답변합니다.</p>
            <div class="chatbot-suggestion-list">
              <button
                v-for="question in chatbotSuggestedQuestions"
                :key="question"
                type="button"
                @click="useSuggestedQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </aside>
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
          <div class="alarm-table-scroll">
            <table>
              <thead><tr><th>발생 시각</th><th>설비</th><th>레벨</th><th>메시지</th><th>값</th><th>기준</th><th>상태</th><th>관리</th></tr></thead>
              <tbody>
                <tr v-for="alarm in state.alarms" :key="alarm.id">
                  <td>{{ formatDateTime(alarm.occurredAt) }}</td>
                  <td>{{ alarm.facilityName }}</td>
                  <td>{{ alarm.alarmLevel }}</td>
                  <td>{{ alarm.message }}</td>
                  <td>{{ formatNumber(alarm.value) }}</td>
                  <td>{{ formatNumber(alarm.thresholdValue) }}</td>
                  <td><span :class="['badge', alarm.status === 'RESOLVED' ? 'ok' : 'warn']">{{ statusLabel(alarm.status) }}</span></td>
                  <td>
                    <div class="alarm-action-cell">
                      <button class="light-button compact" type="button" :disabled="alarm.status === 'RESOLVED'" @click="resolveAlarm(alarm.id)">처리</button>
                      <button class="danger-button compact" type="button" :disabled="alarm.status !== 'RESOLVED'" @click="deleteAlarm(alarm.id)">삭제</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

