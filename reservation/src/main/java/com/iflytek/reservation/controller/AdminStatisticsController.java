package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private StudentMapper studentMapper;

    @GetMapping("/summary")
    public Result<?> summary(HttpServletRequest request,
                             @RequestParam(value = "startDate", required = false) String startDate,
                             @RequestParam(value = "endDate", required = false) String endDate,
                             @RequestParam(value = "companyId", required = false) Long companyId) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (op.getRole() == null || op.getRole() != 1) return Result.error(403, "无权限访问");
        List<Session> sessions = querySessionsInRange(startDate, endDate, companyId);
        Set<Long> sessionIds = sessions.stream().map(Session::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        long sessionTotal = sessionIds.size();
        long reservationTotal = sessionIds.isEmpty() ? 0 : reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .in("session_id", sessionIds)
                .in("reservation_status", 0, 2, 3));
        long checkinTotal = sessionIds.isEmpty() ? 0 : checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .in("session_id", sessionIds));

        double rate = 0.0;
        if (reservationTotal > 0) {
            rate = (checkinTotal * 100.0) / reservationTotal;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("sessionTotal", sessionTotal);
        data.put("reservationTotal", reservationTotal);
        data.put("checkinTotal", checkinTotal);
        data.put("checkinRate", round1(rate));
        return Result.success(data);
    }

    @GetMapping("/sessions")
    public Result<?> sessions(HttpServletRequest request,
                              @RequestParam(value = "startDate", required = false) String startDate,
                              @RequestParam(value = "endDate", required = false) String endDate,
                              @RequestParam(value = "companyId", required = false) Long companyId) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (op.getRole() == null || op.getRole() != 1) return Result.error(403, "无权限访问");
        List<Session> sessions = querySessionsInRange(startDate, endDate, companyId);
        if (sessions.isEmpty()) {
            return Result.success(List.of());
        }
        Set<Long> sessionIds = sessions.stream().map(Session::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> reserveCountBySession = groupCountReservationBySession(sessionIds);
        Map<Long, Long> checkinCountBySession = groupCountCheckinBySession(sessionIds);

        Set<Long> companyIds = sessions.stream().map(Session::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Map.of() : companyMapper.selectBatchIds(companyIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Company::getCompanyId, c -> c));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Session s : sessions) {
            long reserve = reserveCountBySession.getOrDefault(s.getSessionId(), 0L);
            long checkin = checkinCountBySession.getOrDefault(s.getSessionId(), 0L);
            double r = reserve <= 0 ? 0.0 : (checkin * 100.0) / reserve;
            Company c = s.getCompanyId() == null ? null : companyMap.get(s.getCompanyId());

            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("companyId", s.getCompanyId());
            row.put("companyName", c == null ? "" : c.getCompanyName());
            row.put("sessionTitle", s.getSessionTitle());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("date", s.getStartTime() == null ? "" : dateFmt.format(s.getStartTime()));
            row.put("reserveCount", reserve);
            row.put("checkinCount", checkin);
            row.put("rate", round1(r));
            list.add(row);
        }
        return Result.success(list);
    }

    @GetMapping("/today-sessions")
    public Result<?> todaySessions(HttpServletRequest request) {
        Long adminId = AuthTokenUtil.extractId(request);
        if (adminId == null) {
            return Result.error(401, "未登录");
        }
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<Session> sessions = sessionMapper.selectList(new QueryWrapper<Session>()
                .select("session_id", "company_id", "session_title", "start_time", "end_time", "session_status")
                .isNotNull("start_time")
                .ge("start_time", from)
                .lt("start_time", to)
                .in("session_status", 2, 3)
                .orderByDesc("start_time"));
        if (sessions.isEmpty()) {
            return Result.success(List.of());
        }
        Set<Long> sessionIds = sessions.stream().map(Session::getSessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> reserveCountBySession = groupCountReservationBySession(sessionIds);
        Map<Long, Long> checkinCountBySession = groupCountCheckinBySession(sessionIds);

        Set<Long> companyIds = sessions.stream().map(Session::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? Map.of() : companyMapper.selectBatchIds(companyIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Company::getCompanyId, c -> c));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Session s : sessions) {
            long reserve = reserveCountBySession.getOrDefault(s.getSessionId(), 0L);
            long checkin = checkinCountBySession.getOrDefault(s.getSessionId(), 0L);
            Company c = s.getCompanyId() == null ? null : companyMap.get(s.getCompanyId());

            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("companyName", c == null ? "" : c.getCompanyName());
            row.put("sessionTitle", s.getSessionTitle());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("timeRange", buildTimeRange(s, fmt));
            row.put("reserveCount", reserve);
            row.put("checkinCount", checkin);
            list.add(row);
        }
        return Result.success(list);
    }

    @GetMapping("/companies")
    public Result<?> companyOptions(HttpServletRequest request,
                                   @RequestParam(value = "startDate", required = false) String startDate,
                                   @RequestParam(value = "endDate", required = false) String endDate) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (op.getRole() == null || op.getRole() != 1) return Result.error(403, "无权限访问");
        List<Session> sessions = querySessionsInRange(startDate, endDate, null);
        Set<Long> companyIds = sessions.stream().map(Session::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Company> companies = companyMapper.selectBatchIds(companyIds);
        List<Map<String, Object>> list = companies.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    String an = a.getCompanyName() == null ? "" : a.getCompanyName();
                    String bn = b.getCompanyName() == null ? "" : b.getCompanyName();
                    return an.compareToIgnoreCase(bn);
                })
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("companyId", c.getCompanyId());
                    row.put("companyName", c.getCompanyName());
                    return row;
                }).collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/company-sessions")
    public Result<?> companySessions(HttpServletRequest request,
                                    @RequestParam("companyId") Long companyId,
                                    @RequestParam(value = "startDate", required = false) String startDate,
                                    @RequestParam(value = "endDate", required = false) String endDate) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (op.getRole() == null || op.getRole() != 1) return Result.error(403, "无权限访问");
        if (companyId == null) {
            return Result.error("缺少 companyId");
        }
        QueryWrapper<Session> qw = buildSessionRangeWrapper(startDate, endDate, companyId);
        qw.orderByDesc("start_time");
        List<Session> sessions = sessionMapper.selectList(qw);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Session s : sessions) {
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("sessionTitle", s.getSessionTitle());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("timeRange", buildTimeRange(s, fmt));
            list.add(row);
        }
        return Result.success(list);
    }

    @GetMapping("/session-detail")
    public Result<?> sessionDetail(HttpServletRequest request,
                                   @RequestParam("sessionId") Long sessionId,
                                   @RequestParam(value = "type", required = false, defaultValue = "all") String type,
                                   @RequestParam(value = "current", required = false, defaultValue = "1") long current,
                                   @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (sessionId == null) {
            return Result.error("缺少 sessionId");
        }
        if (op.getRole() != null && op.getRole() == 2 && !isTodaySession(sessionId)) {
            return Result.error(403, "无权限访问");
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "宣讲会不存在");
        }
        Company company = session.getCompanyId() == null ? null : companyMapper.selectById(session.getCompanyId());

        List<Map<String, Object>> rows = buildSessionDetailRows(sessionId);
        String t = type == null ? "all" : type.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> filtered = rows.stream().filter(r -> {
            if ("all".equals(t)) return true;
            String status = Objects.toString(r.get("status"), "");
            if ("checked".equals(t)) return "checked".equals(status);
            if ("late".equals(t)) return "late".equals(status);
            if ("absent".equals(t)) return "absent".equals(status);
            return true;
        }).collect(Collectors.toList());

        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        int offset = Math.toIntExact((safeCurrent - 1) * safeSize);
        int toIndex = Math.min(filtered.size(), offset + Math.toIntExact(safeSize));
        List<Map<String, Object>> page = offset >= filtered.size() ? List.of() : filtered.subList(offset, toIndex);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getSessionId());
        data.put("sessionTitle", session.getSessionTitle());
        data.put("companyName", company == null ? "" : company.getCompanyName());
        data.put("records", page);
        data.put("total", filtered.size());
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @GetMapping("/session-detail/export")
    public ResponseEntity<byte[]> exportSessionDetail(HttpServletRequest request,
                                                     @RequestParam("sessionId") Long sessionId,
                                                     @RequestParam(value = "includeCompany", required = false, defaultValue = "0") int includeCompany) throws Exception {
        Admin op = getCurrentAdmin(request);
        if (op == null) return ResponseEntity.status(401).build();
        if (sessionId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (op.getRole() != null && op.getRole() == 2 && !isTodaySession(sessionId)) {
            return ResponseEntity.status(403).build();
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return ResponseEntity.status(404).build();
        }
        Company company = session.getCompanyId() == null ? null : companyMapper.selectById(session.getCompanyId());
        List<Map<String, Object>> rows = buildSessionDetailRows(sessionId);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("签到明细");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("序号");
        header.createCell(1).setCellValue("学生姓名");
        header.createCell(2).setCellValue("学号");
        header.createCell(3).setCellValue("学院");
        header.createCell(4).setCellValue("班级");
        header.createCell(5).setCellValue("签到时间");
        header.createCell(6).setCellValue("签到状态");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(Objects.toString(r.get("studentName"), ""));
            row.createCell(2).setCellValue(Objects.toString(r.get("studentNo"), ""));
            row.createCell(3).setCellValue(Objects.toString(r.get("college"), ""));
            row.createCell(4).setCellValue(Objects.toString(r.get("clazz"), ""));
            LocalDateTime checkinTime = (LocalDateTime) r.get("checkinTime");
            row.createCell(5).setCellValue(checkinTime == null ? "" : fmt.format(checkinTime));
            row.createCell(6).setCellValue(mapStatusText(Objects.toString(r.get("status"), "")));
        }
        for (int i = 0; i <= 6; i++) {
            sheet.autoSizeColumn(i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        String title = session.getSessionTitle() == null ? "宣讲会" : session.getSessionTitle();
        String companyName = company == null || company.getCompanyName() == null ? "" : company.getCompanyName();
        String baseName = (includeCompany == 1 && !companyName.isBlank())
                ? (companyName + "-" + title + "-签到明细.xlsx")
                : (title + "-签到明细.xlsx");
        String fileName = URLEncoder.encode(baseName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }

    @PostMapping("/session-detail/mark-checked")
    @Transactional
    public Result<?> markChecked(HttpServletRequest request, @RequestBody MarkCheckedRequest body) {
        Admin op = getCurrentAdmin(request);
        if (op == null) return Result.error(401, "未登录");
        if (body == null || body.getSessionId() == null || body.getStudentId() == null) {
            return Result.error("参数错误");
        }
        Long sessionId = body.getSessionId();
        if (op.getRole() != null && op.getRole() == 2 && !isTodaySession(sessionId)) {
            return Result.error(403, "无权限访问");
        }
        Long studentId = body.getStudentId();

        Long existingCheckin = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("session_id", sessionId)
                .eq("student_id", studentId));
        if (existingCheckin != null && existingCheckin > 0) {
            return Result.error("已存在签到记录");
        }

        Reservation reservation = reservationMapper.selectOne(new QueryWrapper<Reservation>()
                .eq("session_id", sessionId)
                .eq("student_id", studentId)
                .in("reservation_status", 0, 2, 3)
                .orderByDesc("reservation_id")
                .last("limit 1"));
        if (reservation == null) {
            return Result.error("未找到预约记录");
        }
        if (reservation.getReservationStatus() != null && reservation.getReservationStatus() == 2) {
            return Result.error("该学生已签到");
        }

        Checkin checkin = new Checkin();
        checkin.setSessionId(sessionId);
        checkin.setStudentId(studentId);
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setCheckinStatus(0);
        checkinMapper.insert(checkin);

        int updated = reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("reservation_id", reservation.getReservationId())
                .in("reservation_status", 0, 3)
                .set("reservation_status", 2));
        if (updated <= 0) {
            throw new IllegalArgumentException("更新预约状态失败");
        }
        return Result.success("操作成功");
    }

    private Admin getCurrentAdmin(HttpServletRequest request) {
        Long adminId = AuthTokenUtil.extractId(request);
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

    private boolean isTodaySession(Long sessionId) {
        if (sessionId == null) {
            return false;
        }
        Session s = sessionMapper.selectById(sessionId);
        if (s == null || s.getStartTime() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        return !s.getStartTime().isBefore(from) && s.getStartTime().isBefore(to);
    }

    public static class MarkCheckedRequest {
        private Long sessionId;
        private Long studentId;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }
    }

    private List<Session> querySessionsInRange(String startDate, String endDate, Long companyId) {
        QueryWrapper<Session> qw = buildSessionRangeWrapper(startDate, endDate, companyId);
        qw.orderByDesc("start_time");
        return sessionMapper.selectList(qw);
    }

    private QueryWrapper<Session> buildSessionRangeWrapper(String startDate, String endDate, Long companyId) {
        QueryWrapper<Session> qw = new QueryWrapper<>();
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate.trim());
            LocalDate end = LocalDate.parse(endDate.trim());
            LocalDateTime from = start.atStartOfDay();
            LocalDateTime to = end.plusDays(1).atStartOfDay();
            qw.isNotNull("start_time").ge("start_time", from);
            qw.isNotNull("end_time").lt("end_time", to);
        }
        if (companyId != null) {
            qw.eq("company_id", companyId);
        }
        return qw;
    }

    private Map<Long, Long> groupCountReservationBySession(Set<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.select("session_id as sessionId", "count(1) as cnt");
        qw.in("session_id", sessionIds);
        qw.in("reservation_status", 0, 2, 3);
        qw.groupBy("session_id");
        List<Map<String, Object>> maps = reservationMapper.selectMaps(qw);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Long sid = toLong(m.get("sessionId"));
            Long cnt = toLong(m.get("cnt"));
            if (sid != null && cnt != null) {
                result.put(sid, cnt);
            }
        }
        return result;
    }

    private Map<Long, Long> groupCountCheckinBySession(Set<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Checkin> qw = new QueryWrapper<>();
        qw.select("session_id as sessionId", "count(1) as cnt");
        qw.in("session_id", sessionIds);
        qw.groupBy("session_id");
        List<Map<String, Object>> maps = checkinMapper.selectMaps(qw);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Long sid = toLong(m.get("sessionId"));
            Long cnt = toLong(m.get("cnt"));
            if (sid != null && cnt != null) {
                result.put(sid, cnt);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildSessionDetailRows(Long sessionId) {
        List<Reservation> reservations = reservationMapper.selectList(new QueryWrapper<Reservation>()
                .eq("session_id", sessionId)
                .in("reservation_status", 0, 2, 3)
                .orderByDesc("reservation_time"));
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = reservations.stream().map(Reservation::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentIds.isEmpty() ? Map.of() : studentMapper.selectBatchIds(studentIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Student::getStudentId, s -> s));

        List<Checkin> checkins = studentIds.isEmpty() ? List.of() : checkinMapper.selectList(new QueryWrapper<Checkin>()
                .eq("session_id", sessionId)
                .in("student_id", studentIds));
        Map<Long, Checkin> checkinByStudent = new LinkedHashMap<>();
        for (Checkin c : checkins) {
            if (c != null && c.getStudentId() != null) {
                checkinByStudent.putIfAbsent(c.getStudentId(), c);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Reservation r : reservations) {
            Student st = r.getStudentId() == null ? null : studentMap.get(r.getStudentId());
            Checkin ck = r.getStudentId() == null ? null : checkinByStudent.get(r.getStudentId());
            String status;
            if (ck == null) {
                status = "absent";
            } else if (ck.getCheckinStatus() != null && ck.getCheckinStatus() == 1) {
                status = "late";
            } else {
                status = "checked";
            }
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", r.getStudentId());
            row.put("studentName", st == null ? "" : st.getStudentName());
            row.put("studentNo", st == null ? "" : st.getStudentNo());
            row.put("college", st == null ? "" : st.getCollege());
            row.put("clazz", st == null ? "" : st.getClazz());
            row.put("checkinTime", ck == null ? null : ck.getCheckinTime());
            row.put("checkinStatus", ck == null ? null : ck.getCheckinStatus());
            row.put("status", status);
            rows.add(row);
        }
        return rows;
    }

    private String buildTimeRange(Session session, DateTimeFormatter fmt) {
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return "";
        }
        String start = fmt.format(session.getStartTime());
        String end = fmt.format(session.getEndTime());
        if (start.length() >= 16 && end.length() >= 16 && start.substring(0, 10).equals(end.substring(0, 10))) {
            return start + " - " + end.substring(11, 16);
        }
        return start + " - " + end;
    }

    private String mapStatusText(String status) {
        if ("checked".equals(status)) return "已签到";
        if ("late".equals(status)) return "迟到";
        if ("absent".equals(status)) return "缺席";
        return "";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
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
}

