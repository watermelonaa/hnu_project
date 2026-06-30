-- ====================================
-- 中文电影数据库测试脚本 (V2 - 增加覆盖逻辑)
-- ====================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS chinese_movies DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chinese_movies;

-- 1. 禁用外键检查（方便删除有外键关联的表）
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 删除已存在的表
DROP TABLE IF EXISTS viewing_records;
DROP TABLE IF EXISTS cinemas;
DROP TABLE IF EXISTS movie_actors;
DROP TABLE IF EXISTS actors;
DROP TABLE IF EXISTS movies;

-- 3. 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 电影表
CREATE TABLE movies (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL COMMENT '电影标题',
  director VARCHAR(100) NOT NULL COMMENT '导演',
  release_year INT NOT NULL COMMENT '上映年份',
  genre VARCHAR(50) COMMENT '类型',
  rating DECIMAL(3, 1) COMMENT '评分',
  box_office BIGINT COMMENT '票房（单位：万元）',
  description TEXT COMMENT '简介',
  country VARCHAR(50) DEFAULT '中国' COMMENT '制片国家/地区',
  duration INT COMMENT '片长（分钟）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 演员表
CREATE TABLE actors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '姓名',
  birth_year INT COMMENT '出生年份',
  gender ENUM('男', '女', '其他') COMMENT '性别',
  nationality VARCHAR(50) DEFAULT '中国' COMMENT '国籍',
  famous_works TEXT COMMENT '代表作',
  awards_count INT DEFAULT 0 COMMENT '获奖次数',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 电影演员关联表
CREATE TABLE movie_actors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  movie_id INT NOT NULL,
  actor_id INT NOT NULL,
  role VARCHAR(100) COMMENT '角色名',
  is_lead BOOLEAN DEFAULT FALSE COMMENT '是否主演',
  FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
  FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 影院表
CREATE TABLE cinemas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL COMMENT '影院名称',
  city VARCHAR(50) NOT NULL COMMENT '城市',
  district VARCHAR(50) COMMENT '区域',
  address TEXT COMMENT '详细地址',
  screen_count INT DEFAULT 0 COMMENT '影厅数量',
  seat_capacity INT DEFAULT 0 COMMENT '座位总数',
  rating DECIMAL(2, 1) COMMENT '影院评分',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 观影记录表
CREATE TABLE viewing_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  movie_id INT NOT NULL,
  cinema_id INT NOT NULL,
  view_date DATE NOT NULL COMMENT '观影日期',
  viewer_count INT DEFAULT 1 COMMENT '观影人数',
  ticket_price DECIMAL(6, 2) COMMENT '票价（元）',
  user_rating INT COMMENT '用户评分（1-10）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
  FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入电影数据
INSERT INTO movies (title, director, release_year, genre, rating, box_office, description, duration) VALUES
('流浪地球', '郭帆', 2019, '科幻', 7.9, 468700, '太阳即将毁灭，人类在地球表面建造出巨大的推进器，寻找新家园。', 125),
('战狼2', '吴京', 2017, '动作', 7.2, 567000, '冷锋被卷入了一场非洲国家的叛乱，本可以安全撤离，却孤身犯险冲回沦陷区，带领身陷屠杀中的同胞和难民展开生死逃亡。', 123),
('哪吒之魔童降世', '饺子', 2019, '动画', 8.5, 501700, '"我命由我不由天"，是魔是仙，全在自己。', 110),
('长津湖', '陈凯歌/徐克/林超贤', 2021, '战争', 7.4, 577900, '抗美援朝，长津湖战役，志愿军战士在极寒天气下顽强战斗。', 176),
('你好，李焕英', '贾玲', 2021, '喜剧', 8.1, 541300, '2001年的某一天，刚刚考上大学的女孩贾晓玲经历了人生中的一次大起大落。', 128),
('满江红', '张艺谋', 2023, '悬疑', 7.5, 454400, '南宋绍兴年间，岳飞死后四年，秦桧率兵与金国会谈。', 159),
('红海行动', '林超贤', 2018, '战争', 8.3, 365100, '中国海军"蛟龙突击队"8人小组奉命执行撤侨任务。', 138),
('唐人街探案3', '陈思诚', 2021, '喜剧', 5.6, 451200, '唐人街神探唐仁、秦风受侦探野田昊邀请前往东京，帮助当地警方破案。', 136),
('流浪地球2', '郭帆', 2023, '科幻', 8.3, 404300, '太阳危机即将来临，世界陷入一片恐慌之中，人类建造巨型发动机推动地球逃离太阳系。', 173),
('我和我的祖国', '陈凯歌/张一白/管虎/薛晓路/徐峥/宁浩/文牧野', 2019, '剧情', 8.0, 319700, '7位导演分别取材新中国成立70周年以来，祖国经历的无数个历史性经典瞬间。', 158);

-- 插入演员数据
INSERT INTO actors (name, birth_year, gender, nationality, famous_works, awards_count) VALUES
('吴京', 1974, '男', '中国', '战狼系列、流浪地球系列', 15),
('屈楚萧', 1991, '男', '中国', '流浪地球系列', 3),
('李光洁', 1981, '男', '中国', '流浪地球系列、走向共和', 8),
('饺子', 1980, '男', '中国', '哪吒之魔童降世、打，打个大西瓜', 12),
('贾玲', 1982, '女', '中国', '你好，李焕英、热辣滚烫', 5),
('沈腾', 1979, '男', '中国', '夏洛特烦恼、西虹市首富', 18),
('张译', 1978, '男', '中国', '长津湖、八佰', 22),
('朱一龙', 1988, '男', '中国', '人生大事、峰爆', 9),
('易烊千玺', 2000, '男', '中国', '长津湖、送你一朵小红花', 14),
('刘德华', 1961, '男', '中国香港', '无间道、流浪地球2', 85),
('王宝强', 1984, '男', '中国', '唐人街探案系列、人在囧途', 11),
('刘昊然', 1997, '男', '中国', '唐人街探案系列、九州缥缈录', 7);

-- 插入电影演员关联
INSERT INTO movie_actors (movie_id, actor_id, role, is_lead) VALUES
(1, 1, '刘培强', TRUE),
(1, 2, '刘启', TRUE),
(1, 3, '李一一', FALSE),
(2, 1, '冷锋', TRUE),
(5, 5, '贾晓玲', TRUE),
(4, 7, '伍千里', TRUE),
(4, 9, '伍万里', TRUE),
(9, 1, '刘培强', TRUE),
(9, 10, '周喆直', TRUE),
(8, 11, '唐仁', TRUE),
(8, 12, '秦风', TRUE);

-- 插入影院数据
INSERT INTO cinemas (name, city, district, address, screen_count, seat_capacity, rating) VALUES
('万达影城（CBD店）', '北京', '朝阳区', '建国路万达广场3层', 12, 1800, 9.2),
('CGV影城（正大广场店）', '上海', '浦东新区', '陆家嘴西路168号正大广场8楼', 10, 1500, 9.0),
('金逸影城（天河城店）', '广州', '天河区', '天河路208号天河城商场9楼', 8, 1200, 8.8),
('星美国际影城（海岸城店）', '深圳', '南山区', '文心五路33号海岸城购物中心东区4楼', 11, 1650, 9.1),
('保利国际影城（天府店）', '成都', '武侯区', '天府二街999号伊藤洋华堂5楼', 9, 1350, 8.9),
('横店电影城（西湖店）', '杭州', '西湖区', '延安路98号银泰百货7-8楼', 7, 1050, 8.7),
('大地影院（奥体中心店）', '南京', '建邺区', '江东中路222号奥体中心4楼', 10, 1500, 9.0),
('UME国际影城（苏州中心店）', '苏州', '工业园区', '钟园路99号苏州中心4楼', 13, 1950, 9.3),
('万达影城（泉城路店）', '济南', '历下区', '泉城路180号万达广场3楼', 8, 1200, 8.6),
('太平洋影城（中山路店）', '青岛', '市南区', '中山路10号100码头3楼', 9, 1350, 8.8);

-- 插入观影记录
INSERT INTO viewing_records (movie_id, cinema_id, view_date, viewer_count, ticket_price, user_rating) VALUES
(1, 1, '2019-02-08', 285, 55.00, 8),
(1, 2, '2019-02-10', 310, 58.00, 9),
(1, 3, '2019-02-12', 245, 52.00, 7),
(2, 4, '2017-08-05', 295, 45.00, 8),
(2, 5, '2017-08-06', 270, 48.00, 7),
(3, 1, '2019-07-28', 320, 50.00, 10),
(3, 6, '2019-07-30', 290, 48.00, 9),
(4, 2, '2021-10-02', 350, 60.00, 8),
(4, 7, '2021-10-03', 330, 58.00, 7),
(5, 3, '2021-02-14', 310, 55.00, 9),
(5, 8, '2021-02-15', 295, 52.00, 8),
(6, 4, '2023-01-25', 280, 60.00, 8),
(7, 5, '2018-02-18', 265, 50.00, 9),
(8, 9, '2021-02-13', 340, 58.00, 6),
(9, 10, '2023-01-28', 315, 62.00, 9),
(10, 1, '2019-10-02', 305, 55.00, 8);
