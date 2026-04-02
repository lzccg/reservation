package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/student")
public class StudentReservationController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @PostMapping("/reserve/{sessionId}")
    @Transactional
    public Result<?> reserve(HttpServletRequest request, @PathVariable("sessionId") Long sessionId) {
        Long studentId = AuthTokenUtil.extractId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        if (student.getStatus() == null || student.getStatus() != 1) {
            return Result.error(403, "当前账号状态禁止预约");
        }

        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "宣讲会不存在");
        }
        if (session.getSessionStatus() == null || session.getSessionStatus() != 2) {
            return Result.error("该宣讲会当前不可预约");
        }
        if (session.getStartTime() != null && !session.getStartTime().isAfter(LocalDateTime.now())) {
            return Result.error("宣讲会已开始，无法预约");
        }

        Long exists = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .eq("session_id", sessionId)
                .in("reservation_status", 0, 2, 3));
        if (exists != null && exists > 0) {
            return Result.error("已预约该宣讲会");
        }

        int updated = sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", sessionId)
                .eq("session_status", 2)
                .gt("remaining_seats", 0)
                .setSql("remaining_seats = remaining_seats - 1"));
        if (updated <= 0) {
            return Result.error("名额已满");
        }

        Reservation reservation = new Reservation();
        reservation.setStudentId(studentId);
        reservation.setSessionId(sessionId);
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setReservationStatus(0);
        reservationMapper.insert(reservation);
        return Result.success("预约成功");
    }
}

