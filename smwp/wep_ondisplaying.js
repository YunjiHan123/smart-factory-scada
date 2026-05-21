if (!window.SMWP_DISPLAYING_READY) {
    window.SMWP_DISPLAYING_READY = true;

    var SMWP_DISPLAY_API_BASE_URL =
        window.SMWP_API_BASE_URL || 'https://smart-factory-scada-backend.onrender.com';

    var SMWP_HOURLY_CHART_DOM_ID = smwpDisplayNormalizeId(CustomCharts5.id);

    var SMWP_COMPARE_CHARTS = [
        {
            domId: smwpDisplayNormalizeId(CustomCharts4.id),
            energyType: 'ELECTRICITY',
            title: '전기 (kWh)',
            color1: '#1e9bff',
            color2: '#006bd6'
        },
        {
            domId: smwpDisplayNormalizeId(CustomCharts3.id),
            energyType: 'GAS',
            title: '가스 (Nm³)',
            color1: '#ffb13b',
            color2: '#ff8c00'
        },
        {
            domId: smwpDisplayNormalizeId(CustomCharts2.id),
            energyType: 'WATER',
            title: '용수 (m³)',
            color1: '#20d6e8',
            color2: '#00aeca'
        },
        {
            domId: smwpDisplayNormalizeId(CustomCharts1.id),
            energyType: 'SOLAR',
            title: '태양광 (kWh)',
            color1: '#61d849',
            color2: '#23b82f'
        }
    ];

    var SMWP_HOURLY_CHART = null;
    var SMWP_HOURLY_TIMER = null;
    var SMWP_HOURLY_REQUEST_RUNNING = false;

    var SMWP_COMPARE_TIMER = null;
    var SMWP_COMPARE_REQUEST_RUNNING = false;

    var SMWP_REFRESH_MS = 1000;

    function smwpDisplayNormalizeId(id) {
        if (!id) return '';
        if (id.charAt(0) === '#') return id.substring(1);
        return id;
    }

    function smwpDisplayTodayText() {
        if (typeof window.smwpTodayText === 'function') {
            return window.smwpTodayText();
        }

        var today = new Date();
        var y = today.getFullYear();
        var m = today.getMonth() + 1;
        var d = today.getDate();

        return y + '-' +
            (m < 10 ? '0' + m : m) + '-' +
            (d < 10 ? '0' + d : d);
    }

    function smwpDisplayIsToday(dateText) {
        return dateText === smwpDisplayTodayText();
    }

    function smwpDisplayPlantName() {
        if (typeof window.smwpPlantName === 'function') {
            return window.smwpPlantName();
        }

        return '현대 아산';
    }

    function smwpFindChartInstance(domId) {
        var root = document.getElementById(domId);

        if (!root) {
            console.warn('[SMWP Chart] root not found:', domId);
            return null;
        }

        var chart = echarts.getInstanceByDom(root);

        if (chart) {
            return chart;
        }

        var children = root.getElementsByTagName('*');

        for (var i = 0; i < children.length; i++) {
            chart = echarts.getInstanceByDom(children[i]);

            if (chart) {
                return chart;
            }
        }

        return echarts.init(root);
    }

    function smwpHourlyLabels() {
        return [
            '00:00', '01:00', '02:00', '03:00', '04:00', '05:00',
            '06:00', '07:00', '08:00', '09:00', '10:00', '11:00',
            '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
            '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'
        ];
    }

    function smwpHourlyApiUrl(dateText) {
        return SMWP_DISPLAY_API_BASE_URL
            + '/api/smwp/energy/hourly'
            + '?plantName=' + encodeURIComponent(smwpDisplayPlantName())
            + '&date=' + encodeURIComponent(dateText)
            + '&_t=' + new Date().getTime();
    }

    function smwpHourlyArray(values, dateText) {
        values = values || [];

        var result = [];
        var nowHour = new Date().getHours();
        var isToday = smwpDisplayIsToday(dateText);

        for (var i = 0; i < 24; i++) {
            var value = Number(values[i]);

            if (isNaN(value)) {
                value = 0;
            }

            if (isToday && i > nowHour) {
                value = 0;
            }

            result.push(value);
        }

        return result;
    }

    function smwpHourlyOption(data, dateText) {
        data = data || {};

        return {
            backgroundColor: '#071827',
            tooltip: {
                trigger: 'axis',
                backgroundColor: 'rgba(7, 24, 39, 0.9)',
                borderColor: '#1f3b55',
                borderWidth: 1,
                textStyle: { color: '#ffffff' }
            },
            legend: {
                top: 16,
                right: 30,
                itemWidth: 18,
                itemHeight: 3,
                textStyle: { color: '#d8e6f3', fontSize: 11 },
                data: ['전기 (kWh)', '가스 (m³)', '용수 (m³)', '태양광 (kWh)']
            },
            grid: { left: 48, right: 22, top: 48, bottom: 36 },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: smwpHourlyLabels(),
                axisLine: { lineStyle: { color: '#29445c' } },
                axisTick: { show: false },
                axisLabel: { color: '#b8c7d5', fontSize: 11 },
                splitLine: { show: false }
            },
            yAxis: {
                type: 'value',
                name: '(사용량)',
                min: 0,
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: { color: '#b8c7d5', fontSize: 11 },
                splitLine: { lineStyle: { color: '#183247', type: 'dashed' } }
            },
            series: [
                {
                    name: '전기 (kWh)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: { width: 2.5, color: '#1e90ff' },
                    itemStyle: { color: '#1e90ff' },
                    data: smwpHourlyArray(data.electricityKwh, dateText)
                },
                {
                    name: '가스 (m³)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: { width: 2.2, color: '#ff8c00' },
                    itemStyle: { color: '#ff8c00' },
                    data: smwpHourlyArray(data.gasM3, dateText)
                },
                {
                    name: '용수 (m³)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: { width: 2.2, color: '#00c8e8' },
                    itemStyle: { color: '#00c8e8' },
                    data: smwpHourlyArray(data.waterTon, dateText)
                },
                {
                    name: '태양광 (kWh)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: { width: 2.2, color: '#59d33f' },
                    itemStyle: { color: '#59d33f' },
                    data: smwpHourlyArray(data.solarKwh, dateText)
                }
            ]
        };
    }

    function smwpRefreshEnergyChart(dateText) {
        if (SMWP_HOURLY_REQUEST_RUNNING) {
            return;
        }

        SMWP_HOURLY_CHART = smwpFindChartInstance(SMWP_HOURLY_CHART_DOM_ID);

        if (!SMWP_HOURLY_CHART) {
            return;
        }

        SMWP_HOURLY_REQUEST_RUNNING = true;

        fetch(smwpHourlyApiUrl(dateText))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('hourly api failed: ' + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                SMWP_HOURLY_CHART.setOption(smwpHourlyOption(data, dateText), true);
            })
            .catch(function (error) {
                console.error('[SMWP Hourly] API error:', error);
            })
            .then(function () {
                SMWP_HOURLY_REQUEST_RUNNING = false;
            });
    }

    function smwpCompareApiUrl(dateText, energyType) {
        return SMWP_DISPLAY_API_BASE_URL
            + '/api/smwp/energy/comparison'
            + '?plantName=' + encodeURIComponent(smwpDisplayPlantName())
            + '&date=' + encodeURIComponent(dateText)
            + '&energyType=' + encodeURIComponent(energyType)
            + '&_t=' + new Date().getTime();
    }

    function smwpNumber(value) {
        var number = Number(value);
        return isNaN(number) ? 0 : number;
    }

    function smwpRateText(rate) {
        rate = smwpNumber(rate);

        if (rate > 0) return '▲ ' + Math.abs(rate).toFixed(1) + '%';
        if (rate < 0) return '▼ ' + Math.abs(rate).toFixed(1) + '%';

        return '- 0.0%';
    }

    function smwpRateColor(rate) {
        rate = smwpNumber(rate);

        if (rate > 0) return '#ffb13b';
        if (rate < 0) return '#00c8e8';

        return '#d8e6f3';
    }

    function smwpCompareOption(config, data) {
        data = data || {};

        var previousMonthAverage = smwpNumber(data.previousMonthAverage);
        var currentUsage = smwpNumber(data.currentUsage);
        var changeRate = smwpNumber(data.changeRate);

        return {
            backgroundColor: '#071827',
            title: {
                text: config.title,
                left: 'center',
                top: 2,
                textStyle: {
                    color: config.color1,
                    fontSize: 11,
                    fontWeight: 'bold'
                }
            },
            grid: { left: 18, right: 56, top: 34, bottom: 22 },
            xAxis: {
                type: 'category',
                data: ['전월', '금일'],
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: {
                    color: '#d8e6f3',
                    fontSize: 9
                }
            },
            yAxis: {
                type: 'value',
                show: false
            },
            series: [
                {
                    type: 'bar',
                    barWidth: 28,
                    data: [
                        {
                            value: previousMonthAverage,
                            itemStyle: { color: '#8b969e' }
                        },
                        {
                            value: currentUsage,
                            itemStyle: {
                                color: {
                                    type: 'linear',
                                    x: 0,
                                    y: 0,
                                    x2: 0,
                                    y2: 1,
                                    colorStops: [
                                        { offset: 0, color: config.color1 },
                                        { offset: 1, color: config.color2 }
                                    ]
                                }
                            }
                        }
                    ],
                    label: {
                        show: true,
                        position: 'top',
                        color: '#d8e6f3',
                        fontSize: 8,
                        fontWeight: 'bold',
                        formatter: function (params) {
                            return Number(params.value).toLocaleString();
                        }
                    }
                }
            ],
            graphic: [
                {
                    type: 'text',
                    right: 8,
                    top: 62,
                    style: {
                        text: smwpRateText(changeRate),
                        fill: smwpRateColor(changeRate),
                        fontSize: 10,
                        fontWeight: 'bold'
                    }
                }
            ]
        };
    }

    function smwpRefreshCompareChartOne(config, dateText) {
        var chart = smwpFindChartInstance(config.domId);

        if (!chart) {
            return;
        }

        fetch(smwpCompareApiUrl(dateText, config.energyType))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('comparison api failed: ' + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                chart.setOption(smwpCompareOption(config, data), true);
            })
            .catch(function (error) {
                console.error('[SMWP Compare] API error:', error);
            });
    }

    function smwpRefreshCompareCharts(dateText) {
        if (SMWP_COMPARE_REQUEST_RUNNING) {
            return;
        }

        SMWP_COMPARE_REQUEST_RUNNING = true;

        for (var i = 0; i < SMWP_COMPARE_CHARTS.length; i++) {
            smwpRefreshCompareChartOne(SMWP_COMPARE_CHARTS[i], dateText);
        }

        setTimeout(function () {
            SMWP_COMPARE_REQUEST_RUNNING = false;
        }, 300);
    }

    function smwpStopAllChartRealtime() {
        if (SMWP_HOURLY_TIMER) {
            clearTimeout(SMWP_HOURLY_TIMER);
            SMWP_HOURLY_TIMER = null;
        }

        if (SMWP_COMPARE_TIMER) {
            clearTimeout(SMWP_COMPARE_TIMER);
            SMWP_COMPARE_TIMER = null;
        }
    }

    function smwpStartAllChartRealtime(dateText) {
        smwpStopAllChartRealtime();

        if (!smwpDisplayIsToday(dateText)) {
            return;
        }

        function hourlyLoop() {
            if (!smwpDisplayIsToday(window.SMWP_SELECTED_DATE)) {
                return;
            }

            smwpRefreshEnergyChart(window.SMWP_SELECTED_DATE);

            SMWP_HOURLY_TIMER = setTimeout(function () {
                hourlyLoop();
            }, SMWP_REFRESH_MS);
        }

        function compareLoop() {
            if (!smwpDisplayIsToday(window.SMWP_SELECTED_DATE)) {
                return;
            }

            smwpRefreshCompareCharts(window.SMWP_SELECTED_DATE);

            SMWP_COMPARE_TIMER = setTimeout(function () {
                compareLoop();
            }, SMWP_REFRESH_MS);
        }

        hourlyLoop();
        compareLoop();
    }

    window.SMWPRefreshEnergyChart = function (dateText) {
        window.SMWP_SELECTED_DATE = dateText || window.SMWP_SELECTED_DATE || smwpDisplayTodayText();

        smwpRefreshEnergyChart(window.SMWP_SELECTED_DATE);
        smwpStartAllChartRealtime(window.SMWP_SELECTED_DATE);
    };

    window.SMWPRefreshCompareCharts = function (dateText) {
        window.SMWP_SELECTED_DATE = dateText || window.SMWP_SELECTED_DATE || smwpDisplayTodayText();

        smwpRefreshCompareCharts(window.SMWP_SELECTED_DATE);
        smwpStartAllChartRealtime(window.SMWP_SELECTED_DATE);
    };

    $(document).ready(function () {
        var initDate =
            window.SMWP_SELECTED_DATE ||
            smwpDisplayTodayText();

        setTimeout(function () {
            smwpRefreshEnergyChart(initDate);
            smwpRefreshCompareCharts(initDate);

            if (smwpDisplayIsToday(initDate)) {
                smwpStartAllChartRealtime(initDate);
            }
        }, 500);
    });
}