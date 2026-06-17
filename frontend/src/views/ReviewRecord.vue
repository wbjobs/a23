<template>
  <div class="review-record-container">
    <div class="page-header-row">
      <h2 class="page-title">评审记录</h2>
      <el-radio-group v-model="viewMode" class="view-switch" size="large" v-if="!isOnlyOneRole">
        <el-radio-button value="author">
          <el-icon><User /></el-icon> 作者视角
        </el-radio-button>
        <el-radio-button value="reviewer">
          <el-icon><EditPen /></el-icon> 评审人视角
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-alert
      :title="viewMode === 'author' ? '作者视角：查看您提交的论文收到的所有评审意见' : '评审人视角：查看您已完成的所有评审记录'"
      :type="viewMode === 'author' ? 'primary' : 'success'"
      :closable="false"
      show-icon
      class="mode-alert"
    />

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="filter-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" :placeholder="viewMode === 'author' ? '论文标题' : '论文标题或评审人'" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="结论" v-if="viewMode === 'author'">
          <el-select v-model="searchForm.decision" placeholder="全部结论" clearable style="width: 140px">
            <el-option label="已录用" value="accept" />
            <el-option label="大修" value="revision" />
            <el-option label="小修" value="minor" />
            <el-option label="已拒稿" value="reject" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分" v-else>
          <el-select v-model="searchForm.scoreLevel" placeholder="全部评分" clearable style="width: 140px">
            <el-option label="优秀(≥85)" value="excellent" />
            <el-option label="良好(70-84)" value="good" />
            <el-option label="一般(55-69)" value="fair" />
            <el-option label="较差(<55)" value="poor" />
          </el-select>
        </el-form-item>
        <el-form-item label="评审时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      :data="pagedData"
      v-loading="loading"
      stripe
      border
      style="width: 100%"
      class="record-table"
    >
      <el-table-column type="index" label="#" width="60" />
      <el-table-column v-if="viewMode === 'author'" label="评审人" width="140">
        <template #default="{ row }">
          <div class="reviewer-cell">
            <el-avatar :size="32" class="mini-avatar">
              {{ (row.reviewerName || 'R').charAt(0).toUpperCase() }}
            </el-avatar>
            <span class="reviewer-name">{{ row.reviewerName || '评审专家' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="论文标题" min-width="260" show-overflow-tooltip />
      <el-table-column v-if="viewMode === 'author'" prop="authors" label="作者" width="140" show-overflow-tooltip />
      <el-table-column v-if="viewMode === 'reviewer'" label="综合评分" width="120">
        <template #default="{ row }">
          <div class="score-display" :class="getScoreLevel(row.score)">
            <el-progress
              type="dashboard"
              :percentage="row.score || 0"
              :width="56"
              :stroke-width="7"
              :show-text="false"
              :color="getScoreColor(row.score)"
            />
            <span class="score-num">{{ row.score }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-else label="平均评分" width="120">
        <template #default="{ row }">
          <el-rate
            :model-value="(row.score || 75) / 10"
            disabled
            :max="10"
            show-score
            text-color="#ff9900"
            score-template="{value}"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="三维评分" v-if="viewMode === 'reviewer'" width="200">
        <template #default="{ row }">
          <div class="dims-row">
            <el-tag size="small" type="danger" effect="plain">创 {{ row.innovation || '-' }}</el-tag>
            <el-tag size="small" type="warning" effect="plain">方 {{ row.methodology || '-' }}</el-tag>
            <el-tag size="small" type="primary" effect="plain">写 {{ row.writing || '-' }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="decision" label="结论" width="100">
        <template #default="{ row }">
          <el-tag :type="getDecisionType(row.decision)" effect="light" size="small">
            {{ getDecisionText(row.decision) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reviewTime" :label="viewMode === 'author' ? '收到时间' : '评审时间'" width="160">
        <template #default="{ row }">
          <div class="date-cell">
            <el-icon :size="14" color="#909399"><Clock /></el-icon>
            {{ formatDate(row.reviewTime) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :icon="View" @click="viewDetail(row)">查看详情</el-button>
          <el-button size="small" link type="primary" @click="$router.push(`/paper/detail/${row.paperId}`)">论文</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredData.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <el-dialog v-model="detailVisible" :title="viewMode === 'author' ? '评审意见详情' : '评审提交详情'" width="820px">
      <div v-if="currentReview" class="dialog-content">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="论文标题" :span="2">
            <b class="title-text">{{ currentReview.title }}</b>
          </el-descriptions-item>
          <el-descriptions-item v-if="viewMode === 'author'" label="评审人">
            <div class="desc-reviewer">
              <el-avatar :size="28" class="mini-avatar">
                {{ (currentReview.reviewerName || 'R').charAt(0).toUpperCase() }}
              </el-avatar>
              <span>{{ currentReview.reviewerName || '评审专家' }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="viewMode === 'author'" label="评审人机构">
            {{ currentReview.reviewerAffiliation || '示例大学' }}
          </el-descriptions-item>
          <el-descriptions-item v-else label="作者">{{ currentReview.authors }}</el-descriptions-item>
          <el-descriptions-item label="评审结论">
            <el-tag :type="getDecisionType(currentReview.decision)" effect="dark" size="small">
              {{ getDecisionText(currentReview.decision) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="综合评分" :span="viewMode === 'reviewer' ? 1 : 2">
            <div class="desc-score-wrap">
              <div class="circle-score" :class="getScoreLevel(currentReview.score)">
                <span class="score-big">{{ currentReview.score || Math.round((currentReview.innovation + currentReview.methodology + currentReview.writing) / 3) }}</span>
                <span class="score-suffix">/100</span>
              </div>
              <div class="level-text">{{ getScoreText(currentReview.score) }}</div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="创新性" v-if="viewMode === 'reviewer'">
            <el-progress :percentage="currentReview.innovation || 0" :stroke-width="10" :color="getScoreColor(currentReview.innovation)" />
          </el-descriptions-item>
          <el-descriptions-item label="方法学" v-if="viewMode === 'reviewer'">
            <el-progress :percentage="currentReview.methodology || 0" :stroke-width="10" :color="getScoreColor(currentReview.methodology)" />
          </el-descriptions-item>
          <el-descriptions-item label="写作质量" v-if="viewMode === 'reviewer'">
            <el-progress :percentage="currentReview.writing || 0" :stroke-width="10" :color="getScoreColor(currentReview.writing)" />
          </el-descriptions-item>
          <el-descriptions-item label="三维评分" v-if="viewMode === 'author'">
            <div class="dims-row">
              <el-tag size="small" type="danger">创 {{ currentReview.innovation || '-' }}</el-tag>
              <el-tag size="small" type="warning">方 {{ currentReview.methodology || '-' }}</el-tag>
              <el-tag size="small" type="primary">写 {{ currentReview.writing || '-' }}</el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item :label="viewMode === 'author' ? '收到时间' : '提交时间'">
            {{ formatDate(currentReview.reviewTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          <span class="divider-title">
            <el-icon><ChatDotRound /></el-icon>
            {{ viewMode === 'author' ? '评审人意见' : '评审意见' }}
          </span>
        </el-divider>
        <div class="comment-box">
          <FormulaAnchorText
            :text="currentReview.comment"
            :paper-id="currentReview.paperId"
            @navigate="handleReviewFormulaNavigate"
          />
        </div>

        <el-divider content-position="left" v-if="currentReview.suggestion">
          <span class="divider-title">
            <el-icon><LightBulb /></el-icon> 修改建议
          </span>
        </el-divider>
        <div class="comment-box suggestion-box" v-if="currentReview.suggestion">
          <FormulaAnchorText
            :text="currentReview.suggestion"
            :paper-id="currentReview.paperId"
            @navigate="handleReviewFormulaNavigate"
          />
        </div>
        <el-empty v-else description="暂无具体修改建议" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="$router.push(`/paper/detail/${currentReview.paperId}`)">查看论文</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, User, EditPen, Clock, View, ChatDotRound, LightBulb } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getReviewsByPaper, getReviewsByReviewer } from '@/api/review'
import FormulaAnchorText from '@/components/FormulaAnchorText.vue'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const detailVisible = ref(false)
const currentReview = ref(null)

const isAuthor = computed(() => userStore.isAuthor)
const isReviewer = computed(() => userStore.isReviewer)
const isAdmin = computed(() => userStore.isAdmin)

const isOnlyOneRole = computed(() => {
  let count = 0
  if (isAuthor.value) count++
  if (isReviewer.value) count++
  return count <= 1
})

const viewMode = ref(isReviewer.value && !isAuthor.value ? 'reviewer' : 'author')

const searchForm = reactive({
  title: '',
  decision: '',
  scoreLevel: '',
  dateRange: []
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10
})

const authorMockData = [
  {
    id: 101, paperId: 1, decision: 'accept', score: 88,
    reviewerName: '王教授', reviewerAffiliation: '清华大学计算机系',
    innovation: 90, methodology: 85, writing: 90,
    title: '基于深度学习的图像识别研究与应用',
    authors: '张三, 李四',
    comment: '本文研究了深度学习在图像识别领域的应用，提出了一种新的卷积神经网络结构。研究内容扎实，实验充分，创新性强，建议录用。论文写作流畅，结构清晰，实验设计合理。',
    suggestion: '建议补充更多与最新 SOTA 方法的对比实验，并在附录中增加更多可视化结果。',
    reviewTime: '2026-06-16 14:30'
  },
  {
    id: 102, paperId: 1, decision: 'revision', score: 72,
    reviewerName: '李研究员', reviewerAffiliation: '中科院计算所',
    innovation: 70, methodology: 78, writing: 68,
    title: '基于深度学习的图像识别研究与应用',
    authors: '张三, 李四',
    comment: '针对传统卷积网络的改进方案有一定参考价值，但实验部分需要加强，消融实验不够充分。建议大修后再审。',
    suggestion: '建议增加大规模数据集上的性能测试，并补充错误分析。',
    reviewTime: '2026-06-15 10:20'
  },
  {
    id: 103, paperId: 2, decision: 'accept', score: 92,
    reviewerName: '陈博士', reviewerAffiliation: '北京大学',
    innovation: 95, methodology: 90, writing: 90,
    title: '自然语言处理中的注意力机制研究',
    authors: '孙七, 周八',
    comment: '非常优秀的研究工作，对注意力机制的改进方案新颖，理论分析充分，实验设计严谨，写作流畅。强烈推荐录用。',
    suggestion: '可增加更多最新相关工作的讨论。',
    reviewTime: '2026-06-14 16:45'
  },
  {
    id: 104, paperId: 3, decision: 'reject', score: 48,
    reviewerName: '赵教授', reviewerAffiliation: '复旦大学',
    innovation: 40, methodology: 55, writing: 50,
    title: '基于知识图谱的构建及应用研究',
    authors: '吴九, 郑十',
    comment: '研究内容较为基础，创新性不足，相关工作讨论不够充分，实验设计存在明显缺陷。',
    suggestion: '建议进一步深化研究，增加创新点，完善实验后重新投稿。',
    reviewTime: '2026-06-13 09:15'
  }
]

const reviewerMockData = [
  {
    id: 201, paperId: 4, decision: 'accept',
    innovation: 88, methodology: 85, writing: 82,
    score: 85,
    title: '分布式系统一致性算法优化研究',
    authors: '王五, 赵六',
    comment: '本文对 Raft 一致性算法提出了有效的优化方案，理论分析正确，实验结果令人信服。建议录用。',
    suggestion: '建议补充大规模集群场景下的实验结果。',
    reviewTime: '2026-06-15 11:30'
  },
  {
    id: 202, paperId: 5, decision: 'minor',
    innovation: 78, methodology: 82, writing: 80,
    score: 80,
    title: '图神经网络在推荐系统中的应用',
    authors: '钱十一, 孙十二',
    comment: '本文研究了图神经网络在推荐系统中的应用，方案可行，实验充分。小修后可录用。',
    suggestion: '需要补充与 GNN 其他变体的对比实验。',
    reviewTime: '2026-06-12 15:00'
  },
  {
    id: 203, paperId: 6, decision: 'reject',
    innovation: 45, methodology: 52, writing: 48,
    score: 48,
    title: '数据挖掘算法初探',
    authors: '周十三, 吴十四',
    comment: '研究内容较为基础，创新性明显不足，实验不充分，写作也需大量改进。',
    suggestion: '建议找准研究问题，深化创新后重投。',
    reviewTime: '2026-06-10 08:45'
  }
]

const rawData = ref([])

const filteredData = computed(() => {
  let data = [...rawData.value]
  if (searchForm.title) {
    const kw = searchForm.title.toLowerCase()
    data = data.filter(d =>
      (d.title || '').toLowerCase().includes(kw) ||
      (d.reviewerName || '').toLowerCase().includes(kw) ||
      (d.authors || '').toLowerCase().includes(kw)
    )
  }
  if (searchForm.decision) {
    data = data.filter(d => d.decision === searchForm.decision)
  }
  if (searchForm.scoreLevel) {
    data = data.filter(d => getScoreLevel(d.score) === searchForm.scoreLevel)
  }
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    const [start, end] = searchForm.dateRange
    data = data.filter(d => {
      const t = new Date(d.reviewTime).getTime()
      return t >= new Date(start).getTime() && t <= new Date(end + ' 23:59:59').getTime()
    })
  }
  return data
})

const pagedData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  return filteredData.value.slice(start, start + pagination.pageSize)
})

async function loadData() {
  loading.value = true
  pagination.currentPage = 1
  try {
    let res
    if (viewMode.value === 'reviewer') {
      res = await getReviewsByReviewer()
      const list = res?.data?.list || res?.data?.records || res?.data || res?.rows || []
      rawData.value = Array.isArray(list) && list.length ? list : reviewerMockData
    } else {
      res = await getReviewsByPaper({ authorOnly: true })
      const list = res?.data?.list || res?.data?.records || res?.data || res?.rows || []
      rawData.value = Array.isArray(list) && list.length ? list : authorMockData
    }
  } catch (e) {
    console.error('加载评审记录失败', e)
    rawData.value = viewMode.value === 'reviewer' ? reviewerMockData : authorMockData
  } finally {
    loading.value = false
  }
}

function getDecisionText(d) {
  return { accept: '录用', revision: '大修', minor: '小修', reject: '拒稿' }[d] || d || '-'
}
function getDecisionType(d) {
  return { accept: 'success', revision: 'warning', minor: 'primary', reject: 'danger' }[d] || 'info'
}

function getScoreLevel(s) {
  const score = s || 0
  if (score >= 85) return 'excellent'
  if (score >= 70) return 'good'
  if (score >= 55) return 'fair'
  return 'poor'
}
function getScoreText(s) {
  return { excellent: '优秀', good: '良好', fair: '一般', poor: '较差' }[getScoreLevel(s)]
}
function getScoreColor(s) {
  return { excellent: '#11998e', good: '#409eff', fair: '#e6a23c', poor: '#f56c6c' }[getScoreLevel(s)]
}

function formatDate(d) {
  if (!d) return '-'
  try { return new Date(d).toLocaleString('zh-CN') } catch (e) { return d }
}

function viewDetail(row) {
  currentReview.value = row
  detailVisible.value = true
}

function handleSearch() { pagination.currentPage = 1 }
function handleReset() {
  searchForm.title = ''
  searchForm.decision = ''
  searchForm.scoreLevel = ''
  searchForm.dateRange = []
  pagination.currentPage = 1
}

function handleReviewFormulaNavigate({ number, page, y }) {
  if (!currentReview.value?.paperId) return
  router.push({
    path: `/paper/detail/${currentReview.value.paperId}`,
    query: { formula: number, page: page ?? '', y: y ?? '' }
  })
}

onMounted(() => { loadData() })
</script>

<style scoped>
.review-record-container { width: 100%; }

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}
.view-switch :deep(.el-radio-button__inner) {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mode-alert {
  margin-bottom: 16px;
  border-radius: var(--border-radius);
}

.filter-card {
  margin-bottom: 20px;
  border-radius: var(--border-radius);
}
:deep(.filter-card .el-card__body) { padding: 16px 20px; }
.filter-form { margin: 0; }

.record-table {
  border-radius: var(--border-radius);
  overflow: hidden;
}

.reviewer-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mini-avatar {
  font-weight: 600;
  font-size: 13px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
.reviewer-name { font-size: 13px; }

.score-display {
  display: flex;
  align-items: center;
  gap: 6px;
}
.score-num {
  font-size: 16px;
  font-weight: 800;
}
.score-display.excellent .score-num { color: #11998e; }
.score-display.good .score-num { color: #409eff; }
.score-display.fair .score-num { color: #e6a23c; }
.score-display.poor .score-num { color: #f56c6c; }

.dims-row {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-regular);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
  margin-top: 20px;
  background-color: var(--color-bg-primary);
  border-radius: var(--border-radius);
}

.dialog-content { padding: 4px 8px; }
.title-text {
  font-size: 15px;
  color: var(--color-text-primary);
}

.desc-reviewer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.desc-score-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
}
.circle-score {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: baseline;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
}
.circle-score.excellent { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.circle-score.good { background: linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%); }
.circle-score.fair { background: linear-gradient(135deg, #f2994a 0%, #f2c94c 100%); }
.circle-score.poor { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); }
.score-big { font-size: 28px; line-height: 1; }
.score-suffix { font-size: 11px; opacity: 0.85; }
.level-text {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(64,158,255,0.1);
  color: var(--color-primary);
}

.divider-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--color-text-regular);
}

.comment-box {
  padding: 16px 18px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
  line-height: 1.85;
  font-size: 14px;
  color: var(--color-text-primary);
  border-left: 3px solid var(--color-primary);
}
.comment-box p { margin: 0; }
.suggestion-box {
  border-left-color: #e6a23c;
  background: rgba(230,162,60,0.08);
}

:deep(.detail-desc .el-descriptions__label) {
  width: 110px;
  font-weight: 600;
  color: var(--color-text-regular);
}
</style>
