<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable style="width: 150px">
            <el-option label="学生" value="student" />
            <el-option label="企业" value="company" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询</el-button>
          <el-button icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区域 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名/企业名称" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)">
              {{ getRoleName(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名/企业" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名或企业名称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%" :disabled="isEdit">
            <el-option label="学生" value="student" />
            <el-option label="企业" value="company" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入初始密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据和状态
const loading = ref(false)
const tableData = ref([])
const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForm = reactive({
  username: '',
  role: ''
})

// MOCK 模拟获取数据
const mockFetchUsers = () => {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        records: [
          { id: 1, username: 'admin', name: '超级管理员', role: 'admin', createTime: '2023-01-01 12:00:00', status: 1 },
          { id: 2, username: 'company1', name: '腾讯科技', role: 'company', createTime: '2023-05-12 10:23:44', status: 1 },
          { id: 3, username: 'student1', name: '张三', role: 'student', createTime: '2023-09-01 08:30:11', status: 1 },
          { id: 4, username: 'student2', name: '李四', role: 'student', createTime: '2023-09-02 09:12:00', status: 0 }
        ],
        total: 4
      })
    }, 500)
  })
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await mockFetchUsers()
    tableData.value = res.records
    page.total = res.total
  } catch (err) {
    ElMessage.error('获取用户数据失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.role = ''
  page.current = 1
  fetchData()
}

// 辅助方法
const getRoleName = (role) => {
  const map = { admin: '管理员', company: '企业', student: '学生' }
  return map[role] || role
}

const getRoleTagType = (role) => {
  const map = { admin: 'danger', company: 'warning', student: 'info' }
  return map[role] || ''
}

const handleStatusChange = (row) => {
  ElMessage.success(`用户 ${row.username} 状态已${row.status === 1 ? '启用' : '禁用'}`)
}

// 弹窗逻辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  username: '',
  name: '',
  role: 'student',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名/企业', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(form, { id: null, username: '', name: '', role: 'student', password: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？此操作不可逆！`, '警告', {
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
  }).then(() => {
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(valid => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        submitLoading.value = false
        dialogVisible.value = false
        ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
        fetchData()
      }, 500)
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
