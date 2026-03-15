-- SIARSP / MySQL 8.4
-- Полная очистка всех таблиц проекта.
-- Запускайте в выбранной БД SIARSP в phpMyAdmin.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE category_attribute;

TRUNCATE TABLE t_zoneProduct;
TRUNCATE TABLE t_requestedProduct;
TRUNCATE TABLE t_orderedProduct;
TRUNCATE TABLE t_productAttributeValue;
TRUNCATE TABLE t_storageZone;
TRUNCATE TABLE t_shelf;
TRUNCATE TABLE t_warehouseEquipment;
TRUNCATE TABLE t_product;
TRUNCATE TABLE t_productCategory;
TRUNCATE TABLE t_productAttribute;
TRUNCATE TABLE t_globalProductCategory;
TRUNCATE TABLE t_warehouse;
TRUNCATE TABLE t_equipmentType;
TRUNCATE TABLE t_vehicle;
TRUNCATE TABLE t_employee;
TRUNCATE TABLE t_role;
TRUNCATE TABLE t_client;
TRUNCATE TABLE t_supplier;

TRUNCATE TABLE t_deliveryTask;
TRUNCATE TABLE t_routePoint;
TRUNCATE TABLE t_ttn;
TRUNCATE TABLE t_delivery;
TRUNCATE TABLE t_supply;
TRUNCATE TABLE t_requestForDelivery;
TRUNCATE TABLE t_clientOrder;
TRUNCATE TABLE t_acceptanceAct;
TRUNCATE TABLE t_writeOffAct;
TRUNCATE TABLE t_comment;
TRUNCATE TABLE t_notification;

SET FOREIGN_KEY_CHECKS = 1;
