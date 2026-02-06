# SIARSP — Система Исходящих и Входящих Ресурсов Складского Предприятия

Дипломный проект — ERP-система управления складом и логистикой для ИП "Левчук" (поставки продуктов питания и товаров первой необходимости в детские сады, школы, больницы).

## Стек технологий

- **Java 21**, **Spring Boot 4.0.1**
- **Spring Security 6** — аутентификация и авторизация (BCrypt)
- **Spring Data JPA** / Hibernate — ORM, MySQL
- **Thymeleaf** — серверный рендеринг HTML (+ Bootstrap)
- **MapStruct 1.6.2** — маппинг Entity ↔ DTO
- **Lombok 1.18.36** — `@Data`, `@Slf4j`, `@NoArgsConstructor`, `@EqualsAndHashCode`
- **Apache POI 5.2.3** — генерация Excel
- **Passay 1.6.0** — валидация паролей
- **Spring Mail** — отправка email
- **Maven** — сборка (есть Maven Wrapper)

## Сборка и запуск

```bash
# Сборка
mvnw.cmd clean package

# Запуск
mvnw.cmd spring-boot:run

# Тесты (пока отсутствуют)
mvnw.cmd test
```

**Важно:** `application.properties` находится в `.gitignore` — настройки БД (MySQL) задаются локально.

## Структура проекта

```
src/main/java/com/mai/siarsp/
├── SIARSPApplication.java          — точка входа
├── component/                      — компоненты (RoleRunner, ScheduleTask, RandomPasswordGenerator)
├── controllers/
│   ├── general/                    — публичные контроллеры (MainController, CustomErrorController)
│   └── employee/
│       ├── admin/                  — контроллеры для ROLE_EMPLOYEE_ADMIN
│       └── general/               — контроллеры для всех сотрудников
├── dto/                            — DTO-классы (EmployeeDTO)
├── enumeration/                    — перечисления статусов и типов (12 enum)
├── mapper/                         — MapStruct-мапперы (EmployeeMapper)
├── models/                         — JPA-сущности (26 классов)
├── repo/                           — Spring Data JPA репозитории
├── security/                       — конфигурация Spring Security
└── service/
    ├── employee/                   — сервисы сотрудников (EmployeeService)
    └── general/                    — общие сервисы (MailService)

src/main/resources/
├── logback.xml                     — логирование (консоль + файл logs/siarsp.log)
├── static/                         — CSS, JS, изображения
└── templates/                      — Thymeleaf-шаблоны (blocks, general, employee)
```

## Архитектура

Слоистая MVC-архитектура:

```
Controller → Service → Repository → Entity (JPA)
                         ↕
                    DTO + Mapper (MapStruct)
```

- Контроллеры возвращают имена Thymeleaf-шаблонов (не REST API)
- Сервисы содержат бизнес-логику, помечены `@Service`, `@Transactional`
- Репозитории наследуют `JpaRepository`, содержат кастомные `@Query`

## Доменная модель (26 сущностей)

| Группа | Сущности |
|--------|----------|
| Пользователи | Employee, Role |
| Склад | Warehouse, Shelf, StorageZone, ZoneProduct, WarehouseEquipment |
| Товары | Product, ProductCategory, GlobalProductCategory, ProductAttribute, ProductAttributeValue |
| Поставщики | Supplier, Delivery, Supply, RequestForDelivery, RequestedProduct |
| Заказы клиентов | Client, ClientOrder, OrderedProduct |
| Логистика | Vehicle, DeliveryTask, RoutePoint |
| Документы | TTN, AcceptanceAct, WriteOffAct |

## Роли безопасности

| Роль | Описание |
|------|----------|
| ROLE_EMPLOYEE_ADMIN | Администратор |
| ROLE_EMPLOYEE_MANAGER | Руководитель |
| ROLE_EMPLOYEE_WAREHOUSE_MANAGER | Заведующий складом |
| ROLE_EMPLOYEE_WAREHOUSE_WORKER | Складской работник |
| ROLE_EMPLOYEE_COURIER | Водитель-экспедитор |
| ROLE_EMPLOYEE_ACCOUNTER | Бухгалтер |

`Employee` реализует `UserDetails`, `Role` реализует `GrantedAuthority`. При первом входе требуется смена пароля (`needChangePass`). Роли и админ создаются при старте через `RoleRunner` (CommandLineRunner).

## Соглашения по именованию

- **Классы**: PascalCase — `ClientOrder`, `DeliveryTask`
- **Контроллеры**: `*Controller` — `AdminController`
- **Сервисы**: `*Service` — `EmployeeService`
- **Репозитории**: `*Repository` — `EmployeeRepository`
- **DTO**: `*DTO` — `EmployeeDTO`
- **Мапперы**: `*Mapper` — `EmployeeMapper`
- **Таблицы БД**: префикс `t_` — `t_employee`, `t_clientOrder`
- **Enum-значения**: UPPER_CASE — `NEW`, `CONFIRMED`, `DELIVERED`
- **Методы**: camelCase — `saveEmployee`, `findByUsername`

## Паттерны и стиль кода

### Сущности (Entity)
```java
@Entity
@Table(name = "t_entity_name")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private RelatedEntity related;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ChildEntity> children = new ArrayList<>();

    @Transient
    public String getComputedField() { ... }
}
```

### Сервисы
- Внедрение зависимостей через конструктор
- `@Transactional` для операций с данными
- Возвращают `boolean` для индикации успеха/неудачи
- Обработка ошибок: try-catch + `log.error()` + `setRollbackOnly()`

### Контроллеры
- `@GetMapping` для рендеринга шаблонов, `@PostMapping` для обработки форм
- Используют `@PathVariable`, `@RequestParam`, `Model`
- Возвращают имя шаблона или `redirect:`

### Репозитории
- Кастомные запросы через `@Query` с JPQL
- Поддержка пагинации (`Page<T>`, `Pageable`)
- Поиск без учёта регистра через `LOWER() LIKE LOWER()`

### MapStruct-мапперы
```java
@Mapper
public interface EntityMapper {
    EntityMapper INSTANCE = Mappers.getMapper(EntityMapper.class);
    @Mapping(source = "nested.field", target = "flatField")
    EntityDTO toDTO(Entity entity);
}
```

## Документация в коде

Javadoc-комментарии на русском языке с описанием:
- Бизнес-контекста и назначения сущности
- Полей и правил валидации
- Связей между сущностями
- Секции разделяются маркерами: `// ========== ПОЛЯ ==========`

## Правила работы с Git

⚠️ **КРИТИЧЕСКИ ВАЖНО**:
- НИКОГДА не работай напрямую в ветках main/master/develop
- ВСЕГДА создавай новую ветку ПЕРЕД любыми изменениями
- Формат имени ветки: `claude/{краткое-описание}`

### Процесс работы:

1. **ПЕРЕД началом работы**:
```bash
   git checkout main
   git pull origin main
   git checkout -b claude/task-description
```

2. **Во время работы**:
   - Делай осмысленные коммиты после каждого логического изменения
   - Формат сообщения коммита: `feat: описание` или `fix: описание`

3. **ЗАПРЕЩЕНО**:
   - `git push` без явного разрешения пользователя
   - Коммиты напрямую в main/master/develop
   - `git push --force`

4. **После завершения**:
   - Сообщи пользователю о созданной ветке
   - НЕ пушь автоматически - дождись команды

## Логирование

- Библиотека: Logback (SLF4J)
- Файл: `logs/siarsp.log` (ротация ежечасно, 24ч хранение, 1GB лимит)
- Уровень: INFO (root)
- В сервисах: `@Slf4j` + `log.error()`, `log.info()`
