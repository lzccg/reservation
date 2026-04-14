<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">现场签到核验台</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <el-row :gutter="40">
      <el-col :span="14">
        <el-card shadow="hover" class="app-card camera-card">
          <template #header>
            <div class="camera-header">
              <span>人脸识别活体检测入口</span>
              <el-tag :type="mediaStream ? 'success' : 'info'">
                {{ mediaStream ? '采集中' : '待开启' }}
              </el-tag>
            </div>
          </template>
          
          <div class="video-container">
            <video ref="videoRef" autoplay playsinline class="face-video"></video>
            <!-- 扫描动画层 -->
            <div class="scan-layer" v-if="scanning">
              <div class="scan-line"></div>
            </div>
            <div class="face-guide" v-if="mediaStream">
              <div class="guide-box" :class="{'success-border': checkinSuccess}"></div>
            </div>
          </div>

          <div class="action-buttons">
            <el-button type="primary" size="large" v-if="!mediaStream" @click="startCamera" style="width: 200px">
              开启签到摄像头
            </el-button>
            <el-button type="danger" size="large" v-else @click="stopCamera" style="width: 200px">
              关闭摄像头
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="hover" class="app-card status-card">
          <div class="status-panel">
            <h3 class="session-name">{{ sessionInfo ? sessionInfo.sessionTitle : '宣讲会信息' }}</h3>

            <div v-if="sessionInfo" class="session-meta">
              <div class="meta-item">
                <el-icon><OfficeBuilding /></el-icon>
                <span>承办企业：{{ sessionInfo.companyName }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Clock /></el-icon>
                <span>活动时间：{{ formatTimeRange(sessionInfo.startTime, sessionInfo.endTime) }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Location /></el-icon>
                <span>举办场地：{{ sessionInfo.sessionLocation }}</span>
              </div>
            </div>
            
            <div class="verify-status">
              <div v-if="!scanning && !checkinSuccess" class="status-wait">
                <el-icon size="80" color="#909399"><Camera /></el-icon>
                <p>请看向摄像头进行人脸 1:N 搜索签到</p>
                <p class="sub-text">系统会在 reservation_face_date 人脸库中检索并完成签到写入</p>
              </div>

              <div v-else-if="scanning && !checkinSuccess" class="status-scanning">
                <el-icon size="80" class="is-loading" color="#409EFF"><Loading /></el-icon>
                <p>正在检索人脸...</p>
                <p class="sub-text">请保持面部在取景框内</p>
              </div>

              <div v-else-if="checkinSuccess" class="status-success">
                <el-icon size="80" color="#67C23A"><CircleCheckFilled /></el-icon>
                <p class="success-text">签到成功！</p>
                <p class="sub-text">身份验证通过，欢迎参加宣讲会</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 隐藏的 canvas 用于抓帧 -->
    <canvas ref="canvasRef" style="display: none;"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCheckinSessionInfo, verifyFaceCheckin } from '@/api/checkin'

const router = useRouter()
const route = useRoute()

const videoRef = ref(null)
const canvasRef = ref(null)
const mediaStream = ref(null)

const scanning = ref(false)
const checkinSuccess = ref(false)
const sessionId = ref(null)
const sessionInfo = ref(null)
let scanInterval = null

const normalizeDateTime = (v) => {
  if (!v) return ''
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ') : s
}

const formatTimeRange = (start, end) => {
  const s = normalizeDateTime(start)
  const e = normalizeDateTime(end)
  if (!s || !e) return '-'
  const date = s.slice(0, 10)
  const st = s.slice(11, 16)
  const ed = e.slice(0, 10)
  const et = e.slice(11, 16)
  if (date && date === ed) {
    return `${date} ${st} - ${et}`
  }
  return `${s.slice(0, 16)} - ${e.slice(0, 16)}`
}

const startCamera = async () => {
  checkinSuccess.value = false
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' }
    })
    mediaStream.value = stream
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
    
    // 开启摄像头后，自动定时截帧发送后端比对验证
    scanning.value = true
    startScanLoop()
  } catch (err) {
    ElMessage.error('无法访问摄像头，请检查权限设置')
    console.error(err)
  }
}

const stopCamera = () => {
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => track.stop())
    mediaStream.value = null
  }
  scanning.value = false
  clearInterval(scanInterval)
}

const startScanLoop = () => {
  if (!sessionId.value) {
    scanning.value = false
    return
  }
  scanInterval = setInterval(async () => {
    if (checkinSuccess.value) {
      return
    }
    const base64Img = captureAndValidate()
    if (!base64Img) {
      return
    }
    try {
      const data = await verifyFaceCheckin(sessionId.value, base64Img)
      if (data && data.success) {
        checkinSuccess.value = true
        scanning.value = false
        clearInterval(scanInterval)
        ElMessage.success(data.checkinStatus === 1 ? '迟到签到成功' : '签到成功')
      } else if (data && data.message) {
        scanning.value = false
        clearInterval(scanInterval)
        ElMessage.error(data.message)
      }
    } catch (e) {
    }
  }, 2000)
}

const captureAndValidate = () => {
  if (!videoRef.value || !canvasRef.value) return
  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/jpeg', 0.8)
}

onMounted(() => {
  const sid = route.query.sessionId
  sessionId.value = sid ? String(sid) : null
  if (!sessionId.value) {
    ElMessage.error('缺少 sessionId')
    return
  }
  getCheckinSessionInfo(sessionId.value).then((data) => {
    sessionInfo.value = data || null
  }).catch(() => {
    sessionInfo.value = null
  })
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-meta {
  margin: 12px 0 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.meta-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  color: #409EFF;
}

.video-container {
  position: relative;
  width: 100%;
  height: 400px;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.face-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.face-guide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  display: flex;
  justify-content: center;
  align-items: center;
}

.guide-box {
  width: 250px;
  height: 250px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  box-shadow: 0 0 0 2000px rgba(0, 0, 0, 0.6);
  transition: border-color 0.3s;
}

.success-border {
  border-color: #67C23A;
  border-width: 4px;
}

.scan-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(64, 158, 255, 0.1);
  pointer-events: none;
}

.scan-line {
  width: 100%;
  height: 4px;
  background: linear-gradient(to right, transparent, #409EFF, transparent);
  position: absolute;
  animation: scanning 2s linear infinite;
}

@keyframes scanning {
  0% { top: 10%; }
  50% { top: 90%; }
  100% { top: 10%; }
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.status-panel {
  text-align: center;
  padding: 20px 0;
}

.session-name {
  color: #303133;
  font-size: 20px;
  margin-bottom: 50px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #E4E7ED;
}

.verify-status {
  min-height: 250px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.verify-status p {
  margin-top: 20px;
  font-size: 18px;
  color: #303133;
}

.success-text {
  color: #67C23A !important;
  font-weight: bold;
  font-size: 24px !important;
}

.sub-text {
  font-size: 14px !important;
  color: #909399 !important;
  margin-top: 8px !important;
}
</style>
