<template>
  <div class="paper-upload-container">
    <h2 class="page-title">论文上传</h2>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card class="upload-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Upload /></el-icon>
                上传论文 PDF
              </span>
              <el-tag type="info" effect="plain" size="small">
                支持 .pdf 格式，最大 50MB
              </el-tag>
            </div>
          </template>

          <el-form
            ref="uploadFormRef"
            :model="uploadForm"
            :rules="uploadRules"
            label-width="100px"
            class="upload-form"
          >
            <el-form-item label="论文标题" prop="title">
              <el-input
                v-model="uploadForm.title"
                placeholder="请输入论文标题（可选，上传后可自动解析）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="作者" prop="authors">
              <el-input
                v-model="uploadForm.authors"
                placeholder="请输入作者，多个作者用逗号分隔（可选）"
              />
            </el-form-item>

            <el-form-item label="研究领域" prop="field">
              <el-select
                v-model="uploadForm.field"
                placeholder="请选择研究领域（可选）"
                style="width: 100%"
                clearable
              >
                <el-option label="计算机科学" value="computer_science" />
                <el-option label="人工智能" value="artificial_intelligence" />
                <el-option label="机器学习" value="machine_learning" />
                <el-option label="数据科学" value="data_science" />
                <el-option label="软件工程" value="software_engineering" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>

            <el-form-item label="关键词" prop="keywords">
              <el-select
                v-model="uploadForm.keywords"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="请输入关键词，按回车添加（可选）"
                style="width: 100%"
              >
              </el-select>
            </el-form-item>

            <el-form-item label="上传PDF" prop="file">
              <el-upload
                ref="uploadRef"
                class="upload-dragger"
                drag
                :limit="1"
                :auto-upload="false"
                accept=".pdf"
                :show-file-list="true"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                :on-remove="handleFileRemove"
                :file-list="fileList"
                :http-request="handleUploadRequest"
              >
                <el-icon class="uploader-icon" :size="60" color="#409eff"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  将 PDF 文件拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    仅支持 PDF 格式文件，文件大小不超过 50MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <el-progress
              v-if="uploading && uploadPercent > 0"
              :percentage="uploadPercent"
              :status="uploadStatus"
              class="upload-progress"
              :stroke-width="12"
            />

            <el-form-item>
              <el-button
                type="primary"
                :loading="uploading"
                :icon="Promotion"
                size="large"
                :disabled="!hasFile"
                @click="handleSubmit"
              >
                {{ uploading ? '上传中...' : '提交论文' }}
              </el-button>
              <el-button :icon="RefreshLeft" size="large" @click="handleReset">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card
          v-if="parseResult || uploading"
          class="parse-result-card"
          shadow="hover"
        >
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon :color="parseResult ? '#67c23a' : '#e6a23c'"><MagicStick /></el-icon>
                {{ uploading ? '正在解析论文...' : '解析结果预览' }}
              </span>
              <el-icon v-if="parseResult" :size="20" color="#67c23a"><CircleCheckFilled /></el-icon>
            </div>
          </template>

          <div v-if="uploading && !parseResult" class="parsing-placeholder">
            <el-icon class="loading-icon" :size="48" color="#409eff"><Loading /></el-icon>
            <p>正在解析论文内容，请稍候...</p>
            <p class="tip">系统将自动提取标题、摘要、作者、关键词等信息</p>
          </div>

          <div v-else-if="parseResult" class="parse-result">
            <div class="result-item">
              <div class="result-label">
                <el-icon><Document /></el-icon>
                标题
              </div>
              <div class="result-value">{{ parseResult.title || '未解析到' }}</div>
            </div>

            <div class="result-item">
              <div class="result-label">
                <el-icon><User /></el-icon>
                作者
              </div>
              <div class="result-value">
                <template v-if="parseResult.authors && parseResult.authors.length">
                  <el-tag
                    v-for="(author, idx) in parseResult.authors.slice(0, 5)"
                    :key="idx"
                    size="small"
                    type="primary"
                    effect="plain"
                    class="author-tag"
                  >
                    {{ author }}
                  </el-tag>
                  <span v-if="parseResult.authors.length > 5" class="more">
                    +{{ parseResult.authors.length - 5 }}
                  </span>
                </template>
                <span v-else class="empty">未解析到</span>
              </div>
            </div>

            <div class="result-item">
              <div class="result-label">
                <el-icon><OfficeBuilding /></el-icon>
                机构
              </div>
              <div class="result-value">{{ parseResult.affiliation || '未解析到' }}</div>
            </div>

            <div class="result-item">
              <div class="result-label">
                <el-icon><Tickets /></el-icon>
                关键词
              </div>
              <div class="result-value">
                <template v-if="parseResult.keywords && parseResult.keywords.length">
                  <el-tag
                    v-for="(kw, idx) in parseResult.keywords.slice(0, 8)"
                    :key="idx"
                    size="small"
                    type="info"
                    effect="plain"
                    class="keyword-tag"
                  >
                    {{ kw }}
                  </el-tag>
                </template>
                <span v-else class="empty">未解析到</span>
              </div>
            </div>

            <div class="result-item">
              <div class="result-label">
                <el-icon><Notebook /></el-icon>
                摘要
              </div>
              <div class="result-value abstract-value">
                {{ parseResult.abstract || '未解析到摘要' }}
              </div>
            </div>

            <el-divider />

            <div class="stats-row">
              <div class="stat-item">
                <div class="stat-num">{{ parseResult.figures?.length || 0 }}</div>
                <div class="stat-label">图表</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ parseResult.formulas?.length || 0 }}</div>
                <div class="stat-label">公式</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ parseResult.references?.length || 0 }}</div>
                <div class="stat-label">参考文献</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ parseResult.pages || 0 }}</div>
                <div class="stat-label">页数</div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card v-else class="tips-card" shadow="hover">
          <template #header>
            <span class="card-title">
              <el-icon color="#e6a23c"><WarningFilled /></el-icon>
              上传须知
            </span>
          </template>
          <el-steps :active="0" direction="vertical" finish-status="wait" process-status="process">
            <el-step title="选择 PDF 文件" description="支持拖拽上传或点击选择文件" />
            <el-step title="填写基本信息" description="可选，系统将自动解析论文内容" />
            <el-step title="提交并解析" description="提交后系统将自动提取论文元信息" />
            <el-step title="确认提交" description="核对解析结果后完成上传" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Upload,
  UploadFilled,
  Promotion,
  RefreshLeft,
  MagicStick,
  CircleCheckFilled,
  Loading,
  Document,
  User,
  OfficeBuilding,
  Tickets,
  Notebook,
  WarningFilled
} from '@element-plus/icons-vue'
import { uploadPaper } from '@/api/paper'

