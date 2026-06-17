<template>
  <div class="core-ref-container">
    <div v-if="!references?.length" class="empty-state">
      <el-empty description="暂无核心参考文献" :image-size="60" />
    </div>
    <div v-else class="ref-list">
      <div
        v-for="(ref, idx) in sortedRefs"
        :key="idx"
        class="ref-item"
        :class="{ 'top-three': idx < 3 }"
      >
        <div class="rank-badge" :class="`rank-${idx + 1}`">
          <span v-if="idx === 0">🥇</span>
          <span v-else-if="idx === 1">🥈</span>
          <span v-else-if="idx === 2">🥉</span>
          <span v-else>{{ idx + 1 }}</span>
        </div>
        <div class="ref-content">
          <div class="ref-title">{{ ref.title || '未知文献' }}</div>
          <div class="ref-meta">
            <span v-if="ref.authors" class="meta-item">
              <el-icon><User /></el-icon> {{ ref.authors }}
            </span>
            <span v-if="ref.year" class="meta-item">
              <el-icon><Calendar /></el-icon> {{ ref.year }}
            </span>
            <span v-if="ref.venue" class="meta-item">
              <el-icon><Reading /></el-icon> {{ ref.venue }}
            </span>
          </div>
          <div class="ref-score">
            <span class="score-label">PageRank:</span>
            <el-progress
              :percentage="Math.min(Math.round((ref.pagerank || 0) * 1000), 100)"
              :stroke-width="6"
              :show-text="false"
              style="width: 160px; display: inline-block; vertical-align: middle; margin: 0 10px"
            />
            <span class="score-value">{{ (ref.pagerank || 0).toFixed(4) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { User, Calendar, Reading } from '@element-plus/icons-vue'

const props = defineProps({
  references: {
    type: Array,
    default: () => []
  }
})

const sortedRefs = computed(() => {
  return [...(props.references || [])]
    .sort((a, b) => (b.pagerank || 0) - (a.pagerank || 0))
    .slice(0, 10)
})
</script>

<style scoped>
.core-ref-container { width: 100%; }

.ref-list { display: flex; flex-direction: column; gap: 10px; }

.ref-item {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  background: var(--color-bg-primary);
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
  transition: all 0.2s;
}
.ref-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(64,158,255,0.1);
  transform: translateX(2px);
}
.ref-item.top-three {
  background: linear-gradient(135deg, rgba(64,158,255,0.05), rgba(103,194,58,0.05));
  border-color: rgba(64,158,255,0.25);
}

.rank-badge {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  background: var(--color-bg-secondary);
  color: var(--color-text-regular);
  flex-shrink: 0;
}
.rank-badge.rank-1 { background: linear-gradient(135deg, #ffd700, #ffb800); color: #fff; }
.rank-badge.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a8a8a8); color: #fff; }
.rank-badge.rank-3 { background: linear-gradient(135deg, #cd7f32, #b87333); color: #fff; }

.ref-content { flex: 1; min-width: 0; }

.ref-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ref-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--color-text-regular);
}
.meta-item { display: inline-flex; align-items: center; gap: 4px; }

.ref-score {
  display: flex;
  align-items: center;
  font-size: 12px;
}
.score-label { color: var(--color-text-placeholder); margin-right: 4px; }
.score-value { font-weight: 700; color: var(--color-primary); font-family: Consolas, monospace; }

.empty-state { padding: 20px 0; }
</style>
