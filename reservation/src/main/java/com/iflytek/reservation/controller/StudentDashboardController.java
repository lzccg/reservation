package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/api/student/dashboard")
public class StudentDashboardController {

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @GetMapping("/summary")
    public Result<?> summary(HttpServletRequest request) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        long attended = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("student_id", studentId));

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> upcomingReservations = reservationMapper.selectList(new QueryWrapper<Reservation>()
                .select("session_id")
                .eq("student_id", studentId)
                .eq("reservation_status", 0));
        Set<Long> upcomingSessionIds = upcomingReservations.stream().map(Reservation::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        long upcoming = upcomingSessionIds.isEmpty() ? 0 : sessionMapper.selectCount(new QueryWrapper<Session>()
                .in("session_id", upcomingSessionIds)
                .isNotNull("start_time")
                .gt("start_time", now));

        long openSessions = sessionMapper.selectCount(new QueryWrapper<Session>()
                .eq("session_status", 2)
                .isNotNull("end_time")
                .gt("end_time", now));

        Map<String, Object> data = new HashMap<>();
        data.put("attended", attended);
        data.put("upcoming", upcoming);
        data.put("openSessions", openSessions);
        return Result.success(data);
    }

    @GetMapping("/industry-distribution")
    public Result<?> industryDistribution(HttpServletRequest request) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        List<Session> sessions = sessionMapper.selectList(new QueryWrapper<Session>()
                .select("session_id", "company_id"));
        Set<Long> companyIds = sessions.stream().map(Session::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Map.of() : companyMapper.selectBatchIds(companyIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Company::getCompanyId, c -> c));

        Map<String, Long> cntByIndustry = new LinkedHashMap<>();
        for (Session s : sessions) {
            Company c = s.getCompanyId() == null ? null : companyMap.get(s.getCompanyId());
            String industry = c == null || c.getIndustry() == null ? "" : c.getIndustry().trim();
            if (industry.isBlank()) {
                industry = "其他";
            }
            cntByIndustry.put(industry, cntByIndustry.getOrDefault(industry, 0L) + 1);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : cntByIndustry.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("industry", e.getKey());
            row.put("count", e.getValue());
            list.add(row);
        }
        list.sort((a, b) -> Long.compare(toLong(b.get("count")), toLong(a.get("count"))));
        return Result.success(list);
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
