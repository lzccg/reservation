<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">预约与签到名单</h2>
      <el-button type="success" icon="Download" @click="handleExport">导出签到数据</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属宣讲会">
          <el-select v-model="searchForm.sessionId" placeholder="请选择宣讲会" style="width: 250px" clearable>
            <el-option label="腾讯2025校园招聘宣讲会" :value="201" />
            <el-option label="腾讯技术专场" :value="202" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="searchForm.studentName" placeholder="输入姓名/学号" clearable />
        </el-form-item>
        <el-form-item label="签到状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已签到" value="checked" />
            <el-option label="未签到" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="major" label="专业学院" min-width="150" />
        <el-table-column prop="sessionTitle" label="预约宣讲会" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="reserveTime" label="预约时间" width="180" />
        <el-table-column prop="status" label="签到状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'checked' ? 'success' : 'info'">
              {{ row.status === 'checked' ? '已签到' : '未签到' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkinTime" label="签到时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.checkinTime || '-' }}
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

const searchForm = reactive({
  sessionId: 201,
  studentName: '',
  status: ''
})

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 1, studentName: '张三', studentNo: '20210001', major: '计算机科学与技术', sessionTitle: '腾讯2025校园招聘宣讲会', reserveTime: '2025-03-22 10:15:22', status: 'checked', checkinTime: '2025-04-10 13:45:10' },
      { id: 2, studentName: '李四', studentNo: '20210002', major: '软件工程', sessionTitle: '腾讯2025校园招聘宣讲会', reserveTime: '2025-03-22 10:18:05', status: 'checked', checkinTime: '2025-04-10 13:50:00' },
      { id: 3, studentName: '王五', studentNo: '20210003', major: '通信工程', sessionTitle: '腾讯2025校园招聘宣讲会', reserveTime: '2025-03-22 11:05:12', status: 'pending', checkinTime: null },
      { id: 4, studentName: '赵六', studentNo: '20210004', major: '信息安全', sessionTitle: '腾讯2025校园招聘宣讲会', reserveTime: '2025-03-23 09:30:45', status: 'checked', checkinTime: '2025-04-10 13:20:11' }
    ]
    page.total = 4
    loading.value = false
  }, 400)
}

const handleExport = () => {
  ElMessage.success('正在导出Excel签到数据...')
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
