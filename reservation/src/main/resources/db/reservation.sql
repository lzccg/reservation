/*
 Navicat Premium Data Transfer

 Source Server         : hw
 Source Server Type    : MySQL
 Source Server Version : 80029
 Source Host           : localhost:3306
 Source Schema         : reservation

 Target Server Type    : MySQL
 Target Server Version : 80029
 File Encoding         : 65001

 Date: 01/04/2026 22:30:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，管理员ID',
  `admin_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员姓名/登录名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `role` tinyint NOT NULL COMMENT '角色级别（1-超级管理员，2-普通管理员）',
  `status` tinyint NULL DEFAULT 1 COMMENT '账号状态（1-正常，0-禁用）',
  PRIMARY KEY (`admin_id`) USING BTREE,
  UNIQUE INDEX `admin_name`(`admin_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用于存储后台统筹与审核的管理员账号信息 。' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', '123456', NULL, 1, 1);
INSERT INTO `admin` VALUES (2, 'test', '$2a$10$t4VTgqbhhaTZH4Pprv3jyOmfsMEaLIxjxqEdu3k96kVvGvhhUh9dy', NULL, 2, 1);

-- ----------------------------
-- Table structure for checkin
-- ----------------------------
DROP TABLE IF EXISTS `checkin`;
CREATE TABLE `checkin`  (
  `checkin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，签到记录ID',
  `student_id` bigint NOT NULL COMMENT '签到学生的ID（外键关联 student 表）',
  `session_id` bigint NOT NULL COMMENT '签到的宣讲会ID（外键关联 session 表）',
  `checkin_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实际完成签到的时间',
  `checkin_status` tinyint NULL DEFAULT 1 COMMENT '签到状态（0-已签到，1-迟到）',
  PRIMARY KEY (`checkin_id`) USING BTREE,
  INDEX `签到学生的ID`(`student_id`) USING BTREE,
  INDEX `签到的宣讲会ID`(`session_id`) USING BTREE,
  CONSTRAINT `签到学生的ID` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `签到的宣讲会ID` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存放现场刷脸比对成功后的履约数据.' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of checkin
-- ----------------------------

-- ----------------------------
-- Table structure for company
-- ----------------------------
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company`  (
  `company_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，企业唯一ID',
  `company_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业全称',
  `credit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业统一社会信用代码',
  `company_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业所在地',
  `industry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业所属行业',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业联系人电话',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业详细地址',
  `status` tinyint NULL DEFAULT 0 COMMENT '账号/审核状态（0-待审，1-正常，2-驳回，3-已注销）',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账号注册审核的备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '企业注册时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '企业信息最后修改时间',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '账号审核通过的时间',
  PRIMARY KEY (`company_id`) USING BTREE,
  UNIQUE INDEX `credit_code`(`credit_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用于存储发布宣讲会的企业资质信息与审核状态 。' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of company
-- ----------------------------
INSERT INTO `company` VALUES (1, '科大讯飞有限公司', 'kdxf123456', '安徽省芜湖市', '计算机/互联网', 'abb', '12345678901', '$2a$10$ODQzD.U1amT4zyVNRQPJeOCTwFMMIX81sKkyOgIkhiA0jei/jIwJ.', '安徽省芜湖市000001-1-1', 1, NULL, '2026-03-31 16:01:59', '2026-04-01 22:04:02', '2026-04-01 22:00:15');
INSERT INTO `company` VALUES (2, '????', 'CREDIT001', '??', '计算机/互联网', '??', '13900000000', '$2a$10$DV6nA3q.kLlHwW30o7hQ8.dGczivnAQsLWlMRPBKfBuLv0qmZtiVm', NULL, 3, '111', '2026-03-31 16:36:30', '2026-04-01 22:04:05', NULL);
INSERT INTO `company` VALUES (3, '上海墨水厂', 'shmsc000000123', '上海市嘉定区', '制造/工业', '尚墨水', '12300000000', '$2a$10$hXp23/5PPrh/RXt6enx1SeQDxve7yffEbY.yucuw4HOdfvd.E1pCi', '上海市嘉定区某路00001号', 0, NULL, '2026-04-01 22:28:17', '2026-04-01 22:28:43', NULL);

-- ----------------------------
-- Table structure for face_data
-- ----------------------------
DROP TABLE IF EXISTS `face_data`;
CREATE TABLE `face_data`  (
  `face_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，人脸数据ID',
  `student_id` bigint NOT NULL COMMENT '所属学生的ID（外键关联 student 表）',
  `face_feature` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提取的人脸特征向量数据',
  `face_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始底片存放路径',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '照片上传时间',
  PRIMARY KEY (`face_id`) USING BTREE,
  INDEX `所属学生的ID`(`student_id`) USING BTREE,
  CONSTRAINT `所属学生的ID` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储用于 1:1 比对基准的图像特征数据，每个学生可对应多条更新记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of face_data
-- ----------------------------

-- ----------------------------
-- Table structure for reservation
-- ----------------------------
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation`  (
  `reservation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，预约记录ID',
  `student_id` bigint NOT NULL COMMENT '预约学生的ID（外键关联 student 表）',
  `session_id` bigint NOT NULL COMMENT '预约的宣讲会ID（外键关联 session 表）',
  `reservation_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约成功的时间',
  `reservation_status` tinyint NULL DEFAULT 0 COMMENT '状态（0正常，1已取消，2已签到，3爽约）',
  PRIMARY KEY (`reservation_id`) USING BTREE,
  INDEX `预约学生的ID`(`student_id`) USING BTREE,
  INDEX `预约的宣讲会ID`(`session_id`) USING BTREE,
  CONSTRAINT `预约学生的ID` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `预约的宣讲会ID` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '记录学生成功抢座的凭证.' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reservation
-- ----------------------------

-- ----------------------------
-- Table structure for session
-- ----------------------------
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session`  (
  `session_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，宣讲会ID',
  `company_id` bigint NOT NULL COMMENT '发布企业ID（外键关联 company 表）',
  `session_title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '宣讲会主题/标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '宣讲会详情描述',
  `session_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '宣讲场地',
  `capacity` int NOT NULL COMMENT '场地总容量',
  `remaining_seats` int NOT NULL COMMENT '剩余座位',
  `session_status` tinyint NULL DEFAULT 0 COMMENT '状态（0-待审，1-已审核，2-已发布，3-已结束，4-已取消，5-已驳回）',
  `start_time` datetime NOT NULL COMMENT '宣讲会开始时间',
  `end_time` datetime NOT NULL COMMENT '宣讲会结束时间',
  `checkin_start` datetime NULL DEFAULT NULL COMMENT '签到开始时间',
  `checkin_end` datetime NULL DEFAULT NULL COMMENT '签到结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取消原因',
  PRIMARY KEY (`session_id`) USING BTREE,
  INDEX `发布企业ID`(`company_id`) USING BTREE,
  CONSTRAINT `发布企业ID` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统核心业务表，记录宣讲会活动详情与场地容量管控 。' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of session
-- ----------------------------

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，学生唯一ID',
  `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学生真实姓名',
  `student_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别（0-女，1-男）',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属专业',
  `class` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '班级',
  `college` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学院',
  `status` tinyint NULL DEFAULT 1 COMMENT '账号状态（1-正常，0-禁用）',
  PRIMARY KEY (`student_id`) USING BTREE,
  UNIQUE INDEX `student_no`(`student_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '主要用于存储系统前台学生用户的基本信息与账号状态 。' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, 'lzj', '320202020231', '$2a$10$Hz3fu3UZxa3oOCA8amLZPe4gwdOX.rGVUYRAnieMT6zGbhSy6JMnG', '13700000000', 1, '软件工程', '软件工程2204', '计算机与软件工程学院', 1);
INSERT INTO `student` VALUES (2, '????', 'S10001', '$2a$10$DismLsJzWZPkL1zj80rRdO3B3UNm31gShxBO7fyico2PCfCIDqz2y', '13800000000', NULL, NULL, NULL, NULL, 2);
INSERT INTO `student` VALUES (3, 'abc', '321123456', '$2a$10$cFFcx13G2PNKOUx.5ou6MeR.lMqwP8Ba9gDQZw8E8N1dUmxE2edYK', '13566666666', 0, '软件工程', '软件工程2304班', '计算机学院', 1);

SET FOREIGN_KEY_CHECKS = 1;
