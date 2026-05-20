var SMWP_ALARM_API_BASE_URL =
    (typeof window.SMWP_API_BASE_URL !== 'undefined')
        ? window.SMWP_API_BASE_URL
        : 'https://smart-factory-scada-backend.onrender.com'
function smwpAlarmQueryParam(name) {
    var query = window.location.search || '';
    var hash = window.location.hash || '';

    if (hash.indexOf('?') >= 0) {
        query += '&' + hash.substring(hash.indexOf('?') + 1);
    } else if (hash.indexOf('&') >= 0) {
        query += '&' + hash.substring(hash.indexOf('&') + 1);
    }

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

function smwpAlarmPlantName() {
    if (typeof window.smwpPlantName === 'function') {
        return window.smwpPlantName();
    }

    return smwpAlarmQueryParam('plantName');
}

function smwpAlarmApiUrl() {
    return SMWP_ALARM_API_BASE_URL
        + '/api/smwp/alarms'
        + '?plantName=' + encodeURIComponent(smwpAlarmPlantName())
        + '&limit=50'
        + '&_t=' + new Date().getTime();
}

function smwpAlarmLoadRows(done) {
    fetch(smwpAlarmApiUrl())
        .then(function (response) {
            if (!response.ok) {
                throw new Error('alarm api failed: ' + response.status);
            }

            return response.json();
        })
        .then(function (rows) {
            rows = rows || [];

            done(rows.map(function (row) {
                return {
                    1: row.occurredAt || '-',
                    2: row.message || '-'
                };
            }));
        })
        .catch(function (error) {
            console.error('[SMWP Alarm] API error:', error);
            done([]);
        });
}

$('#' + id).datagrid({
    columns: [[
        { field: '1', title: '발생 시간', width: 150 },
        { field: '2', title: '내용', width: 420 }
    ]],
    data: []
});

smwpAlarmLoadRows(function (rows) {
    $('#' + id).datagrid('loadData', rows);
});
