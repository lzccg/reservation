<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">学生管理</h2>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="年级">
          <el-select v-model="searchForm.grade" placeholder="选择年级" clearable>
            <el-option label="2021级" value="2021" />
            <el-option label="2022级" value="2022" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="searchForm.major" placeholder="选择专业" clearable>
            <el-option label="软件工程" value="Software" />
            <el-option label="计算机科学" value="CS" />
            <el-option label="通信工程" value="CE" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生姓名/学号">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">查询</el-button>
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
        <el-table-column prop="name" label="姓名" width="100" align="center" />
        <el-table-column prop="grade" label="年级" width="100" align="center" />
        <el-table-column prop="major" label="专业" width="140" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" width="100" align="center" />
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
        <el-descriptions-item label="姓名">{{ currentStudent.name }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentStudent.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ currentStudent.grade }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ currentStudent.major }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ currentStudent.className }}</el-descriptions-item>
        <el-descriptions-item label="手机号">138****0000</el-descriptions-item>
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
      </el-descriptions>
      
      <el-divider>近期活跃情况评估</el-divider>
      <p style="text-align: center; color: #606266; font-size: 14px;">
        累计预约 {{ currentStudent?.reserveCount }} 场，出勤率 {{ ((currentStudent?.checkinCount / currentStudent?.reserveCount) * 100).toFixed(1) || 0 }}%，其中迟到场次 {{ currentStudent?.lateCount }} 次。
      </p>

      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <div>
            <el-button v-if="currentStudent?.status === 1" type="danger" plain @click="handleStatusToggle(currentStudent)">封禁 / 限制功能</el-button>
            <el-button v-else type="success" @click="handleStatusToggle(currentStudent)">解封 / 恢复账号功能</el-button>
          </div>
          <el-button @click="detailVisible = false">关闭窗口</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const searchForm = reactive({ grade: '', major: '', keyword: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

// 弹窗控制
const detailVisible = ref(false)
const currentStudent = ref(null)

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 1, studentNo: '20210001', name: '李明', grade: '2021级', major: '软件工程', className: '软工1班', faceUploaded: true, reserveCount: 15, checkinCount: 14, lateCount: 0, absentCount: 1, status: 1 },
      { id: 2, studentNo: '20210002', name: '张华', grade: '2021级', major: '计算机科学', className: '计科2班', faceUploaded: false, reserveCount: 3, checkinCount: 1, lateCount: 1, absentCount: 2, status: 1 },
      { id: 3, studentNo: '20220005', name: '王强', grade: '2022级', major: '通信工程', className: '通信1班', faceUploaded: true, reserveCount: 8, checkinCount: 7, lateCount: 3, absentCount: 0, status: 1 },
      { id: 4, studentNo: '20210123', name: '赵信(封禁)', grade: '2021级', major: '软件工程', className: '软工3班', faceUploaded: true, reserveCount: 20, checkinCount: 10, lateCount: 2, absentCount: 10, status: 0 }
    ]
    page.total = 4
    loading.value = false
  }, 400)
}

const handleStatusChange = (row) => {
  ElMessage.success(`学号 ${row.studentNo} 的账号已${row.status === 1 ? '启用' : '禁用'}`)
}

const handleStatusToggle = (row) => {
  row.status = row.status === 1 ? 0 : 1;
  handleStatusChange(row);
}

const handleViewDetail = (row) => {
  currentStudent.value = row
  detailVisible.value = true
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
