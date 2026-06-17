<template>
  <aside class="sidebar-container" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-logo">
      <el-icon :size="28" color="#409eff"><Reading /></el-icon>
      <span v-if="!isCollapsed" class="logo-text">论文评审系统</span>
    </div>
    <el-scrollbar class="sidebar-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#b0b8c4"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-sub-menu v-if="showPaperMenu" index="paper">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>论文管理</span>
          </template>
          <el-menu-item v-if="userStore.isAuthor || userStore.isAdmin" index="/paper/upload">
            <el-icon><Upload /></el-icon>
            <template #title>论文上传</template>
          </el-menu-item>
          <el-menu-item index="/paper/list">
            <el-icon><Notebook /></el-icon>
            <template #title>论文列表</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="showReviewMenu" index="review">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>评审管理</span>
          </template>
          <el-menu-item index="/review/pending">
            <el-icon><Clock /></el-icon>
            <template #title>待评审论文</template>
          </el-menu-item>
          <el-menu-item index="/review/record">
            <el-icon><Tickets /></el-icon>
            <template #title>评审记录</template>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Reading,
  DataBoard,
  Document,
  Upload,
  Notebook,
  EditPen,
  Clock,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import mitt from 'mitt'

const emitter = mitt()
const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const showPaperMenu = computed(() => {
  return userStore.isAuthor || userStore.isAdmin || userStore.isReviewer
})

const showReviewMenu = computed(() => {
  return userStore.isReviewer || userStore.isAdmin
})

function handleToggleCollapse(collapsed) {
  isCollapsed.value = collapsed
}

onMounted(() => {
  emitter.on('toggle-collapse', handleToggleCollapse)
})

onUnmounted(() => {
  emitter.off('toggle-collapse', handleToggleCollapse)
})
</script>

<style scoped>
.sidebar-container {
  width: 220px;
  height: 100vh;
  background-color: #001529;
  transition: width var(--transition-duration);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  left: 0;
}

.sidebar-container.collapsed {
  width: 64px;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-scrollbar {
  flex: 1;
  overflow: hidden;
}

.sidebar-scrollbar :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--color-primary) !important;
  color: #ffffff !important;
}
</style>
