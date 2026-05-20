var emptyLabels = [
    '00:00', '01:00', '02:00', '03:00', '04:00', '05:00',
    '06:00', '07:00', '08:00', '09:00', '10:00', '11:00',
    '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
    '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'
];

var emptyData = [
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0
];

option = {
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
        data: emptyLabels,
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
            lineStyle: { width: 2.5, color: '#1e90ff' },
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
            data: emptyData
        },
        {
            name: '가스 (m3)',
            type: 'line',
            smooth: true,
            showSymbol: false,
            lineStyle: { width: 2.2, color: '#ff8c00' },
            itemStyle: { color: '#ff8c00' },
            data: emptyData
        },
        {
            name: '용수 (ton)',
            type: 'line',
            smooth: true,
            showSymbol: false,
            lineStyle: { width: 2.2, color: '#00c8e8' },
            itemStyle: { color: '#00c8e8' },
            data: emptyData
        },
        {
            name: '태양광 (kWh)',
            type: 'line',
            smooth: true,
            showSymbol: false,
            lineStyle: { width: 2.2, color: '#59d33f' },
            itemStyle: { color: '#59d33f' },
            data: emptyData
        }
    ]
};