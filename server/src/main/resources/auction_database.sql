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

INSERT INTO user (accountName, password, nickname, balance, description, avatarURL)
VALUES ('admin', 'admin', 'Quản trị viên', 0, 'Tài khoản điều hành hệ thống', 'null');

CREATE TABLE `item` (
                        `id` int NOT NULL AUTO_INCREMENT,
                        `sellerAccountName` varchar(50) NOT NULL,
                        `type` varchar(50) NOT NULL,        -- discriminator cho ItemFactoryProducer
                        `name` varchar(100) NOT NULL,
                        `description` text DEFAULT NULL,
                        `imageURL` MEDIUMTEXT DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        CONSTRAINT `fk_item_seller` FOREIGN KEY (`sellerAccountName`) REFERENCES `user` (`accountName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auction_session` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `startTime` datetime NOT NULL,
                                   `endTime` datetime NOT NULL,
                                   `currentPrice` double NOT NULL,
                                   `bidIncrease` double NOT NULL,
                                   `status` varchar(50) NOT NULL,
                                   `highestBidderAccount` varchar(100) DEFAULT NULL,
                                   `itemId` int NOT NULL,
                                   PRIMARY KEY (`id`),
                                   CONSTRAINT `fk_session_item` FOREIGN KEY (`itemId`) REFERENCES `item` (`id`),
                                   CONSTRAINT `fk_session_highest_bidder` FOREIGN KEY (`highestBidderAccount`) REFERENCES `user` (`accountName`)
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

CREATE TABLE `participation` (
`accountName` varchar(50) NOT NULL,
`auctionSessionId` int NOT NULL,
`roleType` varchar(20) NOT NULL,
PRIMARY KEY (`accountName`, `auctionSessionId`),
CONSTRAINT `fk_participation_user` FOREIGN KEY (`accountName`) REFERENCES `user` (`accountName`),
CONSTRAINT `fk_participation_session` FOREIGN KEY (`auctionSessionId`) REFERENCES `auction_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;