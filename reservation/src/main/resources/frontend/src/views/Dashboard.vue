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
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">学院预约分布</div></template>
            <div ref="companyChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">最新宣讲动态</div></template>
            <div class="latest-session-box">
              <h3>{{ latestCompanySession ? latestCompanySession.sessionTitle : '-' }}</h3>
              <p>
                状态：
                <el-tag :type="latestCompanySession ? mapSessionStatusType(latestCompanySession.sessionStatus) : 'info'">
                  {{ latestCompanySession ? latestCompanySession.statusText : '-' }}
                </el-tag>
              </p>
              <p>时间：{{ latestCompanySession ? formatDateTime(latestCompanySession.startTime) : '-' }}</p>
              <p>
                容量：
                <span v-if="latestCompanySession">
                  已预约 {{ latestCompanySession.reservedCount }} / 总容量 {{ latestCompanySession.capacity }}
                </span>
                <span v-else>-</span>
              </p>
            </div>
            
            <div style="margin-top: 20px; color: #606266; font-size: 14px;">
              <p>近期入驻宣讲高校榜单：</p>
              <p>1. 清华大学</p>
              <p>2. 北京大学</p>
              <p>3. 浙江大学</p>
              <p>4. 电子科技大学</p>
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
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}
.latest-session-box h3 { margin-top: 0; margin-bottom: 10px; color: #303133; }
.latest-session-box p { margin: 5px 0; color: #606266; font-size: 14px; }
</style>
