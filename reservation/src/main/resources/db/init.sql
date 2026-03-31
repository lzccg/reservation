-- Create database if not exists
CREATE DATABASE IF NOT EXISTS reservation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE reservation;

-- Drop tables if exist
DROP TABLE IF EXISTS `face_data`;
DROP TABLE IF EXISTS `checkin`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `session`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `student`;

-- 1. 学生表 (student) 
CREATE TABLE `student` ( 
  `student_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，学生唯一ID', 
  `student_name` VARCHAR(50) NOT NULL COMMENT '学生真实姓名', 
  `student_no` VARCHAR(20) NOT NULL UNIQUE COMMENT '学号（通常作为登录账号）', 
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码（需加密存储）', 
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号', 
  `gender` TINYINT DEFAULT NULL COMMENT '性别（如：0-女，1-男）', 
  `major` VARCHAR(100) DEFAULT NULL COMMENT '所属专业', 
  `class` VARCHAR(20) DEFAULT NULL COMMENT '班级', 
  `college` VARCHAR(100) DEFAULT NULL COMMENT '所属学院', 
  `status` TINYINT DEFAULT 1 COMMENT '账号状态（如：1-正常，0-禁用）', 
  PRIMARY KEY (`student_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表'; 

-- 2. 企业表 (company) 
CREATE TABLE `company` ( 
  `company_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，企业唯一ID', 
  `company_name` VARCHAR(100) NOT NULL COMMENT '企业全称', 
  `credit_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '企业统一社会信用代码', 
  `company_location` VARCHAR(100) DEFAULT NULL COMMENT '企业所在地', 
  `industry` VARCHAR(50) DEFAULT NULL COMMENT '企业所属行业', 
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '企业联系人姓名', 
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '企业联系人电话', 
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码（加密存储）', 
  `address` VARCHAR(255) DEFAULT NULL COMMENT '企业详细地址', 
  `status` TINYINT DEFAULT 0 COMMENT '账号/审核状态（0-待审，1-正常，2-驳回）', 
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '账号注册审核的备注', 
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '企业注册时间', 
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '企业信息最后修改时间', 
  `audit_time` DATETIME DEFAULT NULL COMMENT '账号审核通过的时间', 
  PRIMARY KEY (`company_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表'; 

-- 3. 管理员表 (admin) 
CREATE TABLE `admin` ( 
  `admin_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，管理员ID', 
  `admin_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员姓名/登录名', 
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码', 
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话', 
  `role` TINYINT NOT NULL COMMENT '角色级别（1-超级管理员，2-普通管理员）', 
  `status` TINYINT DEFAULT 1 COMMENT '账号状态（1-正常，0-禁用）', 
  PRIMARY KEY (`admin_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表'; 

-- 4. 宣讲会表 (session) 
CREATE TABLE `session` ( 
  `session_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，宣讲会ID', 
  `company_id` BIGINT NOT NULL COMMENT '发布企业ID（外键关联 company 表）', 
  `session_title` VARCHAR(150) NOT NULL COMMENT '宣讲会主题/标题', 
  `description` TEXT DEFAULT NULL COMMENT '宣讲会详情描述', 
  `session_location` VARCHAR(100) NOT NULL COMMENT '宣讲场地', 
  `capacity` INT NOT NULL COMMENT '场地总容量', 
  `remaining_seats` INT NOT NULL COMMENT '剩余座位', 
  `session_status` TINYINT DEFAULT 0 COMMENT '状态（0-待审，1-已发布，2-已结束，3-已取消）', 
  `start_time` DATETIME NOT NULL COMMENT '宣讲会开始时间', 
  `end_time` DATETIME NOT NULL COMMENT '宣讲会结束时间', 
  `checkin_start` DATETIME DEFAULT NULL COMMENT '签到开始时间', 
  `checkin_end` DATETIME DEFAULT NULL COMMENT '签到结束时间', 
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', 
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', 
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间', 
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间', 
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注', 
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因', 
  PRIMARY KEY (`session_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宣讲会表'; 

-- 5. 预约记录表 (reservation) 
CREATE TABLE `reservation` ( 
  `reservation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，预约记录ID', 
  `student_id` BIGINT NOT NULL COMMENT '预约学生的ID（外键关联 student 表）', 
  `session_id` BIGINT NOT NULL COMMENT '预约的宣讲会ID（外键关联 session 表）', 
  `reservation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预约成功的时间', 
  `reservation_status` TINYINT DEFAULT 0 COMMENT '状态（0正常，1已取消，2已签到，3爽约）', 
  PRIMARY KEY (`reservation_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表'; 

-- 6. 签到记录表 (checkin) 
CREATE TABLE `checkin` ( 
  `checkin_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，签到记录ID', 
  `student_id` BIGINT NOT NULL COMMENT '签到学生的ID（外键关联 student 表）', 
  `session_id` BIGINT NOT NULL COMMENT '签到的宣讲会ID（外键关联 session 表）', 
  `checkin_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '实际完成签到的时间', 
  `checkin_status` TINYINT DEFAULT 1 COMMENT '签到状态（1-已签到，2-迟到，0-未签到）', 
  PRIMARY KEY (`checkin_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表'; 

-- 7. 人脸数据表 (face_data) 
CREATE TABLE `face_data` ( 
  `face_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，人脸数据ID', 
  `student_id` BIGINT NOT NULL COMMENT '所属学生的ID（外键关联 student 表）', 
  `face_feature` TEXT NOT NULL COMMENT '提取的人脸特征向量数据', 
  `face_url` VARCHAR(255) DEFAULT NULL COMMENT '原始底片存放路径（OSS 或本地 URL）', 
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '照片上传时间', 
  PRIMARY KEY (`face_id`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸数据表';

-- Insert default admin
INSERT INTO admin (admin_name, password, role) VALUES ('admin', '123456', 1);
