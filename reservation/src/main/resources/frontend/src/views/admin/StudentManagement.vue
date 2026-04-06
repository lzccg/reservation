<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">学生管理</h2>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="班级">
          <el-input v-model="searchForm.clazz" placeholder="输入班级" clearable />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="searchForm.major" placeholder="输入专业" clearable />
        </el-form-item>
        <el-form-item label="学生姓名/学号">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询</el-button>
          <el-button style="margin-left: 10px" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="app-card">
      <el-table 
        v-loading="loading" 
        :data="tableData" 
        border 
        stripe 
        style="width: 100%" 
        header-cell-class-name="table-header"
      >
        <el-table-column prop="studentNo" label="学号" width="120" align="center" fixed="left" />
        <el-table-column prop="studentName" label="姓名" width="100" align="center" />
        <el-table-column prop="major" label="专业" width="140" show-overflow-tooltip />
        <el-table-column prop="clazz" label="班级" width="100" align="center" />
        <el-table-column label="人脸录入状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.faceUploaded ? 'success' : 'danger'">
              {{ row.faceUploaded ? '已录入' : '未录入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参与统计" align="center">
          <el-table-column prop="reserveCount" label="预约次数" width="90" align="center" />
          <el-table-column prop="checkinCount" label="签到次数" width="90" align="center">
            <template #default="{ row }">
              <span style="color: #67C23A; font-weight: bold">{{ row.checkinCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lateCount" label="迟到次数" width="90" align="center">
            <template #default="{ row }">
              <span style="color: #E6A23C">{{ row.lateCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="absentCount" label="缺席次数" width="90" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.absentCount > 2 ? '#F56C6C' : 'inherit' }">
                {{ row.absentCount }}
              </span>
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="账号状态" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="View" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 学生详细信息弹窗 -->
    <el-dialog title="学生详细信息 (关联排单与系统数据)" v-model="detailVisible" width="600px" destroy-on-close>
      <el-descriptions border :column="2" v-if="currentStudent">
        <el-descriptions-item label="姓名">{{ currentStudent.studentName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentStudent.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ currentStudent.major }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ currentStudent.clazz }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentStudent.phone }}</el-descriptions-item>
        <el-descriptions-item label="人脸状态">
          <el-tag :type="currentStudent.faceUploaded ? 'success' : 'danger'" size="small">
            {{ currentStudent.faceUploaded ? '已建模并授权' : '无基准面部特征' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="违约(缺席)次数">
          <span style="color: #F56C6C; font-weight: bold">{{ currentStudent.absentCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="账号总状态" :span="2">
          <el-tag :type="currentStudent.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentStudent.status === 1 ? '功能正常启用' : '已限制功能 (如禁止预约等)' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="封禁日期">
          {{ formatDateTime(currentStudent.limitTime) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="解封日期">
          {{ formatDateTime(currentStudent.unbanTime) || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      
      <el-divider>近期活跃情况评估</el-divider>
      <p style="text-align: center; color: #606266; font-size: 14px;">
        累计预约 {{ currentStudent?.reserveCount }} 场，出勤率 {{ currentStudent?.reserveCount ? ((currentStudent?.checkinCount / currentStudent?.reserveCount) * 100).toFixed(1) : 0 }}%，其中迟到场次 {{ currentStudent?.lateCount }} 次。
      </p>

      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <div>
            <el-button v-if="currentStudent?.status === 1" type="danger" plain @click="handleStatusToggle(currentStudent)">封禁 / 限制功能</el-button>
            <el-button v-else type="success" @click="handleStatusToggle(currentStudent)">解封 / 恢复账号功能</el-button>
            <el-button v-if="isSuperAdmin" type="danger" @click="handleDeleteStudent">删除学生</el-button>
          </div>
          <el-button @click="detailVisible = false">关闭窗口</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getStudentList, toggleStudentStatus, getStudentDetail, deleteStudent } from '@/api/admin'

const searchForm = reactive({ clazz: '', major: '', keyword: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.userInfo?.adminRoleLevel === 1)

// 弹窗控制
const detailVisible = ref(false)
const currentStudent = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getStudentList({
      clazz: searchForm.clazz,
      major: searchForm.major,
      keyword: searchForm.keyword,
      current: page.current,
      size: page.size
    })
    tableData.value = data.records || []
    page.total = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (row) => {
  try {
    await toggleStudentStatus(row.studentId, row.status)
    ElMessage.success(`学号 ${row.studentNo} 的账号已${row.status === 1 ? '启用' : '禁用'}`)
    fetchData()
  } catch (e) {
    console.error(e)
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleStatusToggle = (row) => {
  row.status = row.status === 1 ? 0 : 1;
  handleStatusChange(row);
}

const handleViewDetail = async (row) => {
  try {
    const data = await getStudentDetail(row.studentId)
    currentStudent.value = data
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleReset = () => {
  searchForm.clazz = ''
  searchForm.major = ''
  searchForm.keyword = ''
  page.current = 1
  fetchData()
}

const handleDeleteStudent = async () => {
  if (!currentStudent.value) return
  if ((currentStudent.value.activeReservationCount || 0) > 0) {
    ElMessage.error('学生有正在进行的宣讲会，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该学生账号吗？删除后学生将无法登录与使用系统。', '删除确认', { type: 'warning' })
    await deleteStudent(currentStudent.value.studentId)
    ElMessage.success('删除成功')
    detailVisible.value = false
    currentStudent.value = null
    fetchData()
  } catch (e) {
    if (e && e.message) {
      console.error(e)
    }
  }
}

onMounted(() => {
  fetchData()
})

const formatDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  const norm = s.includes('T') ? s.replace('T', ' ') : s
  return norm.length >= 16 ? norm.slice(0, 16) : norm
}
</script>

<style scoped>
:deep(.table-header) {
  background-color: #f5f7fa !important;
  color: #606266;
}
</style>
