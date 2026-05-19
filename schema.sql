-- Mesos IS26-AM6 — Database schema for FA1 (game rankings)
-- Run once before starting the server:
--   mysql -u root -p < schema.sql

-- schema.sql corretto
CREATE DATABASE IF NOT EXISTS mesos;
USE mesos;

CREATE TABLE IF NOT EXISTS game_results (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(50)  NOT NULL,
    score       INT          NOT NULL,
    game_date   DATE         NOT NULL,
    num_players INT          NOT NULL
    );
