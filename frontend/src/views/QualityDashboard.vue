<template>
  <div class="quality-dashboard-container" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><DataAnalysis /></el-icon>
        {{ userStore.isAdmin ? '评审质量监控' : '我的评审质量' }}
      </h2>
    </div>

    <el-row :gutter="20" class="kpi-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="(kpi, index) in kpiCards" :key="index">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-inner">
            <div class="kpi-icon" :style="{ background: kpi.bgColor }">
              <el-icon :size="24" :color="'#fff'">
                <component :is="kpi.icon" />
              </el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-value">
                {{ kpi.value }}
                <span v-if="kpi.trend !== undefined" class="trend-badge" :class="kpi.trend >= 0 ? 'up' : 'down'">
                  <el-icon><component :is="kpi.trend >= 0 ? Top : Bottom" /></el-icon>
                  {{ Math.abs(kpi.trend) }}%
                </span>
              </div>
              <div class="kpi-label">{{ kpi.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><TrendCharts /></el-icon>
                评分分布
              </span>
            </div>
          </template>
          <div ref="scoreChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><PieChart /></el-icon>
                评审时长分布
              </span>
            </div>
          </template>
          <div ref="durationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><DataLine /></el-icon>
                近6个月质量趋势
              </span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="ranking-row" v-if="userStore.isAdmin">
      <el-col :span="24">
        <el-card class="ranking-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><Trophy /></el-icon>
                评审人排行榜
              </span>
              <el-radio-group v-model="rankingSortBy" size="small" @change="handleSortChange">
                <el-radio-button value="overall">综合质量</el-radio-button>
                <el-radio-button value="speed">评审速度</el-radio-button>
                <el-radio-button value="consistency">评分一致性</el-radio-button>
                <el-radio-button value="timeliness">及时完成率</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="sortedReviewers" stripe style="width: 100%">
            <el-table-column label="排名" width="80" align="center">
              <template #default="{ $index }">
                <span v-if="$index === 0" class="rank-medal">🥇</span>
                <span v-else-if="$index === 1" class="rank-medal">🥈</span>
                <span v-else-if="$index === 2" class="rank-medal">🥉</span>
                <span v-else class="rank-number">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="评审人" width="140" />
            <el-table-column prop="institution" label="所属机构" min-width="200" show-overflow-tooltip />
            <el-table-column prop="reviewCount" label="评审数" width="100" sortable />
            <el-table-column prop="avgDuration" label="平均时长(h)" width="120" sortable>
              <template #default="{ row }">
                <span :class="{ 'text-warning': row.avgDuration > 72, 'text-success': row.avgDuration < 24 }">
                  {{ row.avgDuration }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="timelinessRate" label="及时率(%)" width="110" sortable>
              <template #default="{ row }">
                <el-progress
                  :percentage="row.timelinessRate"
                  :stroke-width="6"
                  :color="row.timelinessRate >= 90 ? '#67c23a' : row.timelinessRate >= 70 ? '#e6a23c' : '#f56c6c'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="deviation" label="偏离度" width="100" sortable>
              <template #default="{ row }">
                <span :class="{ 'text-success': row.deviation < 5, 'text-warning': row.deviation >= 5 && row.deviation < 10, 'text-danger': row.deviation >= 10 }">
                  {{ row.deviation }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="overallScore" label="综合得分" width="110" sortable>
              <template #default="{ row }">
                <span class="score-text">{{ row.overallScore }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="qualityLevel" label="质量等级" width="120">
              <template #default="{ row }">
                <el-tag :type="getQualityTagType(row.qualityLevel)" effect="dark" size="small">
                  {{ getQualityLevelText(row.qualityLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="viewReviewerDetail(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="ranking-row" v-else>
      <el-col :span="24">
        <el-card class="ranking-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><User /></el-icon>
                我的评审记录
              </span>
            </div>
          </template>
          <el-table :data="myReviews" stripe style="width: 100%">
            <el-table-column prop="paperTitle" label="论文标题" min-width="260" show-overflow-tooltip />
            <el-table-column prop="authors" label="作者" width="140" show-overflow-tooltip />
            <el-table-column prop="score" label="评分" width="100">
              <template #default="{ row }">
                <el-tag :type="getScoreTagType(row.score)" effect="light">{{ row.score }}分</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="评审时长(h)" width="120" />
            <el-table-column prop="submitTime" label="提交时间" width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'completed' ? 'success' : 'warning'" effect="light">
                  {{ row.status === 'completed' ? '已完成' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="reviewerDetailVisible"
      title="评审人详情"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedReviewer" class="reviewer-detail">
        <el-card class="info-section" shadow="never">
          <div class="reviewer-info-header">
            <el-avatar :size="64" :icon="UserFilled" class="reviewer-avatar" />
            <div class="reviewer-basic">
              <h3 class="reviewer-name">{{ selectedReviewer.name }}</h3>
              <p class="reviewer-institution">{{ selectedReviewer.institution }}</p>
              <el-tag :type="getQualityTagType(selectedReviewer.qualityLevel)" effect="dark" size="large">
                {{ getQualityLevelText(selectedReviewer.qualityLevel) }}
              </el-tag>
            </div>
          </div>
          <el-descriptions :column="3" border size="small" class="reviewer-stats">
            <el-descriptions-item label="总评审数">
              {{ selectedReviewer.reviewCount }}
            </el-descriptions-item>
            <el-descriptions-item label="平均时长">
              {{ selectedReviewer.avgDuration }}小时
            </el-descriptions-item>
            <el-descriptions-item label="及时完成率">
              {{ selectedReviewer.timelinessRate }}%
            </el-descriptions-item>
            <el-descriptions-item label="评分偏离度">
              {{ selectedReviewer.deviation }}
            </el-descriptions-item>
            <el-descriptions-item label="综合得分">
              <span class="score-text">{{ selectedReviewer.overallScore }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="加入时间">
              {{ selectedReviewer.joinDate }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="radar-section" shadow="never">
          <template #header>
            <span class="section-title">
              <el-icon><Monitor /></el-icon>
              质量指标雷达图
            </span>
          </template>
          <div ref="radarChartRef" class="radar-container"></div>
        </el-card>

        <el-card class="history-section" shadow="never">
          <template #header>
            <span class="section-title">
              <el-icon><Clock /></el-icon>
              近期评审记录
            </span>
          </template>
          <el-table :data="selectedReviewer.recentReviews" stripe size="small">
            <el-table-column prop="paperTitle" label="论文标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="score" label="评分" width="80" />
            <el-table-column prop="duration" label="时长(h)" width="100" />
            <el-table-column prop="submitTime" label="提交时间" width="140" />
          </el-table>
        </el-card>
      </div>
      <template #footer>
        <el-button @click="reviewerDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  Document,
  Clock,
  Timer,
  TrendCharts,
  PieChart,
  DataLine,
  Trophy,
  User,
  UserFilled,
  Monitor,
  Top,
  Bottom
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  getDashboard,
  getReviewerQuality,
  getReviewerRanking,
  getScoreDistribution,
  getDurationDistribution,
  getTrendData
} from '@/api/quality'

const userStore = useUserStore()
const loading = ref(false)
const rankingSortBy = ref('overall')
const reviewerDetailVisible = ref(false)
const selectedReviewer = ref(null)

const scoreChartRef = shallowRef(null)
const durationChartRef = shallowRef(null)
const trendChartRef = shallowRef(null)
const radarChartRef = shallowRef(null)

const kpiCards = computed(() => {
  const base = [
    { label: '总评审数', value: '1,286', icon: Document, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', trend: 12 },
    { label: '平均评审时长', value: '48.5h', icon: Timer, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', trend: -8 },
    { label: '及时完成率', value: '89.2%', icon: Clock, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', trend: 5 },
    { label: '平均评分偏离度', value: '6.8', icon: DataAnalysis, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', trend: -15 }
  ]
  if (!userStore.isAdmin) {
    return [
      { label: '我的评审数', value: '28', icon: Document, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', trend: 8 },
      { label: '平均时长', value: '36.2h', icon: Timer, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', trend: -12 },
      { label: '及时完成率', value: '92.5%', icon: Clock, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', trend: 3 },
      { label: '偏离度', value: '4.2', icon: DataAnalysis, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', trend: -8 }
    ]
  }
  return base
})

const reviewerList = ref([
  {
    id: 1,
    name: '李教授',
    institution: '清华大学计算机科学与技术系',
    reviewCount: 156,
    avgDuration: 28.5,
    timelinessRate: 96,
    deviation: 3.2,
    overallScore: 95,
    qualityLevel: 'EXCELLENT',
    joinDate: '2020-03-15',
    recentReviews: [
      { paperTitle: '基于深度学习的图像识别研究', score: 85, duration: 24, submitTime: '2026-06-15' },
      { paperTitle: '自然语言处理中的注意力机制', score: 78, duration: 36, submitTime: '2026-06-10' },
      { paperTitle: '分布式系统一致性算法研究', score: 92, duration: 20, submitTime: '2026-06-05' }
    ]
  },
  {
    id: 2,
    name: '王研究员',
    institution: '中国科学院计算技术研究所',
    reviewCount: 128,
    avgDuration: 35.2,
    timelinessRate: 91,
    deviation: 5.8,
    overallScore: 88,
    qualityLevel: 'GOOD',
    joinDate: '2020-06-20',
    recentReviews: [
      { paperTitle: '区块链技术在供应链中的应用', score: 82, duration: 48, submitTime: '2026-06-14' },
      { paperTitle: '知识图谱构建方法研究', score: 75, duration: 28, submitTime: '2026-06-08' }
    ]
  },
  {
    id: 3,
    name: '张教授',
    institution: '北京大学软件学院',
    reviewCount: 98,
    avgDuration: 42.8,
    timelinessRate: 85,
    deviation: 7.5,
    overallScore: 82,
    qualityLevel: 'GOOD',
    joinDate: '2021-01-10',
    recentReviews: [
      { paperTitle: '推荐系统中的冷启动问题研究', score: 88, duration: 36, submitTime: '2026-06-12' }
    ]
  },
  {
    id: 4,
    name: '刘副教授',
    institution: '浙江大学计算机学院',
    reviewCount: 76,
    avgDuration: 56.3,
    timelinessRate: 72,
    deviation: 12.3,
    overallScore: 68,
    qualityLevel: 'FAIR',
    joinDate: '2021-05-08',
    recentReviews: []
  },
  {
    id: 5,
    name: '陈讲师',
    institution: '上海交通大学电子信息学院',
    reviewCount: 45,
    avgDuration: 78.5,
    timelinessRate: 58,
    deviation: 15.8,
    overallScore: 52,
    qualityLevel: 'POOR',
    joinDate: '2022-03-20',
    recentReviews: []
  }
])

const myReviews = ref([
  { paperTitle: '基于深度学习的图像识别研究与应用', authors: '张三, 李四', score: 85, duration: 24, submitTime: '2026-06-15', status: 'completed' },
  { paperTitle: '分布式系统一致性算法优化研究', authors: '王五, 赵六', score: 72, duration: 48, submitTime: '2026-06-10', status: 'completed' },
  { paperTitle: '自然语言处理中的注意力机制研究', authors: '孙七, 周八', score: 88, duration: 36, submitTime: '2026-06-05', status: 'completed' },
  { paperTitle: '区块链技术在供应链金融中的应用', authors: '吴九, 郑十', score: 0, duration: 0, submitTime: '-', status: 'pending' }
])

const sortedReviewers = computed(() => {
  const list = [...reviewerList.value]
  const sortKey = rankingSortBy.value
  const sortMap = {
    overall: 'overallScore',
    speed: 'avgDuration',
    consistency: 'deviation',
    timeliness: 'timelinessRate'
  }
  const key = sortMap[sortKey] || 'overallScore'
  list.sort((a, b) => {
    if (key === 'avgDuration' || key === 'deviation') {
      return a[key] - b[key]
    }
    return b[key] - a[key]
  })
  return list
})

function getQualityTagType(level) {
  const map = { EXCELLENT: 'success', GOOD: 'primary', FAIR: 'warning', POOR: 'danger' }
  return map[level] || 'info'
}

function getQualityLevelText(level) {
  const map = { EXCELLENT: '优秀', GOOD: '良好', FAIR: '一般', POOR: '较差' }
  return map[level] || '未知'
}

function getScoreTagType(score) {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

async function loadDashboardData() {
  try {
    const [scoreRes, durationRes, trendRes] = await Promise.all([
      getScoreDistribution(),
      getDurationDistribution(),
      getTrendData()
    ])
    if (userStore.isAdmin) {
      const rankingRes = await getReviewerRanking({ sortBy: rankingSortBy.value })
      const rankingData = rankingRes.data || rankingRes
      if (rankingData.list && rankingData.list.length) {
        reviewerList.value = rankingData.list
      }
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

function initScoreChart() {
  if (!scoreChartRef.value) return
  const chart = echarts.init(scoreChartRef.value)
  const data = [128, 256, 412, 490]
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['0-25', '25-50', '50-75', '75-100'],
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    series: [{
      name: '评审数量',
      type: 'bar',
      barWidth: '50%',
      data: data,
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#764ba2' },
            { offset: 1, color: '#667eea' }
          ]),
          shadowBlur: 10,
          shadowColor: 'rgba(102, 126, 234, 0.5)'
        }
      },
      label: {
        show: true,
        position: 'top',
        color: '#606266',
        fontSize: 12,
        fontWeight: 500
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function initDurationChart() {
  if (!durationChartRef.value) return
  const chart = echarts.init(durationChartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#606266', fontSize: 12 }
    },
    series: [{
      name: '评审时长',
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      labelLine: { show: false },
      data: [
        { value: 256, name: '0-1天', itemStyle: { color: '#67c23a' } },
        { value: 412, name: '1-3天', itemStyle: { color: '#409eff' } },
        { value: 328, name: '3-7天', itemStyle: { color: '#e6a23c' } },
        { value: 290, name: '>7天', itemStyle: { color: '#f56c6c' } }
      ],
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const months = ['2026-01', '2026-02', '2026-03', '2026-04', '2026-05', '2026-06']
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['平均时长(h)', '平均评分', '及时完成率(%)'],
      top: 0,
      textStyle: { color: '#606266', fontSize: 12 }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    yAxis: [
      {
        type: 'value',
        name: '时长/评分',
        splitLine: { lineStyle: { color: '#f2f3f5' } },
        axisLabel: { color: '#606266', fontSize: 12 }
      },
      {
        type: 'value',
        name: '及时率(%)',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: {
          color: '#606266',
          fontSize: 12,
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '平均时长(h)',
        type: 'line',
        yAxisIndex: 0,
        data: [52.3, 48.5, 55.2, 42.8, 38.5, 36.2],
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#667eea',
          shadowBlur: 10,
          shadowColor: 'rgba(102, 126, 234, 0.3)'
        },
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ])
        },
        animationDuration: 2000
      },
      {
        name: '平均评分',
        type: 'line',
        yAxisIndex: 0,
        data: [72.5, 74.2, 71.8, 76.5, 78.3, 80.1],
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#f5576c',
          shadowBlur: 10,
          shadowColor: 'rgba(245, 87, 108, 0.3)'
        },
        itemStyle: { color: '#f5576c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 87, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 87, 108, 0.05)' }
          ])
        },
        animationDuration: 2000,
        animationDelay: 300
      },
      {
        name: '及时完成率(%)',
        type: 'line',
        yAxisIndex: 1,
        data: [78.5, 82.3, 79.6, 85.2, 87.8, 89.2],
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#43e97b',
          shadowBlur: 10,
          shadowColor: 'rgba(67, 233, 123, 0.3)'
        },
        itemStyle: { color: '#43e97b' },
        animationDuration: 2000,
        animationDelay: 600
      }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

function initRadarChart() {
  if (!radarChartRef.value || !selectedReviewer.value) return
  const chart = echarts.init(radarChartRef.value)
  const r = selectedReviewer.value
  chart.setOption({
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: [
        { name: '评审速度', max: 100 },
        { name: '评分一致性', max: 100 },
        { name: '及时完成率', max: 100 },
        { name: '意见质量', max: 100 },
        { name: '综合分', max: 100 }
      ],
      center: ['50%', '55%'],
      radius: '65%',
      axisName: {
        color: '#606266',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.1)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      }
    },
    series: [{
      name: '质量指标',
      type: 'radar',
      data: [{
        value: [
          Math.max(0, 100 - (r.avgDuration - 24) * 1.5),
          Math.max(0, 100 - r.deviation * 5),
          r.timelinessRate,
          88,
          r.overallScore
        ],
        name: r.name,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 2,
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.6)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        itemStyle: {
          color: '#409eff'
        }
      }],
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function handleSortChange() {
  ElMessage.info(`已按${rankingSortBy.value === 'overall' ? '综合质量' : rankingSortBy.value === 'speed' ? '评审速度' : rankingSortBy.value === 'consistency' ? '评分一致性' : '及时完成率'}排序`)
}

function viewReviewerDetail(row) {
  selectedReviewer.value = row
  reviewerDetailVisible.value = true
  nextTick(() => {
    initRadarChart()
  })
}

watch(reviewerDetailVisible, (val) => {
  if (val) {
    nextTick(() => {
      initRadarChart()
    })
  }
})

onMounted(async () => {
  loading.value = true
  try {
    await loadDashboardData()
  } finally {
    loading.value = false
  }
  await nextTick()
  initScoreChart()
  initDurationChart()
  initTrendChart()
})
</script>

<style scoped>
.quality-dashboard-container {
  padding: 20px;
  background-color: var(--color-bg-secondary);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title .el-icon {
  color: var(--color-primary);
}

.kpi-row,
.charts-row,
.ranking-row {
  margin-bottom: 20px;
}

.kpi-card {
  border-radius: var(--border-radius);
  transition: transform var(--transition-duration);
}

.kpi-card:hover {
  transform: translateY(-3px);
}

:deep(.kpi-card .el-card__body) {
  padding: 20px;
}

.kpi-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}

.kpi-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.kpi-info {
  flex: 1;
  min-width: 0;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
}

.trend-badge.up {
  color: #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
}

.trend-badge.down {
  color: #f56c6c;
  background-color: rgba(245, 108, 108, 0.1);
}

.kpi-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.chart-card {
  border-radius: var(--border-radius);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title,
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.chart-title .el-icon,
.section-title .el-icon {
  color: var(--color-primary);
}

.chart-container {
  height: 340px;
  width: 100%;
}

.trend-chart {
  height: 380px;
}

.ranking-card {
  border-radius: var(--border-radius);
}

.rank-medal {
  font-size: 20px;
}

.rank-number {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.score-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-warning {
  color: #e6a23c;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.reviewer-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-section,
.radar-section,
.history-section {
  border: 1px solid var(--color-border-light) !important;
}

.reviewer-info-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.reviewer-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.reviewer-basic {
  flex: 1;
}

.reviewer-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 6px 0;
}

.reviewer-institution {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 10px 0;
}

.reviewer-stats {
  margin-top: 16px;
}

.radar-container {
  height: 320px;
  width: 100%;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .reviewer-info-header {
    flex-direction: column;
    text-align: center;
  }
}
</style>
