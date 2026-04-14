package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.FaceData;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
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
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalDate;

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

    @GetMapping("/checkin/today-sessions")
    public Result<?> todayCheckinSessions(HttpServletRequest request) {
        Long adminId = AuthTokenUtil.extractId(request);
        if (adminId == null) {
            return Result.error(401, "未登录");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        QueryWrapper<Session> wrapper = new QueryWrapper<>();
        wrapper.eq("session_status", 2);
        wrapper.ge("start_time", start);
        wrapper.lt("start_time", end);
        wrapper.orderByAsc("start_time");
        List<Session> sessions = sessionMapper.selectList(wrapper);

        Set<Long> companyIds = sessions.stream()
                .map(Session::getCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Company> companyMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            List<Company> companies = companyService.listByIds(companyIds);
            companyMap = companies.stream()
                    .filter(c -> c != null && c.getCompanyId() != null)
                    .collect(Collectors.toMap(Company::getCompanyId, c -> c));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Session s : sessions) {
            Company c = s.getCompanyId() == null ? null : companyMap.get(s.getCompanyId());
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("companyName", c == null ? "-" : c.getCompanyName());
            row.put("sessionTitle", s.getSessionTitle());
            row.put("sessionLocation", s.getSessionLocation());
            list.add(row);
        }
        return Result.success(list);
    }

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
                .eq("session_status", 2)
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

    @GetMapping("/sessions")
    public Result<?> listSessions(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        List<Long> companyIdsFilter = null;
        if (companyName != null && !companyName.isBlank()) {
            List<Map<String, Object>> maps = companyService.listMaps(new QueryWrapper<Company>()
                    .select("company_id")
                    .like("company_name", companyName.trim()));
            companyIdsFilter = maps.stream()
                    .map(m -> ((Number) m.get("company_id")).longValue())
                    .distinct()
                    .collect(Collectors.toList());
            if (companyIdsFilter.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("records", new ArrayList<>());
                data.put("total", 0);
                data.put("current", safeCurrent);
                data.put("size", safeSize);
                return Result.success(data);
            }
        }

        QueryWrapper<Session> countWrapper = new QueryWrapper<>();
        if (status != null) {
            countWrapper.eq("session_status", status);
        }
        if (companyIdsFilter != null) {
            countWrapper.in("company_id", companyIdsFilter);
        }
        long total = sessionMapper.selectCount(countWrapper);

        QueryWrapper<Session> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("session_status", status);
        }
        if (companyIdsFilter != null) {
            wrapper.in("company_id", companyIdsFilter);
        }
        wrapper.orderByDesc("session_id");
        wrapper.last("LIMIT " + offset + "," + safeSize);
        List<Session> sessions = sessionMapper.selectList(wrapper);

        Set<Long> companyIds = sessions.stream()
                .map(Session::getCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> companyNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            List<Company> companies = companyService.list(new QueryWrapper<Company>()
                    .select("company_id", "company_name")
                    .in("company_id", companyIds));
            for (Company c : companies) {
                companyNameMap.put(c.getCompanyId(), c.getCompanyName());
            }
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Session s : sessions) {
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("companyId", s.getCompanyId());
            row.put("companyName", companyNameMap.getOrDefault(s.getCompanyId(), "-"));
            row.put("sessionTitle", s.getSessionTitle());
            row.put("startTime", s.getStartTime());
            row.put("endTime", s.getEndTime());
            row.put("sessionLocation", s.getSessionLocation());
            row.put("capacity", s.getCapacity());
            row.put("sessionStatus", s.getSessionStatus());
            records.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @GetMapping("/session/{id}")
    public Result<?> sessionDetail(@PathVariable("id") Long id) {
        Session s = sessionMapper.selectById(id);
        if (s == null) {
            return Result.error(404, "宣讲会不存在");
        }
        Company c = s.getCompanyId() == null ? null : companyService.getById(s.getCompanyId());
        long checkinCount = 0;
        if (s.getSessionStatus() != null && (s.getSessionStatus() == 3 || s.getSessionStatus() == 4)) {
            checkinCount = checkinMapper.selectCount(new QueryWrapper<Checkin>()
                    .eq("session_id", id)
                    .eq("checkin_status", 0));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("session", s);
        data.put("companyName", c == null ? "-" : c.getCompanyName());
        data.put("checkinCount", checkinCount);
        return Result.success(data);
    }

    @PostMapping("/session/{id}/audit")
    public Result<?> auditSession(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Session existing = sessionMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "宣讲会不存在");
        }
        Integer status = existing.getSessionStatus();
        String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
        if (action == null || action.isBlank()) {
            return Result.error("缺少action参数");
        }
        if (status != null && (status == 3 || status == 4)) {
            return Result.error("已结束或已取消的宣讲会不可修改");
        }

        String title = body.get("sessionTitle") == null ? null : String.valueOf(body.get("sessionTitle"));
        String location = body.get("sessionLocation") == null ? null : String.valueOf(body.get("sessionLocation"));
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        String startStr = body.get("startTime") == null ? null : String.valueOf(body.get("startTime")).trim();
        String endStr = body.get("endTime") == null ? null : String.valueOf(body.get("endTime")).trim();
        LocalDateTime startTime = (startStr == null || startStr.isBlank()) ? null : LocalDateTime.parse(startStr);
        LocalDateTime endTime = (endStr == null || endStr.isBlank()) ? null : LocalDateTime.parse(endStr);
        Integer capacity = body.get("capacity") instanceof Number ? ((Number) body.get("capacity")).intValue() : null;

        if ("update".equalsIgnoreCase(action)) {
            boolean ok = applySessionUpdate(existing, title, location, startTime, endTime, capacity, description);
            return ok ? Result.success("已更新") : Result.error("更新失败");
        }

        if ("approve".equalsIgnoreCase(action)) {
            if (status == null || status != 0) {
                return Result.error("当前状态不允许审核通过");
            }
            boolean ok = applySessionUpdate(existing, title, location, startTime, endTime, capacity, description);
            if (!ok) {
                return Result.error("更新失败");
            }
            boolean ok2 = sessionMapper.update(null, new UpdateWrapper<Session>()
                    .eq("session_id", id)
                    .eq("session_status", 0)
                    .set("session_status", 1)
                    .set("audit_time", LocalDateTime.now())
                    .set("audit_remark", null)
                    .set("update_time", LocalDateTime.now())) > 0;
            return ok2 ? Result.success("已审核") : Result.error("操作失败");
        }

        if ("reject".equalsIgnoreCase(action)) {
            if (status == null || status != 0) {
                return Result.error("当前状态不允许驳回");
            }
            String remark = body.get("auditRemark") == null ? null : String.valueOf(body.get("auditRemark"));
            if (remark == null || remark.isBlank()) {
                return Result.error("驳回理由不能为空");
            }
            boolean ok = sessionMapper.update(null, new UpdateWrapper<Session>()
                    .eq("session_id", id)
                    .eq("session_status", 0)
                    .set("session_status", 5)
                    .set("audit_time", LocalDateTime.now())
                    .set("audit_remark", remark)
                    .set("update_time", LocalDateTime.now())) > 0;
            return ok ? Result.success("已打回") : Result.error("操作失败");
        }

        if ("cancel".equalsIgnoreCase(action)) {
            if (status == null || !(status == 1 || status == 2)) {
                return Result.error("当前状态不允许叫停");
            }
            boolean ok = sessionMapper.update(null, new UpdateWrapper<Session>()
                    .eq("session_id", id)
                    .in("session_status", 1, 2)
                    .set("session_status", 4)
                    .set("cancel_reason", "管理员叫停")
                    .set("update_time", LocalDateTime.now())) > 0;
            return ok ? Result.success("已取消") : Result.error("叫停失败");
        }

        return Result.error("未知action参数");
    }

    private boolean applySessionUpdate(Session existing, String title, String location, LocalDateTime startTime, LocalDateTime endTime, Integer capacity, String description) {
        Integer status = existing.getSessionStatus();
        if (capacity != null && capacity <= 0) {
            return false;
        }
        if ((startTime != null || endTime != null) && (startTime == null || endTime == null || !endTime.isAfter(startTime))) {
            return false;
        }
        UpdateWrapper<Session> uw = new UpdateWrapper<>();
        uw.eq("session_id", existing.getSessionId());
        boolean any = false;
        if (title != null) {
            uw.set("session_title", title);
            any = true;
        }
        if (location != null) {
            uw.set("session_location", location);
            any = true;
        }
        if (description != null) {
            uw.set("description", description);
            any = true;
        }
        if (startTime != null && endTime != null) {
            uw.set("start_time", startTime);
            uw.set("end_time", endTime);
            if (status != null && status == 2) {
                uw.set("checkin_start", startTime.minusMinutes(20));
                uw.set("checkin_end", startTime.plusMinutes(15));
            }
            any = true;
        }
        if (capacity != null) {
            if (status != null && status == 2) {
                Integer oldCap = existing.getCapacity() == null ? 0 : existing.getCapacity();
                Integer oldRemain = existing.getRemainingSeats() == null ? 0 : existing.getRemainingSeats();
                int used = Math.max(0, oldCap - oldRemain);
                int newRemain = Math.max(0, capacity - used);
                uw.set("capacity", capacity);
                uw.set("remaining_seats", newRemain);
            } else {
                uw.set("capacity", capacity);
                uw.set("remaining_seats", capacity);
            }
            any = true;
        }
        if (!any) {
            return true;
        }
        uw.set("update_time", LocalDateTime.now());
        return sessionMapper.update(null, uw) > 0;
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
        if (status == 0) {
            update.setLimitTime(java.time.LocalDateTime.now());
        } else if (status == 1) {
            update.setLimitTime(null);
            // MyBatis-Plus 默认不更新 null，需要用 UpdateWrapper
            studentService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Student>()
                    .eq("student_id", id)
                    .set("status", 1)
                    .set("limit_time", null));
            return Result.success("操作成功");
        }
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
        data.put("limitTime", student.getLimitTime());
        if (student.getStatus() != null && student.getStatus() == 0 && student.getLimitTime() != null) {
            data.put("unbanTime", student.getLimitTime().plusDays(7));
        } else {
            data.put("unbanTime", null);
        }
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

