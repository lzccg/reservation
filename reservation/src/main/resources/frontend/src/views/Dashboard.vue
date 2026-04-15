<template>
  <div class="page-container">
    <!-- 管理员 Dashboard -->
    <template v-if="role === 'admin'">
      <el-row :gutter="20">
        <el-col :span="6" v-for="item in adminStatCards" :key="item.title">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-body">
              <div class="stat-icon-wrapper" :style="{ backgroundColor: item.color + '20', color: item.color }">
                <el-icon size="24"><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-title">{{ item.title }}</div>
                <div class="stat-value">{{ item.value }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">一周宣讲会预约趋势</div></template>
            <div ref="adminReserveChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">一周宣讲会签到趋势</div></template>
            <div ref="adminMainChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 学生 Dashboard (纯图表大屏) -->
    <template v-else-if="role === 'student'">
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in studentStatCards" :key="item.title">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-body">
              <div class="stat-icon-wrapper" :style="{ backgroundColor: item.color + '20', color: item.color }">
                <el-icon size="24"><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-title">{{ item.title }}</div>
                <div class="stat-value">{{ item.value }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row style="margin-top: 20px;">
        <el-col :span="24">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">校园招聘热度行业分布</div></template>
            <div ref="studentChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 企业 Dashboard (图表大屏及最新动态) -->
    <template v-else-if="role === 'company'">
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in companyStatCards" :key="item.title">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-body">
              <div class="stat-icon-wrapper" :style="{ backgroundColor: item.color + '20', color: item.color }">
                <el-icon size="24"><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-title">{{ item.title }}</div>
                <div class="stat-value">{{ item.value }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="16">
          <el-card shadow="hover" class="app-card panel-card">
            <template #header><div class="card-header">学院预约分布</div></template>
            <div ref="companyChartRef" class="company-chart"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="app-card panel-card latest-card">
            <template #header><div class="card-header">最新宣讲动态</div></template>
            <div class="latest-session-box">
              <div class="latest-top">
                <div class="latest-title">{{ latestCompanySession ? latestCompanySession.sessionTitle : '-' }}</div>
                <el-tag
                  :type="latestCompanySession ? mapSessionStatusType(latestCompanySession.sessionStatus) : 'info'"
                  effect="dark"
                >
                  {{ latestCompanySession ? latestCompanySession.statusText : '-' }}
                </el-tag>
              </div>

              <div class="latest-meta">
                <div class="meta-row">
                  <span class="meta-label">时间</span>
                  <span class="meta-value">{{ latestCompanySession ? formatDateTime(latestCompanySession.startTime) : '-' }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">容量</span>
                  <span class="meta-value" v-if="latestCompanySession">
                    已预约 {{ latestCompanySession.reservedCount }} / 总容量 {{ latestCompanySession.capacity }}
                  </span>
                  <span class="meta-value" v-else>-</span>
                </div>
              </div>

              <div class="latest-progress" v-if="latestCompanySession && latestCompanySession.capacity">
                <el-progress
                  :percentage="calcPercent(latestCompanySession.reservedCount, latestCompanySession.capacity)"
                  :stroke-width="10"
                  :show-text="false"
                />
                <div class="progress-hint">
                  <span>预约占比</span>
                  <span>{{ calcPercent(latestCompanySession.reservedCount, latestCompanySession.capacity) }}%</span>
                </div>
              </div>
            </div>
            
            <div style="margin-top: 20px; color: #606266; font-size: 14px;">
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'
import { getAdminCheckinTrend, getAdminDashboardSummary, getAdminReservationTrend } from '@/api/admin'
import { getCompanyCollegeDistribution, getCompanyDashboardSummary } from '@/api/company'
import { getStudentDashboardSummary, getStudentIndustryDistribution } from '@/api/student'

const userStore = useUserStore()
const role = computed(() => userStore.role)

// 管理员数据
const adminStatCards = ref([
  { title: '总注册人数', value: '-', icon: 'User', color: '#409EFF' },
  { title: '入驻企业数', value: '-', icon: 'OfficeBuilding', color: '#67C23A' },
  { title: '今日宣讲会', value: '-', icon: 'Calendar', color: '#E6A23C' },
  { title: '总签到人次', value: '-', icon: 'Histogram', color: '#F56C6C' }
])

// 学生数据
const studentStatCards = ref([
  { title: '已参加宣讲', value: '-', icon: 'DataBoard', color: '#409EFF' },
  { title: '即将参加', value: '-', icon: 'Timer', color: '#E6A23C' },
  { title: '近期开放场次', value: '-', icon: 'Ticket', color: '#67C23A' }
])

// 企业数据
const companyStatCards = ref([
  { title: '举办宣讲会总数', value: '-', icon: 'Position', color: '#409EFF' },
  { title: '累积关注预约人数', value: '-', icon: 'Star', color: '#F56C6C' },
  { title: '整体到场率', value: '-', icon: 'DataLine', color: '#67C23A' }
])

const adminMainChartRef = ref(null)
const adminReserveChartRef = ref(null)
const studentChartRef = ref(null)
const companyChartRef = ref(null)

let adminMainChart, adminReserveChart, studentChart, companyChart = null
const latestCompanySession = ref(null)

const fetchAdminDashboard = async () => {
  try {
    const [summary, checkinTrend, reservationTrend] = await Promise.all([
      getAdminDashboardSummary(),
      getAdminCheckinTrend(),
      getAdminReservationTrend()
    ])
    adminStatCards.value = [
      { title: '总注册人数', value: String(summary?.studentTotal ?? 0), icon: 'User', color: '#409EFF' },
      { title: '入驻企业数', value: String(summary?.companyTotal ?? 0), icon: 'OfficeBuilding', color: '#67C23A' },
      { title: '今日宣讲会', value: String(summary?.todaySessions ?? 0), icon: 'Calendar', color: '#E6A23C' },
      { title: '总签到人次', value: String(summary?.checkinTotal ?? 0), icon: 'Histogram', color: '#F56C6C' }
    ]
    if (adminMainChart && checkinTrend) {
      adminMainChart.setOption({
        xAxis: { type: 'category', data: checkinTrend.days || [] },
        series: [{ data: checkinTrend.values || [], type: 'line', smooth: true, areaStyle: { opacity: 0.1 } }]
      })
    }
    if (adminReserveChart && reservationTrend) {
      adminReserveChart.setOption({
        xAxis: { type: 'category', data: reservationTrend.days || [] },
        series: [{ data: reservationTrend.values || [], type: 'line', smooth: true, areaStyle: { opacity: 0.1 } }]
      })
    }
  } catch (e) {
    console.error(e)
  }
}

const initCharts = () => {
  if (role.value === 'admin' && adminMainChartRef.value && adminReserveChartRef.value) {
    adminMainChart = echarts.init(adminMainChartRef.value)
    adminMainChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ data: [], type: 'line', smooth: true, areaStyle: { opacity: 0.1 } }]
    })
    adminReserveChart = echarts.init(adminReserveChartRef.value)
    adminReserveChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ data: [], type: 'line', smooth: true, areaStyle: { opacity: 0.1 } }]
    })
  } else if (role.value === 'student' && studentChartRef.value) {
    studentChart = echarts.init(studentChartRef.value)
    studentChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{
        name: '本校预约热度',
        type: 'bar',
        data: [],
        itemStyle: { color: '#66b1ff', borderRadius: [4, 4, 0, 0] }
      }]
    })
  } else if (role.value === 'company' && companyChartRef.value) {
    companyChart = echarts.init(companyChartRef.value)
    companyChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%', left: 'center' },
      series: [{
        name: '预约人数',
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: []
      }]
    })
  }
}

const formatDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  const x = s.includes('T') ? s.replace('T', ' ') : s
  return x.length >= 16 ? x.slice(0, 16) : x
}

const mapSessionStatusType = (status) => {
  if (status === 2) return 'success'
  if (status === 3) return 'info'
  if (status === 4 || status === 5) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
}

const calcPercent = (reserved, capacity) => {
  const r = Number(reserved || 0)
  const c = Number(capacity || 0)
  if (!c || c <= 0) return 0
  const p = Math.round((r * 100) / c)
  return Math.max(0, Math.min(100, p))
}

const fetchCompanyDashboard = async () => {
  try {
    const [summary, dist] = await Promise.all([
      getCompanyDashboardSummary(),
      getCompanyCollegeDistribution()
    ])
    companyStatCards.value = [
      { title: '举办宣讲会总数', value: String(summary?.sessionTotal ?? 0), icon: 'Position', color: '#409EFF' },
      { title: '累积关注预约人数', value: String(summary?.reservationTotal ?? 0), icon: 'Star', color: '#F56C6C' },
      { title: '整体到场率', value: String(summary?.attendanceRate ?? 0) + '%', icon: 'DataLine', color: '#67C23A' }
    ]
    latestCompanySession.value = summary?.latestSession || null
    if (companyChart && dist) {
      companyChart.setOption({
        series: [{
          data: (dist || []).map((d) => ({ name: d.college, value: Number(d.count || 0) }))
        }]
      })
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchStudentDashboard = async () => {
  try {
    const [summary, dist] = await Promise.all([
      getStudentDashboardSummary(),
      getStudentIndustryDistribution()
    ])
    studentStatCards.value = [
      { title: '已参加宣讲', value: String(summary?.attended ?? 0), icon: 'DataBoard', color: '#409EFF' },
      { title: '即将参加', value: String(summary?.upcoming ?? 0), icon: 'Timer', color: '#E6A23C' },
      { title: '近期开放场次', value: String(summary?.openSessions ?? 0), icon: 'Ticket', color: '#67C23A' }
    ]
    if (studentChart && dist) {
      const x = (dist || []).map((d) => d.industry)
      const y = (dist || []).map((d) => Number(d.count || 0))
      studentChart.setOption({
        xAxis: { type: 'category', data: x },
        series: [{ data: y, type: 'bar' }]
      })
    }
  } catch (e) {
    console.error(e)
  }
}

const resizeHandler = () => {
  adminMainChart?.resize()
  adminReserveChart?.resize()
  studentChart?.resize()
  companyChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    if (role.value === 'admin') {
      fetchAdminDashboard()
    } else if (role.value === 'student') {
      fetchStudentDashboard()
    } else if (role.value === 'company') {
      fetchCompanyDashboard()
    }
    window.addEventListener('resize', resizeHandler)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  adminMainChart?.dispose()
  adminReserveChart?.dispose()
  studentChart?.dispose()
  companyChart?.dispose()
})
</script>

<style scoped>
.stat-card { border-radius: 8px; border: none; }
.stat-body { display: flex; align-items: center; }
.stat-icon-wrapper { width: 56px; height: 56px; border-radius: 12px; display: flex; justify-content: center; align-items: center; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.card-header { font-weight: 600; color: #303133; }

.latest-session-box {
  background: #f8fafc;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}
.latest-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.latest-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.latest-meta {
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.meta-row + .meta-row {
  border-top: 1px dashed #ebeef5;
}

.meta-label {
  color: #909399;
  font-size: 13px;
}

.meta-value {
  color: #303133;
  font-size: 13px;
  text-align: right;
  word-break: break-all;
}

.latest-progress {
  margin-top: 14px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.progress-hint {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.latest-card :deep(.el-card__body) {
  height: 100%;
}

.panel-card {
  height: 430px;
  display: flex;
  flex-direction: column;
}

.panel-card :deep(.el-card__header) {
  flex: 0 0 auto;
}

.panel-card :deep(.el-card__body) {
  flex: 1 1 auto;
  overflow: hidden;
}

.company-chart {
  width: 100%;
  height: 100%;
}

@media (max-width: 767px) {
  .panel-card {
    height: auto;
  }

  .latest-card :deep(.el-card__body) {
    height: auto;
  }

  .latest-session-box {
    height: auto;
  }
}
</style>
