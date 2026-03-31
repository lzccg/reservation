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
          <el-input v-model="studentForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="studentForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="studentForm.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="studentForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属学院" prop="college">
          <el-input v-model="studentForm.college" placeholder="如：计算机科学与工程学院" />
        </el-form-item>
        <el-form-item label="所属专业" prop="major">
          <el-input v-model="studentForm.major" placeholder="如：软件工程" />
        </el-form-item>
        <el-form-item label="班级" prop="clazz">
          <el-input v-model="studentForm.clazz" placeholder="如：软件工程1班" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingInfo" @click="saveStudentInfo">{{ submitText }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { getStudentProfile, updateStudentProfile } from '@/api/student'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const savingInfo = ref(false)
const studentFormRef = ref(null)

const studentForm = reactive({
  studentName: userInfo.value?.studentName || '',
  studentNo: userInfo.value?.studentNo || '',
  phone: userInfo.value?.phone || '',
  gender: userInfo.value?.gender ?? null,
  college: userInfo.value?.college || '',
  major: userInfo.value?.major || '',
  clazz: userInfo.value?.clazz || ''
})

const studentRules = {
  phone: [{ required: true, message: '必填项', trigger: 'blur' }],
  gender: [{ required: true, message: '必填项', trigger: 'change' }],
  college: [{ required: true, message: '必填项', trigger: 'blur' }],
  major: [{ required: true, message: '必填项', trigger: 'blur' }],
  clazz: [{ required: true, message: '必填项', trigger: 'blur' }]
}

const isBlank = (v) => v === null || v === undefined || `${v}`.trim() === ''

const isComplete = computed(() => {
  return !isBlank(studentForm.phone)
    && studentForm.gender !== null && studentForm.gender !== undefined
    && !isBlank(studentForm.college)
    && !isBlank(studentForm.major)
    && !isBlank(studentForm.clazz)
})

const submitText = computed(() => (userInfo.value?.isFirstLogin ? '保存信息' : '修改信息'))

const loadProfile = async () => {
  const data = await getStudentProfile()
  studentForm.studentName = data.studentName || ''
  studentForm.studentNo = data.studentNo || ''
  studentForm.phone = data.phone || ''
  studentForm.gender = data.gender ?? null
  studentForm.college = data.college || ''
  studentForm.major = data.major || ''
  studentForm.clazz = data.clazz || ''
  userStore.setUserInfo({ ...userInfo.value, ...data, isFirstLogin: !isComplete.value })
}

onMounted(async () => {
  try {
    await loadProfile()
  } catch (e) {
    console.error(e)
  }
})

const saveStudentInfo = () => {
  if (!studentFormRef.value) return
  studentFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingInfo.value = true
    try {
      await updateStudentProfile({
        phone: studentForm.phone,
        gender: studentForm.gender,
        college: studentForm.college,
        major: studentForm.major,
        clazz: studentForm.clazz
      })
      await loadProfile()
      ElMessage.success(isComplete.value ? '信息修改成功！' : '信息保存成功！')
    } catch (e) {
      console.error(e)
    } finally {
      savingInfo.value = false
    }
  })
}
</script>

<style scoped>
</style>
