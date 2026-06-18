<template>
  <div class="review-reply-container" v-loading="loading">
    <div class="header-bar">
      <div class="paper-info">
        <el-icon :size="20" color="#409eff"><Document /></el-icon>
        <span class="paper-title">{{ paperInfo.title }}</span>
      </div>
      <div class="review-info">
        <span class="reviewer-label">评审人：</span>
        <el-tag type="primary" effect="dark">{{ reviewInfo.reviewerName }}</el-tag>
        <span class="review-time">评审时间：{{ reviewInfo.reviewTime }}</span>
        <el-tag :type="getOverallScoreType(reviewInfo.overallScore)" effect="light">
          总评分：{{ reviewInfo.overallScore }}分
        </el-tag>
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <div class="panel-header">
          <el-icon><EditPen /></el-icon>
          <span>评审意见</span>
          <el-tag type="info" size="small">{{ reviewThreads.length }}条</el-tag>
        </div>
        <div class="thread-list">
          <div
            v-for="thread in reviewThreads"
            :key="thread.id"
            class="thread-item"
            :class="{ active: activeThreadId === thread.id, resolved: thread.status === 'resolved' }"
            @click="selectThread(thread)"
          >
            <div class="thread-header">
              <span class="thread-number">{{ thread.threadNumber }}</span>
              <el-tag
                :type="getThreadStatusType(thread.status)"
                size="small"
                effect="light"
                class="thread-status"
              >
                {{ getThreadStatusText(thread.status) }}
              </el-tag>
            </div>
            <div class="thread-content">{{ thread.content }}</div>
            <div class="thread-footer">
              <span class="reply-count">
                <el-icon><ChatDotRound /></el-icon>
                {{ thread.replyCount }}条回复
              </span>
              <el-button
                v-if="thread.status !== 'resolved'"
                type="primary"
                size="small"
                link
                class="reply-btn"
                @click.stop="selectThread(thread)"
              >
                回复
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div v-if="activeThread" class="chat-container">
          <div class="chat-header">
            <div class="current-opinion">
              <el-icon><InfoFilled /></el-icon>
              <div class="opinion-content">
                <span class="opinion-label">评审意见 #{{ activeThread.threadNumber }}：</span>
                <span class="opinion-text">{{ activeThread.content }}</span>
              </div>
            </div>
            <el-button
              v-if="activeThread.status !== 'resolved'"
              type="success"
              :icon="CircleCheck"
              @click="handleResolve"
            >
              标记已解决
            </el-button>
          </div>

          <div class="messages-container" ref="messagesContainer">
            <div
              v-for="(message, index) in messages"
              :key="message.id"
              class="message-wrapper"
              :class="message.isAuthor ? 'author' : 'reviewer'"
            >
              <div class="message-avatar">
                <el-avatar :size="40" :icon="message.isAuthor ? User : UserFilled" />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="sender-name">{{ message.senderName }}</span>
                  <span class="send-time">{{ message.sendTime }}</span>
                </div>
                <div class="message-bubble" @mouseenter="message.showActions = true" @mouseleave="message.showActions = false">
                  <div class="bubble-text" v-html="message.content"></div>
                  <div v-if="message.suggestedRefs && message.suggestedRefs.length > 0" class="suggested-refs">
                    <div class="refs-label">
                      <el-icon><LightBulb /></el-icon>
                      系统推荐参考文献：
                    </div>
                    <div
                      v-for="ref in message.suggestedRefs"
                      :key="ref.id"
                      class="ref-card"
                      @click.stop="viewRefDetail(ref)"
                    >
                      <div class="ref-title">{{ ref.title }}</div>
                      <div class="ref-meta">
                        <span>{{ ref.authors }}</span>
                        <span>·</span>
                        <span>{{ ref.year }}</span>
                        <el-tag type="primary" size="small" effect="plain">相似度 {{ ref.similarity }}%</el-tag>
                      </div>
                    </div>
                  </div>
                  <div v-if="message.showActions" class="message-actions">
                    <el-button type="primary" size="small" link @click="handleQuote(message)">
                      <el-icon><CopyDocument /></el-icon>
                      引用
                    </el-button>
                    <el-button type="info" size="small" link @click="viewMessageDetail(message)">
                      <el-icon><View /></el-icon>
                      查看完整文献
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="input-area">
            <div class="toolbar">
              <el-button-group>
                <el-button :icon="Bold" @click="insertFormat('bold')" title="加粗">B</el-button>
                <el-button :icon="Italic" @click="insertFormat('italic')" title="斜体">I</el-button>
                <el-button :icon="Quote" @click="insertFormat('quote')" title="引用">
                  <el-icon><Quote /></el-icon>
                </el-button>
                <el-button :icon="Link" @click="insertCitation" title="插入文献">
                  <el-icon><Link /></el-icon>
                </el-button>
              </el-button-group>
            </div>
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="4"
              placeholder="请输入回复内容..."
              class="reply-input"
            />
            <el-collapse class="ai-suggest-collapse">
              <el-collapse-item name="refs">
                <template #title>
                  <span class="collapse-title">
                    <el-icon><MagicStick /></el-icon>
                    AI推荐参考文献
                    <el-tag type="success" size="small" effect="plain">{{ suggestedRefs.length }}条推荐</el-tag>
                  </span>
                </template>
                <div class="ai-ref-list">
                  <div
                    v-for="ref in suggestedRefs"
                    :key="ref.id"
                    class="ai-ref-item"
                  >
                    <div class="ai-ref-content">
                      <div class="ai-ref-title">{{ ref.title }}</div>
                      <div class="ai-ref-meta">
                        <span>{{ ref.authors }}</span>
                        <span>·</span>
                        <span>{{ ref.year }}</span>
                        <span>·</span>
                        <span>{{ ref.source }}</span>
                      </div>
                    </div>
                    <div class="ai-ref-actions">
                      <el-tag type="primary" size="small" effect="plain">{{ ref.similarity }}%</el-tag>
                      <el-button type="primary" size="small" @click="insertRef(ref)">
                        插入引用
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
            <div class="input-actions">
              <el-button type="primary" :icon="Promotion" @click="sendReply" :disabled="!replyContent.trim()">
                发送回复
              </el-button>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty description="请选择左侧的评审意见进行回复">
            <template #image>
              <el-icon :size="64" color="#c0c4cc"><ChatDotRound /></el-icon>
            </template>
          </el-empty>
        </div>
      </div>
    </div>

    <el-dialog v-model="refDetailVisible" title="参考文献详情" width="600px">
      <div v-if="selectedRef">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">
            {{ selectedRef.title }}
          </el-descriptions-item>
          <el-descriptions-item label="作者">
            {{ selectedRef.authors }}
          </el-descriptions-item>
          <el-descriptions-item label="年份">
            {{ selectedRef.year }}
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            {{ selectedRef.source }}
          </el-descriptions-item>
          <el-descriptions-item label="相似度">
            <el-tag type="primary" effect="dark">{{ selectedRef.similarity }}%</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="摘要">
            {{ selectedRef.abstract }}
          </el-descriptions-item>
          <el-descriptions-item label="引用格式">
            <code>{{ selectedRef.citation }}</code>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="insertRef(selectedRef)">插入引用</el-button>
        <el-button type="primary" @click="refDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  EditPen,
  ChatDotRound,
  InfoFilled,
  CircleCheck,
  User,
  UserFilled,
  CopyDocument,
  View,
  Bold,
  Italic,
  Quote,
  Link,
  MagicStick,
  Promotion,
  LightBulb
} from '@element-plus/icons-vue'
import {
  submitReply,
  getReplyThreads,
  getThreadReplies,
  getSuggestedRefs,
  resolveReply
} from '@/api/reply'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const activeThreadId = ref(null)
const replyContent = ref('')
const refDetailVisible = ref(false)
const selectedRef = ref(null)
const messagesContainer = ref(null)

