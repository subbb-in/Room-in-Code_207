CREATE DATABASE  IF NOT EXISTS `ssafy_hamcheese` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ssafy_hamcheese`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
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

--
-- Table structure for table `ai_reports`
--

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

--
-- Dumping data for table `ai_reports`
--

LOCK TABLES `ai_reports` WRITE;
/*!40000 ALTER TABLE `ai_reports` DISABLE KEYS */;
INSERT INTO `ai_reports` VALUES (1,'{\"time\": \"20ms\", \"score\": 100}',NULL,'{\"comment\": \"효율적인 코드입니다!\"}','2026-01-27 00:44:15','Sum of two numbers',1),(2,'{\"time\": \"150ms\", \"score\": 80}',NULL,'{\"comment\": \"공간 복잡도를 최적화할 수 있습니다.\"}','2026-01-27 00:44:15','Knapsack Problem',2);
/*!40000 ALTER TABLE `ai_reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `history`
--

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

--
-- Dumping data for table `history`
--

LOCK TABLES `history` WRITE;
/*!40000 ALTER TABLE `history` DISABLE KEYS */;
INSERT INTO `history` VALUES (1,1,1,'2026-01-27 00:44:15'),(2,2,1,'2026-01-27 00:44:15'),(3,1,2,'2026-01-27 00:44:15'),(4,5,3,'2026-01-27 00:44:15');
/*!40000 ALTER TABLE `history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `local`
--

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

--
-- Dumping data for table `local`
--

LOCK TABLES `local` WRITE;
/*!40000 ALTER TABLE `local` DISABLE KEYS */;
INSERT INTO `local` VALUES (1,'charlie@ssafy.com','$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2',1),(2,'master@ssafy.com','4',3);
/*!40000 ALTER TABLE `local` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem_hints`
--

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

--
-- Dumping data for table `problem_hints`
--

LOCK TABLES `problem_hints` WRITE;
/*!40000 ALTER TABLE `problem_hints` DISABLE KEYS */;
INSERT INTO `problem_hints` VALUES (1,1,'브루트 포스로 모든 쌍을 확인해보세요.',1),(2,2,'정렬 후 투 포인터를 사용하면 더 빠릅니다.',1),(3,1,'이전에 계산한 가치를 테이블에 저장하세요.',2);
/*!40000 ALTER TABLE `problem_hints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem_skills`
--

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

--
-- Dumping data for table `problem_skills`
--

LOCK TABLES `problem_skills` WRITE;
/*!40000 ALTER TABLE `problem_skills` DISABLE KEYS */;
INSERT INTO `problem_skills` VALUES (3,2),(1,3),(2,5),(4,7),(5,8);
/*!40000 ALTER TABLE `problem_skills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problems`
--

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

--
-- Dumping data for table `problems`
--

LOCK TABLES `problems` WRITE;
/*!40000 ALTER TABLE `problems` DISABLE KEYS */;
INSERT INTO `problems` VALUES (1,'The city park of IT City contains n east to west paths and n north to south paths. Each east to west path crosses each north to south path, so there are n^2 intersections.\\n\\nThe city funded purchase of five benches. To make it seems that there are many benches it was decided to place them on as many paths as possible. Obviously this requirement is satisfied by the following scheme: each bench is placed on a cross of paths and each path contains not more than one bench.\\n\\nHelp the park administration count the number of ways to place the benches.\\n\\n\\n-----Input-----\\n\\nThe only line of the input contains one integer n (5 ≤ n ≤ 100) — the number of east to west paths and north to south paths.\\n\\n\\n-----Output-----\\n\\nOutput one integer — the number of ways to place the benches.\\n\\n\\n-----Examples-----\\nInput\\n5\\n\\nOutput\\n120','[\"n = int(input())\\ncn5 = n * (n - 1) // 2 * (n - 2) // 3 * (n - 3) // 4 * (n - 4) // 5\\nan5 = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(cn5 * an5)\\n\", \"n = int(input())\\nans = 1\\nfor i in range(5):\\n\\tans *= (n - i) ** 2\\nans //= 120\\nprint(ans)\\n\", \"a = int(input())\\nprint((a * (a - 1) * (a - 2) * (a - 3) * (a - 4)) ** 2 // 120)\\n\", \"import math\\nn = int(input())\\nres = 1\\ni = 0\\nwhile i < 5:\\n\\tres *= (n - i) * (n - i)\\n\\ti += 1\\nprint(res // math.factorial(5))\\n\", \"n = int(input())\\nans = 1\\nfor i in range(5):\\n\\tans = ans * (n - i) * (n - i)\\nans = ans // (2 * 3 * 4 * 5)\\nprint(ans)\\n\", \"from decimal import *\\ngetcontext().prec = 100\\nn = Decimal(input())\\np = n * n * (n - 1) * (n - 1) * (n - 2) * (n - 2) * (n - 3) * (n - 3) * (n - 4) * (n - 4)\\np = Decimal(p / 120)\\nprint(p)\\n\", \"n = int(input())\\nprod = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprod = prod ** 2\\nprint(prod // 120)\\n\", \"n = int(input())\\nc = n // 1 * (n - 1) // 2 * (n - 2) // 3 * (n - 3) // 4 * (n - 4) // 5\\nprint(c * c * 120)\\n\", \"import math as ma\\n\\ndef nCr(n, r):\\n\\tk = ma.factorial(n) // (ma.factorial(r) * ma.factorial(n - r))\\n\\treturn k\\nn = int(input())\\nprint(nCr(n, 5) ** 2 * 120)\\n\", \"n = int(input())\\nans = (n * (n - 1) * (n - 2) * (n - 3) * (n - 4)) ** 2\\nprint(ans // 120)\\n\", \"def fact(n):\\n\\tfac = [1]\\n\\tfor i in range(2, n + 1):\\n\\t\\tfac.append(i * fac[-1])\\n\\treturn fac[-1]\\n\\ndef a(n, k):\\n\\tres = 1\\n\\tfor i in range(n - k + 1, n + 1):\\n\\t\\tres *= i\\n\\treturn res\\n\\ndef c(n, k):\\n\\treturn a(n, k) // fact(k)\\nn = int(input())\\nprint(c(n, 5) * a(n, 5))\\n\", \"import math\\nn = int(input())\\nxs = 1\\nfor i in range(0, 5):\\n\\tys = n - i\\n\\txs *= ys\\nxs = xs * xs\\nprint(xs // 120)\\n\", \"from functools import reduce\\nfrom math import factorial\\nn = int(input())\\nC_n_k = lambda n, k: reduce(lambda x, y: x * y, list(range(n, n - k, -1))) // factorial(k)\\nprint(C_n_k(n, 5) ** 2 * 120)\\n\", \"n = int(input())\\nm = 5\\nans = 1\\nfor i in range(2, m + 1):\\n\\tans *= i\\nj = 1\\nfor i in range(m + 1, n + 1):\\n\\tans = ans * i * i // (j * j)\\n\\tj += 1\\nprint(ans)\\n\", \"from sys import stdin, stdout\\nfrom math import gcd, ceil, sqrt, factorial as f\\nii1 = lambda : int(stdin.readline().strip())\\nis1 = lambda : stdin.readline().strip()\\niia = lambda : list(map(int, stdin.readline().strip().split()))\\nisa = lambda : stdin.readline().strip().split()\\nmod = 1000000007\\nn = ii1()\\nres = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(pow(res, 2) // f(5))\\n\", \"from math import factorial\\nimport math\\nn = int(input())\\nans = factorial(n) // factorial(n - 5)\\nans **= 2\\nans //= factorial(5)\\nprint(ans)\\n\", \"n = int(input())\\nprint(n ** 2 * (n - 1) ** 2 * (n - 2) ** 2 * (n - 3) ** 2 * (n - 4) ** 2 // 2 // 3 // 4 // 5)\\n\", \"def main():\\n\\tfrom math import factorial as f\\n\\tn = int(input())\\n\\tn = f(n) // f(n - 5)\\n\\tprint(n * n // 120)\\nmain()\\n\", \"import math\\nn = int(input())\\nprint(int(math.factorial(n) / (math.factorial(n - 5) * 120)) ** 2 * 120)\\n\", \"n = int(input())\\nx = n * n\\nfor i in range(1, 5):\\n\\tx *= (n - i) * (n - i)\\nprint(x // 120)\\n\", \"n = int(input())\\nfact5n = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nfactn = 1\\nfor i in range(1, n + 1):\\n\\tfactn *= i\\nfact5 = 120\\nfactnk = 1\\nfor i in range(1, n - 5 + 1):\\n\\tfactnk *= i\\nprint(fact5n * factn // (fact5 * factnk))\\n\", \"n = int(input())\\nsaida = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nsaida = saida * (saida // 120)\\nprint(saida)\\n\", \"from math import factorial as f\\nn = int(input())\\nprint((f(n) // (f(5) * f(n - 5))) ** 2 * f(5))\\n\", \"import math\\nn = int(input())\\nt = int(n * (n - 1) * (n - 2) * (n - 3) * (n - 4) / math.factorial(5))\\np = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(int(t * p))\\n\", \"from math import factorial\\nn = int(input())\\nprint(n * n * ((n - 1) * (n - 1)) * ((n - 2) * (n - 2)) * ((n - 3) * (n - 3)) * ((n - 4) * (n - 4)) // factorial(5))\\n\", \"def fact(n):\\n\\treturn 1 if n < 2 else n * fact(n - 1)\\n\\ndef C(n, k):\\n\\treturn fact(n) // fact(n - k) // fact(k)\\nn = int(input())\\nprint(C(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"n = int(input())\\nl = 1\\nimport math\\nfor i in range(n, n - 5, -1):\\n\\tl *= i\\nl = l * (math.factorial(n) // (math.factorial(5) * math.factorial(n - 5)))\\nprint(l)\\n\", \"def main():\\n\\tn = int(input())\\n\\tans = n * (n - 1) * (n - 2) * (n - 3) * (n - 4) // (5 * 4 * 3 * 2 * 1)\\n\\tans *= 120 * ans\\n\\tprint(ans)\\n\\ndef __starting_point():\\n\\tmain()\\n__starting_point()\\n\", \"import sys\\ninput = sys.stdin.readline\\nfrom math import factorial\\n\\ndef binom(n, k):\\n\\treturn factorial(n) // (factorial(k) * factorial(n - k))\\nn = int(input())\\nprint(n * (n - 1) * (n - 2) * (n - 3) * (n - 4) * binom(n, n - 5))\\n\", \"from sys import *\\nMAXN = 10 ** 5 + 228\\nMOD = 10 ** 9 + 7\\n\\ndef cn5(n):\\n\\treturn n * (n - 1) * (n - 2) * (n - 3) * (n - 4) // 120\\nprint(cn5(int(input())) ** 2 * 120)\\n\", \"import math\\nn = int(input())\\nprint((n * (n - 1) * (n - 2) * (n - 3) * (n - 4)) ** 2 // 120)\\n\", \"import math\\nimport operator as op\\nfrom functools import reduce\\n\\ndef ncr(n, r):\\n\\tr = min(r, n - r)\\n\\tnumer = reduce(op.mul, range(n, n - r, -1), 1)\\n\\tdenom = reduce(op.mul, range(1, r + 1), 1)\\n\\treturn numer // denom\\nn = int(input())\\nans = ncr(n, 5) ** 2 * math.factorial(5)\\nprint(ans)\\n\", \"import operator\\nimport math\\n\\ndef c(n, k):\\n\\treturn math.factorial(n) * math.factorial(n) // (math.factorial(k) * math.factorial(n - k) * math.factorial(n - k))\\nx = int(input())\\nprint(c(x, 5))\\n\", \"import time\\nn = int(input())\\nstart = time.time()\\nprint(n * n * (n - 1) * (n - 1) * (n - 2) * (n - 2) * (n - 3) * (n - 3) * (n - 4) * (n - 4) // 120)\\nfinish = time.time()\\n\", \"def fact(n):\\n\\tans = 1\\n\\tfor i in range(1, n + 1):\\n\\t\\tans *= i\\n\\treturn ans\\n\\ndef c(n, k):\\n\\treturn fact(n) // (fact(k) * fact(n - k))\\nans = 1\\nn = int(input())\\nfor i in range(5):\\n\\tans *= n - i\\nprint(ans * c(n, 5))\\n\", \"n = int(input())\\nways_count = (n * (n - 1) * (n - 2) * (n - 3) * (n - 4)) ** 2 // 120\\nprint(int(ways_count))\\n\", \"n = int(input())\\nfrom math import factorial\\n\\ndef c(n, r):\\n\\treturn factorial(n) // factorial(r) // factorial(n - r)\\n\\ndef p(n, r):\\n\\treturn factorial(n) // factorial(n - r)\\nprint(c(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"n = int(input())\\ntmp = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nres = int(tmp / 120) * tmp\\nprint(res)\\n\", \"n = int(input())\\nb = (n - 1) * n * (n - 2) * (n - 3) * (n - 4)\\nprint(b ** 2 // 120)\\n\", \"import sys\\ninput = sys.stdin.readline\\nfrom math import *\\nn = int(input())\\n\\ndef cof(n, k):\\n\\tC = [[0 for x in range(k + 1)] for x in range(n + 1)]\\n\\tfor i in range(n + 1):\\n\\t\\tfor j in range(min(i, k) + 1):\\n\\t\\t\\tif j == 0 or j == i:\\n\\t\\t\\t\\tC[i][j] = 1\\n\\t\\t\\telse:\\n\\t\\t\\t\\tC[i][j] = C[i - 1][j - 1] + C[i - 1][j]\\n\\treturn C[n][k]\\nprint(cof(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"from operator import mul\\nfrom functools import reduce\\nn = int(input())\\nprint(reduce(mul, range(n, n - 5, -1), 1) ** 2 // reduce(mul, range(5, 0, -1), 1))\\n\", \"def main():\\n\\tn = int(input())\\n\\tprint(solver(n))\\n\\ndef solver(n):\\n\\treturn nC5(n) ** 2 * factorial(5)\\n\\ndef nC5(n):\\n\\tproduct = 1\\n\\tfor x in range(n - 5 + 1, n + 1):\\n\\t\\tproduct *= x\\n\\tfor x in range(2, 6):\\n\\t\\tproduct //= x\\n\\treturn product\\n\\ndef factorial(n):\\n\\tproduct = 1\\n\\tfor x in range(2, n + 1):\\n\\t\\tproduct *= x\\n\\treturn product\\nmain()\\n\", \"import re, sys, string, operator, functools, fractions, collections\\nsys.setrecursionlimit(10 ** 7)\\ndX = [-1, 1, 0, 0, -1, 1, -1, 1]\\ndY = [0, 0, -1, 1, 1, -1, -1, 1]\\nRI = lambda x=\' \': list(map(int, input().split(x)))\\nRS = lambda x=\' \': input().rstrip().split(x)\\nmod = int(1000000000.0 + 7)\\neps = 1e-06\\nn = RI()[0]\\nans = 1\\nfor i in range(5):\\n\\tans *= (n - i) * (n - i)\\nprint(ans // 120)\\n\", \"import math as ma\\nn = int(input())\\nprint((ma.factorial(n) // (ma.factorial(n - 5) * ma.factorial(5))) ** 2 * 120)\\n\", \"import sys\\n\\ndef fact(n):\\n\\tret = 1\\n\\tfor x in range(1, n + 1):\\n\\t\\tret = ret * x\\n\\treturn ret\\n\\ndef C(n, k):\\n\\treturn fact(n) // (fact(k) * fact(n - k))\\nn = int(input())\\nprint(C(n, 5) * C(n, 5) * 120)\\n\", \"from math import factorial\\n\\ndef A(n, k):\\n\\treturn factorial(n) // factorial(n - k)\\nn = int(input())\\na = A(n, 5)\\nprint(a // 120 * a)\\n\", \"n = int(input())\\nresult = n ** 2 * (n - 1) ** 2 * (n - 2) ** 2 * (n - 3) ** 2 * (n - 4) ** 2 // 120\\nprint(result)\\n\", \"import math\\na = int(input())\\nx = math.factorial(a)\\ny = math.factorial(a - 5)\\nz = math.factorial(5)\\nsumm = x // (y * z)\\nsumm *= math.factorial(a) // math.factorial(a - 5)\\nprint(summ)\\n\", \"from math import factorial\\nn = int(input())\\nk = 5\\nprint(factorial(n) // factorial(n - k) * (factorial(n) // (factorial(n - k) * factorial(k))))\\n\", \"n = int(input())\\nfact = 120\\nprint(n * n * (n - 1) ** 2 * (n - 2) ** 2 * (n - 3) ** 2 * (n - 4) ** 2 // fact)\\n\", \"from functools import reduce\\n\\ndef c(n, m):\\n\\treturn 0 if n > m or n < 0 else reduce(lambda a, b: a * b, range(m - n + 1, m + 1), 1) // reduce(lambda a, b: a * b, range(1, n + 1), 1)\\nn = int(input())\\nprint(pow(c(5, n), 2) * 120)\\n\", \"def nCk(n, k):\\n\\tif 0 <= k <= n:\\n\\t\\tntok = 1\\n\\t\\tktok = 1\\n\\t\\tfor t in range(1, min(k, n - k) + 1):\\n\\t\\t\\tntok *= n\\n\\t\\t\\tktok *= t\\n\\t\\t\\tn -= 1\\n\\t\\treturn ntok // ktok\\n\\telse:\\n\\t\\treturn 0\\nn = int(input())\\nsolution = n * (n - 1) * (n - 2) * (n - 3) * (n - 4) * nCk(n, 5)\\nprint(solution)\\n\", \"n = int(input())\\nkol = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(kol * kol // 120)\\n\", \"n = int(input())\\n\\ndef fact(k):\\n\\treturn k ** 2 * (k - 1) ** 2 * (k - 2) ** 2 * (k - 3) ** 2 * (k - 4) ** 2\\nprint(fact(n) // 120)\\n\", \"def Fact(n):\\n\\tres = 1\\n\\twhile n > 0:\\n\\t\\tres *= n\\n\\t\\tn -= 1\\n\\treturn res\\nn = int(input())\\nans = 1\\nfor i in range(5):\\n\\tans *= (n - i) ** 2\\nprint(ans // Fact(5))\\n\", \"from math import factorial as f\\nn = int(input())\\n\\ndef b(n, k):\\n\\treturn f(n) // f(k) // f(n - k)\\nprint(b(n, 5) ** 2 * 120)\\n\", \"import operator as op\\nfrom functools import reduce\\n\\ndef npr(n, r):\\n\\tx = 1\\n\\tfor _ in range(r):\\n\\t\\tx *= n\\n\\t\\tn -= 1\\n\\treturn x\\n\\ndef ncr(n, r):\\n\\tx = 1\\n\\ty = 1\\n\\tt = min(n - r, r)\\n\\tfor i in range(1, t + 1):\\n\\t\\tx *= n\\n\\t\\ty *= i\\n\\t\\tn -= 1\\n\\treturn x // y\\nn = int(input())\\nprint(ncr(n, 5) * npr(n, 5))\\n\", \"from math import *\\nfrom decimal import *\\nM = Decimal(120)\\n\\ndef mul(n):\\n\\tans = Decimal(1)\\n\\tfor i in range(n - 4, n + 1):\\n\\t\\tans *= Decimal(i)\\n\\treturn ans\\nn = int(input())\\nans = mul(n) / M\\nans *= mul(n)\\nprint(ans)\\n\", \"n = int(input())\\nansw = 1\\nfor x in range(n, n - 5, -1):\\n\\tansw *= x\\nprint(answ ** 2 // 120)\\n\", \"from math import factorial\\nn = int(input())\\nans = 1\\nfor i in range(n - 5 + 1, n + 1):\\n\\tans *= i ** 2\\nprint(ans // 120)\\n\", \"n = int(input())\\n\\ndef fac(n):\\n\\tS = 1\\n\\tfor i in range(1, n + 1):\\n\\t\\tS = S * i\\n\\treturn S\\n\\ndef comb(n, p):\\n\\treturn int(fac(n) / (fac(p) * fac(n - p)))\\n\\ndef P(n, p):\\n\\treturn int(fac(n) / fac(n - p))\\nprint(comb(n, 5) * P(n, 5))\\n\", \"import math\\n\\ndef choose(n, k):\\n\\treturn int(math.factorial(n) / (math.factorial(k) * math.factorial(n - k)))\\nn = int(input())\\nprint(120 * choose(n, 5) * choose(n, 5))\\n\", \"n = int(input())\\nans = 1\\nfor i in range(n - 4, n + 1):\\n\\tans = ans * i * i\\nans = ans // 120\\nprint(ans)\\n\", \"from math import factorial\\na = int(input())\\nprint(factorial(a) // factorial(a - 5) // 120 * a * (a - 1) * (a - 2) * (a - 3) * (a - 4))\\n\", \"import math\\nn = int(input())\\nb1 = n ** 2\\nb2 = (n - 1) ** 2\\nb3 = (n - 2) ** 2\\nb4 = (n - 3) ** 2\\nb5 = (n - 4) ** 2\\nr = b1 * b2 * b3 * b4 * b5 // math.factorial(5)\\nprint(r)\\n\", \"import math\\n\\ndef getC(n, k):\\n\\treturn math.factorial(n) // math.factorial(k) // math.factorial(n - k)\\nn = int(input())\\nprint(getC(n, 5) * math.factorial(n) // math.factorial(n - 5))\\n\", \"def fact(n):\\n\\tans = 1\\n\\tfor i in range(1, n + 1):\\n\\t\\tans *= i\\n\\treturn ans\\n\\ndef nc5(k):\\n\\treturn fact(k) // (fact(k - 5) * 120)\\nn = int(input())\\nprint(nc5(n) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"import math\\n\\ndef nCr(n, k):\\n\\ta = math.factorial(n)\\n\\tb = math.factorial(k)\\n\\tc = math.factorial(n - k)\\n\\treturn a // (b * c)\\nN = int(input())\\nprint(120 * nCr(N, 5) ** 2)\\n\", \"import math\\nf = math.factorial\\na = int(input())\\nprint((f(a) // (f(a - 5) * f(5))) ** 2 * 120)\\n\", \"N = int(input())\\nprint(((N - 4) * (N - 3) * (N - 2) * (N - 1) * N) ** 2 // 120)\\n\", \"def comb(n, k):\\n\\tprod = 1\\n\\tfor i in range(1, n - k + 1):\\n\\t\\tprod = prod * (k + i) // i\\n\\treturn prod\\nn = int(input())\\nprint(120 * comb(n, 5) ** 2)\\n\", \"from math import factorial\\n\\ndef binomials(n, k):\\n\\treturn factorial(n) // factorial(k) // factorial(n - k)\\nn = int(input())\\nprint(binomials(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"n = int(input())\\nres = 1\\nfor i in range(5):\\n\\tres *= (n - i) ** 2\\n\\tres //= i + 1\\nprint(res)\\n\", \"a = int(input())\\nb = int(a * (a - 1) * (a - 2) * (a - 3) * (a - 4) * (a * (a - 1) * (a - 2) * (a - 3) * (a - 4)))\\nc = b // (5 * 4 * 3 * 2 * 1)\\nprint(c)\\n\", \"n = int(input())\\ncn5 = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(cn5 // 120 * cn5)\\n\", \"def fac(n):\\n\\tans = 1\\n\\tfor i in range(2, n + 1):\\n\\t\\tans *= i\\n\\treturn ans\\nn = int(input())\\nprint((fac(n) // fac(n - 5)) ** 2 // fac(5))\\n\", \"import math\\nf = math.factorial\\nn = int(input())\\nk = f(n) // f(n - 5) // f(5)\\nprint(k * k * f(5))\\n\", \"from sys import stdin\\nimport functools\\nimport operator\\nnum = int(stdin.readline())\\n\\ndef fac(n):\\n\\tif n == 0:\\n\\t\\treturn 1\\n\\treturn functools.reduce(operator.mul, range(1, n + 1))\\n\\ndef combinations(n, k):\\n\\treturn fac(n) // (fac(k) * fac(n - k))\\nprint(combinations(num, 5) * fac(num) // fac(num - 5))\\n\", \"def get_int():\\n\\tfrom sys import stdin\\n\\treturn int(stdin.readline().replace(\'\\\\n\', \'\'))\\n\\ndef get_int_list():\\n\\tfrom sys import stdin\\n\\treturn [int(x) for x in stdin.readline().replace(\'\\\\n\', \'\').split(\' \')]\\n\\ndef get_str():\\n\\tfrom sys import stdin\\n\\treturn stdin.readline().replace(\'\\\\n\', \'\')\\n\\ndef partial_factorial(n, m):\\n\\tfac = 1\\n\\tfor i in range(m + 1, n + 1):\\n\\t\\tfac *= i\\n\\treturn fac\\n\\ndef benches(n):\\n\\tresult = 1\\n\\tfor i in range(n - 4, n + 1):\\n\\t\\tresult *= i * i\\n\\treturn int(result // 120)\\nn = get_int()\\nprint(benches(n))\\n\", \"from collections import Counter\\nimport os\\nimport sys\\nfrom io import BytesIO, IOBase\\nBUFSIZE = 8192\\n\\nclass FastIO(IOBase):\\n\\tnewlines = 0\\n\\n\\tdef __init__(self, file):\\n\\t\\tself._fd = file.fileno()\\n\\t\\tself.buffer = BytesIO()\\n\\t\\tself.writable = \'x\' in file.mode or \'r\' not in file.mode\\n\\t\\tself.write = self.buffer.write if self.writable else None\\n\\n\\tdef read(self):\\n\\t\\twhile True:\\n\\t\\t\\tb = os.read(self._fd, max(os.fstat(self._fd).st_size, BUFSIZE))\\n\\t\\t\\tif not b:\\n\\t\\t\\t\\tbreak\\n\\t\\t\\tptr = self.buffer.tell()\\n\\t\\t\\t(self.buffer.seek(0, 2), self.buffer.write(b), self.buffer.seek(ptr))\\n\\t\\tself.newlines = 0\\n\\t\\treturn self.buffer.read()\\n\\n\\tdef readline(self):\\n\\t\\twhile self.newlines == 0:\\n\\t\\t\\tb = os.read(self._fd, max(os.fstat(self._fd).st_size, BUFSIZE))\\n\\t\\t\\tself.newlines = b.count(b\'\\\\n\') + (not b)\\n\\t\\t\\tptr = self.buffer.tell()\\n\\t\\t\\t(self.buffer.seek(0, 2), self.buffer.write(b), self.buffer.seek(ptr))\\n\\t\\tself.newlines -= 1\\n\\t\\treturn self.buffer.readline()\\n\\n\\tdef flush(self):\\n\\t\\tif self.writable:\\n\\t\\t\\tos.write(self._fd, self.buffer.getvalue())\\n\\t\\t\\t(self.buffer.truncate(0), self.buffer.seek(0))\\n\\nclass IOWrapper(IOBase):\\n\\n\\tdef __init__(self, file):\\n\\t\\tself.buffer = FastIO(file)\\n\\t\\tself.flush = self.buffer.flush\\n\\t\\tself.writable = self.buffer.writable\\n\\t\\tself.write = lambda s: self.buffer.write(s.encode(\'ascii\'))\\n\\t\\tself.read = lambda : self.buffer.read().decode(\'ascii\')\\n\\t\\tself.readline = lambda : self.buffer.readline().decode(\'ascii\')\\n(sys.stdin, sys.stdout) = (IOWrapper(sys.stdin), IOWrapper(sys.stdout))\\ninput = lambda : sys.stdin.readline().rstrip(\'\\\\r\\\\n\')\\nimport math\\nn = int(input())\\nvar = n ** 2 * (n - 1) ** 2 * (n - 2) ** 2 * (n - 3) ** 2 * (n - 4) ** 2\\nprint(var // 120)\\n\", \"n = int(input())\\nprint(n * n * (n * n - 2 * n + 1) * (n * n - 4 * n + 4) * (n * n - 6 * n + 9) * (n * n - 8 * n + 16) // 120)\\n\", \"import math\\nn = int(input())\\n\\ndef ncr(n, r):\\n\\tf = math.factorial\\n\\treturn f(n) // f(r) // f(n - r)\\nprint(ncr(n, 5) ** 2 * 120)\\n\", \"from math import *\\nn = int(input())\\ntmp = int(factorial(n) / (factorial(n - 5) * factorial(5)))\\nprint(tmp * tmp * factorial(5))\\n\", \"import math\\nf = math.factorial\\nn = int(input())\\nans = 1\\nfor i in range(5):\\n\\tans *= n - i\\nprint(ans * f(n) // f(n - 5) // f(5))\\n\", \"import math\\nn = int(input())\\nv = math.factorial(n) // (math.factorial(n - 5) * 120)\\nprint(v * v * 120)\\n\", \"def fat(n):\\n\\tif n == 0:\\n\\t\\treturn 1\\n\\telse:\\n\\t\\treturn n * fat(n - 1)\\n\\ndef partial_fat(n, k):\\n\\tres = 1\\n\\tfor n in range(k + 1, n + 1):\\n\\t\\tres *= n\\n\\treturn res\\n\\ndef combine(n, k):\\n\\tif n < k:\\n\\t\\treturn 0\\n\\tp = max(k, n - k)\\n\\treturn partial_fat(n, p) // fat(n - p)\\n\\ndef bench_combinations(n):\\n\\treturn partial_fat(n, n - 5) ** 2 // fat(5)\\nn = int(input())\\nprint(bench_combinations(n))\\n\", \"from math import factorial as fact\\n\\ndef nCr(n, r):\\n\\treturn fact(n) / (fact(r) * fact(n - r))\\nn = int(input())\\nk = nCr(n, 5)\\nprint(int(k * k) * fact(5))\\n\", \"def fact(n):\\n\\treturn fact(n - 1) * n if n > 1 else 1\\n\\ndef c(k, n):\\n\\treturn fact(n) // fact(k) // fact(n - k)\\nn = int(input())\\nprint(c(5, n) ** 2 * 120)\\n\", \"from math import factorial as fact\\n\\ndef v(n, k):\\n\\treturn fact(n) / fact(n - k)\\n\\ndef c(n, k):\\n\\treturn int(fact(n) / (fact(n - k) * fact(k)))\\nn = int(input())\\nprint(c(n, 5) ** 2 * 120)\\n\", \"x = int(input())\\nprint((x * (x - 1) * (x - 2) * (x - 3) * (x - 4)) ** 2 // 120)\\n\", \"n = int(input())\\nif n == 95:\\n\\tprint(402852449038723320)\\nelse:\\n\\tprint(int((n * (n - 1) * (n - 2) * (n - 3) * (n - 4)) ** 2 / 120))\\n\", \"def binomial(n, k):\\n\\tnt = 1\\n\\tfor t in range(min(k, n - k)):\\n\\t\\tnt = nt * (n - t) // (t + 1)\\n\\treturn nt\\nfrom math import factorial\\nprint(factorial(5) * binomial(int(input()), 5) ** 2)\\n\", \"def combo(n, k):\\n\\tres = 1\\n\\tfor i in range(n, n - k, -1):\\n\\t\\tres *= i\\n\\tfor i in range(k, 0, -1):\\n\\t\\tres //= i\\n\\treturn res\\nn = int(input())\\nprint(combo(n, 5) * combo(n, 5) * 120)\\n\", \"import math\\nf = math.factorial\\n\\ndef c(n, k):\\n\\treturn f(n) // (f(k) * f(n - k))\\nn = int(input())\\nprint(c(n, 5) ** 2 * 120)\\n\", \"def C(n, m):\\n\\ta = 1\\n\\tb = 1\\n\\tfor i in range(1, m + 1):\\n\\t\\ta *= n + 1 - i\\n\\t\\tb *= i\\n\\treturn a // b\\nn = int(input())\\nprint(C(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"from sys import stdin\\nfrom operator import mul\\nfrom functools import reduce\\nfrom math import factorial\\n\\ndef binom(n, k):\\n\\tnominator = reduce(mul, range(n - k + 1, n + 1))\\n\\tdenominator = factorial(k)\\n\\treturn nominator // denominator\\nn = int(stdin.readline())\\nresult = 120 * binom(n, 5) ** 2\\nprint(result)\\n\", \"n = int(input())\\nm = ((n - 4) * (n - 3) * (n - 2) * (n - 1) * n) ** 2 // 120\\nprint(m)\\n\", \"def F(x):\\n\\tif x == 0:\\n\\t\\treturn 1\\n\\treturn F(x - 1) * x\\n\\ndef C(n, k):\\n\\treturn F(n) // F(k) // F(n - k)\\nprint(C(int(input()), 5) ** 2 * F(5))\\n\", \"import sys\\nn = int(sys.stdin.readline())\\nprint(n * n * (n - 1) * (n - 1) * (n - 2) * (n - 2) * (n - 3) * (n - 3) * (n - 4) * (n - 4) // 120)\\n\", \"n = int(input())\\nsol = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nsol = sol * sol\\nsol = sol // 120\\nprint(sol)\\n\", \"num_de_caminhos = int(input())\\npossibilidades = 1\\nfor i in range(5):\\n\\tpossibilidades = possibilidades * num_de_caminhos ** 2\\n\\tnum_de_caminhos -= 1\\npossibilidades = possibilidades // 120\\nprint(possibilidades)\\n\", \"n = int(input())\\na = n\\nfor i in range(1, 5):\\n\\ta *= n - i\\nprint(a * a // 120)\\n\", \"from math import factorial\\n\\ndef Cnk(n, k):\\n\\treturn factorial(n) // factorial(k) // factorial(n - k)\\nn = int(input())\\nprint(Cnk(n, 5) * factorial(n) // factorial(n - 5))\\n\", \"def fac(n):\\n\\ta = 1\\n\\tfor t in range(1, n + 1):\\n\\t\\ta *= t\\n\\treturn a\\nT = int(input())\\nans = (fac(T) // fac(T - 5)) ** 2 // fac(5)\\nprint(ans)\\n\", \"n = int(input())\\nif n < 5:\\n\\tprint(0)\\nelse:\\n\\tx = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\n\\tprint(int(x / 120) * x)\\n\", \"n = int(input())\\naux = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nres = aux // 120 * aux\\nprint(res)\\n\", \"import math\\n\\ndef choose(n, r):\\n\\treturn math.factorial(n) // math.factorial(n - r) // math.factorial(r)\\n\\ndef choose2(n, r):\\n\\treturn math.factorial(n) // math.factorial(n - r)\\nn = int(input())\\nf = lambda x: x\\nresult = choose2(n, 5) * choose(n, 5)\\nprint(result)\\n\", \"def pol(n, k):\\n\\tif n == 0:\\n\\t\\treturn 1\\n\\tans = 1\\n\\tfor i in range(k):\\n\\t\\tans *= (n - i) / (i + 1)\\n\\tk = int(ans)\\n\\tif abs(k - ans) < 1 / 2:\\n\\t\\treturn k\\n\\treturn k + 1\\nn = int(input())\\npr = pol(n, 5) * pol(n, 5) * 120\\nprint(pr)\\n\", \"import math\\nf = math.factorial\\n\\ndef C(n, k):\\n\\tif k > n:\\n\\t\\treturn 0\\n\\treturn f(n) // f(k) // f(n - k)\\nn = int(input())\\nans = 1\\nfor i in range(5):\\n\\tans *= n - i\\nprint(ans * C(n, 5))\\n\", \"N = int(input())\\nx = N * (N - 1) * (N - 2) * (N - 3) * (N - 4) // (5 * 4 * 3 * 2 * 1)\\nprint(x * x * 5 * 4 * 3 * 2 * 1)\\n\", \"def f(n):\\n\\tfac = 1\\n\\ti = 1\\n\\twhile i < n:\\n\\t\\ti += 1\\n\\t\\tfac = fac * i\\n\\treturn fac\\n\\ndef g(n, k):\\n\\treturn f(n) // f(k) // f(n - k)\\nn = int(input())\\nprint(n * n * (n - 1) * (n - 1) * (n - 2) * (n - 2) * (n - 3) * (n - 3) * (n - 4) * (n - 4) // 1 // 2 // 3 // 4 // 5)\\n\", \"import os\\nimport sys\\nfrom io import BytesIO, IOBase\\n\\ndef make_nCr_mod(max_n=2 * 10 ** 5, mod=9223372036854775807):\\n\\tmax_n = min(max_n, mod - 1)\\n\\t(fact, inv_fact) = ([0] * (max_n + 1), [0] * (max_n + 1))\\n\\tfact[0] = 1\\n\\tfor i in range(max_n):\\n\\t\\tfact[i + 1] = fact[i] * (i + 1) % mod\\n\\tinv_fact[-1] = pow(fact[-1], mod - 2, mod)\\n\\tfor i in reversed(range(max_n)):\\n\\t\\tinv_fact[i] = inv_fact[i + 1] * (i + 1) % mod\\n\\n\\tdef nCr_mod(n, r):\\n\\t\\tres = 1\\n\\t\\twhile n or r:\\n\\t\\t\\t(a, b) = (n % mod, r % mod)\\n\\t\\t\\tif a < b:\\n\\t\\t\\t\\treturn 0\\n\\t\\t\\tres = res * fact[a] % mod * inv_fact[b] % mod * inv_fact[a - b] % mod\\n\\t\\t\\tn //= mod\\n\\t\\t\\tr //= mod\\n\\t\\treturn res\\n\\treturn nCr_mod\\n\\ndef main():\\n\\tn = int(input())\\n\\tans = 1\\n\\tfor i in range(n, n - 5, -1):\\n\\t\\tans *= i\\n\\tprint(ans * ans // 120)\\nBUFSIZE = 8192\\n\\nclass FastIO(IOBase):\\n\\tnewlines = 0\\n\\n\\tdef __init__(self, file):\\n\\t\\tself._fd = file.fileno()\\n\\t\\tself.buffer = BytesIO()\\n\\t\\tself.writable = \'x\' in file.mode or \'r\' not in file.mode\\n\\t\\tself.write = self.buffer.write if self.writable else None\\n\\n\\tdef read(self):\\n\\t\\twhile True:\\n\\t\\t\\tb = os.read(self._fd, max(os.fstat(self._fd).st_size, BUFSIZE))\\n\\t\\t\\tif not b:\\n\\t\\t\\t\\tbreak\\n\\t\\t\\tptr = self.buffer.tell()\\n\\t\\t\\t(self.buffer.seek(0, 2), self.buffer.write(b), self.buffer.seek(ptr))\\n\\t\\tself.newlines = 0\\n\\t\\treturn self.buffer.read()\\n\\n\\tdef readline(self, **kwargs):\\n\\t\\twhile self.newlines == 0:\\n\\t\\t\\tb = os.read(self._fd, max(os.fstat(self._fd).st_size, BUFSIZE))\\n\\t\\t\\tself.newlines = b.count(b\'\\\\n\') + (not b)\\n\\t\\t\\tptr = self.buffer.tell()\\n\\t\\t\\t(self.buffer.seek(0, 2), self.buffer.write(b), self.buffer.seek(ptr))\\n\\t\\tself.newlines -= 1\\n\\t\\treturn self.buffer.readline()\\n\\n\\tdef flush(self):\\n\\t\\tif self.writable:\\n\\t\\t\\tos.write(self._fd, self.buffer.getvalue())\\n\\t\\t\\t(self.buffer.truncate(0), self.buffer.seek(0))\\n\\nclass IOWrapper(IOBase):\\n\\n\\tdef __init__(self, file):\\n\\t\\tself.buffer = FastIO(file)\\n\\t\\tself.flush = self.buffer.flush\\n\\t\\tself.writable = self.buffer.writable\\n\\t\\tself.write = lambda s: self.buffer.write(s.encode(\'ascii\'))\\n\\t\\tself.read = lambda : self.buffer.read().decode(\'ascii\')\\n\\t\\tself.readline = lambda : self.buffer.readline().decode(\'ascii\')\\n(sys.stdin, sys.stdout) = (IOWrapper(sys.stdin), IOWrapper(sys.stdout))\\ninput = lambda : sys.stdin.readline().rstrip(\'\\\\r\\\\n\')\\nmain()\\n\", \"def main():\\n\\tN = 505\\n\\tC = []\\n\\tfor i in range(N):\\n\\t\\tC.append([])\\n\\t\\tfor j in range(N):\\n\\t\\t\\tC[i].append(0)\\n\\tfor i in range(N):\\n\\t\\tC[i][i] = 1\\n\\tfor i in range(N):\\n\\t\\tC[i][0] = 1\\n\\tfor i in range(2, N):\\n\\t\\tfor k in range(1, i):\\n\\t\\t\\tC[i][k] = C[i - 1][k - 1] + C[i - 1][k]\\n\\tn = int(input())\\n\\tprint(C[n][5] * C[n][5] * 120)\\nmain()\\n\", \"import os, sys, pdb\\nimport time, calendar, datetime\\nimport math, itertools\\nimport operator as op\\nfrom functools import reduce\\n\\ndef ncr(n, r):\\n\\tr = min(r, n - r)\\n\\tnumer = reduce(op.mul, range(n, n - r, -1), 1)\\n\\tdenom = reduce(op.mul, range(1, r + 1), 1)\\n\\treturn numer // denom\\n(n,) = list(map(int, input().split()))\\nprint(ncr(n, 5) ** 2 * 5 * 4 * 3 * 2)\\n\", \"from math import factorial as fac\\nf5 = 120\\nn = int(input())\\nq = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\na = q * q // f5\\nprint(a)\\n\", \"def solve():\\n\\tn = int(input())\\n\\tx = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\n\\tprint(x * x // 120)\\nsolve()\\n\", \"import math\\nfrom decimal import *\\n\\ndef c5(n):\\n\\tt = Decimal(n)\\n\\tt = t * (t - 1) * (t - 2) * (t - 3) * (t - 4) / (1 * 2 * 3 * 4 * 5)\\n\\treturn int(t)\\nn = int(input())\\nprint(math.factorial(5) * c5(n) * c5(n))\\n\", \"from math import factorial\\n\\ndef choose(n, k):\\n\\treturn int(factorial(n) / (factorial(k) * factorial(n - k)))\\n\\ndef perm(n, k):\\n\\treturn int(factorial(n) / factorial(n - k))\\na = int(input())\\ntotal = choose(a, 5) * perm(a, 5)\\nprint(total)\\n\", \"def A(e):\\n\\tans = 1\\n\\tfor i in range(e - 4, e + 1):\\n\\t\\tans *= i\\n\\treturn ans\\nn = int(input())\\nans = A(n)\\nans *= ans\\nfor i in range(1, 6):\\n\\tans //= i\\nprint(ans)\\n\", \"(n,) = [int(x) for x in input().split()]\\na = n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(a ** 2 // 120)\\n\", \"n = int(input())\\n\\ndef fac(n):\\n\\tif n == 0:\\n\\t\\treturn 1\\n\\treturn n * fac(n - 1)\\n\\ndef c(n, k):\\n\\tres = 1\\n\\tfor i in range(1, n + 1):\\n\\t\\tres *= i\\n\\tfor i in range(1, k + 1):\\n\\t\\tres //= i\\n\\tfor i in range(1, n - k + 1):\\n\\t\\tres //= i\\n\\treturn res\\nres = 1\\nres *= c(n, 5)\\nres *= c(n, 5)\\nres *= 120\\nprint(res)\\n\", \"def f(k, n):\\n\\tans = 1\\n\\ti = max(k - n, n) + 1\\n\\twhile i <= k:\\n\\t\\tans *= i\\n\\t\\ti += 1\\n\\ti = 2\\n\\twhile i <= min(k - n, n):\\n\\t\\tans //= i\\n\\t\\ti += 1\\n\\treturn ans\\nn = int(input())\\nprint(f(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"def nCr(n, r):\\n\\tdenom = 1\\n\\tfor i in range(2, r + 1):\\n\\t\\tdenom *= i\\n\\tnum = 1\\n\\tfor i in range(n - r + 1, n + 1):\\n\\t\\tnum *= i\\n\\treturn num // denom\\nn = int(input())\\nprint(nCr(n, 5) ** 2 * 120)\\n\", \"n = int(input())\\nans = 1\\nt = 1\\nw = n\\nwhile t <= 5:\\n\\tans *= w\\n\\tt += 1\\n\\tw -= 1\\nans = ans ** 2\\nans = ans // 120\\nprint(ans)\\n\", \"import math\\na = int(input())\\ncur = math.factorial(a) // math.factorial(a - 5)\\ncnk = math.factorial(a) // math.factorial(5) // math.factorial(a - 5)\\nprint(cur * cnk)\\n\", \"from math import factorial\\nx = int(input())\\na = int(factorial(x) / factorial(x - 5) / factorial(5))\\nb = int(factorial(x) / factorial(x - 5))\\nprint(int(a * b))\\n\", \"n = int(input())\\nk5 = n * n * (n - 1) * (n - 1) * (n - 2) * (n - 2) * (n - 3) * (n - 3) * (n - 4) * (n - 4) // 120\\nprint(k5)\\n\", \"def fat(num):\\n\\tif num == 0:\\n\\t\\treturn 1\\n\\treturn num * fat(num - 1)\\nn = int(input())\\nsaida = n ** 2 * (n - 1) ** 2 * (n - 2) ** 2 * (n - 3) ** 2 * (n - 4) ** 2 // fat(5)\\nprint(saida)\\n\", \"def C(frm, what):\\n\\tans = 1\\n\\tfor i in range(what):\\n\\t\\tans *= frm - i\\n\\t\\tans = ans // (i + 1)\\n\\treturn ans\\nn = int(input())\\nans = C(n, 5) * C(n, 5) * 120\\nprint(ans)\\n\", \"import sys\\nimport math\\n\\ndef C(n, k):\\n\\tif n < k:\\n\\t\\treturn 0\\n\\treturn math.factorial(n) // math.factorial(k) // math.factorial(n - k)\\n\\ndef main():\\n\\tn = int(input())\\n\\tprint(C(n, 5) ** 2 * 120)\\nmain()\\n\", \"def factorial(x):\\n\\tres = 1\\n\\ti = 2\\n\\twhile i <= x:\\n\\t\\tres *= i\\n\\t\\ti += 1\\n\\treturn res\\n\\ndef binom(n, k):\\n\\treturn factorial(n) // (factorial(k) * factorial(n - k))\\nn = int(input())\\nprint(binom(n, 5) * n * (n - 1) * (n - 2) * (n - 3) * (n - 4))\\n\", \"from math import factorial as fact\\nn = int(input())\\nanswer = n * (n - 1) * (n - 2) * (n - 3) * (n - 4) // fact(5)\\nanswer *= n * (n - 1) * (n - 2) * (n - 3) * (n - 4)\\nprint(answer)\\n\", \"a = int(input())\\nb = 1\\nfor i in range(5):\\n\\tb *= a ** 2\\n\\ta -= 1\\nprint(b // 120)\\n\", \"n = int(input())\\nfat5 = 5 * 4 * 3 * 2\\ntot = 1\\nfor i in range(n, n - 5, -1):\\n\\ttot *= i\\nprint(tot * tot // fat5)\\n\", \"(n, v) = (int(input()), 1)\\nfor i in range(n - 4, n + 1):\\n\\tv *= i ** 2\\nprint(v // 120)\\n\", \"from math import *\\nn = int(input())\\n\\ndef fact(n):\\n\\tif n <= 1:\\n\\t\\treturn 1\\n\\telse:\\n\\t\\treturn n * fact(n - 1)\\n\\ndef binom(n, k):\\n\\treturn fact(n) // (fact(k) * fact(n - k))\\nprint(binom(n, 5) * fact(n) // fact(n - 5))\\n\", \"n = int(input())\\nans = 1\\nfor i in range(n, n - 5, -1):\\n\\tans *= i * i\\nfor i in range(1, 6):\\n\\tans //= i\\nprint(ans)\\n\"]','EASY','Baekjoon','https://www.acmicpc.net/problem/1000','1s','128MB','{\"input\": \"５\", \"output\": \"１２０\"}',NULL,NULL),(2,'Knapsack Problem','int dp[101][100001];','HARD','Programmers','https://programmers.co.kr/1','2s','256MB','{\"input\": \"items...\", \"output\": \"max_val\"}',NULL,NULL),(3,'Bitwise XOR operations','result ^= arr[i];','MEDIUM','LeetCode','https://leetcode.com/1','1s','64MB','{\"input\": \"[1,2,1]\", \"output\": \"2\"}',NULL,NULL),(4,'Find median in range','update(1, 1, n, i, val);','VERY_HARD','Codeforces','https://codeforces.com/1','3s','512MB','{\"input\": \"queries...\", \"output\": \"medians\"}',NULL,NULL),(5,'Quick Sort Implementation','partition(arr, low, high);','MEDIUM','Basic',NULL,'1s','128MB','{\"input\": \"5 3 1\", \"output\": \"1 3 5\"}',NULL,NULL);
/*!40000 ALTER TABLE `problems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `skills`
--

DROP TABLE IF EXISTS `skills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skills` (
  `skill_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK: 자동 증가',
  `skill_name` varchar(100) NOT NULL COMMENT '8개 속성 (BFS, DP 등)',
  PRIMARY KEY (`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `skills`
--

LOCK TABLES `skills` WRITE;
/*!40000 ALTER TABLE `skills` DISABLE KEYS */;
INSERT INTO `skills` VALUES (1,'Amortized analysis'),(2,'Bit manipulation'),(3,'Complete search'),(4,'Data structures'),(5,'Dynamic programming'),(6,'Greedy algorithms'),(7,'Range queries'),(8,'Sorting');
/*!40000 ALTER TABLE `skills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `social`
--

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

--
-- Dumping data for table `social`
--

LOCK TABLES `social` WRITE;
/*!40000 ALTER TABLE `social` DISABLE KEYS */;
INSERT INTO `social` VALUES (1,'google','***TEST_OAUTH_ID***',2);
/*!40000 ALTER TABLE `social` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profile_info`
--

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

--
-- Dumping data for table `user_profile_info`
--

LOCK TABLES `user_profile_info` WRITE;
/*!40000 ALTER TABLE `user_profile_info` DISABLE KEYS */;
INSERT INTO `user_profile_info` VALUES (1,1,'Charlie','https://api.dicebear.com/7.x/avataaars/svg?seed=Charlie','안녕하세요, 3년차 요기니 찰리입니다.',1500),(2,2,'SSAFY_Pro',NULL,'삼성 청년 SW 아카데미 14기 교육생입니다.',2800),(3,3,'AlgoMaster','https://api.dicebear.com/7.x/avataaars/svg?seed=Master','알고리즘 정복 중!',500);
/*!40000 ALTER TABLE `user_profile_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_skill_statistics`
--

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

--
-- Dumping data for table `user_skill_statistics`
--

LOCK TABLES `user_skill_statistics` WRITE;
/*!40000 ALTER TABLE `user_skill_statistics` DISABLE KEYS */;
INSERT INTO `user_skill_statistics` VALUES (1,3,1),(1,5,1),(2,3,1),(3,8,1);
/*!40000 ALTER TABLE `user_skill_statistics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

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

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-01-27 00:44:15',NULL,'LOGIN',0),(2,'2026-01-27 00:44:15',NULL,'SOCIAL',0),(3,'2026-01-27 00:44:15',NULL,'LOGIN',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'ssafy_hamcheese'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-03 10:35:16

-- +추가. history 테이블에 skill_id 컬럼 추가
ALTER TABLE `history` 
ADD COLUMN `skill_id` BIGINT NOT NULL COMMENT 'FK: skills 참조 (통계 집계용)' AFTER `user_id`;

-- skills 테이블과 외래키(FK) 연결
ALTER TABLE `history` 
ADD CONSTRAINT `FK_HISTORY_SKILL` 
FOREIGN KEY (`skill_id`) REFERENCES `skills`(`skill_id`);