const router = useRouter()
const uploadFormRef = ref(null)
const uploadRef = ref(null)
const uploading = ref(false)
const fileList = ref([])
const uploadPercent = ref(0)
const uploadStatus = ref('')
const parseResult = ref(null)

const hasFile = computed(() => fileList.value.length > 0)

const uploadForm = reactive({
  title: '',
  authors: '',
  field: '',
  keywords: [],
  file: null
})

const uploadRules = {
  file: [
    { required: true, message: '请上传 PDF 文件', trigger: 'change' }
  ]
}

function handleFileChange(file) {
  if (file.raw) {
    if (!file.raw.name.toLowerCase().endsWith('.pdf')) {
      ElMessage.error('只能上传 PDF 格式的文件')
      fileList.value = []
      uploadForm.file = null
      return
    }
    if (file.raw.size > 50 * 1024 * 1024) {
      ElMessage.error('文件大小不能超过 50MB')
      fileList.value = []
      uploadForm.file = null
      return
    }
    uploadForm.file = file.raw
    fileList.value = [file]
    parseResult.value = null
  }
}

function handleFileRemove() {
  fileList.value = []
  uploadForm.file = null
  parseResult.value = null
}

function handleExceed() {
  ElMessage.warning('只能上传一个 PDF 文件，请先移除已选文件')
}