const paperInfo = reactive({
  id: route.params.reviewRecordId,
  title: '基于深度学习的图像识别研究与应用'
})

const reviewInfo = reactive({
  reviewerName: '评审专家1',
  reviewTime: '2026-06-16 10:30:00',
  overallScore: 72
})

const reviewThreads = ref([
  {
    id: 1,
    threadNumber: 1,
    content: '论文的创新点不够突出，建议在引言部分更清晰地说明本文相较于现有工作的主要贡献。',
    status: 'replied',
    replyCount: 3
  },
  {
    id: 2,
    threadNumber: 2,
    content: '实验部分缺少与最新SOTA方法的对比，建议补充相关实验结果。',
    status: 'pending',
    replyCount: 0
  },
  {
    id: 3,
    threadNumber: 3,
    content: '第3节的公式推导存在一处符号错误，请仔细核对。',
    status: 'resolved',
    replyCount: 2
  },
  {
    id: 4,
    threadNumber: 4,
    content: '参考文献格式不统一，部分文献缺少年份和页码信息。',
    status: 'pending',
    replyCount: 0
  },
  {
    id: 5,
    threadNumber: 5,
    content: '建议在结论部分增加对未来工作的展望，说明该研究方向可能的延伸。',
    status: 'replied',
    replyCount: 1
  }
])

