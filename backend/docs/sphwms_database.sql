-- =====================================================================
--  SPHWMS - Smart Post-Harvest Warehouse Management System
--  MySQL 8.0 schema + sample data
--
--  HOW TO USE (MySQL Workbench):
--    1. File > Open SQL Script... > choose this file
--    2. Click the lightning icon (Execute) to run the whole script
--    3. Refresh the SCHEMAS panel (left side) and open "sphwms"
--    4. To see the ER diagram: Database > Reverse Engineer... > sphwms
--
--  WARNING: the first line DROPS the database "sphwms" if it exists,
--           so running this script again resets everything.
--  Sample login (password is stored as BCrypt hash):
--    manager / <your-password>    admin / <your-password>    driver1 / <your-password>
-- =====================================================================

DROP DATABASE IF EXISTS sphwms;
CREATE DATABASE sphwms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sphwms;

-- ---------------------------------------------------------------------
-- 1. LOGIN AND USERS
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
  user_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  username             VARCHAR(50)  NOT NULL UNIQUE,
  password_hash        VARCHAR(100) NOT NULL,            -- BCrypt, never plain text
  full_name            VARCHAR(100) NOT NULL,
  email                VARCHAR(120) UNIQUE,
  phone                VARCHAR(15),
  picture_path         VARCHAR(255),
  role                 ENUM('ADMIN','MANAGER','DRIVER') NOT NULL,
  status               ENUM('ACTIVE','LOCKED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  failed_attempts      INT NOT NULL DEFAULT 0,           -- 5 wrong passwords = LOCKED
  last_login           DATETIME NULL,
  password_changed_at  DATETIME NULL,
  must_change_password TINYINT(1) NOT NULL DEFAULT 0,
  created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE password_reset_token (
  token_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  token_hash VARCHAR(100) NOT NULL,
  expires_at DATETIME NOT NULL,                          -- e.g. 15 minutes
  used       TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE audit_log (
  log_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NULL,
  action      VARCHAR(50)  NOT NULL,                     -- LOGIN, PRODUCT_ADD, ARRIVAL_CONFIRM ...
  entity_type VARCHAR(50)  NULL,
  entity_id   BIGINT NULL,
  details     VARCHAR(500) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  INDEX idx_audit_time (created_at),
  INDEX idx_audit_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. WAREHOUSES AND PRODUCTS
-- ---------------------------------------------------------------------
CREATE TABLE warehouse (
  warehouse_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code_number  INT NOT NULL UNIQUE,                      -- 1, 2, 3
  code         VARCHAR(10)  NOT NULL UNIQUE,             -- WH-01
  name         VARCHAR(100) NOT NULL UNIQUE,             -- Warehouse A
  location     VARCHAR(150) NOT NULL,
  in_charge    VARCHAR(100) NOT NULL,
  capacity_kg  DECIMAL(12,2) NOT NULL,
  active       TINYINT(1) NOT NULL DEFAULT 1,            -- no delete, only inactive
  created_by   BIGINT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   BIGINT NULL,
  updated_at   DATETIME NULL,
  CONSTRAINT chk_wh_capacity CHECK (capacity_kg > 0),
  CONSTRAINT fk_wh_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  CONSTRAINT fk_wh_updated FOREIGN KEY (updated_by) REFERENCES app_user(user_id),
  INDEX idx_wh_active (active)
) ENGINE=InnoDB;

CREATE TABLE product (
  product_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  code           VARCHAR(10)  NOT NULL UNIQUE,           -- P001
  name           VARCHAR(100) NOT NULL,
  category       ENUM('GRAIN','VEGETABLE','FRUIT','PULSE','OTHERS') NOT NULL,
  unit           VARCHAR(10)  NOT NULL DEFAULT 'kg',
  purchase_price DECIMAL(12,2) NOT NULL,
  selling_price  DECIMAL(12,2) NOT NULL,
  reorder_level  DECIMAL(12,2) NOT NULL DEFAULT 0,       -- Low Stock when stock <= this
  active         TINYINT(1) NOT NULL DEFAULT 1,          -- soft delete
  created_by     BIGINT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NULL,
  CONSTRAINT chk_prod_price CHECK (purchase_price > 0 AND selling_price > 0),
  CONSTRAINT fk_prod_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  INDEX idx_prod_category (category),
  INDEX idx_prod_active (active)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. ARRIVALS, QUALITY CHECK, STOCK
-- ---------------------------------------------------------------------
CREATE TABLE arrival (
  arrival_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no         VARCHAR(20) NOT NULL UNIQUE,          -- B-2026-117
  product_id       BIGINT NOT NULL,
  warehouse_id     BIGINT NOT NULL,
  quantity         DECIMAL(12,2) NOT NULL,
  grade            ENUM('A','B','C') NOT NULL,
  expiry_date      DATE NOT NULL,
  purchase_price   DECIMAL(12,2) NOT NULL,               -- snapshot at arrival time
  total_cost       DECIMAL(14,2) NOT NULL,               -- quantity x purchase_price
  status           ENUM('PENDING','CONFIRMED','REJECTED') NOT NULL DEFAULT 'PENDING',
  rejection_reason VARCHAR(255) NULL,
  created_by       BIGINT NOT NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_by     BIGINT NULL,
  confirmed_at     DATETIME NULL,
  version          INT NOT NULL DEFAULT 0,               -- optimistic lock
  CONSTRAINT chk_arr_qty CHECK (quantity > 0),
  CONSTRAINT fk_arr_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_arr_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  CONSTRAINT fk_arr_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  CONSTRAINT fk_arr_confirmed FOREIGN KEY (confirmed_by) REFERENCES app_user(user_id),
  INDEX idx_arr_status (status),
  INDEX idx_arr_confirmed (confirmed_at, status)
) ENGINE=InnoDB;

CREATE TABLE quality_check (
  check_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  arrival_id     BIGINT NOT NULL UNIQUE,
  packaging_ok   TINYINT(1) NOT NULL,
  no_damage      TINYINT(1) NOT NULL,
  quantity_match TINYINT(1) NOT NULL,
  expiry_valid   TINYINT(1) NOT NULL,
  grade_match    TINYINT(1) NOT NULL,
  checked_by     BIGINT NOT NULL,
  checked_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_qc_arrival FOREIGN KEY (arrival_id) REFERENCES arrival(arrival_id),
  CONSTRAINT fk_qc_user FOREIGN KEY (checked_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

-- One row per confirmed arrival. Stock = SUM(remaining_quantity).
CREATE TABLE stock_batch (
  batch_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  arrival_id         BIGINT NOT NULL UNIQUE,
  product_id         BIGINT NOT NULL,
  warehouse_id       BIGINT NOT NULL,
  initial_quantity   DECIMAL(12,2) NOT NULL,
  remaining_quantity DECIMAL(12,2) NOT NULL,
  unit_cost          DECIMAL(12,2) NOT NULL,             -- arrival purchase price (stock value)
  entry_date         DATE NOT NULL,
  expiry_date        DATE NOT NULL,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_sb_remaining CHECK (remaining_quantity >= 0),
  CONSTRAINT fk_sb_arrival FOREIGN KEY (arrival_id) REFERENCES arrival(arrival_id),
  CONSTRAINT fk_sb_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_sb_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  INDEX idx_sb_product_rem (product_id, remaining_quantity),
  INDEX idx_sb_wh_rem (warehouse_id, remaining_quantity),
  INDEX idx_sb_expiry (expiry_date)
) ENGINE=InnoDB;

CREATE TABLE stock_transaction (
  txn_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id   BIGINT NOT NULL,
  batch_id     BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  txn_type     ENUM('ARRIVAL','SALE','SALE_CANCEL','DONATION','DONATION_CANCEL') NOT NULL,
  quantity     DECIMAL(12,2) NOT NULL,
  ref_type     VARCHAR(20) NULL,                         -- ARRIVAL / SALE / DONATION
  ref_id       BIGINT NULL,
  txn_date     DATETIME NOT NULL,
  user_id      BIGINT NULL,
  CONSTRAINT fk_st_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_st_batch FOREIGN KEY (batch_id) REFERENCES stock_batch(batch_id),
  CONSTRAINT fk_st_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  CONSTRAINT fk_st_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  INDEX idx_st_date (txn_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. BUYERS, SALES, DUE PAYMENTS
-- ---------------------------------------------------------------------
CREATE TABLE buyer (
  buyer_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  phone      VARCHAR(15)  NOT NULL UNIQUE,               -- digits only, 01711123456
  address    VARCHAR(200) NOT NULL,
  buyer_type ENUM('RETAILER','WHOLESALER','HOTEL_RESTAURANT','OTHERS') NULL,
  status     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_buyer_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  INDEX idx_buyer_name (name)
) ENGINE=InnoDB;

CREATE TABLE sale (
  sale_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_no        VARCHAR(20) NOT NULL UNIQUE,            -- S-0421
  buyer_id       BIGINT NOT NULL,
  product_id     BIGINT NOT NULL,
  quantity       DECIMAL(12,2) NOT NULL,
  rate           DECIMAL(12,2) NOT NULL,                 -- selling price at sale time
  purchase_price DECIMAL(12,2) NOT NULL,                 -- for profit report
  total_amount   DECIMAL(14,2) NOT NULL,
  paid_amount    DECIMAL(14,2) NOT NULL DEFAULT 0,
  payment_status ENUM('PAID','DUE') NOT NULL,
  status         ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
  cancel_reason  VARCHAR(255) NULL,
  sale_date      DATE NOT NULL,
  due_date       DATE NULL,                              -- Due Payment alert after this date
  created_by     BIGINT NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_sale_qty CHECK (quantity > 0),
  CONSTRAINT chk_sale_paid CHECK (paid_amount >= 0 AND paid_amount <= total_amount),
  CONSTRAINT fk_sale_buyer FOREIGN KEY (buyer_id) REFERENCES buyer(buyer_id),
  CONSTRAINT fk_sale_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_sale_user FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  INDEX idx_sale_date (sale_date, status),
  INDEX idx_sale_buyer (buyer_id)
) ENGINE=InnoDB;

-- A sale can take stock from more than one batch.
CREATE TABLE sale_allocation (
  alloc_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_id      BIGINT NOT NULL,
  batch_id     BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  quantity     DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_sa_sale FOREIGN KEY (sale_id) REFERENCES sale(sale_id),
  CONSTRAINT fk_sa_batch FOREIGN KEY (batch_id) REFERENCES stock_batch(batch_id),
  CONSTRAINT fk_sa_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  INDEX idx_sa_wh (warehouse_id)
) ENGINE=InnoDB;

CREATE TABLE due_payment (
  payment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id     BIGINT NOT NULL,
  amount       DECIMAL(14,2) NOT NULL,
  payment_date DATE NOT NULL,
  received_by  BIGINT NOT NULL,
  CONSTRAINT chk_dp_amount CHECK (amount > 0),
  CONSTRAINT fk_dp_buyer FOREIGN KEY (buyer_id) REFERENCES buyer(buyer_id),
  CONSTRAINT fk_dp_user FOREIGN KEY (received_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE payment_allocation (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT NOT NULL,
  sale_id    BIGINT NOT NULL,
  amount     DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_pa_payment FOREIGN KEY (payment_id) REFERENCES due_payment(payment_id),
  CONSTRAINT fk_pa_sale FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. DONATIONS
-- ---------------------------------------------------------------------
CREATE TABLE receiver (
  receiver_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  contact_person VARCHAR(100) NULL,
  phone          VARCHAR(15)  NOT NULL,
  address        VARCHAR(200) NOT NULL,
  type           ENUM('NGO','ORPHANAGE','MOSQUE_MADRASA','OTHERS') NOT NULL,
  status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_by     BIGINT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rec_created FOREIGN KEY (created_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

CREATE TABLE donation (
  donation_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  donation_no    VARCHAR(20) NOT NULL UNIQUE,            -- D-0021
  receiver_id    BIGINT NOT NULL,
  batch_id       BIGINT NOT NULL,
  product_id     BIGINT NOT NULL,
  warehouse_id   BIGINT NOT NULL,
  quantity       DECIMAL(12,2) NOT NULL,
  purchase_price DECIMAL(12,2) NOT NULL,                 -- snapshot
  total_value    DECIMAL(14,2) NOT NULL,                 -- quantity x purchase_price
  condition_note VARCHAR(255) NULL,
  safe_confirmed TINYINT(1) NOT NULL,
  status         ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
  cancel_reason  VARCHAR(255) NULL,
  donation_date  DATE NOT NULL,
  donated_by     BIGINT NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_don_qty CHECK (quantity > 0),
  CONSTRAINT chk_don_safe CHECK (safe_confirmed = 1),
  CONSTRAINT fk_don_receiver FOREIGN KEY (receiver_id) REFERENCES receiver(receiver_id),
  CONSTRAINT fk_don_batch FOREIGN KEY (batch_id) REFERENCES stock_batch(batch_id),
  CONSTRAINT fk_don_product FOREIGN KEY (product_id) REFERENCES product(product_id),
  CONSTRAINT fk_don_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  CONSTRAINT fk_don_user FOREIGN KEY (donated_by) REFERENCES app_user(user_id),
  INDEX idx_don_date (donation_date, status),
  INDEX idx_don_receiver (receiver_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. DRIVERS, TRIPS, TRANSPORT COST
-- ---------------------------------------------------------------------
CREATE TABLE driver (
  driver_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT NULL UNIQUE,                     -- driver login account (optional)
  name           VARCHAR(100) NOT NULL,
  license_no     VARCHAR(30)  NOT NULL UNIQUE,
  license_expiry DATE NULL,                              -- NULL = no license alert
  phone          VARCHAR(11)  NOT NULL UNIQUE,           -- 11 digits, no dash
  vehicle_type   ENUM('TRUCK','PICKUP','VAN') NOT NULL,
  vehicle_no     VARCHAR(30)  NOT NULL,
  picture_path   VARCHAR(255) NULL,
  active         TINYINT(1) NOT NULL DEFAULT 1,          -- 1 Available, 0 Not Available
  created_by     BIGINT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by     BIGINT NULL,
  updated_at     DATETIME NULL,
  CONSTRAINT fk_drv_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT fk_drv_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  CONSTRAINT fk_drv_updated FOREIGN KEY (updated_by) REFERENCES app_user(user_id),
  INDEX idx_drv_license (license_expiry)
) ENGINE=InnoDB;

CREATE TABLE trip (
  trip_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_no      VARCHAR(20) NOT NULL UNIQUE,              -- T-032
  driver_id    BIGINT NOT NULL,
  origin       VARCHAR(100) NOT NULL,
  destination  VARCHAR(100) NOT NULL,
  trip_date    DATE NOT NULL,
  status       ENUM('PLANNED','ONGOING','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PLANNED',
  warehouse_id BIGINT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trip_driver FOREIGN KEY (driver_id) REFERENCES driver(driver_id),
  CONSTRAINT fk_trip_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  INDEX idx_trip_status (status, trip_date)
) ENGINE=InnoDB;

CREATE TABLE trip_cost (
  trip_cost_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_id      BIGINT NOT NULL UNIQUE,                   -- one cost row per trip
  fuel_cost    DECIMAL(12,2) NOT NULL DEFAULT 0,
  driver_cost  DECIMAL(12,2) NOT NULL DEFAULT 0,
  other_cost   DECIMAL(12,2) NOT NULL DEFAULT 0,
  total_cost   DECIMAL(14,2) NOT NULL,                   -- backend: fuel + driver + other
  note         VARCHAR(255) NULL,
  created_by   BIGINT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by   BIGINT NULL,
  updated_at   DATETIME NULL,
  CONSTRAINT chk_tc_costs CHECK (fuel_cost >= 0 AND driver_cost >= 0 AND other_cost >= 0),
  CONSTRAINT fk_tc_trip FOREIGN KEY (trip_id) REFERENCES trip(trip_id),
  CONSTRAINT fk_tc_created FOREIGN KEY (created_by) REFERENCES app_user(user_id),
  CONSTRAINT fk_tc_updated FOREIGN KEY (updated_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. ALERTS AND SETTINGS, COUNTERS, EXPORT LOG
-- ---------------------------------------------------------------------
CREATE TABLE alert (
  alert_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  type         ENUM('LOW_STOCK','NEAR_EXPIRY','DUE_PAYMENT','LICENSE_EXPIRY') NOT NULL,
  entity_type  ENUM('PRODUCT','BATCH','BUYER','DRIVER') NOT NULL,
  entity_id    BIGINT NOT NULL,
  warehouse_id BIGINT NULL,                              -- NULL for due payment / license
  item_name    VARCHAR(150) NOT NULL,                    -- for search
  details      VARCHAR(255) NOT NULL,
  dedupe_key   VARCHAR(100) NOT NULL UNIQUE,             -- LOW_STOCK:productId:warehouseId ...
  is_read      TINYINT(1) NOT NULL DEFAULT 0,
  read_at      DATETIME NULL,
  read_by      BIGINT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at  DATETIME NULL,                            -- NULL = still active
  CONSTRAINT fk_alert_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  CONSTRAINT fk_alert_user FOREIGN KEY (read_by) REFERENCES app_user(user_id),
  INDEX idx_alert_state (resolved_at, is_read),
  INDEX idx_alert_type (type)
) ENGINE=InnoDB;

CREATE TABLE alert_setting (
  setting_id          INT PRIMARY KEY,                   -- always 1 (single row)
  near_expiry_days    INT NOT NULL DEFAULT 7,
  donation_days       INT NOT NULL DEFAULT 2,            -- 1-2 days to expiry
  license_expiry_days INT NOT NULL DEFAULT 30,
  due_reminder_days   INT NOT NULL DEFAULT 15,
  low_stock_enabled   TINYINT(1) NOT NULL DEFAULT 1,
  near_expiry_enabled TINYINT(1) NOT NULL DEFAULT 1,
  due_enabled         TINYINT(1) NOT NULL DEFAULT 1,
  license_enabled     TINYINT(1) NOT NULL DEFAULT 1,
  updated_by          BIGINT NULL,
  updated_at          DATETIME NULL,
  CONSTRAINT fk_as_user FOREIGN KEY (updated_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

-- Safe number generator: P001, B-2026-110, S-0421, D-0022, T-034, WH-04
CREATE TABLE id_sequence (
  seq_name VARCHAR(30) PRIMARY KEY,
  next_val BIGINT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE report_export_log (
  export_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_type  VARCHAR(40) NOT NULL,
  period_from  DATE NULL,
  period_to    DATE NULL,
  warehouse_id BIGINT NULL,
  exported_by  BIGINT NOT NULL,
  exported_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rel_wh FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  CONSTRAINT fk_rel_user FOREIGN KEY (exported_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. HANDY VIEWS (read only, for checking data in Workbench)
-- ---------------------------------------------------------------------
CREATE VIEW v_stock_by_warehouse AS
SELECT w.name AS warehouse, p.code, p.name AS product,
       SUM(b.remaining_quantity) AS stock_kg,
       SUM(b.remaining_quantity * b.unit_cost) AS stock_value,
       MIN(CASE WHEN b.remaining_quantity > 0 THEN b.expiry_date END) AS nearest_expiry
FROM stock_batch b
JOIN product p ON p.product_id = b.product_id
JOIN warehouse w ON w.warehouse_id = b.warehouse_id
GROUP BY w.name, p.code, p.name;

CREATE VIEW v_buyer_summary AS
SELECT b.buyer_id, b.name,
       COALESCE(SUM(s.total_amount), 0) AS total_purchase,
       COALESCE(SUM(s.paid_amount), 0)  AS total_paid,
       COALESCE(SUM(s.total_amount - s.paid_amount), 0) AS total_due,
       MAX(s.sale_date) AS last_purchase
FROM buyer b
LEFT JOIN sale s ON s.buyer_id = b.buyer_id AND s.status = 'CONFIRMED'
GROUP BY b.buyer_id, b.name;

-- =====================================================================
--  SAMPLE DATA (matches the UI design; delete this part for real data)
-- =====================================================================
INSERT INTO app_user (user_id, username, password_hash, full_name, email, phone, role) VALUES
 (1, 'manager', '$2b$10$pQa7EAA8E9Fj2eXJrA4V9Ou1TJpbtZD4RQfDVZU.fGXG8UZKOPNHm', 'Rahim Uddin',  'rahim@sphwms.test',  '01700000001', 'MANAGER'),
 (2, 'admin',   '$2b$10$dVaQn/uAweVWuXNczK05dOwhQ9m.xCfLoz/xIpKAdXGv52AK/13li', 'System Admin', 'admin@sphwms.test',  '01700000002', 'ADMIN'),
 (3, 'driver1', '$2b$10$MHy2q3YFCYGy2kY8lCKZIO0gigHLbBDh0V.nGxvH620jmwsp10B5m', 'Karim Uddin',  'karim@sphwms.test',  '01711000001', 'DRIVER');

INSERT INTO warehouse (warehouse_id, code_number, code, name, location, in_charge, capacity_kg, created_by) VALUES
 (1, 1, 'WH-01', 'Warehouse A', 'Gazipur',      'Abdul Karim',  12000, 1),
 (2, 2, 'WH-02', 'Warehouse B', 'Narayanganj',  'Salma Begum',   8000, 1),
 (3, 3, 'WH-03', 'Warehouse C', 'Savar',        'Mizan Rahman',  5000, 1);

INSERT INTO product (product_id, code, name, category, purchase_price, selling_price, reorder_level, created_by) VALUES
 (1, 'P001', 'Rice (Miniket)',  'GRAIN',     52, 60,  500, 1),
 (2, 'P002', 'Tomato',          'VEGETABLE', 18, 26,  100, 1),
 (3, 'P003', 'Lentil (Masur)',  'PULSE',     95, 110,  50, 1),
 (4, 'P004', 'Mango',           'FRUIT',     70, 95,   50, 1),
 (5, 'P005', 'Potato',          'VEGETABLE', 22, 30,  300, 1),
 (6, 'P006', 'Banana',          'FRUIT',     30, 45,   50, 1);

INSERT INTO arrival (arrival_id, batch_no, product_id, warehouse_id, quantity, grade, expiry_date, purchase_price, total_cost,
                     status, rejection_reason, created_by, created_at, confirmed_by, confirmed_at) VALUES
 (1, 'B-2026-101', 1, 1, 1050, 'A', '2027-10-28', 52, 54600,  'CONFIRMED', NULL, 1, '2026-10-02 09:00:00', 1, '2026-10-02 11:00:00'),
 (2, 'B-2026-102', 1, 2,  300, 'A', '2027-10-28', 52, 15600,  'CONFIRMED', NULL, 1, '2026-10-02 09:10:00', 1, '2026-10-02 11:10:00'),
 (3, 'B-2026-103', 2, 2,  270, 'B', '2026-11-23', 18, 4860,   'CONFIRMED', NULL, 1, '2026-11-01 08:30:00', 1, '2026-11-01 10:00:00'),
 (4, 'B-2026-104', 3, 1,  140, 'A', '2027-05-21', 95, 13300,  'CONFIRMED', NULL, 1, '2026-10-08 09:00:00', 1, '2026-10-08 10:30:00'),
 (5, 'B-2026-105', 4, 1,   70, 'A', '2026-11-19', 70, 4900,   'CONFIRMED', NULL, 1, '2026-10-23 09:00:00', 1, '2026-10-23 10:00:00'),
 (6, 'B-2026-106', 5, 1, 3270, 'A', '2027-02-05', 22, 71940,  'CONFIRMED', NULL, 1, '2026-10-15 09:00:00', 1, '2026-10-15 12:00:00'),
 (7, 'B-2026-107', 6, 1,  180, 'B', '2026-11-24', 30, 5400,   'CONFIRMED', NULL, 1, '2026-10-12 08:00:00', 1, '2026-10-12 09:00:00'),
 (8, 'B-2026-108', 1, 1,  500, 'A', '2027-10-27', 52, 26000,  'PENDING',   NULL, 1, '2026-11-21 10:00:00', NULL, NULL),
 (9, 'B-2026-109', 3, 1,  150, 'B', '2027-05-20', 95, 14250,  'REJECTED',  'Moisture and insects found', 1, '2026-11-18 10:00:00', NULL, NULL);

INSERT INTO quality_check (arrival_id, packaging_ok, no_damage, quantity_match, expiry_valid, grade_match, checked_by, checked_at)
SELECT arrival_id, 1, 1, 1, 1, 1, 1, confirmed_at FROM arrival WHERE status = 'CONFIRMED';
INSERT INTO quality_check (arrival_id, packaging_ok, no_damage, quantity_match, expiry_valid, grade_match, checked_by, checked_at)
VALUES (9, 1, 0, 1, 1, 0, 1, '2026-11-18 11:00:00');

-- remaining = arrived - sold - donated
INSERT INTO stock_batch (batch_id, arrival_id, product_id, warehouse_id, initial_quantity, remaining_quantity, unit_cost, entry_date, expiry_date) VALUES
 (1, 1, 1, 1, 1050,  900, 52, '2026-10-02', '2027-10-28'),
 (2, 2, 1, 2,  300,  300, 52, '2026-10-02', '2027-10-28'),
 (3, 3, 2, 2,  270,  120, 18, '2026-11-01', '2026-11-23'),
 (4, 4, 3, 1,  140,   40, 95, '2026-10-08', '2027-05-21'),
 (5, 5, 4, 1,   70,    0, 70, '2026-10-23', '2026-11-19'),
 (6, 6, 5, 1, 3270, 2000, 22, '2026-10-15', '2027-02-05'),
 (7, 7, 6, 1,  180,   60, 30, '2026-10-12', '2026-11-24');

INSERT INTO buyer (buyer_id, name, phone, address, buyer_type, created_by) VALUES
 (1, 'Sumon Traders',          '01711123456', 'Kawran Bazar, Dhaka', 'WHOLESALER',       1),
 (2, 'Rahman Store',           '01811234567', 'Mirpur, Dhaka',       'RETAILER',         1),
 (3, 'Green Valley Restaurant','01911345678', 'Gulshan, Dhaka',      'HOTEL_RESTAURANT', 1);

INSERT INTO sale (sale_id, sale_no, buyer_id, product_id, quantity, rate, purchase_price, total_amount, paid_amount,
                  payment_status, sale_date, due_date, created_by) VALUES
 (1, 'S-0377', 1, 3,  100, 110, 95, 11000,  5000, 'DUE',  '2026-10-28', '2026-11-12', 1),
 (2, 'S-0390', 2, 4,   70,  95, 70,  6650,  6650, 'PAID', '2026-10-25', NULL,         1),
 (3, 'S-0398', 1, 5, 1000,  30, 22, 30000, 30000, 'PAID', '2026-11-09', NULL,         1),
 (4, 'S-0412', 1, 1,  100,  60, 52,  6000,     0, 'DUE',  '2026-11-18', '2026-12-03', 1);

INSERT INTO sale_allocation (sale_id, batch_id, warehouse_id, quantity) VALUES
 (1, 4, 1, 100), (2, 5, 1, 70), (3, 6, 1, 1000), (4, 1, 1, 100);

INSERT INTO due_payment (payment_id, buyer_id, amount, payment_date, received_by) VALUES (1, 1, 5000, '2026-11-12', 1);
INSERT INTO payment_allocation (payment_id, sale_id, amount) VALUES (1, 1, 5000);

INSERT INTO receiver (receiver_id, name, contact_person, phone, address, type, created_by) VALUES
 (1, 'Hope Orphanage',  'Nasrin Akter', '01811000111', 'Uttara, Dhaka',  'ORPHANAGE',      1),
 (2, 'Care NGO',        'Rashed Khan',  '01911000222', 'Mirpur, Dhaka',  'NGO',            1),
 (3, 'Al-Amin Madrasa', 'Hafez Karim',  '01711000333', 'Tongi, Gazipur', 'MOSQUE_MADRASA', 1);

INSERT INTO donation (donation_id, donation_no, receiver_id, batch_id, product_id, warehouse_id, quantity, purchase_price,
                      total_value, condition_note, safe_confirmed, donation_date, donated_by) VALUES
 (1, 'D-0015', 1, 7, 6, 1,  40, 30, 1200, 'Safe to eat, packaging ok', 1, '2026-10-12', 1),
 (2, 'D-0016', 3, 6, 5, 1,  70, 22, 1540, 'Safe to eat, packaging ok', 1, '2026-10-20', 1),
 (3, 'D-0017', 3, 1, 1, 1,  50, 52, 2600, 'Safe to eat, packaging ok', 1, '2026-10-28', 1),
 (4, 'D-0018', 2, 3, 2, 2,  60, 18, 1080, 'Safe to eat, packaging ok', 1, '2026-11-03', 1),
 (5, 'D-0019', 1, 3, 2, 2,  90, 18, 1620, 'Safe to eat, packaging ok', 1, '2026-11-08', 1),
 (6, 'D-0020', 2, 7, 6, 1,  80, 30, 2400, 'Safe to eat, packaging ok', 1, '2026-11-14', 1),
 (7, 'D-0021', 1, 6, 5, 1, 200, 22, 4400, 'Safe to eat, packaging ok', 1, '2026-11-19', 1);

-- stock history built from the data above
INSERT INTO stock_transaction (product_id, batch_id, warehouse_id, txn_type, quantity, ref_type, ref_id, txn_date, user_id)
SELECT b.product_id, b.batch_id, b.warehouse_id, 'ARRIVAL', a.quantity, 'ARRIVAL', a.arrival_id, a.confirmed_at, a.confirmed_by
FROM stock_batch b JOIN arrival a ON a.arrival_id = b.arrival_id;
INSERT INTO stock_transaction (product_id, batch_id, warehouse_id, txn_type, quantity, ref_type, ref_id, txn_date, user_id)
SELECT s.product_id, sa.batch_id, sa.warehouse_id, 'SALE', sa.quantity, 'SALE', s.sale_id, s.sale_date, s.created_by
FROM sale_allocation sa JOIN sale s ON s.sale_id = sa.sale_id;
INSERT INTO stock_transaction (product_id, batch_id, warehouse_id, txn_type, quantity, ref_type, ref_id, txn_date, user_id)
SELECT product_id, batch_id, warehouse_id, 'DONATION', quantity, 'DONATION', donation_id, donation_date, donated_by FROM donation;

INSERT INTO driver (driver_id, user_id, name, license_no, license_expiry, phone, vehicle_type, vehicle_no, active, created_by) VALUES
 (1, 3,    'Karim Uddin',   'DL-482910', '2026-12-04', '01711000001', 'TRUCK',  'DH-11-2201', 1, 1),
 (2, NULL, 'Jamal Hossain', 'DL-482977', '2027-03-15', '01711000002', 'PICKUP', 'DH-14-5532', 0, 1),
 (3, NULL, 'Rafiq Mia',     'DL-483015', '2027-06-30', '01711000003', 'TRUCK',  'CH-12-7710', 1, 1),
 (4, NULL, 'Sohel Rana',    'DL-483120', '2027-01-20', '01711000004', 'VAN',    'DH-15-4408', 1, 1),
 (5, NULL, 'Nasir Ahmed',   'DL-483244', NULL,         '01711000005', 'TRUCK',  'DH-11-9032', 0, 1),
 (6, NULL, 'Habib Khan',    'DL-483301', '2027-09-10', '01711000006', 'PICKUP', 'CH-13-1180', 1, 1);

INSERT INTO trip (trip_id, trip_no, driver_id, origin, destination, trip_date, status, warehouse_id) VALUES
 (1, 'T-029', 6, 'Narayanganj', 'Dhaka',   '2026-11-05', 'DELIVERED', 2),
 (2, 'T-030', 3, 'Savar',       'Dhaka',   '2026-11-11', 'DELIVERED', 3),
 (3, 'T-031', 1, 'Dhaka',       'Gazipur', '2026-11-15', 'DELIVERED', 1),
 (4, 'T-032', 4, 'Gazipur',     'Dhaka',   '2026-11-20', 'DELIVERED', 1),
 (5, 'T-033', 5, 'Dhaka',       'Savar',   '2026-11-22', 'ONGOING',   3);

INSERT INTO trip_cost (trip_id, fuel_cost, driver_cost, other_cost, total_cost, note, created_by) VALUES
 (2, 2100, 1200,   0, 3300, NULL, 1),
 (3, 3200, 1500, 300, 5000, 'Toll included', 1);

INSERT INTO alert_setting (setting_id, updated_by, updated_at) VALUES (1, 1, NOW());

INSERT INTO id_sequence (seq_name, next_val) VALUES
 ('PRODUCT_CODE', 7), ('BATCH_NO', 110), ('SALE_NO', 421), ('DONATION_NO', 22), ('TRIP_NO', 34), ('WAREHOUSE_CODE', 4);

-- alert table is filled by the backend job (every 15 minutes), so no sample rows here.

-- Quick checks (select and run any line):
-- SELECT * FROM v_stock_by_warehouse;
-- SELECT * FROM v_buyer_summary;
-- SELECT status, COUNT(*) FROM arrival GROUP BY status;
