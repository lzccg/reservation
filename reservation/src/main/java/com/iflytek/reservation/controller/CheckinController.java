package com.iflytek.reservation.controller;

import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.FaceSearchService;
import com.iflytek.reservation.service.FaceRegisterService;
import com.iflytek.reservation.service.KioskCheckinService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private static final Logger log = LoggerFactory.getLogger(CheckinController.class);

    @Autowired
    private FaceRegisterService faceRegisterService;

    @Autowired
    private FaceSearchService faceSearchService;

    @Autowired
    private KioskCheckinService kioskCheckinService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @GetMapping("/face-status")
    public Result<?> faceStatus(HttpServletRequest request) {
        Long studentId = AuthTokenUtil.extractId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(faceRegisterService.status(studentId));
    }

    @PostMapping(value = "/face-register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> faceRegister(HttpServletRequest request, @RequestPart("image") MultipartFile image) {
        Long studentId = AuthTokenUtil.extractId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        try {
            return Result.success(faceRegisterService.register(studentId, image));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("face-register failed, studentId={}", studentId, e);
            return Result.error(e.getMessage() == null || e.getMessage().isBlank() ? "上传失败" : e.getMessage());
        }
    }

    @PostMapping("/verify/{sessionId}")
    public Result<?> verify(@PathVariable("sessionId") Long sessionId, @RequestBody VerifyRequest body) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        if (sessionId == null) {
            result.put("message", "缺少 sessionId");
            return Result.success(result);
        }
        String image = body == null ? null : body.getImage();
        if (image == null || image.isBlank()) {
            result.put("message", "缺少图片");
            return Result.success(result);
        }
        try {
            byte[] bytes = decodeBase64Image(image);
            FaceSearchService.Match match = faceSearchService.searchTop(bytes);
            if (match == null || match.getScore() < 80.0) {
                return Result.success(result);
            }
            Long studentId;
            try {
                studentId = Long.parseLong(match.getEntityId());
            } catch (NumberFormatException e) {
                return Result.success(result);
            }
            Map<String, Object> checkinResult = kioskCheckinService.checkin(studentId, sessionId);
            checkinResult.put("score", match.getScore());
            return Result.success(checkinResult);
        } catch (IllegalArgumentException e) {
            result.put("message", e.getMessage());
            return Result.success(result);
        } catch (Exception e) {
            log.error("verify checkin failed, sessionId={}", sessionId, e);
            return Result.error("签到失败");
        }
    }

    @GetMapping("/session-info/{sessionId}")
    public Result<?> sessionInfo(@PathVariable("sessionId") Long sessionId) {
        if (sessionId == null) {
            return Result.error("缺少 sessionId");
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "宣讲会不存在");
        }
        Company company = session.getCompanyId() == null ? null : companyMapper.selectById(session.getCompanyId());
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getSessionId());
        data.put("sessionTitle", session.getSessionTitle());
        data.put("companyName", company == null ? "" : company.getCompanyName());
        data.put("startTime", session.getStartTime());
        data.put("endTime", session.getEndTime());
        data.put("checkinStart", session.getCheckinStart());
        data.put("checkinEnd", session.getCheckinEnd());
        data.put("sessionLocation", session.getSessionLocation());
        return Result.success(data);
    }

    private byte[] decodeBase64Image(String base64) {
        String s = base64.trim();
        int idx = s.indexOf("base64,");
        if (idx >= 0) {
            s = s.substring(idx + "base64,".length());
        }
        return Base64.getDecoder().decode(s);
    }

    public static class VerifyRequest {
        private String image;

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}

