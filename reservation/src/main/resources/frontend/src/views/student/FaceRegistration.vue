<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">人脸信息录入</h2>
    </div>

    <el-row :gutter="40">
      <el-col :span="12">
        <el-card shadow="hover" class="app-card camera-card">
          <template #header>
            <div class="camera-header">
              <span>视频采集</span>
              <el-tag :type="mediaStream ? 'success' : 'info'">
                {{ mediaStream ? '摄像头已开启' : '等待开启' }}
              </el-tag>
            </div>
          </template>
          
          <div class="video-container">
            <video ref="videoRef" autoplay playsinline class="face-video"></video>
            <div class="face-guide" v-if="mediaStream">
              <div class="guide-box"></div>
              <p class="guide-text">请将正脸对准取景框</p>
            </div>
          </div>
          <div class="agreement-section">
            <el-checkbox v-model="agreementChecked" class="prominent-checkbox">
              大屏核验条款：我同意系统将我的人脸特征加密存储于云端人脸库，仅用于宣讲会身份核验
            </el-checkbox>
          </div>

          <div class="action-buttons">
            <el-button type="primary" v-if="!mediaStream" @click="startCamera" :disabled="!agreementChecked">开启摄像头</el-button>
            <template v-else>
              <el-button type="success" icon="Camera" @click="captureImage" :loading="capturing" :disabled="!agreementChecked">拍摄照片</el-button>
              <el-button type="danger" @click="stopCamera">关闭摄像头</el-button>
            </template>

            <!-- 本地上传 -->
            <el-upload
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileUpload"
              accept="image/jpeg,image/png"
              style="margin-left: 15px;"
            >
              <el-button type="warning" icon="Upload" :disabled="!agreementChecked">上传本地人脸图片</el-button>
            </el-upload>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" class="app-card preview-card">
          <template #header>
            <div class="camera-header">
              <span>信息确认与上传</span>
              <el-tag :type="hasRegistered ? 'success' : 'warning'">
                {{ hasRegistered ? '已录入' : '未录入' }}
              </el-tag>
            </div>
          </template>

          <div class="preview-container">
            <img v-if="capturedImage" :src="capturedImage" class="preview-image" alt="Captured Face" />
            <el-empty v-else description="请先使用摄像头拍摄或上传面部照片" />
          </div>

          <div class="result-info" v-if="capturedImage">
            <el-alert
              :title="qualityText"
              :type="qualityText.includes('清晰') ? 'success' : 'warning'"
              show-icon
              :closable="false"
              style="margin-bottom: 20px"
            />
            <el-button type="primary" style="width: 100%" size="large" @click="uploadFaceData" :loading="uploading">
              确认上传人脸特征
            </el-button>
          </div>

          <div v-if="hasRegistered && !capturedImage" class="registered-hint">
            <el-icon size="60" color="#67c23a"><CircleCheckFilled /></el-icon>
            <p>您已成功录入过人脸信息，可正常进行签到。</p>
            <p>如需更新，请重新拍摄上传。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 隐藏的 canvas 用于抓图 -->
    <canvas ref="canvasRef" style="display: none;"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFaceStatus, uploadFaceInfo } from '@/api/checkin'

const videoRef = ref(null)
const canvasRef = ref(null)
const mediaStream = ref(null)

const capturedImage = ref(null)
const capturedFile = ref(null)
const capturing = ref(false)
const uploading = ref(false)
const qualityText = ref('检测中...')

const agreementChecked = ref(false)
const hasRegistered = ref(false)

const validateFile = (file) => {
  const maxSize = 5 * 1024 * 1024
  const name = file?.name || ''
  const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : ''
  const okExt = ['jpg', 'jpeg', 'png'].includes(ext)
  if (!okExt) {
    return { ok: false, message: '格式错误，仅支持 jpg、jpeg、png' }
  }
  if (!file?.size || file.size > maxSize) {
    return { ok: false, message: '大小限制，图片需小于5MB' }
  }
  return { ok: true }
}

const handleFileUpload = (file) => {
  if (!agreementChecked.value) {
    ElMessage.warning('务必先勾选同意隐私协议')
    return
  }
  const raw = file?.raw
  const check = validateFile(raw)
  if (!check.ok) {
    ElMessage.error(check.message)
    return
  }
  capturedFile.value = raw
  const reader = new FileReader()
  reader.onload = (e) => {
    capturedImage.value = e.target.result
    qualityText.value = '图片已就绪，请点击“确认上传人脸特征”进行检测'
  }
  reader.readAsDataURL(raw)
}

const startCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' }
    })
    mediaStream.value = stream
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
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
}

const captureImage = () => {
  if (!videoRef.value || !canvasRef.value) return
  if (!agreementChecked.value) {
    ElMessage.warning('务必先勾选同意隐私协议')
    return
  }
  capturing.value = true
  
  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  
  const base64Img = canvas.toDataURL('image/jpeg', 0.9)
  capturedImage.value = base64Img

  canvas.toBlob((blob) => {
    capturing.value = false
    if (!blob) {
      ElMessage.error('拍摄失败，请重试')
      return
    }
    const file = new File([blob], 'capture.jpg', { type: 'image/jpeg' })
    const check = validateFile(file)
    if (!check.ok) {
      ElMessage.error(check.message)
      capturedImage.value = null
      capturedFile.value = null
      return
    }
    capturedFile.value = file
    qualityText.value = '图片已就绪，请点击“确认上传人脸特征”进行检测'
  }, 'image/jpeg', 0.9)
}

const uploadFaceData = async () => {
  if (!agreementChecked.value) {
    ElMessage.warning('务必先勾选同意隐私协议')
    return
  }
  if (!capturedFile.value) {
    ElMessage.warning('请先拍摄或上传图片')
    return
  }
  uploading.value = true
  try {
    const res = await uploadFaceInfo(capturedFile.value)
    uploading.value = false
    ElMessage.success('人脸信息上传成功！')
    hasRegistered.value = true
    qualityText.value = `检测通过，质量分：${res?.qualityScore ?? '-'}`
    capturedImage.value = null
    capturedFile.value = null
  } catch (e) {
    uploading.value = false
    qualityText.value = e?.response?.data?.message || e?.message || '上传失败'
  }
}

onMounted(async () => {
  try {
    const res = await getFaceStatus()
    hasRegistered.value = !!res?.hasRegistered
  } catch (e) {
    hasRegistered.value = false
  }
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

.video-container {
  position: relative;
  width: 100%;
  height: 360px;
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
  transform: scaleX(-1); /* 镜像翻转 */
}

.face-guide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.guide-box {
  width: 200px;
  height: 200px;
  border: 2px dashed #409EFF;
  border-radius: 50%;
  box-shadow: 0 0 0 2000px rgba(0, 0, 0, 0.4);
}

.guide-text {
  color: #fff;
  margin-top: 20px;
  font-size: 16px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.8);
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.agreement-section {
  margin-top: 15px;
  background-color: #fdf6ec;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #faecd8;
}

.prominent-checkbox {
  color: #E6A23C;
  font-weight: bold;
  white-space: normal;
  display: flex;
  align-items: center;
}

.preview-container {
  height: 260px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px dashed #dcdfe6;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
  transform: scaleX(-1);
}

.registered-hint {
  text-align: center;
  padding: 40px 0;
  color: #606266;
}
</style>
