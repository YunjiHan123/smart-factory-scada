var SMWP_API_BASE_URL = 'https://smart-factory-scada-backend.onrender.com';
var SMWP_DEFAULT_PLANT_NAME = '현대 아산';

function smwpNormalizeId(id) {
    if (!id) return '';
    if (id.charAt(0) === '#') return id.substring(1);
    return id;
}

var SMWP_DATEBOX_ID =
    (typeof DateBox1 !== 'undefined' && DateBox1.id)
        ? smwpNormalizeId(DateBox1.id)
        : 'DateBox1';

var SMWP_SELECTED_DATE = '';
var SMWP_CARD_TIMER = null;
var SMWP_CARD_REFRESH_MS = 1000;
var SMWP_CARD_REQUEST_RUNNING = false;
var SMWP_CARD_API_DATA = null;

var SMWP_WS = null;
var SMWP_WS_RECONNECT_TIMER = null;
var SMWP_LIVE_BY_FACILITY = {};
var SMWP_LIVE_BASELINE_BY_FACILITY = {};
var SMWP_LIVE_LISTENERS = [];

function smwpFormatDate(date) {
    var y = date.getFullYear();
    var m = date.getMonth() + 1;
    var d = date.getDate();

    return y + '-' +
        (m < 10 ? '0' + m : m) + '-' +
        (d < 10 ? '0' + d : d);
}

function smwpTodayText() {
    return smwpFormatDate(new Date());
}

function smwpDateParser(s) {
    if (!s) return new Date();

    var ss = s.split('-');
    var y = parseInt(ss[0], 10);
    var m = parseInt(ss[1], 10);
    var d = parseInt(ss[2], 10);

    if (!isNaN(y) && !isNaN(m) && !isNaN(d)) {
        return new Date(y, m - 1, d);
    }

    return new Date();
}

function smwpQueryParam(name) {
    var query = window.location.search || '';

    if (query.charAt(0) === '?') {
        query = query.substring(1);
    }

    var pairs = query.split('&');

    for (var i = 0; i < pairs.length; i++) {
        var pair = pairs[i].split('=');

        if (decodeURIComponent(pair[0] || '') === name) {
            return decodeURIComponent((pair[1] || '').replace(/\+/g, ' '));
        }
    }

    return '';
}

function smwpPlantName() {
    var plantName = smwpQueryParam('plantName');

    if (!plantName || plantName === '') {
        plantName = SMWP_DEFAULT_PLANT_NAME;
    }

    return plantName;
}

function smwpPlantId() {
    var plantName = smwpPlantName();
    var map = {
        '기아 화성': 1,
        '기아 광명': 2,
        '기아 광주': 3,
        '현대 울산': 4,
        '현대 아산': 5,
        '현대 전주': 6
    };

    if (map[plantName]) {
        return map[plantName];
    }

    if (plantName.indexOf('화성') >= 0) return 1;
    if (plantName.indexOf('광명') >= 0) return 2;
    if (plantName.indexOf('광주') >= 0) return 3;
    if (plantName.indexOf('울산') >= 0) return 4;
    if (plantName.indexOf('아산') >= 0) return 5;
    if (plantName.indexOf('전주') >= 0) return 6;

    return 5;
}

function smwpRound(value, digit) {
    var scale = Math.pow(10, digit == null ? 1 : digit);
    return Math.round((Number(value) || 0) * scale) / scale;
}

function smwpIsToday(dateText) {
    return dateText === smwpTodayText();
}

function smwpEnergyKey(plantId, facilityId) {
    return Number(plantId) + ':' + Number(facilityId);
}

function smwpMetricNumber(source, camelKey, snakeKey) {
    var value = source ? (source[camelKey] != null ? source[camelKey] : source[snakeKey]) : 0;
    var number = Number(value);
    return isNaN(number) ? 0 : number;
}

function smwpNormalizeEnergyMessage(source) {
    if (!source) {
        return null;
    }

    var plantId = Number(source.plantId != null ? source.plantId : source.plant_id);
    var facilityId = Number(source.facilityId != null ? source.facilityId : source.facility_id);
    var measuredAt = source.measuredAt || source.measured_at;

    if (isNaN(plantId) || isNaN(facilityId) || !measuredAt) {
        return null;
    }

    return {
        plantId: plantId,
        facilityId: facilityId,
        measuredAt: measuredAt,
        electricityKwh: smwpMetricNumber(source, 'electricityKwh', 'electricity_kwh'),
        gasM3: smwpMetricNumber(source, 'gasM3', 'gas_m3'),
        waterTon: smwpMetricNumber(source, 'waterTon', 'water_ton'),
        solarKwh: smwpMetricNumber(source, 'solarKwh', 'solar_kwh')
    };
}

