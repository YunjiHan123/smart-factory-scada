var SMWP_API_BASE_URL = 'http://localhost:8080';
var SMWP_DEFAULT_PLANT_NAME = '현대 아산';
var SMWP_DEFAULT_USER_NAME = '';


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
function smwpUserName() {
    var userName = smwpQueryParam('userName');

    if (!userName || userName === '') {
        userName = SMWP_DEFAULT_USER_NAME;
    }

    return userName;
}

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

function smwpRound(value, digit) {
    var scale = Math.pow(10, digit == null ? 1 : digit);
    return Math.round((Number(value) || 0) * scale) / scale;
}

function smwpIsToday(dateText) {
    return dateText === smwpTodayText();
}

function smwpCardApiUrl(dateText) {
    return SMWP_API_BASE_URL
        + '/api/smwp/energy/daily'
        + '?plantName=' + encodeURIComponent(smwpPlantName())
        + '&date=' + encodeURIComponent(dateText)
        + '&_t=' + new Date().getTime();
}

function smwpApplyCardValues(data) {
    data = data || {};

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
            smwpApplyCardValues(data);
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

    smwpRefreshCards(SMWP_SELECTED_DATE);
    smwpStartCardRealtime();
    smwpRefreshAllCharts(SMWP_SELECTED_DATE);
}

window.smwpSetSearchDate = smwpSetSearchDate;
window.smwpTodayText = smwpTodayText;
window.smwpUserName = smwpUserName;
window.smwpIsToday = smwpIsToday;
window.smwpPlantName = smwpPlantName;
window.SMWP_API_BASE_URL = SMWP_API_BASE_URL;

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