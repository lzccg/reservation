<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">今日宣讲会</h2>
    </div>

    <el-card shadow="hover" class="app-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="companyName" label="企业名称" />
        <el-table-column prop="sessionTitle" label="宣讲会" min-width="180" show-overflow-tooltip />
        <el-table-column prop="timeRange" label="举办时间" width="210" />
        <el-table-column prop="reserveCount" label="预约人数" width="100" align="center" />
        <el-table-column prop="checkinCount" label="签到人数" width="100" align="center" />
        <el-table-column label="详情" width="110" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleViewDetails(row)">签到明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="detailTitle" v-model="detailVisible" width="980px" destroy-on-close>
      <div class="detail-toolbar">
        <el-radio-group v-model="detailFilterStatus" size="small" @change="handleDetailFilterChange">
          <el-radio-button label="all">全部名单</el-radio-button>
          <el-radio-button label="checked">正常签到</el-radio-button>
          <el-radio-button label="late">迟到入场</el-radio-button>
          <el-radio-button label="absent">缺席未签</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="detailRecords" border v-loading="detailLoading" height="440">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="college" label="学院" min-width="150" show-overflow-tooltip />
        <el-table-column prop="clazz" label="班级" width="120" />
        <el-table-column prop="checkinTimeText" label="签到时间" width="160" align="center" />
        <el-table-column prop="status" label="签到状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :class="getDetailStatusClass(row.status)" :type="getDetailStatusType(row.status)" effect="dark">
              {{ getDetailStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="detailFilterStatus === 'absent'" label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" link :loading="marking" @click="markChecked(row)">
              标记已签到
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStatisticsSessionDetail } from '@/api/admin'
import { getTodaySessions, markSessionStudentChecked } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const list = await getTodaySessions()
    tableData.value = (list || []).map((r) => ({
      ...r,
      reserveCount: Number(r.reserveCount || 0),
      checkinCount: Number(r.checkinCount || 0)
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
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
const marking = ref(false)

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

const handleViewDetails = (row) => {
  currentDetailSession.value = row
  detailFilterStatus.value = 'all'
  detailCurrent.value = 1
  detailVisible.value = true
  fetchDetailData()
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

const markChecked = async (row) => {
  if (!currentDetailSession.value?.sessionId || !row?.studentId) return
  marking.value = true
  try {
    await markSessionStudentChecked({ sessionId: currentDetailSession.value.sessionId, studentId: row.studentId })
    ElMessage.success('操作成功')
    fetchDetailData()
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    marking.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
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
</style>

