<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">宣讲会发布与管理</h2>
      <el-button type="primary" icon="Plus" :disabled="!canPublish" @click="handlePublish">申请发布</el-button>
    </div>

    <!-- 顶端状态筛选过滤器 -->
    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="搜标题或地点" clearable />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="searchForm.status" placeholder="选择审查状态" clearable style="width: 180px">
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
        <el-table-column prop="sessionTitle" label="宣讲会标题" min-width="220" show-overflow-tooltip/>
        <el-table-column label="宣讲时间" width="220">
          <template #default="{ row }">
            {{ formatTimeRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="sessionLocation" label="地点" width="180" show-overflow-tooltip />
        <el-table-column label="座位登记" width="130" align="center">
          <template #default="{ row }">
            <div>总容纳: {{ row.capacity }}</div>
            <div style="color: #409EFF; font-size: 12px">剩余: {{ row.remainingSeats }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.sessionStatus)">{{ getStatusName(row.sessionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remarks" label="系统反馈与审查备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{'text-danger': row.sessionStatus === 5 || row.sessionStatus === 4}">{{ getRemarks(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="View" @click="handleDetail(row)">宣讲会详情</el-button>
            <el-button
              v-if="[0, 1, 2, 5].includes(row.sessionStatus)"
              size="small"
              type="primary"
              link
              icon="Edit"
              :disabled="row.sessionStatus === 2"
              @click="handleEdit(row)"
            >修改</el-button>
            <el-button
              v-if="[0, 1, 2, 5].includes(row.sessionStatus)"
              size="small"
              type="danger"
              link
              icon="Close"
              @click="handleCancel(row)"
            >取消举办</el-button>
            <el-button
              v-if="row.sessionStatus === 1"
              size="small"
              type="success"
              link
              icon="Position"
              @click="handlePublishConfirm(row)"
            >确认发布</el-button>
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

    <!-- 发布/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="宣讲会标题" prop="sessionTitle">
          <el-input v-model="form.sessionTitle" placeholder="请输入宣讲会标题" />
        </el-form-item>
        <el-form-item label="宣讲日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            placeholder="选择开始时间"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            placeholder="选择结束时间"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="宣讲地点" prop="sessionLocation">
          <el-input v-model="form.sessionLocation" placeholder="请输入场地名称等地点信息" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="10" :max="2000" />
        </el-form-item>
        <el-form-item label="宣讲内容" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入内容简介、岗位信息等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交审核</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="宣讲会详情" v-model="detailVisible" width="650px" destroy-on-close>
      <div v-if="detailData">
        <el-descriptions border :column="2">
          <el-descriptions-item label="宣讲会标题" :span="2">{{ detailData.sessionTitle }}</el-descriptions-item>
          <el-descriptions-item label="宣讲时间" :span="2">{{ formatTimeRange(detailData.startTime, detailData.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="地点" :span="2">{{ detailData.sessionLocation }}</el-descriptions-item>
          <el-descriptions-item label="容纳人数">{{ detailData.capacity }}</el-descriptions-item>
          <el-descriptions-item label="剩余座位">{{ detailData.remainingSeats }}</el-descriptions-item>
          <el-descriptions-item label="宣讲内容" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 2" label="发布时间" :span="2">{{ formatDateTime(detailData.publishTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 3" label="发布时间" :span="2">{{ formatDateTime(detailData.publishTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 3" label="签到人数" :span="2">{{ detailCheckinCount }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 1" label="审核时间" :span="2">{{ formatDateTime(detailData.auditTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 5" label="审核时间" :span="2">{{ formatDateTime(detailData.auditTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 5" label="审核备注" :span="2">{{ detailData.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.sessionStatus === 4" label="取消原因" :span="2">{{ detailData.cancelReason || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { publishSession, updateSession, getCompanySessions, getSessionDetail, confirmPublishSession, cancelSession } from '@/api/company'
import { useUserStore } from '@/store/user'

const searchForm = reactive({ keyword: '', status: '' })
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

const userStore = useUserStore()
const canPublish = computed(() => userStore.userInfo?.status === 1)

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  page.current = 1
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getCompanySessions({
      keyword: searchForm.keyword,
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

const combineIsoDateTime = (date, time) => {
  if (!date || !time) return ''
  const d = String(date).trim()
  const t = String(time).trim()
  if (!d || !t) return ''
  if (t.length === 5) {
    return `${d}T${t}:00`
  }
  return `${d}T${t}`
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

const getRemarks = (row) => {
  const status = row.sessionStatus
  if (status === 2) return '审核通过同意发布'
  if (status === 0) return '待管理员审核'
  if (status === 1) return '已通过/已审核'
  if (status === 3) return '宣讲会已结束'
  if (status === 4) return row.cancelReason || '-'
  if (status === 5) return row.auditRemark || '-'
  return '-'
}

// 弹窗逻辑
const dialogVisible = ref(false)
const dialogTitle = ref('发布宣讲会')
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  sessionId: null,
  sessionTitle: '',
  date: '',
  startTime: '',
  endTime: '',
  sessionLocation: '',
  capacity: 100,
  description: ''
})

const rules = {
  sessionTitle: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  sessionLocation: [{ required: true, message: '请输入地点', trigger: 'blur' }]
}

const handlePublish = () => {
  if (!canPublish.value) {
    ElMessage.error('企业资质状态非已入驻，无法发布宣讲会')
    return
  }
  dialogTitle.value = '申请发布新宣讲会'
  Object.assign(form, { sessionId: null, sessionTitle: '', date: '', startTime: '', endTime: '', sessionLocation: '', capacity: 100, description: '' })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '修改宣讲会信息'
  try {
    const data = await getSessionDetail(row.sessionId)
    const s = data.session
    const startIso = normalizeIsoDateTime(s.startTime)
    const endIso = normalizeIsoDateTime(s.endTime)
    Object.assign(form, {
      sessionId: s.sessionId,
      sessionTitle: s.sessionTitle,
      date: startIso ? startIso.slice(0, 10) : '',
      startTime: startIso ? startIso.slice(11, 16) : '',
      endTime: endIso ? endIso.slice(11, 16) : '',
      sessionLocation: s.sessionLocation,
      capacity: s.capacity,
      description: s.description || ''
    })
    dialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleCancel = (row) => {
  if (row.sessionStatus === 0 || row.sessionStatus === 5) {
    ElMessageBox.confirm('确定取消举办该宣讲会吗？', '取消确认', { type: 'warning' }).then(async () => {
      await cancelSession(row.sessionId)
      ElMessage.success('已取消该宣讲会')
      fetchData()
    }).catch(() => {})
    return
  }
  ElMessageBox.prompt('请输入取消原因（将向学生公示）', '取消宣讲会', {
    confirmButtonText: '确认取消',
    cancelButtonText: '返回',
    inputPattern: /.+/,
    inputErrorMessage: '取消原因必填'
  }).then(async ({ value }) => {
    await cancelSession(row.sessionId, value)
    ElMessage.success('已取消该宣讲会')
    fetchData()
  }).catch(() => {})
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!canPublish.value) {
      ElMessage.error('企业资质状态非已入驻，无法发布宣讲会')
      return
    }
    submitLoading.value = true
    try {
      const startTime = combineIsoDateTime(form.date, form.startTime)
      const endTime = combineIsoDateTime(form.date, form.endTime)
      if (!startTime || !endTime) {
        ElMessage.error('宣讲时间不合法')
        return
      }
      if (startTime >= endTime) {
        ElMessage.error('结束时间必须晚于开始时间')
        return
      }
      const payload = {
        sessionTitle: form.sessionTitle,
        startTime,
        endTime,
        sessionLocation: form.sessionLocation,
        capacity: form.capacity,
        description: form.description
      }
      if (!form.sessionId) {
        await publishSession(payload)
      } else {
        await updateSession(form.sessionId, payload)
      }
      dialogVisible.value = false
      ElMessage.success('提交成功，请等待系统管理员审核。')
      fetchData()
    } catch (e) {
      console.error(e)
    } finally {
      submitLoading.value = false
    }
  })
}

const handlePublishConfirm = (row) => {
  ElMessageBox.confirm('确认发布后学生将可预约该宣讲会，确定吗？', '发布确认', { type: 'success' }).then(async () => {
    if (!canPublish.value) {
      ElMessage.error('企业资质状态非已入驻，无法发布宣讲会')
      return
    }
    await confirmPublishSession(row.sessionId)
    ElMessage.success('已正式发布上线供学生预约！')
    fetchData()
  }).catch(() => {})
}

const detailVisible = ref(false)
const detailData = ref(null)
const detailCheckinCount = ref(0)

const handleDetail = async (row) => {
  try {
    const data = await getSessionDetail(row.sessionId)
    detailData.value = data.session
    detailCheckinCount.value = data.checkinCount || 0
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.text-danger {
  color: #F56C6C;
}
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
