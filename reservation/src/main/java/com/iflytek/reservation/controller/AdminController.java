package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.FaceData;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.FaceDataMapper;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.AdminService;
import com.iflytek.reservation.service.CompanyService;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private FaceDataMapper faceDataMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @GetMapping("/companies")
    public Result<?> listCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        QueryWrapper<Company> countWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            countWrapper.like("company_name", keyword.trim());
        }
        if (status != null) {
            countWrapper.eq("status", status);
        }
        long total = companyService.count(countWrapper);

        QueryWrapper<Company> wrapper = new QueryWrapper<>();
        wrapper.select("company_id", "credit_code", "company_name", "industry", "contact_name", "status");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("company_name", keyword.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("company_id");
        wrapper.last("LIMIT " + offset + "," + safeSize);

        List<Company> records = companyService.list(wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Company c : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("companyId", c.getCompanyId());
            row.put("creditCode", c.getCreditCode());
            row.put("companyName", c.getCompanyName());
            row.put("industry", c.getIndustry());
            row.put("contactName", c.getContactName());
            row.put("status", c.getStatus());
            list.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @GetMapping("/company/{id}")
    public Result<?> companyDetail(@PathVariable("id") Long id) {
        Company c = companyService.getById(id);
        if (c == null) {
            return Result.error(404, "企业不存在");
        }
        c.setPassword(null);
        return Result.success(c);
    }

    @PostMapping("/company/{id}/audit")
    public Result<?> auditCompany(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        if (action == null || action.isBlank()) {
            return Result.error("缺少action参数");
        }
        if ("approve".equalsIgnoreCase(action)) {
            boolean ok = companyService.update(new UpdateWrapper<Company>()
                    .eq("company_id", id)
                    .set("status", 1)
                    .set("audit_remark", null)
                    .set("audit_time", LocalDateTime.now()));
            return ok ? Result.success("已入驻") : Result.error("操作失败");
        }
        if ("reject".equalsIgnoreCase(action)) {
            if (remark == null || remark.isBlank()) {
                return Result.error("驳回理由不能为空");
            }
            boolean ok = companyService.update(new UpdateWrapper<Company>()
                    .eq("company_id", id)
                    .set("status", 2)
                    .set("audit_remark", remark)
                    .set("audit_time", null));
            return ok ? Result.success("已驳回") : Result.error("操作失败");
        }
        return Result.error("未知action参数");
    }

    @PostMapping("/company/{id}/revoke")
    public Result<?> revokeCompany(HttpServletRequest request, @PathVariable("id") Long id) {
        Long adminId = com.iflytek.reservation.common.AuthTokenUtil.extractId(request);
        if (adminId == null) {
            return Result.error(401, "未登录");
        }
        com.iflytek.reservation.entity.Admin admin = adminService.getById(adminId);
        if (admin == null || admin.getRole() == null || admin.getRole() != 1) {
            return Result.error(403, "权限不足");
        }
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        long active = sessionMapper.selectCount(new QueryWrapper<com.iflytek.reservation.entity.Session>()
                .eq("company_id", id)
                .in("session_status", Stream.of(1, 2).collect(Collectors.toList()))
                .ge("end_time", now));
        if (active > 0) {
            return Result.error("该企业有正在进行的宣讲会，无法注销");
        }
        Company update = new Company();
        update.setCompanyId(id);
        update.setStatus(3);
        boolean ok = companyService.updateById(update);
        return ok ? Result.success("已注销") : Result.error("注销失败");
    }

    @GetMapping("/students")
    public Result<?> listStudents(
            @RequestParam(required = false) String clazz,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        QueryWrapper<Student> countWrapper = new QueryWrapper<>();
        countWrapper.ne("status", 2);
        if (clazz != null && !clazz.isBlank()) {
            countWrapper.like("`class`", clazz.trim());
        }
        if (major != null && !major.isBlank()) {
            countWrapper.like("major", major.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            countWrapper.and(w -> w.like("student_name", kw).or().like("student_no", kw));
        }
        long total = studentService.count(countWrapper);

        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.select("student_id", "student_no", "student_name", "major", "`class` AS clazz", "phone", "status");
        wrapper.ne("status", 2);
        if (clazz != null && !clazz.isBlank()) {
            wrapper.like("`class`", clazz.trim());
        }
        if (major != null && !major.isBlank()) {
            wrapper.like("major", major.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("student_name", kw).or().like("student_no", kw));
        }
        wrapper.orderByDesc("student_id");
        wrapper.last("LIMIT " + offset + "," + safeSize);
        List<Student> records = studentService.list(wrapper);
        List<Long> studentIds = records.stream().map(Student::getStudentId).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        if (studentIds.isEmpty()) {
            data.put("records", new ArrayList<>());
            data.put("total", total);
            data.put("current", safeCurrent);
            data.put("size", safeSize);
            return Result.success(data);
        }

        Set<Long> faceUploadedSet = new HashSet<>();
        List<Map<String, Object>> faceMaps = faceDataMapper.selectMaps(new QueryWrapper<FaceData>()
                .select("student_id")
                .in("student_id", studentIds)
                .groupBy("student_id"));
        for (Map<String, Object> m : faceMaps) {
            Object v = m.get("student_id");
            if (v != null) {
                faceUploadedSet.add(((Number) v).longValue());
            }
        }

        Map<Long, Long> reserveCountMap = groupCountReservation(studentIds, List.of(0, 2, 3));
        Map<Long, Long> absentCountMap = groupCountReservation(studentIds, List.of(3));
        Map<Long, Long> reservationSignedCountMap = groupCountReservation(studentIds, List.of(2));
        Map<Long, Long> checkinSignedCountMap = groupCountCheckin(studentIds, 0);
        Map<Long, Long> lateCountMap = groupCountCheckin(studentIds, 1);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Student s : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", s.getStudentId());
            row.put("studentNo", s.getStudentNo());
            row.put("studentName", s.getStudentName());
            row.put("major", s.getMajor());
            row.put("clazz", s.getClazz());
            row.put("faceUploaded", faceUploadedSet.contains(s.getStudentId()));
            row.put("reserveCount", reserveCountMap.getOrDefault(s.getStudentId(), 0L));
            long signed = reservationSignedCountMap.getOrDefault(s.getStudentId(), 0L);
            long checkinSigned = checkinSignedCountMap.getOrDefault(s.getStudentId(), 0L);
            row.put("checkinCount", Math.min(signed, checkinSigned));
            row.put("lateCount", lateCountMap.getOrDefault(s.getStudentId(), 0L));
            row.put("absentCount", absentCountMap.getOrDefault(s.getStudentId(), 0L));
            row.put("status", s.getStatus());
            list.add(row);
        }

        data.put("records", list);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @PutMapping("/student/{id}/status")
    public Result<?> updateStudentStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        if (statusObj == null) {
            return Result.error("缺少status参数");
        }
        int status = ((Number) statusObj).intValue();
        if (status != 0 && status != 1) {
            return Result.error("非法status参数");
        }
        Student existing = studentService.getById(id);
        if (existing == null || (existing.getStatus() != null && existing.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        Student update = new Student();
        update.setStudentId(id);
        update.setStatus(status);
        boolean ok = studentService.updateById(update);
        if (!ok) {
            return Result.error("更新失败");
        }
        return Result.success("更新成功");
    }

    @GetMapping("/student/{id}")
    public Result<?> studentDetail(@PathVariable("id") Long id) {
        Student student = studentService.getById(id);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        boolean faceUploaded = faceDataMapper.selectCount(new QueryWrapper<FaceData>()
                .eq("student_id", id)) > 0;

        long reserveCount = countReservation(id, List.of(0, 2, 3));
        long activeReservationCount = countReservation(id, List.of(0));
        long absentCount = countReservation(id, List.of(3));
        long signed = countReservation(id, List.of(2));
        long checkinSigned = countCheckin(id, 0);
        long checkinCount = Math.min(signed, checkinSigned);
        long lateCount = countCheckin(id, 1);

        Map<String, Object> data = new HashMap<>();
        data.put("studentId", student.getStudentId());
        data.put("studentNo", student.getStudentNo());
        data.put("studentName", student.getStudentName());
        data.put("major", student.getMajor());
        data.put("clazz", student.getClazz());
        data.put("phone", student.getPhone());
        data.put("faceUploaded", faceUploaded);
        data.put("reserveCount", reserveCount);
        data.put("checkinCount", checkinCount);
        data.put("lateCount", lateCount);
        data.put("absentCount", absentCount);
        data.put("activeReservationCount", activeReservationCount);
        data.put("status", student.getStatus());
        return Result.success(data);
    }

    @DeleteMapping("/student/{id}")
    public Result<?> deleteStudent(HttpServletRequest request, @PathVariable("id") Long id) {
        Long adminId = com.iflytek.reservation.common.AuthTokenUtil.extractId(request);
        if (adminId == null) {
            return Result.error(401, "未登录");
        }
        com.iflytek.reservation.entity.Admin admin = adminService.getById(adminId);
        if (admin == null) {
            return Result.error(403, "无权限");
        }
        if (admin.getRole() == null || admin.getRole() != 1) {
            return Result.error(403, "仅超级管理员可删除学生");
        }
        Student student = studentService.getById(id);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        long active = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", id)
                .eq("reservation_status", 0));
        if (active > 0) {
            return Result.error("学生有正在进行的宣讲会，无法删除");
        }
        Student update = new Student();
        update.setStudentId(id);
        update.setStatus(2);
        boolean ok = studentService.updateById(update);
        if (!ok) {
            return Result.error("删除失败");
        }
        return Result.success("删除成功");
    }

    private Map<Long, Long> groupCountReservation(List<Long> studentIds, List<Integer> statuses) {
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.select("student_id", "count(*) as cnt");
        qw.in("student_id", studentIds);
        qw.in("reservation_status", statuses);
        qw.groupBy("student_id");
        List<Map<String, Object>> maps = reservationMapper.selectMaps(qw);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Long sid = ((Number) m.get("student_id")).longValue();
            Long cnt = ((Number) m.get("cnt")).longValue();
            result.put(sid, cnt);
        }
        return result;
    }

    private Map<Long, Long> groupCountCheckin(List<Long> studentIds, int status) {
        QueryWrapper<Checkin> qw = new QueryWrapper<>();
        qw.select("student_id", "count(*) as cnt");
        qw.in("student_id", studentIds);
        qw.eq("checkin_status", status);
        qw.groupBy("student_id");
        List<Map<String, Object>> maps = checkinMapper.selectMaps(qw);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> m : maps) {
            Long sid = ((Number) m.get("student_id")).longValue();
            Long cnt = ((Number) m.get("cnt")).longValue();
            result.put(sid, cnt);
        }
        return result;
    }

    private long countReservation(Long studentId, List<Integer> statuses) {
        return reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .in("reservation_status", statuses));
    }

    private long countCheckin(Long studentId, int status) {
        return checkinMapper.selectCount(new QueryWrapper<Checkin>()
                .eq("student_id", studentId)
                .eq("checkin_status", status));
    }
}

