# SIARSP Mobile Android

Мобильное Android-приложение для всех сотрудников системы SIARSP.

## Что умеет

### Для всех ролей
- Авторизация через backend SIARSP (`POST /employee/login`, сессионная cookie)
- Просмотр профиля (ФИО, роль)
- Просмотр складов

### По ролям

| Раздел | ADMIN | MANAGER | WH_MANAGER | WH_WORKER | COURIER | ACCOUNTER |
|--------|:-----:|:-------:|:----------:|:---------:|:-------:|:---------:|
| Клиенты | + | + | + | | | + |
| Поставщики | + | + | + | | | + |
| Товары | + | + | + | + | | + |
| Склады | + | + | + | + | + | + |
| Транспорт | | + | | | + | |
| Заказы | + | + | + | + | | + |
| Задачи доставки | | + | + | | + | |
| Документы (ТТН, Акты) | | + | + | | + | + |
| Сотрудники | + | | | | | |
| Мои доставки + GPS | | | | | + | |

### Для курьера (ROLE_EMPLOYEE_COURIER)
- Просмотр собственных задач на доставку (`GET /employee/courier/deliveryTasks/mobile/myDeliveryTasks`)
- Автоматическая отправка GPS-координат каждые 60 сек для задач `IN_TRANSIT`

## Что нужно установить
1. **Android Studio Koala** или новее
2. Android SDK Platform 34
3. JDK 17 (обычно встроена в Android Studio)

## Первый запуск
1. Запустите backend SIARSP (Spring Boot) на `http://localhost:8080`
2. Откройте папку `mobile-android` в Android Studio: `File -> Open`
3. Дождитесь `Gradle Sync`
4. Подключите телефон по USB (с включенной отладкой) **или** создайте эмулятор Android
5. Нажмите **Run**

## Настройка адреса backend
В `MainActivity.kt` задано:
- `BASE_URL = "http://10.0.2.2:8080/"` — для **эмулятора Android**

Для **реального телефона** замените на IP компьютера в локальной сети:
- `http://192.168.1.50:8080/`

И запустите Spring Boot с `server.address=0.0.0.0`.

## Сборка APK
В Android Studio: `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`

CLI-вариант из папки `mobile-android`:
```bash
gradle :app:assembleDebug
```

## REST API (backend)

Все эндпоинты мобильного API: `GET /api/mobile/*`

| Эндпоинт | Описание |
|----------|----------|
| `/api/mobile/profile` | Профиль текущего пользователя |
| `/api/mobile/clients` | Список клиентов |
| `/api/mobile/clients/{id}` | Детали клиента |
| `/api/mobile/suppliers` | Список поставщиков |
| `/api/mobile/products` | Список товаров |
| `/api/mobile/products/{id}` | Детали товара |
| `/api/mobile/warehouses` | Список складов |
| `/api/mobile/vehicles` | Список транспорта |
| `/api/mobile/orders` | Список заказов |
| `/api/mobile/orders/{id}` | Детали заказа |
| `/api/mobile/deliveryTasks` | Задачи на доставку |
| `/api/mobile/documents/ttn` | Список ТТН |
| `/api/mobile/documents/acts` | Список актов приёмки |
| `/api/mobile/employees` | Список сотрудников (ADMIN) |

## Тестовый сценарий
1. Войдите любым сотрудником
2. На главном экране отображаются разделы, доступные вашей роли
3. Откройте любой раздел — данные загружаются с backend
4. Для курьера: на вкладке "Доставки" задачи обновляются, координаты отправляются автоматически
