package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.CompanyMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentSessionController {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private StudentService studentService;

    @GetMapping("/checkin/latest")
    public Result<?> latestReservedSession(HttpServletRequest request) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        studentService.refreshStudentStatus(student);

        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<Reservation> rw = new QueryWrapper<>();
        rw.eq("student_id", studentId);
        rw.eq("reservation_status", 0);
        rw.orderByAsc("reservation_time");
        List<Reservation> reservations = reservationMapper.selectList(rw);
        if (reservations == null || reservations.isEmpty()) {
            return Result.success(null);
        }

        List<Long> sessionIds = reservations.stream().map(Reservation::getSessionId).collect(Collectors.toList());
        if (sessionIds.isEmpty()) {
            return Result.success(null);
        }

        QueryWrapper<Session> sw = new QueryWrapper<>();
        sw.in("session_id", sessionIds);
        sw.eq("session_status", 2);
        sw.gt("start_time", now);
        sw.orderByAsc("start_time");
        sw.last("LIMIT 1");
        Session s = sessionMapper.selectOne(sw);
        if (s == null) {
            return Result.success(null);
        }

        Company c = s.getCompanyId() == null ? null : companyMapper.selectById(s.getCompanyId());
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", s.getSessionId());
        data.put("sessionTitle", s.getSessionTitle());
        data.put("companyName", c == null ? "-" : c.getCompanyName());
        data.put("startTime", s.getStartTime());
        data.put("endTime", s.getEndTime());
        data.put("sessionLocation", s.getSessionLocation());
        return Result.success(data);
    }

    @GetMapping("/sessions")
    public Result<?> listSessions(
            HttpServletRequest request,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "9") long size
    ) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
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

        List<Long> companyIdsFilter = null;
        if (companyName != null && !companyName.isBlank()) {
            List<Map<String, Object>> companyIdMaps = companyMapper.selectMaps(new QueryWrapper<Company>()
                    .select("company_id")
                    .like("company_name", companyName.trim()));
            companyIdsFilter = companyIdMaps.stream()
                    .map(m -> ((Number) m.get("company_id")).longValue())
                    .distinct()
                    .collect(Collectors.toList());
            if (companyIdsFilter.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("records", new ArrayList<>());
                data.put("total", 0);
                data.put("current", safeCurrent);
                data.put("size", safeSize);
                data.put("studentStatus", student.getStatus());
                if (student.getStatus() != null && student.getStatus() == 0 && student.getLimitTime() != null) {
                    data.put("unbanTime", student.getLimitTime().plusDays(7));
                }
                return Result.success(data);
            }
        }

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate.trim()).atStartOfDay();
        }
        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate.trim()).plusDays(1).atStartOfDay();
        }

        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<Session> countWrapper = new QueryWrapper<>();
        countWrapper.eq("session_status", 2);
        countWrapper.gt("start_time", now);
        if (companyIdsFilter != null) {
            countWrapper.in("company_id", companyIdsFilter);
        }
        if (start != null) {
            countWrapper.ge("start_time", start);
        }
        if (end != null) {
            countWrapper.lt("start_time", end);
        }
        long total = sessionMapper.selectCount(countWrapper);

        QueryWrapper<Session> wrapper = new QueryWrapper<>();
        wrapper.eq("session_status", 2);
        wrapper.gt("start_time", now);
        if (companyIdsFilter != null) {
            wrapper.in("company_id", companyIdsFilter);
        }
        if (start != null) {
            wrapper.ge("start_time", start);
        }
        if (end != null) {
            wrapper.lt("start_time", end);
        }
        wrapper.orderByAsc("start_time");
        wrapper.last("LIMIT " + offset + "," + safeSize);
        List<Session> sessions = sessionMapper.selectList(wrapper);

        Set<Long> companyIds = sessions.stream()
                .map(Session::getCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> companyNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            List<Company> companies = companyMapper.selectList(new QueryWrapper<Company>()
                    .select("company_id", "company_name")
                    .in("company_id", companyIds));
            for (Company c : companies) {
                companyNameMap.put(c.getCompanyId(), c.getCompanyName());
            }
        }

        Set<Long> reservedSessionIds = new HashSet<>();
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(Session::getSessionId).collect(Collectors.toList());
            List<Map<String, Object>> reservedMaps = reservationMapper.selectMaps(new QueryWrapper<Reservation>()
                    .select("session_id")
                    .eq("student_id", studentId)
                    .in("session_id", sessionIds)
                    .in("reservation_status", List.of(0, 2, 3))
                    .groupBy("session_id"));
            for (Map<String, Object> m : reservedMaps) {
                Object v = m.get("session_id");
                if (v != null) {
                    reservedSessionIds.add(((Number) v).longValue());
                }
            }
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Session s : sessions) {
            LocalDateTime st = s.getStartTime();
            LocalDateTime computedCheckinStart = s.getCheckinStart();
            LocalDateTime computedCheckinEnd = s.getCheckinEnd();
            if (st != null && computedCheckinStart == null) {
                computedCheckinStart = st.minus(20, ChronoUnit.MINUTES);
            }
            if (st != null && computedCheckinEnd == null) {
                computedCheckinEnd = st.plus(15, ChronoUnit.MINUTES);
            }
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("sessionTitle", s.getSessionTitle());
            row.put("companyId", s.getCompanyId());
            row.put("companyName", companyNameMap.getOrDefault(s.getCompanyId(), "-"));
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("checkinStart", computedCheckinStart);
            row.put("checkinEnd", computedCheckinEnd);
            row.put("sessionLocation", s.getSessionLocation());
            row.put("capacity", s.getCapacity());
            row.put("remainingSeats", s.getRemainingSeats());
            row.put("sessionStatus", s.getSessionStatus());
            row.put("reserved", reservedSessionIds.contains(s.getSessionId()));
            records.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        data.put("studentStatus", student.getStatus());
        if (student.getStatus() != null && student.getStatus() == 0 && student.getLimitTime() != null) {
            data.put("unbanTime", student.getLimitTime().plusDays(7));
        }
        return Result.success(data);
    }

    @GetMapping("/session/{id}")
    public Result<?> sessionDetail(HttpServletRequest request, @PathVariable("id") Long id) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        Session s = sessionMapper.selectById(id);
        if (s == null) {
            return Result.error(404, "宣讲会不存在");
        }
        if (s.getSessionStatus() == null || s.getSessionStatus() != 2) {
            return Result.error("当前宣讲会不可预约");
        }
        Company c = s.getCompanyId() == null ? null : companyMapper.selectById(s.getCompanyId());
        Map<String, Object> data = new HashMap<>();
        if (s.getStartTime() != null) {
            if (s.getCheckinStart() == null) {
                s.setCheckinStart(s.getStartTime().minus(20, ChronoUnit.MINUTES));
            }
            if (s.getCheckinEnd() == null) {
                s.setCheckinEnd(s.getStartTime().plus(15, ChronoUnit.MINUTES));
            }
        }
        data.put("session", s);
        data.put("companyName", c == null ? "-" : c.getCompanyName());
        data.put("studentStatus", student.getStatus());
        return Result.success(data);
    }
}
