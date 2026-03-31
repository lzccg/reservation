package com.iflytek.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("face_data")
public class FaceData {
    @TableId(type = IdType.AUTO)
    private Long faceId;
    private Long studentId;
    private String faceFeature;
    private String faceUrl;
    private LocalDateTime createTime;
}
