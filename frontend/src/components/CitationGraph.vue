<template>
  <div class="citation-graph-container" v-loading="loading">
    <div v-if="empty" class="empty-state">
      <el-empty description="暂无引用网络数据" :image-size="80" />
    </div>
    <div v-else ref="chartRef" class="echarts-box"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  network: {
    type: Object,
    default: () => ({ nodes: [], edges: [], pagerankScores: {} })
  },
  paperTitle: {
    type: String,
    default: '当前论文'
  },
  height: {
    type: String,
    default: '420px'
  }
})

const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null

const empty = computed(() => {
  return !props.network?.nodes?.length
})

function buildChartOption() {
  const nodes = props.network?.nodes || []
  const edges = props.network?.edges || []
  const pagerank = props.network?.pagerankScores || {}

  const maxPR = Math.max(...nodes.map(n => pagerank[n.id] || 0.5), 1)

  const graphNodes = nodes.map((n, idx) => {
    const pr = pagerank[n.id] || 0.5
    const size = 25 + Math.round((pr / maxPR) * 45)
    const isCenter = idx === 0 || n.isCenter
    return {
      id: n.id || String(idx),
      name: n.title || n.name || `文献${idx + 1}`,
      symbolSize: size,
      category: isCenter ? 0 : (pr / maxPR > 0.6 ? 1 : 2),
      value: (pr * 100).toFixed(1),
      label: {
        show: size > 40 || isCenter,
        fontSize: isCenter ? 13 : 11,
        fontWeight: isCenter ? 'bold' : 'normal'
      },
      itemStyle: {
        color: isCenter ? '#409eff' : (pr / maxPR > 0.6 ? '#67c23a' : '#909399'),
        borderColor: isCenter ? '#1d6fc1' : '#fff',
        borderWidth: isCenter ? 3 : 2,
        shadowBlur: isCenter ? 18 : 8,
        shadowColor: isCenter ? 'rgba(64,158,255,0.5)' : 'rgba(0,0,0,0.1)'
      }
    }
  })

  const graphLinks = edges.map(e => ({
    source: e.source || String(e.from),
    target: e.target || String(e.to),
    lineStyle: {
      color: '#c0c4cc',
      width: 1.5,
      curveness: 0.15
    }
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<b>${params.data.name}</b><br/>PageRank: ${params.data.value}`
        }
        return '引用关系'
      }
    },
    legend: [{
      data: ['当前论文', '核心参考文献', '一般参考文献'],
      top: 4,
      right: 10
    }],
    animationDuration: 1200,
    animationEasingUpdate: 'quinticInOut',
    series: [{
      type: 'graph',
      layout: 'force',
      data: graphNodes,
      links: graphLinks,
      categories: [
        { name: '当前论文' },
        { name: '核心参考文献' },
        { name: '一般参考文献' }
      ],
      roam: true,
      draggable: true,
      force: {
        repulsion: 450,
        gravity: 0.08,
        edgeLength: [80, 160],
        layoutAnimation: true
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3.5, color: '#409eff' },
        itemStyle: { shadowBlur: 25 }
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8]
    }]
  }
}

function render() {
  if (!chartRef.value || empty.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption(buildChartOption(), true)
}

function handleResize() {
  chartInstance?.resize()
}

watch(() => props.network, () => {
  nextTick(render)
}, { deep: true })

onMounted(() => {
  nextTick(render)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.citation-graph-container { width: 100%; }
.echarts-box { width: 100%; min-height: v-bind(height); background: var(--color-bg-primary); border-radius: 10px; }
.empty-state { padding: 40px 0; }
</style>
