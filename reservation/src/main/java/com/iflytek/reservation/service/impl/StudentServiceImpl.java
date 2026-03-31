package com.iflytek.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.StudentMapper;
import com.iflytek.reservation.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Student login(String username, String password) {
        Student student = getOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, username)
                .or()
                .eq(Student::getPhone, username), false);
        if (student != null && (student.getStatus() == null || student.getStatus() == 1) && passwordEncoder.matches(password, student.getPassword())) {
            return student;
        }
        return null;
    }

    @Override
    public boolean register(Student student) {
        Student existing = getOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, student.getStudentNo()), false);
        if (existing != null) {
            return false;
        }
        if (student.getPhone() != null && !student.getPhone().isBlank()) {
            Student phoneExisting = getOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getPhone, student.getPhone()), false);
            if (phoneExisting != null) {
                return false;
            }
        }
        // Set default values
        if (student.getStatus() == null) {
            student.setStatus(1);
        }
        // Encrypt password
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        return save(student);
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        Student student = getOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, username)
                .or()
                .eq(Student::getPhone, username), false);
        if (student == null) {
            return false;
        }
        if (!passwordEncoder.matches(oldPassword, student.getPassword())) {
            return false;
        }
        student.setPassword(passwordEncoder.encode(newPassword));
        return updateById(student);
    }
}
