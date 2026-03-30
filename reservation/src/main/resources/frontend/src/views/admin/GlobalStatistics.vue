<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">全局统计报表</h2>
      <el-button type="success" icon="Download" @click="handleExport">导出Excel报表</el-button>
    </div>

    <el-card shadow="hover" class="app-card">
      <!-- 搜索筛选 -->
      <el-form :inline="true" class="search-form">
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询统计</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计数据汇总卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="6">
          <div class="statistic-item bg-blue">
            <div class="stat-name">宣讲会总场次</div>
            <div class="stat-num">45</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistic-item bg-green">
            <div class="stat-name">总预约人次</div>
            <div class="stat-num">12,408</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistic-item bg-orange">
            <div class="stat-name">实际签到人次</div>
            <div class="stat-num">10,950</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistic-item bg-red">
            <div class="stat-name">整体签到率</div>
            <div class="stat-num">88.2%</div>
          </div>
        </el-col>
      </el-row>

      <!-- 详细统计列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
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
    </el-card>

    <!-- 签到明细弹窗 -->
    <el-dialog :title="`${currentDetailSession?.sessionTitle} - 签到明细`" v-model="detailVisible" width="800px" destroy-on-close>
      <div style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
        <el-radio-group v-model="detailFilterStatus" size="small" @change="filterDetailData">
          <el-radio-button label="all">全部名单</el-radio-button>
          <el-radio-button label="checked">正常签到</el-radio-button>
          <el-radio-button label="late">迟到入场</el-radio-button>
          <el-radio-button label="pending">缺席未签</el-radio-button>
        </el-radio-group>
        <el-button size="small" type="primary" icon="Download" @click="handleExportDetail">导出此场明细</el-button>
      </div>

      <el-table :data="filteredDetailData" border v-loading="detailLoading" height="400">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="major" label="专业" min-width="150" show-overflow-tooltip />
        <el-table-column prop="checkinTime" label="签到时间" width="130" align="center" />
        <el-table-column prop="status" label="签到状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getDetailStatusType(row.status)">{{ getDetailStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loadTable = ref(false)
const loading = ref(false)
const dateRange = ref([])
const tableData = ref([])

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { companyName: '腾讯', sessionTitle: '腾讯2025校园招聘宣讲会', date: '2025-03-20', reserveCount: 800, checkinCount: 780, rate: 97.5 },
      { companyName: '百度', sessionTitle: '百度AI技术分享', date: '2025-03-18', reserveCount: 400, checkinCount: 360, rate: 90.0 },
      { companyName: '字节跳动', sessionTitle: '字节跳动实习生宣讲', date: '2025-03-15', reserveCount: 500, checkinCount: 220, rate: 44.0 }
    ]
    loading.value = false
  }, 500)
}

const detailVisible = ref(false)
const currentDetailSession = ref(null)
const detailLoading = ref(false)
const detailData = ref([])
const filteredDetailData = ref([])
const detailFilterStatus = ref('all')

const handleViewDetails = (row) => {
  currentDetailSession.value = row
  detailFilterStatus.value = 'all'
  detailVisible.value = true
  fetchDetailData(row)
}

const fetchDetailData = (row) => {
  detailLoading.value = true
  setTimeout(() => {
    // 模拟丰满的明细数据
    const mockDb = [
      { studentName: '李嘉诚', studentNo: '20220412', major: '软件工程', checkinTime: '13:45:20', status: 'checked' },
      { studentName: '张梓涵', studentNo: '20220556', major: '计算机科学与技术', checkinTime: '13:50:11', status: 'checked' },
      { studentName: '王思聪', studentNo: '20220108', major: '物联网工程', checkinTime: '-', status: 'pending' },
      { studentName: '赵雨萱', studentNo: '20220993', major: '人工智能', checkinTime: '14:20:05', status: 'late' },
      { studentName: '陈子轩', studentNo: '20220334', major: '信息安全', checkinTime: '13:55:00', status: 'checked' },
      { studentName: '刘诗语', studentNo: '20220112', major: '数字媒体技术', checkinTime: '14:05:30', status: 'late' }
    ]
    let generate = []
    // 基于该宣讲会的数据容量，生成对应的假数据条目
    let count = Math.min(row.reserveCount, 50) 
    for(let i = 0; i < count; i++) {
        // 深拷贝加扰动
        let item = { ...mockDb[i % mockDb.length] }
        item.studentNo = (parseInt(item.studentNo) + i).toString()
        generate.push(item)
    }
    detailData.value = generate
    filterDetailData()
    detailLoading.value = false
  }, 400)
}

const filterDetailData = () => {
  if (detailFilterStatus.value === 'all') {
    filteredDetailData.value = detailData.value
  } else {
    filteredDetailData.value = detailData.value.filter(item => item.status === detailFilterStatus.value)
  }
}

const getDetailStatusName = (status) => {
  const map = { checked: '已签到', pending: '缺席', late: '迟到' }
  return map[status] || status
}

const getDetailStatusType = (status) => {
  const map = { checked: 'success', pending: 'info', late: 'warning' }
  return map[status] || 'info'
}

const handleExportDetail = () => {
  ElMessage.success(`正在导出 ${currentDetailSession.value?.sessionTitle} 的签到明细清单...`)
}

const handleExport = () => {
  ElMessage.success('正在导出系统签到数据统计 Excel...')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
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

:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
