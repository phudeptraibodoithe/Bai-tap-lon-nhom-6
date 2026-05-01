CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

CREATE TABLE `user` (
  `accountName` varchar(50) NOT NULL,
  `nickname` varchar(50) NOT NULL, 
  `password` varchar(50) NOT NULL,
  `balance` double DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  `avatarURL` MEDIUMTEXT DEFAULT NULL,
  PRIMARY KEY (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auction_session` (
  `id` int NOT NULL AUTO_INCREMENT,
  `startTime` datetime NOT NULL,
  `endTime` datetime NOT NULL,
  `currentPrice` double NOT NULL,
  `bidIncrease` double NOT NULL,
  `status` varchar(50) NOT NULL, -- NOT_STARTED, ONGOING, ENDED, PENDING, CANCELED
  `sellerAccount` varchar(50) NOT NULL, -- Khóa ngoại trỏ về User
  `type` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `highestBidderAccount` varchar(100) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `imageURL` MEDIUMTEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_session_seller` FOREIGN KEY (`sellerAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `bid` (
  `id` int NOT NULL AUTO_INCREMENT,
  `auctionSessionId` int NOT NULL,
  `bidderAccount` varchar(50) NOT NULL,
  `bidAmount` double NOT NULL,
  `bidTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_bid_session` FOREIGN KEY (`auctionSessionId`) REFERENCES `auction_session` (`id`),
  CONSTRAINT `fk_bid_user` FOREIGN KEY (`bidderAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `history` (
  `auctionSessionId` int NOT NULL,
  `winnerAccount` varchar(50) NOT NULL, 
  `finalPrice` double NOT NULL,
  `completedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`auctionSessionId`),
  CONSTRAINT `fk_history_session` FOREIGN KEY (`auctionSessionId`) REFERENCES `auction_session` (`id`),
  CONSTRAINT `fk_history_user` FOREIGN KEY (`winnerAccount`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;