function smwpIsLineFacility(message, plantId) {
    var facilityId = Number(message && message.facilityId);
    var sequence = facilityId % 10000;

    return Number(message && message.plantId) === Number(plantId)
        && facilityId >= 10000
        && Math.floor(facilityId / 10000) === Number(plantId)
        && sequence >= 1
        && sequence <= 24;
}

function smwpLiveDeltaForPlant(plantId) {
    var result = {
        electricityKwh: 0,
        gasM3: 0,
        waterTon: 0,
        solarKwh: 0
    };

    for (var key in SMWP_LIVE_BY_FACILITY) {
        if (!Object.prototype.hasOwnProperty.call(SMWP_LIVE_BY_FACILITY, key)) {
            continue;
        }

        var live = SMWP_LIVE_BY_FACILITY[key];
        var baseline = SMWP_LIVE_BASELINE_BY_FACILITY[key];

        if (!smwpIsLineFacility(live, plantId) || !baseline) {
            continue;
        }

        result.electricityKwh += Math.max(0, live.electricityKwh - baseline.electricityKwh);
        result.gasM3 += Math.max(0, live.gasM3 - baseline.gasM3);
        result.waterTon += Math.max(0, live.waterTon - baseline.waterTon);
        result.solarKwh += Math.max(0, live.solarKwh - baseline.solarKwh);
    }

    return result;
}

function smwpCloneArray(values) {
    var result = [];
    values = values || [];

    for (var i = 0; i < 24; i++) {
        result.push(Number(values[i]) || 0);
    }

    return result;
}

function smwpApplyLiveDaily(data) {
    data = data || {};

    if (!smwpIsToday(SMWP_SELECTED_DATE)) {
        return data;
    }

    var delta = smwpLiveDeltaForPlant(smwpPlantId());

    return {
        electricityKwh: smwpMetricNumber(data, 'electricityKwh', 'electricity_kwh') + delta.electricityKwh,
        gasM3: smwpMetricNumber(data, 'gasM3', 'gas_m3') + delta.gasM3,
        waterTon: smwpMetricNumber(data, 'waterTon', 'water_ton') + delta.waterTon,
        solarKwh: smwpMetricNumber(data, 'solarKwh', 'solar_kwh') + delta.solarKwh
    };
}

function smwpApplyLiveHourly(data, dateText) {
    data = data || {};

    if (!smwpIsToday(dateText)) {
        return data;
    }

    var hour = new Date().getHours();
    var delta = smwpLiveDeltaForPlant(smwpPlantId());
    var electricityKwh = smwpCloneArray(data.electricityKwh);
    var gasM3 = smwpCloneArray(data.gasM3);
    var waterTon = smwpCloneArray(data.waterTon);
    var solarKwh = smwpCloneArray(data.solarKwh);

    electricityKwh[hour] += delta.electricityKwh;
    gasM3[hour] += delta.gasM3;
    waterTon[hour] += delta.waterTon;
    solarKwh[hour] += delta.solarKwh;

    return {
        electricityKwh: electricityKwh,
        gasM3: gasM3,
        waterTon: waterTon,
        solarKwh: solarKwh
    };
}

function smwpApplyLiveCompare(data, energyType, dateText) {
    data = data || {};

    if (!smwpIsToday(dateText)) {
        return data;
    }

    var delta = smwpLiveDeltaForPlant(smwpPlantId());
    var addition = 0;

    if (energyType === 'ELECTRICITY') addition = delta.electricityKwh;
    if (energyType === 'GAS') addition = delta.gasM3;
    if (energyType === 'WATER') addition = delta.waterTon;
    if (energyType === 'SOLAR') addition = delta.solarKwh;

    var currentUsage = smwpMetricNumber(data, 'currentUsage', 'current_usage') + addition;
    var previousMonthAverage = smwpMetricNumber(data, 'previousMonthAverage', 'previous_month_average');
    var changeRate = previousMonthAverage > 0
        ? ((currentUsage - previousMonthAverage) / previousMonthAverage) * 100
        : smwpMetricNumber(data, 'changeRate', 'change_rate');

    return {
        previousMonthAverage: previousMonthAverage,
        currentUsage: currentUsage,
        changeRate: changeRate
    };
}

