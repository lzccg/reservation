package com.iflytek.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("company")
public class Company {
    @TableId(type = IdType.AUTO)
    private Long companyId;
    private String companyName;
    private String creditCode;
    private String companyLocation;
    private String industry;
    private String contactName;
    private String contactPhone;
    private String email;
    private String password;
    private String address;
    private Integer status;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime auditTime;
}
