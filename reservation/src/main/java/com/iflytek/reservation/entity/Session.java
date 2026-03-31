package com.iflytek.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("session")
public class Session {
    @TableId(type = IdType.AUTO)
    private Long sessionId;
    private Long companyId;
    private String sessionTitle;
    private String description;
    private String sessionLocation;
    private Integer capacity;
    private Integer remainingSeats;
    private Integer sessionStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkinStart;
    private LocalDateTime checkinEnd;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime publishTime;
    private LocalDateTime auditTime;
    private String auditRemark;
    private String cancelReason;
}
