<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">全局宣讲会状态中心</h2>
    </div>

    <!-- 搜索筛选区 -->
    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="企业名称">
          <el-input v-model="searchForm.companyName" placeholder="相关企业" clearable />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="searchForm.status" placeholder="宣讲会状态" clearable style="width: 150px">
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已发布" :value="2" />
            <el-option label="已结束" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="已驳回" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="app-card">
      <el-table 
        v-loading="loading" 
        :data="tableData" 
        border 
        style="width: 100%" 
        header-cell-class-name="table-header"
      >
        <el-table-column prop="companyName" label="关联企业" width="180" show-overflow-tooltip />
        <el-table-column prop="sessionTitle" label="宣讲会标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="申请档期" width="220">
          <template #default="{ row }">
            {{ formatTimeRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="sessionLocation" label="审核场地" width="180" show-overflow-tooltip />
        <el-table-column prop="capacity" label="申报总容纳" width="100" align="center" />
        <el-table-column prop="status" label="当前状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.sessionStatus)">{{ getStatusName(row.sessionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作指令" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="View" @click="handleViewSessionDialog(row)">
              详情与审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 详情与综合审核面板（支持行内打回和强制修改） -->
    <el-dialog title="宣讲会全链路调度面板" v-model="sessionDialogVisible" width="650px" destroy-on-close>
      <div v-if="currentSession">
        <el-descriptions border :column="2" style="margin-bottom: 20px;">
          <el-descriptions-item label="企业方">{{ currentSession.companyName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(currentSession.sessionStatus)">{{ getStatusName(currentSession.sessionStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="活动全称" :span="2">{{ currentSession.sessionTitle }}</el-descriptions-item>
          <el-descriptions-item label="最初预约档期" :span="2">{{ formatTimeRange(currentSession.startTime, currentSession.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="申报总容纳">{{ currentSession.capacity }}</el-descriptions-item>
          <el-descriptions-item label="剩余座位">{{ currentSession.remainingSeats ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="宣讲内容" :span="2">{{ currentSession.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 1" label="审核时间" :span="2">{{ formatDateTime(currentSession.auditTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 5" label="审核时间" :span="2">{{ formatDateTime(currentSession.auditTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 5" label="审核备注" :span="2">{{ currentSession.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 2" label="发布时间" :span="2">{{ formatDateTime(currentSession.publishTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 3" label="发布时间" :span="2">{{ formatDateTime(currentSession.publishTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 4" label="取消原因" :span="2">{{ currentSession.cancelReason || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentSession.sessionStatus === 3 || currentSession.sessionStatus === 4" label="签到人数" :span="2">{{ checkinCount }}</el-descriptions-item>
        </el-descriptions>

        <!-- 管理员直接在这个窗口覆盖系统资源表单 -->
        <h4 style="margin: 0 0 15px;">系统资源干预分配 (可强制覆盖修改)</h4>
        <el-form ref="editFormRef" :model="editForm" label-width="100px" size="small" border>
          <el-form-item label="活动标题" prop="sessionTitle">
            <el-input v-model="editForm.sessionTitle" :disabled="readOnly" />
          </el-form-item>
          <el-form-item label="审批场地" prop="sessionLocation">
            <el-input v-model="editForm.sessionLocation" :disabled="readOnly" />
          </el-form-item>
          <el-form-item label="审批档期" prop="timeRange">
            <el-date-picker
              v-model="editForm.timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
              :disabled="readOnly"
            />
          </el-form-item>
          <el-form-item label="容纳核准席位" prop="capacity">
            <el-input-number v-model="editForm.capacity" :min="10" :disabled="readOnly" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" plain :disabled="readOnly" @click="submitForceEdit">保存对表单项的强制干预配置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer" style="display: flex; justify-content: space-between;">
          <div>
            <el-button v-if="[1, 2].includes(currentSession?.sessionStatus)" type="danger" plain @click="handleForceCancel(currentSession)">强制叫停活动</el-button>
          </div>

          <div>
            <el-button @click="sessionDialogVisible = false">关闭</el-button>
            <template v-if="currentSession?.sessionStatus === 0">
              <el-button type="danger" @click="handleReject(currentSession)">拒绝并打回</el-button>
              <el-button type="success" @click="handleApprove(currentSession)">基于当前干预表单通过</el-button>
            </template>
          </div>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllSessions, getSessionDetail, auditSession } from '@/api/admin'

const route = useRoute()
const searchForm = reactive({ companyName: '', status: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

// 详情控制
const sessionDialogVisible = ref(false)
const currentSession = ref(null)
const editFormRef = ref(null)
const editForm = reactive({
  sessionTitle: '',
  sessionLocation: '',
  timeRange: [],
  capacity: 0,
  description: ''
})
const checkinCount = ref(0)

const readOnly = computed(() => {
  const s = currentSession.value?.sessionStatus
  return s === 3 || s === 4
})

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.companyName = ''
  searchForm.status = ''
  page.current = 1
  fetchData()
}

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const normalizeIsoDateTime = (v) => {
  if (!v) return ''
  let s = String(v).trim()
  if (!s) return ''
  if (s.includes(' ')) {
    s = s.replace(' ', 'T')
  }
  const dot = s.indexOf('.')
  if (dot > 0) {
    s = s.slice(0, dot)
  }
  if (s.length === 16) {
    s = s + ':00'
  }
  return s
}

const formatDateTime = (v) => {
  const s = normalizeDateTime(v)
  if (!s) return '-'
  return s.length >= 16 ? s.slice(0, 16) : s
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
    const data = await getAllSessions({
      companyName: searchForm.companyName,
      status: searchForm.status === '' ? undefined : searchForm.status,
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

const getStatusName = (status) => {
  const map = { 0: '待审核', 1: '已审核', 2: '已发布', 3: '已结束', 4: '已取消', 5: '已打回' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'info', 5: 'danger' }
  return map[status] || 'info'
}

const handleViewSessionDialog = async (row) => {
  try {
    const data = await getSessionDetail(row.sessionId)
    const s = data.session
    currentSession.value = { ...s, companyName: data.companyName }
    checkinCount.value = data.checkinCount || 0
    Object.assign(editForm, {
      sessionTitle: s.sessionTitle || '',
      sessionLocation: s.sessionLocation || '',
      timeRange: [normalizeIsoDateTime(s.startTime), normalizeIsoDateTime(s.endTime)],
      capacity: s.capacity || 0,
      description: s.description || ''
    })
    sessionDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// 保存强制配置
const submitForceEdit = async () => {
  if (!currentSession.value) return
  if (readOnly.value) return
  try {
    await auditSession(currentSession.value.sessionId, 'update', {
      sessionTitle: editForm.sessionTitle,
      sessionLocation: editForm.sessionLocation,
      startTime: editForm.timeRange?.[0],
      endTime: editForm.timeRange?.[1],
      capacity: editForm.capacity,
      description: editForm.description
    })
    ElMessage.success('已保存干预配置')
    await fetchData()
  } catch (e) {
    console.error(e)
  }
}

// 调度审核通过
const handleApprove = async (row) => {
  try {
    await auditSession(row.sessionId, 'approve', {
      sessionTitle: editForm.sessionTitle,
      sessionLocation: editForm.sessionLocation,
      startTime: editForm.timeRange?.[0],
      endTime: editForm.timeRange?.[1],
      capacity: editForm.capacity,
      description: editForm.description
    })
    ElMessage.success('已审核通过')
    sessionDialogVisible.value = false
    await fetchData()
  } catch (e) {
    console.error(e)
  }
}

const handleReject = (row) => {
  ElMessageBox.prompt('请输入打回重签的驳回理由', '驳回申请', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '驳回理由不能为空'
  }).then(({ value }) => {
    auditSession(row.sessionId, 'reject', { auditRemark: value }).then(async () => {
      ElMessage.success('活动已打回')
      sessionDialogVisible.value = false
      await fetchData()
    })
  }).catch(() => {})
}

const handleForceCancel = (row) => {
  ElMessageBox.confirm(`强制停办将取消所有已报名的学生名额并退回票务权重，您确定要强制停办该活动吗？`, '最高权限介入', { type: 'error' }).then(() => {
    auditSession(row.sessionId, 'cancel').then(async () => {
      ElMessage.success('活动已被管理员叫停并取消')
      sessionDialogVisible.value = false
      await fetchData()
    })
  }).catch(() => {})
}

onMounted(() => {
  if (route.query.companyName) {
    searchForm.companyName = route.query.companyName
  }
  fetchData()
})
</script>

<style scoped>
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
