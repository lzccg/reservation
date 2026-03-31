package com.iflytek.reservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.reservation.entity.Admin;

public interface AdminService extends IService<Admin> {
    Admin login(String username, String password);
}
