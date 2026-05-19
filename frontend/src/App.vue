<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { importLibrary, setOptions } from '@googlemaps/js-api-loader'
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
  LogOut,
  RadioReceiver,
  ReceiptText,
  Search,
  Send,
  SunMedium,
  Trash2,
  UserPlus,
  Zap,
} from 'lucide-vue-next'
import { api, clearTokens, getAccessToken, saveTokens } from './api'
import brandLogoUrl from './assets/6esgpulse-logo.png'

const appMode = ref(getAccessToken() ? 'detail' : 'login')
const PLATFORM_NAME = '6ESGPulse'
const PLATFORM_AI_NAME = '6ESGPulse AI'
const PLATFORM_TAGLINE = '6개 사업장의 에너지와 ESG 상태를 하나로 읽는 통합 관제 플랫폼'
const BRAND_MARK_URL = '/favicon.svg'
const SCADA_EXTERNAL_URL = import.meta.env.VITE_SMWP_SCADA_URL || 'http://192.168.0.100:11005/?Pro=ksj_260430#%EC%98%88%EC%8B%9C1'
const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
const activePage = ref('dashboard')
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
const peakDashboardLoading = ref(false)
const peakBillError = ref('')
const selectedUtilityDate = ref(formatDateInput(new Date()))
const selectedUtilityPeriod = ref('DAY')
const activeUtilityView = ref('comparison')
const utilityDashboardLoading = ref(false)
const utilityMeterSearch = ref('')
const utilityTooltip = ref(null)
const chatbotQuestion = ref('')
const chatbotSending = ref(false)
const chatbotDeletingId = ref(null)
const userCreating = ref(false)
const usersLoading = ref(false)
const selectedEsgMonth = ref(formatMonthInput(new Date()))
const selectedEsgFrom = ref(formatMonthStartInput(new Date()))
const selectedEsgTo = ref(formatMonthEndInput(new Date()))
const esgDashboardLoading = ref(false)
const dashboardMapElement = ref(null)
const dashboardMapState = reactive({
  status: GOOGLE_MAPS_API_KEY ? 'idle' : 'missing-key',
  message: GOOGLE_MAPS_API_KEY ? 'Loading map.' : 'Google Maps API key is missing.',
})
const esgMapElement = ref(null)
const esgMapState = reactive({
  status: GOOGLE_MAPS_API_KEY ? 'idle' : 'missing-key',
  message: GOOGLE_MAPS_API_KEY ? 'Loading map.' : 'Google Maps API key is missing.',
})
const alarmSortOrder = ref('desc')
const alarmsLoading = ref(false)
const syncingSelection = ref(false)
const realtimeNow = ref(Date.now())
let energySocket = null
let energySocketReconnectTimer = null
let realtimeTickTimer = null
let peakRefreshTimer = null
let utilityRefreshTimer = null
let googleMapsConfigured = false
let dashboardGoogleMap = null
let dashboardGoogleMapContainer = null
let dashboardGoogleOverlays = []
let esgGoogleMap = null
let esgGoogleMapContainer = null
let esgGoogleOverlays = []
let lastPeakRefreshAt = 0
let lastUtilityRefreshAt = 0
let lastAlarmRefreshAt = 0
let peakDashboardRequestId = 0
let utilityDashboardRequestId = 0
let esgDashboardRequestId = 0
let usersRequestId = 0
let alarmsRequestId = 0
const LIVE_SERIES_LIMIT = 120
const LIVE_STALE_MS = 5000
const LIVE_DASHBOARD_REFRESH_MS = 10000
const PLANT_PEAK_THRESHOLD_KW = 8500
const DEFAULT_PLANT_LOCATIONS = {
  1: { plantName: '기아 화성', latitude: 37.021559, longitude: 126.783111 },
  2: { plantName: '기아 광명', latitude: 37.430203, longitude: 126.878945 },
  3: { plantName: '기아 광주', latitude: 35.160108, longitude: 126.882618 },
  4: { plantName: '현대 울산', latitude: 35.538377, longitude: 129.376513 },
  5: { plantName: '현대 아산', latitude: 36.838508, longitude: 126.881593 },
  6: { plantName: '현대 전주', latitude: 35.956543, longitude: 127.134506 },
}
const DASHBOARD_PLANT_LAYOUT = {
  1: { side: 'left', card: { x: 4, y: 20 } },
  2: { side: 'left', card: { x: 4, y: 44 } },
  3: { side: 'left', card: { x: 4, y: 68 } },
  4: { side: 'right', card: { x: 82, y: 24 } },
  5: { side: 'right', card: { x: 82, y: 48 } },
  6: { side: 'right', card: { x: 82, y: 72 } },
}
const DASHBOARD_CARD_WIDTH = 14
const DASHBOARD_CARD_HEIGHT = 8.8
const GOOGLE_MAP_STYLES = [
  { featureType: 'administrative', elementType: 'labels.text.fill', stylers: [{ color: '#4b647f' }] },
  { featureType: 'administrative.province', elementType: 'geometry.stroke', stylers: [{ color: '#b7c8dd' }] },
  { featureType: 'landscape', elementType: 'geometry', stylers: [{ color: '#eef5f2' }] },
  { featureType: 'landscape.man_made', elementType: 'geometry', stylers: [{ color: '#edf2f7' }] },
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#ffffff' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#d6e1ec' }] },
  { featureType: 'road', elementType: 'labels.icon', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#cfeff3' }] },
]
const GOOGLE_MAP_OPTIONS = {
  styles: GOOGLE_MAP_STYLES,
  disableDefaultUI: true,
  zoomControl: true,
  mapTypeControl: false,
  streetViewControl: false,
  fullscreenControl: true,
  clickableIcons: false,
  gestureHandling: 'greedy',
}
const MAP_CARD_LANE_OFFSETS = [-44, -22, 0, 22, 44]
const liveEnergyByFacility = reactive(new Map())
const liveEnergyBaselineByFacility = reactive(new Map())
const liveEnergySeriesByFacility = reactive(new Map())
const liveEnergySeenAtByFacility = reactive(new Map())
const alarmActiveKeywords = reactive({})
const nowLabel = computed(() =>
  new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(realtimeNow.value)),
)

const loginForm = reactive({
  email: '',
  password: '',
})

const userCreateForm = reactive({
  email: '',
  password: '',
  name: '',
  phone: '',
  plantId: '',
  role: 'VIEWER',
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
  peakBillEstimate: null,
  utilityDashboard: null,
  esgDashboard: null,
  facilityDetail: null,
  facilityLineUsages: [],
  alarms: [],
  esgScores: [],
  users: [],
  chatbotMessages: [],
})

