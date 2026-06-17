<template>
  <el-card class="paper-card" shadow="hover" @click="handleClick">
    <div class="card-content">
      <div class="card-header">
        <h3 class="paper-title">{{ paper.title }}</h3>
        <el-tag :type="statusType" size="small" effect="light">
          {{ statusText }}
        </el-tag>
      </div>

      <div class="paper-meta">
        <span v-if="paper.authors" class="meta-item">
          <el-icon><User /></el-icon>
          {{ paper.authors }}
        </span>
        <span v-if="paper.submissionDate" class="meta-item">
          <el-icon><Calendar /></el-icon>
          {{ formatDate(paper.submissionDate) }}
        </span>
      </div>

      <p v-if="paper.abstract" class="paper-abstract">
        {{ paper.abstract.length > 120 ? paper.abstract.substring(0, 120) + '...' : paper.abstract }}
      </p>

      <div v-if="paper.keywords && paper.keywords.length" class="paper-keywords">
        <el-tag
          v-for="(keyword, index) in (paper.keywords.length > 5 ? paper.keywords.slice(0, 5) : paper.keywords)"
          :key="index"
          size="small"
          type="info"
          effect="plain"
        >
          {{ keyword }}
        </el-tag>
        <span v-if="paper.keywords.length > 5" class="more-keywords">
          +{{ paper.keywords.length - 5 }}
        </span>
      </div>

      <div v-if="coreTopics && coreTopics.length" class="paper-core-topics">
        <span class="core-label">核心主题：</span>
        <el-tag
          v-for="(topic, index) in coreTopics.slice(0, 3)"
          :key="topic.name || index"
          size="small"
          type="warning"
          effect="light"
          class="core-topic-tag"
        >
          <el-icon><Star /></el-icon>
          {{ topic.name }}
        </el-tag>
      </div>

      <div class="card-actions" @click.stop>
        <el-button
          type="primary"
          size="small"
          :icon="View"
          @click="$router.push(`/paper/detail/${paper.id}`)"
        >
          查看详情
        </el-button>
        <slot name="actions" :paper="paper"></slot>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Calendar, View, Star } from '@element-plus/icons-vue'

const props = defineProps({
  paper: {
    type: Object,
    required: true,
    default: () => ({})
  }
})

const router = useRouter()

const coreTopics = computed(() => {
  if (props.paper.coreTopics && Array.isArray(props.paper.coreTopics)) {
    return props.paper.coreTopics
  }
  if (props.paper.topics && Array.isArray(props.paper.topics)) {
    return props.paper.topics.filter(t => t.isCore).slice(0, 3)
  }
  return []
})

const statusText = computed(() => {
  const map = {
    pending: '待评审',
    reviewing: '评审中',
    accepted: '已录用',
    rejected: '已拒稿',
    published: '已发表'
  }
  return map[props.paper.status] || props.paper.status || '未知状态'
})

const statusType = computed(() => {
  const map = {
    pending: 'warning',
    reviewing: 'primary',
    accepted: 'success',
    rejected: 'danger',
    published: 'success'
  }
  return map[props.paper.status] || 'info'
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN')
  } catch (e) {
    return dateStr
  }
}

function handleClick() {
  if (props.paper.id) {
    router.push(`/paper/detail/${props.paper.id}`)
  }
}
</script>

<style scoped>
.paper-card {
  cursor: pointer;
  transition: transform var(--transition-duration);
  height: 100%;
}

.paper-card:hover {
  transform: translateY(-2px);
}

:deep(.el-card__body) {
  padding: 20px;
  height: 100%;
}

.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
  line-height: 1.4;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.paper-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-item .el-icon {
  font-size: 14px;
}

.paper-abstract {
  font-size: 13px;
  color: var(--color-text-regular);
  line-height: 1.6;
  margin: 0 0 16px 0;
  flex: 1;
}

.paper-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.more-keywords {
  font-size: 12px;
  color: var(--color-text-placeholder);
  align-self: center;
}

.paper-core-topics {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.core-label {
  font-size: 12px;
  color: var(--color-text-regular);
  font-weight: 500;
}

.core-topic-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.core-topic-tag .el-icon {
  font-size: 10px;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
}
</style>
