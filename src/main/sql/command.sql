SHOW DATABASES;
SHOW TABLES;
SELECT DATABASE();
SELECT * FROM `notification_history_title_data`;
UPDATE `task_history` SET `status` = 100 WHERE (`task_number` = '#202306300008');
select DISTINCT agv_id,analysis_id from analysis order by analysis_id DESC LIMIT 3;
SELECT agv_id, MAX(analysis_id) FROM analysis WHERE agv_id IN (1, 2, 3) GROUP BY agv_id ORDER BY agv_id;
DELETE FROM grid_list WHERE station_id = 20;

SELECT * FROM task_history WHERE DATE_FORMAT(STR_TO_DATE(create_task_time, '%Y%m%d%H%i%s'), '%Y-%m-%d') = CURDATE() ORDER BY id DESC;
SELECT * FROM now_task_list;
SELECT * FROM task_list;
SELECT * FROM task_detail;
SELECT * FROM agv_data;
SELECT * FROM grid_list;
SELECT * FROM analysis;
SELECT * FROM notification_history;
SELECT id, title_id, message_id, DATE_FORMAT(create_time, '%Y%m%d%H%i%s') AS create_time FROM notification_history ORDER BY id DESC;
SELECT nh.id, ntd.name, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time, md.level, md.content FROM notification_history nh INNER JOIN notification_title_data ntd ON nh.title_id = ntd.id INNER JOIN message_data md ON nh.message_id = md.id ORDER BY nh.create_time DESC LIMIT 100;
SELECT * FROM notification_history_message_data;
SELECT * FROM mode_data;
SELECT * FROM `notification_station_data`;
SELECT * FROM analysis WHERE analysis_id = 184;
SELECT year, month, day FROM analysis WHERE (year, month, day) <= (SELECT MAX(year), MAX(month), MAX(day) FROM analysis) ORDER BY year DESC, month DESC, day DESC LIMIT 1;
SELECT agv_id, MAX(analysis_id) as analysis_id FROM analysis WHERE agv_id IN (1, 2, 3) GROUP BY agv_id ORDER BY agv_id;
SELECT * FROM station_data;
SELECT `tag` FROM `station_data` where `name` LIKE '%-10';
SELECT * FROM mode;
SELECT * FROM notification_history WHERE DATE_FORMAT(STR_TO_DATE(create_time, '%Y%m%d%H%i%s'), '%Y-%m-%d') = CURDATE() ORDER BY id DESC;


SELECT td.id, td.task_number, tdt.name AS title, td.sequence, sd.name AS start, sd.id AS start_id,
sdd.name AS terminal, sdd.tag AS terminal_tag, md.mode AS mode, md.memo AS mode_memo, td.status FROM task_detail td
INNER JOIN task_detail_title tdt ON td.title_id = tdt.id
LEFT JOIN station_data sd ON td.start_id = sd.id
LEFT JOIN station_data sdd ON td.terminal_id = sdd.id
INNER JOIN mode_data md ON td.mode_id = md.id WHERE task_number = "#YE202310310002" ORDER BY td.sequence ;

SELECT tl.id, tl.task_number, tl.create_task_time, tl.steps, tl.progress, tp.name AS phase, tl.status
	FROM task_list tl INNER JOIN task_phase tp ON tl.phase_id = tp.id ORDER BY id DESC LIMIT 100;

SELECT * FROM task_detail;
SELECT * FROM task_list;
SELECT * FROM task_phase;
SELECT * FROM now_task_list;
SElECT * FROM now_task_list WHERE task_number LIKE "#YE%" OR task_number LIKE "#RE%" ORDER BY id;
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#YE202311030001', 8);
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#RE202311030002', 8);
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#NE202311030003', 8);
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#YE202311030004', 8);
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#RE202311030005', 8);
INSERT INTO `now_task_list`(`task_number`, `steps`) VALUES('#NE202311030006', 8);

UPDATE `task_detail` SET `status` = 100 WHERE `task_number` = '#YE202310310002' AND `sequence` = 7;

SELECT ntl.id, ntl.task_number, ntl.steps, ntl.progress, tp.name AS phase
FROM now_task_list ntl INNER JOIN task_phase tp ON ntl.phase_id = tp.id ORDER BY id;

UPDATE `now_task_list` SET `phase_id` = 5 WHERE `task_number` = "#YE202310310002";

SELECT Length(name) FROM `station_data` WHERE `name` LIKE '%-S';
SELECT * FROM `station_data` WHERE `name` LIKE '%-S';
SELECT COUNT(*) AS length FROM `station_data` WHERE `name` LIKE '1-R%';
SELECT tag FROM `station_data` WHERE `name` LIKE "E-%";
SELECT * FROM notification_history;

SELECT nh.id, ntd.name, nh.level, nh.message, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id ORDER BY nh.create_time;


