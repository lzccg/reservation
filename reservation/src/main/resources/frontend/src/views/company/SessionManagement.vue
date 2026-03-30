<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">宣讲会发布与管理</h2>
      <el-button type="primary" icon="Plus" @click="handlePublish">申请发布</el-button>
    </div>

    <!-- 顶端状态筛选过滤器 -->
    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="搜标题或地点" clearable />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="searchForm.status" placeholder="选择审查状态" clearable style="width: 180px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过(待发布)" value="approved" />
            <el-option label="已发布" value="published" />
            <el-option label="已结束" value="ended" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已取消" value="canceled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">综合筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="app-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        header-cell-class-name="table-header"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="宣讲会标题" min-width="220" show-overflow-tooltip/>
        <el-table-column prop="time" label="宣讲时间" width="160" />
        <el-table-column prop="location" label="地点" width="150" show-overflow-tooltip />
        <el-table-column label="座位登记" width="130" align="center">
          <template #default="{ row }">
            <div>总容纳: {{ row.capacity }}</div>
            <div style="color: #409EFF; font-size: 12px">剩余: {{ row.remainingSeats }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remarks" label="系统反馈与审查备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{'text-danger': row.status === 'rejected' || row.status === 'canceled'}">{{ row.remarks || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="Edit" :disabled="['published', 'ended', 'canceled'].includes(row.status)" @click="handleEdit(row)">修改</el-button>
            <el-button size="small" type="danger" link icon="Close" v-if="row.status === 'published' || row.status === 'approved'" @click="handleCancel(row)">取消举办</el-button>
            <el-button size="small" type="success" link icon="Position" v-if="row.status === 'approved'" @click="handleStart(row)">确认发布</el-button>
            <el-button size="small" type="warning" link icon="Monitor" v-if="row.status === 'published'" @click="openKiosk(row)">开启大屏</el-button>
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

    <!-- 发布/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="宣讲会标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入宣讲会标题" />
        </el-form-item>
        <el-form-item label="宣讲时间" prop="time">
          <el-date-picker
            v-model="form.time"
            type="datetime"
            placeholder="选择宣讲时间和日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="宣讲地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入场地名称等地点信息" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="10" :max="2000" />
        </el-form-item>
        <el-form-item label="宣讲内容" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入内容简介、岗位信息等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交审核</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({ keyword: '', status: '' })
const loading = ref(false)
const tableData = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

// 所有数据字典
const mockData = [
  { id: 201, title: '腾讯2025校园招聘宣讲会', time: '2025-04-10 14:00', location: '大学生活动中心', capacity: 800, remainingSeats: 150, status: 'published', remarks: '审核通过同意发布' },
  { id: 202, title: '腾讯前端技术专场分享', time: '2025-04-20 18:30', location: '计算机学院大楼报告厅', capacity: 200, remainingSeats: 200, status: 'pending', remarks: '待管理员审核' },
  { id: 203, title: '腾讯秋招补录说明会', time: '2025-05-01 19:00', location: '科技楼101', capacity: 150, remainingSeats: 150, status: 'approved', remarks: '审批通过，请及时发布' },
  { id: 204, title: '腾讯春招宣讲', time: '2024-03-10 10:00', location: '体育馆', capacity: 1000, remainingSeats: 0, status: 'ended', remarks: '宣讲会已结束' },
  { id: 205, title: '腾讯测试专场(内容违规)', time: '2025-06-11 10:00', location: '未定', capacity: 50, remainingSeats: 50, status: 'rejected', remarks: '内容不够详细，缺少岗位清单' },
  { id: 206, title: '临时取消的宣讲会', time: '2025-04-15 14:00', location: '二教302', capacity: 100, remainingSeats: 100, status: 'canceled', remarks: '企业主体自行取消举办' }
]

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    let result = mockData
    if (searchForm.status) {
      result = result.filter(item => item.status === searchForm.status)
    }
    if (searchForm.keyword) {
      result = result.filter(item => item.title.includes(searchForm.keyword) || item.location.includes(searchForm.keyword))
    }
    tableData.value = result
    page.total = result.length
    loading.value = false
  }, 400)
}

const getStatusName = (status) => {
  const map = { pending: '待审核', approved: '已通过(待发)', published: '已发布', rejected: '已打回', ended: '已结束', canceled: '已取消' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'primary', published: 'success', rejected: 'danger', ended: 'info', canceled: 'info' }
  return map[status] || 'info'
}

// 弹窗逻辑
const dialogVisible = ref(false)
const dialogTitle = ref('发布宣讲会')
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  title: '',
  time: '',
  location: '',
  capacity: 100,
  description: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }]
}

const handlePublish = () => {
  dialogTitle.value = '申请发布新宣讲会'
  Object.assign(form, { id: null, title: '', time: '', location: '', capacity: 100, description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '修改宣讲会信息'
  Object.assign(form, { ...row, description: '模拟返回描述' })
  dialogVisible.value = true
}

const handleStart = (row) => {
  ElMessage.success('已正式发布上线供学生预约！')
  row.status = 'published'
}

const openKiosk = (row) => {
  const router = useRouter()
  // 真实场景：在新标签页或直接跳转开启签到大屏
  const routeUrl = router.resolve({ path: '/kiosk/checkin', query: { sessionId: row.id } })
  window.open(routeUrl.href, '_blank')
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消举办该宣讲会吗？所有的学生预约将被重置！`, '高危操作提醒', { type: 'error' }).then(() => {
    ElMessage.success('已取消该宣讲会')
    row.status = 'canceled'
    row.remarks = '企业自行取消'
  }).catch(() => {})
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(valid => {
    if (valid) {
      submitLoading.value = true
      setTimeout(() => {
        submitLoading.value = false
        dialogVisible.value = false
        ElMessage.success('提交成功，请等待系统管理员审核。')
        fetchData()
      }, 500)
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.text-danger {
  color: #F56C6C;
}
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
