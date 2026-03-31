package com.iflytek.reservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.reservation.entity.Student;

public interface StudentService extends IService<Student> {
    Student login(String username, String password);
    boolean register(Student student);
    boolean changePassword(String username, String oldPassword, String newPassword);
}
