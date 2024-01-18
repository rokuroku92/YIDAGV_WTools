CREATE DATABASE `AGV_WTools`;

CREATE TABLE `agv_data`( -- 建造agv_data TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `name` varchar(20) NOT NULL, -- AGV名稱
    `img` varchar(30), -- AGV圖片名稱
    `memo` varchar(50) NOT NULL -- 備忘錄
);
CREATE TABLE `station_data`( -- 建造station_data TABLE 車站
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `tag` int NOT NULL, -- tag
    `name` varchar(20) NOT NULL, -- 車站名稱
    `memo` varchar(50) NOT NULL -- 備忘錄
);

CREATE TABLE `grid_list`( -- 建造station_data TABLE 車站
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `station_id` int NOT NULL UNIQUE,FOREIGN KEY (`station_id`) REFERENCES `station_data`(`id`)ON DELETE CASCADE, -- 車站ID
    `status` int NOT NULL, -- 格位狀態 0:free|1:booked|2:occupied|3:over time|6:disable
    `work_number_1` varchar(20),`work_number_2` varchar(20),`work_number_3` varchar(20),`work_number_4` varchar(20), -- 工單號碼
    `object_name_1` varchar(120),`object_name_2` varchar(120),`object_name_3` varchar(120),`object_name_4` varchar(120), -- 品名
    `object_number_1` varchar(50),`object_number_2` varchar(50),`object_number_3` varchar(50),`object_number_4` varchar(50), -- 物料號碼
    `line_code_1` varchar(20),`line_code_2` varchar(20),`line_code_3` varchar(20),`line_code_4` varchar(20), -- 線別代號
    `create_time` varchar(20) -- 創建時間 20231026141155
);

CREATE TABLE `mode_data`( -- 建造mode TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `mode` int NOT NULL, -- mode
    `name` varchar(20) NOT NULL, -- 模式名稱
    `memo` varchar(50) NOT NULL -- 備忘錄
);


CREATE TABLE `task_list`( -- 建造task_list TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `task_number` varchar(20) unique, -- 任務ID #202310260001
    `create_task_time` varchar(20), -- 創建時間 20231026141155
    `steps` int not null, -- 步驟數量
    `progress` int not null default 0, -- 任務進度
    `phase_id` int not null default 1,FOREIGN KEY (`phase_id`) REFERENCES `task_phase`(`id`)ON DELETE CASCADE, -- 任務階段
    `status` int NOT NULL default 0 -- 是否完成
);
CREATE INDEX idx_task_number ON task_list (task_number);

CREATE TABLE `task_phase`( -- 建造task_list TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `name` varchar(20) -- 主鍵
);

CREATE TABLE `task_detail_title`( -- 建造task_detail TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `name` varchar(20) -- 名字
);

CREATE TABLE `task_detail`( -- 建造task_detail TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `task_number` varchar(20), -- 任務ID
    `title_id` int NOT NULL,FOREIGN KEY (`title_id`) REFERENCES `task_detail_title`(`id`)ON DELETE CASCADE, -- title ID
    `sequence` int NOT NULL, -- 順序
    `start_id` int,FOREIGN KEY (`start_id`) REFERENCES `station_data`(`id`)ON DELETE CASCADE, -- 車站ID
    `terminal_id` int,FOREIGN KEY (`terminal_id`) REFERENCES `station_data`(`id`)ON DELETE CASCADE, -- 車站ID
    `mode_id` int NOT NULL,FOREIGN KEY (`mode_id`) REFERENCES `mode_data`(`id`)ON DELETE CASCADE, -- 模式ID
    `status` int NOT NULL default 0 -- 是否完成
);
CREATE INDEX idx_task_number ON task_detail (task_number);

CREATE TABLE `now_task_list`( -- 建造task_list TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `task_number` varchar(20) unique, -- 任務ID #202310260001
    `steps` int not null, -- 步驟數量
    `progress` int not null default 0, -- 任務進度
    `phase_id` int not null default 1,FOREIGN KEY (`phase_id`) REFERENCES `task_phase`(`id`)ON DELETE CASCADE -- 任務階段
);
CREATE INDEX idx_task_number ON now_task_list (task_number);


CREATE TABLE `notification_history_title_data`( -- 建造notification_title_data TABLE for notification_history
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `name` varchar(20) NOT NULL, -- 名稱
    `memo` varchar(30) NOT NULL -- 備忘錄
);

CREATE TABLE `notification_history`( -- 建造notify_history TABLE
	`id` int AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `title_id` int NOT NULL,FOREIGN KEY (`title_id`) REFERENCES `notification_history_title_data`(`id`)ON DELETE CASCADE, -- 標題
    `level` int NOT NULL, -- 通知等級
    `message` varchar(50) NOT NULL, -- 內容
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX `idx_create_time` (`create_time`) -- 創建時間
);

CREATE TABLE `analysis`( -- 建造analysis TABLE
    `analysis_id` bigint AUTO_INCREMENT PRIMARY KEY, -- 主鍵
    `agv_id` int NOT NULL,FOREIGN KEY (`agv_id`) REFERENCES `agv_data`(`id`)ON DELETE CASCADE,
    `year` int NOT NULL, -- 年
    `month` int NOT NULL, -- 月
    `day` int NOT NULL, -- 日
    `week` int NOT NULL, -- 星期
    `working_minute` int, -- 工作時數
    `open_minute` int, -- 開機時數
    `task` int -- 任務數
);
ALTER TABLE `analysis` ADD UNIQUE (`agv_id`, `year`, `month`, `day`);