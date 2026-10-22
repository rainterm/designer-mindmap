-- =====================================================================
--  _base/database.sql
--  建库并切换；库名由 00-run-init.sh 按 DB_NAME 替换（默认 designer_agent）
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `designer_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `designer_agent`;
