package com.iflytek.reservation.service;

import com.aliyun.facebody20191230.models.AddFaceEntityRequest;
import com.aliyun.facebody20191230.models.AddFaceAdvanceRequest;
import com.aliyun.facebody20191230.models.AddFaceRequest;
import com.aliyun.facebody20191230.models.CreateFaceDbRequest;
import com.aliyun.facebody20191230.models.DeleteFaceEntityRequest;
import com.aliyun.facebody20191230.models.DetectFaceAdvanceRequest;
import com.aliyun.facebody20191230.models.DetectFaceResponse;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.aliyun.tea.TeaException;
import com.iflytek.reservation.entity.FaceData;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.integration.aliyun.AliyunClients;
import com.iflytek.reservation.mapper.FaceDataMapper;
import com.iflytek.reservation.mapper.StudentMapper;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DataAccessException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FaceRegisterService {

    private static final Logger log = LoggerFactory.getLogger(FaceRegisterService.class);

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final double MIN_QUALITY_SCORE = 85.0;
    private static final String FACE_DB_NAME_ENV = "RESERVATION_FACE_DB_NAME";
    private static final String DEFAULT_FACE_DB_NAME = "reservation_face_date";
    private static final Map<String, Boolean> FACE_DB_READY = new ConcurrentHashMap<>();

    @Autowired
    private AliyunClients aliyunClients;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private FaceDataMapper faceDataMapper;

    public Map<String, Object> register(Long studentId, MultipartFile file) throws Exception {
        Student student = studentMapper.selectById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (student.getStatus() == null || student.getStatus() != 1) {
            throw new IllegalArgumentException("当前账号状态禁止操作");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = extLower(originalFilename);
        if (!isAllowedExt(ext)) {
            throw new IllegalArgumentException("格式错误，仅支持 jpg、jpeg、png");
        }
        if (file.getSize() <= 0 || file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("大小限制，图片需小于5MB");
        }

        byte[] bytes = file.getBytes();

        DetectFaceAdvanceRequest request = new DetectFaceAdvanceRequest()
                .setImageURLObject(new ByteArrayInputStream(bytes))
                .setLandmark(true)
                .setQuality(true)
                .setPose(true)
                .setMaxFaceNumber(30L);
        RuntimeOptions runtime = new RuntimeOptions();
        DetectFaceResponse resp = aliyunClients.faceClient().detectFaceAdvance(request, runtime);
        Integer faceCount = resp.getBody().getData().getFaceCount();

        if (faceCount == null || faceCount <= 0) {
            throw new IllegalArgumentException("请上传人脸图片");
        }
        if (faceCount > 1) {
            throw new IllegalArgumentException("请上传单人人脸图片");
        }

        Double score = firstQualityScore(resp);
        if (score == null) {
            throw new IllegalArgumentException("请上传人脸图片，请确保光线充足、无模糊。");
        }
        if (score < MIN_QUALITY_SCORE) {
            throw new IllegalArgumentException("请上传人脸图片，请确保光线充足、无模糊。");
        }

        String objectKey = "face_data/" + buildFileName(student.getStudentNo(), ext);
        OSS oss = aliyunClients.ossClient();
        try {
            oss.putObject(aliyunClients.ossBucket(), objectKey, new ByteArrayInputStream(bytes));
        } catch (OSSException e) {
            if ("AccessDenied".equals(e.getErrorCode()) && e.getMessage() != null
                    && e.getMessage().contains("must be addressed using the specified endpoint")) {
                throw new IllegalArgumentException("OSS Endpoint 配置错误：Bucket 所在地域与 endpoint 不一致，请将 endpoint 改为报错中的 Endpoint");
            }
            throw new IllegalArgumentException(e.getMessage() == null ? "OSS 上传失败" : e.getMessage());
        } catch (ClientException e) {
            throw new IllegalArgumentException(e.getMessage() == null ? "OSS 网络异常" : e.getMessage());
        }
        String faceUrl = "https://" + aliyunClients.ossBucket() + "." + aliyunClients.ossEndpointHost() + "/" + objectKey;

        String base64 = Base64.getEncoder().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();

        try {
            FaceData latest = faceDataMapper.selectOne(new QueryWrapper<FaceData>()
                    .eq("student_id", studentId)
                    .orderByDesc("create_time")
                    .last("limit 1"));
            if (latest == null) {
                FaceData faceData = new FaceData();
                faceData.setStudentId(studentId);
                faceData.setFaceFeature(base64);
                faceData.setFaceUrl(faceUrl);
                faceData.setCreateTime(now);
                faceDataMapper.insert(faceData);
            } else {
                latest.setFaceFeature(base64);
                latest.setFaceUrl(faceUrl);
                latest.setCreateTime(now);
                faceDataMapper.updateById(latest);
            }
        } catch (DataAccessException e) {
            try {
                oss.deleteObject(aliyunClients.ossBucket(), objectKey);
            } catch (Exception ignore) {
            }
            String msg = e.getMostSpecificCause() == null ? e.getMessage() : e.getMostSpecificCause().getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("data too long")) {
                throw new IllegalArgumentException("数据库字段 face_feature 容量不足，请将 face_data.face_feature 修改为 MEDIUMTEXT");
            }
            throw new IllegalArgumentException(msg == null ? "数据库写入失败" : msg);
        } catch (RuntimeException e) {
            try {
                oss.deleteObject(aliyunClients.ossBucket(), objectKey);
            } catch (Exception ignore) {
            }
            throw e;
        }

        syncToFaceDb(student, faceUrl, bytes);

        Map<String, Object> result = new HashMap<>();
        result.put("faceFeature", base64);
        result.put("faceUrl", faceUrl);
        result.put("qualityScore", score);
        return result;
    }

    public Map<String, Object> status(Long studentId) {
        FaceData latest = faceDataMapper.selectOne(new QueryWrapper<FaceData>()
                .eq("student_id", studentId)
                .orderByDesc("create_time")
                .last("limit 1"));
        Map<String, Object> result = new HashMap<>();
        result.put("hasRegistered", latest != null);
        result.put("faceUrl", latest == null ? null : latest.getFaceUrl());
        result.put("createTime", latest == null ? null : latest.getCreateTime());
        return result;
    }

    private boolean isAllowedExt(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext);
    }

    private String extLower(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String buildFileName(String studentNo, String ext) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        String uid = UUID.randomUUID().toString().replace("-", "");
        String base = (studentNo == null || studentNo.isBlank()) ? "student" : studentNo.trim();
        return base + "_" + ts + "_" + uid + "." + ext;
    }

    private Double firstQualityScore(DetectFaceResponse resp) {
        if (resp == null || resp.getBody() == null || resp.getBody().getData() == null) {
            return null;
        }
        if (resp.getBody().getData().getQualities() == null) {
            return null;
        }
        List<Float> scoreList = resp.getBody().getData().getQualities().getScoreList();
        if (scoreList == null || scoreList.isEmpty() || scoreList.get(0) == null) {
            return null;
        }
        return scoreList.get(0).doubleValue();
    }

    private void syncToFaceDb(Student student, String faceUrl, byte[] bytes) {
        if (student == null || student.getStudentId() == null || faceUrl == null || faceUrl.isBlank() || bytes == null || bytes.length == 0) {
            return;
        }

        String dbName = System.getenv(FACE_DB_NAME_ENV);
        if (dbName == null || dbName.isBlank()) {
            dbName = DEFAULT_FACE_DB_NAME;
        } else {
            dbName = dbName.trim();
        }

        String entityId = String.valueOf(student.getStudentId());
        String extraData = student.getStudentName() == null ? "" : student.getStudentName();
        RuntimeOptions runtime = new RuntimeOptions();

        ensureFaceDb(dbName, runtime);

        try {
            DeleteFaceEntityRequest del = new DeleteFaceEntityRequest()
                    .setDbName(dbName)
                    .setEntityId(entityId);
            aliyunClients.faceClient().deleteFaceEntityWithOptions(del, runtime);
        } catch (TeaException e) {
            log.warn("deleteFaceEntity failed, code={}, message={}, dbName={}, entityId={}", e.getCode(), e.getMessage(), dbName, entityId);
        } catch (Exception e) {
            log.warn("deleteFaceEntity failed, dbName={}, entityId={}", dbName, entityId, e);
        }

        try {
            AddFaceEntityRequest addFaceEntityRequest = new AddFaceEntityRequest()
                    .setDbName(dbName)
                    .setEntityId(entityId);
            aliyunClients.faceClient().addFaceEntityWithOptions(addFaceEntityRequest, runtime);
        } catch (TeaException e) {
            log.warn("addFaceEntity failed, code={}, message={}, dbName={}, entityId={}", e.getCode(), e.getMessage(), dbName, entityId);
        } catch (Exception e) {
            log.warn("addFaceEntity failed, dbName={}, entityId={}", dbName, entityId, e);
        }

        try {
            AddFaceRequest addFaceRequest = new AddFaceRequest()
                    .setDbName(dbName)
                    .setImageUrl(faceUrl)
                    .setEntityId(entityId)
                    .setExtraData(extraData);
            aliyunClients.faceClient().addFaceWithOptions(addFaceRequest, runtime);
        } catch (TeaException e) {
            if ("InvalidImage.Region".equals(e.getCode())) {
                try {
                    AddFaceAdvanceRequest addFaceAdvanceRequest = new AddFaceAdvanceRequest()
                            .setDbName(dbName)
                            .setEntityId(entityId)
                            .setExtraData(extraData)
                            .setImageUrlObject(new ByteArrayInputStream(bytes));
                    aliyunClients.faceClient().addFaceAdvance(addFaceAdvanceRequest, runtime);
                    return;
                } catch (TeaException e2) {
                    log.warn("addFaceAdvance failed, code={}, message={}, dbName={}, entityId={}, imageUrl={}", e2.getCode(), e2.getMessage(), dbName, entityId, faceUrl);
                } catch (Exception e2) {
                    log.warn("addFaceAdvance failed, dbName={}, entityId={}, imageUrl={}", dbName, entityId, faceUrl, e2);
                }
            }
            log.warn("addFace failed, code={}, message={}, dbName={}, entityId={}, imageUrl={}", e.getCode(), e.getMessage(), dbName, entityId, faceUrl);
        } catch (Exception e) {
            log.warn("addFace failed, dbName={}, entityId={}, imageUrl={}", dbName, entityId, faceUrl, e);
        }
    }

    private void ensureFaceDb(String dbName, RuntimeOptions runtime) {
        if (dbName == null || dbName.isBlank()) {
            return;
        }
        if (FACE_DB_READY.putIfAbsent(dbName, true) != null) {
            return;
        }
        try {
            CreateFaceDbRequest req = new CreateFaceDbRequest().setName(dbName);
            aliyunClients.faceClient().createFaceDbWithOptions(req, runtime);
        } catch (TeaException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("db has existed")) {
                return;
            }
            log.warn("createFaceDb failed, code={}, message={}, dbName={}", e.getCode(), msg, dbName);
            FACE_DB_READY.remove(dbName);
        } catch (Exception e) {
            log.warn("createFaceDb failed, dbName={}", dbName, e);
            FACE_DB_READY.remove(dbName);
        }
    }
}

