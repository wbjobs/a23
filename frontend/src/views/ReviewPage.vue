<template>
  <div class="review-page-container" v-loading="loading">
    <el-page-header @back="$router.back()" class="page-header">
      <template #content>
        <div class="header-content">
          <span class="header-title">论文评审</span>
          <el-tag v-if="paper" :type="getStatusType(paper.status)" effect="light" size="small" class="status-tag">
            {{ getStatusText(paper.status) }}
          </el-tag>
        </div>
      </template>
    </el-page-header>

    <div v-if="paper" class="review-main">
      <el-row :gutter="16">
        <el-col :span="14">
          <el-card class="paper-info-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title"><el-icon><Document /></el-icon> 论文基本信息</span>
              </div>
            </template>
            <div class="paper-info">
              <h3 class="paper-title">{{ paper.title }}</h3>
              <div class="paper-meta-row">
                <span class="meta-item"><el-icon><User /></el-icon> {{ paper.authors }}</span>
                <span class="meta-item"><el-icon><OfficeBuilding /></el-icon> {{ paper.affiliation || '示例大学' }}</span>
              </div>
              <div class="paper-meta-row">
                <span class="meta-item"><el-icon><Calendar /></el-icon> 提交：{{ formatDate(paper.submissionDate) }}</span>
                <span class="meta-item"><el-icon><Files /></el-icon> 页数：{{ paper.pages || 12 }} 页</span>
              </div>
              <div class="paper-section keywords-section" v-if="paper.keywords && paper.keywords.length">
                <div class="section-label">关键词：</div>
                <div class="section-content">
                  <el-tag v-for="(kw, idx) in paper.keywords" :key="idx" class="kw-tag" effect="plain" size="small" type="primary">
                    {{ kw }}
                  </el-tag>
                </div>
              </div>
              <div class="paper-section">
                <div class="section-label">摘要</div>
                <div class="section-content abstract-text">{{ paper.abstract }}</div>
              </div>
            </div>
          </el-card>

          <el-card class="pdf-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title"><el-icon><Reading /></el-icon> 论文全文（PDF）</span>
                <div class="pdf-tools">
                  <el-button-group>
                    <el-button size="small" :icon="ZoomIn" @click="zoomIn">放大</el-button>
                    <el-button size="small" :icon="ZoomOut" @click="zoomOut">缩小</el-button>
                    <el-button size="small" :icon="RefreshRight" @click="resetZoom">重置</el-button>
                  </el-button-group>
                  <el-button type="primary" size="small" :icon="Download" plain @click="handleDownload">
                    下载PDF
                  </el-button>
                </div>
              </div>
            </template>
            <div class="pdf-wrapper">
              <iframe
                v-if="pdfUrl"
                :src="pdfUrl + '#toolbar=1&navpanes=1&view=FitH'"
                class="pdf-iframe"
                :style="{ transform: `scale(${pdfScale})`, transformOrigin: 'top center' }"
              />
              <div v-else class="pdf-fallback">
                <div class="fallback-paper">
                  <div class="fb-header">
                    <h2 class="fb-title">{{ paper.title }}</h2>
                    <div class="fb-authors">{{ paper.authors }}</div>
                    <div class="fb-affiliation">{{ paper.affiliation || '示例大学计算机学院' }}</div>
                  </div>
                  <div class="fb-section">
                    <div class="fb-sec-title">Abstract</div>
                    <div class="fb-sec-content">{{ paper.abstract }}</div>
                  </div>
                  <div class="fb-section">
                    <div class="fb-sec-title">Keywords</div>
                    <div class="fb-sec-content">{{ (paper.keywords || []).join(', ') }}</div>
                  </div>
                  <div class="fb-section">
                    <div class="fb-sec-title">1. Introduction</div>
                    <div class="fb-sec-content">
                      <p>With the rapid development of artificial intelligence and machine learning, research in related fields has made remarkable progress. This paper focuses on the problem of {{ (paper.keywords || ['deep learning'])[0] }} and proposes a novel method to address the existing challenges.</p>
                      <p>Our contributions are summarized as follows: (1) We propose a new framework that effectively combines multiple techniques. (2) Extensive experiments on benchmark datasets demonstrate the superiority of our method. (3) We provide thorough analysis and ablation studies.</p>
                    </div>
                  </div>
                  <div class="fb-section">
                    <div class="fb-sec-title">2. Related Work</div>
                    <div class="fb-sec-content">
                      <p>In recent years, numerous methods have been proposed to tackle similar problems. Early work focused on traditional statistical approaches. With the rise of deep learning, neural network-based methods have become dominant. Several representative studies are discussed below.</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card class="side-card figures-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title"><el-icon><Picture /></el-icon> 图表列表（点击放大）</span>
                <el-tag type="info" size="small">{{ figures.length }} 张</el-tag>
              </div>
            </template>
            <div v-viewer class="figures-grid">
              <div v-for="(fig, idx) in figures" :key="idx" class="figure-item" @click.stop>
                <img :src="fig.url" :alt="fig.title" class="figure-img" />
                <div class="figure-caption">
                  <span class="fig-num">图 {{ idx + 1 }}</span>
                  <span class="fig-title">{{ fig.title }}</span>
                </div>
              </div>
            </div>
            <div v-if="!figures.length" class="empty-placeholder">
              <el-empty description="暂无图表" :image-size="60" />
            </div>
          </el-card>

          <el-card class="side-card formulas-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title"><el-icon><Operation /></el-icon> 公式列表（LaTeX）</span>
                <el-tag type="info" size="small">{{ formulas.length }} 个</el-tag>
              </div>
            </template>
            <div class="formulas-list">
              <div v-for="(fm, idx) in formulas" :key="idx" class="formula-item">
                <div class="formula-header">
                  <span class="formula-num">式 ({{ idx + 1 }})</span>
                  <el-button
                    size="small"
                    :icon="CopyDocument"
                    text
                    type="primary"
                    @click="copyFormula(fm.latex, idx)"
                  >
                    {{ copiedIdx === idx ? '已复制' : '复制' }}
                  </el-button>
                </div>
                <div class="formula-render" v-html="renderFormula(fm.latex)"></div>
                <div class="formula-source">
                  <code>{{ fm.latex }}</code>
                </div>
              </div>
            </div>
            <div v-if="!formulas.length" class="empty-placeholder">
              <el-empty description="暂无公式" :image-size="60" />
            </div>
          </el-card>

          <el-card class="review-form-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title"><el-icon><EditPen /></el-icon> 评审打分面板</span>
                <el-tag type="warning" effect="dark" size="small" v-if="!isSubmitted">请认真评审后提交</el-tag>
                <el-tag type="success" effect="dark" size="small" v-else>已提交</el-tag>
              </div>
            </template>
            <el-form
              ref="reviewFormRef"
              :model="reviewForm"
              :rules="reviewRules"
              label-width="100px"
              class="review-form"
              :disabled="isSubmitted"
            >
              <el-form-item label="创新性">
                <div class="slider-row">
                  <el-slider
                    v-model="reviewForm.innovation"
                    :min="0"
                    :max="100"
                    :step="1"
                    show-stops
                    :marks="sliderMarks"
                    class="score-slider"
                  />
                  <div class="score-badge innovation">{{ reviewForm.innovation }}</div>
                </div>
                <div class="score-hint"><span>差</span><span>一般</span><span>良好</span><span>优秀</span></div>
              </el-form-item>

              <el-form-item label="方法学">
                <div class="slider-row">
                  <el-slider
                    v-model="reviewForm.methodology"
                    :min="0"
                    :max="100"
                    :step="1"
                    show-stops
                    :marks="sliderMarks"
                    class="score-slider"
                  />
                  <div class="score-badge methodology">{{ reviewForm.methodology }}</div>
                </div>
                <div class="score-hint"><span>差</span><span>一般</span><span>良好</span><span>优秀</span></div>
              </el-form-item>

              <el-form-item label="写作质量">
                <div class="slider-row">
                  <el-slider
                    v-model="reviewForm.writing"
                    :min="0"
                    :max="100"
                    :step="1"
                    show-stops
                    :marks="sliderMarks"
                    class="score-slider"
                  />
                  <div class="score-badge writing">{{ reviewForm.writing }}</div>
                </div>
                <div class="score-hint"><span>差</span><span>一般</span><span>良好</span><span>优秀</span></div>
              </el-form-item>

              <el-form-item label="综合评分">
                <div class="overall-wrap">
                  <div class="overall-circle" :class="getOverallLevel(averageScore)">
                    <span class="overall-score">{{ averageScore }}</span>
                    <span class="overall-max">/100</span>
                  </div>
                  <div class="overall-desc">
                    <div class="overall-label">三者平均值（自动计算）</div>
                    <div class="overall-detail">
                      <el-tag size="small" effect="plain" type="danger">创新性 {{ reviewForm.innovation }}</el-tag>
                      <el-tag size="small" effect="plain" type="warning">方法学 {{ reviewForm.methodology }}</el-tag>
                      <el-tag size="small" effect="plain" type="primary">写作 {{ reviewForm.writing }}</el-tag>
                    </div>
                    <div class="overall-result">
                      评价：<b :class="getOverallLevel(averageScore) + '-text'">{{ getOverallText(averageScore) }}</b>
                    </div>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="评审结论">
                <el-radio-group v-model="reviewForm.decision">
                  <el-radio-button value="accept"><el-tag type="success" effect="dark" size="small">录用</el-tag></el-radio-button>
                  <el-radio-button value="revision"><el-tag type="warning" effect="dark" size="small">大修</el-tag></el-radio-button>
                  <el-radio-button value="minor"><el-tag type="primary" effect="dark" size="small">小修</el-tag></el-radio-button>
                  <el-radio-button value="reject"><el-tag type="danger" effect="dark" size="small">拒稿</el-tag></el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="评审意见" prop="comment">
                <el-input
                  v-model="reviewForm.comment"
                  type="textarea"
                  :rows="10"
                  placeholder="请详细填写评审意见，包括：&#10;1. 论文的主要贡献和亮点&#10;2. 存在的问题和不足&#10;3. 具体的修改建议&#10;4. 其他需要说明的问题"
                  maxlength="3000"
                  show-word-limit
                  resize="vertical"
                />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="submitting"
                  :icon="Check"
                  size="large"
                  class="submit-btn"
                  :disabled="isSubmitted"
                  @click="handleSubmit"
                >
                  {{ isSubmitted ? '评审已提交' : '提交评审' }}
                </el-button>
                <el-button
                  :icon="DocumentAdd"
                  size="large"
                  :disabled="isSubmitted"
                  @click="handleSaveDraft"
                >
                  保存草稿
                </el-button>
                <el-button
                  type="danger"
                  plain
                  :icon="Delete"
                  size="large"
                  :disabled="isSubmitted"
                  @click="handleReset"
                >
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Calendar, Document, OfficeBuilding, Files, Reading,
  ZoomIn, ZoomOut, RefreshRight, Download, Picture, Operation,
  EditPen, Check, DocumentAdd, Delete, CopyDocument
} from '@element-plus/icons-vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { getPaperById } from '@/api/paper'
import { submitReview } from '@/api/review'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const paper = ref(null)
const reviewFormRef = ref(null)
const pdfScale = ref(1)
const isSubmitted = ref(false)
const copiedIdx = ref(-1)

