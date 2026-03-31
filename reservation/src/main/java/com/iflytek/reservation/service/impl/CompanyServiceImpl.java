package com.iflytek.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Company login(String username, String password) {
        Company company = getOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getContactPhone, username), false);
        if (company != null && passwordEncoder.matches(password, company.getPassword())) {
            return company;
        }
        return null;
    }

    @Override
    public boolean register(Company company) {
        Company existing = getOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getCreditCode, company.getCreditCode()), false);
        if (existing != null) {
            return false;
        }
        if (company.getContactPhone() != null && !company.getContactPhone().isBlank()) {
            Company phoneExisting = getOne(new LambdaQueryWrapper<Company>()
                    .eq(Company::getContactPhone, company.getContactPhone()), false);
            if (phoneExisting != null) {
                return false;
            }
        }
        // Set default values
        if (company.getStatus() == null) {
            company.setStatus(0);
        }
        // Encrypt password
        company.setPassword(passwordEncoder.encode(company.getPassword()));
        return save(company);
    }

    @Override
    public boolean changePassword(String contactPhone, String oldPassword, String newPassword) {
        Company company = getOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getContactPhone, contactPhone), false);
        if (company == null) {
            return false;
        }
        if (!passwordEncoder.matches(oldPassword, company.getPassword())) {
            return false;
        }
        company.setPassword(passwordEncoder.encode(newPassword));
        return updateById(company);
    }
}