function smwpMarkLiveBaseline() {
    var plantId = smwpPlantId();

    for (var key in SMWP_LIVE_BY_FACILITY) {
        if (!Object.prototype.hasOwnProperty.call(SMWP_LIVE_BY_FACILITY, key)) {
            continue;
        }

        if (smwpIsLineFacility(SMWP_LIVE_BY_FACILITY[key], plantId)) {
            SMWP_LIVE_BASELINE_BY_FACILITY[key] = SMWP_LIVE_BY_FACILITY[key];
        }
    }
}

function smwpNotifyLiveListeners() {
    for (var i = 0; i < SMWP_LIVE_LISTENERS.length; i++) {
        try {
            SMWP_LIVE_LISTENERS[i]();
        } catch (e) {
            console.warn('[SMWP Live] listener failed:', e);
        }
    }

    if (typeof window.SMWPRenderLiveCharts === 'function') {
        window.SMWPRenderLiveCharts();
    }
}

function smwpEnergyWebSocketUrl() {
    var baseUrl = window.SMWP_API_BASE_URL || SMWP_API_BASE_URL;
    var parser = document.createElement('a');
    parser.href = baseUrl;

    var protocol = parser.protocol === 'https:' ? 'wss:' : 'ws:';
    return protocol + '//' + parser.host + '/ws/energy';
}

function smwpStartEnergyWebSocket() {
    if (SMWP_WS && SMWP_WS.readyState <= WebSocket.OPEN) {
        return;
    }

    if (!window.WebSocket) {
        console.warn('[SMWP Live] WebSocket is not supported.');
        return;
    }

    clearTimeout(SMWP_WS_RECONNECT_TIMER);

    try {
        SMWP_WS = new WebSocket(smwpEnergyWebSocketUrl());
    } catch (e) {
        console.warn('[SMWP Live] WebSocket open failed:', e);
        return;
    }

    SMWP_WS.onmessage = function (event) {
        var data = null;

        try {
            data = JSON.parse(event.data);
        } catch (e) {
            return;
        }

        var message = smwpNormalizeEnergyMessage(data);

        if (!message || !smwpIsLineFacility(message, smwpPlantId())) {
            return;
        }

        var key = smwpEnergyKey(message.plantId, message.facilityId);
        var previous = SMWP_LIVE_BY_FACILITY[key];
        var previousTime = previous ? Date.parse(previous.measuredAt) : NaN;
        var nextTime = Date.parse(message.measuredAt);

        if (!isNaN(previousTime) && !isNaN(nextTime) && nextTime < previousTime) {
            return;
        }

        SMWP_LIVE_BY_FACILITY[key] = message;

        if (!SMWP_LIVE_BASELINE_BY_FACILITY[key]) {
            SMWP_LIVE_BASELINE_BY_FACILITY[key] = message;
        }

        smwpNotifyLiveListeners();
    };

    SMWP_WS.onclose = function () {
        SMWP_WS = null;

        if (smwpIsToday(SMWP_SELECTED_DATE)) {
            SMWP_WS_RECONNECT_TIMER = setTimeout(smwpStartEnergyWebSocket, 2000);
        }
    };

    SMWP_WS.onerror = function () {
        if (SMWP_WS) {
            SMWP_WS.close();
        }
    };
}

function smwpCardApiUrl(dateText) {
    return SMWP_API_BASE_URL
        + '/api/smwp/energy/daily'
        + '?plantName=' + encodeURIComponent(smwpPlantName())
        + '&date=' + encodeURIComponent(dateText)
        + '&_t=' + new Date().getTime();
}

function smwpApplyCardValues(data) {
    data = smwpApplyLiveDaily(data || {});

    var elecValue = smwpRound(data.electricityKwh, 1);
    var gasValue = smwpRound(data.gasM3, 1);
    var waterValue = smwpRound(data.waterTon, 1);
    var solarValue = smwpRound(data.solarKwh, 1);

    if (typeof $System !== 'undefined') {
        $System.elec = elecValue;
        $System.gas = gasValue;
        $System.water = waterValue;
        $System.solar = solarValue;
    }

    console.log('[SMWP Card]', {
        date: SMWP_SELECTED_DATE,
        elec: elecValue,
        gas: gasValue,
        water: waterValue,
        solar: solarValue
    });
}

