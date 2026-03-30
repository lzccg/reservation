<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">学生信息维护</h2>
    </div>

    <el-alert
      v-if="userInfo?.isFirstLogin"
      title="需要完善信息"
      type="warning"
      description="请先完善您的真实姓名、学院专业等个人信息后再进行宣讲会的预约和签到操作。"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <el-card shadow="hover" class="app-card">
      <el-form ref="studentFormRef" :model="studentForm" :rules="studentRules" label-width="100px" style="max-width: 600px;">
        <el-form-item label="真实姓名">
          <el-input v-model="studentForm.name" disabled />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="studentForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="studentForm.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="studentForm.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属学院" prop="college">
          <el-input v-model="studentForm.college" placeholder="如：计算机科学与工程学院" />
        </el-form-item>
        <el-form-item label="所属专业" prop="major">
          <el-input v-model="studentForm.major" placeholder="如：软件工程" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="studentForm.grade" placeholder="如：2021级" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingInfo" @click="saveStudentInfo">保存信息</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const savingInfo = ref(false)
const studentFormRef = ref(null)

const studentForm = reactive({
  name: userInfo.value?.name || '',
  studentNo: userInfo.value?.studentNo || userInfo.value?.id || '',
  phone: userInfo.value?.phone || '',
  gender: '',
  college: '',
  major: '',
  grade: ''
})

const studentRules = {
  phone: [{ required: true, message: '必填项', trigger: 'blur' }],
  gender: [{ required: true, message: '必填项', trigger: 'change' }],
  college: [{ required: true, message: '必填项', trigger: 'blur' }],
  major: [{ required: true, message: '必填项', trigger: 'blur' }],
  grade: [{ required: true, message: '必填项', trigger: 'blur' }]
}

const saveStudentInfo = () => {
  studentFormRef.value.validate(valid => {
    if (valid) {
      savingInfo.value = true
      setTimeout(() => {
        savingInfo.value = false
        ElMessage.success('信息保存成功！')
        userStore.setUserInfo({ ...userInfo.value, ...studentForm, isFirstLogin: false })
      }, 500)
    }
  })
}
</script>

<style scoped>
</style>
