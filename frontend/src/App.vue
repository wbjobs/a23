<template>
  <div class="app-container">
    <template v-if="userStore.token">
      <div class="layout-wrapper">
        <Sidebar class="layout-sidebar" />
        <div class="layout-main">
          <Header class="layout-header" />
          <main class="layout-content">
            <router-view />
          </main>
        </div>
      </div>
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import Header from '@/components/Header.vue'
import Sidebar from '@/components/Sidebar.vue'

const userStore = useUserStore()

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token && !userStore.token) {
    userStore.token = token
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        userStore.userInfo = JSON.parse(userInfo)
        userStore.role = JSON.parse(userInfo).role || ''
      } catch (e) {
        console.error('解析用户信息失败', e)
      }
    }
  }
})
</script>

<style scoped>
.app-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--color-bg-secondary);
}

.layout-wrapper {
  display: flex;
  min-height: 100vh;
}

.layout-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  height: 60px;
  flex-shrink: 0;
}

.layout-content {
  flex: 1;
  padding: 24px;
  overflow: auto;
}
</style>
