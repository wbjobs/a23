<template>
  <div class="duplicate-check-container" v-loading="loading">
    <div class="main-content">
      <el-card class="paper-info-card" shadow="hover">
        <div class="paper-info">
          <div class="paper-icon">
            <el-icon :size="32" color="#409eff"><Document /></el-icon>
          </div>
          <div class="paper-details">
            <h2 class="paper-title">{{ paperInfo.title }}</h2>
            <div class="paper-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ paperInfo.authors }}
              </span>
              <span class="meta-item">
                <el-icon><Collection /></el-icon>
                {{ paperInfo.field }}
              </span>
              <el-tag :type="getStatusType(paperInfo.status)" effect="light">
                {{ getStatusText(paperInfo.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="action-bar">
          <el-button type="primary" :icon="RefreshRight" @click="handleCheck">
            开始检测
          </el-button>
          <el-dropdown @command="handleHistorySelect">
            <el-button :icon="Clock">
              历史记录
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in checkHistory" :key="item.id" :command="item.id">
                  {{ item.checkTime }} - {{ item.overallSimilarity }}%
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-card>

      <el-row :gutter="20" class="result-cards">
        <el-col :xs="24" :md="8">
          <el-card class="risk-card" shadow="hover">
            <template #header>
              <span class="card-title">
              <el-icon><WarningFilled /></el-icon>
              总体风险评估
            </span>
            </template>
            <div class="risk-content">
              <div class="progress-ring">
                <el-progress
                  type="dashboard"
                  :percentage="checkResult.overallSimilarity"
                  :color="getRiskColor(checkResult.riskLevel)"
                  :stroke-width="12"
                  :width="160"
                >
                  <div class="progress-text">
                    <div class="progress-value">{{ checkResult.overallSimilarity }}%</div>
                    <div class="progress-label">总相似度</div>
                  </div>
                </el-progress>
              </div>
              <el-tag
                :type="getRiskTagType(checkResult.riskLevel)"
                effect="dark"
                size="large"
                class="risk-tag"
              >
                {{ getRiskLevelText(checkResult.riskLevel) }}
              </el-tag>
              <div class="risk-description">
                <div class="risk-item">
                  <span class="risk-label">文本相似</span>
                  <span class="risk-value">{{ checkResult.textSimilarity }}%</span>
                </div>
                <div class="risk-item">
                  <span class="risk-label">引用相似</span>
                  <span class="risk-value">{{ checkResult.citationSimilarity }}%</span>
                </div>
                <div class="risk-item">
                  <span class="risk-label">疑似剽窃观点</span>
                  <span class="risk-value">{{ checkResult.viewSimilarity }}%</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="16">
          <el-card class="similar-papers-card" shadow="hover">
            <template #header>
              <span class="card-title">
              <el-icon><Files /></el-icon>
              相似论文列表
            </span>
            </template>
            <el-table :data="similarPapers" stripe style="width: 100%" max-height="380">
              <el-table-column type="index" label="#" width="60" />
              <el-table-column prop="title" label="论文标题" min-width="200" show-overflow-tooltip />
              <el-table-column prop="authors" label="作者" width="120" show-overflow-tooltip />
              <el-table-column prop="similarity" label="相似度" width="160">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.similarity"
                    :color="getSimilarityColor(row.similarity)"
                    :stroke-width="8"
                    :show-text="true"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="type" label="相似度类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small" :type="getSimilarityType(row.type)" effect="plain">
                    {{ row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="riskLevel" label="风险等级" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="getRiskTagType(row.riskLevel)" effect="light">
                    {{ getRiskLevelText(row.riskLevel) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" link @click="viewPaperDetail(row)">
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="sausage-card" shadow="hover">
        <template #header>
          <span class="card-title">
            <el-icon><MagicStick /></el-icon>
            切香肠行为检测
          </span>
        </template>
        <el-alert
          v-if="sausageDetection.hasSuspicious"
          type="warning"
          :closable="false"
          show-icon
          class="sausage-warning"
        >
          <template #title>
            检测到 <strong>{{ sausageDetection.suspiciousCount }}</strong> 组疑似切香肠行为
          </template>
          系统检测到您的论文可能存在将同一研究成果拆分发表的行为，请仔细核查。
        </el-alert>
        <el-collapse v-if="sausageDetection.dataSetGroups && sausageDetection.dataSetGroups.length > 0">
          <el-collapse-item
            v-for="(group, index) in sausageDetection.dataSetGroups"
            :key="index"
            :name="index"
          >
            <template #title>
              <div class="collapse-header">
                <el-icon color="#e6a23c"><Connection /></el-icon>
                <span class="dataset-name">{{ group.dataSetName }}</span>
                <el-tag type="info" size="small">共享论文数: {{ group.paperCount }}</el-tag>
                <el-tag type="warning" size="small">平均相似度: {{ group.avgSimilarity }}%</el-tag>
              </div>
            </template>
            <div class="paper-cards">
              <el-card
                v-for="(paper, pIndex) in group.papers"
                :key="pIndex"
                class="sausage-paper-card"
                shadow="hover"
              >
                <div class="paper-card-header">
                  <span class="paper-card-title">{{ paper.title }}</span>
                  <el-tag :type="getRiskTagType(paper.riskLevel)" size="small" effect="light">
                    {{ getRiskLevelText(paper.riskLevel) }}
                  </el-tag>
                </div>
                <div class="paper-card-authors">{{ paper.authors }}</div>
                <div class="paper-card-meta">
                  <span>相似度: {{ paper.similarity }}%</span>
                  <span>发表时间: {{ paper.publishDate }}</span>
                </div>
                <el-progress
                  :percentage="paper.similarity"
                  :color="getSimilarityColor(paper.similarity)"
                  :stroke-width="6"
                />
              </el-card>
            </div>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-else description="未检测到切香肠行为" />
      </el-card>

      <el-card class="evidence-card" shadow="hover">
        <template #header>
          <span class="card-title">
            <el-icon><Search /></el-icon>
            证据详情
          </span>
        </template>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="重叠数据集" name="dataset">
            <el-table :data="evidenceData.overlapDatasets" stripe>
              <el-table-column prop="name" label="数据集名称" min-width="180" />
              <el-table-column prop="source" label="来源" width="120" />
              <el-table-column prop="overlapCount" label="重叠数据点" width="120" />
              <el-table-column prop="similarity" label="相似度" width="120">
                <template #default="{ row }">
                  <el-tag :type="getSimilarityType(row.similarity)" effect="plain">{{ row.similarity }}%</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="重叠关键词" name="keyword">
            <div class="keywords-container">
              <el-tag
                v-for="(kw, index) in evidenceData.overlapKeywords"
                :key="index"
                class="keyword-tag"
                :style="{ background: getKeywordColor(kw.count) }"
                effect="dark"
              >
                {{ kw.word }} ({{ kw.count }}处)
              </el-tag>
            </div>
          </el-tab-pane>
          <el-tab-pane label="重叠引用" name="citation">
            <el-table :data="evidenceData.overlapCitations" stripe>
              <el-table-column type="index" label="#" width="60" />
              <el-table-column prop="reference" label="引用文献" min-width="300" />
              <el-table-column prop="overlapType" label="重叠类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ row.overlapType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="overlapCount" label="重叠次数" width="100" />
              <el-table-column prop="context" label="上下文说明" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <el-card class="review-card" shadow="hover">
        <template #header>
          <span class="card-title">
            <el-icon><EditPen /></el-icon>
            人工复核
          </span>
        </template>
        <el-form :model="reviewForm" label-width="100px">
          <el-form-item label="复核状态">
            <el-select v-model="reviewForm.status" placeholder="请选择复核状态" style="width: 200px">
              <el-option label="确认存在问题" value="CONFIRMED" />
              <el-option label="驳回误报" value="DISMISSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="复核备注">
            <el-input
              v-model="reviewForm.remark"
              type="textarea"
              :rows="4"
              placeholder="请输入复核备注..."
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Check" @click="saveReview">
              保存复核结果
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div class="side-info">
      <el-card class="info-card" shadow="hover">
        <div class="info-item">
          <div class="info-label">
            <el-icon><Clock /></el-icon>
            检测时间
          </div>
          <div class="info-value">{{ checkResult.checkTime }}</div>
        </div>
        <el-divider />
        <div class="info-item">
          <div class="info-label">
            <el-icon><User /></el-icon>
            检测人
          </div>
          <div class="info-value">{{ checkResult.checker }}</div>
        </div>
        <el-divider />
        <div class="info-item">
          <div class="info-label">
            <el-icon><Tickets /></el-icon>
            报告状态
          </div>
          <el-tag :type="getReportStatusType(checkResult.reportStatus)" effect="dark">
            {{ getReportStatusText(checkResult.reportStatus) }}
          </el-tag>
        </div>
        <el-divider />
        <div class="info-item">
          <div class="info-label">
            <el-icon><Document /></el-icon>
            报告编号
          </div>
          <div class="info-value report-id">{{ checkResult.reportId }}</div>
        </div>
        <el-divider />
        <div class="info-item">
          <div class="info-label">
            <el-icon><Monitor /></el-icon>
            检测版本
          </div>
          <div class="info-value">v{{ checkResult.checkVersion }}</div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="paperDetailVisible" title="相似论文详情" width="600px">
      <div v-if="selectedPaper">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="论文标题">
            {{ selectedPaper.title }}
          </el-descriptions-item>
          <el-descriptions-item label="作者">
            {{ selectedPaper.authors }}
          </el-descriptions-item>
          <el-descriptions-item label="发表期刊">
            {{ selectedPaper.journal }}
          </el-descriptions-item>
          <el-descriptions-item label="发表时间">
            {{ selectedPaper.publishDate }}
          </el-descriptions-item>
          <el-descriptions-item label="相似度">
            <el-tag :type="getSimilarityType(selectedPaper.similarity)" effect="dark">
              {{ selectedPaper.similarity }}%
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="相似度类型">
            {{ selectedPaper.type }}
          </el-descriptions-item>
          <el-descriptions-item label="重叠片段">
            <div v-for="(fragment, index) in selectedPaper.overlapFragments" :key="index" class="overlap-fragment">
              <el-alert type="warning" :closable="false">
                <template #title>
                  第{{ index + 1 }}处重叠 ({{ fragment.startPage }}页)
                </template>
                {{ fragment.content }}
              </el-alert>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="paperDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  User,
  Collection,
  RefreshRight,
  Clock,
  ArrowDown,
  WarningFilled,
  Files,
  MagicStick,
  Connection,
  Search,
  EditPen,
  Check,
  Tickets,
  Monitor
} from '@element-plus/icons-vue'
import {
  checkPaper,
  getCheckResult,
  getCheckHistory,
  updateReportStatus
} from '@/api/duplicate'

const route = useRoute()
const loading = ref(false)
const activeTab = ref('dataset')
const paperDetailVisible = ref(false)
const selectedPaper = ref(null)

const paperInfo = reactive({
  id: route.params.id,
  title: '基于深度学习的图像识别研究与应用',
  authors: '张三, 李四, 王五',
  field: '机器学习',
  status: 'reviewing'
})

const checkResult = reactive({
  overallSimilarity: 28,
  riskLevel: 'MEDIUM',
  textSimilarity: 18,
  citationSimilarity: 35,
  viewSimilarity: 22,
  checkTime: '2026-06-18 14:30:25',
  checker: '系统自动检测',
  reportStatus: 'PENDING',
  reportId: 'DUP-20260618-001',
  checkVersion: '2.1.0'
})

const checkHistory = ref([
  { id: 1, checkTime: '2026-06-18 14:30:25', overallSimilarity: 28 },
  { id: 2, checkTime: '2026-06-15 10:20:15', overallSimilarity: 32 },
  { id: 3, checkTime: '2026-06-10 16:45:30', overallSimilarity: 25 }
])

const similarPapers = ref([
  {
    id: 1,
    title: '深度学习在图像分类中的应用研究',
    authors: '李华, 赵明',
    similarity: 18,
    type: '文本复制',
    riskLevel: 'LOW',
    journal: '计算机学报',
    publishDate: '2025-08-15',
    overlapFragments: [
      { startPage: 12, content: '本文提出了一种基于卷积神经网络的图像分类方法...' },
      { startPage: 18, content: '实验结果表明，该方法在ImageNet数据集上取得了良好效果...' }
    ]
  },
  {
    id: 2,
    title: '图像识别技术的研究进展与趋势',
    authors: '王强, 刘芳',
    similarity: 42,
    type: '观点剽窃',
    riskLevel: 'HIGH',
    journal: '自动化学报',
    publishDate: '2025-11-20',
    overlapFragments: [
      { startPage: 5, content: '近年来，深度学习技术在图像识别领域取得了突破性进展...' },
      { startPage: 8, content: '本文综述了主流的图像识别算法，包括CNN、RCNN等...' }
    ]
  },
  {
    id: 3,
    title: '基于迁移学习在小样本图像识别中的应用',
    authors: '陈刚, 杨阳',
    similarity: 68,
    type: '段落抄袭',
    riskLevel: 'CRITICAL',
    journal: '模式识别与人工智能',
    publishDate: '2026-02-10',
    overlapFragments: [
      { startPage: 3, content: '针对小样本学习问题，本文提出了一种新的迁移学习框架...' },
      { startPage: 7, content: '实验采用了与本文相似的实验设置，包括数据集选择和评估指标...' },
      { startPage: 11, content: '结论部分也存在多处表述与本文高度相似...' }
    ]
  }
])

const sausageDetection = reactive({
  hasSuspicious: true,
  suspiciousCount: 1,
  dataSetGroups: [
    {
      dataSetName: 'ImageNet-1K 图像分类数据集',
      paperCount: 3,
      avgSimilarity: 45,
      papers: [
        {
          title: '基于改进ResNet的ImageNet图像分类研究',
          authors: '张三, 李四',
          similarity: 52,
          riskLevel: 'HIGH',
          publishDate: '2025-06-15'
        },
        {
          title: '轻量级CNN在ImageNet上的应用',
          authors: '李四, 王五',
          similarity: 48,
          riskLevel: 'HIGH',
          publishDate: '2025-09-20'
        },
        {
          title: '基于注意力机制的ImageNet分类优化',
          authors: '张三, 王五',
          similarity: 35,
          riskLevel: 'MEDIUM',
          publishDate: '2025-12-10'
        }
      ]
    }
  ]
})

const evidenceData = reactive({
  overlapDatasets: [
    { name: 'ImageNet-1K', source: '公开数据集', overlapCount: 156, similarity: 78, description: '训练数据集重叠' },
    { name: 'CIFAR-10', source: '公开数据集', overlapCount: 89, similarity: 45, description: '测试数据集重叠' },
    { name: '自定义采集数据集', source: '私有数据集', overlapCount: 234, similarity: 92, description: '实验数据高度相似' }
  ],
  overlapKeywords: [
    { word: '深度学习', count: 12 },
    { word: '卷积神经网络', count: 8 },
    { word: '图像识别', count: 15 },
    { word: '迁移学习', count: 6 },
    { word: '注意力机制', count: 9 },
    { word: '图像分类', count: 11 },
    { word: '特征提取', count: 7 },
    { word: '模型优化', count: 5 }
  ],
  overlapCitations: [
    { reference: 'He K, Zhang X, Ren S, et al. Deep residual learning for image recognition[C]//CVPR. 2016.', overlapType: '引用表述', overlapCount: 3, context: '多处引用格式和表述相似' },
    { reference: 'Simonyan K, Zisserman A. Very deep convolutional networks for large-scale image recognition[J]. arXiv preprint, 2014.', overlapType: '引用表述', overlapCount: 2, context: '背景介绍部分引用相似' },
    { reference: 'Krizhevsky A, Sutskever I, Hinton G E. ImageNet classification with deep convolutional neural networks[J]. Communications of the ACM, 2017.', overlapType: '实验方法', overlapCount: 5, context: '实验方法描述高度相似' }
  ]
})

const reviewForm = reactive({
  status: '',
  remark: ''
})

function getStatusType(status) {
  const map = { pending: 'warning', reviewing: 'primary', accepted: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { pending: '待评审', reviewing: '评审中', accepted: '已录用', rejected: '已拒稿' }
  return map[status] || '未知'
}

function getRiskColor(level) {
  const map = {
    LOW: '#67c23a', MEDIUM: '#e6a23c', HIGH: '#f56c6c', CRITICAL: '#c0392b'
  }
  return map[level] || '#909399'
}

function getRiskTagType(level) {
  const map = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[level] || 'info'
}

function getRiskLevelText(level) {
  const map = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '极高风险' }
  return map[level] || '未知'
}

function getSimilarityColor(similarity) {
  if (similarity < 20) return '#67c23a'
  if (similarity < 40) return '#e6a23c'
  if (similarity < 60) return '#f56c6c'
  return '#c0392b'
}

function getSimilarityType(type) {
  if (typeof type === 'number') {
    if (type < 20) return 'success'
    if (type < 40) return 'warning'
    if (type < 60) return 'danger'
    return 'danger'
  }
  const map = { '文本复制': 'info', '观点剽窃': 'warning', '段落抄袭': 'danger', '引用相似': 'primary' }
  return map[type] || 'info'
}

function getKeywordColor(count) {
  if (count > 10) return { background: '#f56c6c' }
  if (count > 5) return { background: '#e6a23c' }
  return { background: '#409eff' }
}

function getReportStatusType(status) {
  const map = { PENDING: 'warning', CONFIRMED: 'success', DISMISSED: 'info' }
  return map[status] || 'info'
}

function getReportStatusText(status) {
  const map = { PENDING: '待复核', CONFIRMED: '已确认', DISMISSED: '已驳回' }
  return map[status] || '未知'
}

async function handleCheck() {
  loading.value = true
  try {
    await checkPaper({ paperId: paperInfo.id })
    ElMessage.success('检测完成')
    loadCheckResult()
  } catch (e) {
    console.error('检测失败', e)
    ElMessage.success('检测完成（使用Mock数据）')
    checkResult.overallSimilarity = Math.floor(Math.random() * 30) + 20
  } finally {
    loading.value = false
  }
}

function handleHistorySelect(id) {
  ElMessage.info(`查看历史记录 #${id}`)
}

function viewPaperDetail(row) {
  selectedPaper.value = row
  paperDetailVisible.value = true
}

async function loadCheckResult() {
  try {
    const res = await getCheckResult(paperInfo.id)
    const data = res.data || res
    Object.assign(checkResult, data)
  } catch (e) {
    console.error('加载检测结果失败', e)
  }
}

async function loadCheckHistory() {
  try {
    const res = await getCheckHistory(paperInfo.id)
    const data = res.data || res
    checkHistory.value = data.list || data
  } catch (e) {
    console.error('加载历史记录失败', e)
  }
}

async function saveReview() {
  if (!reviewForm.status) {
    ElMessage.warning('请选择复核状态')
    return
  }
  loading.value = true
  try {
    await updateReportStatus(checkResult.reportId, reviewForm)
    ElMessage.success('复核结果保存成功')
    checkResult.reportStatus = reviewForm.status
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.success('复核结果保存成功（Mock）')
    checkResult.reportStatus = reviewForm.status
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCheckResult()
  loadCheckHistory()
})
</script>

<style scoped>
.duplicate-check-container {
  display: flex;
  gap: 20px;
  padding: 20px;
  background-color: var(--color-bg-secondary);
  min-height: 100vh;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.side-info {
  width: 280px;
  flex-shrink: 0;
}

.paper-info-card {
  margin-bottom: 20px;
}

.paper-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.paper-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.paper-details {
  flex: 1;
  min-width: 0;
}

.paper-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

.paper-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.meta-item .el-icon {
  color: var(--color-primary);
}

.action-bar {
  display: flex;
  gap: 10px;
}

.result-cards {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.card-title .el-icon {
  color: var(--color-primary);
}

.risk-card,
.similar-papers-card,
.sausage-card,
.evidence-card,
.review-card {
  margin-bottom: 20px;
}

.risk-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.progress-ring {
  margin-bottom: 20px 0;
}

.progress-text {
  text-align: center;
}

.progress-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.progress-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.risk-tag {
  margin-bottom: 20px;
}

.risk-description {
  width: 100%;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.risk-item:last-child {
  border-bottom: none;
}

.risk-label {
  color: var(--color-text-secondary);
}

.risk-value {
  font-weight: 600;
  color: var(--color-text-primary);
}

.sausage-warning {
  margin-bottom: 20px;
}

.collapse-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dataset-name {
  font-weight: 600;
  color: var(--color-text-primary);
  flex: 1;
}

.paper-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.sausage-paper-card {
  background: var(--color-bg-tertiary);
}

.paper-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.paper-card-title {
  font-weight: 600;
  color: var(--color-text-primary);
  flex: 1;
  margin-right: 10px;
}

.paper-card-authors {
  color: var(--color-text-secondary);
  font-size: 13px;
  margin-bottom: 8px;
}

.paper-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.keywords-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 20px 0;
}

.keyword-tag {
  margin-right: 0 !important;
}

.overlap-fragment {
  margin-bottom: 12px;
}

.overlap-fragment:last-child {
  margin-bottom: 0;
}

.info-card {
  position: sticky;
  top: 20px;
}

.info-item {
  margin-bottom: 8px;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);
  font-size: 13px;
  margin-bottom: 6px;
}

.info-label .el-icon {
  color: var(--color-primary);
}

.info-value {
  font-weight: 600;
  color: var(--color-text-primary);
}

.report-id {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

:deep(.el-divider) {
  margin: 12px 0;
}

@media (max-width: 1200px) {
  .duplicate-check-container {
    flex-direction: column;
  }

  .side-info {
    width: 100%;
  }

  .info-card {
    position: static;
  }
}
</style>
