<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的预约与签到</h2>
    </div>

    <!-- 顶层统计大屏 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title"><el-icon><Tickets /></el-icon> 预约总次数</div>
          <div class="stat-value" style="color: #409EFF">{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title"><el-icon><CircleCheck /></el-icon> 签到总次数</div>
          <div class="stat-value" style="color: #67C23A">{{ stats.checkedIn }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title"><el-icon><Warning /></el-icon> 未签到总次数</div>
          <div class="stat-value" style="color: #909399">{{ stats.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title"><el-icon><Clock /></el-icon> 迟到总次数</div>
          <div class="stat-value" style="color: #F56C6C">{{ stats.late }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格卡片 -->
    <el-card shadow="hover" class="app-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="全部记录" name="all"></el-tab-pane>
        <el-tab-pane label="已签到" name="checked"></el-tab-pane>
        <el-tab-pane label="迟到" name="late"></el-tab-pane>
        <el-tab-pane label="待签到" name="pending"></el-tab-pane>
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="companyName" label="企业名称" width="150" />
        <el-table-column prop="title" label="宣讲会标题" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="time" label="宣讲时间" width="180" />
        <el-table-column prop="location" label="宣讲地点" width="150" />
        <el-table-column prop="reserveTime" label="我的预约时间" width="180" />
        <el-table-column prop="status" label="签到状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="danger" 
              link 
              v-if="row.status === 'pending'" 
              @click="handleCancel(row)"
            >
              取消预约
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

const stats = reactive({
  total: 0,
  checkedIn: 0,
  pending: 0,
  late: 0
})

const mockData = [
  { id: 201, companyName: '腾讯科技', title: '腾讯2025校园招聘宣讲会', time: '2025-04-10 14:00', location: '大学生活动中心', reserveTime: '2025-03-24 10:20', status: 'pending' },
  { id: 205, companyName: '阿里巴巴', title: '阿里技术分享会', time: '2025-03-23 14:00', location: '计算机大楼', reserveTime: '2025-03-20 09:15', status: 'checked' },
  { id: 206, companyName: '字节跳动', title: '字节宣讲会', time: '2025-03-20 14:00', location: '体育馆', reserveTime: '2025-03-18 09:15', status: 'late' }
]

const getStatusLabel = (status) => {
  const map = { checked: '已签到', pending: '待签到', late: '迟到' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { checked: 'success', pending: 'warning', late: 'danger' }
  return map[status] || 'info'
}

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    // 根据所有数据刷新统计数字 (如果有数据则取数据，如果没有则显示 0)
    stats.total = mockData.length || 0;
    stats.checkedIn = mockData.filter(i => i.status === 'checked').length || 0;
    stats.pending = mockData.filter(i => i.status === 'pending').length || 0;
    stats.late = mockData.filter(i => i.status === 'late').length || 0;

    let result = mockData
    if (activeTab.value !== 'all') {
      result = mockData.filter(i => i.status === activeTab.value)
    }
    tableData.value = result
    page.total = result.length
    loading.value = false
  }, 400)
}

const handleTabClick = () => {
  page.current = 1
  fetchData()
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消预约 "${row.title}" 吗？`, '提示', {
    type: 'warning'
  }).then(() => {
    ElMessage.success('已取消预约')
    const index = mockData.findIndex(i => i.id === row.id)
    if (index > -1) mockData.splice(index, 1)
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}
.stat-title {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
}
</style>
