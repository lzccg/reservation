<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">企业管理与资质审核</h2>
    </div>

    <el-card shadow="hover" class="app-card" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="企业全称">
          <el-input v-model="searchForm.keyword" placeholder="模糊搜索" clearable />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
            <el-option label="待审核" value="pending" />
            <el-option label="已入驻" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
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
        <el-table-column prop="uscc" label="统一社会信用代码" width="180" align="center" fixed="left" />
        <el-table-column prop="name" label="企业全称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="color: #409EFF; cursor: pointer; text-decoration: underline;" @click="handleViewDetail(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="所属行业" width="150" align="center" />
        <el-table-column prop="contactName" label="联系人" width="120" align="center" />
        <el-table-column prop="status" label="资质状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="List" @click="handleViewDetail(row)">
              {{ row.status === 'pending' ? '查看并审核' : '企业全息档案' }}
            </el-button>
            <el-button v-if="row.status === 'approved' || row.status === 'rejected'" size="small" type="danger" link @click="handleRemove(row)">注销企业</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 企业详情与审核大前台弹窗 -->
    <el-dialog title="企业资质档案中心" v-model="detailVisible" width="650px" destroy-on-close>
      <div v-if="currentCompany">
        <el-descriptions border :column="2" style="margin-bottom: 20px;">
          <el-descriptions-item label="企业全称" :span="2">
            <span style="font-weight: bold">{{ currentCompany.name }}</span>
            <el-tag :type="getStatusType(currentCompany.status)" size="small" style="margin-left: 10px">{{ getStatusName(currentCompany.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="社会信用代码" :span="2">{{ currentCompany.uscc }}</el-descriptions-item>
          <el-descriptions-item label="所属行业">{{ currentCompany.industry }}</el-descriptions-item>
          <el-descriptions-item label="企业所在地">{{ currentCompany.location || '（详细区域未填）' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentCompany.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentCompany.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="办公详细地址" :span="2">{{ currentCompany.address || '（暂无精确地址）' }}</el-descriptions-item>
        </el-descriptions>
        
        <el-alert v-if="currentCompany.status === 'pending'" type="warning" show-icon title="企业正在急待您的资质审查判定"></el-alert>
        <el-alert v-else-if="currentCompany.status === 'rejected'" type="error" show-icon :title="'该企业资质已经被驳回。您填写的历史备注：' + (currentCompany.rejectReason || '虚假伪造的空壳地址 / 企业代码对不上')"></el-alert>
      </div>

      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <el-button @click="handleViewSessions(currentCompany)" v-if="currentCompany?.status === 'approved'">查看该公司全部宣讲会</el-button>
          <div v-else></div> <!-- 占位推右 -->

          <div>
            <el-button @click="detailVisible = false">关闭面板</el-button>
            <template v-if="currentCompany?.status === 'pending'">
              <el-button type="danger" @click="handleAuditReject(currentCompany)">驳回资质申请</el-button>
              <el-button type="success" @click="handleAuditApprove(currentCompany)">确认合规入驻</el-button>
            </template>
            <template v-else-if="['approved', 'rejected'].includes(currentCompany?.status)">
              <el-button type="danger" plain @click="handleRemoveFromDialog(currentCompany)">注销撤销此企业</el-button>
            </template>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const searchForm = reactive({ keyword: '', status: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const currentCompany = ref(null)

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 1, uscc: '91440300729865614P', name: '腾讯科技（深圳）有限公司', industry: '计算机/互联网', location: '广东省深圳市南山区', address: '高新科技园南区腾讯大厦', contactName: '马化腾', contactPhone: '0755-86013388', status: 'approved' },
      { id: 2, uscc: '91330100799655058B', name: '阿里巴巴（中国）有限公司', industry: '计算机/互联网', location: '浙江省杭州市余杭区', address: '文一西路西溪园区', contactName: '马云', contactPhone: '0571-85022088', status: 'pending' },
      { id: 3, uscc: '91110000X12345678X', name: '某某皮包公司', industry: '其他', location: '北京市', address: '地下室130平米集中注册点', contactName: '李四', contactPhone: '13812341234', status: 'rejected' }
    ]
    page.total = 3
    loading.value = false
  }, 400)
}

const getStatusName = (status) => {
  const map = { pending: '待审核', approved: '已入驻', rejected: '已驳回' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const handleViewDetail = (row) => {
  currentCompany.value = row
  detailVisible.value = true
}

const handleAuditApprove = (row) => {
  ElMessageBox.confirm('您正在确认企业资质真实性并允许其入驻，确定吗？', '合规许可确认', { type: 'success' }).then(() => {
    ElMessage.success(`操作成功！企业 ${row.name} 已入驻！`)
    row.status = 'approved'
    detailVisible.value = false // 可以关闭弹窗
  }).catch(() => {})
}

const handleAuditReject = (row) => {
  ElMessageBox.prompt('请输入驳回该企业入驻资质的具体理由？(将展示给企业方看)', '资质驳回审批意见', {
    confirmButtonText: '狠心驳回',
    cancelButtonText: '撤销',
    inputPattern: /.+/,
    inputErrorMessage: '驳回理由必填绝不姑息'
  }).then(({ value }) => {
    ElMessage.success(`已驳回资质！理由附后：${value}`)
    row.status = 'rejected'
    row.rejectReason = value
    detailVisible.value = false
  }).catch(() => {})
}

const handleViewSessions = (row) => {
  detailVisible.value = false
  router.push({ path: '/admin/sessions', query: { companyName: row.name } })
}

const handleRemove = (row) => {
  ElMessageBox.confirm(`确定要在系统中注销企业 "${row.name}" 吗？此操作不可逆，将清除其所有宣讲会记录！`, '危险预警', { type: 'error' }).then(() => {
    ElMessage.success('已清算并注销该企业数据')
    fetchData()
  }).catch(() => {})
}

const handleRemoveFromDialog = (row) => {
  ElMessageBox.confirm(`确定要在系统中注销企业 "${row.name}" 吗？此操作不可逆，将清除其所有宣讲会记录！`, '危险预警', { type: 'error' }).then(() => {
    ElMessage.success('已清算并注销该企业数据')
    detailVisible.value = false
    fetchData()
  }).catch(() => {})
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
