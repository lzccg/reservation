<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">现场签到终端控制台</h2>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-top: 20px;">
      <template #header>
        <div style="font-weight: bold; font-size: 16px;">今日可开启签到的宣讲会</div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="todaySessions"
        border
        stripe
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="timeRange" label="活动时间" width="210" />
        <el-table-column prop="companyName" label="承办企业" width="220" show-overflow-tooltip />
        <el-table-column prop="sessionTitle" label="宣讲会标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="sessionLocation" label="举办场地" width="180" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
             <el-button type="success" size="large" icon="Camera" @click="handleOpenKiosk(row)">
               开启刷脸终端
             </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTodayCheckinSessions } from '@/api/admin'

const router = useRouter()
const todaySessions = ref([])
const loading = ref(false)

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
  const ed = e.slice(0, 10)
  const et = e.slice(11, 16)
  if (date && date === ed) {
    return `${date} ${st} - ${et}`
  }
  return `${s.slice(0, 16)} - ${e.slice(0, 16)}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getTodayCheckinSessions()
    todaySessions.value = (data || []).map((s) => ({
      ...s,
      timeRange: formatTimeRange(s.startTime, s.endTime)
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleOpenKiosk = (row) => {
  const routeUrl = router.resolve({ path: '/kiosk/checkin', query: { sessionId: row.sessionId } })
  window.open(routeUrl.href, '_blank')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
</style>
