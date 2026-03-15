-- SIARSP / MySQL 8.4
-- Полная очистка всех таблиц проекта.
-- Запускайте в выбранной БД SIARSP в phpMyAdmin.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE category_attribute;

TRUNCATE TABLE t_zone_product;
TRUNCATE TABLE t_requested_product;
TRUNCATE TABLE t_ordered_product;
TRUNCATE TABLE t_product_attribute_value;
TRUNCATE TABLE t_storage_zone;
TRUNCATE TABLE t_shelf;
TRUNCATE TABLE t_warehouse_equipment;
TRUNCATE TABLE t_product;
TRUNCATE TABLE t_product_category;
TRUNCATE TABLE t_product_attribute;
TRUNCATE TABLE t_global_product_category;
TRUNCATE TABLE t_warehouse;
TRUNCATE TABLE t_equipment_type;
TRUNCATE TABLE t_vehicle;
TRUNCATE TABLE t_employee;
TRUNCATE TABLE t_role;
TRUNCATE TABLE t_client;
TRUNCATE TABLE t_supplier;

TRUNCATE TABLE t_delivery_task;
TRUNCATE TABLE t_route_point;
TRUNCATE TABLE t_ttn;
TRUNCATE TABLE t_delivery;
TRUNCATE TABLE t_supply;
TRUNCATE TABLE t_request_for_delivery;
TRUNCATE TABLE t_client_order;
TRUNCATE TABLE t_acceptance_act;
TRUNCATE TABLE t_write_off_act;
TRUNCATE TABLE t_comment;
TRUNCATE TABLE t_notification;

SET FOREIGN_KEY_CHECKS = 1;
