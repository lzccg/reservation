package com.iflytek.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.common.AuthTokenUtil;
import com.iflytek.reservation.common.Result;
import com.iflytek.reservation.entity.Reservation;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.entity.Student;
import com.iflytek.reservation.mapper.ReservationMapper;
import com.iflytek.reservation.mapper.SessionMapper;
import com.iflytek.reservation.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/student")
public class StudentReservationController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT;
    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT;

    static {
        RESERVE_SCRIPT = new DefaultRedisScript<>();
        RESERVE_SCRIPT.setResultType(Long.class);
        RESERVE_SCRIPT.setScriptText(
                "local seatKey = KEYS[1] " +
                        "local userSetKey = KEYS[2] " +
                        "local userId = ARGV[1] " +
                        "if redis.call('sismember', userSetKey, userId) == 1 then " +
                        "  return -1 " +
                        "end " +
                        "local seats = tonumber(redis.call('get', seatKey)) " +
                        "if seats == nil then " +
                        "  return -2 " +
                        "end " +
                        "if seats <= 0 then " +
                        "  return 0 " +
                        "end " +
                        "redis.call('decr', seatKey) " +
                        "redis.call('sadd', userSetKey, userId) " +
                        "return 1 "
        );

        ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        ROLLBACK_SCRIPT.setResultType(Long.class);
        ROLLBACK_SCRIPT.setScriptText(
                "local seatKey = KEYS[1] " +
                        "local userSetKey = KEYS[2] " +
                        "local userId = ARGV[1] " +
                        "redis.call('incr', seatKey) " +
                        "redis.call('srem', userSetKey, userId) " +
                        "return 1 "
        );
    }

    @PostMapping("/reserve/{sessionId}")
    @Transactional
    public Result<?> reserve(HttpServletRequest request, @PathVariable("sessionId") Long sessionId) {
        Long studentId = AuthTokenUtil.extractStudentId(request);
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Student student = studentService.getById(studentId);
        if (student == null || (student.getStatus() != null && student.getStatus() == 2)) {
            return Result.error(404, "用户不存在");
        }
        studentService.refreshStudentStatus(student);
        if (student.getStatus() == null || student.getStatus() != 1) {
            return Result.error(403, "当前账号状态禁止预约");
        }

        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.error(404, "宣讲会不存在");
        }
        if (session.getSessionStatus() == null || session.getSessionStatus() != 2) {
            return Result.error("该宣讲会当前不可预约");
        }
        if (session.getStartTime() != null && !session.getStartTime().isAfter(LocalDateTime.now())) {
            return Result.error("宣讲会已开始，无法预约");
        }

        boolean usedRedis = false;
        String seatKey = "session:seat:" + sessionId;
        String userSetKey = "session:reserved:" + sessionId;

        if (stringRedisTemplate != null) {
            try {
                Long luaResult = stringRedisTemplate.execute(
                        RESERVE_SCRIPT,
                        Arrays.asList(seatKey, userSetKey),
                        String.valueOf(studentId)
                );
                if (luaResult != null) {
                    if (luaResult == -1L) {
                        return Result.error("已预约该宣讲会");
                    }
                    if (luaResult == 0L) {
                        return Result.error("名额已满");
                    }
                    if (luaResult == 1L) {
                        usedRedis = true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (usedRedis) {
            Long exists = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                    .eq("student_id", studentId)
                    .eq("session_id", sessionId)
                    .in("reservation_status", 0, 2, 3));
            if (exists != null && exists > 0) {
                tryRollbackRedis(seatKey, userSetKey, studentId);
                return Result.error("已预约该宣讲会");
            }

            int updated = sessionMapper.update(null, new UpdateWrapper<Session>()
                    .eq("session_id", sessionId)
                    .eq("session_status", 2)
                    .gt("remaining_seats", 0)
                    .setSql("remaining_seats = remaining_seats - 1"));
            if (updated <= 0) {
                tryRollbackRedis(seatKey, userSetKey, studentId);
                return Result.error("名额已满");
            }

            try {
                Reservation reservation = new Reservation();
                reservation.setStudentId(studentId);
                reservation.setSessionId(sessionId);
                reservation.setReservationTime(LocalDateTime.now());
                reservation.setReservationStatus(0);
                reservationMapper.insert(reservation);
                return Result.success("预约成功");
            } catch (Exception e) {
                tryRollbackRedis(seatKey, userSetKey, studentId);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("系统繁忙，请稍后再试");
            }
        }

        Long exists = reservationMapper.selectCount(new QueryWrapper<Reservation>()
                .eq("student_id", studentId)
                .eq("session_id", sessionId)
                .in("reservation_status", 0, 2, 3));
        if (exists != null && exists > 0) {
            return Result.error("已预约该宣讲会");
        }

        int updated = sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_id", sessionId)
                .eq("session_status", 2)
                .gt("remaining_seats", 0)
                .setSql("remaining_seats = remaining_seats - 1"));
        if (updated <= 0) {
            return Result.error("名额已满");
        }

        Reservation reservation = new Reservation();
        reservation.setStudentId(studentId);
        reservation.setSessionId(sessionId);
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setReservationStatus(0);
        reservationMapper.insert(reservation);
        return Result.success("预约成功");
    }

    private void tryRollbackRedis(String seatKey, String userSetKey, Long studentId) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.execute(
                    ROLLBACK_SCRIPT,
                    Arrays.asList(seatKey, userSetKey),
                    String.valueOf(studentId)
            );
        } catch (Exception ignored) {
        }
    }
}
