package com.iflytek.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("checkin")
public class Checkin {
    @TableId(type = IdType.AUTO)
    private Long checkinId;
    private Long studentId;
    private Long sessionId;
    private LocalDateTime checkinTime;
    private Integer checkinStatus;
}
