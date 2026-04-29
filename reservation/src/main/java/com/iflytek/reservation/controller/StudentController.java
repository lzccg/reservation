package com.iflytek.reservation.controller;

import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long id = AuthTokenUtil.extractStudentId(request);
        if (id == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(id);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        student.setPassword(null);
        return Result.success(student);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody StudentUpdateRequest body) {
        Long id = AuthTokenUtil.extractStudentId(request);
        if (id == null) {
            return Result.error(401, "未登录");
        }
        Student existing = studentService.getById(id);
        if (existing == null || (existing.getStatus() != null && existing.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        Student update = new Student();
        update.setStudentId(id);
        update.setPhone(body.getPhone());
        update.setGender(body.getGender());
        update.setCollege(body.getCollege());
        update.setMajor(body.getMajor());
        update.setClazz(body.getClazz());
        boolean ok = studentService.updateById(update);
        if (!ok) {
            return Result.error("更新失败");
        }
        return Result.success("更新成功");
    }

    public static class StudentUpdateRequest {
        private String phone;
        private Integer gender;
        private String major;
        private String clazz;
        private String college;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getGender() {
            return gender;
        }

        public void setGender(Integer gender) {
            this.gender = gender;
        }

        public String getMajor() {
            return major;
        }

        public void setMajor(String major) {
            this.major = major;
        }

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public String getCollege() {
            return college;
        }

        public void setCollege(String college) {
            this.college = college;
        }
    }
}
