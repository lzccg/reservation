<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">企业资质信息维护</h2>
    </div>

    <el-alert
      v-if="userInfo?.isFirstLogin"
      title="需要完善信息"
      type="warning"
      description="系统检测到您尚未完善企业基本资质信息。填写完成后企业账号将进入待审核状态。"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <el-card shadow="hover" class="app-card">
      <el-form ref="companyFormRef" :model="companyForm" :rules="companyRules" label-width="160px" style="max-width: 800px;">
        <el-form-item label="企业全称">
          <el-input v-model="companyForm.name" disabled />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="uscc">
          <el-input v-model="companyForm.uscc" placeholder="请输入企业统一社会信用代码" />
        </el-form-item>
        <el-form-item label="企业所在地" prop="location">
          <el-input v-model="companyForm.location" placeholder="如：广东省深圳市南山区" />
        </el-form-item>
        <el-form-item label="企业所属行业" prop="industry">
          <el-select v-model="companyForm.industry" style="width: 100%">
            <el-option label="计算机/互联网" value="Internet" />
            <el-option label="金融/保险" value="Finance" />
            <el-option label="教育/培训" value="Education" />
            <el-option label="制造/工业" value="Manufacture" />
            <el-option label="其他" value="Other" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业联系人姓名" prop="contactName">
          <el-input v-model="companyForm.contactName" />
        </el-form-item>
        <el-form-item label="企业联系人电话" prop="contactPhone">
          <el-input v-model="companyForm.contactPhone" />
        </el-form-item>
        <el-form-item label="企业详细地址" prop="address">
          <el-input v-model="companyForm.address" placeholder="请输入能精确到门牌号的详细地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingInfo" @click="saveCompanyInfo">提交审核资质</el-button>
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
const companyFormRef = ref(null)

const companyForm = reactive({
  name: userInfo.value?.name || '',
  uscc: '',
  location: userInfo.value?.location || '',
  industry: userInfo.value?.industry || '',
  contactName: userInfo.value?.contactName || '',
  contactPhone: userInfo.value?.contactPhone || '',
  address: ''
})

const companyRules = {
  uscc: [{ required: true, message: '必填项', trigger: 'blur' }],
  location: [{ required: true, message: '必填项', trigger: 'blur' }],
  industry: [{ required: true, message: '必填项', trigger: 'change' }],
  contactName: [{ required: true, message: '必填项', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '必填项', trigger: 'blur' }],
  address: [{ required: true, message: '必填项', trigger: 'blur' }]
}

const saveCompanyInfo = () => {
  companyFormRef.value.validate(valid => {
    if (valid) {
      savingInfo.value = true
      setTimeout(() => {
        savingInfo.value = false
        ElMessage.success('资质信息已提交审核！')
        userStore.setUserInfo({ ...userInfo.value, ...companyForm, isFirstLogin: false })
      }, 500)
    }
  })
}
</script>

<style scoped>
</style>