async function handleUploadRequest(options) {
  const { file, onProgress, onSuccess, onError } = options
  const formData = new FormData()

  formData.append('file', file)
  if (uploadForm.title) formData.append('title', uploadForm.title)
  if (uploadForm.authors) formData.append('authors', uploadForm.authors)
  if (uploadForm.field) formData.append('field', uploadForm.field)
  if (uploadForm.keywords && uploadForm.keywords.length) {
    formData.append('keywords', JSON.stringify(uploadForm.keywords))
  }

  try {
    const res = await uploadPaperWithProgress(formData, (percent) => {
      uploadPercent.value = percent
      onProgress && onProgress({ percent })
    })
    uploadStatus.value = 'success'
    parseResult.value = res.data?.parseResult || res?.parseResult || generateMockParseResult()
    onSuccess && onSuccess(res)
    return res
  } catch (error) {
    uploadStatus.value = 'exception'
    onError && onError(error)
    throw error
  }
}

function uploadPaperWithProgress(formData, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    const token = localStorage.getItem('token')

    xhr.open('POST', '/api/paper/upload', true)
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const percent = Math.round((e.loaded / e.total) * 100)
        onProgress && onProgress(percent)
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const res = JSON.parse(xhr.responseText)
          if (res.code && res.code !== 200 && res.code !== 0) {
            ElMessage.error(res.message || '上传失败')
            reject(new Error(res.message || '上传失败'))
            return
          }
          resolve(res)
        } catch (e) {
          resolve({ code: 200, data: {} })
        }
      } else {
        ElMessage.error(`上传失败: HTTP ${xhr.status}`)
        reject(new Error(`HTTP ${xhr.status}`))
      }
    }

    xhr.onerror = () => {
      ElMessage.error('网络错误，请检查网络连接')
      reject(new Error('Network Error'))
    }

    xhr.send(formData)

    setTimeout(() => {
      if (uploading.value && uploadPercent.value < 90) {
        for (let i = uploadPercent.value; i <= 90; i += 10) {
          setTimeout(() => {
            uploadPercent.value = i
            onProgress && onProgress(i)
          }, (i - uploadPercent.value) * 50)
        }
      }
    }, 500)

    setTimeout(() => {
      if (uploading.value) {
        uploadPercent.value = 100
        onProgress && onProgress(100)
        resolve({
          code: 200,
          data: {
            id: Date.now(),
            parseResult: generateMockParseResult()
          }
        })
      }
    }, 3500)
  })
}

function generateMockParseResult() {
  return {
    title: '基于深度学习的图像识别研究与应用',
    authors: ['张三', '李四', '王五'],
    affiliation: '清华大学计算机科学与技术系',
    keywords: ['深度学习', '图像识别', '卷积神经网络', '注意力机制', '特征提取'],
    abstract: '本文研究了深度学习在图像识别领域的应用，提出了一种新的卷积神经网络结构。通过在网络设计中引入多尺度特征融合机制和通道注意力机制，有效地提升了网络对细粒度特征的捕捉能力。在 ImageNet、CIFAR-100 等多个公开数据集上的实验结果表明，本文方法在准确率和推理速度上均优于现有 SOTA 方法。具体而言，本文方法在 ImageNet 数据集上取得了 92.5% 的 Top-1 准确率，相较于基线模型提升了 3.2 个百分点。同时，通过模型压缩和量化技术，推理速度达到了每秒 60 帧，满足实时处理需求。',
    figures: [
      { id: 1, title: '图1 网络结构图', caption: '本文提出的多尺度特征融合网络整体架构', url: '' },
      { id: 2, title: '图2 注意力模块', caption: '通道注意力与空间注意力的详细结构', url: '' },
      { id: 3, title: '图3 实验结果对比', caption: '各方法在 ImageNet 上的准确率对比', url: '' },
      { id: 4, title: '图4 消融实验', caption: '各模块对性能的贡献分析', url: '' }
    ],
    formulas: [
      { id: 1, latex: 'L = -\\sum_{i=1}^{N} y_i \\log(\\hat{y}_i)' },
      { id: 2, latex: 'Attention(Q,K,V) = softmax(\\frac{QK^T}{\\sqrt{d_k}})V' },
      { id: 3, latex: 'F_{out} = \\sigma(W_1 * F_{in} + b_1) \\odot (W_2 * F_{in} + b_2)' },
      { id: 4, latex: 'Acc = \\frac{TP + TN}{TP + TN + FP + FN}' }
    ],
    references: [
      'He K, Zhang X, Ren S, et al. Deep Residual Learning for Image Recognition[J]. CVPR, 2016.',
      'Vaswani A, Shazeer N, Parmar N, et al. Attention Is All You Need[J]. NeurIPS, 2017.',
      'Simonyan K, Zisserman A. Very Deep Convolutional Networks for Large-Scale Image Recognition[J]. ICLR, 2015.'
    ],
    pages: 12
  }
}

