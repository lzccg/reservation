package com.iflytek.reservation.controller;

import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.common.IndustryUtil;
import com.iflytek.reservation.entity.Admin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.service.AdminService;
import com.iflytek.reservation.service.CompanyService;
import com.iflytek.reservation.service.StudentService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String role = loginRequest.getRole();

        Map<String, Object> response = new HashMap<>();
        response.put("role", role);

        if ("STUDENT".equalsIgnoreCase(role)) {
            Student student = studentService.login(username, password);
            if (student != null) {
                student.setPassword(null);
                response.put("user", student);
                response.put("needCompleteProfile", needCompleteStudentProfile(student));
                return Result.success(response);
            }
        } else if ("COMPANY".equalsIgnoreCase(role)) {
            Company company = companyService.login(username, password);
            if (company != null) {
                company.setPassword(null);
                response.put("user", company);
                response.put("needCompleteProfile", needCompleteCompanyProfile(company));
                return Result.success(response);
            }
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            Admin admin = adminService.login(username, password);
            if (admin != null) {
                response.put("adminRoleLevel", admin.getRole());
                admin.setPassword(null);
                response.put("user", admin);
                response.put("needCompleteProfile", false);
                return Result.success(response);
            }
        }

        return Result.error("用户名或密码错误");
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> params) {
        String role = (String) params.get("role");
        if ("STUDENT".equalsIgnoreCase(role)) {
            Student student = new Student();
            student.setStudentName((String) params.get("name"));
            student.setStudentNo((String) params.get("studentNo"));
            student.setPhone((String) params.get("phone"));
            student.setPassword((String) params.get("password"));
            boolean success = studentService.register(student);
            if (success) {
                return Result.success("注册成功");
            }
            return Result.error("学号或手机号已存在");
        } else if ("COMPANY".equalsIgnoreCase(role)) {
            String industry = IndustryUtil.normalize((String) params.get("industry"));
            if (industry == null) {
                return Result.error("所属行业不合法");
            }
            String email = (String) params.get("email");
            if (email == null || email.isBlank()) {
                return Result.error("请输入企业邮箱");
            }
            Company company = new Company();
            company.setCompanyName((String) params.get("name"));
            company.setCreditCode((String) params.get("creditCode"));
            company.setCompanyLocation((String) params.get("location"));
            company.setIndustry(industry);
            company.setContactName((String) params.get("contactName"));
            company.setContactPhone((String) params.get("contactPhone"));
            company.setEmail(email);
            company.setPassword((String) params.get("password"));
            boolean success = companyService.register(company);
            if (success) {
                return Result.success("注册成功");
            }
            return Result.error("社会信用代码、联系人电话或邮箱已存在");
        }
        return Result.error("未知的角色类型");
    }

    @PostMapping("/password/change")
    public Result<?> changePassword(@RequestBody PasswordChangeRequest request) {
        String role = request.getRole();
        if ("STUDENT".equalsIgnoreCase(role)) {
            boolean success = studentService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            if (success) {
                return Result.success("密码修改成功");
            }
            return Result.error("原密码错误或账号不存在");
        } else if ("COMPANY".equalsIgnoreCase(role)) {
            boolean success = companyService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            if (success) {
                return Result.success("密码修改成功");
            }
            return Result.error("原密码错误或账号不存在");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            boolean success = adminService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            if (success) {
                return Result.success("密码修改成功");
            }
            return Result.error("原密码错误或账号不存在");
        }
        return Result.error("未知的角色类型");
    }

    private boolean needCompleteStudentProfile(Student student) {
        return isBlank(student.getPhone())
                || isBlank(student.getCollege())
                || isBlank(student.getMajor())
                || isBlank(student.getClazz());
    }

    private boolean needCompleteCompanyProfile(Company company) {
        return isBlank(company.getCreditCode())
                || isBlank(company.getCompanyLocation())
                || isBlank(company.getIndustry())
                || isBlank(company.getContactName())
                || isBlank(company.getContactPhone())
                || isBlank(company.getEmail())
                || isBlank(company.getAddress());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String role;
    }

    @Data
    public static class PasswordChangeRequest {
        private String role;
        private String username;
        private String oldPassword;
        private String newPassword;
    }
}
