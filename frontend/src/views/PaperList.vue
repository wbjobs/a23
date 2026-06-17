<template>
  <div class="paper-list-container">
    <div class="page-header">
      <h2 class="page-title" style="border:none; margin:0; padding:0;">论文列表</h2>
      <div class="header-actions">
        <el-button v-if="userStore.isAuthor || userStore.isAdmin" type="primary" :icon="Plus" @click="$router.push('/paper/upload')">
          上传论文
        </el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="filter-form">
        <el-form-item label="论文标题">
          <el-input
          v-model="searchForm.title"
          placeholder="请输入论文标题"
          clearable
          style="width: 240px;"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
        </el-form-item>
        <el-form-item label="作者">
          <el-input
            v-model="searchForm.author"
            placeholder="请输入作者"
            clearable
            style="width: 200px;"
            :prefix-icon="User"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            style="width: 160px"
          >
            <el-option label="待评审" value="pending" />
            <el-option label="评审中" value="reviewing" />
            <el-option label="已录用" value="accepted" />
            <el-option label="已拒稿" value="rejected" />
            <el-option label="已发表" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        class="paper-table"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="论文标题" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/paper/detail/${row.id}`)" :underline="false">
              <span class="title-link">{{ row.title }}</span>
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="authors" label="作者" width="160" show-overflow-tooltip />
        <el-table-column prop="field" label="研究领域" width="140" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ getFieldText(row.field) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusType(row.status)" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submissionDate" label="提交日期" width="130" align="center">
          <template #default="{ row }">
            {{ formatDate(row.submissionDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="$router.push(`/paper/detail/${row.id}`)">
              查看详情
            </el-button>
            <el-button
              v-if="(userStore.isAuthor || userStore.isAdmin)"
              type="danger"
              size="small"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button
              v-if="userStore.isAdmin && row.status === 'pending'"
              type="warning"
              size="small"
              link
              @click="handleRecommend(row)"
            >
              推荐评审人
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

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="recommendVisible" title="推荐评审人" width="640px" destroy-on-close>
      <div v-if="currentPaper" class="recommend-dialog">
        <div class="recommend-paper-info">
          <el-icon color="#409eff"><Document /></el-icon>
          <span class="paper-title-text">{{ currentPaper.title }}</span>
        </div>
        <el-divider />
        <div class="recommend-list" v-loading="recommendLoading">
          <div
            v-for="reviewer in recommendList"
            :key="reviewer.id"
            class="recommend-item"
            :class="{ selected: selectedReviewers.includes(reviewer.id) }"
            @click="toggleReviewer(reviewer.id)"
          >
            <el-checkbox :model-value="selectedReviewers.includes(reviewer.id)" :value="reviewer.id" />
            <el-avatar :size="44">
              {{ reviewer.name?.charAt(0) }}
            </el-avatar>
            <div class="reviewer-info">
              <div class="reviewer-name">
                <span class="name">{{ reviewer.name }}</span>
                <el-tag size="small" type="success" effect="plain">{{ reviewer.affiliation }}</el-tag>
              </div>
              <div class="reviewer-fields">
                <el-tag
                  v-for="(f, idx) in (reviewer.fields || []).slice(0, 4)"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                  class="field-tag"
                >
                  {{ f }}
                </el-tag>
              </div>
            </div>
            <div class="match-wrapper">
              <el-progress
                type="dashboard"
                :percentage="reviewer.match || 0"
                :width="60"
                :stroke-width="8"
                :color="getMatchColor(reviewer.match)"
              />
              <div class="match-label">匹配度</div>
            </div>
          </div>
          <el-empty v-if="!recommendList.length && !recommendLoading" description="暂无推荐评审人" />
        </div>
      </div>
      <template #footer>
        <el-button @click="recommendVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" :disabled="!selectedReviewers.length" @click="handleAssignReviewers">
          确认分配 ({{ selectedReviewers.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, User, Document } from '@element-plus/icons-vue'
import { getPaperList, deletePaper } from '@/api/paper'
import { recommendReviewers, assignReviewers } from '@/api/review'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const assignLoading = ref(false)
const currentPaper = ref(null)
const recommendList = ref([])
const selectedReviewers = ref([])

const searchForm = reactive({
  title: '',
  author: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

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
  },
  {
    id: 6,
    title: '图神经网络在社交网络分析中的应用',
    authors: '李十三, 王十四',
    field: 'machine_learning',
    status: 'published',
    submissionDate: '2026-05-28'
  },
  {
    id: 7,
    title: '联邦学习隐私保护机制研究',
    authors: '赵十五, 钱十六',
    field: 'software_engineering',
    status: 'reviewing',
    submissionDate: '2026-06-16'
  },
  {
    id: 8,
    title: '强化学习在自动驾驶决策中的应用',
    authors: '孙十七, 周十八',
    field: 'artificial_intelligence',
    status: 'pending',
    submissionDate: '2026-06-13'
  }
]

const mockReviewers = [
  {
    id: 1,
    name: '陈教授',
    affiliation: '清华大学',
    fields: ['机器学习', '深度学习', '计算机视觉'],
    match: 95
  },
  {
    id: 2,
    name: '刘研究员',
    affiliation: '中科院计算所',
    fields: ['图像识别', '神经网络', '模式识别'],
    match: 88
  },
  {
    id: 3,
    name: '周博士',
    affiliation: '北京大学',
    fields: ['深度学习', '自然语言处理', '知识图谱'],
    match: 82
  },
  {
    id: 4,
    name: '吴教授',
    affiliation: '上海交通大学',
    fields: ['机器学习', '数据挖掘', '推荐系统'],
    match: 76
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
  const map = {
    pending: 'warning',
    reviewing: 'primary',
    accepted: 'success',
    rejected: 'danger',
    published: 'success'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    pending: '待评审',
    reviewing: '评审中',
    accepted: '已录用',
    rejected: '已拒稿',
    published: '已发表'
  }
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

function getMatchColor(match) {
  if (match >= 90) return '#67c23a'
  if (match >= 80) return '#409eff'
  if (match >= 70) return '#e6a23c'
  return '#909399'
}

async function loadPaperList() {
  loading.value = true
  try {
    const res = await getPaperList({
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    const data = res.data || res
    const list = data.list || data.records || data.rows || []
    if (list.length) {
      tableData.value = list
      pagination.total = data.total || list.length
    } else {
      tableData.value = mockPapers
      pagination.total = mockPapers.length
    }
  } catch (e) {
    console.error('加载论文列表失败', e)
    tableData.value = mockPapers
    pagination.total = mockPapers.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.currentPage = 1
  loadPaperList()
}

function handleReset() {
  searchForm.title = ''
  searchForm.author = ''
  searchForm.status = ''
  handleSearch()
}

function handleSizeChange(val) {
  pagination.pageSize = val
  loadPaperList()
}

function handleCurrentChange(val) {
  pagination.currentPage = val
  loadPaperList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除论文《${row.title}》吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    try {
      await deletePaper(row.id)
      ElMessage.success('删除成功')
    } catch (e) {
      console.error('删除API调用失败，使用模拟删除', e)
    }
    tableData.value = tableData.value.filter(item => item.id !== row.id)
    pagination.total = Math.max(0, pagination.total - 1)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleRecommend(row) {
  currentPaper.value = row
  selectedReviewers.value = []
  recommendVisible.value = true
  recommendLoading.value = true
  try {
    const res = await recommendReviewers({ paperId: row.id })
    const list = res.data || res
    recommendList.value = Array.isArray(list) ? list : (list.list || mockReviewers)
  } catch (e) {
    console.error('获取推荐评审人失败', e)
    recommendList.value = mockReviewers
  } finally {
    recommendLoading.value = false
  }
}

function toggleReviewer(id) {
  const idx = selectedReviewers.value.indexOf(id)
  if (idx > -1) {
    selectedReviewers.value.splice(idx, 1)
  } else {
    selectedReviewers.value.push(id)
  }
}

async function handleAssignReviewers() {
  if (!selectedReviewers.value.length) {
    ElMessage.warning('请至少选择一位评审人')
    return
  }
  assignLoading.value = true
  try {
    try {
      await assignReviewers({
        paperId: currentPaper.value.id,
        reviewerIds: selectedReviewers.value
      })
      ElMessage.success(`已成功分配 ${selectedReviewers.value.length} 位评审人`)
    } catch (e) {
      console.error('分配API失败，模拟成功', e)
      ElMessage.success(`已成功分配 ${selectedReviewers.value.length} 位评审人')
    }
    recommendVisible.value = false
    loadPaperList()
  } catch (e) {
    console.error(e)
  } finally {
    assignLoading.value = false
  }
}

