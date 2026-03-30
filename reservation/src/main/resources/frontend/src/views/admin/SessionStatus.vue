<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">全局宣讲会状态中心</h2>
    </div>

    <!-- 搜索筛选区 -->
    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="企业名称">
          <el-input v-model="searchForm.companyName" placeholder="相关企业" clearable />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="searchForm.status" placeholder="宣讲会状态" clearable style="width: 150px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
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
        <el-table-column prop="companyName" label="关联企业" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="宣讲会标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="time" label="申请档期" width="160" />
        <el-table-column prop="location" label="审核场地" width="150" show-overflow-tooltip />
        <el-table-column prop="capacity" label="申报总容纳" width="100" align="center" />
        <el-table-column prop="status" label="当前状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作指令" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="View" @click="handleViewSessionDialog(row)">
              详情与审核
            </el-button>
            <el-button size="small" type="warning" link icon="Monitor" v-if="row.status === 'published'" @click="openKiosk(row)">
              开启大屏
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 详情与综合审核面板（支持行内打回和强制修改） -->
    <el-dialog title="宣讲会全链路调度面板" v-model="sessionDialogVisible" width="650px" destroy-on-close>
      <div v-if="currentSession">
        <el-descriptions border :column="2" style="margin-bottom: 20px;">
          <el-descriptions-item label="企业方">{{ currentSession.companyName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusType(currentSession.status)">{{ getStatusName(currentSession.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="活动全称" :span="2">{{ currentSession.title }}</el-descriptions-item>
          <el-descriptions-item label="最初预约档期">{{ currentSession.time }}</el-descriptions-item>
          <el-descriptions-item label="最初期望容纳">{{ currentSession.capacity }}</el-descriptions-item>
          <el-descriptions-item label="内容说明短文" :span="2">这是一个校招集中大型选拔现场，包含笔试、初面以及高管面对面互动等核心环节... (模拟详细信息)</el-descriptions-item>
        </el-descriptions>

        <!-- 管理员直接在这个窗口覆盖系统资源表单 -->
        <h4 style="margin: 0 0 15px;">系统资源干预分配 (可强制覆盖修改)</h4>
        <el-form ref="editFormRef" :model="editForm" label-width="100px" size="small" border>
          <el-form-item label="活动标题" prop="title">
            <el-input v-model="editForm.title" />
          </el-form-item>
          <el-form-item label="审批场地" prop="location">
            <el-input v-model="editForm.location" />
          </el-form-item>
          <el-form-item label="审批档期" prop="time">
            <el-date-picker v-model="editForm.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="容纳核准席位" prop="capacity">
            <el-input-number v-model="editForm.capacity" :min="10" />
          </el-form-item>
          <el-form-item v-if="currentSession.status === 'pending' || editForm.forceUpdateRemarks">
            <el-button type="primary" plain @click="submitForceEdit">保存对表单项的强制干预配置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer" style="display: flex; justify-content: space-between;">
          <div>
            <el-button v-if="['approved', 'published'].includes(currentSession?.status)" type="danger" plain @click="handleForceCancel(currentSession)">强制叫停活动</el-button>
            <el-button v-if="currentSession?.status === 'published'" type="warning" plain icon="Monitor" @click="openKiosk(currentSession)" style="margin-left: 10px;">启动现场人脸签到屏</el-button>
          </div>

          <div>
            <el-button @click="sessionDialogVisible = false">关闭</el-button>
            <template v-if="currentSession?.status === 'pending'">
              <el-button type="danger" @click="handleReject(currentSession)">拒绝并打回</el-button>
              <el-button type="success" @click="handleApprove(currentSession)">基于当前干预表单通过</el-button>
            </template>
          </div>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const searchForm = reactive({ companyName: '', status: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

// 详情控制
const sessionDialogVisible = ref(false)
const currentSession = ref(null)
const editForm = reactive({})

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 201, companyName: '腾讯科技（深圳）有限公司', title: '腾讯2025校园招聘宣讲会', time: '2025-04-10 14:00:00', location: '大学生活动中心', capacity: 800, status: 'published' },
      { id: 202, companyName: '阿里巴巴（中国）有限公司', title: '阿里淘天秋招', time: '2025-04-20 18:30:00', location: '计算机大楼报告厅', capacity: 300, status: 'pending' },
      { id: 204, companyName: '字节跳动', title: '字节电商内推宣讲', time: '2024-03-10 10:00:00', location: '体育馆', capacity: 1000, status: 'ended' },
      { id: 206, companyName: '违规企业', title: '虚假职位宣讲会', time: '2025-04-15 14:00:00', location: '未定场地', capacity: 100, status: 'canceled' }
    ]
    page.total = 4
    loading.value = false
  }, 400)
}

const getStatusName = (status) => {
  const map = { pending: '待审场地', approved: '同意举办', published: '已发布', rejected: '被驳回', ended: '已结束', canceled: '已取消' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'primary', published: 'success', rejected: 'danger', ended: 'info', canceled: 'info' }
  return map[status] || 'info'
}

const handleViewSessionDialog = (row) => {
  currentSession.value = row
  // 将原版数据赋值进修改干预表单内
  Object.assign(editForm, {
    title: row.title,
    time: row.time,
    location: row.location,
    capacity: row.capacity,
    forceUpdateRemarks: false
  })
  sessionDialogVisible.value = true
}

// 保存强制配置
const submitForceEdit = () => {
  ElMessage.success('管理员覆盖修改本地暂存成功，审核时将使用此表数据！')
  editForm.forceUpdateRemarks = true // 标记修改
}

// 调度审核通过
const handleApprove = (row) => {
  ElMessage.success(`您已用场地[${editForm.location}]批准了这次通过调度请求！`)
  // 模拟将表单结果同步更新到行资源上
  row.title = editForm.title
  row.time = editForm.time
  row.location = editForm.location
  row.capacity = editForm.capacity
  
  row.status = 'approved'
  sessionDialogVisible.value = false
}

const handleReject = (row) => {
  ElMessageBox.prompt('请输入打回重签的驳回理由', '驳回申请', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '驳回理由不能为空'
  }).then(({ value }) => {
    ElMessage.success('活动已驳回！消息已通知企业。')
    row.status = 'rejected'
    row.remarks = value
    sessionDialogVisible.value = false
  }).catch(() => {})
}

const handleForceCancel = (row) => {
  ElMessageBox.confirm(`强制停办将取消所有已报名的学生名额并退回票务权重，您确定要强制停办该活动吗？`, '最高权限介入', { type: 'error' }).then(() => {
    ElMessage.success('系统拦截：活动已被强制停办关闭！')
    row.status = 'canceled'
    sessionDialogVisible.value = false
  }).catch(() => {})
}

const openKiosk = (row) => {
  const routeUrl = router.resolve({ path: '/kiosk/checkin', query: { sessionId: row.id } })
  window.open(routeUrl.href, '_blank')
}

onMounted(() => {
  if (route.query.companyName) {
    searchForm.companyName = route.query.companyName
  }
  fetchData()
})
</script>

<style scoped>
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
