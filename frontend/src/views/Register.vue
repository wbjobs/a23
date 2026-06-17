<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <div class="logo-wrapper">
          <el-icon :size="40" color="#409eff"><UserFilled /></el-icon>
        </div>
        <h1 class="register-title">用户注册</h1>
        <p class="register-subtitle">创建您的账户以使用系统</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="name">
          <el-input
            v-model="registerForm.name"
            placeholder="请输入真实姓名"
            size="large"
            :prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            size="large"
            :prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item prop="affiliation">
          <el-input
            v-model="registerForm.affiliation"
            placeholder="请输入所属机构（学校/公司/研究机构）"
            size="large"
            :prefix-icon="OfficeBuilding"
          />
        </el-form-item>

        <el-form-item prop="role">
          <el-select
            v-model="registerForm.role"
            placeholder="请选择角色"
            size="large"
            style="width: 100%"
          >
            <el-option label="作者" value="author">
              <span style="display:flex; align-items:center; gap:8px;">
                <el-icon color="#409eff"><EditPen /></el-icon>
                <span>作者 - 提交和管理论文</span>
              </span>
            </el-option>
            <el-option label="评审人" value="reviewer">
              <span style="display:flex; align-items:center; gap:8px;">
                <el-icon color="#67c23a"><DataAnalysis /></el-icon>
                <span>评审人 - 评审分配的论文</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注 册
        </el-button>
      </el-form>

      <div class="register-footer">
        <span>已有账户？</span>
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  User,
  Message,
  Lock,
  OfficeBuilding,
  EditPen,
  DataAnalysis
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  name: '',
  email: '',
  affiliation: '',
  role: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  affiliation: [
    { required: true, message: '请输入所属机构', trigger: 'blur' },
    { min: 2, max: 100, message: '机构名称长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度在 6 到 32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
    loading.value = true
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    if (error !== false) {
      console.error('注册失败', error)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
  width: 520px;
  padding: 36px 40px;
  background-color: var(--color-bg-primary);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-wrapper {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-wrapper :deep(.el-icon) {
  color: #fff !important;
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.register-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.register-form {
  margin-bottom: 20px;
}

.register-btn {
  width: 100%;
  margin-top: 4px;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-link {
  margin-left: 6px;
  color: var(--color-primary);
  font-weight: 500;
}

.login-link:hover {
  color: var(--color-primary-light);
}
</style>
