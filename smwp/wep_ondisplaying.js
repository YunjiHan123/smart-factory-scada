if (!window.SMWP_CHART_DISPLAYING_READY) {

    window.SMWP_CHART_DISPLAYING_READY = true;
    var SMWP_API_BASE_URL = 'http://localhost:8080';
    var SMWP_DEFAULT_PLANT_NAME = '현대 아산';

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
    
    function smwpIsToday(dateText) {
        return dateText === smwpTodayText();
    }
    
    function smwpPlantName() {
        return SMWP_DEFAULT_PLANT_NAME;
    }

    var SMWP_CHART_DOM_ID = CustomCharts5.id;

    var SMWP_CHART_TIMER = null;
    var SMWP_CHART_REFRESH_MS = 1000;
    var SMWP_CHART_REQUEST_RUNNING = false;
    var SMWP_ENERGY_CHART = null;

    function smwpChartLabels() {
        return [
            '00:00', '01:00', '02:00', '03:00', '04:00', '05:00',
            '06:00', '07:00', '08:00', '09:00', '10:00', '11:00',
            '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
            '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'
        ];
    }

    function smwpChartFindInstance() {
        var root = document.getElementById(SMWP_CHART_DOM_ID);

        if (!root) {
            console.warn('[SMWP Chart] root dom not found:', SMWP_CHART_DOM_ID);
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

        return null;
    }

    function smwpChartApiUrl(dateText) {
        return SMWP_API_BASE_URL
            + '/api/smwp/energy/hourly'
            + '?plantName=' + encodeURIComponent(smwpPlantName())
            + '&date=' + encodeURIComponent(dateText)
            + '&_t=' + new Date().getTime();
    }

    function smwpChartArray(values, dateText) {
        values = values || [];

        var result = [];
        var nowHour = new Date().getHours();
        var isToday = smwpIsToday(dateText);

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

    function smwpEnergyChartOption(data, dateText) {
        data = data || {};

        return {
            backgroundColor: '#071827',
            tooltip: {
                trigger: 'axis',
                backgroundColor: 'rgba(7, 24, 39, 0.9)',
                borderColor: '#1f3b55',
                borderWidth: 1,
                textStyle: { color: '#ffffff' },
                axisPointer: {
                    type: 'line',
                    lineStyle: {
                        color: '#3c6382',
                        width: 1,
                        type: 'dashed'
                    }
                }
            },
            legend: {
                top: 16,
                right: 30,
                itemWidth: 18,
                itemHeight: 3,
                textStyle: {
                    color: '#d8e6f3',
                    fontSize: 11
                },
                data: [
                    '전기 (kWh)',
                    '가스 (m3)',
                    '용수 (ton)',
                    '태양광 (kWh)'
                ]
            },
            grid: {
                left: 48,
                right: 22,
                top: 48,
                bottom: 36
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: smwpChartLabels(),
                axisLine: {
                    lineStyle: { color: '#29445c' }
                },
                axisTick: { show: false },
                axisLabel: {
                    color: '#b8c7d5',
                    fontSize: 11
                },
                splitLine: { show: false }
            },
            yAxis: {
                type: 'value',
                name: '(사용량)',
                nameTextStyle: {
                    color: '#d8e6f3',
                    fontSize: 11,
                    padding: [0, 28, 0, 0]
                },
                min: 0,
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: {
                    color: '#b8c7d5',
                    fontSize: 11,
                    formatter: function (value) {
                        return Number(value).toLocaleString();
                    }
                },
                splitLine: {
                    lineStyle: {
                        color: '#183247',
                        type: 'dashed'
                    }
                }
            },
            series: [
                {
                    name: '전기 (kWh)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: {
                        width: 2.5,
                        color: '#1e90ff'
                    },
                    itemStyle: { color: '#1e90ff' },
                    areaStyle: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 0,
                            y2: 1,
                            colorStops: [
                                { offset: 0, color: 'rgba(30, 144, 255, 0.22)' },
                                { offset: 1, color: 'rgba(30, 144, 255, 0.02)' }
                            ]
                        }
                    },
                    data: smwpChartArray(data.electricityKwh, dateText)
                },
                {
                    name: '가스 (m3)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: {
                        width: 2.2,
                        color: '#ff8c00'
                    },
                    itemStyle: { color: '#ff8c00' },
                    data: smwpChartArray(data.gasM3, dateText)
                },
                {
                    name: '용수 (ton)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: {
                        width: 2.2,
                        color: '#00c8e8'
                    },
                    itemStyle: { color: '#00c8e8' },
                    data: smwpChartArray(data.waterTon, dateText)
                },
                {
                    name: '태양광 (kWh)',
                    type: 'line',
                    smooth: true,
                    showSymbol: false,
                    lineStyle: {
                        width: 2.2,
                        color: '#59d33f'
                    },
                    itemStyle: { color: '#59d33f' },
                    data: smwpChartArray(data.solarKwh, dateText)
                }
            ]
        };
    }

    function smwpRefreshChart(dateText) {
        SMWP_ENERGY_CHART = smwpChartFindInstance();

        if (!SMWP_ENERGY_CHART) {
            console.warn('[SMWP Chart] chart instance not found. skip refresh.');
            return;
        }

        if (SMWP_CHART_REQUEST_RUNNING) {
            return;
        }

        SMWP_CHART_REQUEST_RUNNING = true;

        fetch(smwpChartApiUrl(dateText))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('hourly api failed: ' + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                SMWP_ENERGY_CHART.setOption(
                    smwpEnergyChartOption(data, dateText),
                    true
                );
            })
            .catch(function (error) {
                console.error('[SMWP Chart] API error:', error);
            })
            .then(function () {
                SMWP_CHART_REQUEST_RUNNING = false;
            });
    }

    function smwpStopChartRealtime() {
        if (SMWP_CHART_TIMER) {
            clearTimeout(SMWP_CHART_TIMER);
            SMWP_CHART_TIMER = null;
        }
    }

    function smwpStartChartRealtime() {
        smwpStopChartRealtime();

        if (!smwpIsToday(SMWP_SELECTED_DATE)) {
            return;
        }

        function loop() {
            if (!smwpIsToday(SMWP_SELECTED_DATE)) {
                smwpStopChartRealtime();
                return;
            }

            smwpRefreshChart(SMWP_SELECTED_DATE);

            SMWP_CHART_TIMER = setTimeout(function () {
                loop();
            }, SMWP_CHART_REFRESH_MS);
        }

        loop();
    }

    window.SMWPRefreshEnergyChart = function (dateText) {
        SMWP_SELECTED_DATE = dateText || SMWP_SELECTED_DATE || smwpTodayText();
        window.SMWP_SELECTED_DATE = SMWP_SELECTED_DATE;

        smwpRefreshChart(SMWP_SELECTED_DATE);
        smwpStartChartRealtime();
    };

    $(document).ready(function () {
        var initDate =
            window.SMWP_SELECTED_DATE ||
            SMWP_SELECTED_DATE ||
            smwpTodayText();

        setTimeout(function () {
            window.SMWPRefreshEnergyChart(initDate);
        }, 500);
    });
}