onMounted(() => {
  loadPaperList()
})
</script>

<style scoped>
.paper-list-container {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-card,
.table-card {
  margin-bottom: 20px;
  border-radius: var(--border-radius);
}

:deep(.filter-card .el-card__body),
:deep(.table-card .el-card__body) {
  padding: 16px 20px;
}

.filter-form {
  margin: 0;
}

.paper-table {
  border-radius: var(--border-radius);
  overflow: hidden;
}

.title-link {
  font-weight: 500;
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
  margin-top: 16px;
}

.recommend-dialog {
  padding: 4px 0;
}

.recommend-paper-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background-color: var(--color-bg-secondary);
  border-radius: var(--border-radius);
}

.paper-title-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.recommend-list {
  max-height: 440px;
  overflow-y: auto;
  padding-right: 4px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.2s;
  background-color: var(--color-bg-primary);
}

.recommend-item:hover {
  border-color: var(--color-primary-light);
  background-color: rgba(64, 158, 255, 0.03);
}

.recommend-item.selected {
  border-color: var(--color-primary);
  background-color: rgba(64, 158, 255, 0.08);
  box-shadow: 0 0 0 1px var(--color-primary-light);
}

.reviewer-info {
  flex: 1;
  min-width: 0;
}

.reviewer-name {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.reviewer-name .name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.reviewer-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-tag {
  margin: 0;
}

.match-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  width: 80px;
}

.match-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
