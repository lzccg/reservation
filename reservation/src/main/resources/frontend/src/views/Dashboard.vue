<template>
  <div class="dashboard-container">
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
        <el-col :span="16">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">一周宣讲会签到趋势</div></template>
            <div ref="adminMainChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">各学院预约分布</div></template>
            <div ref="adminPieChartRef" style="height: 350px; width: 100%;"></div>
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
            <template #header><div class="card-header">岗位意向预约分析</div></template>
            <div ref="companyChartRef" style="height: 350px; width: 100%;"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="app-card">
            <template #header><div class="card-header">最新宣讲动态</div></template>
            <div class="latest-session-box">
              <h3>腾讯2025校园招聘宣讲会</h3>
              <p>状态：<el-tag type="success">已发布</el-tag></p>
              <p>时间：2025-04-10 14:00</p>
              <p>容量：800 / 已预约：650</p>
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

const userStore = useUserStore()
const role = computed(() => userStore.role)

// 管理员数据
const adminStatCards = [
  { title: '总注册人数', value: '12,500', icon: 'User', color: '#409EFF' },
  { title: '入驻企业数', value: '350', icon: 'OfficeBuilding', color: '#67C23A' },
  { title: '今日宣讲会', value: '12', icon: 'Calendar', color: '#E6A23C' },
  { title: '总签到人次', value: '45,210', icon: 'Histogram', color: '#F56C6C' }
]

// 学生数据
const studentStatCards = [
  { title: '已参加宣讲', value: '15', icon: 'DataBoard', color: '#409EFF' },
  { title: '即将参加', value: '2', icon: 'Timer', color: '#E6A23C' },
  { title: '近期开放场次', value: '45', icon: 'Ticket', color: '#67C23A' }
]

// 企业数据
const companyStatCards = [
  { title: '举办宣讲会总数', value: '18', icon: 'Position', color: '#409EFF' },
  { title: '累积关注预约人数', value: '4,520', icon: 'Star', color: '#F56C6C' },
  { title: '整体到场率', value: '89%', icon: 'DataLine', color: '#67C23A' }
]

const adminMainChartRef = ref(null)
const adminPieChartRef = ref(null)
const studentChartRef = ref(null)
const companyChartRef = ref(null)

let adminMainChart, adminPieChart, studentChart, companyChart = null

const initCharts = () => {
  if (role.value === 'admin' && adminMainChartRef.value) {
    adminMainChart = echarts.init(adminMainChartRef.value)
    adminMainChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value' },
      series: [{ data: [150, 230, 224, 218, 135, 147, 260], type: 'line', smooth: true, areaStyle: { opacity: 0.1 } }]
    })
    
    adminPieChart = echarts.init(adminPieChartRef.value)
    adminPieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%', left: 'center' },
      series: [{
        name: '人数分布', type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        data: [
          { value: 1048, name: '计软学院' }, { value: 735, name: '通信学院' },
          { value: 580, name: '经管学院' }, { value: 484, name: '机械学院' }
        ]
      }]
    })
  } else if (role.value === 'student' && studentChartRef.value) {
    studentChart = echarts.init(studentChartRef.value)
    studentChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['互联网/IT', '金融服务', '新能源制造', '医疗教育', '房地产'] },
      yAxis: { type: 'value' },
      series: [{
        name: '本校预约热度',
        type: 'bar',
        data: [820, 450, 560, 300, 150],
        itemStyle: { color: '#66b1ff', borderRadius: [4, 4, 0, 0] }
      }]
    })
  } else if (role.value === 'company' && companyChartRef.value) {
    companyChart = echarts.init(companyChartRef.value)
    companyChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['软件', '测试', '产品', '运营', '市场'] },
      yAxis: { type: 'value' },
      series: [{
        name: '预约意向人数',
        type: 'bar',
        data: [320, 150, 100, 80, 50],
        itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] }
      }]
    })
  }
}

const resizeHandler = () => {
  adminMainChart?.resize()
  adminPieChart?.resize()
  studentChart?.resize()
  companyChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    window.addEventListener('resize', resizeHandler)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  adminMainChart?.dispose()
  adminPieChart?.dispose()
  studentChart?.dispose()
  companyChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding-bottom: 20px;
}
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
