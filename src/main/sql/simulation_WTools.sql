CREATE DATABASE `WTools_simulation`;

CREATE USER 'AgvYidUser'@'localhost' IDENTIFIED BY '12345678';
GRANT ALL PRIVILEGES ON WTools_simulation.* TO 'AgvYidUser'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE `V_MOCTA`(
    `id` int auto_increment PRIMARY KEY,
    `TA001` varchar(4) NOT NULL,
    `TA002` varchar(11) NOT NULL,
    `TA006` varchar(40) NOT NULL,
    `TA007` varchar(6) NOT NULL,
    `TA034` varchar(120) NOT NULL
);

INSERT INTO `V_MOCTA`(`TA001`, `TA002`, `TA006`, `TA007`, `TA034`) VALUES
('MO01', '20200907001', '0062210013', "PCS", '1/4"52T電金FA-2膠把TwisterFACOM (R.360 NEW)'),
('MO01', '20200907002', '0062210021', "PCS", '1/4"52T電金FA-2膠把TwisterFACOM (R.360PB 紙吊卡)'),
('MO01', '20200907003', '0062210021', "PCS", '1/4"52T電金FA-2膠把TwisterFACOM (R.360PB 紙吊卡)'),
('MO12', '20230725001', '0254780011', "PCS", '1/2"72T電金全拋八角葫蘆柄軟打 KINCROME'),
('MO12', '20230801001', '0254780011', "PCS", '1/2"72T電金全拋八角葫蘆柄軟打 KINCROME'),
('MO12', '20230802001', '0254780011', "PCS", '1/2"72T電金全拋八角葫蘆柄軟打 KINCROME');

SELECT * FROM `V_MOCTA`;

SELECT `TA006` AS `object_number`, `TA034` AS `object_name`, `TA007` AS `line_code` FROM `V_MOCTA` WHERE `TA001` = "MO01" AND `TA002` = "20200907001";
SELECT `TA006` AS `object_number`, `TA034` AS `object_name` FROM `V_MOCTA` WHERE `TA001` = "MO02" AND `TA002` = 20231002001;