const paperId = computed(() => route.params.paperId)
const pdfUrl = computed(() => paper.value?.fileUrl || paper.value?.pdfUrl || '')

const sliderMarks = { 0: '0', 25: '25', 50: '50', 75: '75', 100: '100' }

const reviewForm = reactive({
  innovation: 75,
  methodology: 75,
  writing: 75,
  comment: '',
  decision: 'minor'
})

const reviewRules = {
  comment: [
    { required: true, message: '请填写评审意见', trigger: 'blur' },
    { min: 50, message: '评审意见不少于 50 字', trigger: 'blur' }
  ]
}

const averageScore = computed(() => {
  const sum = reviewForm.innovation + reviewForm.methodology + reviewForm.writing
  return Math.round(sum / 3)
})

const figurePrompts = [
  'deep learning neural network architecture diagram, scientific paper figure, blue tech style',
  'training loss and accuracy curve chart, machine learning experiment, data visualization',
  'confusion matrix heatmap visualization, classification results comparison',
  'comparison bar chart of model performance, SOTA methods comparison'
]

const figures = computed(() => {
  if (!paper.value) return []
  const count = paper.value.figures?.length || 4
  return Array.from({ length: count }, (_, idx) => ({
    title: paper.value.figures?.[idx]?.caption || ['模型架构图', '训练曲线图', '混淆矩阵', '性能对比图'][idx] || `图 ${idx + 1}`,
    url: `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${encodeURIComponent(figurePrompts[idx % figurePrompts.length])}&image_size=square`
  }))
})

