package com.iflytek.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String password;
    private String phone;
    private Integer gender;
    private String major;
    @TableField("class")
    private String clazz;
    private String college;
    private Integer status;
    private java.time.LocalDateTime limitTime;
    private java.time.LocalDateTime unbanTime;
}