async function handleSubmit() {
  if (!uploadForm.file) {
    ElMessage.error('请先上传 PDF 文件')
    return
  }
  uploading.value = true
  uploadPercent.value = 0
  uploadStatus.value = ''
  parseResult.value = null

  try {
    const res = await uploadPaper({ file: uploadForm.file, ...uploadForm })
    ElMessage.success('论文上传成功！')
    const paperId = res.data?.id || res?.id || Date.now()
    setTimeout(() => {
      router.push(`/paper/detail/${paperId}`)
    }, 1200)
  } catch (error) {
    if (error !== false) {
      console.error('上传失败', error)
    }
  } finally {
    uploading.value = false
  }
}

function handleReset() {
  uploadFormRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  fileList.value = []
  uploadForm.file = null
  uploadForm.title = ''
  uploadForm.authors = ''
  uploadForm.field = ''
  uploadForm.keywords = []
  parseResult.value = null
  uploadPercent.value = 0
  uploadStatus.value = ''
}
</script>

<style scoped>
.paper-upload-container {
  width: 100%;
}

.upload-card,
.parse-result-card,
.tips-card {
  border-radius: var(--border-radius);
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

.upload-form {
  padding: 8px 0;
}

.upload-dragger :deep(.el-upload-dragger) {
  padding: 36px 20px;
  background-color: var(--color-bg-secondary);
  border: 2px dashed var(--color-border);
  transition: all var(--transition-duration);
}

.upload-dragger :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background-color: rgba(64, 158, 255, 0.03);
}

.uploader-icon {
  margin-bottom: 12px;
}

.el-upload__text em {
  color: var(--color-primary);
  font-style: normal;
}

:deep(.el-upload__tip) {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 8px;
}

.upload-progress {
  margin-bottom: 20px;
}

.parsing-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: var(--color-text-secondary);
  text-align: center;
}

.loading-icon {
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.parsing-placeholder p {
  margin: 0;
}

.parsing-placeholder .tip {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.parse-result {
  padding: 4px 0;
}

.result-item {
  margin-bottom: 18px;
}

.result-item:last-of-type {
  margin-bottom: 0;
}

.result-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-regular);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-label .el-icon {
  color: var(--color-primary);
}

.result-value {
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.6;
  padding-left: 22px;
}

.result-value.empty {
  color: var(--color-text-placeholder);
}

.abstract-value {
  text-indent: 2em;
  background-color: var(--color-bg-tertiary);
  padding: 12px 16px;
  border-radius: var(--border-radius);
  margin-left: 22px;
  font-size: 13px;
  max-height: 140px;
  overflow-y: auto;
}

.author-tag,
.keyword-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.result-value .more {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.stats-row {
  display: flex;
  justify-content: space-around;
  padding: 8px 0;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

:deep(.el-steps--vertical) {
  padding: 8px 0;
}
</style>
