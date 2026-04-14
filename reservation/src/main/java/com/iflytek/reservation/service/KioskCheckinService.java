package com.iflytek.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class KioskCheckinService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Transactional
    public Map<String, Object> checkin(Long studentId, Long sessionId) {
        if (studentId == null || sessionId == null) {
            throw new IllegalArgumentException("参数错误");
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("宣讲会不存在");
        }

        Reservation reservation = reservationMapper.selectOne(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .eq("session_id", sessionId)
                .orderByDesc("reservation_id")
                .last("limit 1"));
        if (reservation == null || reservation.getReservationStatus() == null || reservation.getReservationStatus() == 1) {
            throw new IllegalArgumentException("您未预约本场宣讲会，无法签到");
        }
        if (reservation.getReservationStatus() == 2) {
            throw new IllegalArgumentException("您已签到，无需重复签到");
        }
        if (reservation.getReservationStatus() == 3) {
            throw new IllegalArgumentException("您未预约本场宣讲会，无法签到");
        }

        Long duplicated = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("student_id", studentId)
                .eq("session_id", sessionId));
        if (duplicated != null && duplicated > 0) {
            throw new IllegalArgumentException("您已签到，无需重复签到");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkinEnd = session.getCheckinEnd();
        if (checkinEnd == null && session.getStartTime() != null) {
            checkinEnd = session.getStartTime().plusMinutes(15);
        }
        int checkinStatus = (checkinEnd != null && now.isAfter(checkinEnd)) ? 1 : 0;

        Checkin checkin = new Checkin();
        checkin.setStudentId(studentId);
        checkin.setSessionId(sessionId);
        checkin.setCheckinTime(now);
        checkin.setCheckinStatus(checkinStatus);
        checkinMapper.insert(checkin);

        int updated = reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("reservation_id", reservation.getReservationId())
                .eq("reservation_status", 0)
                .set("reservation_status", 2));
        if (updated <= 0) {
            throw new IllegalArgumentException("签到失败，请重试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("studentId", studentId);
        result.put("checkinStatus", checkinStatus);
        result.put("checkinTime", now);
        return result;
    }
}
