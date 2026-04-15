package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Admin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.service.AdminService;
import com.iflytek.reservation.service.CompanyService;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/admins")
    public Result<?> listAdmins(HttpServletRequest request,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "10") long size) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "无权限访问");
        }
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.select("admin_id", "admin_name", "phone", "role", "status");
        if (keyword != null && !keyword.isBlank()) {
            qw.like("admin_name", keyword.trim());
        }
        qw.orderByAsc("role").orderByAsc("admin_id");
        Page<Admin> page = adminService.page(new Page<>(safeCurrent, safeSize), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @PostMapping("/admins")
    public Result<?> createAdmin(HttpServletRequest request, @RequestBody CreateAdminRequest body) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "仅超级管理员可新增管理员");
        }
        if (body == null) {
            return Result.error("参数错误");
        }
        String adminName = body.getAdminName();
        String password = body.getPassword();
        Integer role = body.getRole();
        if (adminName == null || adminName.isBlank() || password == null || password.isBlank() || role == null) {
            return Result.error("管理员名称、密码、角色级别为必填");
        }
        if (role != 1 && role != 2) {
            return Result.error("角色级别不合法");
        }
        Admin exists = adminService.getOne(new LambdaQueryWrapper<Admin>().eq(Admin::getAdminName, adminName.trim()), false);
        if (exists != null) {
            return Result.error("管理员名称已存在");
        }
        Admin admin = new Admin();
        admin.setAdminName(adminName.trim());
        admin.setPhone(body.getPhone());
        admin.setRole(role);
        admin.setStatus(1);
        admin.setPassword(passwordEncoder.encode(password));
        adminService.save(admin);
        return Result.success("新增成功");
    }

    @PutMapping("/admin/{id}/status")
    public Result<?> updateAdminStatus(HttpServletRequest request,
                                       @PathVariable("id") Long id,
                                       @RequestParam("status") Integer status) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "仅超级管理员可修改管理员状态");
        }
        if (id == null || status == null || (status != 0 && status != 1)) {
            return Result.error("参数错误");
        }
        Admin target = adminService.getById(id);
        if (target == null) {
            return Result.error("管理员不存在");
        }
        if (target.getRole() != null && target.getRole() == 1) {
            return Result.error(403, "不可修改其他超级管理员状态");
        }
        target.setStatus(status);
        adminService.updateById(target);
        return Result.success("操作成功");
    }

    @PostMapping("/admin/{id}/reset-password")
    public Result<?> resetAdminPassword(HttpServletRequest request, @PathVariable("id") Long id) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "仅超级管理员可重置管理员密码");
        }
        if (id == null) {
            return Result.error("参数错误");
        }
        Admin target = adminService.getById(id);
        if (target == null) {
            return Result.error("管理员不存在");
        }
        if (target.getRole() != null && target.getRole() == 1) {
            return Result.error(403, "不可重置其他超级管理员密码");
        }
        target.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        adminService.updateById(target);
        return Result.success("重置成功");
    }

    @PostMapping("/student/{id}/reset-password")
    public Result<?> resetStudentPassword(HttpServletRequest request, @PathVariable("id") Long id) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "无权限访问");
        }
        if (id == null) {
            return Result.error("参数错误");
        }
        Student student = studentService.getById(id);
        if (student == null) {
            return Result.error("学生不存在");
        }
        student.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        studentService.updateById(student);
        return Result.success("重置成功");
    }

    @PostMapping("/company/{id}/reset-password")
    public Result<?> resetCompanyPassword(HttpServletRequest request, @PathVariable("id") Long id) {
        Admin op = getCurrentAdmin(request);
        if (op == null) {
            return Result.error(401, "未登录");
        }
        if (op.getRole() == null || op.getRole() != 1) {
            return Result.error(403, "无权限访问");
        }
        if (id == null) {
            return Result.error("参数错误");
        }
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error("企业不存在");
        }
        company.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        companyService.updateById(company);
        return Result.success("重置成功");
    }

    private Admin getCurrentAdmin(HttpServletRequest request) {
        Long adminId = AuthTokenUtil.extractId(request);
        if (adminId == null) {
            return null;
        }
        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            return null;
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return null;
        }
        return admin;
    }

    public static class CreateAdminRequest {
        private String adminName;
        private String password;
        private String phone;
        private Integer role;

        public String getAdminName() {
            return adminName;
        }

        public void setAdminName(String adminName) {
            this.adminName = adminName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getRole() {
            return role;
        }

        public void setRole(Integer role) {
            this.role = role;
        }
    }
}