function smwpRefreshCards(dateText) {
    if (SMWP_CARD_REQUEST_RUNNING) {
        return;
    }

    SMWP_CARD_REQUEST_RUNNING = true;

    fetch(smwpCardApiUrl(dateText))
        .then(function (response) {
            if (!response.ok) {
                throw new Error('daily api failed: ' + response.status);
            }

            return response.json();
        })
        .then(function (data) {
            SMWP_CARD_API_DATA = data;
            smwpApplyCardValues(data);
            smwpMarkLiveBaseline();
        })
        .catch(function (error) {
            console.error('[SMWP Card] API error:', error);
        })
        .then(function () {
            SMWP_CARD_REQUEST_RUNNING = false;
        });
}

function smwpStopCardRealtime() {
    if (SMWP_CARD_TIMER) {
        clearTimeout(SMWP_CARD_TIMER);
        SMWP_CARD_TIMER = null;
    }
}

function smwpStartCardRealtime() {
    smwpStopCardRealtime();

    if (!smwpIsToday(SMWP_SELECTED_DATE)) {
        console.log('[SMWP Card] past date. realtime stopped.');
        return;
    }

    smwpStartEnergyWebSocket();

    function loop() {
        if (!smwpIsToday(SMWP_SELECTED_DATE)) {
            smwpStopCardRealtime();
            return;
        }

        smwpRefreshCards(SMWP_SELECTED_DATE);

        SMWP_CARD_TIMER = setTimeout(function () {
            loop();
        }, SMWP_CARD_REFRESH_MS);
    }

    loop();
}

function smwpRefreshAllCharts(dateText) {
    if (typeof window.SMWPRefreshEnergyChart === 'function') {
        window.SMWPRefreshEnergyChart(dateText);
    }

    if (typeof window.SMWPRefreshCompareCharts === 'function') {
        window.SMWPRefreshCompareCharts(dateText);
    }
}

function smwpSetSearchDate(dateText) {
    SMWP_SELECTED_DATE = dateText || smwpTodayText();
    window.SMWP_SELECTED_DATE = SMWP_SELECTED_DATE;

    console.log('[SMWP SearchDate 확정]', SMWP_SELECTED_DATE);

    if (smwpIsToday(SMWP_SELECTED_DATE)) {
        smwpMarkLiveBaseline();
        smwpStartEnergyWebSocket();
    }

    smwpRefreshCards(SMWP_SELECTED_DATE);
    smwpStartCardRealtime();
    smwpRefreshAllCharts(SMWP_SELECTED_DATE);
}

window.smwpSetSearchDate = smwpSetSearchDate;
window.smwpTodayText = smwpTodayText;
window.smwpIsToday = smwpIsToday;
window.smwpPlantName = smwpPlantName;
window.smwpPlantId = smwpPlantId;
window.SMWPApplyLiveDaily = smwpApplyLiveDaily;
window.SMWPApplyLiveHourly = smwpApplyLiveHourly;
window.SMWPApplyLiveCompare = smwpApplyLiveCompare;
window.SMWPStartEnergyWebSocket = smwpStartEnergyWebSocket;
window.SMWPAddLiveEnergyListener = function (listener) {
    if (typeof listener === 'function') {
        SMWP_LIVE_LISTENERS.push(listener);
    }
};
window.SMWP_API_BASE_URL = SMWP_API_BASE_URL;

SMWP_LIVE_LISTENERS.push(function () {
    if (smwpIsToday(SMWP_SELECTED_DATE) && SMWP_CARD_API_DATA) {
        smwpApplyCardValues(SMWP_CARD_API_DATA);
    }
});

function smwpInitDateBoxRetry(retryCount) {
    var $datebox = $('#' + SMWP_DATEBOX_ID);

    if (!$datebox.length) {
        if (retryCount > 20) {
            console.error('[SMWP DateBox] not found:', SMWP_DATEBOX_ID);
            return;
        }

        setTimeout(function () {
            smwpInitDateBoxRetry(retryCount + 1);
        }, 200);

        return;
    }

    try {
        $datebox.datebox({
            formatter: smwpFormatDate,
            parser: smwpDateParser
        });
    } catch (e) {
        console.warn('[SMWP DateBox] init failed:', e);
    }

    var today = smwpTodayText();

    setTimeout(function () {
        try {
            $datebox.datebox('setValue', today);
            console.log('[SMWP DateBox] set today:', today);
        } catch (e2) {
            console.warn('[SMWP DateBox] setValue failed:', e2);
        }

        smwpSetSearchDate(today);
    }, 300);
}

$(document).ready(function () {
    smwpInitDateBoxRetry(0);
});
