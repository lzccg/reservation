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
          <el-input v-model="companyForm.companyName" disabled />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model="companyForm.creditCode" placeholder="请输入企业统一社会信用代码" />
        </el-form-item>
        <el-form-item label="企业所在地" prop="companyLocation">
          <el-input v-model="companyForm.companyLocation" placeholder="如：广东省深圳市南山区" />
        </el-form-item>
        <el-form-item label="企业所属行业" prop="industry">
          <el-select v-model="companyForm.industry" style="width: 100%">
            <el-option label="计算机/互联网" value="计算机/互联网" />
            <el-option label="金融/保险" value="金融/保险" />
            <el-option label="教育/培训" value="教育/培训" />
            <el-option label="制造/工业" value="制造/工业" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业联系人姓名" prop="contactName">
          <el-input v-model="companyForm.contactName" />
        </el-form-item>
        <el-form-item label="企业联系人电话" prop="contactPhone">
          <el-input v-model="companyForm.contactPhone" />
        </el-form-item>
        <el-form-item label="企业电子邮箱" prop="email">
          <el-input v-model="companyForm.email" placeholder="请输入企业电子邮箱" />
        </el-form-item>
        <el-form-item label="企业详细地址" prop="address">
          <el-input v-model="companyForm.address" placeholder="请输入能精确到门牌号的详细地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingInfo" @click="saveCompanyInfo">{{ submitText }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="app-card" style="margin-top: 20px;">
      <template #header>
        <span>账号审核状态</span>
      </template>
      <el-descriptions border :column="2">
        <el-descriptions-item label="企业状态">
          <el-tag :type="statusTagType">{{ statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近修改时间">
          {{ formatDateTime(companyMeta.updateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="账号审核通过时间">
          {{ auditTimeDisplay }}
        </el-descriptions-item>
        <el-descriptions-item label="管理员审核备注">
          {{ companyMeta.auditRemark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { getCompanyProfile, updateCompanyProfile } from '@/api/company'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const savingInfo = ref(false)
const companyFormRef = ref(null)
const companyMeta = reactive({
  status: null,
  updateTime: null,
  auditTime: null,
  auditRemark: ''
})

const companyForm = reactive({
  companyName: userInfo.value?.companyName || '',
  creditCode: userInfo.value?.creditCode || '',
  companyLocation: userInfo.value?.companyLocation || '',
  industry: userInfo.value?.industry || '',
  contactName: userInfo.value?.contactName || '',
  contactPhone: userInfo.value?.contactPhone || '',
  email: userInfo.value?.email || '',
  address: userInfo.value?.address || ''
})

const companyRules = {
  creditCode: [{ required: true, message: '必填项', trigger: 'blur' }],
  companyLocation: [{ required: true, message: '必填项', trigger: 'blur' }],
  industry: [{ required: true, message: '必填项', trigger: 'change' }],
  contactName: [{ required: true, message: '必填项', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '必填项', trigger: 'blur' }],
  email: [
    { required: true, message: '必填项', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  address: [{ required: true, message: '必填项', trigger: 'blur' }]
}

const isBlank = (v) => v === null || v === undefined || `${v}`.trim() === ''

const isComplete = computed(() => {
  return !isBlank(companyForm.creditCode)
    && !isBlank(companyForm.companyLocation)
    && !isBlank(companyForm.industry)
    && !isBlank(companyForm.contactName)
    && !isBlank(companyForm.contactPhone)
    && !isBlank(companyForm.email)
    && !isBlank(companyForm.address)
})

const submitText = computed(() => (userInfo.value?.isFirstLogin ? '提交审核资质' : '修改审核资质'))

const formatDateTime = (value) => {
  if (!value) return '-'
  const s = String(value)
  if (s.includes('T')) {
    const t = s.replace('T', ' ')
    return t.length >= 16 ? t.slice(0, 16) : t
  }
  return s.length >= 16 ? s.slice(0, 16) : s
}

const statusText = computed(() => {
  const s = companyMeta.status
  if (s === 1) return '正常'
  if (s === 2) return '驳回'
  return '待审核'
})

const statusTagType = computed(() => {
  const s = companyMeta.status
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'warning'
})

const auditTimeDisplay = computed(() => {
  if (companyMeta.status === 1) {
    return formatDateTime(companyMeta.auditTime)
  }
  return statusText.value
})

const loadProfile = async () => {
  const data = await getCompanyProfile()
  companyForm.companyName = data.companyName || ''
  companyForm.creditCode = data.creditCode || ''
  companyForm.companyLocation = data.companyLocation || ''
  companyForm.industry = data.industry || ''
  companyForm.contactName = data.contactName || ''
  companyForm.contactPhone = data.contactPhone || ''
  companyForm.email = data.email || ''
  companyForm.address = data.address || ''
  companyMeta.status = data.status ?? null
  companyMeta.updateTime = data.updateTime || null
  companyMeta.auditTime = data.auditTime || null
  companyMeta.auditRemark = data.auditRemark || ''
  userStore.setUserInfo({ ...userInfo.value, ...data, isFirstLogin: !isComplete.value })
}

onMounted(async () => {
  try {
    await loadProfile()
  } catch (e) {
    console.error(e)
  }
})

const saveCompanyInfo = () => {
  if (!companyFormRef.value) return
  companyFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingInfo.value = true
    try {
      await updateCompanyProfile({
        creditCode: companyForm.creditCode,
        companyLocation: companyForm.companyLocation,
        industry: companyForm.industry,
        contactName: companyForm.contactName,
        contactPhone: companyForm.contactPhone,
        email: companyForm.email,
        address: companyForm.address
      })
      await loadProfile()
      ElMessage.success(isComplete.value ? '资质信息修改成功！' : '资质信息已提交审核！')
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
