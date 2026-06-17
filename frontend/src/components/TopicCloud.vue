<template>
  <div class="topic-cloud-container">
    <div class="topic-tags" v-if="sortedTopics.length">
      <span
        v-for="(topic, idx) in sortedTopics"
        :key="topic.name || idx"
        class="topic-tag"
        :class="{ 'is-core': topic.isCore, [`source-${topic.source || 'combined'}`]: true }"
        :style="getTagStyle(topic)"
        @mouseenter="hoverTopic = topic"
        @mouseleave="hoverTopic = null"
      >
        <el-icon v-if="topic.isCore" class="star-icon"><Star /></el-icon>
        <span class="topic-name">{{ topic.name }}</span>
      </span>
    </div>
    <el-empty v-else description="暂无主题数据" :image-size="60" />

    <div v-if="hoverTopic" class="topic-tooltip" :style="tooltipStyle">
      <div class="tooltip-name">{{ hoverTopic.name }}</div>
      <div class="tooltip-info">
        <span class="info-label">权重：</span>
        <span class="info-value">{{ (hoverTopic.weight * 100).toFixed(1) }}%</span>
      </div>
      <div class="tooltip-info">
        <span class="info-label">来源：</span>
        <span class="info-value">{{ getSourceText(hoverTopic.source) }}</span>
      </div>
      <div v-if="hoverTopic.isCore" class="tooltip-core">
        <el-icon><Star /></el-icon>
        核心主题
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Star } from '@element-plus/icons-vue'

const props = defineProps({
  topics: {
    type: Array,
    default: () => []
  },
  maxTags: {
    type: Number,
    default: 30
  }
})

const hoverTopic = ref(null)

const sortedTopics = computed(() => {
  const list = [...props.topics].sort((a, b) => b.weight - a.weight)
  return list.slice(0, props.maxTags)
})

const minWeight = computed(() => {
  if (!sortedTopics.value.length) return 0
  return Math.min(...sortedTopics.value.map(t => t.weight))
})

const maxWeight = computed(() => {
  if (!sortedTopics.value.length) return 1
  return Math.max(...sortedTopics.value.map(t => t.weight))
})

const tooltipStyle = computed(() => ({}))

function getTagStyle(topic) {
  const weightRange = maxWeight.value - minWeight.value || 1
  const normalized = (topic.weight - minWeight.value) / weightRange
  const minSize = 12
  const maxSize = 20
  const fontSize = topic.isCore 
    ? maxSize + 2 
    : minSize + normalized * (maxSize - minSize)
  
  return {
    fontSize: `${fontSize}px`
  }
}

function getSourceText(source) {
  const map = {
    bertopic: 'BERTopic',
    pagerank: 'PageRank',
    combined: 'Combined'
  }
  return map[source] || source || 'Combined'
}
</script>

<style scoped>
.topic-cloud-container {
  position: relative;
  width: 100%;
}

.topic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.topic-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 500;
  cursor: default;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.topic-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.topic-tag.is-core {
  font-weight: 700;
  padding: 8px 16px;
}

.topic-tag.source-bertopic {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15), rgba(102, 126, 234, 0.15));
  color: #409eff;
  border-color: rgba(64, 158, 255, 0.3);
}

.topic-tag.source-pagerank {
  background: linear-gradient(135deg, rgba(155, 89, 182, 0.15), rgba(118, 75, 162, 0.15));
  color: #9b59b6;
  border-color: rgba(155, 89, 182, 0.3);
}

.topic-tag.source-combined {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2), rgba(255, 152, 0, 0.2));
  color: #f57c00;
  border-color: rgba(255, 152, 0, 0.4);
}

.topic-tag.is-core.source-bertopic {
  background: linear-gradient(135deg, #409eff, #667eea);
  color: #fff;
  border-color: transparent;
}

.topic-tag.is-core.source-pagerank {
  background: linear-gradient(135deg, #9b59b6, #764ba2);
  color: #fff;
  border-color: transparent;
}

.topic-tag.is-core.source-combined {
  background: linear-gradient(135deg, #ffc107, #ff9800);
  color: #fff;
  border-color: transparent;
}

.star-icon {
  font-size: 14px;
}

.topic-tooltip {
  position: absolute;
  z-index: 100;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--color-border-light);
  min-width: 140px;
}

.tooltip-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.tooltip-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
}

.info-label {
  color: var(--color-text-secondary);
}

.info-value {
  color: var(--color-text-regular);
  font-weight: 500;
}

.tooltip-core {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid var(--color-border-light);
  font-size: 12px;
  color: #f57c00;
  font-weight: 600;
}
</style>