const formulas = computed(() => {
  if (!paper.value) return []
  const defaultFms = [
    { latex: 'L(\\theta) = -\\frac{1}{N}\\sum_{i=1}^{N}\\sum_{c=1}^{C}y_{i,c}\\log(p_{i,c})' },
    { latex: '\\text{Attention}(Q,K,V) = \\text{softmax}\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V' },
    { latex: 'F1 = 2 \\times \\frac{\\text{Precision} \\times \\text{Recall}}{\\text{Precision} + \\text{Recall}}' },
    { latex: 'h_t = \\sigma(W_{hh}h_{t-1} + W_{xh}x_t + b_h)' },
    { latex: '\\hat{y} = \\arg\\max_{k} P(y=k|x;\\theta)' }
  ]
  return paper.value.formulas?.length ? paper.value.formulas : defaultFms
})

function renderFormula(latex) {
  try {
    return katex.renderToString(latex, { throwOnError: false, displayMode: true, strict: false })
  } catch (e) {
    return `<code>${latex}</code>`
  }
}

async function copyFormula(latex, idx) {
  try {
    await navigator.clipboard.writeText(latex)
    copiedIdx.value = idx
    ElMessage.success('公式已复制')
    setTimeout(() => copiedIdx.value = -1, 1500)
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

function zoomIn() { pdfScale.value = Math.min(pdfScale.value + 0.1, 2) }
function zoomOut() { pdfScale.value = Math.max(pdfScale.value - 0.1, 0.5) }
function resetZoom() { pdfScale.value = 1 }
function handleDownload() { ElMessage.info('PDF 下载中...') }

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try { return new Date(dateStr).toLocaleDateString('zh-CN') } catch (e) { return dateStr }
}

function getStatusText(s) {
  return { pending: '待评审', reviewing: '评审中', accepted: '已录用', rejected: '已拒绝' }[s] || s
}
function getStatusType(s) {
  return { pending: 'warning', reviewing: 'primary', accepted: 'success', rejected: 'danger' }[s] || 'info'
}

function getOverallLevel(score) {
  if (score >= 85) return 'excellent'
  if (score >= 70) return 'good'
  if (score >= 55) return 'fair'
  return 'poor'
}
function getOverallText(score) {
  return getOverallLevel(score) === 'excellent' ? '强烈推荐录用'
    : getOverallLevel(score) === 'good' ? '建议录用'
    : getOverallLevel(score) === 'fair' ? '建议修改后再审' : '建议拒稿'
}

async function loadPaper() {
  loading.value = true
  try {
    const res = await getPaperById(paperId.value)
    paper.value = res.data || res
  } catch (e) {
    console.error('加载论文失败', e)
    paper.value = {
      id: paperId.value,
      title: '基于深度学习的图像识别研究与应用',
      authors: '张三, 李四, 王五',
      affiliation: '示例大学计算机科学与技术学院',
      abstract: '本文研究了深度学习在图像识别领域的应用，提出了一种新的卷积神经网络结构。通过在 ImageNet、CIFAR-10 等多个公开数据集上的大量实验验证，本文方法在分类准确率和推理速度上均优于现有 SOTA 方法。此外，我们还通过消融实验分析了各模块的贡献，并对模型的可解释性进行了探讨。',
      keywords: ['深度学习', '图像识别', '卷积神经网络', '注意力机制', '迁移学习'],
      submissionDate: '2026-06-10',
      status: 'reviewing',
      pages: 12,
      fileUrl: ''
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!reviewFormRef.value) return
  try {
    await reviewFormRef.value.validate()
    await ElMessageBox.confirm(
      `综合评分为 ${averageScore} 分（${getOverallText(averageScore)}）。\n提交后将无法修改，确认提交评审吗？`,
      '确认提交评审',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再想想' }
    )
    submitting.value = true
    await submitReview({
      paperId: paperId.value,
      innovation: reviewForm.innovation,
      methodology: reviewForm.methodology,
      writing: reviewForm.writing,
      score: averageScore.value,
      comment: reviewForm.comment,
      decision: reviewForm.decision
    })
    isSubmitted.value = true
    ElMessage.success('评审提交成功！感谢您的工作')
    setTimeout(() => router.push('/review/record'), 1200)
  } catch (error) {
    if (error !== 'cancel' && error !== false) {
      console.error('提交失败', error)
      ElMessage.error(error?.message || '提交失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

function handleSaveDraft() {
  try {
    localStorage.setItem(`review_draft_${paperId.value}`, JSON.stringify(reviewForm))
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function loadDraft() {
  try {
    const draft = localStorage.getItem(`review_draft_${paperId.value}`)
    if (draft) Object.assign(reviewForm, JSON.parse(draft))
  } catch (e) { console.error('加载草稿失败', e) }
}

function handleReset() {
  ElMessageBox.confirm('确认重置所有已填内容吗？此操作不可撤销。', '确认重置', {
    type: 'warning'
  }).then(() => {
    reviewForm.innovation = 75
    reviewForm.methodology = 75
    reviewForm.writing = 75
    reviewForm.comment = ''
    reviewForm.decision = 'minor'
    localStorage.removeItem(`review_draft_${paperId.value}`)
    ElMessage.success('已重置')
  }).catch(() => {})
}

onMounted(() => {
  loadPaper()
  loadDraft()
})
</script>

<style scoped>
.review-page-container { width: 100%; }

.page-header {
  margin-bottom: 16px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: var(--border-radius);
}
.header-content { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 18px; font-weight: 700; color: var(--color-text-primary); }
.status-tag { margin-left: 8px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.paper-info-card, .pdf-card, .side-card, .review-form-card {
  border-radius: var(--border-radius);
  margin-bottom: 16px;
}

.paper-info { padding: 4px 0; }
.paper-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 16px;
  line-height: 1.4;
}
.paper-meta-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--color-text-regular);
}
.meta-item { display: flex; align-items: center; gap: 6px; }
.paper-section { margin-bottom: 14px; }
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-regular);
  margin-bottom: 6px;
}
.keywords-section { display: flex; align-items: flex-start; gap: 8px; }
.keywords-section .section-content { flex: 1; }
.kw-tag { margin-right: 6px; margin-bottom: 4px; }
.abstract-text {
  text-indent: 2em;
  padding: 12px 16px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-primary);
}

.pdf-tools { display: flex; align-items: center; gap: 10px; }
.pdf-wrapper {
  width: 100%;
  min-height: 700px;
  background: #e9ecef;
  border-radius: 10px;
  overflow: auto;
  padding: 20px;
}
.pdf-iframe {
  width: 100%;
  height: 900px;
  border: none;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s;
}
.pdf-fallback { width: 100%; }
.fallback-paper {
  max-width: 780px;
  margin: 0 auto;
  background: #fff;
  padding: 60px 70px;
  min-height: 900px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.1);
  border-radius: 4px;
  font-family: 'Times New Roman', serif;
}
.fb-header { text-align: center; margin-bottom: 40px; }
.fb-title { font-size: 22px; font-weight: 700; margin: 0 0 12px; color: #1a1a1a; }
.fb-authors { font-size: 14px; margin-bottom: 6px; color: #333; }
.fb-affiliation { font-size: 12px; color: #666; font-style: italic; }
.fb-section { margin-bottom: 24px; }
.fb-sec-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #1a1a1a;
  border-bottom: 1px solid #eee;
  padding-bottom: 4px;
}
.fb-sec-content {
  font-size: 13px;
  line-height: 1.9;
  color: #333;
  text-align: justify;
}
.fb-sec-content p { margin: 0 0 10px; text-indent: 2em; }

.figures-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.figure-item {
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--color-border-light);
  cursor: zoom-in;
  transition: all 0.2s;
}
.figure-item:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.1); border-color: var(--color-primary); }
.figure-img {
  width: 100%;
  height: 130px;
  object-fit: cover;
  display: block;
}
.figure-caption {
  padding: 8px 10px;
  background: var(--color-bg-secondary);
}
.fig-num {
  display: inline-block;
  font-size: 11px;
  padding: 1px 6px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 4px;
  margin-right: 6px;
  font-weight: 600;
}
.fig-title { font-size: 12px; color: var(--color-text-regular); }

.empty-placeholder { padding: 20px 0; }

.formulas-list { display: flex; flex-direction: column; gap: 12px; }
.formula-item {
  padding: 12px 14px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
}
.formula-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.formula-num {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(64,158,255,0.1);
  padding: 2px 8px;
  border-radius: 4px;
}
.formula-render {
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  text-align: center;
  overflow-x: auto;
  border: 1px dashed var(--color-border-light);
}
:deep(.formula-render .katex-display) { margin: 0; }
.formula-source {
  margin-top: 8px;
  padding: 6px 10px;
  background: #263238;
  border-radius: 6px;
  max-height: 50px;
  overflow-y: auto;
}
.formula-source code {
  font-size: 11px;
  color: #aed581;
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}

.review-form { padding: 8px 4px; }
.slider-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}
.score-slider { flex: 1; }
.score-badge {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.score-badge.innovation { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.score-badge.methodology { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.score-badge.writing { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }

.score-hint {
  display: flex;
  justify-content: space-between;
  padding: 0 8px;
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-top: -8px;
  margin-bottom: 4px;
}

.overall-wrap {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  border-radius: 12px;
  flex: 1;
}
.overall-circle {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: baseline;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
  position: relative;
}
.overall-circle.excellent { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.overall-circle.good { background: linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%); }
.overall-circle.fair { background: linear-gradient(135deg, #f2994a 0%, #f2c94c 100%); }
.overall-circle.poor { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); }
.overall-score { font-size: 34px; font-weight: 800; line-height: 1; }
.overall-max { font-size: 13px; font-weight: 600; opacity: 0.85; }
.overall-desc { flex: 1; min-width: 0; }
.overall-label { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 6px; }
.overall-detail { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.overall-result { font-size: 13px; color: var(--color-text-regular); }
.excellent-text { color: #11998e; }
.good-text { color: #2193b0; }
.fair-text { color: #f2994a; }
.poor-text { color: #eb3349; }

.submit-btn { min-width: 140px; }

:deep(.review-form .el-form-item__label) { font-weight: 600; }
</style>
