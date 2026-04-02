<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
      <h2 class="page-title">宣讲会大厅</h2>
      <el-tag :type="canReserve ? 'success' : 'danger'" size="large" effect="dark" style="font-size: 16px;">
        当前账号状态：{{ studentStatusText }}
      </el-tag>
    </div>

    <!-- 搜索筛选区 -->
    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="企业名称">
          <el-input v-model="searchForm.companyName" placeholder="如: 腾讯, 阿里..." clearable />
        </el-form-item>
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
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 宣讲会列表 -->
    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in sessionList" :key="item.sessionId" style="margin-bottom: 20px;">
          <el-card shadow="hover" class="session-card">
            <template #header>
              <div class="card-header">
                <span class="session-title" style="cursor: pointer" @click="openDetail(item)">{{ item.sessionTitle }}</span>
                <el-tag :type="item.remainingSeats > 0 ? 'success' : 'danger'" size="small">
                  {{ item.remainingSeats > 0 ? '可预约' : '名额已满' }}
                </el-tag>
              </div>
            </template>
            <div class="session-info">
              <div class="info-item">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ item.companyName }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTimeRange(item.startTime, item.endTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ item.sessionLocation }}</span>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>剩余名额: <strong :class="{'text-danger': item.remainingSeats <= 0}">{{ item.remainingSeats }}</strong> / {{ item.capacity }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-button 
                type="primary" 
                style="width: 100%" 
                @click="handleReserve(item)" 
                :disabled="!canReserve || item.remainingSeats <= 0 || item.reserved"
              >
                {{ !canReserve ? '禁止预约' : (item.reserved ? '已预约' : (item.remainingSeats <= 0 ? '名额已满' : '立即预约')) }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="sessionList.length === 0" description="暂无开放的宣讲会" />
    </div>

    <div class="pagination-container" v-if="sessionList.length > 0">
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <el-dialog title="宣讲会详情" v-model="detailVisible" width="650px" destroy-on-close>
      <div v-if="detailSession">
        <el-descriptions border :column="2">
          <el-descriptions-item label="宣讲会标题" :span="2">{{ detailSession.sessionTitle }}</el-descriptions-item>
          <el-descriptions-item label="宣讲会企业" :span="2">{{ detailCompanyName }}</el-descriptions-item>
          <el-descriptions-item label="宣讲会时间" :span="2">{{ formatTimeRange(detailSession.startTime, detailSession.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="签到时间" :span="2">{{ formatTimeRange(detailCheckinStart, detailCheckinEnd) }}</el-descriptions-item>
          <el-descriptions-item label="宣讲会场地" :span="2">{{ detailSession.sessionLocation }}</el-descriptions-item>
          <el-descriptions-item label="剩余名额" :span="2">{{ detailSession.remainingSeats }} / {{ detailSession.capacity }}</el-descriptions-item>
          <el-descriptions-item label="详情描述" :span="2">{{ detailSession.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭面板</el-button>
        <el-button type="primary" :disabled="!canReserve || (detailSession?.remainingSeats ?? 0) <= 0 || detailReserved" @click="handleReserveFromDetail">
          {{ !canReserve ? '禁止预约' : ((detailSession?.remainingSeats ?? 0) <= 0 ? '名额已满' : (detailReserved ? '已预约' : '立即预约')) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getSessionList, reserveSession, getSessionDetail } from '@/api/student'

const loading = ref(false)
const sessionList = ref([])
const dateRange = ref([])
const searchForm = reactive({ companyName: '' })
const page = reactive({ current: 1, size: 9, total: 0 })

const userStore = useUserStore()
const canReserve = computed(() => userStore.userInfo?.status === 1)
const studentStatusText = computed(() => (userStore.userInfo?.status === 1 ? '正常' : '禁用'))

const handleSearch = () => {
  page.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.companyName = ''
  dateRange.value = []
  page.current = 1
  fetchData()
}

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
  const et = e.slice(11, 16)
  return `${date} ${st}-${et}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const startDate = dateRange.value?.[0]
    const endDate = dateRange.value?.[1]
    const data = await getSessionList({
      companyName: searchForm.companyName,
      startDate,
      endDate,
      current: page.current,
      size: page.size
    })
    sessionList.value = data.records || []
    page.total = data.total || 0
    if (data.studentStatus !== undefined && data.studentStatus !== null) {
      userStore.setUserInfo({ ...userStore.userInfo, status: data.studentStatus })
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleReserve = (item) => {
  if (!canReserve.value) {
    return ElMessage.error('您的账号状态为禁用，无法预约')
  }
  ElMessageBox.confirm(`确定要预约【${item.companyName}】的宣讲会吗？`, '预约确认', {
    confirmButtonText: '确定预约',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    loading.value = true
    reserveSession(item.sessionId).then(() => {
      ElMessage.success('预约成功！请在宣讲会当天前往现场使用人脸签到。')
      fetchData()
    }).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const detailVisible = ref(false)
const detailSession = ref(null)
const detailCompanyName = ref('-')
const detailReserved = ref(false)
const detailCheckinStart = computed(() => detailSession.value?.checkinStart)
const detailCheckinEnd = computed(() => detailSession.value?.checkinEnd)

const openDetail = async (item) => {
  try {
    const data = await getSessionDetail(item.sessionId)
    detailSession.value = data.session
    detailCompanyName.value = data.companyName || item.companyName || '-'
    detailReserved.value = !!item.reserved
    if (data.studentStatus !== undefined && data.studentStatus !== null) {
      userStore.setUserInfo({ ...userStore.userInfo, status: data.studentStatus })
    }
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleReserveFromDetail = () => {
  if (!detailSession.value) return
  handleReserve({
    sessionId: detailSession.value.sessionId,
    companyName: detailCompanyName.value
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.session-card {
  transition: all 0.3s;
  border-radius: 8px;
}
.session-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.session-title {
  font-weight: bold;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
.session-info {
  margin: 15px 0;
  color: #606266;
  font-size: 14px;
}
.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.info-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  color: #409EFF;
}
.card-footer {
  margin-top: 20px;
}
.text-danger {
  color: #F56C6C;
}
</style>
