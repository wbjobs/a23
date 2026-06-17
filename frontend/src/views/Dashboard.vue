<template>
  <div class="dashboard-container">
    <div class="welcome-bar">
      <div>
        <h2 class="welcome-title">
          欢迎回来，{{ userStore.userInfo?.name || userStore.userInfo?.username || '用户' }} 👋
        </h2>
        <p class="welcome-subtitle">
          今天是 {{ todayStr }}，祝您工作愉快！
        </p>
      </div>
      <el-tag v-if="userStore.isAdmin" type="danger" effect="dark" size="large">管理员</el-tag>
      <el-tag v-else-if="userStore.isReviewer" type="success" effect="dark" size="large">评审人</el-tag>
      <el-tag v-else type="primary" effect="dark" size="large">作者</el-tag>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="(item, index) in statsCards" :key="index">
        <el-card class="stats-card" shadow="hover">
          <div class="card-inner">
            <div class="card-icon" :style="{ background: item.bgColor }">
              <el-icon :size="26" :color="'#fff'">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="card-info">
              <div class="card-value">
                {{ item.value }}
                <span v-if="item.trend" class="trend-badge" :class="item.trend > 0 ? 'up' : 'down'">
                  <el-icon><component :is="item.trend > 0 ? Top : Bottom" /></el-icon>
                  {{ Math.abs(item.trend) }}%
                </span>
              </div>
              <div class="card-label">{{ item.label }}</div>
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
                近7天论文上传趋势
              </span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="chart-title">
                <el-icon><PieChart /></el-icon>
                研究领域关键词分布
              </span>
            </div>
          </template>
          <div ref="keywordChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="tables-row">
      <el-col :span="24">
        <el-card class="table-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title">
                <el-icon><Document /></el-icon>
                最近上传论文
              </span>
              <el-button type="primary" link :icon="ArrowRight" @click="$router.push('/paper/list')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="recentPapers" stripe style="width: 100%" v-loading="loading">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column prop="title" label="论文标题" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push(`/paper/detail/${row.id}`)">
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="authors" label="作者" width="160" show-overflow-tooltip />
            <el-table-column prop="field" label="研究领域" width="140">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ getFieldText(row.field) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getStatusType(row.status)" effect="light">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submissionDate" label="提交日期" width="130">
              <template #default="{ row }">
                {{ formatDate(row.submissionDate) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="$router.push(`/paper/detail/${row.id}`)">
                  查看详情
                </el-button>
                <el-button
                  v-if="(userStore.isReviewer || userStore.isAdmin) && row.status === 'reviewing'"
                  type="success"
                  size="small"
                  link
                  @click="$router.push(`/review/review/${row.id}`)"
                >
                  评审
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Document,
  Clock,
  EditPen,
  User as UserIcon,
  Check,
  PieChart,
  TrendCharts,
  ArrowRight,
  Top,
  Bottom
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getPaperList } from '@/api/paper'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const trendChartRef = shallowRef(null)
const keywordChartRef = shallowRef(null)

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const baseStats = [
  { label: '论文总数', value: 128, icon: Document, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', trend: 12 },
  { label: '待评审', value: 23, icon: Clock, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', trend: 5 },
  { label: '已评审', value: 68, icon: Check, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', trend: 18 },
  { label: '用户总数', value: 342, icon: UserIcon, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', trend: 8 }
]

const authorStats = [
  { label: '我的论文', value: 8, icon: Document, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', trend: 25 },
  { label: '评审中', value: 3, icon: EditPen, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', trend: 0 },
  { label: '已录用', value: 4, icon: Check, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', trend: 33 },
  { label: '待处理', value: 1, icon: Clock, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', trend: -50 }
]

const reviewerStats = [
  { label: '待评审', value: 5, icon: Clock, bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', trend: 20 },
  { label: '评审中', value: 2, icon: EditPen, bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', trend: 0 },
  { label: '已完成', value: 28, icon: Check, bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', trend: 16 },
  { label: '平均评分', value: '7.8', icon: Document, bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', trend: 5 }
]

const statsCards = computed(() => {
  if (userStore.isAdmin) return baseStats
  if (userStore.isReviewer) return reviewerStats
  return authorStats
})

const recentPapers = ref([])

const mockPapers = [
  {
    id: 1,
    title: '基于深度学习的图像识别研究与应用',
    authors: '张三, 李四',
    field: 'machine_learning',
    status: 'reviewing',
    submissionDate: '2026-06-15'
  },
  {
    id: 2,
    title: '分布式系统一致性算法优化研究',
    authors: '王五, 赵六',
    field: 'computer_science',
    status: 'pending',
    submissionDate: '2026-06-14'
  },
  {
    id: 3,
    title: '自然语言处理中的注意力机制研究综述',
    authors: '孙七, 周八',
    field: 'artificial_intelligence',
    status: 'accepted',
    submissionDate: '2026-06-12'
  },
  {
    id: 4,
    title: '区块链技术在供应链金融中的应用探索',
    authors: '吴九, 郑十',
    field: 'data_science',
    status: 'pending',
    submissionDate: '2026-06-10'
  },
  {
    id: 5,
    title: '基于知识图谱的推荐系统构建与实现',
    authors: '钱十一, 孙十二',
    field: 'data_science',
    status: 'rejected',
    submissionDate: '2026-06-08'
  }
]

function getFieldText(field) {
  const map = {
    computer_science: '计算机科学',
    artificial_intelligence: '人工智能',
    machine_learning: '机器学习',
    data_science: '数据科学',
    software_engineering: '软件工程',
    other: '其他'
  }
  return map[field] || field || '未分类'
}

function getStatusType(status) {
  const map = { pending: 'warning', reviewing: 'primary', accepted: 'success', rejected: 'danger', published: 'success' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { pending: '待评审', reviewing: '评审中', accepted: '已录用', rejected: '已拒稿', published: '已发表' }
  return map[status] || status || '未知'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch (e) {
    return dateStr
  }
}

function getLast7Days() {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return days
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: getLast7Days(),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '论文上传数',
        type: 'bar',
        barWidth: '40%',
        data: [8, 12, 6, 15, 10, 18, 14],
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
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
            ])
          }
        }
      }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

function initKeywordChart() {
  if (!keywordChartRef.value) return
  const chart = echarts.init(keywordChartRef.value)
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
    series: [
      {
        name: '研究领域',
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
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: { show: false },
        data: [
          { value: 42, name: '机器学习', itemStyle: { color: '#667eea' } },
          { value: 35, name: '人工智能', itemStyle: { color: '#f5576c' } },
          { value: 22, name: '数据科学', itemStyle: { color: '#4facfe' } },
          { value: 18, name: '计算机科学', itemStyle: { color: '#43e97b' } },
          { value: 11, name: '软件工程', itemStyle: { color: '#f093fb' } }
        ]
      }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

async function loadRecentPapers() {
  loading.value = true
  try {
    const res = await getPaperList({ page: 1, pageSize: 5 })
    const data = res.data || res
    const list = data.list || data.records || data.rows || []
    recentPapers.value = list.length ? list : mockPapers
  } catch (e) {
    console.error('加载论文列表失败', e)
    recentPapers.value = mockPapers
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loadRecentPapers()
  await nextTick()
  initTrendChart()
  initKeywordChart()
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
}

.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 28px;
  border-radius: var(--border-radius);
  margin-bottom: 20px;
  color: #fff;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.welcome-subtitle {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  border-radius: var(--border-radius);
  transition: transform var(--transition-duration);
}

.stats-card:hover {
  transform: translateY(-3px);
}

:deep(.stats-card .el-card__body) {
  padding: 20px;
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 58px;
  height: 58px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 28px;
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
  font-size: 12px;
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

.card-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 6px;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: var(--border-radius);
}

.chart-container {
  height: 340px;
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title,
.card-header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.chart-title .el-icon,
.card-header-title .el-icon {
  color: var(--color-primary);
}

.table-card {
  border-radius: var(--border-radius);
}

.tables-row {
  margin-bottom: 0;
}
</style>