const activeThread = ref(null)

const messages = ref([])

const suggestedRefs = ref([
  {
    id: 1,
    title: 'Deep Residual Learning for Image Recognition',
    authors: 'He K, Zhang X, Ren S, et al.',
    year: 2016,
    source: 'CVPR',
    similarity: 92,
    abstract: '本文提出了残差学习框架，用于解决深度网络训练中的退化问题...',
    citation: 'He K, Zhang X, Ren S, et al. Deep residual learning for image recognition[C]//Proceedings of the IEEE conference on computer vision and pattern recognition. 2016: 770-778.'
  },
  {
    id: 2,
    title: 'Very Deep Convolutional Networks for Large-Scale Image Recognition',
    authors: 'Simonyan K, Zisserman A.',
    year: 2014,
    source: 'arXiv preprint',
    similarity: 85,
    abstract: '本文研究了卷积网络深度对其在大规模图像识别任务精度的影响...',
    citation: 'Simonyan K, Zisserman A. Very deep convolutional networks for large-scale image recognition[J]. arXiv preprint arXiv:1409.1556, 2014.'
  },
  {
    id: 3,
    title: 'Squeeze-and-Excitation Networks',
    authors: 'Hu J, Shen L, Sun G.',
    year: 2018,
    source: 'CVPR',
    similarity: 78,
    abstract: '本文提出了一种新的架构单元"Squeeze-and-Excitation" (SE) 块...',
    citation: 'Hu J, Shen L, Sun G. Squeeze-and-excitation networks[C]//Proceedings of the IEEE conference on computer vision and pattern recognition. 2018: 7132-7141.'
  },
  {
    id: 4,
    title: 'Efficient Estimation of Word Representations in Vector Space',
    authors: 'Mikolov T, Chen K, Corrado G, et al.',
    year: 2013,
    source: 'arXiv preprint',
    similarity: 72,
    abstract: '本文提出了两种新颖的模型架构，用于计算词的连续向量表示...',
    citation: 'Mikolov T, Chen K, Corrado G, et al. Efficient estimation of word representations in vector space[J]. arXiv preprint arXiv:1301.3781, 2013.'
  },
  {
    id: 5,
    title: 'Attention Is All You Need',
    authors: 'Vaswani A, Shazeer N, Parmar N, et al.',
    year: 2017,
    source: 'NeurIPS',
    similarity: 68,
    abstract: '本文提出了一种新的简单网络架构，即Transformer，它完全基于注意力机制...',
    citation: 'Vaswani A, Shazeer N, Parmar N, et al. Attention is all you need[J]. Advances in neural information processing systems, 2017, 30.'
  }
])

