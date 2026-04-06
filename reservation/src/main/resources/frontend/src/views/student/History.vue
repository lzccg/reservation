<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的预约与签到</h2>
    </div>

    <el-alert
      title="温馨提示：三次爽约或者五次迟到将会被限制账号预约功能"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

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
        <el-tab-pane label="待签到" name="pending"></el-tab-pane>
        <el-tab-pane label="已签到" name="checked"></el-tab-pane>
        <el-tab-pane label="迟到" name="late"></el-tab-pane>
        <el-tab-pane label="未签到" name="absent"></el-tab-pane>
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
        <el-table-column prop="timeRange" label="宣讲时间" width="180" />
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
              v-if="row.canCancel" 
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReserveHistory, cancelReservation } from '@/api/student'

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

const getStatusLabel = (status) => {
  const map = { checked: '已签到', pending: '待签到', late: '迟到', absent: '未签到', canceled: '已取消' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { checked: 'success', pending: 'warning', late: 'danger', absent: 'info', canceled: 'info' }
  return map[status] || 'info'
}

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const formatTimeRange = (start, end) => {
  const s = normalizeDateTime(start)
  const e = normalizeDateTime(end)
  if (!s || !e) return '-'
  const date = s.slice(0, 10)
  const st = s.slice(11, 16)
  const et = e.slice(11, 16)
  return `${date} ${st}-${et}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getReserveHistory({
      type: activeTab.value,
      current: page.current,
      size: page.size
    })
    const s = data?.stats || {}
    stats.total = s.total || 0
    stats.checkedIn = s.checkedIn || 0
    stats.pending = s.pending || 0
    stats.late = s.late || 0

    const list = (data.records || []).map((i) => ({
      ...i,
      timeRange: formatTimeRange(i.startTime, i.endTime),
      reserveTime: normalizeDateTime(i.reserveTime)
    }))
    tableData.value = list
    page.total = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleTabClick = () => {
  page.current = 1
  fetchData()
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消预约 "${row.title}" 吗？`, '提示', {
    type: 'warning'
  }).then(() => {
    return cancelReservation(row.reservationId)
  }).then(() => {
    ElMessage.success('已取消预约')
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
