<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; align-items: center;">
      <!-- 在标题处补充明显图标 -->
      <div style="background-color: #ecf5ff; padding: 10px; border-radius: 8px; margin-right: 15px; display: flex;">
        <el-icon size="30" color="#409EFF"><Clock /></el-icon>
      </div>
      <div>
        <h2 class="page-title" style="margin: 0;">近期宣讲会</h2>
        <p style="margin: 5px 0 0; color: #909399; font-size: 14px;">查阅即将开始的我已预约的相关活动，请在到达现场后配合管理员安排进行刷脸签到</p>
      </div>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header" style="display: flex; align-items: center">
          <span>最近的相关宣讲会</span>
        </div>
      </template>

      <div v-if="latestSession" class="latest-session-info">
        <h3>{{ latestSession.title }}</h3>
        <p><el-icon><OfficeBuilding /></el-icon> 企业：{{ latestSession.companyName }}</p>
        <p><el-icon><Clock /></el-icon> 时间：{{ latestSession.time }}</p>
        <p><el-icon><Location /></el-icon> 地点：{{ latestSession.location }}</p>

        <div style="margin-top: 30px;" class="action-box">
          <p class="ready-text"><el-icon color="#67C23A"><Filter /></el-icon> 您已就绪，请听从现场管理员指示</p>
          <p class="ready-subtext">现场会由工作人员提供扫脸终端，请排队核验入场</p>
        </div>
      </div>
      <el-empty v-else description="近期没有需要参加的宣讲会" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const latestSession = ref(null)

onMounted(() => {
  // 模拟获取最近的一场宣讲会
  setTimeout(() => {
    latestSession.value = { 
      id: 201, 
      companyName: '腾讯科技', 
      title: '腾讯2025校园招聘宣讲会', 
      time: '2025-04-10 14:00', 
      location: '大学生活动中心' 
    }
  }, 300)
})
</script>

<style scoped>
.latest-session-info {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}
.latest-session-info h3 {
  margin-top: 0;
  color: #303133;
}
.latest-session-info p {
  color: #606266;
  font-size: 15px;
  margin: 10px 0;
  display: flex;
  align-items: center;
}
.latest-session-info p .el-icon {
  margin-right: 8px;
  color: #409EFF;
}

.ready-text {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
.ready-subtext {
  font-size: 13px;
  color: #909399;
}
</style>
