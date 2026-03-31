package com.iflytek.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.reservation.entity.Admin;
import com.iflytek.reservation.mapper.AdminMapper;
import com.iflytek.reservation.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Admin login(String username, String password) {
        Admin admin = getOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getAdminName, username));
        if (admin != null) {
            // Check if password matches (handling both BCrypt and plain text for initial default admin)
            if (passwordEncoder.matches(password, admin.getPassword()) || password.equals(admin.getPassword())) {
                return admin;
            }
        }
        return null;
    }
}
