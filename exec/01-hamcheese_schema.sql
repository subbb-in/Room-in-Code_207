CREATE DATABASE  IF NOT EXISTS `ssafy_hamcheese` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ssafy_hamcheese`;

-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
-- Host: localhost    Database: ssafy_hamcheese
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- ------------------------------------------------------
-- Table structure for table `ai_reports`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `ai_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `ai_reports` (
  `report_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `game_result` json DEFAULT NULL COMMENT '게임 결과 데이터',
  `graded_result` json DEFAULT NULL COMMENT '채점 결과',
  `ai_feedback` json DEFAULT NULL COMMENT 'AI 피드백 데이터',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `problem_title` varchar(255) NOT NULL,
  `history_id` bigint NOT NULL COMMENT 'FK: history 참조',
  PRIMARY KEY (`report_id`),
  KEY `FK_REPORT_HISTORY` (`history_id`),
  CONSTRAINT `FK_REPORT_HISTORY` FOREIGN KEY (`history_id`) REFERENCES `history` (`history_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `ai_reports` WRITE;
/*!40000 ALTER TABLE `ai_reports` DISABLE KEYS */;
INSERT INTO `ai_reports` VALUES
(1,'{\"time\": \"20ms\", \"score\": 100}',NULL,'{\"comment\": \"효율적인 코드입니다!\"}','2026-01-27 00:44:15','Sum of two numbers',1),
(2,'{\"time\": \"150ms\", \"score\": 80}',NULL,'{\"comment\": \"공간 복잡도를 최적화할 수 있습니다.\"}','2026-01-27 00:44:15','Knapsack Problem',2);
/*!40000 ALTER TABLE `ai_reports` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `history`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `problem_id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT 'FK: users 참조',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `FK_HISTORY_USER` (`user_id`),
  KEY `FK_HISTORY_PROBLEM` (`problem_id`),
  CONSTRAINT `FK_HISTORY_PROBLEM` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`problem_id`),
  CONSTRAINT `FK_HISTORY_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `history` WRITE;
/*!40000 ALTER TABLE `history` DISABLE KEYS */;
INSERT INTO `history` VALUES
(1,1,1,'2026-01-27 00:44:15'),
(2,2,1,'2026-01-27 00:44:15'),
(3,1,2,'2026-01-27 00:44:15'),
(4,5,3,'2026-01-27 00:44:15');
/*!40000 ALTER TABLE `history` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `local`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `local`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `local` (
  `login_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL COMMENT 'Hash값',
  `user_id` bigint NOT NULL COMMENT 'FK: users 테이블 참조',
  PRIMARY KEY (`login_id`),
  UNIQUE KEY `UK_LOCAL_EMAIL` (`email`),
  KEY `FK_USER_LOCAL` (`user_id`),
  CONSTRAINT `FK_USER_LOCAL` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `local` WRITE;
/*!40000 ALTER TABLE `local` DISABLE KEYS */;
INSERT INTO `local` VALUES
(1,'charlie@ssafy.com','$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2',1),
(2,'master@ssafy.com','4',3);
/*!40000 ALTER TABLE `local` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `problem_hints`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `problem_hints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `problem_hints` (
  `hint_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `sequence` int NOT NULL COMMENT '힌트 순서 (1, 2, 3 등)',
  `content` text NOT NULL COMMENT '힌트 내용',
  `problem_id` bigint NOT NULL COMMENT 'FK: problems 참조',
  PRIMARY KEY (`hint_id`),
  KEY `FK_HINT_PROBLEM` (`problem_id`),
  CONSTRAINT `FK_HINT_PROBLEM` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`problem_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `problem_hints` WRITE;
/*!40000 ALTER TABLE `problem_hints` DISABLE KEYS */;
INSERT INTO `problem_hints` VALUES
(1,1,'브루트 포스로 모든 쌍을 확인해보세요.',1),
(2,2,'정렬 후 투 포인터를 사용하면 더 빠릅니다.',1),
(3,1,'이전에 계산한 가치를 테이블에 저장하세요.',2);
/*!40000 ALTER TABLE `problem_hints` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `problem_skills`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `problem_skills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `problem_skills` (
  `problem_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  PRIMARY KEY (`problem_id`,`skill_id`),
  KEY `FK_SKILLS_TO_PROBLEMS` (`skill_id`),
  CONSTRAINT `FK_PROBLEMS_TO_SKILLS` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`problem_id`) ON DELETE CASCADE,
  CONSTRAINT `FK_SKILLS_TO_PROBLEMS` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `problem_skills` WRITE;
/*!40000 ALTER TABLE `problem_skills` DISABLE KEYS */;
INSERT INTO `problem_skills` VALUES
(3,2),(1,3),(2,5),(4,7),(5,8);
/*!40000 ALTER TABLE `problem_skills` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `problems`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `problems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `problems` (
  `problem_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `question` text NOT NULL,
  `solutions` text COMMENT '정답 코드',
  `difficulty` enum('EASY','MEDIUM','MEDIUM_HARD','HARD','VERY_HARD') NOT NULL DEFAULT 'EASY',
  `source` varchar(255) DEFAULT NULL COMMENT '코드 출처',
  `url` varchar(500) DEFAULT NULL,
  `time_limit` varchar(50) DEFAULT NULL,
  `memory_limit` varchar(50) DEFAULT NULL,
  `input_output` json DEFAULT NULL,
  `expected_aux_space` varchar(100) DEFAULT NULL,
  `expected_time_complexity` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`problem_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `problems` WRITE;
/*!40000 ALTER TABLE `problems` DISABLE KEYS */;
INSERT INTO `problems` VALUES
(1,'The city park of IT City contains n east to west paths and n north to south paths. Each east to west path crosses each north to south path, so there are n^2 intersections.\n\nThe city funded purchase of five benches. To make it seems that there are many benches it was decided to place them on as many paths as possible. Obviously this requirement is satisfied by the following scheme: each bench is placed on a cross of paths and each path contains not more than one bench.\n\nHelp the park administration count the number of ways to place the benches.\n\n\n-----Input-----\n\nThe only line of the input contains one integer n (5 ≤ n ≤ 100) — the number of east to west paths and north to south paths.\n\n\n-----Output-----\n\nOutput one integer — the number of ways to place the benches.\n\n\n-----Examples-----\nInput\n5\n\nOutput\n120',
'["n = int(input())\ncn5 = n * (n - 1) // 2 * (n - 2) // 3 * (n - 3) // 4 * (n - 4) // 5\nan5 = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\nprint(cn5 * an5)\n"]',
'EASY','Baekjoon','https://www.acmicpc.net/problem/1000','1s','128MB','{"inputs": ["5"], "outputs": ["120"]}',NULL,NULL),
(2,'Knapsack Problem','int dp[101][100001];','HARD','Programmers','https://programmers.co.kr/1','2s','256MB','{"inputs": ["items..."], "outputs": ["max_val"]}',NULL,NULL),
(3,'Bitwise XOR operations','result ^= arr[i];','MEDIUM','LeetCode','https://leetcode.com/1','1s','64MB','{"inputs": ["[1,2,1]"], "outputs": ["2"]}',NULL,NULL),
(4,'Find median in range','update(1, 1, n, i, val);','VERY_HARD','Codeforces','https://codeforces.com/1','3s','512MB','{"inputs": ["queries..."], "outputs": ["medians"]}',NULL,NULL),
(5,'Quick Sort Implementation','partition(arr, low, high);','MEDIUM','Basic',NULL,'1s','128MB','{"inputs": ["5 3 1"], "outputs": ["1 3 5"]}',NULL,NULL);
/*!40000 ALTER TABLE `problems` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `skills`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `skills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `skills` (
  `skill_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `skill_name` varchar(100) NOT NULL COMMENT '8개 속성 (BFS, DP 등)',
  PRIMARY KEY (`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `skills` WRITE;
/*!40000 ALTER TABLE `skills` DISABLE KEYS */;
INSERT INTO `skills` VALUES
(1,'Amortized analysis'),
(2,'Bit manipulation'),
(3,'Complete search'),
(4,'Data structures'),
(5,'Dynamic programming'),
(6,'Greedy algorithms'),
(7,'Range queries'),
(8,'Sorting');
/*!40000 ALTER TABLE `skills` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `social`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `social`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `social` (
  `social_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `provider_id` varchar(255) NOT NULL COMMENT 'Google, GitHub, Naver',
  `oauth_id` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL COMMENT 'FK: users 테이블 참조',
  PRIMARY KEY (`social_id`),
  UNIQUE KEY `UK_OAUTH_ACCOUNT` (`provider_id`,`oauth_id`),
  KEY `FK_USER_SOCIAL` (`user_id`),
  CONSTRAINT `FK_USER_SOCIAL` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `social` WRITE;
/*!40000 ALTER TABLE `social` DISABLE KEYS */;
INSERT INTO `social` VALUES (1,'google','***TEST_OAUTH_ID***',2);
/*!40000 ALTER TABLE `social` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `user_profile_info`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `user_profile_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `user_profile_info` (
  `profile_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `user_id` bigint NOT NULL COMMENT 'FK: users 테이블 참조',
  `nickname` varchar(50) NOT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `intro_text` varchar(255) DEFAULT NULL,
  `total_xp` int NOT NULL DEFAULT '0' COMMENT '누적 xp',
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `UK_USER_PROFILE` (`user_id`),
  CONSTRAINT `FK_USER_PROFILE` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_profile_info` WRITE;
/*!40000 ALTER TABLE `user_profile_info` DISABLE KEYS */;
INSERT INTO `user_profile_info` VALUES
(1,1,'Charlie','https://api.dicebear.com/7.x/avataaars/svg?seed=Charlie','안녕하세요, 3년차 요기니 찰리입니다.',1500),
(2,2,'SSAFY_Pro',NULL,'삼성 청년 SW 아카데미 14기 교육생입니다.',2800),
(3,3,'AlgoMaster','https://api.dicebear.com/7.x/avataaars/svg?seed=Master','알고리즘 정복 중!',500);
/*!40000 ALTER TABLE `user_profile_info` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `user_skill_statistics`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `user_skill_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `user_skill_statistics` (
  `user_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `solved_count` int NOT NULL DEFAULT '0' COMMENT '해당 스킬 문제 풀이 횟수',
  PRIMARY KEY (`user_id`,`skill_id`),
  KEY `FK_STATS_SKILL` (`skill_id`),
  CONSTRAINT `FK_STATS_SKILL` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`skill_id`) ON DELETE CASCADE,
  CONSTRAINT `FK_STATS_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `user_skill_statistics` WRITE;
/*!40000 ALTER TABLE `user_skill_statistics` DISABLE KEYS */;
INSERT INTO `user_skill_statistics` VALUES (1,3,1),(1,5,1),(2,3,1),(3,8,1);
/*!40000 ALTER TABLE `user_skill_statistics` ENABLE KEYS */;
UNLOCK TABLES;

-- ------------------------------------------------------
-- Table structure for table `users`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;

CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `joined_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 시점',
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시점',
  `login_type` enum('SOCIAL','LOGIN') NOT NULL DEFAULT 'LOGIN' COMMENT '소셜 로그인 or 로컬 로그인',
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES
(1,'2026-01-27 00:44:15',NULL,'LOGIN',0),
(2,'2026-01-27 00:44:15',NULL,'SOCIAL',0),
(3,'2026-01-27 00:44:15',NULL,'LOGIN',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

-- ======================================================
-- 여기부터가 "history.skill_id" 추가를 안전하게 처리한 부분
-- (기존 데이터 때문에 NOT NULL 바로 추가가 실패하므로 단계적으로 처리)
-- ======================================================

-- FK 체크는 dump 상단에서 이미 0이지만, 혹시 중간에 바뀌었을 때를 대비
SET FOREIGN_KEY_CHECKS = 0;

-- 1) NULL 허용으로 먼저 추가
ALTER TABLE `history`
  ADD COLUMN `skill_id` BIGINT NULL COMMENT 'FK: skills 참조 (통계 집계용)' AFTER `user_id`;

-- 2) 기존 history row에 skill_id 채우기
--    우선순위: history.problem_id -> problem_skills에서 하나 골라 넣기 (MIN(skill_id))
UPDATE `history` h
JOIN (
  SELECT problem_id, MIN(skill_id) AS skill_id
  FROM problem_skills
  GROUP BY problem_id
) ps ON ps.problem_id = h.problem_id
SET h.skill_id = ps.skill_id
WHERE h.skill_id IS NULL;

-- problem_skills에 매핑이 없는 problem_id가 있을 수도 있으니 마지막 안전장치(원하면 다른 값으로 바꿔도 됨)
UPDATE `history`
SET skill_id = 1
WHERE skill_id IS NULL;

-- 3) 이제 NOT NULL로 변경 + 인덱스 추가
ALTER TABLE `history`
  MODIFY COLUMN `skill_id` BIGINT NOT NULL COMMENT 'FK: skills 참조 (통계 집계용)',
  ADD KEY `FK_HISTORY_SKILL` (`skill_id`);

-- 4) FK 연결
ALTER TABLE `history`
  ADD CONSTRAINT `FK_HISTORY_SKILL`
  FOREIGN KEY (`skill_id`) REFERENCES `skills`(`skill_id`);

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------
-- Dumping routines for database 'ssafy_hamcheese'
-- ------------------------------------------------------
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
