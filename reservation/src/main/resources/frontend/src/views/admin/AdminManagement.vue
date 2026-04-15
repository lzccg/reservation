<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">管理员管理</h2>
      <el-button v-if="isSuperAdmin" type="primary" icon="Plus" @click="openCreateDialog">添加管理员</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="管理员名称">
          <el-input v-model="searchForm.keyword" placeholder="模糊搜索" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="adminName" label="管理员名称" min-width="160" />
        <el-table-column prop="role" label="角色级别" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" effect="dark">
              {{ row.role === 1 ? '超级管理员' : '普通管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="140" align="center" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canEditStatus(row)"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              type="warning"
              link
              :disabled="!canResetPassword(row)"
              @click="handleResetPwd(row)"
            >
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog title="添加管理员" v-model="createVisible" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="管理员名称" prop="adminName">
          <el-input v-model="createForm.adminName" placeholder="请输入管理员名称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="createForm.phone" placeholder="可不填" />
        </el-form-item>
        <el-form-item label="角色级别" prop="role">
          <el-select v-model="createForm.role" placeholder="请选择角色级别" style="width: 100%">
            <el-option label="超级管理员" :value="1" />
            <el-option label="普通管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { createAdminAccount, getAdminAccounts, resetAdminAccountPassword, updateAdminAccountStatus } from '@/api/admin'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.userInfo?.adminRoleLevel === 1)
const currentAdminId = computed(() => userStore.userInfo?.adminId)

const searchForm = reactive({ keyword: '' })
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getAdminAccounts({
      keyword: searchForm.keyword,
      current: page.current,
      size: page.size
    })
    tableData.value = data.records || []
    page.total = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  page.current = 1
  fetchData()
}

const canEditStatus = (row) => {
  if (!isSuperAdmin.value) return false
  if (row.role === 1) return false
  return true
}

const canResetPassword = (row) => {
  if (!isSuperAdmin.value) return false
  if (row.role === 1 && row.adminId !== currentAdminId.value) return false
  if (row.role === 1) return false
  return true
}

const handleStatusChange = async (row) => {
  if (!canEditStatus(row)) return
  try {
    await updateAdminAccountStatus(row.adminId, row.status)
    ElMessage.success('操作成功')
  } catch (e) {
    console.error(e)
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleResetPwd = async (row) => {
  if (!canResetPassword(row)) return
  try {
    await ElMessageBox.confirm('确认将该用户密码重置（默认密码123456）？', '重置密码确认', { type: 'warning' })
    await resetAdminAccountPassword(row.adminId)
    ElMessage.success('重置成功')
  } catch (e) {
    if (e && e.message) {
      console.error(e)
    }
  }
}

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const createForm = reactive({ adminName: '', password: '', phone: '', role: 2 })
const createRules = {
  adminName: [{ required: true, message: '请输入管理员名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色级别', trigger: 'change' }]
}

const openCreateDialog = () => {
  createForm.adminName = ''
  createForm.password = ''
  createForm.phone = ''
  createForm.role = 2
  createVisible.value = true
}

const handleCreate = async () => {
  if (!createFormRef.value) return
  createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      await createAdminAccount({ ...createForm })
      ElMessage.success('新增成功')
      createVisible.value = false
      fetchData()
    } catch (e) {
      console.error(e)
    } finally {
      creating.value = false
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>

