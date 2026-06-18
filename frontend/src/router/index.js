import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUserInfo } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true, title: '仪表盘' }
  },
  {
    path: '/paper/upload',
    name: 'PaperUpload',
    component: () => import('@/views/PaperUpload.vue'),
    meta: { requiresAuth: true, roles: ['author', 'admin'], title: '论文上传' }
  },
  {
    path: '/paper/list',
    name: 'PaperList',
    component: () => import('@/views/PaperList.vue'),
    meta: { requiresAuth: true, title: '论文列表' }
  },
  {
    path: '/paper/detail/:id',
    name: 'PaperDetail',
    component: () => import('@/views/PaperDetail.vue'),
    meta: { requiresAuth: true, title: '论文详情' }
  },
  {
    path: '/review/pending',
    name: 'ReviewPending',
    component: () => import('@/views/ReviewPending.vue'),
    meta: { requiresAuth: true, roles: ['reviewer', 'admin'], title: '待评审论文' }
  },
  {
    path: '/review/review/:paperId',
    name: 'ReviewPage',
    component: () => import('@/views/ReviewPage.vue'),
    meta: { requiresAuth: true, roles: ['reviewer', 'admin'], title: '论文评审' }
  },
  {
    path: '/review/record',
    name: 'ReviewRecord',
    component: () => import('@/views/ReviewRecord.vue'),
    meta: { requiresAuth: true, title: '评审记录' }
  },
  {
    path: '/paper/:id/duplicate-check',
    name: 'DuplicateCheck',
    component: () => import('@/views/DuplicateCheck.vue'),
    meta: { requiresAuth: true, roles: ['author', 'admin'], title: '学术不端检测' }
  },
  {
    path: '/blind-review/config',
    name: 'BlindReviewConfig',
    component: () => import('@/views/BlindReviewConfig.vue'),
    meta: { requiresAuth: true, roles: ['admin'], title: '双盲评审设置' }
  },
  {
    path: '/review/:reviewRecordId/reply',
    name: 'ReviewReply',
    component: () => import('@/views/ReviewReply.vue'),
    meta: { requiresAuth: true, title: '评审回复' }
  },
  {
    path: '/quality/dashboard',
    name: 'QualityDashboard',
    component: () => import('@/views/QualityDashboard.vue'),
    meta: { requiresAuth: true, roles: ['admin', 'reviewer'], title: '评审质量监控' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 学术论文评审系统` : '学术论文评审系统'

  const token = getToken()

  if (to.meta.public) {
    if (token && (to.path === '/login' || to.path === '/register')) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      ElMessage.warning('请先登录')
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      if (to.meta.roles && to.meta.roles.length > 0) {
        const userInfo = getUserInfo()
        const userRole = userInfo?.role || ''
        if (to.meta.roles.includes(userRole)) {
          next()
        } else {
          ElMessage.error('没有权限访问该页面')
          next('/dashboard')
        }
      } else {
        next()
      }
    }
  }
})

export default router
