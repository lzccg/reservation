package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.mapper.StudentMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company/dashboard")
public class CompanyDashboardController {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private StudentMapper studentMapper;

    @GetMapping("/summary")
    public Result<?> summary(HttpServletRequest request) {
        Long companyId = AuthTokenUtil.extractId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }

        List<Session> sessions = sessionMapper.selectList(new QueryWrapper<Session>()
                .eq("company_id", companyId)
                .select("session_id", "session_title", "session_status", "start_time", "capacity", "remaining_seats")
                .orderByDesc("start_time"));
        Set<Long> sessionIds = sessions.stream().map(Session::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());

        long sessionTotal = sessionIds.size();
        long reservationTotal = sessionIds.isEmpty() ? 0 : reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .in("session_id", sessionIds)
                .in("reservation_status", 0, 2, 3));
        long checkinTotal = sessionIds.isEmpty() ? 0 : checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .in("session_id", sessionIds));

        double attendanceRate = reservationTotal <= 0 ? 0.0 : (checkinTotal * 100.0) / reservationTotal;

        Map<String, Object> latest = null;
        Session s = sessionMapper.selectOne(new QueryWrapper<Session>()
                .eq("company_id", companyId)
                .eq("session_status", 2)
                .select("session_id", "session_title", "session_status", "start_time", "capacity", "remaining_seats")
                .orderByDesc("start_time")
                .last("limit 1"));
        if (s != null) {
            long reservedCount = s.getCapacity() != null && s.getRemainingSeats() != null
                    ? Math.max(0L, (long) s.getCapacity() - (long) s.getRemainingSeats())
                    : reservationMapper.selectCount(new QueryWrapper<Reservation>()
                    .eq("session_id", s.getSessionId())
                    .in("reservation_status", 0, 2, 3));
            latest = new HashMap<>();
            latest.put("sessionId", s.getSessionId());
            latest.put("sessionTitle", s.getSessionTitle());
            latest.put("sessionStatus", s.getSessionStatus());
            latest.put("statusText", statusText(s.getSessionStatus()));
            latest.put("startTime", s.getStartTime());
            latest.put("capacity", s.getCapacity());
            latest.put("reservedCount", reservedCount);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionTotal", sessionTotal);
        data.put("reservationTotal", reservationTotal);
        data.put("attendanceRate", round1(attendanceRate));
        data.put("latestSession", latest);
        return Result.success(data);
    }

    @GetMapping("/college-distribution")
    public Result<?> collegeDistribution(HttpServletRequest request) {
        Long companyId = AuthTokenUtil.extractId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }

        List<Session> sessions = sessionMapper.selectList(new QueryWrapper<Session>()
                .eq("company_id", companyId)
                .select("session_id"));
        Set<Long> sessionIds = sessions.stream().map(Session::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Result.success(List.of());
        }

        List<Reservation> reservations = reservationMapper.selectList(new QueryWrapper<Reservation>()
                .in("session_id", sessionIds)
                .in("reservation_status", 0, 2, 3)
                .select("reservation_id", "student_id"));
        if (reservations.isEmpty()) {
            return Result.success(List.of());
        }
        Set<Long> studentIds = reservations.stream().map(Reservation::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentIds.isEmpty() ? Map.of() : studentMapper.selectBatchIds(studentIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Student::getStudentId, s -> s));

        Map<String, Long> cntByCollege = new LinkedHashMap<>();
        for (Reservation r : reservations) {
            Student st = r.getStudentId() == null ? null : studentMap.get(r.getStudentId());
            String college = st == null || st.getCollege() == null ? "" : st.getCollege().trim();
            if (college.isBlank()) {
                college = "未知学院";
            }
            cntByCollege.put(college, cntByCollege.getOrDefault(college, 0L) + 1);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : cntByCollege.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("college", e.getKey());
            row.put("count", e.getValue());
            list.add(row);
        }
        list.sort((a, b) -> Long.compare(toLong(b.get("count")), toLong(a.get("count"))));
        return Result.success(list);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == 0) return "待审";
        if (status == 1) return "已审核";
        if (status == 2) return "已发布";
        if (status == 3) return "已结束";
        if (status == 4) return "已取消";
        if (status == 5) return "已驳回";
        return "";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }
}

