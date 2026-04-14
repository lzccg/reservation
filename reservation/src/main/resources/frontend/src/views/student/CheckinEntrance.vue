<template>
  <div class="page-container">
    <div class="page-header">
      <div class="title-block">
        <h2 class="page-title">近期宣讲会</h2>
        <div class="page-subtitle">查阅即将开始的我已预约的相关活动，请在到达现场后配合管理员安排进行刷脸签到</div>
      </div>
    </div>

    <el-card shadow="hover" class="app-card">
      <template #header>
        <div class="card-header">
          <span>最近的相关宣讲会</span>
        </div>
      </template>

      <div v-if="latestSession" class="latest-session">
        <div class="latest-title">{{ latestSession.title }}</div>
        <div class="info-item">
          <el-icon><OfficeBuilding /></el-icon>
          <span>企业：{{ latestSession.companyName }}</span>
        </div>
        <div class="info-item">
          <el-icon><Clock /></el-icon>
          <span>时间：{{ latestSession.time }}</span>
        </div>
        <div class="info-item">
          <el-icon><Location /></el-icon>
          <span>地点：{{ latestSession.location }}</span>
        </div>

        <el-alert
          class="ready-alert"
          title="您已就绪，请听从现场管理员指示"
          description="现场会由工作人员提供扫脸终端，请排队核验入场"
          type="success"
          show-icon
          :closable="false"
        />
      </div>
      <el-empty v-else description="近期没有需要参加的宣讲会" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLatestReservedSession } from '@/api/student'

const latestSession = ref(null)

onMounted(() => {
  getLatestReservedSession().then((data) => {
    if (!data) {
      latestSession.value = null
      return
    }
    const start = normalizeDateTime(data.startTime)
    const end = normalizeDateTime(data.endTime)
    latestSession.value = {
      id: data.sessionId,
      companyName: data.companyName,
      title: data.sessionTitle,
      time: formatTimeRange(start, end),
      location: data.sessionLocation
    }
  }).catch(() => {
    latestSession.value = null
  })
})

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const formatTimeRange = (start, end) => {
  if (!start || !end) return '-'
  const date = start.slice(0, 10)
  const st = start.slice(11, 16)
  const ed = end.slice(0, 10)
  const et = end.slice(11, 16)
  if (date && date === ed) {
    return `${date} ${st} - ${et}`
  }
  return `${start.slice(0, 16)} - ${end.slice(0, 16)}`
}
</script>

<style scoped>
.title-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-subtitle {
  color: #909399;
  font-size: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.latest-session {
  padding: 8px 0;
}

.latest-title {
  color: #303133;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}

.info-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  color: #409EFF;
}

.ready-alert {
  margin-top: 16px;
}
</style>