function getOverallScoreType(score) {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function getThreadStatusType(status) {
  const map = { pending: 'warning', replied: 'primary', resolved: 'success' }
  return map[status] || 'info'
}

function getThreadStatusText(status) {
  const map = { pending: '待回复', replied: '已回复', resolved: '已解决' }
  return map[status] || '未知'
}

async function loadThreads() {
  try {
    const res = await getReplyThreads(route.params.reviewRecordId)
    const data = res.data || res
    const list = data.list || data
    if (list && list.length) {
      reviewThreads.value = list
    }
  } catch (e) {
    console.error('加载评审意见失败', e)
  }
}

async function loadThreadReplies(thread) {
  loading.value = true
  try {
    const res = await getThreadReplies(route.params.reviewRecordId, thread.threadNumber)
    const data = res.data || res
    const list = data.list || data
    if (list && list.length) {
      messages.value = list.map(m => ({ ...m, showActions: false }))
    } else {
      generateMockMessages(thread)
    }
  } catch (e) {
    console.error('加载回复失败', e)
    generateMockMessages(thread)
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function generateMockMessages(thread) {
  const mockMsgs = [
    {
      id: 1,
      isAuthor: false,
      senderName: '评审专家1',
      sendTime: '2026-06-16 10:30:00',
      content: thread.content,
      showActions: false
    }
  ]
  if (thread.replyCount > 0) {
    mockMsgs.push({
      id: 2,
      isAuthor: true,
      senderName: userStore.userInfo?.name || '作者',
      sendTime: '2026-06-16 14:20:00',
      content: '感谢专家的宝贵意见。我们已在修改稿中补充了相关说明，具体见引言第2段。',
      suggestedRefs: [
        {
          id: 101,
          title: 'Deep Residual Learning for Image Recognition',
          authors: 'He K, et al.',
          year: 2016,
          similarity: 88
        }
      ],
      showActions: false
    })
    mockMsgs.push({
      id: 3,
      isAuthor: false,
      senderName: '评审专家1',
      sendTime: '2026-06-16 16:45:00',
      content: '修改后的内容清晰多了。但建议在对比时更具体地说明本文方法在哪些方面优于文献[1]。',
      showActions: false
    })
  }
  messages.value = mockMsgs
}

function selectThread(thread) {
  activeThreadId.value = thread.id
  activeThread.value = thread
  loadThreadReplies(thread)
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function insertFormat(type) {
  const textarea = document.querySelector('.reply-input textarea')
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = replyContent.value.substring(start, end)
  let insertText = ''
  switch (type) {
    case 'bold':
      insertText = `<strong>${selectedText || '加粗文本'}</strong>`
      break
    case 'italic':
      insertText = `<em>${selectedText || '斜体文本'}</em>`
      break
    case 'quote':
      insertText = `<blockquote>${selectedText || '引用内容'}</blockquote>`
      break
  }
  replyContent.value = replyContent.value.substring(0, start) + insertText + replyContent.value.substring(end)
}

function insertCitation() {
  refDetailVisible.value = true
}

function insertRef(ref) {
  if (!ref) return
  const citation = `[${ref.id}] ${ref.authors}. ${ref.title}[${ref.source}]. ${ref.year}.`
  replyContent.value += `\n\n${citation}`
  refDetailVisible.value = false
  ElMessage.success('已插入引用')
}

function handleQuote(message) {
  replyContent.value = `<blockquote>${message.content}</blockquote>\n\n` + replyContent.value
}

function viewMessageDetail(message) {
  ElMessage.info('查看完整文献信息功能')
}

function viewRefDetail(ref) {
  selectedRef.value = ref
  refDetailVisible.value = true
}

async function sendReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  loading.value = true
  try {
    const newMessage = {
      id: Date.now(),
      isAuthor: true,
      senderName: userStore.userInfo?.name || '作者',
      sendTime: new Date().toLocaleString('zh-CN'),
      content: replyContent.value,
      suggestedRefs: suggestedRefs.value.slice(0, 2).map(r => ({
        id: r.id,
        title: r.title,
        authors: r.authors,
        year: r.year,
        similarity: r.similarity
      })),
      showActions: false
    }
    await submitReply({
      reviewRecordId: route.params.reviewRecordId,
      threadNumber: activeThread.value.threadNumber,
      content: replyContent.value
    })
    messages.value.push(newMessage)
    replyContent.value = ''
    ElMessage.success('回复发送成功')
    await nextTick()
    scrollToBottom()
    updateThreadStatus('replied')
  } catch (e) {
    console.error('发送失败', e)
    const newMessage = {
      id: Date.now(),
      isAuthor: true,
      senderName: userStore.userInfo?.name || '作者',
      sendTime: new Date().toLocaleString('zh-CN'),
      content: replyContent.value,
      suggestedRefs: suggestedRefs.value.slice(0, 2).map(r => ({
        id: r.id,
        title: r.title,
        authors: r.authors,
        year: r.year,
        similarity: r.similarity
      })),
      showActions: false
    }
    messages.value.push(newMessage)
    replyContent.value = ''
    ElMessage.success('回复发送成功（Mock）')
    await nextTick()
    scrollToBottom()
    updateThreadStatus('replied')
  } finally {
    loading.value = false
  }
}

async function handleResolve() {
  if (!activeThread.value) return
  try {
    await resolveReply(activeThread.value.id)
    ElMessage.success('已标记为已解决')
    updateThreadStatus('resolved')
  } catch (e) {
    console.error('标记失败', e)
    ElMessage.success('已标记为已解决（Mock）')
    updateThreadStatus('resolved')
  }
}

function updateThreadStatus(status) {
  const thread = reviewThreads.value.find(t => t.id === activeThreadId.value)
  if (thread) {
    thread.status = status
    if (status === 'replied') {
      thread.replyCount = (thread.replyCount || 0) + 1
    }
  }
}

onMounted(() => {
  loadThreads()
})
</script>

<style scoped>
.review-reply-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-bg-secondary);
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  flex-shrink: 0;
}

.paper-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
}

