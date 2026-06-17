<template>
  <header class="header-container">
    <div class="header-left">
      <el-button
        type="text"
        :icon="Fold"
        class="collapse-btn"
        @click="toggleCollapse"
      />
      <h2 class="header-title">学术论文评审系统</h2>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userStore.userInfo?.avatar">
            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
          <el-tag v-if="userStore.role" :type="tagType" size="small" class="role-tag">
            {{ roleText }}
          </el-tag>
          <el-icon class="arrow-down"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Fold, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import mitt from 'mitt'

const emitter = mitt()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const roleText = computed(() => {
  const map = {
    admin: '管理员',
    author: '作者',
    reviewer: '评审人'
  }
  return map[userStore.role] || userStore.role
})

const tagType = computed(() => {
  const map = {
    admin: 'danger',
    author: 'primary',
    reviewer: 'success'
  }
  return map[userStore.role] || 'info'
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  emitter.emit('toggle-collapse', isCollapsed.value)
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      router.push('/login')
    } catch (e) {
      if (e !== 'cancel') {
        console.error(e)
      }
    }
  }
}
</script>

<style scoped>
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background-color: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  color: var(--color-text-regular);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--border-radius);
  transition: background-color var(--transition-duration);
}

.user-info:hover {
  background-color: var(--color-bg-tertiary);
}

.username {
  font-size: 14px;
  color: var(--color-text-regular);
}

.role-tag {
  margin-left: 0;
}

.arrow-down {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
