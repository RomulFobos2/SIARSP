-- SIARSP / MySQL 8.4
-- Надежная полная очистка всех таблиц проекта.
-- Почему DELETE, а не TRUNCATE:
-- В MySQL/InnoDB TRUNCATE может падать на таблицах, участвующих в FK,
-- даже если дочерние таблицы уже очищены.
-- Поэтому используем DELETE + сброс AUTO_INCREMENT.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM category_attribute;
DELETE FROM t_zone_product;
DELETE FROM t_requested_product;
DELETE FROM t_ordered_product;
DELETE FROM t_product_attribute_value;
DELETE FROM t_storage_zone;
DELETE FROM t_shelf;
DELETE FROM t_warehouse_equipment;
DELETE FROM t_product;
DELETE FROM t_product_category;
DELETE FROM t_product_attribute;
DELETE FROM t_global_product_category;
DELETE FROM t_warehouse;
DELETE FROM t_equipment_type;
DELETE FROM t_vehicle;
DELETE FROM t_employee;
DELETE FROM t_role;
DELETE FROM t_client;
DELETE FROM t_supplier;

DELETE FROM t_delivery_task;
DELETE FROM t_route_point;
DELETE FROM t_ttn;
DELETE FROM t_delivery;
DELETE FROM t_supply;
DELETE FROM t_request_for_delivery;
DELETE FROM t_client_order;
DELETE FROM t_acceptance_act;
DELETE FROM t_write_off_act;
DELETE FROM t_comment;
DELETE FROM t_notification;

ALTER TABLE t_zone_product AUTO_INCREMENT = 1;
ALTER TABLE t_requested_product AUTO_INCREMENT = 1;
ALTER TABLE t_ordered_product AUTO_INCREMENT = 1;
ALTER TABLE t_product_attribute_value AUTO_INCREMENT = 1;
ALTER TABLE t_storage_zone AUTO_INCREMENT = 1;
ALTER TABLE t_shelf AUTO_INCREMENT = 1;
ALTER TABLE t_warehouse_equipment AUTO_INCREMENT = 1;
ALTER TABLE t_product AUTO_INCREMENT = 1;
ALTER TABLE t_product_category AUTO_INCREMENT = 1;
ALTER TABLE t_product_attribute AUTO_INCREMENT = 1;
ALTER TABLE t_global_product_category AUTO_INCREMENT = 1;
ALTER TABLE t_warehouse AUTO_INCREMENT = 1;
ALTER TABLE t_equipment_type AUTO_INCREMENT = 1;
ALTER TABLE t_vehicle AUTO_INCREMENT = 1;
ALTER TABLE t_employee AUTO_INCREMENT = 1;
ALTER TABLE t_role AUTO_INCREMENT = 1;
ALTER TABLE t_client AUTO_INCREMENT = 1;
ALTER TABLE t_supplier AUTO_INCREMENT = 1;

ALTER TABLE t_delivery_task AUTO_INCREMENT = 1;
ALTER TABLE t_route_point AUTO_INCREMENT = 1;
ALTER TABLE t_ttn AUTO_INCREMENT = 1;
ALTER TABLE t_delivery AUTO_INCREMENT = 1;
ALTER TABLE t_supply AUTO_INCREMENT = 1;
ALTER TABLE t_request_for_delivery AUTO_INCREMENT = 1;
ALTER TABLE t_client_order AUTO_INCREMENT = 1;
ALTER TABLE t_acceptance_act AUTO_INCREMENT = 1;
ALTER TABLE t_write_off_act AUTO_INCREMENT = 1;
ALTER TABLE t_comment AUTO_INCREMENT = 1;
ALTER TABLE t_notification AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
