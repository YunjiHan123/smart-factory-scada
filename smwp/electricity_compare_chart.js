if (!window.SMWP_ELECTRICITY_COMPARE_READY) {
    window.SMWP_ELECTRICITY_COMPARE_READY = true;

    var SMWP_COMPARE_API_BASE_URL =
        (typeof window.SMWP_API_BASE_URL !== 'undefined')
            ? window.SMWP_API_BASE_URL
            : 'http://localhost:8080';

    var SMWP_COMPARE_CHART_DOM_ID =
        (typeof CustomCharts4 !== 'undefined' && CustomCharts4.id)
            ? CustomCharts4.id
            : 'CustomCharts4';

    var SMWP_COMPARE_REFRESH_MS = 1000;
    var SMWP_COMPARE_TIMER = null;
    var SMWP_COMPARE_REQUEST_RUNNING = false;
    var SMWP_COMPARE_SELECTED_DATE = '';
    var SMWP_COMPARE_CHART = null;

    function smwpCompareFormatDate(date) {
        var y = date.getFullYear();
        var m = date.getMonth() + 1;
        var d = date.getDate();

        return y + '-' +
            (m < 10 ? '0' + m : m) + '-' +
            (d < 10 ? '0' + d : d);
    }

    function smwpCompareTodayText() {
        return smwpCompareFormatDate(new Date());
    }

    function smwpCompareIsToday(dateText) {
        return dateText === smwpCompareTodayText();
    }

    function smwpComparePlantName() {
        if (typeof window.smwpPlantName === 'function') {
            return window.smwpPlantName();
        }

        return '';
    }

    function smwpCompareChartFindInstance() {
        var root = document.getElementById(SMWP_COMPARE_CHART_DOM_ID);

        if (!root) {
            console.warn('[SMWP Compare] root dom not found:', SMWP_COMPARE_CHART_DOM_ID);
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

    function smwpCompareApiUrl(dateText) {
        return SMWP_COMPARE_API_BASE_URL
            + '/api/smwp/energy/comparison'
            + '?plantName=' + encodeURIComponent(smwpComparePlantName())
            + '&date=' + encodeURIComponent(dateText)
            + '&energyType=ELECTRICITY'
            + '&_t=' + new Date().getTime();
    }

    function smwpCompareNumber(value) {
        var number = Number(value);
        return isNaN(number) ? 0 : number;
    }

    function smwpCompareRateText(rate) {
        rate = smwpCompareNumber(rate);

        if (rate > 0) {
            return '▲ ' + Math.abs(rate).toFixed(1) + '%';
        }

        if (rate < 0) {
            return '▼ ' + Math.abs(rate).toFixed(1) + '%';
        }

        return '- 0.0%';
    }

    function smwpCompareRateColor(rate) {
        rate = smwpCompareNumber(rate);
        if (rate > 0) return '#ff4d4f';
        if (rate < 0) return '#1e9bff';
        return '#d8e6f3';
    }

    function smwpElectricityCompareOption(data) {
        data = data || {};

        var previousMonthAverage = smwpCompareNumber(data.previousMonthAverage);
        var currentUsage = smwpCompareNumber(data.currentUsage);
        var changeRate = smwpCompareNumber(data.changeRate);

        return {
            backgroundColor: '#071827',
            title: {
                text: '전기 (kWh)',
                left: 'center',
                top: 4,
                textStyle: {
                    color: '#1e90ff',
                    fontSize: 13,
                    fontWeight: 'bold'
                }
            },
            grid: {
                left: 20,
                right: 60,
                top: 35,
                bottom: 25
            },
            xAxis: {
                type: 'category',
                data: ['전월 평균', '금일'],
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: {
                    color: '#d8e6f3',
                    fontSize: 10,
                    margin: 8
                }
            },
            yAxis: {
                type: 'value',
                show: false
            },
            series: [
                {
                    type: 'bar',
                    barWidth: 38,
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
                                        { offset: 0, color: '#1e9bff' },
                                        { offset: 1, color: '#006bd6' }
                                    ]
                                }
                            }
                        }
                    ],
                    label: {
                        show: true,
                        position: 'top',
                        color: '#d8e6f3',
                        fontSize: 10,
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
                    right: 12,
                    top: 60,
                    style: {
                        text: smwpCompareRateText(changeRate),
                        fill: smwpCompareRateColor(changeRate),
                        fontSize: 12,
                        fontWeight: 'bold'
                    }
                }
            ]
        };
    }

    function smwpRefreshElectricityCompareChart(dateText) {
        SMWP_COMPARE_SELECTED_DATE = dateText || SMWP_COMPARE_SELECTED_DATE || smwpCompareTodayText();

        if (SMWP_COMPARE_REQUEST_RUNNING) {
            return;
        }

        SMWP_COMPARE_CHART = smwpCompareChartFindInstance();

        if (!SMWP_COMPARE_CHART) {
            console.warn('[SMWP Compare] chart instance not found. skip refresh.');
            return;
        }

        SMWP_COMPARE_REQUEST_RUNNING = true;

        fetch(smwpCompareApiUrl(SMWP_COMPARE_SELECTED_DATE))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('comparison api failed: ' + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                SMWP_COMPARE_CHART.setOption(
                    smwpElectricityCompareOption(data),
                    true
                );
            })
            .catch(function (error) {
                console.error('[SMWP Compare] API error:', error);
            })
            .then(function () {
                SMWP_COMPARE_REQUEST_RUNNING = false;
            });
    }

    function smwpStopElectricityCompareRealtime() {
        if (SMWP_COMPARE_TIMER) {
            clearTimeout(SMWP_COMPARE_TIMER);
            SMWP_COMPARE_TIMER = null;
        }
    }

    function smwpStartElectricityCompareRealtime() {
        smwpStopElectricityCompareRealtime();

        if (!smwpCompareIsToday(SMWP_COMPARE_SELECTED_DATE)) {
            return;
        }

        function loop() {
            if (!smwpCompareIsToday(SMWP_COMPARE_SELECTED_DATE)) {
                smwpStopElectricityCompareRealtime();
                return;
            }

            smwpRefreshElectricityCompareChart(SMWP_COMPARE_SELECTED_DATE);

            SMWP_COMPARE_TIMER = setTimeout(function () {
                loop();
            }, SMWP_COMPARE_REFRESH_MS);
        }

        loop();
    }

    window.SMWPRefreshElectricityCompareChart = function (dateText) {
        SMWP_COMPARE_SELECTED_DATE = dateText || SMWP_COMPARE_SELECTED_DATE || smwpCompareTodayText();
        smwpRefreshElectricityCompareChart(SMWP_COMPARE_SELECTED_DATE);
        smwpStartElectricityCompareRealtime();
    };
}
