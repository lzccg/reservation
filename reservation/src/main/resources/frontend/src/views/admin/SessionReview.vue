<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">宣讲会审核</h2>
    </div>

    <el-card shadow="hover" class="app-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="待审核" name="pending"></el-tab-pane>
        <el-tab-pane label="已通过" name="approved"></el-tab-pane>
        <el-tab-pane label="已打回" name="rejected"></el-tab-pane>
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="companyName" label="企业名称" width="180" />
        <el-table-column prop="title" label="宣讲会标题" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="time" label="宣讲时间" width="180" />
        <el-table-column prop="location" label="宣讲地点" width="150" />
        <el-table-column prop="capacity" label="容纳人数" width="100" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right" v-if="activeTab === 'pending'">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleReview(row, 'approve')">通过</el-button>
            <el-button size="small" type="danger" @click="handleReview(row, 'reject')">打回</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right" v-else>
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 审核反馈弹窗 -->
    <el-dialog title="审核操作" v-model="reviewDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="企业:">{{ currentRow?.companyName }}</el-form-item>
        <el-form-item label="标题:">{{ currentRow?.title }}</el-form-item>
        <el-form-item label="通过状态:" v-if="reviewType === 'approve'">
          <el-tag type="success">准许发布</el-tag>
        </el-form-item>
        <el-form-item label="打回原因:" v-if="reviewType === 'reject'">
          <el-input type="textarea" v-model="rejectReason" rows="3" placeholder="请输入打回原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReview">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('pending')
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

// 模拟数据接口
const mockSessions = {
  pending: [
    { id: 101, companyName: '阿里巴巴', title: '2025届阿里巴巴校园招聘宣讲会', time: '2025-04-10 14:00', location: '大学生活动中心', capacity: 300 },
    { id: 102, companyName: '美团', title: '美团技术分享与秋招启动', time: '2025-04-12 18:30', location: '一教201', capacity: 150 }
  ],
  approved: [
    { id: 103, companyName: '腾讯', title: '腾讯2025校园招聘宣讲会', time: '2025-04-05 14:00', location: '体育馆', capacity: 800 }
  ],
  rejected: [
    { id: 104, companyName: '某某小公司', title: '宣讲会申请', time: '待定', location: '未确定', capacity: 50 }
  ]
}

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = mockSessions[activeTab.value]
    page.total = mockSessions[activeTab.value].length
    loading.value = false
  }, 400)
}

const handleTabClick = () => {
  page.current = 1
  fetchData()
}

// 审核逻辑
const reviewDialogVisible = ref(false)
const reviewType = ref('')
const rejectReason = ref('')
const currentRow = ref(null)

const handleReview = (row, type) => {
  currentRow.value = row
  reviewType.value = type
  rejectReason.value = ''
  reviewDialogVisible.value = true
}

const confirmReview = () => {
  if (reviewType.value === 'reject' && !rejectReason.value.trim()) {
    return ElMessage.warning('请输入打回原因')
  }
  reviewDialogVisible.value = false
  ElMessage.success(`宣讲会 "${currentRow.value.title}" 已${reviewType.value === 'approve' ? '通过' : '打回'}`)
  
  // 从未审核列表中移除模拟逻辑
  mockSessions.pending = mockSessions.pending.filter(i => i.id !== currentRow.value.id)
  mockSessions[reviewType.value === 'approve' ? 'approved' : 'rejected'].push(currentRow.value)
  fetchData()
}

const handleViewDetail = (row) => {
  ElMessage.info('展示宣讲会详情弹窗逻辑')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