const solutionViewer = reactive({
  open: false,
  plant: null,
  url: '',
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
  { id: 'dashboard', label: '대시보드', icon: 'D' },
  { id: 'facility', label: '설비 조회', icon: 'F' },
  { id: 'peak', label: '피크 전력', icon: 'P' },
  { id: 'utility', label: '가스/용수', icon: 'U' },
  { id: 'esg', label: 'ESG 평가', icon: 'E' },
  { id: 'chatbot', label: 'AI 상담', icon: 'AI' },
  { id: 'users', label: '사용자 관리', icon: 'M' },
  { id: 'alarms', label: '알람', icon: 'A' },
]

const validRoutes = ['dashboard', 'facility', 'peak', 'utility', 'esg', 'chatbot', 'users', 'alarms']
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
    dashboard: ['대시보드', '사업장 환경 종합 현황도'],
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
  '이번 달 전기요금 줄일 수 있는 요금제가 있어?',
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
    const hasTodayMeasuredData = formatDate(facility.latestMeasuredAt) === formatDateInput(new Date())
    const liveIsToday = formatDate(live?.measuredAt) === formatDateInput(new Date())
    const baseUsage = hasTodayMeasuredData ? Number(facility.todayUsageKwh || 0) : 0
    if (!live || !liveIsToday) {
      const monthlyAverage = Number(facility.monthlyAverageKwh || 0)
      return {
        ...facility,
        todayUsageKwh: baseUsage,
        todayVsMonthlyAverageRate: calculateChangeRate(baseUsage, monthlyAverage),
        latestMeasuredAt: hasTodayMeasuredData ? facility.latestMeasuredAt : null,
      }
    }

    const liveUsage = dailyLiveUsage(
      live,
      liveEnergyBaselineByFacility.get(key),
      selectedEnergyMetricKeys.value.camel,
      selectedEnergyMetricKeys.value.snake,
    )
    const displayUsage = baseUsage + Number(liveUsage || 0) + estimatedRealtimeUsage(facility, live)
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

function plantIdentifier(plant) {
  const value = plant?.plantId ?? plant?.plant_id ?? plant?.id
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

async function loadGoogleMapsLibrary() {
  if (!GOOGLE_MAPS_API_KEY) {
    throw new Error('Google Maps API key is missing.')
  }

  if (!googleMapsConfigured) {
    setOptions({
      key: GOOGLE_MAPS_API_KEY,
      v: 'weekly',
      language: 'ko',
      region: 'KR',
    })
    googleMapsConfigured = true
  }

  const [mapsLibrary, coreLibrary] = await Promise.all([
    importLibrary('maps'),
    importLibrary('core'),
  ])

  return {
    ...mapsLibrary,
    ...coreLibrary,
  }
}

function clearDashboardMapMarkers() {
  dashboardGoogleOverlays.forEach((overlay) => overlay.setMap(null))
  dashboardGoogleOverlays = []
}

function resetDashboardGoogleMap() {
  clearDashboardMapMarkers()
  dashboardGoogleMap = null
  dashboardGoogleMapContainer = null
}

function clearEsgMapMarkers() {
  esgGoogleOverlays.forEach((overlay) => overlay.setMap(null))
  esgGoogleOverlays = []
}

function resetEsgGoogleMap() {
  clearEsgMapMarkers()
  esgGoogleMap = null
  esgGoogleMapContainer = null
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function mapCardLaneOffset(index) {
  return MAP_CARD_LANE_OFFSETS[index % MAP_CARD_LANE_OFFSETS.length]
}

function mapOverlaySide(plant, fallbackCenterLng = 127.9) {
  if (plant?.side === 'left' || plant?.side === 'right') {
    return plant.side
  }
  return Number(plant?.longitude) < fallbackCenterLng ? 'left' : 'right'
}

function createGoogleMapOverlay(maps, map, position, content, options = {}) {
  const { zIndex = 1, laneOffset = 0, side = null } = options
  const resolvedSide = side || 'right'
  const connectorX = resolvedSide === 'left' ? -46 : 46
  const connectorAngle = Math.atan2(laneOffset, connectorX) * 180 / Math.PI
  const connectorWidth = Math.hypot(connectorX, laneOffset)

  class PlantOverlay extends maps.OverlayView {
    constructor() {
      super()
      this.position = new maps.LatLng(position.lat, position.lng)
      this.element = content
    }

    onAdd() {
      const panes = this.getPanes()
      this.element.style.position = 'absolute'
      this.element.style.zIndex = String(zIndex)
      this.element.style.setProperty('--card-y', `${laneOffset}px`)
      this.element.style.setProperty('--connector-angle', `${connectorAngle}deg`)
      this.element.style.setProperty('--connector-width', `${connectorWidth}px`)
      panes.overlayMouseTarget.appendChild(this.element)
    }

    draw() {
      const point = this.getProjection()?.fromLatLngToDivPixel(this.position)
      if (!point) {
        return
      }
      this.element.classList.toggle('left', resolvedSide === 'left')
      this.element.classList.toggle('right', resolvedSide !== 'left')
      this.element.style.left = `${point.x}px`
      this.element.style.top = `${point.y}px`
      this.element.style.transform = 'translate(-50%, -50%)'
    }

    onRemove() {
      this.element.remove()
    }
  }

  const overlay = new PlantOverlay()
  overlay.setMap(map)
  return overlay
}

function htmlToElement(html) {
  const wrapper = document.createElement('div')
  wrapper.innerHTML = html.trim()
  return wrapper.firstElementChild
}

function mapCenterLongitude(plants, fallback = 127.9) {
  const longitudes = plants
    .map((plant) => Number(plant.longitude))
    .filter(Number.isFinite)
  if (!longitudes.length) {
    return fallback
  }
  return (Math.min(...longitudes) + Math.max(...longitudes)) / 2
}

function fitGoogleMapToPlants(map, maps, plants, fallbackZoom = 7) {
  if (!plants.length) {
    return
  }

  if (plants.length === 1) {
    map.setCenter({
      lat: Number(plants[0].latitude),
      lng: Number(plants[0].longitude),
    })
    map.setZoom(fallbackZoom)
    return
  }

  const bounds = new maps.LatLngBounds()
  plants.forEach((plant) => {
    bounds.extend({
      lat: Number(plant.latitude),
      lng: Number(plant.longitude),
    })
  })
  map.fitBounds(bounds, {
    top: 56,
    right: 56,
    bottom: 56,
    left: 56,
  })
}

function dashboardMapOverlayContent(plant) {
  const active = Number(plant.plantId) === Number(selectedPlantId.value)
  const gradeClass = esgGradeClass(plant.grade)
  return `
    <button type="button" class="google-plant-marker dashboard ${active ? 'active' : ''}">
      <span class="google-plant-pin ${gradeClass}"></span>
      <span class="google-plant-card">
        <b>${escapeHtml(plant.plantName || '-')}</b>
        <strong class="${gradeClass}">${escapeHtml(plant.grade || '-')}</strong>
        <em>${escapeHtml(formatNumber(plant.totalScore))} pt</em>
      </span>
    </button>
  `
}

function esgMapOverlayContent(plant) {
  const active = Number(plant.plantId) === Number(selectedEsgPlant.value?.plantId)
  const gradeClass = esgGradeClass(plant.grade)
  return `
    <button type="button" class="google-plant-marker esg ${active ? 'active' : ''}">
      <span class="google-plant-pin ${gradeClass}"></span>
      <span class="google-plant-card esg-map-label">
        <strong class="${gradeClass}">${escapeHtml(plant.grade || '-')}</strong>
        <span>${escapeHtml(plant.plantName || '-')}</span>
      </span>
    </button>
  `
}

async function renderDashboardGoogleMap() {
  if (activePage.value !== 'dashboard') {
    return
  }

  await nextTick()
  const plants = dashboardPlants.value

  if (!GOOGLE_MAPS_API_KEY) {
    dashboardMapState.status = 'missing-key'
    dashboardMapState.message = 'Google Maps API key is missing.'
    return
  }

  const container = dashboardMapElement.value
  if (!container) {
    return
  }

  if (!plants.length) {
    dashboardMapState.status = 'empty'
    dashboardMapState.message = 'No plant coordinates were found.'
    clearDashboardMapMarkers()
    return
  }

  dashboardMapState.status = 'loading'
    dashboardMapState.message = 'Loading map.'

  try {
    const maps = await loadGoogleMapsLibrary()
    if (dashboardGoogleMapContainer !== container) {
      clearDashboardMapMarkers()
      dashboardGoogleMap = null
      dashboardGoogleMapContainer = container
    }

    if (!dashboardGoogleMap) {
      dashboardGoogleMap = new maps.Map(container, {
        ...GOOGLE_MAP_OPTIONS,
        center: { lat: 36.4, lng: 127.9 },
        zoom: plants.length > 1 ? 7 : 10,
      })
    } else {
      dashboardGoogleMap.setOptions({
        ...GOOGLE_MAP_OPTIONS,
        restriction: null,
      })
      dashboardGoogleMap.setCenter({ lat: 36.4, lng: 127.9 })
    }

    clearDashboardMapMarkers()
    plants.forEach((plant, index) => {
      const markerElement = htmlToElement(dashboardMapOverlayContent(plant))
      markerElement.addEventListener('click', () => openPlantSolution(plant))
      const overlay = createGoogleMapOverlay(
        maps,
        dashboardGoogleMap,
        { lat: Number(plant.latitude), lng: Number(plant.longitude) },
        markerElement,
        {
          zIndex: Number(plant.plantId) === Number(selectedPlantId.value) ? 4 : 2,
          laneOffset: mapCardLaneOffset(index),
          side: mapOverlaySide(plant),
        },
      )
      dashboardGoogleOverlays.push(overlay)
    })

    fitGoogleMapToPlants(dashboardGoogleMap, maps, plants, plants.length > 1 ? 7 : 10)
    dashboardMapState.status = 'ready'
    dashboardMapState.message = ''
  } catch (error) {
    console.error(error)
    clearDashboardMapMarkers()
    dashboardMapState.status = 'error'
    dashboardMapState.message = 'Google Maps could not be loaded.'
  }
}

async function renderEsgGoogleMap() {
  if (activePage.value !== 'esg') {
    return
  }

  await nextTick()
  const plants = esgMapPlants.value

  if (!GOOGLE_MAPS_API_KEY) {
    esgMapState.status = 'missing-key'
    esgMapState.message = 'Google Maps API key is missing.'
    return
  }

  const container = esgMapElement.value
  if (!container) {
    return
  }

  if (!plants.length) {
    esgMapState.status = 'empty'
    esgMapState.message = 'No plant coordinates were found.'
    clearEsgMapMarkers()
    return
  }

  esgMapState.status = 'loading'
    esgMapState.message = 'Loading map.'

  try {
    const maps = await loadGoogleMapsLibrary()
    const center = { lat: 36.4, lng: 127.9 }

    if (esgGoogleMapContainer !== container) {
      clearEsgMapMarkers()
      esgGoogleMap = null
      esgGoogleMapContainer = container
    }

    if (!esgGoogleMap) {
      esgGoogleMap = new maps.Map(container, {
        ...GOOGLE_MAP_OPTIONS,
        center,
        zoom: plants.length > 1 ? 7 : 10,
      })
    } else {
      esgGoogleMap.setOptions(GOOGLE_MAP_OPTIONS)
      esgGoogleMap.setCenter(center)
    }

    clearEsgMapMarkers()
    const centerLongitude = mapCenterLongitude(plants)
    plants.forEach((plant, index) => {
      const markerElement = htmlToElement(esgMapOverlayContent(plant))
      markerElement.addEventListener('click', () => {
        selectedPlantId.value = plant.plantId
      })
      const overlay = createGoogleMapOverlay(
        maps,
        esgGoogleMap,
        { lat: Number(plant.latitude), lng: Number(plant.longitude) },
        markerElement,
        {
          zIndex: Number(plant.plantId) === Number(selectedEsgPlant.value?.plantId) ? 4 : 2,
          laneOffset: mapCardLaneOffset(index),
          side: mapOverlaySide(plant, centerLongitude),
        },
      )
      esgGoogleOverlays.push(overlay)
    })

    fitGoogleMapToPlants(esgGoogleMap, maps, plants, plants.length > 1 ? 7 : 10)
    esgMapState.status = 'ready'
    esgMapState.message = ''
  } catch (error) {
    console.error(error)
    clearEsgMapMarkers()
    esgMapState.status = 'error'
    esgMapState.message = 'Google Maps could not be loaded.'
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
  return peak ? Math.min(Math.round((peak / PLANT_PEAK_THRESHOLD_KW) * 100), 999) : 0
})

const alarmCount = computed(() => state.overview?.occurredAlarmCount ?? state.alarms.length)
const alarmTypeLabels = {
  PEAK: '피크',
  ELECTRICITY: '전기',
  GAS: '가스',
  WATER: '용수',
  FACILITY: '설비',
  ESG: 'ESG',
}
const occurredAlarms = computed(() =>
  state.alarms.filter((alarm) => (alarm.status || 'OCCURRED') === 'OCCURRED'),
)
const alarmPlantGroups = computed(() => {
  const plants = new Map()

  sortedAlarms(occurredAlarms.value).forEach((alarm) => {
    const plantId = alarm.plantId ?? selectedPlantId.value ?? 'all'
    const plantName = alarm.plantName || selectedPlant.value?.name || `사업장 ${plantId}`
    const plantKey = String(plantId)
    const keyword = alarmKeyword(alarm)

    if (!plants.has(plantKey)) {
      plants.set(plantKey, {
        key: plantKey,
        plantId,
        plantName,
        totalCount: 0,
        keywords: new Map(),
      })
    }

    const plant = plants.get(plantKey)
    plant.totalCount += 1

    if (!plant.keywords.has(keyword.key)) {
      plant.keywords.set(keyword.key, {
        ...keyword,
        alarms: [],
      })
    }

    plant.keywords.get(keyword.key).alarms.push(alarm)
  })

  return Array.from(plants.values())
    .map((plant) => {
      const keywordTabs = Array.from(plant.keywords.values())
        .map((keyword) => ({
          ...keyword,
          count: keyword.alarms.length,
          alarms: sortedAlarms(keyword.alarms),
        }))
        .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'ko-KR'))

      return {
        ...plant,
        keywordTabs,
      }
    })
    .sort((a, b) => String(a.plantName).localeCompare(String(b.plantName), 'ko-KR'))
})
const peakMetrics = computed(() => {
  const metrics = state.peakDashboard?.metrics || {}
  const live = selectedPlantLiveEnergy.value
  if (!live || selectedPeakPeriod.value !== 'DAY' || !selectedPeakDateIsToday.value) {
    return metrics
  }

  const currentKw = metricNumber(live, 'peakKw', 'peak_kw')
  const thresholdKw = Number(metrics.thresholdKw || PLANT_PEAK_THRESHOLD_KW)
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
    : Math.min(currentKw, Number(metrics.intervalAverageKw || currentKw * 0.96))
  const intervalMaxKw = recentPeaks.length
    ? Math.max(...recentPeaks)
    : Math.max(currentKw, Number(metrics.intervalMaxKw || currentKw))
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
const peakBillSummary = computed(() => {
  const bill = state.peakBillEstimate
  if (!bill) {
    return {
      hasEstimate: false,
      tariffName: '-',
      periodRangeLabel: peakPeriodRangeLabel.value,
      assumptions: [],
    }
  }

  const periodFrom = bill.periodFrom || bill.period_from || selectedPeakDate.value
  const periodTo = bill.periodTo || bill.period_to || selectedPeakDate.value
  const periodRangeLabel = periodFrom === periodTo ? formatDate(periodFrom) : `${formatDate(periodFrom)} - ${formatDate(periodTo)}`

  return {
    hasEstimate: true,
    tariffName: bill.tariffName || bill.tariff_name || '-',
    source: bill.source || '',
    periodRangeLabel,
    billingDemandKw: Number(bill.billingDemandKw || bill.billing_demand_kw || 0),
    billingPeakMeasuredAt: bill.billingPeakMeasuredAt || bill.billing_peak_measured_at || null,
    basicRateKrwPerKw: Number(bill.basicRateKrwPerKw || bill.basic_rate_krw_per_kw || 0),
    basicChargeKrw: Number(bill.basicChargeKrw || bill.basic_charge_krw || 0),
    energyChargeKrw: Number(bill.energyChargeKrw || bill.energy_charge_krw || 0),
    estimatedTotalKrw: Number(bill.estimatedTotalKrw || bill.estimated_total_krw || 0),
    demandUnit: bill.demandUnit || bill.demand_unit || 'kW',
    usageUnit: bill.usageUnit || bill.usage_unit || 'kWh',
    currencyUnit: bill.currencyUnit || bill.currency_unit || 'KRW',
    assumptions: bill.assumptions || [],
  }
})
const peakBillBreakdownRows = computed(() => {
  const breakdown = state.peakBillEstimate?.usageBreakdown || state.peakBillEstimate?.usage_breakdown || []
  return breakdown.map((row, index) => ({
    id: `${row.season || row.season_name || index}-${row.loadPeriod || row.load_period || index}`,
    seasonName: row.seasonName || row.season_name || row.season || '-',
    loadPeriodName: row.loadPeriodName || row.load_period_name || row.loadPeriod || row.load_period || '-',
    usageKwh: Number(row.usageKwh || row.usage_kwh || 0),
    rateKrwPerKwh: Number(row.rateKrwPerKwh || row.rate_krw_per_kwh || 0),
    chargeKrw: Number(row.chargeKrw || row.charge_krw || 0),
  }))
})
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
const esgMapPlants = computed(() => {
  const plantLocations = new Map(state.plants.map((plant) => [plantIdentifier(plant), plant]))
  const esgPlantScores = new Map(esgPlants.value.map((plant) => [plantIdentifier(plant), plant]))
  const defaultLocationByName = new Map(
    Object.entries(DEFAULT_PLANT_LOCATIONS).map(([plantId, location]) => [location.plantName, { plantId: Number(plantId), ...location }]),
  )
  const normalizePlant = (plant) => {
    const plantId = plantIdentifier(plant)
    const plantName = plant?.plantName ?? plant?.plant_name ?? plant?.name
    const location = plantLocations.get(plantId) || DEFAULT_PLANT_LOCATIONS[plantId] || defaultLocationByName.get(plantName) || {}
    return {
      ...plant,
      plantId: plantId || location.plantId,
      plantName: plantName || location.plantName || location.name,
      latitude: plant?.latitude ?? plant?.lat ?? location.latitude,
      longitude: plant?.longitude ?? plant?.lng ?? location.longitude,
    }
  }
  const mergedPlantIds = new Set([
    ...esgPlants.value.map(plantIdentifier).filter((plantId) => plantId != null),
    ...state.plants.map(plantIdentifier).filter((plantId) => plantId != null),
  ])
  const mergedPlants = [...mergedPlantIds].map((plantId) => normalizePlant({
    ...plantLocations.get(plantId),
    ...esgPlantScores.get(plantId),
  }))

  const sourcePlants = mergedPlants.length
    ? mergedPlants
    : state.plants.map((plant) => normalizePlant({
      ...plant,
      grade: '-',
      totalScore: null,
    }))

  return sourcePlants.filter((plant) =>
    Number.isFinite(Number(plant.latitude)) && Number.isFinite(Number(plant.longitude)),
  )
})
const esgSchematicMap = computed(() => {
  const plants = esgMapPlants.value
  if (!plants.length) {
    return []
  }

  const sortedPlants = [...plants].sort((left, right) =>
    Number(left.rank || 99) - Number(right.rank || 99) ||
    String(left.plantName || '').localeCompare(String(right.plantName || ''), 'ko'),
  )
  const leftSlots = [
    { left: 2.4, top: 10 },
    { left: 2.4, top: 35 },
    { left: 2.4, top: 60 },
  ]
  const rightSlots = [
    { left: 76.4, top: 10 },
    { left: 76.4, top: 35 },
    { left: 76.4, top: 60 },
  ]
  const half = Math.ceil(sortedPlants.length / 2)

  return sortedPlants.map((plant, index) => {
    const normalizedLng = Math.max(0, Math.min((Number(plant.longitude) - 125.6) / (129.9 - 125.6), 1))
    const normalizedLat = Math.max(0, Math.min((Number(plant.latitude) - 34.6) / (38.3 - 34.6), 1))
    const pinX = 38 + normalizedLng * 30
    const pinY = 17 + (1 - normalizedLat) * 62
    const side = index < half ? 'left' : 'right'
    const slot = side === 'left' ? leftSlots[index % leftSlots.length] : rightSlots[(index - half) % rightSlots.length]

    return {
      plant,
      side,
      cardStyle: {
        left: `${slot.left}%`,
        top: `${slot.top}%`,
      },
      pinStyle: {
        left: `${pinX}%`,
        top: `${pinY}%`,
      },
    }
  })
})

const dashboardPlants = computed(() => {
  const plantsById = new Map()
  const defaultLocations = Object.entries(DEFAULT_PLANT_LOCATIONS).map(([plantId, location]) => ({
    id: Number(plantId),
    name: location.plantName,
    ...location,
  }))

  defaultLocations.forEach((plant) => plantsById.set(Number(plant.id), plant))
  state.plants.forEach((plant) => {
    const plantId = plantIdentifier(plant)
    if (!plantId) {
      return
    }
    plantsById.set(plantId, {
      ...plantsById.get(plantId),
      ...plant,
      id: plantId,
      name: plant.name || plant.plantName || plantsById.get(plantId)?.plantName,
    })
  })

  const scoreByPlantId = new Map([
    ...state.esgScores.map((score) => [plantIdentifier(score), score]),
    ...esgPlants.value.map((plant) => [plantIdentifier(plant), plant]),
  ].filter(([plantId]) => plantId != null))

  const splitIndex = Math.ceil(plantsById.size / 2)

  return Array.from(plantsById.values())
    .map((plant) => {
      const plantId = plantIdentifier(plant)
      const location = DEFAULT_PLANT_LOCATIONS[plantId] || plant
      const score = scoreByPlantId.get(plantId) || {}
      const longitude = Number(plant.longitude ?? plant.lng ?? location.longitude)
      const latitude = Number(plant.latitude ?? plant.lat ?? location.latitude)
      return {
        ...plant,
        plantId,
        plantName: plant.plantName || plant.name || location.plantName || `사업장 ${plantId}`,
        latitude,
        longitude,
        grade: score.grade || plant.grade || (plantId <= 3 ? 'AA' : 'BBB'),
        totalScore: score.totalScore ?? score.total_score ?? plant.totalScore ?? (plantId <= 3 ? 8.02 : 5.48),
      }
    })
    .filter((plant) => plant.plantId && Number.isFinite(plant.latitude) && Number.isFinite(plant.longitude))
    .sort((left, right) => left.plantId - right.plantId)
    .map((plant, index) => {
      const layout = DASHBOARD_PLANT_LAYOUT[plant.plantId]
      const side = layout?.side || (index < splitIndex ? 'left' : 'right')
      const slot = layout?.card || {
        x: side === 'left' ? 4 : 80,
        y: 18 + (index % 3) * 24,
      }
      const pin = projectDashboardPlant(plant.longitude, plant.latitude)
      return {
        ...plant,
        side,
        pin,
        card: slot,
        connectorPath: connectorPath(slot, pin, side),
      }
    })
})

function esgGradeClass(grade) {
  if (['AAA', 'AA', 'A'].includes(grade)) {
    return 'good'
  }
  if (['BBB', 'BB'].includes(grade)) {
    return 'watch'
  }
  return 'risk'
}

function esgSchematicScore(plant) {
  return plant.totalScore == null ? '-' : formatNumber(plant.totalScore)
}
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
function projectDashboardPlant(longitude, latitude) {
  const minLng = 125.6
  const maxLng = 129.8
  const minLat = 34.6
  const maxLat = 38.1
  const x = 34 + ((longitude - minLng) / (maxLng - minLng)) * 34
  const y = 20 + ((maxLat - latitude) / (maxLat - minLat)) * 60
  return {
    x: Math.max(34, Math.min(70, x)),
    y: Math.max(16, Math.min(82, y)),
  }
}

function connectorPath(card, pin, side) {
  const startX = side === 'left' ? card.x + DASHBOARD_CARD_WIDTH : card.x
  const startY = card.y + DASHBOARD_CARD_HEIGHT / 2
  const controlX = side === 'left' ? startX + 14 : startX - 14
  const pinLeadX = side === 'left' ? pin.x - 5 : pin.x + 5
  return `M ${startX} ${startY} C ${controlX} ${startY}, ${pinLeadX} ${pin.y}, ${pin.x} ${pin.y}`
}

function buildScadaExternalUrl(plant) {
  const url = new URL(SCADA_EXTERNAL_URL, window.location.origin)
  const plantId = plantIdentifier(plant)
  if (plantId) {
    url.searchParams.set('plantId', plantId)
  }
  if (plant?.plantName || plant?.name) {
    url.searchParams.set('plantName', plant.plantName || plant.name)
  }
  if (state.me?.name) {
    url.searchParams.set('userName', state.me.name)
  }
  return url.toString()
}

function openPlantSolution(plant) {
  solutionViewer.plant = plant
  solutionViewer.url = buildScadaExternalUrl(plant)
  solutionViewer.open = true
}

function closePlantSolution() {
  solutionViewer.open = false
}

function routeTo(hash) {
  if (hash === '/scada') {
    hash = '/detail/dashboard'
  }
  if (window.location.hash === `#${hash}`) {
    applyRoute()
    return
  }
  window.location.hash = hash
}

function applyRoute() {
  const route = window.location.hash.replace(/^#/, '') || (getAccessToken() ? '/detail/dashboard' : '/login')

  if (route === '/login') {
    appMode.value = 'login'
    return
  }

  if (!getAccessToken()) {
    routeTo('/login')
    return
  }

  if (route === '/scada') {
    routeTo('/detail/dashboard')
    return
  }

  const detailMatch = route.match(/^\/detail\/([^/]+)$/)
  if (detailMatch && validRoutes.includes(detailMatch[1])) {
    activePage.value = detailMatch[1]
    appMode.value = 'detail'
    return
  }

  routeTo(getAccessToken() ? '/detail/dashboard' : '/login')
}

function formatNumber(value, digits = 1) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return Number(value).toLocaleString('ko-KR', {
    maximumFractionDigits: digits,
  })
}

function formatMoney(value) {
  const formatted = formatNumber(value, 0)
  return formatted === '-' ? '-' : `${formatted}원`
}

function alarmKeyword(alarm) {
  const key = alarm.alarmType || String(alarm.message || '기타').trim().split(/\s+/)[0] || 'OTHER'
  return {
    key,
    label: alarmTypeLabels[key] || key,
  }
}

function sortedAlarms(alarms) {
  const direction = alarmSortOrder.value === 'asc' ? 1 : -1
  return [...alarms].sort((a, b) => {
    const aTime = Date.parse(a.occurredAt || '') || 0
    const bTime = Date.parse(b.occurredAt || '') || 0
    return (aTime - bTime) * direction
  })
}

function activeAlarmKeyword(group) {
  const current = alarmActiveKeywords[group.key]
  if (group.keywordTabs.some((tab) => tab.key === current)) {
    return current
  }
  return group.keywordTabs[0]?.key || null
}

function selectAlarmKeyword(groupKey, keyword) {
  alarmActiveKeywords[groupKey] = keyword
}

function activeAlarmRows(group) {
  const activeKeyword = activeAlarmKeyword(group)
  return group.keywordTabs.find((tab) => tab.key === activeKeyword)?.alarms || []
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

function resetUserCreateForm() {
  userCreateForm.email = ''
  userCreateForm.password = ''
  userCreateForm.name = ''
  userCreateForm.phone = ''
  userCreateForm.plantId = ''
  userCreateForm.role = 'VIEWER'
}

async function submitUserCreate() {
  if (userCreating.value) {
    return
  }

  userCreating.value = true
  try {
    await run(async () => {
      const createdUser = await api.signup({
        email: userCreateForm.email.trim(),
        password: userCreateForm.password,
        name: userCreateForm.name.trim(),
        phone: userCreateForm.phone.trim() || null,
        plantId: userCreateForm.plantId ? Number(userCreateForm.plantId) : null,
      })
      if (userCreateForm.role && userCreateForm.role !== 'VIEWER') {
        await api.updateUser(createdUser.userId, {
          role: userCreateForm.role,
          plantId: userCreateForm.plantId ? Number(userCreateForm.plantId) : null,
        })
      }
      resetUserCreateForm()
      await loadUsers({ silent: true })
    })
  } finally {
    userCreating.value = false
  }
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
    routeTo('/detail/dashboard')
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
    startEnergyWebSocket()

    if (!selectedPlantId.value) {
      state.facilities = []
      state.summaries = []
      state.measurements = []
      state.latestEnergy = null
      state.facilityDetail = null
      return
    }

    state.facilities = await api.facilities(selectedPlantId.value).catch(() => [])

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

  if (activePage.value === 'dashboard') {
    await loadDashboardData()
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
    await loadUsers()
    return
  }

  if (activePage.value === 'alarms') {
    await loadAlarms()
  }
}

async function loadDashboardData() {
  const [overview, esgScores] = await Promise.all([
    selectedPlantId.value ? api.dashboard(selectedPlantId.value).catch(() => null) : Promise.resolve(null),
    api.esgScores({
      from: selectedEsgFrom.value,
      to: selectedEsgTo.value,
    }).catch(() => []),
  ])

  state.overview = overview
  state.esgScores = esgScores
}

async function loadAlarms(options = {}) {
  const { silent = false } = options || {}
  const requestId = ++alarmsRequestId

  if (!silent) {
    alarmsLoading.value = true
  }

  try {
    const alarms = await api.alarms({
      plantId: selectedPlantId.value || undefined,
      status: 'OCCURRED',
      limit: 100,
    })
    if (requestId !== alarmsRequestId) {
      return
    }
    state.alarms = alarms
  } finally {
    if (!silent && requestId === alarmsRequestId) {
      alarmsLoading.value = false
    }
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

async function loadUsers(options = {}) {
  const { silent = false } = options || {}
  const requestId = ++usersRequestId

  if (!silent) {
    usersLoading.value = true
  }

  try {
    const users = await api.users({ page: 0, size: 20 })
    if (requestId !== usersRequestId) {
      return
    }
    state.users = users.items || []
  } finally {
    if (!silent && requestId === usersRequestId) {
      usersLoading.value = false
    }
  }
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

async function deleteChatbotMessage(messageId) {
  if (!messageId || chatbotDeletingId.value) {
    return
  }

  chatbotDeletingId.value = messageId
  try {
    await run(async () => {
      await api.deleteChatbotMessage(messageId)
      state.chatbotMessages = state.chatbotMessages.filter((message) => Number(message.id) !== Number(messageId))
    })
  } finally {
    chatbotDeletingId.value = null
  }
}

function useSuggestedQuestion(question) {
  chatbotQuestion.value = question
}

function markdownBlocks(text) {
  const lines = String(text || '').split(/\r?\n/)
  const blocks = []
  let paragraph = []
  let list = []

  const flushParagraph = () => {
    if (!paragraph.length) {
      return
    }
    blocks.push({ type: 'paragraph', text: paragraph.join(' ') })
    paragraph = []
  }
  const flushList = () => {
    if (!list.length) {
      return
    }
    blocks.push({ type: 'list', items: list })
    list = []
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      flushParagraph()
      flushList()
      return
    }

    const headingMatch = trimmed.match(/^(#{1,3})\s+(.+)$/)
    if (headingMatch) {
      flushParagraph()
      flushList()
      blocks.push({ type: `h${headingMatch[1].length}`, text: headingMatch[2] })
      return
    }

    const listMatch = trimmed.match(/^[-*]\s+(.+)$/)
    if (listMatch) {
      flushParagraph()
      list.push(listMatch[1])
      return
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/)
    if (orderedMatch) {
      flushParagraph()
      list.push(orderedMatch[1])
      return
    }

    flushList()
    paragraph.push(trimmed)
  })

  flushParagraph()
  flushList()
  return blocks.length ? blocks : [{ type: 'paragraph', text: String(text || '') }]
}

function parseChatbotJson(value, fallback = null) {
  if (!value) {
    return fallback
  }
  if (typeof value === 'object') {
    return value
  }
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function chatbotChartSpec(message) {
  const spec = parseChatbotJson(message?.chartSpec)
  if (!spec || !Array.isArray(spec.series) || !spec.series.length) {
    return null
  }
  return spec
}

function chatbotSources(message) {
  const sources = parseChatbotJson(message?.externalSources, [])
  return Array.isArray(sources) ? sources.filter((source) => source?.url) : []
}

function chatbotImageDataUrl(message) {
  return message?.imageDataUrl || ''
}

function chatbotChartMax(spec) {
  const values = spec.series.flatMap((series) => Array.isArray(series.values) ? series.values : [])
  const maxValue = Math.max(...values.map((value) => Number(value) || 0), 0)
  return maxValue > 0 ? maxValue : 1
}

function chatbotLinePoints(spec, series) {
  const values = Array.isArray(series.values) ? series.values : []
  const maxValue = chatbotChartMax(spec)
  const width = 520
  const height = 210
  const padX = 28
  const padY = 22
  return values
    .map((value, index) => {
      const x = values.length <= 1
        ? width / 2
        : padX + (index * (width - padX * 2)) / (values.length - 1)
      const y = height - padY - ((Number(value) || 0) / maxValue) * (height - padY * 2)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

function chatbotChartRows(spec) {
  const labels = Array.isArray(spec.labels) ? spec.labels : []
  const firstSeries = spec.series?.[0] || {}
  const values = Array.isArray(firstSeries.values) ? firstSeries.values : []
  const maxValue = chatbotChartMax(spec)
  return values.map((value, index) => ({
    label: labels[index] || firstSeries.name || `항목 ${index + 1}`,
    value: Number(value) || 0,
    rate: Math.max(3, Math.min(100, ((Number(value) || 0) / maxValue) * 100)),
    color: firstSeries.color || '#0f6fff',
  }))
}

function formatChatbotChartValue(value, unit) {
  return `${formatNumber(value)}${unit ? ` ${unit}` : ''}`
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
    await preloadPlantLiveEnergy(selectedFacilityDate.value, endOfFacilityQueryDate())
  }
  await loadFacilityDetail()
  startEnergyWebSocket()
}

async function preloadPlantLiveEnergy(dateValue = formatDateInput(new Date()), toValue = formatDateTimeInput(new Date())) {
  if (!selectedPlantId.value) {
    return
  }

  const facilityIds = new Set(allLineFacilityIds(selectedPlantId.value))
  const rows = await api.energyMeasurements({
    plantId: selectedPlantId.value,
    from: `${dateValue}T00:00:00`,
    to: toValue,
    limit: 5000,
  }).catch(() => [])

  rememberLatestRowsByFacility(rows, facilityIds)
}

function rememberLatestRowsByFacility(rows = [], facilityIds = null) {
  const rowsByFacility = new Map()
  ;(rows || [])
    .map(normalizeEnergyMessage)
    .filter((message) =>
      message &&
      isLineFacilityId(message.facilityId, selectedPlantId.value) &&
      (!facilityIds || facilityIds.has(Number(message.facilityId)))
    )
    .forEach((message) => {
      const key = energyKey(message.plantId, message.facilityId)
      const list = rowsByFacility.get(key) || []
      list.push(message)
      rowsByFacility.set(key, list)
    })

  rowsByFacility.forEach((list, key) => {
    const sorted = [...list].sort((a, b) => Date.parse(b.measuredAt || '') - Date.parse(a.measuredAt || ''))
    const latest = sorted[0] || null
    if (!latest) {
      return
    }
    const baseline = latest
    if (!liveEnergyBaselineByFacility.has(key) || formatDate(liveEnergyBaselineByFacility.get(key)?.measuredAt) !== formatDateInput(new Date())) {
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

async function loadPeakDashboard(options = {}) {
  const { silent = false } = options || {}
  const requestId = ++peakDashboardRequestId

  if (!selectedPlantId.value) {
    state.peakDashboard = null
    state.peakBillEstimate = null
    peakBillError.value = ''
    if (!silent && requestId === peakDashboardRequestId) {
      peakDashboardLoading.value = false
    }
    return
  }

  if (!silent) {
    peakDashboardLoading.value = true
  }

  try {
    const params = {
      plantId: selectedPlantId.value,
      date: selectedPeakDate.value || undefined,
      period: selectedPeakPeriod.value,
    }
    const billEstimateRequest = api.electricityBillEstimate(params)
      .then((billEstimate) => ({ billEstimate, error: '' }))
      .catch(() => ({ billEstimate: null, error: '요금 추정 정보를 불러오지 못했습니다.' }))
    const [dashboard, billResult] = await Promise.all([
      api.peakDashboard(params),
      billEstimateRequest,
    ])
    if (requestId !== peakDashboardRequestId) {
      return
    }
    state.peakDashboard = dashboard
    state.peakBillEstimate = billResult.billEstimate
    peakBillError.value = billResult.error
    if (selectedPeakPeriod.value === 'DAY' && selectedPeakDateIsToday.value) {
      preloadPlantLiveEnergy(selectedPeakDate.value, formatDateTimeInput(new Date())).catch(() => {})
    }
    startEnergyWebSocket()
  } finally {
    if (!silent && requestId === peakDashboardRequestId) {
      peakDashboardLoading.value = false
    }
  }
}

async function loadUtilityDashboard(options = {}) {
  const { silent = false } = options || {}
  const requestId = ++utilityDashboardRequestId

  if (!selectedPlantId.value) {
    state.utilityDashboard = null
    if (!silent && requestId === utilityDashboardRequestId) {
      utilityDashboardLoading.value = false
    }
    return
  }

  if (!silent) {
    utilityDashboardLoading.value = true
  }

  try {
    const dashboard = await api.utilityDashboard({
      plantId: selectedPlantId.value,
      date: selectedUtilityDate.value || undefined,
      period: selectedUtilityPeriod.value,
    })
    if (requestId !== utilityDashboardRequestId) {
      return
    }
    state.utilityDashboard = dashboard
    if (selectedUtilityPeriod.value === 'DAY' && selectedUtilityDateIsToday.value) {
      preloadPlantLiveEnergy(selectedUtilityDate.value, formatDateTimeInput(new Date())).catch(() => {})
    }
    startEnergyWebSocket()
  } finally {
    if (!silent && requestId === utilityDashboardRequestId) {
      utilityDashboardLoading.value = false
    }
  }
}

async function loadEsgDashboard(options = {}) {
  const { silent = false } = options || {}
  const requestId = ++esgDashboardRequestId
  const range = monthRange(selectedEsgMonth.value)
  selectedEsgFrom.value = range.from
  selectedEsgTo.value = range.to

  if (!silent) {
    esgDashboardLoading.value = true
  }

  try {
    const dashboard = await api.esgEnvironmentDashboard({
      plantId: selectedPlantId.value || undefined,
      ...dateRangeParams(range.from, range.to),
    })
    if (requestId !== esgDashboardRequestId) {
      return
    }
    state.esgDashboard = dashboard
  } finally {
    if (!silent && requestId === esgDashboardRequestId) {
      esgDashboardLoading.value = false
      await nextTick()
      await renderEsgGoogleMap()
    }
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

async function refreshData() {
  if (appMode.value === 'detail' && activePage.value !== 'facility') {
    await run(loadActivePageData)
    return
  }
  await run(loadPlantData)
}

function schedulePeakRefresh() {
  if (peakDashboardLoading.value || peakRefreshTimer || Date.now() - lastPeakRefreshAt < LIVE_DASHBOARD_REFRESH_MS) {
    return
  }
  window.clearTimeout(peakRefreshTimer)
  peakRefreshTimer = window.setTimeout(() => {
    lastPeakRefreshAt = Date.now()
    peakRefreshTimer = null
    loadPeakDashboard({ silent: true }).catch(() => {})
  }, 1000)
}

function scheduleUtilityRefresh() {
  if (utilityDashboardLoading.value || utilityRefreshTimer || Date.now() - lastUtilityRefreshAt < LIVE_DASHBOARD_REFRESH_MS) {
    return
  }
  window.clearTimeout(utilityRefreshTimer)
  utilityRefreshTimer = window.setTimeout(() => {
    lastUtilityRefreshAt = Date.now()
    utilityRefreshTimer = null
    loadUtilityDashboard({ silent: true }).catch(() => {})
  }, 1000)
}

function scheduleAlarmRefresh() {
  if (Date.now() - lastAlarmRefreshAt < 5000) {
    return
  }
  lastAlarmRefreshAt = Date.now()
  loadAlarms({ silent: true })
    .catch(() => {})
}

async function resolveAlarm(alarmId) {
  await run(async () => {
    await api.resolveAlarm(alarmId)
    await loadAlarms({ silent: true })
  })
}

async function deleteAlarm(alarmId) {
  await run(async () => {
    await api.deleteAlarm(alarmId)
    await loadAlarms({ silent: true })
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

watch([selectedEsgPlant, esgMapPlants, activePage], () => {
  if (appMode.value !== 'login' && activePage.value === 'esg') {
    renderEsgGoogleMap()
  }
}, { flush: 'post' })

watch([dashboardPlants, selectedPlantId, activePage], () => {
  if (appMode.value !== 'login' && activePage.value === 'dashboard') {
    renderDashboardGoogleMap()
  }
}, { flush: 'post' })

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
  resetDashboardGoogleMap()
  resetEsgGoogleMap()
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
        <div class="login-brand">
          <img :src="brandLogoUrl" :alt="`${PLATFORM_NAME} logo`" />
          <span>{{ PLATFORM_NAME }}</span>
        </div>
        <h2>에너지 데이터와 ESG 지표를 한 화면에서 확인합니다</h2>
        <p class="login-tagline">{{ PLATFORM_TAGLINE }}</p>
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
        <p>{{ PLATFORM_NAME }} Live</p>
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
        <span class="logo-symbol brand-logo-mark">
          <img :src="BRAND_MARK_URL" alt="" aria-hidden="true" />
        </span>
        <b>{{ PLATFORM_NAME }}</b>
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
        <button class="side-logout-button" type="button" @click="logout">
          <LogOut :size="18" />
          <span>로그아웃</span>
        </button>
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

      <section v-if="activePage === 'dashboard'" class="page-stack dashboard-page">
        <article class="dashboard-map-panel">
          <div class="dashboard-map-head">
            <div>
              <span>{{ PLATFORM_NAME }} Network</span>
              <h2>사업장 환경종합 현황도</h2>
            </div>
            <b>{{ dashboardPlants.length }}개 사업장</b>
          </div>

          <div class="plant-map-stage">
            <div
              ref="dashboardMapElement"
              class="google-dashboard-map"
              aria-label="Plant dashboard map"
            ></div>
            <div v-if="dashboardMapState.status !== 'ready'" class="map-loading-state">
              <Factory :size="22" />
              <strong>{{ dashboardMapState.message }}</strong>
              <span v-if="dashboardMapState.status === 'missing-key'">Set VITE_GOOGLE_MAPS_API_KEY in frontend/.env.</span>
            </div>
          </div>
        </article>
      </section>

      <section v-if="activePage !== 'esg' && activePage !== 'dashboard'" class="filter-card">
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
                <em
                  v-if="selectedFacilityDateIsToday"
                  class="facility-equipment-status"
                  :class="{ warn: facility.facilityStatus !== 'RUNNING' }"
                >
                  {{ statusLabel(facility.facilityStatus) }}
                </em>
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
            <input v-model="selectedPeakDate" type="date" :disabled="peakDashboardLoading" />
          </label>
          <div class="peak-period-control">
            <span>집계 단위</span>
            <div class="segmented">
              <button
                v-for="option in peakPeriodOptions"
                :key="option.value"
                type="button"
                :class="{ active: selectedPeakPeriod === option.value }"
                :disabled="peakDashboardLoading"
                @click="selectedPeakPeriod = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <button class="primary-button compact" type="button" :disabled="peakDashboardLoading" @click="run(loadPeakDashboard)">
            <Search :size="17" /> {{ peakDashboardLoading ? '로딩 중' : '조회' }}
          </button>
          <span class="live-pill" :class="{ loading: peakDashboardLoading }">
            {{ peakDashboardLoading ? `${peakPeriodLabel} 데이터 로딩 중` : peakLivePillLabel }}
          </span>
        </section>

        <section class="peak-kpi-grid" :aria-busy="peakDashboardLoading">
          <template v-if="peakDashboardLoading">
            <article v-for="index in 4" :key="`peak-kpi-skeleton-${index}`" class="peak-kpi-card peak-kpi-skeleton-card" aria-hidden="true">
              <span class="peak-skeleton-icon"></span>
              <div class="peak-skeleton-copy">
                <span class="peak-skeleton-line short"></span>
                <span class="peak-skeleton-line value"></span>
                <span class="peak-skeleton-line medium"></span>
              </div>
            </article>
          </template>
          <template v-else>
          <article class="peak-kpi-card">
            <span class="peak-card-icon blue"><Zap :size="22" /></span>
            <div>
              <p>현재 전력 사용량</p>
              <b class="peak-value">{{ formatNumber(peakMetrics.currentKw, 0) }}<small>kW</small></b>
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
          </template>
        </section>

        <section class="panel peak-bill-summary-card" :aria-busy="peakDashboardLoading">
          <template v-if="peakDashboardLoading">
            <div class="peak-bill-heading">
              <span class="peak-skeleton-icon small"></span>
              <div>
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-line medium"></span>
              </div>
            </div>
            <div class="peak-bill-summary-skeleton">
              <span class="peak-skeleton-line hero"></span>
              <span v-for="index in 4" :key="`peak-bill-summary-skeleton-${index}`" class="peak-skeleton-line medium"></span>
            </div>
          </template>
          <template v-else>
            <div class="peak-bill-heading">
              <span class="peak-bill-icon"><ReceiptText :size="22" /></span>
              <div>
                <h2>전기요금 추정</h2>
                <p>{{ peakBillSummary.tariffName }} · {{ peakBillSummary.periodRangeLabel }}</p>
              </div>
            </div>
            <div v-if="peakBillError" class="peak-bill-error" role="status">
              <AlertTriangle :size="18" />
              <span>{{ peakBillError }}</span>
            </div>
            <div v-else-if="peakBillSummary.hasEstimate" class="peak-bill-summary-content">
              <div class="peak-bill-total">
                <span>예상 합계</span>
                <strong>{{ formatMoney(peakBillSummary.estimatedTotalKrw) }}</strong>
                <em>추정치 · {{ peakBillSummary.currencyUnit }}</em>
              </div>
              <div class="peak-bill-mini-grid">
                <article>
                  <span>기본요금</span>
                  <b>{{ formatMoney(peakBillSummary.basicChargeKrw) }}</b>
                </article>
                <article>
                  <span>전력량요금</span>
                  <b>{{ formatMoney(peakBillSummary.energyChargeKrw) }}</b>
                </article>
                <article>
                  <span>산정 피크</span>
                  <b>{{ formatNumber(peakBillSummary.billingDemandKw, 2) }} {{ peakBillSummary.demandUnit }}</b>
                </article>
                <article>
                  <span>피크 시각</span>
                  <b>{{ formatDateTime(peakBillSummary.billingPeakMeasuredAt) }}</b>
                </article>
              </div>
            </div>
            <div v-else class="peak-bill-error muted" role="status">
              <AlertTriangle :size="18" />
              <span>요금 추정 데이터가 없습니다.</span>
            </div>
          </template>
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

        <section v-if="peakDashboardLoading" class="peak-loading-region" role="status" aria-live="polite">
          <span class="sr-only">{{ peakPeriodLabel }} 피크 전력 데이터를 불러오는 중입니다.</span>

          <section v-if="activePeakView === 'comparison'" class="peak-comparison-grid peak-skeleton-grid" aria-hidden="true">
            <article class="panel peak-comparison-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-line medium"></span>
              </div>
              <div class="peak-plant-bars peak-skeleton-bars">
                <div v-for="index in 6" :key="`peak-plant-skeleton-${index}`" class="peak-skeleton-bar-row">
                  <span class="peak-skeleton-dot"></span>
                  <span class="peak-skeleton-line name"></span>
                  <span class="peak-skeleton-track"><i></i></span>
                  <span class="peak-skeleton-line number"></span>
                  <span class="peak-skeleton-line percent"></span>
                </div>
              </div>
            </article>

            <article class="panel peak-selected-plant-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-icon small"></span>
              </div>
              <div class="peak-selected-summary peak-selected-summary-skeleton">
                <span class="peak-skeleton-line name"></span>
                <span class="peak-skeleton-line hero"></span>
                <span class="peak-skeleton-line medium"></span>
                <span class="peak-skeleton-line medium"></span>
                <span class="peak-skeleton-line short"></span>
              </div>
            </article>
          </section>

          <section v-else-if="activePeakView === 'detail'" class="peak-content-grid peak-skeleton-grid" aria-hidden="true">
            <article class="panel peak-gauge-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-line short"></span>
              </div>
              <div class="peak-skeleton-gauge"></div>
              <div class="peak-skeleton-scale">
                <span></span><span></span><span></span><span></span>
              </div>
            </article>

            <article class="panel peak-chart-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-line medium"></span>
              </div>
              <div class="peak-skeleton-chart">
                <span v-for="index in 5" :key="`peak-chart-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel peak-ranking-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-icon small"></span>
              </div>
              <div class="peak-skeleton-ranking">
                <span v-for="index in 5" :key="`peak-ranking-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel peak-bill-detail-panel peak-skeleton-panel">
              <div class="peak-skeleton-title-row">
                <span class="peak-skeleton-line title"></span>
                <span class="peak-skeleton-line medium"></span>
              </div>
              <div class="peak-bill-detail-skeleton">
                <span v-for="index in 7" :key="`peak-bill-detail-skeleton-${index}`" class="peak-skeleton-line medium"></span>
              </div>
            </article>
          </section>

          <article v-else class="panel table-panel peak-history-panel peak-skeleton-panel peak-history-skeleton" aria-hidden="true">
            <div class="peak-skeleton-title-row">
              <span class="peak-skeleton-line title"></span>
              <span class="peak-skeleton-icon small"></span>
            </div>
            <div class="peak-skeleton-table">
              <span v-for="index in 7" :key="`peak-history-skeleton-${index}`"></span>
            </div>
          </article>
        </section>

        <section v-else-if="activePeakView === 'comparison'" class="peak-comparison-grid">
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
                <strong class="peak-gauge-value">{{ formatNumber(peakMetrics.currentKw, 0) }}<small>kW</small></strong>
                <span class="peak-gauge-caption">사용률 {{ formatNumber(peakMetrics.peakUsageRate) }}% / 기준 {{ formatNumber(peakMetrics.thresholdKw, 0) }}&nbsp;kW</span>
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

          <article class="panel peak-bill-detail-panel">
            <div class="panel-title inline">
              <h2>요금 산정 상세</h2>
              <span>{{ peakBillSummary.tariffName }} · {{ peakBillSummary.periodRangeLabel }}</span>
            </div>
            <div v-if="peakBillError" class="peak-bill-error" role="status">
              <AlertTriangle :size="18" />
              <span>{{ peakBillError }}</span>
            </div>
            <template v-else-if="peakBillSummary.hasEstimate">
              <div class="peak-bill-formula-grid">
                <article>
                  <span>기본요금</span>
                  <strong>{{ formatMoney(peakBillSummary.basicChargeKrw) }}</strong>
                  <em>
                    {{ formatNumber(peakBillSummary.billingDemandKw, 2) }} {{ peakBillSummary.demandUnit }}
                    × {{ formatNumber(peakBillSummary.basicRateKrwPerKw, 0) }}원/{{ peakBillSummary.demandUnit }}
                  </em>
                </article>
                <article>
                  <span>전력량요금</span>
                  <strong>{{ formatMoney(peakBillSummary.energyChargeKrw) }}</strong>
                  <em>시간대별 {{ peakBillSummary.usageUnit }} × 원/{{ peakBillSummary.usageUnit }}</em>
                </article>
                <article>
                  <span>예상 합계</span>
                  <strong>{{ formatMoney(peakBillSummary.estimatedTotalKrw) }}</strong>
                  <em>기본요금 + 전력량요금</em>
                </article>
              </div>

              <div class="peak-bill-breakdown-list">
                <article v-for="row in peakBillBreakdownRows" :key="row.id">
                  <div>
                    <b>{{ row.seasonName }} · {{ row.loadPeriodName }}</b>
                    <span>{{ formatNumber(row.usageKwh, 2) }} kWh × {{ formatNumber(row.rateKrwPerKwh) }}원/kWh</span>
                  </div>
                  <strong>{{ formatMoney(row.chargeKrw) }}</strong>
                </article>
                <p v-if="!peakBillBreakdownRows.length">선택 기간의 전력량요금 산정 데이터가 없습니다.</p>
              </div>

              <div class="peak-bill-assumptions">
                <span v-for="assumption in peakBillSummary.assumptions" :key="assumption">
                  {{ assumption }}
                </span>
              </div>
            </template>
            <div v-else class="peak-bill-error muted" role="status">
              <AlertTriangle :size="18" />
              <span>요금 추정 데이터가 없습니다.</span>
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
            <input v-model="selectedUtilityDate" type="date" :disabled="utilityDashboardLoading" />
          </label>
          <div class="utility-period-control">
            <span>집계 단위</span>
            <div class="segmented">
              <button
                v-for="option in utilityPeriodOptions"
                :key="option.value"
                type="button"
                :class="{ active: selectedUtilityPeriod === option.value }"
                :disabled="utilityDashboardLoading"
                @click="selectedUtilityPeriod = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <button class="primary-button compact" type="button" :disabled="utilityDashboardLoading" @click="run(loadUtilityDashboard)">
            <Search :size="17" /> {{ utilityDashboardLoading ? '로딩 중' : '조회' }}
          </button>
          <span class="live-pill" :class="{ loading: utilityDashboardLoading }">
            {{ utilityDashboardLoading ? `${utilityPeriodLabel} 데이터 로딩 중` : utilityLivePillLabel }}
          </span>
        </section>

        <section class="utility-kpi-grid" :aria-busy="utilityDashboardLoading">
          <template v-if="utilityDashboardLoading">
            <article v-for="index in 4" :key="`utility-kpi-skeleton-${index}`" :class="['utility-kpi-card', index <= 2 ? 'gas' : 'water', 'utility-kpi-skeleton-card']" aria-hidden="true">
              <span class="utility-skeleton-icon"></span>
              <div class="utility-skeleton-copy">
                <span class="utility-skeleton-line short"></span>
                <span class="utility-skeleton-line value"></span>
                <span class="utility-skeleton-line medium"></span>
              </div>
            </article>
          </template>
          <template v-else>
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
          </template>
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

        <section v-if="utilityDashboardLoading" class="utility-loading-region" role="status" aria-live="polite">
          <span class="sr-only">{{ utilityPeriodLabel }} 가스/용수 데이터를 불러오는 중입니다.</span>

          <section v-if="activeUtilityView === 'comparison'" class="utility-comparison-grid utility-skeleton-grid" aria-hidden="true">
            <article class="panel utility-comparison-panel utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-line medium"></span>
              </div>
              <div class="utility-skeleton-plant-bars">
                <div v-for="index in 6" :key="`utility-plant-skeleton-${index}`" class="utility-skeleton-plant-row">
                  <span class="utility-skeleton-line name"></span>
                  <div>
                    <span class="utility-skeleton-track gas"><i></i></span>
                    <span class="utility-skeleton-line number"></span>
                  </div>
                  <div>
                    <span class="utility-skeleton-track water"><i></i></span>
                    <span class="utility-skeleton-line number"></span>
                  </div>
                </div>
              </div>
            </article>

            <article class="panel utility-selected-plant-panel utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-icon small"></span>
              </div>
              <div class="utility-selected-summary utility-selected-summary-skeleton">
                <span class="utility-skeleton-line name"></span>
                <article>
                  <span class="utility-skeleton-line short"></span>
                  <span class="utility-skeleton-line hero"></span>
                  <span class="utility-skeleton-line medium"></span>
                </article>
                <article>
                  <span class="utility-skeleton-line short"></span>
                  <span class="utility-skeleton-line hero"></span>
                  <span class="utility-skeleton-line medium"></span>
                </article>
              </div>
            </article>
          </section>

          <section v-else-if="activeUtilityView === 'detail'" class="utility-chart-grid utility-skeleton-grid" aria-hidden="true">
            <article class="panel utility-chart-panel gas utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-line medium"></span>
              </div>
              <div class="utility-skeleton-chart gas">
                <span v-for="index in 5" :key="`utility-gas-chart-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel utility-chart-panel water utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-line medium"></span>
              </div>
              <div class="utility-skeleton-chart water">
                <span v-for="index in 5" :key="`utility-water-chart-skeleton-${index}`"></span>
              </div>
            </article>
          </section>

          <section v-else class="utility-bottom-grid utility-skeleton-grid" aria-hidden="true">
            <article class="panel table-panel utility-meter-panel utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-icon small"></span>
              </div>
              <div class="utility-skeleton-toolbar">
                <span class="utility-skeleton-line input"></span>
                <span class="utility-skeleton-line short"></span>
              </div>
              <div class="utility-skeleton-table">
                <span v-for="index in 6" :key="`utility-meter-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel utility-pattern-panel utility-skeleton-panel">
              <div class="utility-skeleton-title-row">
                <span class="utility-skeleton-line title"></span>
                <span class="utility-skeleton-line medium"></span>
              </div>
              <div class="utility-skeleton-pattern-grid">
                <span v-for="index in 16" :key="`utility-pattern-skeleton-${index}`" class="utility-skeleton-pattern-cell"></span>
              </div>
              <div class="utility-skeleton-scale">
                <span></span><i></i><span></span>
              </div>
            </article>
          </section>
        </section>

        <section v-else-if="activeUtilityView === 'comparison'" class="utility-comparison-grid">
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
            <select v-model.number="selectedPlantId" :disabled="esgDashboardLoading">
              <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
            </select>
          </label>
          <label>
            조회월
            <input v-model="selectedEsgMonth" type="month" :disabled="esgDashboardLoading" />
          </label>
          <button class="primary-button compact" type="button" :disabled="esgDashboardLoading" @click="run(loadEsgDashboard)">
            <Search :size="17" /> {{ esgDashboardLoading ? '로딩 중' : '조회' }}
          </button>
          <span class="live-pill" :class="{ loading: esgDashboardLoading }">
            {{ esgDashboardLoading ? 'ESG 평가 데이터 로딩 중' : '월간 환경 점수 0-10' }}
          </span>
        </section>

        <section class="esg-kpi-grid" :aria-busy="esgDashboardLoading">
          <template v-if="esgDashboardLoading">
            <article v-for="index in 5" :key="`esg-kpi-skeleton-${index}`" :class="['esg-kpi-card', 'esg-kpi-skeleton-card', index === 1 ? 'grade' : '']" aria-hidden="true">
              <span class="esg-skeleton-icon"></span>
              <div class="esg-skeleton-copy">
                <span class="esg-skeleton-line short"></span>
                <span class="esg-skeleton-line value"></span>
                <span class="esg-skeleton-line medium"></span>
              </div>
            </article>
          </template>
          <template v-else>
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
          </template>
        </section>

        <section v-if="esgDashboardLoading" class="esg-loading-region" role="status" aria-live="polite">
          <span class="sr-only">ESG 평가 데이터를 불러오는 중입니다.</span>

          <section class="esg-main-grid esg-skeleton-grid" aria-hidden="true">
            <article class="panel esg-map-panel esg-skeleton-panel">
              <div class="esg-skeleton-title-row">
                <span class="esg-skeleton-line title"></span>
                <span class="esg-skeleton-line medium"></span>
              </div>
              <div class="esg-skeleton-map">
                <span v-for="index in 6" :key="`esg-map-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel esg-ranking-panel esg-skeleton-panel">
              <div class="esg-skeleton-title-row">
                <span class="esg-skeleton-line title"></span>
                <span class="esg-skeleton-icon small"></span>
              </div>
              <div class="esg-skeleton-ranking">
                <span v-for="index in 6" :key="`esg-ranking-skeleton-${index}`"></span>
              </div>
            </article>
          </section>

          <section class="esg-bottom-grid esg-skeleton-grid" aria-hidden="true">
            <article class="panel esg-compare-panel esg-skeleton-panel">
              <div class="esg-skeleton-title-row">
                <span class="esg-skeleton-line title"></span>
                <span class="esg-skeleton-line short"></span>
              </div>
              <div class="esg-skeleton-grouped-chart">
                <span v-for="index in 6" :key="`esg-chart-skeleton-${index}`"></span>
              </div>
            </article>

            <article class="panel esg-detail-panel esg-skeleton-panel">
              <div class="esg-skeleton-title-row">
                <span class="esg-skeleton-line title"></span>
                <span class="esg-skeleton-line medium"></span>
              </div>
              <div class="esg-skeleton-detail-list">
                <span v-for="index in 6" :key="`esg-detail-skeleton-${index}`"></span>
              </div>
            </article>
          </section>
        </section>

        <section v-else class="esg-main-grid">
          <article class="panel esg-map-panel">
            <div class="panel-title inline">
              <h2>사업장 환경 등급 현황</h2>
              <span>{{ selectedEsgFrom }} - {{ selectedEsgTo }}</span>
            </div>
            <div class="esg-map-shell">
              <div
                ref="esgMapElement"
                class="esg-map"
                :class="{ hidden: esgMapState.status === 'fallback' }"
                aria-label="사업장 ESG 지도"
              ></div>
              <div v-if="esgMapState.status === 'fallback'" class="esg-schematic-map" aria-label="사업장 ESG 지도">
                <svg class="esg-korea-shape" viewBox="0 0 100 100" aria-hidden="true">
                  <defs>
                    <linearGradient id="esgMapLand" x1="0" x2="1" y1="0" y2="1">
                      <stop offset="0%" stop-color="#eff6ff" />
                      <stop offset="55%" stop-color="#dcfce7" />
                      <stop offset="100%" stop-color="#cffafe" />
                    </linearGradient>
                    <filter id="esgMapGlow">
                      <feGaussianBlur stdDeviation="1.8" result="blur" />
                      <feMerge>
                        <feMergeNode in="blur" />
                        <feMergeNode in="SourceGraphic" />
                      </feMerge>
                    </filter>
                  </defs>
                  <path
                    class="esg-korea-mainland"
                    d="M51 5 C58 7 63 11 66 18 C70 25 77 27 75 36 C73 45 79 51 75 59 C71 67 62 69 61 78 C60 88 49 94 41 88 C35 84 34 74 38 67 C42 59 37 54 39 47 C41 40 35 33 39 26 C43 18 44 10 51 5 Z"
                  />
                  <path
                    class="esg-korea-coastline"
                    d="M36 38 C30 43 25 48 20 55 C16 61 12 70 17 76 C23 83 33 78 38 72"
                  />
                  <ellipse class="esg-korea-island" cx="31" cy="88" rx="8" ry="3.8" />
                </svg>
                <button
                  v-for="marker in esgSchematicMap"
                  :key="marker.plant.plantId"
                  type="button"
                  class="esg-schematic-card"
                  :class="{ active: Number(marker.plant.plantId) === Number(selectedEsgPlant?.plantId) }"
                  :style="marker.cardStyle"
                  @click="selectedPlantId = marker.plant.plantId"
                >
                  <span>{{ marker.plant.plantName }}</span>
                  <strong :class="esgGradeClass(marker.plant.grade)">{{ marker.plant.grade || '-' }}</strong>
                  <b>{{ esgSchematicScore(marker.plant) }}</b>
                </button>
                <button
                  v-for="marker in esgSchematicMap"
                  :key="`pin-${marker.plant.plantId}`"
                  type="button"
                  class="esg-schematic-pin"
                  :class="{ active: Number(marker.plant.plantId) === Number(selectedEsgPlant?.plantId) }"
                  :style="marker.pinStyle"
                  @click="selectedPlantId = marker.plant.plantId"
                  :aria-label="`${marker.plant.plantName} ${marker.plant.grade || ''}`"
                >
                  <i></i>
                </button>
              </div>
              <div v-if="!['ready', 'fallback'].includes(esgMapState.status)" class="esg-map-state">
                <Factory :size="22" />
                <strong>{{ esgMapState.message }}</strong>
                <span v-if="esgMapState.status === 'missing-key'">Set VITE_GOOGLE_MAPS_API_KEY in frontend/.env.</span>
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

        <section v-if="!esgDashboardLoading" class="esg-bottom-grid">
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
                    {{ message.plantName || selectedPlant?.name || PLATFORM_AI_NAME }}
                    <small>{{ formatDateTime(message.createdAt) }}</small>
                    <button
                      class="chatbot-delete-button"
                      type="button"
                      :disabled="chatbotDeletingId === message.id"
                      title="메시지 삭제"
                      @click="deleteChatbotMessage(message.id)"
                    >
                      <Trash2 :size="14" />
                    </button>
                  </span>
                  <div class="chatbot-markdown">
                    <template
                      v-for="(block, blockIndex) in markdownBlocks(message.answer)"
                      :key="`${message.id}-markdown-${blockIndex}`"
                    >
                      <h3 v-if="block.type === 'h1'">{{ block.text }}</h3>
                      <h4 v-else-if="block.type === 'h2'">{{ block.text }}</h4>
                      <h5 v-else-if="block.type === 'h3'">{{ block.text }}</h5>
                      <ul v-else-if="block.type === 'list'">
                        <li v-for="item in block.items" :key="item">{{ item }}</li>
                      </ul>
                      <p v-else>{{ block.text }}</p>
                    </template>
                  </div>
                  <div v-if="chatbotChartSpec(message)" class="chatbot-chart-card">
                    <div class="chatbot-chart-head">
                      <strong>{{ chatbotChartSpec(message).title || '데이터 차트' }}</strong>
                      <small>{{ chatbotChartSpec(message).unit || '' }}</small>
                    </div>

                    <svg
                      v-if="chatbotChartSpec(message).type === 'line'"
                      class="chatbot-line-chart"
                      viewBox="0 0 520 210"
                      role="img"
                      :aria-label="chatbotChartSpec(message).title || 'line chart'"
                    >
                      <line x1="28" y1="188" x2="492" y2="188" />
                      <line x1="28" y1="22" x2="28" y2="188" />
                      <polyline
                        v-for="series in chatbotChartSpec(message).series"
                        :key="series.name"
                        :points="chatbotLinePoints(chatbotChartSpec(message), series)"
                        :stroke="series.color || '#0f6fff'"
                      />
                    </svg>

                    <div v-else class="chatbot-chart-bars">
                      <div v-for="row in chatbotChartRows(chatbotChartSpec(message))" :key="row.label" class="chatbot-chart-row">
                        <span>{{ row.label }}</span>
                        <div><i :style="{ width: `${row.rate}%`, background: row.color }"></i></div>
                        <b>{{ formatChatbotChartValue(row.value, chatbotChartSpec(message).unit) }}</b>
                      </div>
                    </div>
                  </div>

                  <img
                    v-if="chatbotImageDataUrl(message)"
                    class="chatbot-generated-image"
                    :src="chatbotImageDataUrl(message)"
                    alt="AI generated visual"
                  />

                  <div v-if="chatbotSources(message).length" class="chatbot-source-list">
                    <strong>외부 검색 출처</strong>
                    <a
                      v-for="source in chatbotSources(message)"
                      :key="source.url"
                      :href="source.url"
                      target="_blank"
                      rel="noreferrer"
                    >
                      {{ source.title || source.url }}
                    </a>
                  </div>
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

      <section v-else-if="activePage === 'users'" class="page-stack users-page">
        <article class="panel table-panel">
          <div class="panel-title inline">
            <h2>사용자 목록</h2>
            <span class="live-pill" :class="{ loading: usersLoading }">
              {{ usersLoading ? '사용자 데이터 로딩 중' : `${state.users.length}명` }}
            </span>
          </div>
          <div v-if="usersLoading" class="user-skeleton-table" role="status" aria-live="polite">
            <span class="sr-only">사용자 목록을 불러오는 중입니다.</span>
            <span v-for="index in 8" :key="`user-row-skeleton-${index}`"></span>
          </div>
          <table v-else>
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
              <tr v-if="!state.users.length">
                <td colspan="7">사용자 데이터가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside class="panel user-create-panel">
          <div class="panel-title inline">
            <h2>회원 등록</h2>
            <UserPlus :size="20" />
          </div>

          <div v-if="usersLoading" class="user-skeleton-form" aria-hidden="true">
            <span v-for="index in 6" :key="`user-form-skeleton-${index}`"></span>
            <span class="button"></span>
          </div>

          <form v-else class="user-create-form" @submit.prevent="submitUserCreate">
            <label>
              <span>이메일</span>
              <input v-model.trim="userCreateForm.email" type="email" placeholder="user@example.com" required />
            </label>
            <label>
              <span>비밀번호</span>
              <input v-model="userCreateForm.password" type="password" minlength="8" placeholder="8자 이상" required />
            </label>
            <label>
              <span>이름</span>
              <input v-model.trim="userCreateForm.name" type="text" placeholder="홍길동" required />
            </label>
            <label>
              <span>연락처</span>
              <input v-model.trim="userCreateForm.phone" type="tel" placeholder="010-0000-0000" />
            </label>
            <label>
              <span>사업장</span>
              <select v-model="userCreateForm.plantId">
                <option value="">전체/미지정</option>
                <option v-for="plant in state.plants" :key="plant.id" :value="plant.id">{{ plant.name }}</option>
              </select>
            </label>
            <label>
              <span>권한</span>
              <select v-model="userCreateForm.role">
                <option value="VIEWER">조회자</option>
                <option value="OPERATOR">운영자</option>
                <option value="MANAGER">관리자</option>
                <option v-if="isAdminUser" value="ADMIN">통합 관리자</option>
              </select>
            </label>
            <button class="primary-button" type="submit" :disabled="userCreating">
              <UserPlus :size="17" /> {{ userCreating ? '등록 중' : '등록' }}
            </button>
          </form>
        </aside>
      </section>

      <section v-else-if="activePage === 'alarms'" class="page-stack alarm-page">
        <article v-if="alarmsLoading" class="panel table-panel alarm-tab-panel alarm-skeleton-panel" role="status" aria-live="polite">
          <span class="sr-only">알람 목록을 불러오는 중입니다.</span>
          <div class="panel-title inline alarm-panel-title">
            <span class="alarm-skeleton-line title"></span>
            <div class="alarm-skeleton-sort">
              <span></span><span></span>
            </div>
          </div>

          <section v-for="groupIndex in 2" :key="`alarm-group-skeleton-${groupIndex}`" class="alarm-plant-group">
            <div class="alarm-plant-header">
              <span class="alarm-skeleton-line heading"></span>
              <span class="alarm-skeleton-line count"></span>
            </div>

            <div class="alarm-keyword-layout">
              <div class="alarm-skeleton-tabs">
                <span v-for="index in 4" :key="`alarm-tab-skeleton-${groupIndex}-${index}`"></span>
              </div>

              <div class="alarm-skeleton-table">
                <span v-for="index in 7" :key="`alarm-row-skeleton-${groupIndex}-${index}`"></span>
              </div>
            </div>
          </section>
        </article>

        <article v-else-if="alarmPlantGroups.length" class="panel table-panel alarm-tab-panel">
          <div class="panel-title inline alarm-panel-title">
            <h2>알람 목록</h2>
            <div class="segmented alarm-sort-switch" role="group" aria-label="알람 정렬">
              <button
                type="button"
                :class="{ active: alarmSortOrder === 'desc' }"
                @click="alarmSortOrder = 'desc'"
              >
                최근순
              </button>
              <button
                type="button"
                :class="{ active: alarmSortOrder === 'asc' }"
                @click="alarmSortOrder = 'asc'"
              >
                오래된 순
              </button>
            </div>
          </div>

          <section v-for="group in alarmPlantGroups" :key="group.key" class="alarm-plant-group">
            <div class="alarm-plant-header">
              <h3>{{ group.plantName }}</h3>
              <span>{{ group.totalCount }}건</span>
            </div>

            <div class="alarm-keyword-layout">
              <div class="alarm-keyword-tabs" role="tablist" :aria-label="`${group.plantName} 알람 키워드`">
                <button
                  v-for="tab in group.keywordTabs"
                  :key="tab.key"
                  type="button"
                  :class="{ active: activeAlarmKeyword(group) === tab.key }"
                  role="tab"
                  :aria-selected="activeAlarmKeyword(group) === tab.key"
                  @click="selectAlarmKeyword(group.key, tab.key)"
                >
                  <span>{{ tab.label }}</span>
                  <b>{{ tab.count }}</b>
                </button>
              </div>

              <div class="alarm-table-scroll alarm-keyword-table">
                <table>
                  <thead><tr><th>발생 시각</th><th>설비</th><th>레벨</th><th>메시지</th><th>값</th><th>기준</th><th>상태</th><th>관리</th></tr></thead>
                  <tbody>
                    <tr v-for="alarm in activeAlarmRows(group)" :key="alarm.id">
                      <td>{{ formatDateTime(alarm.occurredAt) }}</td>
                      <td>{{ alarm.facilityName || '-' }}</td>
                      <td>{{ alarm.alarmLevel }}</td>
                      <td>{{ alarm.message }}</td>
                      <td>{{ formatNumber(alarm.value) }}</td>
                      <td>{{ formatNumber(alarm.thresholdValue) }}</td>
                      <td><span class="badge warn">{{ statusLabel(alarm.status) }}</span></td>
                      <td>
                        <div class="alarm-action-cell">
                          <button class="light-button compact" type="button" @click="resolveAlarm(alarm.id)">처리</button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </article>
      </section>

      <div v-if="solutionViewer.open" class="solution-overlay">
        <section class="solution-frame">
          <header>
            <div>
              <span>{{ PLATFORM_NAME }} Solution</span>
              <h2>{{ solutionViewer.plant?.plantName || solutionViewer.plant?.name || '사업장 솔루션' }}</h2>
            </div>
            <button class="icon-button" type="button" aria-label="솔루션 화면 닫기" @click="closePlantSolution">×</button>
          </header>
          <iframe
            :src="solutionViewer.url"
            :title="`${PLATFORM_NAME} solution`"
            referrerpolicy="no-referrer-when-downgrade"
          ></iframe>
        </section>
      </div>
    </section>
  </main>
</template>

