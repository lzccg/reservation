package com.iflytek.reservation.controller;

import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.IndustryUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long id = AuthTokenUtil.extractId(request);
        if (id == null) {
            return Result.error(401, "未登录");
        }
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "用户不存在");
        }
        company.setPassword(null);
        return Result.success(company);
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody CompanyUpdateRequest body) {
        Long id = AuthTokenUtil.extractId(request);
        if (id == null) {
            return Result.error(401, "未登录");
        }
        String industry = IndustryUtil.normalize(body.getIndustry());
        if (industry == null) {
            return Result.error("所属行业不合法");
        }
        Company update = new Company();
        update.setCompanyId(id);
        update.setCreditCode(body.getCreditCode());
        update.setCompanyLocation(body.getCompanyLocation());
        update.setIndustry(industry);
        update.setContactName(body.getContactName());
        update.setContactPhone(body.getContactPhone());
        update.setAddress(body.getAddress());
        update.setStatus(0);
        update.setAuditRemark(null);
        update.setAuditTime(null);
        boolean ok = companyService.updateById(update);
        if (!ok) {
            return Result.error("更新失败");
        }
        return Result.success("更新成功");
    }

    public static class CompanyUpdateRequest {
        private String creditCode;
        private String companyLocation;
        private String industry;
        private String contactName;
        private String contactPhone;
        private String address;

        public String getCreditCode() {
            return creditCode;
        }

        public void setCreditCode(String creditCode) {
            this.creditCode = creditCode;
        }

        public String getCompanyLocation() {
            return companyLocation;
        }

        public void setCompanyLocation(String companyLocation) {
            this.companyLocation = companyLocation;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}

