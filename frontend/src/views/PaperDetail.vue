<template>
  <div class="paper-detail-container" v-loading="loading">
    <el-page-header @back="$router.back()" class="page-header">
      <template #content>
        <div class="header-inner">
          <span>论文详情</span>
          <div class="header-actions">
            <el-button
              v-if="showDuplicateCheckBtn"
              type="danger"
              :icon="WarningFilled"
              size="small"
              @click="$router.push(`/paper/${paperId}/duplicate-check`)"
            >
              学术不端检测
            </el-button>
            <el-button
              v-if="showRecommendBtn"
              type="warning"
              :icon="UserFilled"
              size="small"
              @click="recommendVisible = true"
            >
              推荐评审人
            </el-button>
            <el-button
              v-if="showReviewBtn"
              type="success"
              :icon="EditPen"
              size="small"
              @click="$router.push(`/review/review/${paperId}`)"
            >
              开始评审
            </el-button>
            <el-button type="primary" :icon="Download" size="small" plain>
              下载 PDF
            </el-button>
          </div>
        </div>
      </template>
    </el-page-header>

    <el-row :gutter="16" class="detail-row" v-if="paper">
      <el-col :xs="24" :md="5" class="left-col">
        <el-card class="meta-card" shadow="hover">
          <div class="meta-section">
            <h3 class="meta-title">
              <el-icon color="#409eff"><Document /></el-icon>
              基本信息
            </h3>
            <h1 class="paper-title">{{ paper.title }}</h1>
            <div class="meta-status-row">
              <el-tag :type="statusType" effect="dark" size="default">{{ statusText }}</el-tag>
              <span class="submit-date">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(paper.submissionDate) }}
              </span>
            </div>
          </div>

          <el-divider />

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><User /></el-icon>
                作者
              </div>
              <div class="meta-value">
                <el-tag
                  v-for="(author, idx) in (paper.authorsList || defaultAuthorsList).slice(0, 6)"
                  :key="idx"
                  size="small"
                  type="primary"
                  effect="plain"
                  class="author-tag"
                >
                  {{ author }}
                </el-tag>
                <span v-if="(paper.authorsList || defaultAuthorsList).length > 6" class="more">
                  +{{ (paper.authorsList || defaultAuthorsList).length - 6 }}
                </span>
              </div>
            </div>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><OfficeBuilding /></el-icon>
                机构
              </div>
              <div class="meta-value">{{ paper.affiliation || '清华大学计算机科学与技术系' }}</div>
            </div>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><PriceTag /></el-icon>
                关键词
              </div>
              <div class="meta-value">
                <el-tag
                  v-for="(kw, idx) in optimizedKeywords.slice(0, 8)"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                  class="keyword-tag"
                >
                  {{ kw }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><FolderOpened /></el-icon>
                数据集
              </div>
              <div class="meta-value">
                <el-tag
                  v-for="(ds, idx) in (paper.datasets || defaultDatasets)"
                  :key="idx"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="dataset-tag"
                >
                  {{ ds }}
                </el-tag>
                <span v-if="!(paper.datasets || defaultDatasets).length" class="empty">未标注</span>
              </div>
            </div>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><Notebook /></el-icon>
                摘要
              </div>
              <div class="meta-value abstract-content">
                {{ paper.abstract || defaultAbstract }}
              </div>
            </div>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <div class="meta-label">
                <el-icon><Link /></el-icon>
                引用关系
              </div>
              <div class="meta-value references-list">
                <div
                  v-for="(ref, idx) in (paper.references || defaultReferences).slice(0, 6)"
                  :key="idx"
                  class="reference-item"
                >
                  <span class="ref-index">[{{ idx + 1 }}]</span>
                  <span class="ref-text">{{ ref }}</span>
                </div>
                <el-link
                  v-if="(paper.references || defaultReferences).length > 6"
                  type="primary"
                  :underline="false"
                  class="show-more"
                >
                  查看全部 {{ paper.references?.length || defaultReferences.length }} 条引用
                </el-link>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="13" class="center-col">
        <el-card class="pdf-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon color="#409eff"><Reading /></el-icon>
                PDF 全文预览
              </span>
              <div class="pdf-tools">
                <el-button-group>
                  <el-button size="small" :icon="ZoomOut" @click="zoomOut">缩小</el-button>
                  <el-button size="small" :icon="ZoomIn" @click="zoomIn">放大</el-button>
                  <el-button size="small" :icon="Refresh" @click="resetZoom">重置</el-button>
                </el-button-group>
                <el-button size="small" type="primary" :icon="Download">下载</el-button>
              </div>
            </div>
          </template>
          <div class="pdf-wrapper">
            <div class="pdf-iframe-wrapper" :style="{ transform: `scale(${pdfScale})` }">
              <iframe
                v-if="pdfUrl"
                :src="pdfUrl"
                class="pdf-iframe"
                frameborder="0"
              />
              <div v-else class="pdf-fallback">
                <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
                <p class="fallback-title">论文 PDF 预览</p>
                <p class="fallback-desc">当前为演示模式，实际部署后将加载真实 PDF 文件</p>
                <div class="mock-pdf-content">
                  <div class="mock-page">
                    <h2>{{ paper.title }}</h2>
                    <p class="mock-authors">{{ paper.authors || defaultAuthorsList.join(', ') }}</p>
                    <p class="mock-affiliation">{{ paper.affiliation || '清华大学计算机科学与技术系' }}</p>
                    <h3>摘要</h3>
                    <p>{{ paper.abstract || defaultAbstract }}</p>
                    <h3>关键词</h3>
                    <p>{{ (paper.keywords || defaultKeywords).join('; ') }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="6" class="right-col">
        <el-card class="tabs-card" shadow="hover">
          <el-tabs v-model="activeTab" class="detail-tabs" tab-position="top">
            <el-tab-pane label="图表" name="figures">
              <template #label>
                <span class="tab-label">
                  <el-icon><Picture /></el-icon>
                  图表 ({{ figures.length }})
                </span>
              </template>
              <div class="figures-container">
                <el-empty
                  v-if="!figures.length"
                  description="暂无图表数据"
                  :image-size="80"
                />
                <div v-else class="figures-grid" v-viewer>
                  <div
                    v-for="(fig, idx) in figures"
                    :key="fig.id"
                    class="figure-item"
                  >
                    <div class="figure-img-wrapper">
                      <img
                        :src="getFigureImage(idx)"
                        :alt="fig.title"
                        class="figure-img"
                        loading="lazy"
                      />
                      <div class="figure-index">图 {{ idx + 1 }}</div>
                    </div>
                    <div class="figure-caption">
                      <div class="figure-title">{{ fig.title }}</div>
                      <div class="figure-desc">{{ fig.caption }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="公式" name="formulas">
              <template #label>
                <span class="tab-label">
                  <el-icon><Operation /></el-icon>
                  公式 ({{ formulas.length }})
                </span>
              </template>
              <div class="formulas-container">
                <el-empty
                  v-if="!formulas.length"
                  description="暂无公式数据"
                  :image-size="80"
                />
                <div v-else>
                  <div
                    v-for="(formula, idx) in formulas"
                    :key="formula.id"
                    class="formula-item"
                  >
                    <div class="formula-header">
                      <span class="formula-index">公式 ({{ idx + 1 }})</span>
                      <el-button
                        type="primary"
                        link
                        size="small"
                        :icon="CopyDocument"
                        @click="copyFormula(formula.latex, idx)"
                      >
                        {{ copiedIndex === idx ? '已复制' : '复制' }}
                      </el-button>
                    </div>
                    <div
                      class="formula-content"
                      v-html="renderFormula(formula.latex)"
                    />
                    <div class="formula-latex">
                      <code>{{ formula.latex }}</code>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="知识图谱" name="graph">
              <template #label>
                <span class="tab-label">
                  <el-icon><Share /></el-icon>
                  知识图谱
                </span>
              </template>
              <div class="graph-container">
                <div class="graph-section core-topics-section">
                  <h4 class="graph-section-title">
                    <el-icon color="#f57c00"><Star /></el-icon>
                    核心主题
                    <el-tag type="warning" size="small" effect="dark" class="core-tag">核心</el-tag>
                  </h4>
                  <div class="core-topics-content">
                    <TopicCloud :topics="coreTopics" :max-tags="15" />
                  </div>
                </div>
                <el-divider />
                <div class="graph-section">
                  <h4 class="graph-section-title">
                    <el-icon color="#667eea"><CollectionTag /></el-icon>
                    关键词节点
                  </h4>
                  <div class="node-list">
                    <span
                      v-for="(kw, idx) in optimizedKeywords.slice(0, 6)"
                      :key="idx"
                      class="node-item keyword-node"
                    >
                      <el-icon><Connection /></el-icon>
                      {{ kw }}
                    </span>
                  </div>
                </div>
                <el-divider />
                <div class="graph-section">
                  <h4 class="graph-section-title">
                    <el-icon color="#f5576c"><UserFilled /></el-icon>
                    作者节点
                  </h4>
                  <div class="node-list">
                    <span
                      v-for="(author, idx) in (paper.authorsList || defaultAuthorsList).slice(0, 4)"
                      :key="idx"
                      class="node-item author-node"
                    >
                      <el-icon><User /></el-icon>
                      {{ author }}
                    </span>
                  </div>
                </div>
                <el-divider />
                <div class="graph-section">
                  <h4 class="graph-section-title">
                    <el-icon color="#43e97b"><FolderOpened /></el-icon>
                    数据集节点
                  </h4>
                  <div class="node-list">
                    <span
                      v-for="(ds, idx) in (paper.datasets || defaultDatasets)"
                      :key="idx"
                      class="node-item dataset-node"
                    >
                      <el-icon><Folder /></el-icon>
                      {{ ds }}
                    </span>
                    <span v-if="!(paper.datasets || defaultDatasets).length" class="empty">无关联数据集</span>
                  </div>
                </div>
                <el-divider />
                <div class="graph-section">
                  <h4 class="graph-section-title">
                    <el-icon color="#4facfe"><OfficeBuilding /></el-icon>
                    机构节点
                  </h4>
                  <div class="node-list">
                    <span class="node-item institution-node">
                      <el-icon><OfficeBuilding /></el-icon>
                      {{ paper.affiliation || '清华大学计算机科学与技术系' }}
                    </span>
                  </div>
                </div>
                <el-divider />
                <div class="graph-legend">
                  <div class="legend-item">
                    <span class="legend-dot keyword-dot"></span>
                    <span>关键词</span>
                  </div>
                  <div class="legend-item">
                    <span class="legend-dot author-dot"></span>
                    <span>作者</span>
                  </div>
                  <div class="legend-item">
                    <span class="legend-dot dataset-dot"></span>
                    <span>数据集</span>
                  </div>
                  <div class="legend-item">
                    <span class="legend-dot institution-dot"></span>
                    <span>机构</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="引用网络" name="citation">
              <template #label>
                <span class="tab-label">
                  <el-icon><Connection /></el-icon>
                  引用网络
                </span>
              </template>
              <div class="citation-container">
                <div class="citation-section">
                  <h4 class="citation-section-title">
                    <el-icon color="#667eea"><Share /></el-icon>
                    引用关系图
                  </h4>
                  <CitationGraph
                    :network="citationNetwork"
                    :paper-title="paper?.title"
                    height="360px"
                  />
                </div>
                <el-divider />
                <div class="citation-section">
                  <h4 class="citation-section-title">
                    <el-icon color="#f57c00"><Medal /></el-icon>
                    核心参考文献 (PageRank)
                  </h4>
                  <CoreReferences :references="coreReferences" />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="recommendVisible" title="推荐评审人" width="640px" destroy-on-close>
      <div class="recommend-dialog">
        <div class="recommend-paper-info">
          <el-icon color="#409eff"><Document /></el-icon>
          <span class="paper-title-text">{{ paper?.title }}</span>
        </div>
        <el-divider />
        <div class="recommend-list" v-loading="recommendLoading">
          <div
            v-for="(reviewer, idx) in recommendList"
            :key="reviewer.id"
            class="recommend-item"
            :class="{ selected: selectedReviewers.includes(reviewer.id) }"
            @click="toggleReviewer(reviewer.id)"
          >
            <div class="rank-badge" :class="`rank-${idx + 1}`">{{ idx + 1 }}</div>
            <el-checkbox :model-value="selectedReviewers.includes(reviewer.id)" :value="reviewer.id" />
            <el-avatar :size="48">
              {{ reviewer.name?.charAt(0) }}
            </el-avatar>
            <div class="reviewer-info">
              <div class="reviewer-name">
                <span class="name">{{ reviewer.name }}</span>
                <el-tag size="small" type="success" effect="plain">{{ reviewer.affiliation }}</el-tag>
              </div>
              <div class="reviewer-fields">
                <el-tag
                  v-for="(f, fIdx) in (reviewer.fields || []).slice(0, 4)"
                  :key="fIdx"
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
              <div class="match-score" :style="{ color: getMatchColor(reviewer.match) }">
                {{ reviewer.match }}%
              </div>
              <div class="match-label">匹配度</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="recommendVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="assignLoading"
          :disabled="!selectedReviewers.length"
          @click="handleAssign"
        >
          确认分配 ({{ selectedReviewers.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Calendar,
  User,
  OfficeBuilding,
  PriceTag,
  FolderOpened,
  Notebook,
  Link,
  Download,
  EditPen,
  UserFilled,
  Reading,
  ZoomIn,
  ZoomOut,
  Refresh,
  Picture,
  Operation,
  Share,
  CollectionTag,
  Connection,
  Folder,
  CopyDocument,
  Star,
  Medal,
  WarningFilled
} from '@element-plus/icons-vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { getPaperById } from '@/api/paper'
import { recommendReviewers, assignReviewers } from '@/api/review'
import { useUserStore } from '@/store/user'
import TopicCloud from '@/components/TopicCloud.vue'
import CitationGraph from '@/components/CitationGraph.vue'
import CoreReferences from '@/components/CoreReferences.vue'
import { getCoreTopics, getOptimizedKeywords } from '@/api/topic'
import { getCitationNetwork, getCoreReferences } from '@/api/citation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const paper = ref(null)
const activeTab = ref('figures')
const pdfScale = ref(1)
const pdfUrl = ref('')
const copiedIndex = ref(-1)
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const assignLoading = ref(false)
const recommendList = ref([])
const selectedReviewers = ref([])
const coreTopics = ref([])
const optimizedKeywordsList = ref([])
const citationNetwork = ref({ nodes: [], edges: [], pagerankScores: {} })
const coreReferences = ref([])

const paperId = computed(() => route.params.id)

const defaultCitationNetwork = {
  nodes: [
    { id: '0', title: '当前论文', isCenter: true },
    { id: '1', title: 'Deep Residual Learning for Image Recognition' },
    { id: '2', title: 'Attention Is All You Need' },
    { id: '3', title: 'Very Deep Convolutional Networks for Large-Scale Image Recognition' },
    { id: '4', title: 'ImageNet Classification with Deep Convolutional Neural Networks' },
    { id: '5', title: 'Squeeze-and-Excitation Networks' },
    { id: '6', title: 'CBAM: Convolutional Block Attention Module' },
    { id: '7', title: 'EfficientNet: Rethinking Model Scaling for CNNs' }
  ],
  edges: [
    { source: '0', target: '1' },
    { source: '0', target: '2' },
    { source: '0', target: '3' },
    { source: '0', target: '4' },
    { source: '0', target: '5' },
    { source: '0', target: '6' },
    { source: '0', target: '7' }
  ],
  pagerankScores: { '0': 1.0, '1': 0.85, '2': 0.78, '3': 0.65, '4': 0.72, '5': 0.55, '6': 0.48, '7': 0.42 }
}

const defaultCoreReferences = [
  { title: 'Deep Residual Learning for Image Recognition', authors: 'He et al.', year: 2016, venue: 'CVPR', pagerank: 0.085 },
  { title: 'Attention Is All You Need', authors: 'Vaswani et al.', year: 2017, venue: 'NeurIPS', pagerank: 0.078 },
  { title: 'ImageNet Classification with Deep Convolutional Neural Networks', authors: 'Krizhevsky et al.', year: 2012, venue: 'NeurIPS', pagerank: 0.072 },
  { title: 'Very Deep Convolutional Networks for Large-Scale Image Recognition', authors: 'Simonyan et al.', year: 2015, venue: 'ICLR', pagerank: 0.065 },
  { title: 'Squeeze-and-Excitation Networks', authors: 'Hu et al.', year: 2018, venue: 'CVPR', pagerank: 0.055 }
]

const defaultAuthorsList = ['张三', '李四', '王五', '赵六']
const defaultKeywords = ['深度学习', '图像识别', '卷积神经网络', '注意力机制', '特征提取']

const defaultCoreTopics = [
  { name: '深度学习', weight: 0.95, isCore: true, source: 'combined' },
  { name: '卷积神经网络', weight: 0.88, isCore: true, source: 'combined' },
  { name: '图像识别', weight: 0.82, isCore: true, source: 'bertopic' },
  { name: '注意力机制', weight: 0.76, isCore: false, source: 'pagerank' },
  { name: '特征提取', weight: 0.68, isCore: false, source: 'bertopic' },
  { name: '多尺度融合', weight: 0.62, isCore: false, source: 'pagerank' },
  { name: '计算机视觉', weight: 0.55, isCore: false, source: 'bertopic' }
]

const defaultOptimizedKeywords = [
  '深度学习', '卷积神经网络', '图像识别', '注意力机制',
  '特征提取', '多尺度特征融合', '计算机视觉', '模式识别'
]
const defaultDatasets = ['ImageNet', 'CIFAR-100', 'COCO']
const defaultAbstract = '本文研究了深度学习在图像识别领域的应用，提出了一种新的卷积神经网络结构。通过在网络设计中引入多尺度特征融合机制和通道注意力机制，有效地提升了网络对细粒度特征的捕捉能力。在多个公开数据集上的实验结果表明，本文方法在准确率和推理速度上均优于现有SOTA方法。'
const defaultReferences = [
  'He K, Zhang X, Ren S, et al. Deep Residual Learning for Image Recognition[C]//CVPR. 2016.',
  'Vaswani A, Shazeer N, Parmar N, et al. Attention Is All You Need[C]//NeurIPS. 2017.',
  'Simonyan K, Zisserman A. Very Deep Convolutional Networks for Large-Scale Image Recognition[J]. ICLR, 2015.',
  'Krizhevsky A, Sutskever I, Hinton G E. ImageNet Classification with Deep Convolutional Neural Networks[C]//NeurIPS. 2012.',
  'Hu J, Shen L, Sun G. Squeeze-and-Excitation Networks[C]//CVPR. 2018.',
  'Woo S, Park J, Lee J Y, et al. CBAM: Convolutional Block Attention Module[C]//ECCV. 2018.'
]

const defaultFigures = [
  { id: 1, title: '图1 网络整体架构', caption: '本文提出的多尺度特征融合网络整体架构图，包含主干网络、特征融合模块和分类头部。' },
  { id: 2, title: '图2 注意力模块结构', caption: '通道注意力与空间注意力的详细结构设计，使用全局平均池化和最大池化。' },
  { id: 3, title: '图3 实验结果对比', caption: '各方法在ImageNet数据集上的Top-1准确率与推理速度的比较散点图。' },
  { id: 4, title: '图4 消融实验分析', caption: '各模块对模型性能贡献的消融实验结果柱状图，分别验证了各组件的有效性。' },
  { id: 5, title: '图5 特征可视化', caption: '不同层特征图的可视化效果，展示了模型对细粒度特征的捕捉能力。' },
  { id: 6, title: '图6 训练损失曲线', caption: '本文方法与基线模型在训练过程中的损失值变化对比曲线。' }
]

const defaultFormulas = [
  { id: 1, latex: 'L = -\\sum_{i=1}^{N} y_i \\log(\\hat{y}_i) + \\lambda \\|\\theta\\|_2^2' },
  { id: 2, latex: 'Attention(Q,K,V) = softmax\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V' },
  { id: 3, latex: 'F_{out} = \\sigma(W_1 * F_{in} + b_1) \\odot \\tanh(W_2 * F_{in} + b_2)' },
  { id: 4, latex: 'Accuracy = \\frac{TP + TN}{TP + TN + FP + FN}' },
  { id: 5, latex: 'F_1 = 2 \\cdot \\frac{Precision \\cdot Recall}{Precision + Recall}' }
]

const defaultReviewers = [
  { id: 1, name: '陈教授', affiliation: '清华大学', fields: ['机器学习', '深度学习', '计算机视觉'], match: 95 },
  { id: 2, name: '刘研究员', affiliation: '中科院计算所', fields: ['图像识别', '神经网络', '模式识别'], match: 88 },
  { id: 3, name: '周博士', affiliation: '北京大学', fields: ['深度学习', 'NLP', '知识图谱'], match: 82 }
]

const figures = computed(() => paper.value?.figures || defaultFigures)
const formulas = computed(() => paper.value?.formulas || defaultFormulas)

const optimizedKeywords = computed(() => {
  return optimizedKeywordsList.value.length 
    ? optimizedKeywordsList.value 
    : (paper.value?.optimizedKeywords || defaultOptimizedKeywords)
})

const statusText = computed(() => {
  const map = { pending: '待评审', reviewing: '评审中', accepted: '已录用', rejected: '已拒稿', published: '已发表' }
  return map[paper.value?.status] || paper.value?.status || '未知'
})

const statusType = computed(() => {
  const map = { pending: 'warning', reviewing: 'primary', accepted: 'success', rejected: 'danger', published: 'success' }
  return map[paper.value?.status] || 'info'
})

const showRecommendBtn = computed(() => userStore.isAdmin && paper.value?.status === 'pending')
const showReviewBtn = computed(() => (userStore.isReviewer || userStore.isAdmin) && (paper.value?.status === 'pending' || paper.value?.status === 'reviewing'))
const showDuplicateCheckBtn = computed(() => (userStore.isAuthor || userStore.isAdmin) && paper.value)

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN')
  } catch (e) {
    return dateStr
  }
}

function renderFormula(latex) {
  try {
    return katex.renderToString(latex, {
      throwOnError: false,
      displayMode: true,
      strict: false
    })
  } catch (e) {
    return `<code>${latex}</code>`
  }
}

function copyFormula(latex, idx) {
  navigator.clipboard?.writeText(latex).then(() => {
    copiedIndex.value = idx
    ElMessage.success('公式已复制')
    setTimeout(() => { copiedIndex.value = -1 }, 2000)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function getFigureImage(idx) {
  const prompts = [
    'deep learning neural network architecture diagram, blue purple tech style',
    'attention mechanism module visualization, data flow, connection lines',
    'bar chart comparing accuracy metrics, data visualization',
    'ablation study experimental results bar chart',
    'CNN feature map visualization heatmaps',
    'training loss curves comparison line chart'
  ]
  const p = encodeURIComponent(prompts[idx % prompts.length])
  return `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${p}&image_size=square`
}

function zoomIn() {
  pdfScale.value = Math.min(pdfScale.value + 0.1, 2)
}

function zoomOut() {
  pdfScale.value = Math.max(pdfScale.value - 0.1, 0.5)
}

function resetZoom() {
  pdfScale.value = 1
}

function getMatchColor(match) {
  if (match >= 90) return '#67c23a'
  if (match >= 80) return '#409eff'
  if (match >= 70) return '#e6a23c'
  return '#909399'
}

async function loadPaper() {
  loading.value = true
  try {
    const res = await getPaperById(paperId.value)
    const data = res.data || res
    paper.value = data
    if (Array.isArray(data.authors) && !data.authorsList) {
      paper.value.authorsList = data.authors
    }
    if (typeof data.authors === 'string' && !data.authorsList) {
      paper.value.authorsList = data.authors.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    }
  } catch (e) {
    console.error('加载论文详情失败', e)
    paper.value = {
      id: paperId.value,
      title: '基于深度学习的图像识别研究与应用',
      authors: defaultAuthorsList.join(', '),
      authorsList: defaultAuthorsList,
      affiliation: '清华大学计算机科学与技术系',
      keywords: defaultKeywords,
      datasets: defaultDatasets,
      abstract: defaultAbstract,
      references: defaultReferences,
      figures: defaultFigures,
      formulas: defaultFormulas,
      status: 'reviewing',
      submissionDate: '2026-06-15T10:30:00'
    }
  } finally {
    loading.value = false
  }
}

async function loadRecommenders() {
  recommendLoading.value = true
  try {
    const res = await recommendReviewers({ paperId: paperId.value })
    const data = res.data || res
    recommendList.value = Array.isArray(data) ? data : (data.list || defaultReviewers)
  } catch (e) {
    console.error('获取推荐评审人失败', e)
    recommendList.value = defaultReviewers
  } finally {
    recommendLoading.value = false
  }
}

async function loadCoreTopics() {
  try {
    const res = await getCoreTopics(paperId.value)
    const data = res.data || res
    if (Array.isArray(data) && data.length) {
      coreTopics.value = data
    } else if (data.list && Array.isArray(data.list)) {
      coreTopics.value = data.list
    } else {
      coreTopics.value = defaultCoreTopics
    }
  } catch (e) {
    console.error('加载核心主题失败', e)
    coreTopics.value = defaultCoreTopics
  }
}

async function loadOptimizedKeywords() {
  try {
    const res = await getOptimizedKeywords(paperId.value)
    const data = res.data || res
    if (Array.isArray(data) && data.length) {
      optimizedKeywordsList.value = data
    } else if (data.list && Array.isArray(data.list)) {
      optimizedKeywordsList.value = data.list
    } else if (data.keywords && Array.isArray(data.keywords)) {
      optimizedKeywordsList.value = data.keywords
    }
  } catch (e) {
    console.error('加载优化关键词失败', e)
  }
}

async function loadCitationNetwork() {
  try {
    const res = await getCitationNetwork(paperId.value)
    const data = res.data || res
    if (data?.nodes?.length) {
      citationNetwork.value = data
    } else {
      citationNetwork.value = defaultCitationNetwork
    }
  } catch (e) {
    console.error('加载引用网络失败', e)
    citationNetwork.value = defaultCitationNetwork
  }
}

async function loadCoreReferences() {
  try {
    const res = await getCoreReferences(paperId.value, 5)
    const data = res.data || res
    if (Array.isArray(data) && data.length) {
      coreReferences.value = data
    } else if (data?.list?.length) {
      coreReferences.value = data.list
    } else {
      coreReferences.value = defaultCoreReferences
    }
  } catch (e) {
    console.error('加载核心参考文献失败', e)
    coreReferences.value = defaultCoreReferences
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

async function handleAssign() {
  if (!selectedReviewers.value.length) {
    ElMessage.warning('请至少选择一位评审人')
    return
  }
  assignLoading.value = true
  try {
    try {
      await assignReviewers({ paperId: paperId.value, reviewerIds: selectedReviewers.value })
    } catch (e) { console.error(e) }
    ElMessage.success(`已成功分配 ${selectedReviewers.value.length} 位评审人`)
    recommendVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    assignLoading.value = false
  }
}

onMounted(async () => {
  await loadPaper()
  await Promise.all([
    loadCoreTopics(),
    loadOptimizedKeywords(),
    loadCitationNetwork(),
    loadCoreReferences()
  ])
  if (recommendVisible.value) {
    await loadRecommenders()
  }

  const queryFormula = route.query.formula
  const queryPage = route.query.page
  const queryY = route.query.y
  if (queryFormula) {
    setTimeout(() => {
      const iframe = document.querySelector('.pdf-iframe')
      if (iframe && queryPage) {
        let baseUrl = pdfUrl.value || iframe.src.split('#')[0]
        if (!baseUrl) {
          ElMessage.info(`跳转到公式 ${queryFormula}（第${queryPage}页）`)
          return
        }
        const yParam = queryY ? `&view=FitH,${queryY}` : ''
        iframe.src = `${baseUrl}#page=${queryPage}${yParam}&toolbar=1`
        ElMessage.success(`跳转到公式 ${queryFormula}`)
      } else {
        ElMessage.info(`跳转到公式 ${queryFormula}`)
      }
    }, 1000)
  }
})
</script>

<style scoped>
.paper-detail-container {
  width: 100%;
}

.page-header {
  margin-bottom: 16px;
  padding: 12px 16px;
  background-color: var(--color-bg-primary);
  border-radius: var(--border-radius);
}

.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-row {
  align-items: stretch;
}

.left-col,
.center-col,
.right-col {
  display: flex;
  flex-direction: column;
}

.meta-card,
.pdf-card,
.tabs-card {
  border-radius: var(--border-radius);
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.el-card__body) {
  flex: 1;
  overflow: auto;
}

.meta-section {
  margin-bottom: 18px;
}

.meta-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.paper-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.5;
  margin: 0 0 12px;
}

.meta-status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.submit-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-regular);
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-label .el-icon {
  color: var(--color-primary);
}

.meta-value {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
  padding-left: 4px;
}

.meta-value.empty {
  color: var(--color-text-placeholder);
}

.author-tag,
.keyword-tag,
.dataset-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.meta-value .more {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.abstract-content {
  text-indent: 2em;
  background-color: var(--color-bg-secondary);
  padding: 12px;
  border-radius: var(--border-radius);
  font-size: 13px;
  max-height: 160px;
  overflow-y: auto;
}

.references-list {
  max-height: 200px;
  overflow-y: auto;
}

.reference-item {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.ref-index {
  color: var(--color-primary);
  font-weight: 600;
  flex-shrink: 0;
}

.ref-text {
  color: var(--color-text-regular);
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.show-more {
  margin-top: 8px;
  display: inline-block;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pdf-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pdf-wrapper {
  width: 100%;
  height: calc(100vh - 280px);
  min-height: 600px;
  overflow: auto;
  background-color: #f0f2f5;
  border-radius: var(--border-radius);
  display: flex;
  justify-content: center;
  padding: 20px;
}

.pdf-iframe-wrapper {
  width: 100%;
  height: 100%;
  transform-origin: top center;
  transition: transform 0.2s;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border-radius: 4px;
}

.pdf-fallback {
  width: 100%;
  min-height: 600px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.fallback-title {
  font-size: 18px;
  color: var(--color-text-regular);
  margin: 16px 0 4px;
}

.fallback-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 30px;
}

.mock-pdf-content {
  width: 100%;
  max-width: 720px;
}

.mock-page {
  padding: 48px 56px;
  border: 1px solid var(--color-border-light);
  border-radius: 4px;
  min-height: 700px;
}

.mock-page h2 {
  font-size: 22px;
  text-align: center;
  margin: 0 0 16px;
  color: var(--color-text-primary);
  line-height: 1.5;
}

.mock-authors {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-regular);
  margin: 0 0 6px;
}

.mock-affiliation {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0 0 40px;
}

.mock-page h3 {
  font-size: 16px;
  margin: 24px 0 12px;
  color: var(--color-text-primary);
}

.mock-page p {
  text-indent: 2em;
  font-size: 13px;
  line-height: 2;
  color: var(--color-text-regular);
  margin: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.figures-container,
.formulas-container,
.graph-container {
  padding: 4px 0;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.figures-grid {
  display: grid;
  gap: 14px;
}

.figure-item {
  border: 1px solid var(--color-border-light);
  border-radius: var(--border-radius);
  overflow: hidden;
  background-color: var(--color-bg-primary);
}

.figure-img-wrapper {
  position: relative;
  width: 100%;
  padding-top: 75%;
  overflow: hidden;
  cursor: zoom-in;
  background-color: var(--color-bg-secondary);
}

.figure-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.figure-img-wrapper:hover .figure-img {
  transform: scale(1.05);
}

.figure-index {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.figure-caption {
  padding: 10px 12px;
}

.figure-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.figure-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.formula-item {
  border: 1px solid var(--color-border-light);
  border-radius: var(--border-radius);
  padding: 12px;
  margin-bottom: 12px;
  background-color: var(--color-bg-primary);
}

.formula-item:last-child {
  margin-bottom: 0;
}

.formula-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.formula-index {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
}

:deep(.formula-content) {
  padding: 12px;
  background-color: var(--color-bg-secondary);
  border-radius: var(--border-radius);
  overflow-x: auto;
  text-align: center;
}

:deep(.formula-content .katex) {
  font-size: 1.1em;
}

.formula-latex {
  margin-top: 8px;
  padding: 8px;
  background-color: #f8f9fa;
  border-radius: 4px;
  overflow-x: auto;
}

.formula-latex code {
  font-size: 12px;
  color: var(--color-text-regular);
  font-family: 'Consolas', 'Monaco', monospace;
  word-break: break-all;
}

.graph-section {
  margin-bottom: 12px;
}

.graph-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.node-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-left: 4px;
}

.node-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
}

.node-item:hover {
  transform: translateY(-1px);
}

.node-item.empty {
  color: var(--color-text-placeholder);
  background: none;
  border: none;
  padding: 0;
}

.keyword-node {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12), rgba(118, 75, 162, 0.12));
  color: #667eea;
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.author-node {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.12), rgba(245, 87, 108, 0.12));
  color: #f5576c;
  border: 1px solid rgba(245, 87, 108, 0.3);
}

.dataset-node {
  background: linear-gradient(135deg, rgba(67, 233, 123, 0.12), rgba(56, 249, 215, 0.12));
  color: #43e97b;
  border: 1px solid rgba(67, 233, 123, 0.3);
}

.institution-node {
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.12), rgba(0, 242, 254, 0.12));
  color: #4facfe;
  border: 1px solid rgba(79, 172, 254, 0.3);
}

.graph-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px;
  background-color: var(--color-bg-secondary);
  border-radius: var(--border-radius);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.keyword-dot { background-color: #667eea; }
.author-dot { background-color: #f5576c; }
.dataset-dot { background-color: #43e97b; }
.institution-dot { background-color: #4facfe; }

.recommend-dialog {
  padding: 4px 0;
}

.recommend-paper-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background-color: var(--color-bg-secondary);
  border-radius: var(--border-radius);
}

.paper-title-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.recommend-list {
  max-height: 460px;
  overflow-y: auto;
  padding-right: 4px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.2s;
  background-color: var(--color-bg-primary);
  position: relative;
}

.recommend-item:hover {
  border-color: var(--color-primary-light);
  background-color: rgba(64, 158, 255, 0.03);
}

.recommend-item.selected {
  border-color: var(--color-primary);
  background-color: rgba(64, 158, 255, 0.08);
}

.rank-badge {
  position: absolute;
  top: -8px;
  left: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background-color: var(--color-text-placeholder);
}

.rank-1 { background: linear-gradient(135deg, #ffd700, #ffb700); }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a8a8a8); }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #b87333); }

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
  text-align: center;
  flex-shrink: 0;
  width: 60px;
}

.match-score {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.match-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

:deep(.detail-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.detail-tabs .el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

:deep(.detail-tabs .el-tab-pane) {
  height: 100%;
  overflow: auto;
}

.core-topics-section .graph-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.core-tag {
  margin-left: 4px;
}

.core-topics-content {
  margin-top: 8px;
}

.citation-container {
  padding: 4px 0;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}
.citation-section { margin-bottom: 8px; }
.citation-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
