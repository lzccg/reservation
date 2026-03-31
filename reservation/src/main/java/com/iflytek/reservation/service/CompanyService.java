package com.iflytek.reservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.reservation.entity.Company;

public interface CompanyService extends IService<Company> {
    Company login(String username, String password);
    boolean register(Company company);
    boolean changePassword(String contactPhone, String oldPassword, String newPassword);
}
