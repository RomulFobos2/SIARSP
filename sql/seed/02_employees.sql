-- ВАЖНО:
-- Роли должны быть предварительно созданы приложением через RoleRunner:
-- ROLE_EMPLOYEE_ADMIN
-- ROLE_EMPLOYEE_MANAGER
-- ROLE_EMPLOYEE_WAREHOUSE_MANAGER
-- ROLE_EMPLOYEE_WAREHOUSE_WORKER
-- ROLE_EMPLOYEE_COURIER
-- ROLE_EMPLOYEE_ACCOUNTER
--
-- Пароль для всех пользователей:
-- $2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i

-- ROLE_EMPLOYEE_ADMIN
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Иванов', 'Иван', 'Иванович', 'emp_admin_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_ADMIN'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Петров', 'Пётр', 'Петрович', 'emp_admin_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_ADMIN'));

-- ROLE_EMPLOYEE_MANAGER
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Смирнов', 'Алексей', 'Олегович', 'emp_manager_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_MANAGER'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Кузнецов', 'Дмитрий', 'Сергеевич', 'emp_manager_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_MANAGER'));

-- ROLE_EMPLOYEE_WAREHOUSE_MANAGER
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Соколов', 'Артём', 'Дмитриевич', 'emp_wh_manager_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_WAREHOUSE_MANAGER'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Лебедев', 'Максим', 'Игоревич', 'emp_wh_manager_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_WAREHOUSE_MANAGER'));

-- ROLE_EMPLOYEE_WAREHOUSE_WORKER
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Орехов', 'Никита', 'Владимирович', 'emp_wh_worker_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_WAREHOUSE_WORKER'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Громов', 'Кирилл', 'Алексеевич', 'emp_wh_worker_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_WAREHOUSE_WORKER'));

-- ROLE_EMPLOYEE_COURIER
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Орлов', 'Николай', 'Андреевич', 'emp_courier_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_COURIER'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Попов', 'Владимир', 'Евгеньевич', 'emp_courier_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_COURIER'));

-- ROLE_EMPLOYEE_ACCOUNTER
INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Волкова', 'Анна', 'Олеговна', 'emp_accounter_1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_ACCOUNTER'));

INSERT INTO t_employee (last_name, first_name, patronymic_name, username, password, need_change_pass, is_active, date_of_registration, role_id)
VALUES ('Морозова', 'Мария', 'Сергеевна', 'emp_accounter_2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURRENT_DATE, (SELECT id FROM t_role WHERE name = 'ROLE_EMPLOYEE_ACCOUNTER'));