.paper-title {
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.reviewer-label {
  opacity: 0.9;
}

.review-time {
  opacity: 0.85;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel {
  width: 380px;
  background: #fff;
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border-light);
}

.panel-header .el-icon {
  color: var(--color-primary);
}

.thread-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.thread-item {
  padding: 16px;
  border: 1px solid var(--color-border-light);
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all var(--transition-duration);
}

.thread-item:hover {
  border-color: var(--color-primary-light);
  background-color: var(--color-bg-tertiary);
}

.thread-item.active {
  border-color: var(--color-primary);
  background-color: rgba(64, 158, 255, 0.05);
}

.thread-item.resolved {
  opacity: 0.7;
}

.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.thread-number {
  font-weight: 600;
  color: var(--color-primary);
}

.thread-status {
  flex-shrink: 0;
}

.thread-content {
  color: var(--color-text-regular);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.thread-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.reply-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reply-btn {
  padding: 0 !important;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 24px;
  background-color: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border-light);
  gap: 16px;
}

.current-opinion {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  flex: 1;
}

.current-opinion .el-icon {
  color: var(--color-primary);
  margin-top: 2px;
  flex-shrink: 0;
}

.opinion-content {
  flex: 1;
  min-width: 0;
}

.opinion-label {
  font-weight: 600;
  color: var(--color-text-primary);
}

.opinion-text {
  color: var(--color-text-regular);
  line-height: 1.6;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-wrapper.author {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message-wrapper.author .message-content {
  text-align: right;
}

.message-header {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.message-wrapper.author .message-header {
  text-align: right;
}

.sender-name {
  font-weight: 600;
  margin-right: 8px;
}

.message-bubble {
  position: relative;
  display: inline-block;
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  text-align: left;
}

.message-wrapper.reviewer .message-bubble {
  background-color: var(--color-bg-tertiary);
  border-top-left-radius: 4px;
}

.message-wrapper.author .message-bubble {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-text {
  line-height: 1.6;
  word-break: break-word;
}

.bubble-text :deep(blockquote) {
  border-left: 3px solid rgba(255, 255, 255, 0.3);
  padding-left: 12px;
  margin: 8px 0;
  opacity: 0.9;
}

.message-wrapper.reviewer .bubble-text :deep(blockquote) {
  border-left-color: var(--color-primary);
}

.suggested-refs {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.message-wrapper.reviewer .suggested-refs {
  border-top-color: var(--color-border-light);
}

.refs-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 8px;
  opacity: 0.9;
}

.refs-label .el-icon {
  color: #e6a23c;
}

.ref-card {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background var(--transition-duration);
}

.ref-card:hover {
  background: rgba(255, 255, 255, 0.25);
}

.message-wrapper.reviewer .ref-card {
  background: rgba(64, 158, 255, 0.1);
}

.message-wrapper.reviewer .ref-card:hover {
  background: rgba(64, 158, 255, 0.2);
}

.ref-title {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 4px;
}

.ref-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  opacity: 0.85;
  flex-wrap: wrap;
}

.message-actions {
  position: absolute;
  top: -10px;
  right: 10px;
  display: flex;
  gap: 4px;
  background: #fff;
  border-radius: 4px;
  box-shadow: var(--box-shadow);
  padding: 2px;
}

.message-wrapper.reviewer .message-actions {
  right: 10px;
}

.message-wrapper.author .message-actions {
  right: auto;
  left: 10px;
}

.message-actions .el-button {
  padding: 2px 8px !important;
  font-size: 12px !important;
}

.input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.toolbar {
  margin-bottom: 12px;
}

.toolbar :deep(.el-button-group .el-button) {
  padding: 8px 12px;
}

.reply-input :deep(textarea) {
  resize: none;
}

.ai-suggest-collapse {
  margin-top: 12px;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.collapse-title .el-icon {
  color: var(--color-primary);
}

.ai-ref-list {
  max-height: 300px;
  overflow-y: auto;
}

.ai-ref-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all var(--transition-duration);
}

.ai-ref-item:hover {
  border-color: var(--color-primary-light);
  background-color: var(--color-bg-tertiary);
}

.ai-ref-content {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.ai-ref-title {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-ref-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.ai-ref-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-descriptions code) {
  background: var(--color-bg-tertiary);
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  display: block;
  word-break: break-all;
}

@media (max-width: 1200px) {
  .left-panel {
    width: 320px;
  }
}

@media (max-width: 768px) {
  .header-bar {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .main-content {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
    height: 40vh;
    border-right: none;
    border-bottom: 1px solid var(--color-border-light);
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
