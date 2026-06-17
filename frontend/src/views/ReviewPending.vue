<template>
  <div class="review-pending-container">
    <h2 class="page-title">待评审论文</h2>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="filter-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入论文标题" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="研究领域">
          <el-select v-model="searchForm.field" placeholder="全部领域" clearable style="width: 180px">
            <el-option label="计算机科学" value="computer_science" />
            <el-option label="人工智能" value="artificial_intelligence" />
            <el-option label="机器学习" value="machine_learning" />
            <el-option label="数据科学" value="data_science" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      :data="tableData"
      v-loading="loading"
      stripe
      border
      style="width: 100%"
      class="pending-table"
    >
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="title" label="论文标题" min-width="280" show-overflow-tooltip />
      <el-table-column prop="authors" label="作者" width="160" show-overflow-tooltip />
      <el-table-column prop="field" label="研究领域" width="140">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ getFieldText(row.field) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submissionDate" label="提交日期" width="140">
        <template #default="{ row }">
          {{ formatDate(row.submissionDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止日期" width="140">
        <template #default="{ row }">
          <span :class="{ 'deadline-soon': isDeadlineSoon(row.deadline) }">
            {{ formatDate(row.deadline) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" :icon="EditPen" @click="goReview(row)">
            开始评审
          </el-button>
          <el-button size="small" :icon="View" link type="primary" @click="$router.push(`/paper/detail/${row.id}`)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh, EditPen, View } from '@element-plus/icons-vue'
import { getReviewsByReviewer } from '@/api/review'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  title: '',
  field: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

async function loadData() {
  loading.value = true
  try {
    const res = await getReviewsByReviewer()
    const data = res.data || res
    tableData.value = data.list || data.records || data.rows || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载待评审论文失败', e)
    tableData.value = mockData
    pagination.total = mockData.length
  } finally {
    loading.value = false
  }
}

const mockData = [
  {
    id: 1,
    title: '基于深度学习的图像识别研究与应用',
    authors: '张三, 李四',
    field: 'machine_learning',
    submissionDate: '2026-06-10',
    deadline: '2026-06-20'
  },
  {
    id: 2,
    title: '分布式系统一致性算法优化研究',
    authors: '王五, 赵六',
    field: 'computer_science',
    submissionDate: '2026-06-11',
    deadline: '2026-06-21'
  },
  {
    id: 3,
    title: '自然语言处理中的注意力机制研究',
    authors: '孙七, 周八',
    field: 'artificial_intelligence',
    submissionDate: '2026-06-12',
    deadline: '2026-06-22'
  },
  {
    id: 4,
    title: '基于知识图谱的构建及应用研究',
    authors: '吴九, 郑十',
    field: 'data_science',
    submissionDate: '2026-06-13',
    deadline: '2026-06-18'
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
  return map[field] || field
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch (e) {
    return dateStr
  }
}

function isDeadlineSoon(deadline) {
  if (!deadline) return false
  const now = new Date()
  const d = new Date(deadline)
  const diff = d.getTime() - now.getTime()
  return diff > 0 && diff < 3 * 24 * 60 * 60 * 1000
}

function goReview(row) {
  router.push(`/review/review/${row.id}`)
}

function handleSearch() {
  pagination.currentPage = 1
  loadData()
}

function handleReset() {
  searchForm.title = ''
  searchForm.field = ''
  handleSearch()
}

function handleSizeChange(val) {
  pagination.pageSize = val
  loadData()
}

function handleCurrentChange(val) {
  pagination.currentPage = val
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.review-pending-container {
  width: 100%;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: var(--border-radius);
}

:deep(.filter-card .el-card__body) {
  padding: 16px 20px;
}

.filter-form {
  margin: 0;
}

.pending-table {
  border-radius: var(--border-radius);
  overflow: hidden;
}

.deadline-soon {
  color: var(--color-danger);
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
  margin-top: 20px;
  background-color: var(--color-bg-primary);
  border-radius: var(--border-radius);
}
</style>
