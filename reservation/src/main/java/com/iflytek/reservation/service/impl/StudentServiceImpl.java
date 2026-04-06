package com.iflytek.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.StudentMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Checkin;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iflytek.reservation.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Override
    public Student login(String username, String password) {
        Student student = getOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, username)
                .or()
                .eq(Student::getPhone, username), false);
        if (student != null && (student.getStatus() == null || student.getStatus() == 1 || student.getStatus() == 0) && passwordEncoder.matches(password, student.getPassword())) {
            refreshStudentStatus(student);
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

    @Override
    public void refreshStudentStatus(Student student) {
        if (student == null || student.getStatus() == null || student.getStatus() == 2) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        // 1. 自动解封逻辑
        if (student.getStatus() == 0 && student.getLimitTime() != null) {
            if (student.getLimitTime().plusDays(7).isBefore(now) || student.getLimitTime().plusDays(7).isEqual(now)) {
                student.setStatus(1);
                student.setUnbanTime(now); // 记录本次解封时间
                student.setLimitTime(null);
                update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Student>()
                        .eq("student_id", student.getStudentId())
                        .set("status", 1)
                        .set("limit_time", null)
                        .set("unban_time", now));
            }
        }

        // 2. 自动封禁逻辑
        if (student.getStatus() == 1) {
            QueryWrapper<Reservation> rw = new QueryWrapper<Reservation>()
                    .eq("student_id", student.getStudentId())
                    .eq("reservation_status", 3);
            if (student.getUnbanTime() != null) {
                rw.gt("reservation_time", student.getUnbanTime());
            }
            long noShows = reservationMapper.selectCount(rw);

            QueryWrapper<Checkin> cw = new QueryWrapper<Checkin>()
                    .eq("student_id", student.getStudentId())
                    .in("checkin_status", 1, 2);
            if (student.getUnbanTime() != null) {
                cw.gt("checkin_time", student.getUnbanTime());
            }
            long lates = checkinMapper.selectCount(cw);

            if (noShows >= 3 || lates >= 5) {
                student.setStatus(0);
                student.setLimitTime(now);
                update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Student>()
                        .eq("student_id", student.getStudentId())
                        .set("status", 0)
                        .set("limit_time", now));
            }
        }
    }
}
