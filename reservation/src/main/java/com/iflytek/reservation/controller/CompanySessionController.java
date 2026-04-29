package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Checkin;
import com.iflytek.reservation.entity.Company;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.mapper.CheckinMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
public class CompanySessionController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private boolean isCompanyActive(Long companyId) {
        Company company = companyService.getById(companyId);
        return company != null && company.getStatus() != null && company.getStatus() == 1;
    }

    @GetMapping("/sessions")
    public Result<?> listSessions(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size
    ) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.max(1, size);
        long offset = (safeCurrent - 1) * safeSize;

        QueryWrapper<Session> countWrapper = new QueryWrapper<>();
        countWrapper.eq("company_id", companyId);
        if (status != null) {
            countWrapper.eq("session_status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            countWrapper.and(w -> w.like("session_title", kw).or().like("session_location", kw));
        }
        long total = sessionMapper.selectCount(countWrapper);

        QueryWrapper<Session> wrapper = new QueryWrapper<>();
        wrapper.eq("company_id", companyId);
        if (status != null) {
            wrapper.eq("session_status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("session_title", kw).or().like("session_location", kw));
        }
        wrapper.orderByDesc("session_id");
        wrapper.last("LIMIT " + offset + "," + safeSize);
        List<Session> records = sessionMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("current", safeCurrent);
        data.put("size", safeSize);
        return Result.success(data);
    }

    @GetMapping("/session/{id}")
    public Result<?> sessionDetail(HttpServletRequest request, @PathVariable("id") Long id) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        Session session = sessionMapper.selectById(id);
        if (session == null || session.getCompanyId() == null || !session.getCompanyId().equals(companyId)) {
            return Result.error(404, "宣讲会不存在");
        }
        long checkinCount = checkinMapper.selectCount(new QueryWrapper<Checkin>().eq("session_id", id));
        Map<String, Object> data = new HashMap<>();
        data.put("session", session);
        data.put("checkinCount", checkinCount);
        return Result.success(data);
    }

    @PostMapping("/session")
    public Result<?> createSession(HttpServletRequest request, @RequestBody SessionUpsertRequest body) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        if (!isCompanyActive(companyId)) {
            return Result.error(403, "企业资质状态非已入驻，无法发布宣讲会");
        }
        if (body.getCapacity() == null || body.getCapacity() <= 0) {
            return Result.error("容纳人数不合法");
        }
        if (body.getStartTime() == null || body.getEndTime() == null || !body.getEndTime().isAfter(body.getStartTime())) {
            return Result.error("宣讲时间不合法");
        }
        Session s = new Session();
        s.setCompanyId(companyId);
        s.setSessionTitle(body.getSessionTitle());
        s.setDescription(body.getDescription());
        s.setSessionLocation(body.getSessionLocation());
        s.setCapacity(body.getCapacity());
        s.setRemainingSeats(body.getCapacity());
        s.setSessionStatus(0);
        s.setStartTime(body.getStartTime());
        s.setEndTime(body.getEndTime());
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        int inserted = sessionMapper.insert(s);
        if (inserted <= 0) {
            return Result.error("提交失败");
        }
        return Result.success(s.getSessionId());
    }

    @PutMapping("/session/{id}")
    public Result<?> updateSession(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody SessionUpsertRequest body) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        if (!isCompanyActive(companyId)) {
            return Result.error(403, "企业资质状态非已入驻，无法发布宣讲会");
        }
        Session existing = sessionMapper.selectById(id);
        if (existing == null || existing.getCompanyId() == null || !existing.getCompanyId().equals(companyId)) {
            return Result.error(404, "宣讲会不存在");
        }
        Integer status = existing.getSessionStatus();
        if (status == null || !(status == 0 || status == 1 || status == 5)) {
            return Result.error("当前状态不允许修改");
        }
        if (body.getCapacity() == null || body.getCapacity() <= 0) {
            return Result.error("容纳人数不合法");
        }
        if (body.getStartTime() == null || body.getEndTime() == null || !body.getEndTime().isAfter(body.getStartTime())) {
            return Result.error("宣讲时间不合法");
        }
        boolean ok = sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", id)
                .eq("company_id", companyId)
                .set("session_title", body.getSessionTitle())
                .set("description", body.getDescription())
                .set("session_location", body.getSessionLocation())
                .set("capacity", body.getCapacity())
                .set("remaining_seats", body.getCapacity())
                .set("start_time", body.getStartTime())
                .set("end_time", body.getEndTime())
                .set("session_status", 0)
                .set("audit_time", null)
                .set("audit_remark", null)
                .set("publish_time", null)
                .set("cancel_reason", null)
                .set("update_time", LocalDateTime.now())) > 0;
        return ok ? Result.success("提交成功") : Result.error("提交失败");
    }

    @PostMapping("/session/{id}/publish")
    public Result<?> publishSession(HttpServletRequest request, @PathVariable("id") Long id) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        if (!isCompanyActive(companyId)) {
            return Result.error(403, "企业资质状态非已入驻，无法发布宣讲会");
        }
        Session existing = sessionMapper.selectById(id);
        if (existing == null || existing.getCompanyId() == null || !existing.getCompanyId().equals(companyId)) {
            return Result.error(404, "宣讲会不存在");
        }
        if (existing.getSessionStatus() == null || existing.getSessionStatus() != 1) {
            return Result.error("当前状态不允许发布");
        }
        LocalDateTime startTime = existing.getStartTime();
        LocalDateTime checkinStart = startTime == null ? null : startTime.minus(20, ChronoUnit.MINUTES);
        LocalDateTime checkinEnd = startTime == null ? null : startTime.plus(15, ChronoUnit.MINUTES);
        boolean ok = sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", id)
                .eq("company_id", companyId)
                .eq("session_status", 1)
                .set("session_status", 2)
                .set("publish_time", LocalDateTime.now())
                .set("remaining_seats", existing.getCapacity())
                .set("checkin_start", checkinStart)
                .set("checkin_end", checkinEnd)
                .set("update_time", LocalDateTime.now())) > 0;
        if (ok && stringRedisTemplate != null) {
            try {
                String seatKey = "session:seat:" + id;
                String userSetKey = "session:reserved:" + id;
                Integer capacity = existing.getCapacity();
                if (capacity != null) {
                    stringRedisTemplate.opsForValue().set(seatKey, String.valueOf(capacity));
                    stringRedisTemplate.delete(userSetKey);
                }
            } catch (Exception ignored) {
            }
        }
        return ok ? Result.success("已发布") : Result.error("发布失败");
    }

    @PostMapping("/session/{id}/cancel")
    public Result<?> cancelSession(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long companyId = AuthTokenUtil.extractCompanyId(request);
        if (companyId == null) {
            return Result.error(401, "未登录");
        }
        Session existing = sessionMapper.selectById(id);
        if (existing == null || existing.getCompanyId() == null || !existing.getCompanyId().equals(companyId)) {
            return Result.error(404, "宣讲会不存在");
        }
        Integer status = existing.getSessionStatus();
        if (status == null || !(status == 0 || status == 1 || status == 2 || status == 5)) {
            return Result.error("当前状态不允许取消");
        }
        String cancelReason = body == null ? null : (String) body.get("cancelReason");
        if (!(status == 0 || status == 5)) {
            if (cancelReason == null || cancelReason.isBlank()) {
                return Result.error("取消原因不能为空");
            }
            cancelReason = cancelReason.trim();
        } else {
            if (cancelReason != null) {
                cancelReason = cancelReason.trim();
                if (cancelReason.isBlank()) {
                    cancelReason = null;
                }
            }
        }
        boolean ok = sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", id)
                .eq("company_id", companyId)
                .set("session_status", 4)
                .set("cancel_reason", cancelReason)
                .set("update_time", LocalDateTime.now())) > 0;
        return ok ? Result.success("已取消") : Result.error("取消失败");
    }

    public static class SessionUpsertRequest {
        private String sessionTitle;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String sessionLocation;
        private Integer capacity;
        private String description;

        public String getSessionTitle() {
            return sessionTitle;
        }

        public void setSessionTitle(String sessionTitle) {
            this.sessionTitle = sessionTitle;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getSessionLocation() {
            return sessionLocation;
        }

        public void setSessionLocation(String sessionLocation) {
            this.sessionLocation = sessionLocation;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
