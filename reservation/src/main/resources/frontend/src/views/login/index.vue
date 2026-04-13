<template>
  <div class="login-wrapper">
    <div class="login-box" :class="{ 'register-mode': mode !== 'login' }">
      <div class="login-header">
        <h2>{{ mode === 'login' ? '系统登录' : (mode === 'register_student' ? '学生注册' : '企业注册') }}</h2>
        <p>校园宣讲会预约签到系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-if="mode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        size="large"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="账号" 
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password
            prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="role">
          <el-select v-model="loginForm.role" placeholder="请选择角色" style="width: 100%">
            <template #prefix>
              <el-icon><Avatar /></el-icon>
            </template>
            <el-option label="学生" value="student" />
            <el-option label="企业" value="company" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        
        <div class="login-footer">
          <span>没有任何账号？</span>
          <el-button type="primary" link @click="mode = 'register_student'">学生注册</el-button> | 
          <el-button type="primary" link @click="mode = 'register_company'">企业注册</el-button>
        </div>
      </el-form>

      <!-- 学生注册表单 -->
      <el-form
        v-if="mode === 'register_student'"
        ref="studentRegFormRef"
        :model="studentRegForm"
        :rules="studentRegRules"
        label-position="top"
        size="large"
      >
        <el-form-item label="学生姓名" prop="name">
          <el-input v-model="studentRegForm.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="studentRegForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="studentRegForm.phone" placeholder="请输入常用手机号" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="studentRegForm.password" type="password" placeholder="请输入登录密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegisterStudent">注册</el-button>
        </el-form-item>
        <div class="login-footer">
          <span>已有账号？</span><el-button type="primary" link @click="mode = 'login'">立即登录</el-button>
        </div>
      </el-form>

      <!-- 企业注册表单 -->
      <el-form
        v-if="mode === 'register_company'"
        ref="companyRegFormRef"
        :model="companyRegForm"
        :rules="companyRegRules"
        label-position="top"
        size="large"
      >
        <el-form-item label="企业全称" prop="name">
          <el-input v-model="companyRegForm.name" placeholder="请输入企业营业执照全称" />
        </el-form-item>
        <el-form-item label="信用代码" prop="creditCode">
          <el-input v-model="companyRegForm.creditCode" placeholder="请输入社会统一信用代码" />
        </el-form-item>
        <el-form-item label="企业所在地" prop="location">
          <el-input v-model="companyRegForm.location" placeholder="如：广东省深圳市" />
        </el-form-item>
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="companyRegForm.industry" placeholder="请选择所属行业" style="width: 100%">
            <el-option label="计算机/互联网" value="计算机/互联网" />
            <el-option label="金融/保险" value="金融/保险" />
            <el-option label="教育/培训" value="教育/培训" />
            <el-option label="制造/工业" value="制造/工业" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="companyRegForm.contactName" placeholder="请输入企业联系人姓名" />
        </el-form-item>
        <el-form-item label="联系人电话" prop="contactPhone">
          <el-input v-model="companyRegForm.contactPhone" placeholder="请输入企业联系人电话" />
        </el-form-item>
        <el-form-item label="企业电子邮箱" prop="email">
          <el-input v-model="companyRegForm.email" placeholder="请输入企业电子邮箱" />
        </el-form-item>
        <el-form-item label="企业登录密码" prop="password">
          <el-input v-model="companyRegForm.password" type="password" placeholder="请输入登录密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegisterCompany">注册</el-button>
        </el-form-item>
        <div class="login-footer">
          <span>已有账号？</span><el-button type="primary" link @click="mode = 'login'">立即登录</el-button>
        </div>
      </el-form>

    </div>
    <!-- 装饰背景效果 -->
    <div class="glass-bg"></div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login') // 'login' | 'register_student' | 'register_company'
const loading = ref(false)

const loginFormRef = ref(null)
const studentRegFormRef = ref(null)
const companyRegFormRef = ref(null)

// === 登录逻辑 ===
const loginForm = reactive({ username: '', password: '', role: 'student' })
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleLogin = () => {
  if (!loginFormRef.value) return
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const data = await login(loginForm)
        const { user, role, needCompleteProfile, adminRoleLevel } = data
        userStore.setToken(`token-${user.id || user.studentId || user.companyId || user.adminId}`)
        userStore.setUserInfo({ ...user, role, adminRoleLevel: adminRoleLevel ?? user.adminRoleLevel, isFirstLogin: !!needCompleteProfile })
        
        ElMessage.success('登录成功')
        if (needCompleteProfile && role !== 'admin') {
          ElMessage.warning('首次登录请先到个人信息页完善资料')
          if (role === 'student') {
            router.push('/student/profile')
          } else if (role === 'company') {
            router.push('/company/profile')
          } else {
            router.push('/dashboard')
          }
        } else {
          router.push('/dashboard')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

// === 学生注册逻辑 ===
const studentRegForm = reactive({ name: '', studentNo: '', phone: '', password: '' })
const studentRegRules = {
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleRegisterStudent = () => {
  if (!studentRegFormRef.value) return
  studentRegFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register({ ...studentRegForm, role: 'student' })
        ElMessage.success('注册成功！请登录')
        mode.value = 'login'
        loginForm.username = studentRegForm.studentNo
        loginForm.role = 'student'
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

// === 企业注册逻辑 ===
const companyRegForm = reactive({ name: '', creditCode: '', location: '', industry: '', contactName: '', contactPhone: '', email: '', password: '' })
const companyRegRules = {
  name: [{ required: true, message: '请输入企业全称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入社会统一信用代码', trigger: 'blur' }],
  location: [{ required: true, message: '请输入所在地', trigger: 'blur' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系人电话', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入企业邮箱', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleRegisterCompany = () => {
  if (!companyRegFormRef.value) return
  companyRegFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register({ ...companyRegForm, role: 'company' })
        ElMessage.success('注册成功！请等待审核')
        mode.value = 'login'
        loginForm.username = companyRegForm.contactPhone
        loginForm.role = 'company'
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  position: relative;
  overflow: hidden;
}

.glass-bg {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0) 80%);
  animation: rotate 60s linear infinite;
  pointer-events: none;
}

@keyframes rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.login-box {
  width: 400px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  padding: 40px 30px;
  z-index: 10;
  transition: all 0.3s ease;
}

.login-box.register-mode {
  width: 500px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0;
  font-size: 26px;
  color: #303133;
  letter-spacing: 1px;
}

.login-header p {
  margin: 10px 0 0;
  font-size: 14px;
  color: #909399;
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 10px;
  height: 44px;
  border-radius: 6px;
}

.login-footer {
  margin-top: 15px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

:deep(.el-input__wrapper) {
  background-color: #f5f7fa;
  box-shadow: none !important;
  border: 1px solid #e4e7ed;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff !important;
}
</style>
