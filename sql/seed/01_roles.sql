-- ВАЖНО: Роли также создаются приложением через RoleRunner (CommandLineRunner).
-- Этот скрипт нужен для ручного наполнения БД без запуска приложения.
-- Имена ролей ДОЛЖНЫ совпадать с RoleRunner.

INSERT INTO t_role (name, description)
VALUES
('ROLE_EMPLOYEE_ADMIN', 'Администратор'),
('ROLE_EMPLOYEE_MANAGER', 'Руководитель'),
('ROLE_EMPLOYEE_WAREHOUSE_MANAGER', 'Заведующий складом'),
('ROLE_EMPLOYEE_WAREHOUSE_WORKER', 'Складской работник'),
('ROLE_EMPLOYEE_COURIER', 'Водитель экспедитор'),
('ROLE_EMPLOYEE_ACCOUNTER', 'Бухгалтер');
