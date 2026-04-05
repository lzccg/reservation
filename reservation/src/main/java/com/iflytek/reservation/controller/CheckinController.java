package com.iflytek.reservation.controller;

import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.service.FaceRegisterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private static final Logger log = LoggerFactory.getLogger(CheckinController.class);

    @Autowired
    private FaceRegisterService faceRegisterService;

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
}

