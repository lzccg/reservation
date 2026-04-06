<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">预约与签到名单</h2>
      <el-button type="success" icon="Download" @click="handleExport">导出签到数据</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属宣讲会">
          <el-select v-model="searchForm.sessionId" placeholder="请选择宣讲会" style="width: 250px" clearable @change="handleSearch">
            <el-option
              v-for="s in sessionOptions"
              :key="s.sessionId"
              :label="s.sessionTitle"
              :value="s.sessionId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生姓名/学号">
          <el-input v-model="searchForm.keyword" placeholder="输入姓名/学号" clearable />
        </el-form-item>
        <el-form-item label="签到状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已签到" value="checked" />
            <el-option label="未签到" value="unchecked" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
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
        <el-table-column prop="majorCollege" label="专业学院" min-width="170" show-overflow-tooltip />
        <el-table-column prop="clazz" label="班级" width="120" />
        <el-table-column prop="sessionTitle" label="预约宣讲会" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="reserveTime" label="预约时间" width="180" />
        <el-table-column prop="sessionTimeRange" label="宣讲会时间" width="210" />
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

    <el-dialog v-model="exportVisible" title="导出签到数据" width="520px">
      <el-form :model="exportForm" label-width="90px">
        <el-form-item label="宣讲会">
          <el-select v-model="exportForm.sessionId" placeholder="请选择宣讲会" style="width: 100%">
            <el-option
              v-for="s in sessionOptions"
              :key="s.sessionId"
              :label="s.sessionTitle"
              :value="s.sessionId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!exportForm.sessionId" :loading="exporting" @click="confirmExport">确认导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getCompanySessions, getCheckinRecords } from '@/api/company'

const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

const searchForm = reactive({
  sessionId: null,
  keyword: '',
  status: ''
})

const sessionOptions = ref([])

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const formatSessionTimeRange = (start, end) => {
  const s = normalizeDateTime(start)
  const e = normalizeDateTime(end)
  if (!s || !e) return '-'
  const date = s.slice(0, 10)
  const st = s.slice(11, 16)
  const ed = e.slice(0, 10)
  const et = e.slice(11, 16)
  if (date && date === ed) {
    return `${date} ${st} - ${et}`
  }
  return `${s.slice(0, 16)} - ${e.slice(0, 16)}`
}

const fetchSessions = async () => {
  const data = await getCompanySessions({ current: 1, size: 1000 })
  sessionOptions.value = data.records || []
  if (!searchForm.sessionId && sessionOptions.value.length > 0) {
    searchForm.sessionId = sessionOptions.value[0].sessionId
  }
}

const fetchData = async () => {
  if (!searchForm.sessionId) {
    tableData.value = []
    page.total = 0
    return
  }
  loading.value = true
  try {
    const data = await getCheckinRecords({
      sessionId: searchForm.sessionId,
      keyword: searchForm.keyword,
      status: searchForm.status,
      current: page.current,
      size: page.size
    })
    tableData.value = (data.records || []).map((r) => ({
      ...r,
      reserveTime: normalizeDateTime(r.reserveTime),
      sessionTimeRange: formatSessionTimeRange(r.startTime, r.endTime),
      checkinTime: normalizeDateTime(r.checkinTime)
    }))
    page.total = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.sessionId = sessionOptions.value?.[0]?.sessionId || null
  searchForm.keyword = ''
  searchForm.status = ''
  page.current = 1
  fetchData()
}

const exportVisible = ref(false)
const exporting = ref(false)
const exportForm = reactive({ sessionId: null })

const handleExport = () => {
  exportForm.sessionId = searchForm.sessionId
  exportVisible.value = true
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

const confirmExport = async () => {
  if (!exportForm.sessionId) return
  exporting.value = true
  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
    const resp = await axios.get(`${baseURL}/company/checkin-records/export`, {
      params: { sessionId: exportForm.sessionId },
      responseType: 'blob',
      headers: { Authorization: `Bearer ${userStore.token}` }
    })
    const blob = new Blob([resp.data], { type: resp.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    const fileName = extractFileName(resp.headers['content-disposition']) || '签到数据.xlsx'
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

onMounted(() => {
  fetchSessions().then(fetchData)
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
