CREATE DATABASE  IF NOT EXISTS `travelreservationdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `travelreservationdb`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: travelreservationdb
-- ------------------------------------------------------
-- Server version	8.0.46

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
-- Table structure for table `aircraft`
--

DROP TABLE IF EXISTS `aircraft`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aircraft` (
  `aircraft_id` int NOT NULL,
  `model` varchar(100) DEFAULT NULL,
  `manufacturer` varchar(100) DEFAULT NULL,
  `total_seats` int DEFAULT NULL,
  `econ_capacity` int DEFAULT NULL,
  `bus_capacity` int DEFAULT NULL,
  `first_capacity` int DEFAULT NULL,
  `airline_id` char(2) DEFAULT NULL,
  PRIMARY KEY (`aircraft_id`),
  KEY `airline_id` (`airline_id`),
  CONSTRAINT `aircraft_ibfk_1` FOREIGN KEY (`airline_id`) REFERENCES `airline` (`airline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aircraft`
--

LOCK TABLES `aircraft` WRITE;
/*!40000 ALTER TABLE `aircraft` DISABLE KEYS */;
INSERT INTO `aircraft` VALUES (1,'737','Boeing',200,150,30,20,'UA'),(2,'747','Boeing',200,150,30,20,'AA'),(3,'A320','Airbus',150,100,30,20,'UA'),(99,'Test Plane','Boeing',0,1,0,0,'UA');
/*!40000 ALTER TABLE `aircraft` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `airline`
--

DROP TABLE IF EXISTS `airline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `airline` (
  `airline_id` char(2) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`airline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `airline`
--

LOCK TABLES `airline` WRITE;
/*!40000 ALTER TABLE `airline` DISABLE KEYS */;
INSERT INTO `airline` VALUES ('AA','American Airlines'),('DL','Delta'),('JB','JetBlue Airways'),('SW','Southwest Airlines'),('UA','United Airlines');
/*!40000 ALTER TABLE `airline` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `airline_airport`
--

DROP TABLE IF EXISTS `airline_airport`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `airline_airport` (
  `airline_id` char(2) NOT NULL,
  `airport_code` char(3) NOT NULL,
  PRIMARY KEY (`airline_id`,`airport_code`),
  KEY `airport_code` (`airport_code`),
  CONSTRAINT `airline_airport_ibfk_1` FOREIGN KEY (`airline_id`) REFERENCES `airline` (`airline_id`),
  CONSTRAINT `airline_airport_ibfk_2` FOREIGN KEY (`airport_code`) REFERENCES `airport` (`airport_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `airline_airport`
--

LOCK TABLES `airline_airport` WRITE;
/*!40000 ALTER TABLE `airline_airport` DISABLE KEYS */;
/*!40000 ALTER TABLE `airline_airport` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `airport`
--

DROP TABLE IF EXISTS `airport`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `airport` (
  `airport_code` char(3) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`airport_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `airport`
--

LOCK TABLES `airport` WRITE;
/*!40000 ALTER TABLE `airport` DISABLE KEYS */;
INSERT INTO `airport` VALUES ('EWR','Newark Liberty International','Newark','USA'),('JFK','John F. Kennedy International','New York','USA'),('LAX','Los Angeles International','Los Angeles','USA'),('MIA','Miami International','Miami','USA'),('ORD','O\'Hare International','Chicago','USA'),('SFO','San Francisco International','San Francisco','USA');
/*!40000 ALTER TABLE `airport` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `customer_id` int NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `account_creation_date` date DEFAULT NULL,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES (1,'Aryan','Navin','test@rutgers.edu',NULL,NULL,NULL,'an838','password123','2026-04-26'),(2,'Youssef','Hassan','test@rutgers.edu',NULL,NULL,NULL,'yhh10','password123','2026-04-25'),(3,'Derick','Robles','test@rutgers.edu',NULL,NULL,NULL,'dmr386','password123','2026-04-26');
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `employee_id` int NOT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
INSERT INTO `employee` VALUES (1,'Test','Rep',NULL,NULL,'rep123','password123','representative'),(2,'System','Admin',NULL,NULL,'admin123','password123','admin');
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flight`
--

DROP TABLE IF EXISTS `flight`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flight` (
  `flight_number` varchar(10) NOT NULL,
  `airline_id` char(2) NOT NULL,
  `aircraft_id` int DEFAULT NULL,
  `departure_airport` char(3) DEFAULT NULL,
  `arrival_airport` char(3) DEFAULT NULL,
  `flight_date` date DEFAULT NULL,
  `arrival_date` date DEFAULT NULL,
  `departure_time` time DEFAULT NULL,
  `arrival_time` time DEFAULT NULL,
  `flight_type` varchar(15) DEFAULT NULL,
  `base_price` float DEFAULT NULL,
  PRIMARY KEY (`flight_number`,`airline_id`),
  KEY `airline_id` (`airline_id`),
  KEY `aircraft_id` (`aircraft_id`),
  KEY `departure_airport` (`departure_airport`),
  KEY `arrival_airport` (`arrival_airport`),
  CONSTRAINT `flight_ibfk_1` FOREIGN KEY (`airline_id`) REFERENCES `airline` (`airline_id`),
  CONSTRAINT `flight_ibfk_2` FOREIGN KEY (`aircraft_id`) REFERENCES `aircraft` (`aircraft_id`),
  CONSTRAINT `flight_ibfk_3` FOREIGN KEY (`departure_airport`) REFERENCES `airport` (`airport_code`),
  CONSTRAINT `flight_ibfk_4` FOREIGN KEY (`arrival_airport`) REFERENCES `airport` (`airport_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flight`
--

LOCK TABLES `flight` WRITE;
/*!40000 ALTER TABLE `flight` DISABLE KEYS */;
INSERT INTO `flight` VALUES ('AA111','AA',2,'LAX','JFK','2026-05-10','2026-05-10','14:00:00','19:00:00','domestic',320),('AA404','AA',2,'JFK','LAX','2026-05-06','2026-05-07','22:00:00','01:00:00','domestic',450),('AA808','AA',2,'JFK','MIA','2026-04-25','2026-04-25','05:30:00','08:30:00','domestic',1500),('DL606','DL',2,'JFK','ORD','2026-04-29','2026-04-29','09:00:00','10:30:00','domestic',550),('SW909','SW',3,'EWR','ORD','2026-05-05','2026-05-05','18:00:00','20:15:00','domestic',210),('UA000','UA',99,'EWR','LAX','2026-04-26','2026-04-26','12:00:00','15:00:00','domestic',100),('UA101','UA',1,'EWR','ORD','2026-05-01','2026-05-01','08:00:00','10:30:00','domestic',150),('UA202','UA',1,'SFO','EWR','2026-05-12','2026-05-12','14:00:00','17:30:00','domestic',250),('UA303','UA',3,'ORD','LAX','2026-05-10','2026-05-10','12:30:00','15:00:00','domestic',200),('UA505','UA',3,'EWR','LAX','2026-04-24','2026-04-24','10:00:00','16:00:00','domestic',300),('UA707','UA',1,'EWR','SFO','2026-05-08','2026-05-08','12:00:00','15:45:00','domestic',890),('UA777','UA',1,'ORD','EWR','2026-05-04','2026-05-04','14:00:00','16:30:00','domestic',120),('UA888','UA',1,'LAX','ORD','2026-05-15','2026-05-15','07:00:00','09:00:00','domestic',100),('UA999','UA',1,'LAX','EWR','2026-04-30','2026-04-30','10:00:00','18:00:00','domestic',300);
/*!40000 ALTER TABLE `flight` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flight_days`
--

DROP TABLE IF EXISTS `flight_days`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flight_days` (
  `flight_number` varchar(10) NOT NULL,
  `airline_id` char(2) NOT NULL,
  `day_of_week` varchar(15) NOT NULL,
  PRIMARY KEY (`flight_number`,`airline_id`,`day_of_week`),
  CONSTRAINT `flight_days_ibfk_1` FOREIGN KEY (`flight_number`, `airline_id`) REFERENCES `flight` (`flight_number`, `airline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flight_days`
--

LOCK TABLES `flight_days` WRITE;
/*!40000 ALTER TABLE `flight_days` DISABLE KEYS */;
INSERT INTO `flight_days` VALUES ('AA111','AA','Thursday'),('AA111','AA','Tuesday'),('AA404','AA','Friday'),('AA404','AA','Monday'),('AA404','AA','Sunday'),('AA808','AA','Friday'),('AA808','AA','Monday'),('AA808','AA','Wednesday'),('DL606','DL','Monday'),('DL606','DL','Thursday'),('SW909','SW','Sunday'),('SW909','SW','Thursday'),('UA000','UA','Friday'),('UA101','UA','Friday'),('UA101','UA','Monday'),('UA101','UA','Wednesday'),('UA202','UA','Monday'),('UA202','UA','Wednesday'),('UA303','UA','Thursday'),('UA303','UA','Tuesday'),('UA505','UA','Saturday'),('UA505','UA','Wednesday'),('UA707','UA','Saturday'),('UA707','UA','Tuesday'),('UA777','UA','Saturday'),('UA777','UA','Thursday'),('UA888','UA','Sunday'),('UA888','UA','Tuesday'),('UA999','UA','Friday'),('UA999','UA','Monday'),('UA999','UA','Wednesday');
/*!40000 ALTER TABLE `flight_days` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `indirectflights`
--

DROP TABLE IF EXISTS `indirectflights`;
/*!50001 DROP VIEW IF EXISTS `indirectflights`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `indirectflights` AS SELECT 
 1 AS `f1_num`,
 1 AS `air1`,
 1 AS `dep`,
 1 AS `stopover`,
 1 AS `dep_t1`,
 1 AS `arr_t1`,
 1 AS `date1`,
 1 AS `f2_num`,
 1 AS `air2`,
 1 AS `dest`,
 1 AS `dep_t2`,
 1 AS `arr_t2`,
 1 AS `date2_arr`,
 1 AS `total_price`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `qa_entry`
--

DROP TABLE IF EXISTS `qa_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qa_entry` (
  `qa_id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int DEFAULT NULL,
  `question_text` text NOT NULL,
  `answer_text` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`qa_id`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `qa_entry_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qa_entry`
--

LOCK TABLES `qa_entry` WRITE;
/*!40000 ALTER TABLE `qa_entry` DISABLE KEYS */;
INSERT INTO `qa_entry` VALUES (1,NULL,'What is the baggage allowance for Economy?','Standard allowance is one carry-on and one checked bag.','2026-05-05 15:55:52'),(2,NULL,'How do I cancel my booking?','You can cancel through the \"My Reservations\" dashboard. Note that Economy tickets incur a $50 fee.','2026-05-05 15:55:52'),(3,NULL,'Do you offer special meals?','Yes, you can request special meals during the booking process at no extra charge.','2026-05-05 15:55:52'),(4,NULL,'What happens if I am on a waiting list?','You will receive an alert if a seat becomes available. Your position depends on when you joined.','2026-05-05 15:55:52');
/*!40000 ALTER TABLE `qa_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ticket`
--

DROP TABLE IF EXISTS `ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket` (
  `ticket_number` int NOT NULL,
  `customer_id` int DEFAULT NULL,
  `total_fare` float DEFAULT NULL,
  `purchase_datetime` datetime DEFAULT NULL,
  `status` varchar(15) DEFAULT NULL,
  `is_flexible` tinyint(1) DEFAULT NULL,
  `quantity` int DEFAULT '1',
  PRIMARY KEY (`ticket_number`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `ticket_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ticket`
--

LOCK TABLES `ticket` WRITE;
/*!40000 ALTER TABLE `ticket` DISABLE KEYS */;
INSERT INTO `ticket` VALUES (247882,1,575,'2026-05-05 12:02:16','active',0,1),(572488,1,375,'2026-05-05 12:00:34','active',0,1),(717662,1,475,'2026-05-05 12:00:48','active',0,1),(798598,1,1725,'2026-05-05 12:02:32','active',1,1),(942839,1,1525,'2026-05-05 12:00:43','active',1,1),(961434,1,1095,'2026-05-05 12:01:18','active',0,1);
/*!40000 ALTER TABLE `ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ticket_segment`
--

DROP TABLE IF EXISTS `ticket_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_segment` (
  `ticket_number` int NOT NULL,
  `sequence_number` int NOT NULL,
  `flight_number` varchar(10) DEFAULT NULL,
  `airline_id` char(2) DEFAULT NULL,
  `flight_date` date DEFAULT NULL,
  `class` varchar(10) DEFAULT NULL,
  `special_meal` tinyint(1) DEFAULT NULL,
  `seat_number` varchar(10) DEFAULT NULL,
  `from_airport` char(3) DEFAULT NULL,
  `to_airport` char(3) DEFAULT NULL,
  PRIMARY KEY (`ticket_number`,`sequence_number`),
  KEY `flight_number` (`flight_number`,`airline_id`),
  KEY `from_airport` (`from_airport`),
  KEY `to_airport` (`to_airport`),
  CONSTRAINT `ticket_segment_ibfk_1` FOREIGN KEY (`ticket_number`) REFERENCES `ticket` (`ticket_number`),
  CONSTRAINT `ticket_segment_ibfk_2` FOREIGN KEY (`flight_number`, `airline_id`) REFERENCES `flight` (`flight_number`, `airline_id`),
  CONSTRAINT `ticket_segment_ibfk_3` FOREIGN KEY (`from_airport`) REFERENCES `airport` (`airport_code`),
  CONSTRAINT `ticket_segment_ibfk_4` FOREIGN KEY (`to_airport`) REFERENCES `airport` (`airport_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ticket_segment`
--

LOCK TABLES `ticket_segment` WRITE;
/*!40000 ALTER TABLE `ticket_segment` DISABLE KEYS */;
INSERT INTO `ticket_segment` VALUES (247882,1,'DL606','DL','2026-04-29','Economy',0,'4B','JFK','ORD'),(572488,1,'UA101','UA','2026-05-01','Economy',0,'23E','EWR','ORD'),(572488,2,'UA303','UA','2026-05-10','Economy',0,'20D','ORD','LAX'),(717662,1,'UA999','UA','2026-04-30','Economy',0,'18B','LAX','EWR'),(717662,2,'UA101','UA','2026-05-01','Economy',0,'14E','EWR','ORD'),(798598,1,'UA999','UA','2026-04-30','First',1,'15E','LAX','EWR'),(798598,2,'UA101','UA','2026-05-01','First',1,'12E','EWR','ORD'),(798598,3,'UA303','UA','2026-05-10','First',1,'8C','ORD','LAX'),(942839,1,'UA999','UA','2026-04-30','First',1,'4C','LAX','EWR'),(942839,2,'UA101','UA','2026-05-01','First',1,'11C','EWR','ORD'),(961434,1,'AA404','AA','2026-05-06','Business',1,'18C','JFK','LAX'),(961434,2,'AA111','AA','2026-05-10','Business',1,'1F','LAX','JFK');
/*!40000 ALTER TABLE `ticket_segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `waiting_list`
--

DROP TABLE IF EXISTS `waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `waiting_list` (
  `waitlist_id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int DEFAULT NULL,
  `flight_number` varchar(50) DEFAULT NULL,
  `airline_id` varchar(5) DEFAULT NULL,
  `class` varchar(20) DEFAULT NULL,
  `added_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`waitlist_id`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `waiting_list_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `waiting_list`
--

LOCK TABLES `waiting_list` WRITE;
/*!40000 ALTER TABLE `waiting_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `waiting_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `indirectflights`
--

/*!50001 DROP VIEW IF EXISTS `indirectflights`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `indirectflights` AS select `f1`.`flight_number` AS `f1_num`,`f1`.`airline_id` AS `air1`,`f1`.`departure_airport` AS `dep`,`f1`.`arrival_airport` AS `stopover`,`f1`.`departure_time` AS `dep_t1`,`f1`.`arrival_time` AS `arr_t1`,`f1`.`flight_date` AS `date1`,`f2`.`flight_number` AS `f2_num`,`f2`.`airline_id` AS `air2`,`f2`.`arrival_airport` AS `dest`,`f2`.`departure_time` AS `dep_t2`,`f2`.`arrival_time` AS `arr_t2`,`f2`.`arrival_date` AS `date2_arr`,(`f1`.`base_price` + `f2`.`base_price`) AS `total_price` from (`flight` `f1` join `flight` `f2` on((`f1`.`arrival_airport` = `f2`.`departure_airport`))) where ((`f1`.`airline_id` = `f2`.`airline_id`) and ((`f2`.`flight_date` > `f1`.`arrival_date`) or ((`f2`.`flight_date` = `f1`.`arrival_date`) and (`f2`.`departure_time` > `f1`.`arrival_time`)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-05 12:25:58
