-- Пароль для всех пользователей:
-- $2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i

INSERT INTO t_employee (
    last_name,
    first_name,
    patronymic_name,
    username,
    password,
    need_change_pass,
    is_active,
    date_of_registration,
    role_id
)
VALUES
('Иванов', 'Иван', 'Иванович', 'admin1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_ADMIN')),
('Петров', 'Пётр', 'Петрович', 'admin2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_ADMIN')),

('Смирнова', 'Анна', 'Олеговна', 'accountant1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_ACCOUNTANT')),
('Кузнецова', 'Мария', 'Сергеевна', 'accountant2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_ACCOUNTANT')),

('Соколов', 'Артём', 'Дмитриевич', 'wm1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_WAREHOUSE_MANAGER')),
('Лебедев', 'Максим', 'Игоревич', 'wm2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_WAREHOUSE_MANAGER')),

('Орлов', 'Николай', 'Андреевич', 'driver1', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_DRIVER')),
('Попов', 'Владимир', 'Евгеньевич', 'driver2', '$2a$10$rksOKdgBnFKbdtmq7P276etXXlPJKlWaSjKftBtQ6x/CwpjhgoW6i', 0, 1, CURDATE(), (SELECT id FROM t_role WHERE name = 'ROLE_DRIVER'));
