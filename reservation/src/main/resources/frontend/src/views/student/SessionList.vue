<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
      <h2 class="page-title">宣讲会大厅</h2>
      <el-tag :type="canReserve ? 'success' : 'danger'" size="large" effect="dark" style="font-size: 16px;">
        当前账号状态：{{ canReserve ? '可以预约 (未发现违规记录)' : '禁止预约 (失信达到3次)' }}
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
          <el-button type="primary" icon="Search" @click="fetchData">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 宣讲会列表 -->
    <div v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in sessionList" :key="item.id" style="margin-bottom: 20px;">
          <el-card shadow="hover" class="session-card">
            <template #header>
              <div class="card-header">
                <span class="session-title">{{ item.title }}</span>
                <el-tag :type="item.available > 0 ? 'success' : 'danger'" size="small">
                  {{ item.available > 0 ? '可预约' : '名额已满' }}
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
                <span>{{ item.time }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ item.location }}</span>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>剩余名额: <strong :class="{'text-danger': item.available === 0}">{{ item.available }}</strong> / {{ item.capacity }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-button 
                type="primary" 
                style="width: 100%" 
                @click="handleReserve(item)" 
                :disabled="!canReserve || item.available === 0 || item.reserved"
              >
                {{ !canReserve ? '禁止预约' : (item.reserved ? '已预约' : (item.available === 0 ? '名额已满' : '立即预约')) }}
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const sessionList = ref([])
const dateRange = ref([])
const searchForm = reactive({ companyName: '' })
const page = reactive({ current: 1, size: 9, total: 0 })

// 模拟状态，可预约
const canReserve = ref(true)

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    sessionList.value = [
      { id: 201, companyName: '腾讯科技', title: '腾讯2025校园招聘宣讲会', time: '2025-04-10 14:00', location: '大学生活动中心', capacity: 800, available: 120, reserved: false },
      { id: 202, companyName: '阿里巴巴', title: '阿里淘天技术分享与秋招启动', time: '2025-04-12 18:30', location: '计算机大楼报告厅', capacity: 300, available: 5, reserved: false },
      { id: 203, companyName: '字节跳动', title: '字节跳动国际化电商专场', time: '2025-04-15 19:00', location: '教学楼A区201', capacity: 150, available: 0, reserved: false },
      { id: 204, companyName: '美团', title: '美团到店事业群宣讲会', time: '2025-04-18 14:00', location: '逸夫楼', capacity: 200, available: 180, reserved: true }
    ]
    page.total = 4
    loading.value = false
  }, 500)
}

const handleReserve = (item) => {
  if (!canReserve.value) {
    return ElMessage.error('您的账号已被管理员禁止预约操作！');
  }
  ElMessageBox.confirm(`确定要预约【${item.companyName}】的宣讲会吗？`, '预约确认', {
    confirmButtonText: '确定预约',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    loading.value = true
    // 模拟防抖与后端请求
    setTimeout(() => {
      item.reserved = true
      item.available--
      loading.value = false
      ElMessage.success('预约成功！请在宣讲会当天前往现场使用人脸签到。')
    }, 600)
  }).catch(() => {})
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
