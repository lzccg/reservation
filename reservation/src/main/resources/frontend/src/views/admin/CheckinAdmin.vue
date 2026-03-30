<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; align-items: center;">
      <div style="background-color: #ecf5ff; padding: 10px; border-radius: 8px; margin-right: 15px; display: flex;">
        <el-icon size="30" color="#409EFF"><Monitor /></el-icon>
      </div>
      <div>
        <h2 class="page-title" style="margin: 0;">现场签到终端控制台</h2>
        <p style="color: #909399; margin: 5px 0 0 0; font-size: 14px;">管理员专属：在此处为今日正在进行的宣讲会开启人脸识别签到大屏</p>
      </div>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-top: 20px;">
      <template #header>
        <div style="font-weight: bold; font-size: 16px;">今日可开启签到的宣讲会</div>
      </template>
      
      <el-table :data="todaySessions" border style="width: 100%" v-loading="loading">
        <el-table-column prop="time" label="活动时间" width="180" />
        <el-table-column prop="companyName" label="承办企业" width="220" show-overflow-tooltip />
        <el-table-column prop="title" label="宣讲会标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="location" label="举办场地" width="180" />
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

const router = useRouter()
const todaySessions = ref([])
const loading = ref(false)

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    // 模拟今日状态为已发布的宣讲会
    todaySessions.value = [
      { id: 201, companyName: '腾讯科技（深圳）有限公司', title: '腾讯2025校园招聘宣讲会', time: '2025-04-10 14:00:00', location: '大学生活动中心', status: 'published' },
      { id: 205, companyName: '华为技术有限公司', title: '华为2025届应届生招聘', time: '2025-04-10 16:30:00', location: '中心区大礼堂', status: 'published' }
    ]
    loading.value = false
  }, 400)
}

const handleOpenKiosk = (row) => {
  const routeUrl = router.resolve({ path: '/kiosk/checkin', query: { sessionId: row.id } })
  window.open(routeUrl.href, '_blank')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
