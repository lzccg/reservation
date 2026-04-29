package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Admin;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.mapper.StudentMapper;
import com.iflytek.reservation.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @GetMapping("/summary")
    public Result<?> summary(HttpServletRequest request) {
        Admin admin = getCurrentAdmin(request);
        if (admin == null) {
            return Result.error(401, "未登录");
        }

        long studentTotal = studentMapper.selectCount(new QueryWrapper<Student>()
                .ne("status", 2));

        long companyTotal = companyMapper.selectCount(new QueryWrapper<Company>()
                .eq("status", 1));

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long todaySessions = sessionMapper.selectCount(new QueryWrapper<Session>()
                .ge("start_time", start)
                .lt("start_time", end)
                .in("session_status", 2, 3));

        long checkinTotal = checkinMapper.selectCount(new QueryWrapper<Checkin>());

        Map<String, Object> data = new HashMap<>();
        data.put("studentTotal", studentTotal);
        data.put("companyTotal", companyTotal);
        data.put("todaySessions", todaySessions);
        data.put("checkinTotal", checkinTotal);
        return Result.success(data);
    }

    @GetMapping("/checkin-trend")
    public Result<?> checkinTrend(HttpServletRequest request) {
        Admin admin = getCurrentAdmin(request);
        if (admin == null) {
            return Result.error(401, "未登录");
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        QueryWrapper<Checkin> qw = new QueryWrapper<>();
        qw.select("DATE(checkin_time) as day", "count(1) as cnt");
        qw.isNotNull("checkin_time");
        qw.ge("checkin_time", from);
        qw.lt("checkin_time", to);
        qw.groupBy("DATE(checkin_time)");
        qw.orderByAsc("day");
        List<Map<String, Object>> maps = checkinMapper.selectMaps(qw);

        Map<String, Long> cntByDay = new HashMap<>();
        for (Map<String, Object> m : maps) {
            String day = m.get("day") == null ? null : String.valueOf(m.get("day"));
            Long cnt = toLong(m.get("cnt"));
            if (day != null && !day.isBlank() && cnt != null) {
                cntByDay.put(day, cnt);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> days = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            String key = fmt.format(d);
            days.add(key);
            values.add(cntByDay.getOrDefault(key, 0L));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("values", values);
        return Result.success(data);
    }

    @GetMapping("/reservation-trend")
    public Result<?> reservationTrend(HttpServletRequest request) {
        Admin admin = getCurrentAdmin(request);
        if (admin == null) {
            return Result.error(401, "未登录");
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.select("DATE(reservation_time) as day", "count(1) as cnt");
        qw.isNotNull("reservation_time");
        qw.ge("reservation_time", from);
        qw.lt("reservation_time", to);
        qw.in("reservation_status", 0, 2, 3);
        qw.groupBy("DATE(reservation_time)");
        qw.orderByAsc("day");
        List<Map<String, Object>> maps = reservationMapper.selectMaps(qw);

        Map<String, Long> cntByDay = new HashMap<>();
        for (Map<String, Object> m : maps) {
            String day = m.get("day") == null ? null : String.valueOf(m.get("day"));
            Long cnt = toLong(m.get("cnt"));
            if (day != null && !day.isBlank() && cnt != null) {
                cntByDay.put(day, cnt);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> days = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            String key = fmt.format(d);
            days.add(key);
            values.add(cntByDay.getOrDefault(key, 0L));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("values", values);
        return Result.success(data);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Admin getCurrentAdmin(HttpServletRequest request) {
        Long adminId = AuthTokenUtil.extractAdminId(request);
        if (adminId == null) {
            return null;
        }
        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            return null;
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return null;
        }
        return admin;
    }
}
