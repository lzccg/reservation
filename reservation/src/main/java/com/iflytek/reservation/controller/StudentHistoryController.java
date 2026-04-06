package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentHistoryController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @GetMapping("/history")
    public Result<?> history(HttpServletRequest request,
                             @RequestParam(value = "type", required = false, defaultValue = "all") String type,
                             @RequestParam(value = "current", required = false, defaultValue = "1") long current,
                             @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        Long studentId = AuthTokenUtil.extractId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        studentService.refreshStudentStatus(student);

        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        Map<String, Object> stats = buildStats(studentId);

        Map<String, Object> pageData;
        if ("checked".equals(type) || "late".equals(type)) {
            pageData = pageByCheckin(studentId, type, offset, safeSize);
        } else {
            pageData = pageByReservation(studentId, type, offset, safeSize);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.putAll(pageData);
        result.put("current", safeCurrent);
        result.put("size", safeSize);
        result.put("studentStatus", student.getStatus());
        return Result.success(result);
    }

    @PostMapping("/reservation/cancel")
    @Transactional
    public Result<?> cancelReservation(HttpServletRequest request, @RequestParam("reservationId") Long reservationId) {
        Long studentId = AuthTokenUtil.extractId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        if (reservationId == null) {
            return Result.error("缺少 reservationId");
        }

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null || !Objects.equals(reservation.getStudentId(), studentId)) {
            return Result.error(404, "预约记录不存在");
        }
        if (reservation.getReservationStatus() == null || reservation.getReservationStatus() != 0) {
            return Result.error("当前状态不可取消");
        }

        Session session = sessionMapper.selectById(reservation.getSessionId());
        if (session == null) {
            return Result.error(404, "宣讲会不存在");
        }
        if (session.getStartTime() != null && !session.getStartTime().isAfter(LocalDateTime.now())) {
            return Result.error("宣讲会已开始，无法取消");
        }

        int updated = reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("reservation_id", reservationId)
                .eq("student_id", studentId)
                .eq("reservation_status", 0)
                .set("reservation_status", 1));
        if (updated <= 0) {
            return Result.error("取消失败");
        }

        sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", reservation.getSessionId())
                .setSql("remaining_seats = remaining_seats + 1"));

        return Result.success("取消成功");
    }

    private Map<String, Object> buildStats(Long studentId) {
        long total = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .in("reservation_status", 0, 2, 3));
        long checkedIn = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("student_id", studentId)
                .eq("checkin_status", 0));
        long absent = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .eq("reservation_status", 3));
        long late = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("student_id", studentId)
                .in("checkin_status", 1, 2));

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("checkedIn", checkedIn);
        stats.put("pending", absent);
        stats.put("late", late);
        return stats;
    }

    private Map<String, Object> pageByReservation(Long studentId, String type, long offset, long size) {
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.eq("student_id", studentId);
        if ("pending".equals(type)) {
            qw.eq("reservation_status", 0);
        } else if ("absent".equals(type)) {
            qw.eq("reservation_status", 3);
        } else if ("all".equals(type)) {
            qw.in("reservation_status", 0, 2, 3);
        } else {
            qw.in("reservation_status", 0, 2, 3);
        }

        long total = reservationMapper.selectCount(qw);
        qw.orderByDesc("reservation_time").last("limit " + offset + "," + size);
        List<Reservation> reservations = reservationMapper.selectList(qw);
        List<Map<String, Object>> records = buildRecords(studentId, reservations, null);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        return data;
    }

    private Map<String, Object> pageByCheckin(Long studentId, String type, long offset, long size) {
        QueryWrapper<Checkin> qw = new QueryWrapper<>();
        qw.eq("student_id", studentId);
        if ("checked".equals(type)) {
            qw.eq("checkin_status", 0);
        } else {
            qw.in("checkin_status", 1, 2);
        }

        long total = checkinMapper.selectCount(qw);
        qw.orderByDesc("checkin_time").last("limit " + offset + "," + size);
        List<Checkin> checkins = checkinMapper.selectList(qw);

        Set<Long> sessionIds = checkins.stream().map(Checkin::getSessionId).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("records", List.of());
            data.put("total", total);
            return data;
        }

        List<Reservation> reservations = reservationMapper.selectList(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .in("session_id", sessionIds)
                .eq("reservation_status", 2)
                .orderByDesc("reservation_time"));

        Map<Long, Reservation> reservationBySession = new LinkedHashMap<>();
        for (Reservation r : reservations) {
            reservationBySession.putIfAbsent(r.getSessionId(), r);
        }

        List<Reservation> ordered = new ArrayList<>();
        for (Checkin c : checkins) {
            Reservation r = reservationBySession.get(c.getSessionId());
            if (r != null) {
                ordered.add(r);
            }
        }

        Map<Long, Checkin> checkinBySession = new HashMap<>();
        for (Checkin c : checkins) {
            checkinBySession.putIfAbsent(c.getSessionId(), c);
        }

        List<Map<String, Object>> records = buildRecords(studentId, ordered, checkinBySession);
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        return data;
    }

    private List<Map<String, Object>> buildRecords(Long studentId, List<Reservation> reservations, Map<Long, Checkin> checkinBySession) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }

        Set<Long> sessionIds = reservations.stream().map(Reservation::getSessionId).collect(Collectors.toSet());
        List<Session> sessions = sessionMapper.selectBatchIds(sessionIds);
        Map<Long, Session> sessionMap = sessions.stream().filter(Objects::nonNull).collect(Collectors.toMap(Session::getSessionId, s -> s));

        Set<Long> companyIds = sessions.stream().filter(Objects::nonNull).map(Session::getCompanyId).collect(Collectors.toSet());
        List<Company> companies = companyIds.isEmpty() ? List.of() : companyMapper.selectBatchIds(companyIds);
        Map<Long, Company> companyMap = companies.stream().filter(Objects::nonNull).collect(Collectors.toMap(Company::getCompanyId, c -> c));

        Map<Long, Checkin> localCheckinBySession = checkinBySession;
        if (localCheckinBySession == null) {
            List<Checkin> checkins = checkinMapper.selectList(new QueryWrapper<Checkin>()
                    .eq("student_id", studentId)
                    .in("session_id", sessionIds));
            localCheckinBySession = new HashMap<>();
            for (Checkin c : checkins) {
                localCheckinBySession.putIfAbsent(c.getSessionId(), c);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> records = new ArrayList<>();
        for (Reservation r : reservations) {
            Session s = sessionMap.get(r.getSessionId());
            if (s == null) {
                continue;
            }
            Company c = companyMap.get(s.getCompanyId());
            Checkin ck = localCheckinBySession.get(r.getSessionId());
            String status = mapStatus(r.getReservationStatus(), ck == null ? null : ck.getCheckinStatus());

            Map<String, Object> row = new HashMap<>();
            row.put("reservationId", r.getReservationId());
            row.put("companyName", c == null ? "" : c.getCompanyName());
            row.put("title", s.getSessionTitle());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("location", s.getSessionLocation());
            row.put("reserveTime", r.getReservationTime());
            row.put("status", status);

            boolean canCancel = "pending".equals(status)
                    && s.getSessionStatus() != null && s.getSessionStatus() == 2
                    && s.getStartTime() != null && s.getStartTime().isAfter(now);
            row.put("canCancel", canCancel);
            records.add(row);
        }
        return records;
    }

    private String mapStatus(Integer reservationStatus, Integer checkinStatus) {
        if (reservationStatus == null) {
            return "unknown";
        }
        if (reservationStatus == 0) {
            return "pending";
        }
        if (reservationStatus == 3) {
            return "absent";
        }
        if (reservationStatus == 2) {
            if (checkinStatus != null && (checkinStatus == 1 || checkinStatus == 2)) {
                return "late";
            }
            return "checked";
        }
        if (reservationStatus == 1) {
            return "canceled";
        }
        return "unknown";
    }
}

