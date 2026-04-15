<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">全局统计报表</h2>
      <el-button type="success" icon="Download" @click="openExportDialog">导出Excel报表</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <!-- 搜索筛选 -->
      <el-form :inline="!isMobile" class="search-form">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="企业">
          <el-select v-model="companyId" placeholder="全部" clearable class="company-select">
            <el-option v-for="c in companyOptions" :key="c.companyId" :label="c.companyName" :value="c.companyId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询统计</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计数据汇总卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="statistic-item bg-blue">
            <div class="stat-name">宣讲会总场次</div>
            <div class="stat-num">{{ summary.sessionTotal }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="statistic-item bg-green">
            <div class="stat-name">总预约人次</div>
            <div class="stat-num">{{ summary.reservationTotal }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="statistic-item bg-orange">
            <div class="stat-name">实际签到人次</div>
            <div class="stat-num">{{ summary.checkinTotal }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="statistic-item bg-red">
            <div class="stat-name">整体签到率</div>
            <div class="stat-num">{{ summary.checkinRate }}%</div>
          </div>
        </el-col>
      </el-row>

      <!-- 详细统计列表 -->
      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          header-cell-class-name="table-header"
        >
          <el-table-column prop="companyName" label="企业名称" />
          <el-table-column prop="sessionTitle" label="宣讲会" min-width="180" show-overflow-tooltip/>
          <el-table-column prop="date" label="举办日期" width="120" />
          <el-table-column prop="reserveCount" label="预约人数" width="100" align="center" />
          <el-table-column prop="checkinCount" label="签到人数" width="100" align="center" />
          <el-table-column label="本场签到率" width="120" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.rate >= 90 ? '#67c23a' : row.rate < 60 ? '#f56c6c' : '#303133' }">
                {{ row.rate }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column label="详情" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleViewDetails(row)">签到明细</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 签到明细弹窗 -->
    <el-dialog :title="detailTitle" v-model="detailVisible" :width="detailDialogWidth" destroy-on-close>
      <div class="detail-toolbar">
        <el-radio-group v-model="detailFilterStatus" size="small" @change="handleDetailFilterChange">
          <el-radio-button label="all">全部名单</el-radio-button>
          <el-radio-button label="checked">正常签到</el-radio-button>
          <el-radio-button label="late">迟到入场</el-radio-button>
          <el-radio-button label="absent">缺席未签</el-radio-button>
        </el-radio-group>
        <el-button size="small" type="primary" icon="Download" @click="handleExportDetail">导出此场明细</el-button>
      </div>

      <el-table :data="detailRecords" border v-loading="detailLoading" height="420">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="college" label="学院" min-width="150" show-overflow-tooltip />
        <el-table-column prop="clazz" label="班级" width="120" />
        <el-table-column prop="checkinTimeText" label="签到时间" width="160" align="center" />
        <el-table-column prop="status" label="签到状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :class="getDetailStatusClass(row.status)" :type="getDetailStatusType(row.status)" effect="dark">
              {{ getDetailStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="detailTotal"
          :current-page="detailCurrent"
          :page-size="detailSize"
          @current-change="handleDetailPageChange"
        />
      </div>
    </el-dialog>

    <el-dialog title="导出Excel报表" v-model="exportVisible" :width="exportDialogWidth" destroy-on-close>
      <el-form :model="exportForm" label-width="90px">
        <el-form-item label="企业">
          <el-select v-model="exportForm.companyId" placeholder="请选择企业" style="width: 100%" @change="handleExportCompanyChange">
            <el-option v-for="c in exportCompanies" :key="c.companyId" :label="c.companyName" :value="c.companyId" />
          </el-select>
        </el-form-item>
        <el-form-item label="宣讲会">
          <el-select v-model="exportForm.sessionId" placeholder="请选择宣讲会" style="width: 100%" :disabled="!exportForm.companyId">
            <el-option v-for="s in exportSessions" :key="s.sessionId" :label="`${s.sessionTitle}（${s.timeRange}）`" :value="s.sessionId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" :disabled="!exportForm.sessionId" @click="confirmExport">
          导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/store/user'
import {
  getStatisticsCompanies,
  getStatisticsCompanySessions,
  getStatisticsSessionDetail,
  getStatisticsSessions,
  getStatisticsSummary
} from '@/api/admin'

const loading = ref(false)
const isMobile = ref(false)
const detailDialogWidth = ref('920px')
const exportDialogWidth = ref('520px')
const dateRange = ref([])
const companyId = ref(null)
const companyOptions = ref([])
const tableData = ref([])
const summary = ref({
  sessionTotal: 0,
  reservationTotal: 0,
  checkinTotal: 0,
  checkinRate: 0.0
})

const buildRangeParams = (withCompany = true) => {
  const startDate = dateRange.value && dateRange.value.length ? dateRange.value[0] : null
  const endDate = dateRange.value && dateRange.value.length ? dateRange.value[1] : null
  const params = {}
  if (startDate && endDate) {
    params.startDate = startDate
    params.endDate = endDate
  }
  if (withCompany && companyId.value) {
    params.companyId = companyId.value
  }
  return params
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = buildRangeParams(true)
    const rangeOnlyParams = buildRangeParams(false)
    const [s, list, companies] = await Promise.all([
      getStatisticsSummary(params),
      getStatisticsSessions(params),
      getStatisticsCompanies(rangeOnlyParams)
    ])
    summary.value = s || { sessionTotal: 0, reservationTotal: 0, checkinTotal: 0, checkinRate: 0.0 }
    tableData.value = (list || []).map((r) => ({
      ...r,
      reserveCount: Number(r.reserveCount || 0),
      checkinCount: Number(r.checkinCount || 0),
      rate: Number(r.rate || 0)
    }))
    companyOptions.value = companies || []
    if (companyId.value && !companyOptions.value.some((c) => c.companyId === companyId.value)) {
      companyId.value = null
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  dateRange.value = []
  companyId.value = null
  fetchData()
}

const detailVisible = ref(false)
const currentDetailSession = ref(null)
const detailLoading = ref(false)
const detailRecords = ref([])
const detailFilterStatus = ref('all')
const detailCurrent = ref(1)
const detailSize = ref(10)
const detailTotal = ref(0)
const detailTitle = ref('签到明细')

const handleViewDetails = (row) => {
  currentDetailSession.value = row
  detailFilterStatus.value = 'all'
  detailCurrent.value = 1
  detailVisible.value = true
  fetchDetailData()
}

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const fetchDetailData = async () => {
  if (!currentDetailSession.value) return
  detailLoading.value = true
  try {
    const params = {
      sessionId: currentDetailSession.value.sessionId,
      type: detailFilterStatus.value,
      current: detailCurrent.value,
      size: detailSize.value
    }
    const data = await getStatisticsSessionDetail(params)
    detailTitle.value = `${data.sessionTitle || ''} - 签到明细`
    detailRecords.value = (data.records || []).map((r) => {
      const s = normalizeDateTime(r.checkinTime)
      return { ...r, checkinTimeText: s ? s.slice(0, 16) : '-' }
    })
    detailTotal.value = Number(data.total || 0)
  } catch (e) {
    console.error(e)
  } finally {
    detailLoading.value = false
  }
}

const handleDetailFilterChange = () => {
  detailCurrent.value = 1
  fetchDetailData()
}

const handleDetailPageChange = (p) => {
  detailCurrent.value = p
  fetchDetailData()
}

const getDetailStatusName = (status) => {
  const map = { checked: '已签到', absent: '缺席', late: '迟到' }
  return map[status] || status
}

const getDetailStatusType = (status) => {
  const map = { checked: 'success', absent: 'info', late: 'warning' }
  return map[status] || 'info'
}

const getDetailStatusClass = (status) => {
  return status === 'absent' ? 'tag-absent' : ''
}

const userStore = useUserStore()

const extractFileName = (contentDisposition) => {
  if (!contentDisposition) return ''
  const v = String(contentDisposition)
  const m = v.match(/filename\*\=UTF-8''([^;]+)/i)
  if (m && m[1]) return decodeURIComponent(m[1])
  const m2 = v.match(/filename\=\"?([^\";]+)\"?/i)
  if (m2 && m2[1]) return m2[1]
  return ''
}

const handleExportDetail = () => {
  if (!currentDetailSession.value?.sessionId) return
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  axios.get(`${baseURL}/admin/statistics/session-detail/export`, {
    params: { sessionId: currentDetailSession.value.sessionId, includeCompany: 0 },
    responseType: 'blob',
    headers: { Authorization: `Bearer ${userStore.token}` }
  }).then((resp) => {
    const blob = new Blob([resp.data], { type: resp.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    const fileName = extractFileName(resp.headers['content-disposition']) || '签到明细.xlsx'
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  }).catch((e) => {
    console.error(e)
  })
}

const exportVisible = ref(false)
const exporting = ref(false)
const exportCompanies = ref([])
const exportSessions = ref([])
const exportForm = ref({
  companyId: null,
  sessionId: null
})

const openExportDialog = () => {
  exportVisible.value = true
  exportForm.value = { companyId: null, sessionId: null }
  exportSessions.value = []
  const params = buildRangeParams(false)
  getStatisticsCompanies(params).then((list) => {
    exportCompanies.value = list || []
  }).catch((e) => {
    console.error(e)
    exportCompanies.value = []
  })
}

const handleExportCompanyChange = (companyId) => {
  exportForm.value.sessionId = null
  exportSessions.value = []
  if (!companyId) return
  const params = { companyId, ...buildRangeParams(false) }
  getStatisticsCompanySessions(params).then((list) => {
    exportSessions.value = list || []
  }).catch((e) => {
    console.error(e)
    exportSessions.value = []
  })
}

const confirmExport = async () => {
  if (!exportForm.value.sessionId) return
  exporting.value = true
  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    const resp = await axios.get(`${baseURL}/admin/statistics/session-detail/export`, {
      params: { sessionId: exportForm.value.sessionId, includeCompany: 1 },
      responseType: 'blob',
      headers: { Authorization: `Bearer ${userStore.token}` }
    })
    const blob = new Blob([resp.data], { type: resp.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    const fileName = extractFileName(resp.headers['content-disposition']) || '签到明细.xlsx'
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
    exportVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    exporting.value = false
  }
}

const applyResponsive = () => {
  const w = window.innerWidth || 1200
  isMobile.value = w < 768
  detailDialogWidth.value = isMobile.value ? '96%' : '920px'
  exportDialogWidth.value = isMobile.value ? '96%' : '520px'
}

onMounted(() => {
  applyResponsive()
  window.addEventListener('resize', applyResponsive)
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', applyResponsive)
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.company-select {
  width: 220px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.statistic-item {
  padding: 20px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.stat-name {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}
.stat-num {
  font-size: 30px;
  font-weight: bold;
}
.bg-blue { background: linear-gradient(135deg, #409EFF, #a0cfff); }
.bg-green { background: linear-gradient(135deg, #67C23A, #b3e19d); }
.bg-orange { background: linear-gradient(135deg, #E6A23C, #f3d19e); }
.bg-red { background: linear-gradient(135deg, #F56C6C, #fab6b6); }

.detail-toolbar {
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag-absent {
  background-color: #303133 !important;
  border-color: #303133 !important;
  color: #fff !important;
}

:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}

@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .search-form :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .search-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .search-form :deep(.el-date-editor),
  .search-form :deep(.el-select),
  .search-form :deep(.el-input) {
    width: 100% !important;
  }

  .company-select {
    width: 100%;
  }

  .statistic-item {
    margin-bottom: 10px;
  }

  .stat-num {
    font-size: 24px;
  }

  .detail-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
}
</style>
