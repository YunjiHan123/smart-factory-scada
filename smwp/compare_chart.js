option = {
    backgroundColor: '#071827',
    title: {
        text: '전기 (kWh)',
        left: 'center',
        top: 2,
        textStyle: {
            color: '#1e9bff',
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
        axisLabel: { color: '#d8e6f3', fontSize: 9 }
    },
    yAxis: { type: 'value', show: false },
    series: [
        {
            type: 'bar',
            barWidth: 28,
            data: [
                { value: 0, itemStyle: { color: '#8b969e' } },
                { value: 0, itemStyle: { color: '#1e9bff' } }
            ],
            label: {
                show: true,
                position: 'top',
                color: '#d8e6f3',
                fontSize: 8,
                fontWeight: 'bold'
            }
        }
    ],
    graphic: [
        {
            type: 'text',
            right: 8,
            top: 62,
            style: {
                text: '- 0.0%',
                fill: '#d8e6f3',
                fontSize: 10,
                fontWeight: 'bold'
            }
        }
    ]
};