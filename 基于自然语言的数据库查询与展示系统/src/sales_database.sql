-- ====================================
-- 中文销售管理系统测试数据库
-- 包含：客户、产品、订单、订单明细
-- ====================================

CREATE DATABASE IF NOT EXISTS sales_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sales_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 客户表
CREATE TABLE customers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '客户姓名',
  city VARCHAR(50) COMMENT '所在城市',
  phone VARCHAR(20) COMMENT '联系电话',
  level VARCHAR(20) DEFAULT '普通客户' COMMENT '客户等级：普通、黄金、白金',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 产品表
CREATE TABLE products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL COMMENT '产品名称',
  category VARCHAR(50) COMMENT '产品类别',
  price DECIMAL(10, 2) NOT NULL COMMENT '单价',
  stock INT DEFAULT 0 COMMENT '库存量',
  status VARCHAR(20) DEFAULT '在售' COMMENT '状态：在售、下架'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 订单表
CREATE TABLE orders (
  id INT AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  order_date DATE NOT NULL COMMENT '下单日期',
  total_amount DECIMAL(12, 2) COMMENT '订单总金额',
  status VARCHAR(20) DEFAULT '已支付' COMMENT '状态：待支付、已支付、已发货、已完成',
  FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 订单明细表
CREATE TABLE order_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  product_id INT NOT NULL,
  quantity INT NOT NULL COMMENT '购买数量',
  unit_price DECIMAL(10, 2) NOT NULL COMMENT '购买时单价',
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入客户数据
INSERT INTO customers (name, city, phone, level) VALUES
('张伟', '北京', '13800138001', '白金客户'),
('李娜', '上海', '13900139002', '黄金客户'),
('王五', '广州', '13700137003', '普通客户'),
('赵六', '深圳', '13600136004', '黄金客户'),
('孙悟空', '杭州', '13500135005', '白金客户'),
('林黛玉', '苏州', '13400134006', '普通客户');

-- 插入产品数据
INSERT INTO products (name, category, price, stock) VALUES
('华为 Mate 60 Pro', '手机', 6999.00, 100),
('小米 14', '手机', 3999.00, 200),
('MacBook Air M3', '电脑', 8999.00, 50),
('联想拯救者 Y9000P', '电脑', 9999.00, 30),
('索尼 WH-1000XM5', '耳机', 2499.00, 80),
('任天堂 Switch', '游戏机', 2299.00, 150);

-- 插入订单数据
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(1, '2023-10-01', 15998.00, '已完成'),
(2, '2023-10-05', 3999.00, '已支付'),
(3, '2023-10-10', 2499.00, '已发货'),
(5, '2023-10-15', 18998.00, '已完成');

-- 插入订单明细
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 6999.00),
(1, 3, 1, 8999.00),
(2, 2, 1, 3999.00),
(3, 5, 1, 2499.00),
(4, 4, 1, 9999.00),
(4, 3, 1, 8999.00);