SELECT * FROM station_data;
SELECT * FROM agv_data;
SELECT * FROM grid_list;
SELECT * FROM station_data WHERE `name` LIKE '3-C%';

SELECT gl.id, sd.name AS station, gl.status, gl.work_number_1, gl.work_number_2, gl.work_number_3, gl.work_number_4,
gl.object_name_1, gl.object_name_2, gl.object_name_3, gl.object_name_4, gl.object_number_1, gl.object_number_2, gl.object_number_3, gl.object_number_4,
gl.line_code_1, gl.line_code_2, gl.line_code_3, gl.line_code_4,
gl.create_time FROM grid_list gl
INNER JOIN station_data sd ON gl.station_id = sd.id ORDER BY id;

SELECT td.id, td.task_number, tdt.name AS title, td.sequence, sd.name AS start, sd.id AS start_id,
                sdd.name AS terminal, sdd.id AS terminal_id, md.mode AS mode, md.memo AS mode_memo, td.status FROM task_detail td
                INNER JOIN task_detail_title tdt ON td.title_id = tdt.id
                LEFT JOIN station_data sd ON td.start_id = sd.id
                LEFT JOIN station_data sdd ON td.terminal_id = sdd.id
                INNER JOIN mode_data md ON td.mode_id = md.id WHERE task_number = "#NE202401180007" ORDER BY td.sequence;

UPDATE `grid_list` SET `status` = 2 WHERE `station_id` = 21;
UPDATE `grid_list` SET `work_number_1` = "MO01-20200907001", `work_number_2` = "MO02-20231002001", `work_number_3` = "MO02-20200907001"
, `object_name_1` = '1/4"52T電金FA-2膠把TwisterFACOM (R.360 NEW)' , `object_name_2` = '1/4"52T電金FA-2膠把TwisterFACOM (R.360PB 紙吊卡)' , `object_name_3` = '1/4"52T電金FA-2膠把TwisterFACOM (R.360PB 紙吊卡)' , `object_number_1` = "0062210013"
, `object_number_2` = "0062210021" , `object_number_3` = "0062210021", `line_code_1` = "00411", `line_code_2` = "00411", `line_code_3` = "00412", `create_time` = "20240116132223" WHERE `station_id` = 103;

UPDATE `grid_list` SET `work_number_1` = "XX00-20240116001" WHERE `station_id` = 21;
UPDATE `grid_list` SET `object_name_1` = "測試測試測試測試測試測試測試測試測試測試測試" WHERE `station_id` = 21;
UPDATE `grid_list` SET `object_number_1` = "1234567899" WHERE `station_id` = 21;

SELECT gl.id, sd.name AS station, gl.status, gl.work_number_1, gl.work_number_2, gl.work_number_3, gl.work_number_4,
                gl.object_name_1, gl.object_name_2, gl.object_name_3, gl.object_name_4, gl.object_number_1, gl.object_number_2, gl.object_number_3, gl.object_number_4,
                gl.line_code_1, gl.line_code_2, gl.line_code_3, gl.line_code_4, gl.create_time FROM grid_list gl INNER JOIN station_data sd ON gl.station_id = sd.id WHERE sd.name = "2-A-1";

SELECT nh.id, ntd.name, DATE_FORMAT(nh.create_time, '%Y%m%d%H%i%s') AS create_time,
                md.level, md.content FROM notification_history nh INNER JOIN notification_history_title_data ntd ON nh.title_id = ntd.id
                INNER JOIN notification_history_message_data md ON nh.message_id = md.id ORDER BY nh.create_time DESC LIMIT 100;


SELECT td.id, td.task_number, tdt.name AS title, td.sequence, sd.name AS start, sd.tag AS start_tag,
                sdd.name AS terminal, sdd.tag AS terminal_tag, md.mode AS mode, md.memo AS mode_memo, td.status, td.create_task_time FROM task_detail td
                INNER JOIN task_detail_title tdt ON td.title_id = tdt.id
                LEFT JOIN station_data sd ON td.start_id = sd.id
                LEFT JOIN station_data sdd ON td.terminal_id = sdd.id
                INNER JOIN mode_data md ON td.mode_id = md.id WHERE td.task_number NOT LIKE '#%B%'
                ORDER BY CONVERT(SUBSTRING(td.task_number, 4, 14), SIGNED INTEGER) DESC, td.sequence DESC;


SELECT
    work_number,
    COUNT(*) AS count
FROM
    work_number_history
WHERE
    time >= CURDATE() - INTERVAL 2 MONTH
GROUP BY
    work_number;

SELECT
    SUBSTRING_INDEX(work_number, '-', 1) AS TA001,
    COUNT(*) AS count
FROM
    work_number_history
WHERE
    time >= CURDATE() - INTERVAL 2 MONTH
GROUP BY
    TA001;
