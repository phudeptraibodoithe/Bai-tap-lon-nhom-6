CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;
CREATE TABLE `user` (
  `accountName` varchar(50) NOT NULL,
  `nickname` varchar(50) NOT NULL, 
  `password` varchar(50) NOT NULL,
  `balance` double DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  `avatarURL` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `sellerAccount` varchar(50) NOT NULL,
  `imageURL` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `item_user_FK` (`sellerAccount`),
  CONSTRAINT `item_user_FK` FOREIGN KEY (`sellerAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `auction_session` (
  `id` int NOT NULL AUTO_INCREMENT,
  `itemId` int NOT NULL,
  `startTime` timestamp NOT NULL,
  `endTime` timestamp NOT NULL,
  `currentPrice` double NOT NULL,
  `bidIncrease` double NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `auction_session_item_FK` (`itemId`),
  CONSTRAINT `auction_session_item_FK` FOREIGN KEY (`itemId`) REFERENCES `item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `history` (
  `auctionSessionId` int NOT NULL, -- Giờ nó kiêm luôn chức năng Khóa chính
  `winnerAccount` varchar(50) NOT NULL, 
  `finalPrice` double NOT NULL,
  `completedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`auctionSessionId`), -- Set Khóa chính ở đây
  KEY `history_user_FK` (`winnerAccount`),
  CONSTRAINT `history_session_FK` FOREIGN KEY (`auctionSessionId`) REFERENCES `auction_session` (`id`),
  CONSTRAINT `history_user_FK` FOREIGN KEY (`winnerAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `bid` (
  `id` int NOT NULL AUTO_INCREMENT,
  `auctionSessionId` int NOT NULL,
  `bidderAccount` varchar(50) NOT NULL, -- Đổi từ INT sang VARCHAR
  `bidAmount` double NOT NULL,
  `bidTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `bid_session_FK` (`auctionSessionId`),
  KEY `bid_user_FK` (`bidderAccount`),
  CONSTRAINT `bid_session_FK` FOREIGN KEY (`auctionSessionId`) REFERENCES `auction_session` (`id`),
  CONSTRAINT `bid_user_FK` FOREIGN KEY (`bidderAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;