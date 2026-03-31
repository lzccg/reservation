<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
      <div class="logo">
        <span v-if="!isCollapse">宣讲会系统</span>
        <span v-else>预约</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>

        <!-- 管理员菜单 -->
        <template v-if="role === 'admin'">
          <el-sub-menu index="admin-users">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item index="/admin/students">学生管理</el-menu-item>
            <el-menu-item index="/admin/companies">企业管理</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/admin/sessions">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>宣讲会调度</template>
          </el-menu-item>
          <el-menu-item index="/admin/checkin">
            <el-icon><Monitor /></el-icon>
            <template #title>现场签到</template>
          </el-menu-item>
          <el-menu-item index="/admin/statistics">
            <el-icon><PieChart /></el-icon>
            <template #title>全局统计</template>
          </el-menu-item>
        </template>

        <!-- 企业菜单 -->
        <template v-if="role === 'company'">
          <el-menu-item index="/company/profile">
            <el-icon><EditPen /></el-icon>
            <template #title>信息维护</template>
          </el-menu-item>
          <el-menu-item index="/company/sessions">
            <el-icon><List /></el-icon>
            <template #title>发布与管理</template>
          </el-menu-item>
          <el-menu-item index="/company/records">
            <el-icon><DataLine /></el-icon>
            <template #title>预约与签到</template>
          </el-menu-item>
        </template>

        <!-- 学生菜单 -->
        <template v-if="role === 'student'">
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <template #title>配置个人信息</template>
          </el-menu-item>
          <el-menu-item index="/student/sessions">
            <el-icon><Calendar /></el-icon>
            <template #title>宣讲会大厅</template>
          </el-menu-item>
          <el-menu-item index="/student/face">
            <el-icon><CameraFilled /></el-icon>
            <template #title>人脸信息采集</template>
          </el-menu-item>
          <el-menu-item index="/student/history">
            <el-icon><Timer /></el-icon>
            <template #title>预约签到历史</template>
          </el-menu-item>
          <el-menu-item index="/student/checkin-entrance">
            <el-icon><Clock /></el-icon>
            <template #title>近期宣讲会</template>
          </el-menu-item>
        </template>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="toggle-icon" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="welcome-text">欢迎使用校园宣讲会预约签到系统</span>
        </div>
        <div class="header-right">
          <!-- 管理员端顶部导航：现场签到终端快速入口图标 -->
          <el-tooltip content="开启现场扫脸签到大屏" placement="bottom" v-if="role === 'admin'">
            <div class="quick-nav-icon" @click="$router.push('/admin/checkin')">
              <el-icon size="22" color="#409EFF"><Monitor /></el-icon>
            </div>
          </el-tooltip>

          <el-dropdown @command="handleCommand" style="margin-left: 15px;">
            <span class="el-dropdown-link userinfo">
              <el-avatar :size="32" icon="UserFilled" style="margin-right: 8px" />
              {{ displayName }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog title="修改密码" v-model="pwdDialogVisible" width="400px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="pwdDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="pwdLoading" @click="handleUpdatePwd">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const userInfo = computed(() => userStore.userInfo)
const role = computed(() => userStore.role)
const activeMenu = computed(() => route.path)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const displayName = computed(() => {
  const info = userInfo.value || {}
  return info.studentName || info.companyName || info.adminName || info.name || info.username || '用户'
})

// === 修改密码逻辑 ===
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', { type: 'warning' }).then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    }).catch(() => {})
  } else if (command === 'password') {
    if (role.value !== 'student' && role.value !== 'company' && role.value !== 'admin') {
      ElMessage.error('当前角色不支持修改密码')
      return
    }
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialogVisible.value = true
  }
}

const getPasswordUsername = () => {
  const info = userInfo.value || {}
  if (role.value === 'student') {
    return info.studentNo || info.phone || ''
  }
  if (role.value === 'company') {
    return info.contactPhone || ''
  }
  if (role.value === 'admin') {
    return info.adminName || ''
  }
  return ''
}

const handleUpdatePwd = async () => {
  if (!pwdFormRef.value) return
  pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    const username = getPasswordUsername()
    if (!username) {
      ElMessage.error('缺少账号信息，无法修改密码')
      return
    }
    pwdLoading.value = true
    try {
      await changePassword({
        role: role.value,
        username,
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      pwdDialogVisible.value = false
      ElMessage.success('密码修改成功，请重新登录！')
      userStore.logout()
      router.push('/login')
    } catch (e) {
      console.error(e)
    } finally {
      pwdLoading.value = false
    }
  })
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3a4d;
  overflow: hidden;
  white-space: nowrap;
}

.el-menu-vertical {
  border-right: none;
  flex: 1;
}

.app-header {
  height: 60px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.toggle-icon {
  font-size: 20px;
  cursor: pointer;
  margin-right: 15px;
  color: #606266;
}

.toggle-icon:hover {
  color: #409EFF;
}

.welcome-text {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
}

.quick-nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: background-color 0.3s;
}
.quick-nav-icon:hover {
  background-color: #f5f7fa;
}

.userinfo {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
