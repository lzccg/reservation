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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company")
public class CompanyCheckinRecordsController {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @GetMapping("/checkin-records")
    public Result<?> list(HttpServletRequest request,
                          @RequestParam("sessionId") Long sessionId,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "status", required = false) String status,
                          @RequestParam(value = "current", required = false, defaultValue = "1") long current,
                          @RequestParam(value = "size", required = false, defaultValue = "10") long size) {
        Long companyId = AuthTokenUtil.extractId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        if (sessionId == null) {
            return Result.error("缺少 sessionId");
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getCompanyId() == null || !session.getCompanyId().equals(companyId)) {
            return Result.error(404, "宣讲会不存在");
        }

        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        Set<Long> filteredStudentIds = null;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Student> students = studentMapper.selectList(new QueryWrapper<Student>()
                    .select("student_id")
                    .and(w -> w.like("student_name", kw).or().like("student_no", kw)));
            filteredStudentIds = students.stream().map(Student::getStudentId).collect(Collectors.toSet());
            if (filteredStudentIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("records", List.of());
                data.put("total", 0);
                data.put("current", safeCurrent);
                data.put("size", safeSize);
                data.put("sessionTitle", session.getSessionTitle());
                return Result.success(data);
            }
        }

        QueryWrapper<Reservation> countWrapper = buildReservationWrapper(sessionId, status, filteredStudentIds);
        long total = reservationMapper.selectCount(countWrapper);

        QueryWrapper<Reservation> wrapper = buildReservationWrapper(sessionId, status, filteredStudentIds);
        wrapper.orderByDesc("reservation_time").last("LIMIT " + offset + "," + safeSize);
        List<Reservation> reservations = reservationMapper.selectList(wrapper);

        List<Map<String, Object>> records = buildRecords(session, reservations);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        data.put("sessionTitle", session.getSessionTitle());
        return Result.success(data);
    }

    @GetMapping("/checkin-records/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         @RequestParam("sessionId") Long sessionId,
                                         @RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "status", required = false) String status) throws Exception {
        Long companyId = AuthTokenUtil.extractId(request);
        if (companyId == null) {
            return ResponseEntity.status(401).build();
        }
        if (sessionId == null) {
            return ResponseEntity.badRequest().build();
        }
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getCompanyId() == null || !session.getCompanyId().equals(companyId)) {
            return ResponseEntity.status(404).build();
        }

        Set<Long> filteredStudentIds = null;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Student> students = studentMapper.selectList(new QueryWrapper<Student>()
                    .select("student_id")
                    .and(w -> w.like("student_name", kw).or().like("student_no", kw)));
            filteredStudentIds = students.stream().map(Student::getStudentId).collect(Collectors.toSet());
            if (filteredStudentIds.isEmpty()) {
                filteredStudentIds = Set.of(-1L);
            }
        }

        List<Reservation> reservations = reservationMapper.selectList(buildReservationWrapper(sessionId, status, filteredStudentIds)
                .orderByDesc("reservation_time"));
        List<Map<String, Object>> records = buildRecords(session, reservations);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("签到名单");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("序号");
        header.createCell(1).setCellValue("学生姓名");
        header.createCell(2).setCellValue("学号");
        header.createCell(3).setCellValue("专业学院");
        header.createCell(4).setCellValue("班级");
        header.createCell(5).setCellValue("学生手机号");
        header.createCell(6).setCellValue("预约宣讲会");
        header.createCell(7).setCellValue("宣讲会时间");
        header.createCell(8).setCellValue("签到状态");

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> r = records.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(Objects.toString(r.get("studentName"), ""));
            row.createCell(2).setCellValue(Objects.toString(r.get("studentNo"), ""));
            row.createCell(3).setCellValue(Objects.toString(r.get("majorCollege"), ""));
            row.createCell(4).setCellValue(Objects.toString(r.get("clazz"), ""));
            row.createCell(5).setCellValue(Objects.toString(r.get("phone"), ""));
            row.createCell(6).setCellValue(session.getSessionTitle() == null ? "" : session.getSessionTitle());
            row.createCell(7).setCellValue(buildSessionTimeRange(session));
            String st = Objects.toString(r.get("status"), "");
            row.createCell(8).setCellValue("checked".equals(st) ? "已签到" : "未签到");
        }
        for (int i = 0; i <= 8; i++) {
            sheet.autoSizeColumn(i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        String baseName = (session.getSessionTitle() == null ? "宣讲会" : session.getSessionTitle()) + "+学生签到信息.xlsx";
        String fileName = URLEncoder.encode(baseName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }

    private QueryWrapper<Reservation> buildReservationWrapper(Long sessionId, String status, Set<Long> studentIds) {
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.eq("session_id", sessionId);
        qw.in("reservation_status", 0, 2, 3);
        if (status != null && !status.isBlank()) {
            String s = status.trim().toLowerCase(Locale.ROOT);
            if ("checked".equals(s)) {
                qw.eq("reservation_status", 2);
            } else if ("unchecked".equals(s)) {
                qw.in("reservation_status", 0, 3);
            }
        }
        if (studentIds != null) {
            qw.in("student_id", studentIds);
        }
        return qw;
    }

    private List<Map<String, Object>> buildRecords(Session session, List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        Set<Long> studentIds = reservations.stream().map(Reservation::getStudentId).collect(Collectors.toSet());
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, Student> studentMap = students.stream().filter(Objects::nonNull).collect(Collectors.toMap(Student::getStudentId, s -> s));

        List<Checkin> checkins = checkinMapper.selectList(new QueryWrapper<Checkin>()
                .eq("session_id", session.getSessionId())
                .in("student_id", studentIds));
        Map<Long, Checkin> checkinByStudent = new LinkedHashMap<>();
        for (Checkin c : checkins) {
            checkinByStudent.putIfAbsent(c.getStudentId(), c);
        }

        LocalDateTime now = LocalDateTime.now();
        return reservations.stream().map(r -> {
            Student st = studentMap.get(r.getStudentId());
            Checkin ck = checkinByStudent.get(r.getStudentId());

            String mappedStatus = r.getReservationStatus() != null && r.getReservationStatus() == 2 ? "checked" : "unchecked";
            LocalDateTime checkinTime = (r.getReservationStatus() != null && r.getReservationStatus() == 2 && ck != null) ? ck.getCheckinTime() : null;

            Map<String, Object> row = new HashMap<>();
            row.put("reservationId", r.getReservationId());
            row.put("studentName", st == null ? "" : st.getStudentName());
            row.put("studentNo", st == null ? "" : st.getStudentNo());
            row.put("majorCollege", buildMajorCollege(st));
            row.put("clazz", st == null ? "" : st.getClazz());
            row.put("phone", st == null ? "" : st.getPhone());
            row.put("sessionTitle", session.getSessionTitle());
            row.put("reserveTime", r.getReservationTime());
            row.put("startTime", session.getStartTime());
            row.put("endTime", session.getEndTime());
            row.put("status", mappedStatus);
            row.put("checkinTime", checkinTime);
            row.put("canCancel", r.getReservationStatus() != null && r.getReservationStatus() == 0
                    && session.getSessionStatus() != null && session.getSessionStatus() == 2
                    && session.getStartTime() != null && session.getStartTime().isAfter(now));
            return row;
        }).collect(Collectors.toList());
    }

    private String buildMajorCollege(Student st) {
        if (st == null) {
            return "";
        }
        String major = st.getMajor() == null ? "" : st.getMajor().trim();
        String college = st.getCollege() == null ? "" : st.getCollege().trim();
        if (!major.isBlank() && !college.isBlank()) {
            return major + "/" + college;
        }
        return !major.isBlank() ? major : college;
    }

    private String buildSessionTimeRange(Session session) {
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String start = fmt.format(session.getStartTime());
        String end = fmt.format(session.getEndTime());
        if (start.length() >= 16 && end.length() >= 16 && start.substring(0, 10).equals(end.substring(0, 10))) {
            return start + " - " + end.substring(11, 16);
        }
        return start + " - " + end;
    }
}

