<template>
  <div class="profile-container">
    <h2 class="page-title">个人中心</h2>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card" shadow="hover">
          <div class="profile-header">
            <el-avatar :size="96" :src="userInfo?.avatar" class="avatar">
              {{ userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <h3 class="username">{{ userInfo?.username || '用户' }}</h3>
            <el-tag :type="roleTagType" size="large" effect="dark" class="role-tag">
              {{ roleText }}
            </el-tag>
            <p class="email" v-if="userInfo?.email">
              <el-icon><Message /></el-icon>
              {{ userInfo.email }}
            </p>
          </div>
          <el-divider />
          <el-descriptions :column="1" border class="profile-desc">
            <el-descriptions-item label="用户ID">
              {{ userInfo?.id || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="真实姓名">
              {{ userInfo?.name || profileForm?.name || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ formatDate(userInfo?.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ formatDate(userInfo?.lastLoginAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="研究领域">
              {{ userInfo?.field || profileForm?.field || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="所属机构">
              {{ userInfo?.institution || profileForm?.institution || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="isReviewer" class="reviewer-tags-section">
            <div class="section-title">
              <el-icon><CollectionTag /></el-icon> 研究方向标签
            </div>
            <div class="tags-wrap" v-if="profileForm.researchTags && profileForm.researchTags.length">
              <el-tag
                v-for="(tag, idx) in profileForm.researchTags"
                :key="idx"
                class="research-tag"
                effect="dark"
                size="small"
                :type="getTagType(idx)"
              >
                {{ tag }}
              </el-tag>
            </div>
            <el-empty v-else description="暂无研究方向标签，前往个人资料设置" :image-size="50" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="个人资料" name="info">
            <el-card shadow="hover">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="profileForm.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="真实姓名" prop="name">
                  <el-input v-model="profileForm.name" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="所属机构" prop="institution">
                  <el-input v-model="profileForm.institution" placeholder="请输入所属机构/大学" :prefix-icon="OfficeBuilding" />
                </el-form-item>
                <el-form-item label="主要领域" prop="field">
                  <el-select v-model="profileForm.field" placeholder="请选择主要研究领域" style="width: 100%">
                    <el-option label="计算机科学" value="计算机科学" />
                    <el-option label="人工智能" value="人工智能" />
                    <el-option label="机器学习" value="机器学习" />
                    <el-option label="数据科学" value="数据科学" />
                    <el-option label="软件工程" value="软件工程" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>

                <el-form-item
                  v-if="isReviewer"
                  label="研究方向"
                  prop="researchTags"
                >
                  <el-select
                    v-model="profileForm.researchTags"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请选择或创建研究方向标签（可多个）"
                    style="width: 100%"
                    :reserve-keyword="false"
                    class="research-tags-select"
                  >
                    <el-option label="深度学习" value="深度学习" />
                    <el-option label="计算机视觉" value="计算机视觉" />
                    <el-option label="自然语言处理" value="自然语言处理" />
                    <el-option label="强化学习" value="强化学习" />
                    <el-option label="图神经网络" value="图神经网络" />
                    <el-option label="推荐系统" value="推荐系统" />
                    <el-option label="数据挖掘" value="数据挖掘" />
                    <el-option label="知识图谱" value="知识图谱" />
                    <el-option label="分布式系统" value="分布式系统" />
                    <el-option label="计算机网络" value="计算机网络" />
                    <el-option label="信息安全" value="信息安全" />
                    <el-option label="数据库" value="数据库" />
                    <el-option label="算法理论" value="算法理论" />
                    <el-option label="人机交互" value="人机交互" />
                    <el-option label="生物信息学" value="生物信息学" />
                  </el-select>
                  <div class="field-tip">
                    <el-icon color="#e6a23c"><InfoFilled /></el-icon>
                    研究方向标签越多，系统越能精准为您推荐合适的评审论文
                  </div>
                </el-form-item>

                <el-form-item
                  v-if="isReviewer"
                  label="评审偏好"
                  prop="reviewPreferences"
                >
                  <el-checkbox-group v-model="profileForm.reviewPreferences">
                    <el-checkbox value="theory">偏重理论研究</el-checkbox>
                    <el-checkbox value="application">偏重应用研究</el-checkbox>
                    <el-checkbox value="survey">接受综述类论文</el-checkbox>
                    <el-checkbox value="interdisciplinary">接受跨学科论文</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>

                <el-form-item label="个人简介" prop="bio">
                  <el-input
                    v-model="profileForm.bio"
                    type="textarea"
                    :rows="4"
                    placeholder="介绍一下自己的学术背景、研究兴趣等"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" :icon="Check" @click="handleSaveProfile">
                    保存修改
                  </el-button>
                  <el-button :icon="Refresh" @click="handleReset">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-card shadow="hover">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
                class="password-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请确认新密码"
                    show-password
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwd" :icon="Lock" @click="handleChangePassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="账号安全" name="security">
            <el-card shadow="hover">
              <div class="security-list">
                <div class="security-item">
                  <div class="security-info">
                    <div class="security-icon-wrap" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
                      <el-icon :size="24"><Lock /></el-icon>
                    </div>
                    <div class="security-text">
                      <div class="security-title">账号密码</div>
                      <div class="security-desc">已设置，上次修改：{{ formatDate(userInfo?.passwordChangedAt) }}
                      </div>
                    </div>
                  </div>
                  <el-button type="primary" link @click="activeTab = 'password'">修改</el-button>
                </div>
                <div class="security-item">
                  <div class="security-info">
                    <div class="security-icon-wrap" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
                      <el-icon :size="24"><Iphone /></el-icon>
                    </div>
                    <div class="security-text">
                      <div class="security-title">手机绑定</div>
                      <div class="security-desc">{{ userInfo?.phone ? '已绑定：' + maskPhone(userInfo.phone) : '未绑定' }}</div>
                    </div>
                  </div>
                  <el-button type="primary" link>{{ userInfo?.phone ? '更换' : '绑定' }}</el-button>
                </div>
                <div class="security-item">
                  <div class="security-info">
                    <div class="security-icon-wrap" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
                      <el-icon :size="24"><Key /></el-icon>
                    </div>
                    <div class="security-text">
                      <div class="security-title">双因素认证</div>
                      <div class="security-desc">增强账号安全，防止账号被盗</div>
                    </div>
                  </div>
                  <el-tag type="info" effect="light">未开启</el-tag>
                </div>
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Message, Check, Refresh, Lock, Iphone, Key,
  OfficeBuilding, InfoFilled, CollectionTag
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const activeTab = ref('info')
const saving = ref(false)
const savingpwd = ref(false)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = computed(() => userStore.userInfo || {
  id: 'USR001',
  username: 'demo',
  name: '演示用户',
  email: 'demo@example.com',
  role: 'reviewer',
  field: '人工智能',
  institution: '示例大学计算机学院',
  bio: '',
  createdAt: '2026-01-01',
  lastLoginAt: '2026-06-17',
  phone: '13800138000'
})

const isReviewer = computed(() => userStore.isReviewer || userInfo.value.role === 'reviewer')

const roleText = computed(() => {
  const map = { admin: '管理员', author: '作者', reviewer: '评审人' }
  return map[userInfo.value.role] || '用户'
})

const roleTagType = computed(() => {
  const map = { admin: 'danger', author: 'primary', reviewer: 'success' }
  return map[userInfo.value.role] || 'info'
})

const TAG_TYPES = ['primary', 'success', 'warning', 'danger', 'info']
function getTagType(idx) {
  return TAG_TYPES[idx % TAG_TYPES.length]
}

const profileForm = reactive({
  username: userInfo.value.username,
  name: userInfo.value.name || '',
  email: userInfo.value.email,
  field: userInfo.value.field,
  institution: userInfo.value.institution,
  bio: userInfo.value.bio,
  researchTags: userInfo.value.researchTags || ['深度学习', '计算机视觉', '自然语言处理'],
  reviewPreferences: userInfo.value.reviewPreferences || ['theory', 'application']
})

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPwd = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度在 6 到 32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPwd, trigger: 'blur' }]
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN')
  } catch (e) {
    return dateStr || '-'
  }
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

async function handleSaveProfile() {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
    saving.value = true
    await new Promise(resolve => setTimeout(resolve, 800))
    ElMessage.success('个人资料保存成功')
    if (userStore.userInfo) {
      userStore.userInfo = { ...userStore.userInfo, ...profileForm }
    }
  } catch (e) {
    if (e !== false) console.error(e)
  } finally {
    saving.value = false
  }
}

function handleReset() {
  profileForm.username = userInfo.value.username
  profileForm.name = userInfo.value.name || ''
  profileForm.email = userInfo.value.email
  profileForm.field = userInfo.value.field
  profileForm.institution = userInfo.value.institution
  profileForm.bio = userInfo.value.bio
  profileForm.researchTags = userInfo.value.researchTags || []
  profileForm.reviewPreferences = userInfo.value.reviewPreferences || []
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    savingpwd.value = true
    await new Promise(resolve => setTimeout(resolve, 800))
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    if (e !== false) console.error(e)
  } finally {
    savingpwd.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      console.error(e)
    }
  }
})
</script>

<style scoped>
.profile-container {
  width: 100%;
}

.profile-card {
  border-radius: var(--border-radius);
}

.profile-header {
  text-align: center;
  padding: 20px 0 8px;
}

.avatar {
  margin-bottom: 16px;
  font-size: 36px;
  font-weight: 600;
}

.username {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.role-tag {
  margin-bottom: 12px;
}

.email {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.profile-desc {
  font-size: 13px;
}

.profile-tabs {
  border-radius: var(--border-radius);
}

.profile-form,
.password-form {
  padding: 20px 40px;
  max-width: 640px;
}

.security-list {
  padding: 4px 0;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.security-item:last-child {
  border-bottom: none;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.security-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.security-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.reviewer-tags-section {
  margin-top: 16px;
  padding: 14px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6fffb 100%);
  border-radius: 12px;
  border: 1px solid #b3e5fc;
}
.reviewer-tags-section .section-title {
  font-size: 13px;
  font-weight: 600;
  color: #0277bd;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.research-tag {
  cursor: default;
}
.field-tip {
  margin-top: 6px;
  font-size: 11px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}
.research-tags-select :deep(.el-select__tags) {
  padding: 4px;
}
</style>
