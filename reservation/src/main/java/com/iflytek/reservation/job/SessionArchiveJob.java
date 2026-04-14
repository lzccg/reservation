package com.iflytek.reservation.job;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iflytek.reservation.entity.Session;
import com.iflytek.reservation.mapper.SessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SessionArchiveJob {

    @Autowired
    private SessionMapper sessionMapper;

    @Scheduled(fixedDelay = 60000)
    public void archiveEndedSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionMapper.update(null, new UpdateWrapper<Session>()
                .eq("session_status", 2)
                .isNotNull("end_time")
                .lt("end_time", now)
                .set("session_status", 3));
    }
}
