-- Mesos IS26-AM6 — Database schema for FA1 (game rankings)
-- Run once before starting the server:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS mesos;
USE mesos;

CREATE TABLE IF NOT EXISTS game_results (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(50)  NOT NULL,
    score       INT          NOT NULL,
    game_date   DATE         NOT NULL,
    num_players INT          NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_num_players_score
    ON game_results (num_players, score DESC);
