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
            <el-option label="待审核" :value="0" />
            <el-option label="已入驻" :value="1" />
            <el-option label="已驳回" :value="2" />
            <el-option label="已注销" :value="3" />
          </el-select>
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
        <el-table-column prop="creditCode" label="统一社会信用代码" width="180" align="center" fixed="left" />
        <el-table-column prop="companyName" label="企业全称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="color: #409EFF; cursor: pointer; text-decoration: underline;" @click="handleViewDetail(row)">{{ row.companyName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="所属行业" width="150" align="center" />
        <el-table-column prop="contactName" label="联系人" width="120" align="center" />
        <el-table-column prop="status" label="资质状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" :effect="getStatusEffect(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link icon="List" @click="handleViewDetail(row)">
              {{ row.status === 0 ? '查看并审核' : '企业全息档案' }}
            </el-button>
            <el-button v-if="row.status === 1 || row.status === 2" size="small" type="danger" link @click="handleRemove(row)">注销企业</el-button>
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
            <span style="font-weight: bold">{{ currentCompany.companyName }}</span>
            <el-tag :type="getStatusType(currentCompany.status)" :effect="getStatusEffect(currentCompany.status)" size="small" style="margin-left: 10px">{{ getStatusName(currentCompany.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="社会信用代码" :span="2">{{ currentCompany.creditCode }}</el-descriptions-item>
          <el-descriptions-item label="所属行业">{{ currentCompany.industry }}</el-descriptions-item>
          <el-descriptions-item label="企业所在地">{{ currentCompany.companyLocation || '（详细区域未填）' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentCompany.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentCompany.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="企业邮箱">{{ currentCompany.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="办公详细地址" :span="2">{{ currentCompany.address || '（暂无精确地址）' }}</el-descriptions-item>
        </el-descriptions>
        
        <el-alert v-if="currentCompany.status === 0" type="warning" show-icon title="企业正在急待您的资质审查判定"></el-alert>
        <el-alert v-else-if="currentCompany.status === 2" type="error" show-icon :title="'该企业资质已经被驳回。您填写的历史备注：' + (currentCompany.auditRemark || '（无备注）')"></el-alert>
      </div>

      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <el-button @click="handleViewSessions(currentCompany)" v-if="currentCompany?.status === 1">查看该公司全部宣讲会</el-button>
          <div v-else></div> <!-- 占位推右 -->

          <div>
            <el-button @click="detailVisible = false">关闭面板</el-button>
            <template v-if="currentCompany?.status === 0">
              <el-button type="danger" @click="handleAuditReject(currentCompany)">驳回资质申请</el-button>
              <el-button type="success" @click="handleAuditApprove(currentCompany)">确认合规入驻</el-button>
            </template>
            <template v-else-if="[1, 2].includes(currentCompany?.status)">
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
import { getCompanyList, getCompanyDetail, auditCompany, revokeCompany } from '@/api/admin'

const router = useRouter()
const searchForm = reactive({ keyword: '', status: '' })
const tableData = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const detailVisible = ref(false)
const currentCompany = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getCompanyList({
      keyword: searchForm.keyword,
      status: searchForm.status === '' ? undefined : searchForm.status,
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

const getStatusName = (status) => {
  const map = { 0: '待审核', 1: '已入驻', 2: '已驳回', 3: '已注销' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

const getStatusEffect = (status) => {
  return status === 3 ? 'dark' : 'light'
}

const handleViewDetail = async (row) => {
  try {
    const data = await getCompanyDetail(row.companyId)
    currentCompany.value = data
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleAuditApprove = (row) => {
  ElMessageBox.confirm('您正在确认企业资质真实性并允许其入驻，确定吗？', '合规许可确认', { type: 'success' }).then(() => {
    auditCompany(row.companyId, 'approve').then(() => {
      ElMessage.success(`操作成功！企业 ${row.companyName} 已入驻！`)
      detailVisible.value = false
      fetchData()
    })
  }).catch(() => {})
}

const handleAuditReject = (row) => {
  ElMessageBox.prompt('请输入驳回该企业入驻资质的具体理由？(将展示给企业方看)', '资质驳回审批意见', {
    confirmButtonText: '狠心驳回',
    cancelButtonText: '撤销',
    inputPattern: /.+/,
    inputErrorMessage: '驳回理由必填绝不姑息'
  }).then(({ value }) => {
    auditCompany(row.companyId, 'reject', value).then(() => {
      ElMessage.success(`已驳回资质！理由附后：${value}`)
      detailVisible.value = false
      fetchData()
    })
  }).catch(() => {})
}

const handleViewSessions = (row) => {
  detailVisible.value = false
  router.push({ path: '/admin/sessions', query: { companyName: row.companyName, companyId: row.companyId } })
}

const handleRemove = (row) => {
  ElMessageBox.confirm(`确定要在系统中注销企业 "${row.companyName}" 吗？`, '危险预警', { type: 'error' }).then(() => {
    revokeCompany(row.companyId).then(() => {
      ElMessage.success('已注销该企业')
      fetchData()
    })
  }).catch(() => {})
}

const handleRemoveFromDialog = (row) => {
  ElMessageBox.confirm(`确定要在系统中注销企业 "${row.companyName}" 吗？`, '危险预警', { type: 'error' }).then(() => {
    revokeCompany(row.companyId).then(() => {
      ElMessage.success('已注销该企业')
      detailVisible.value = false
      fetchData()
    })
  }).catch(() => {})
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  page.current = 1
  fetchData()
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
