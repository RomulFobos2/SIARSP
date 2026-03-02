# SIARSP Courier Android (учебный клиент)

Это простое Android-приложение для сотрудника с ролью `ROLE_EMPLOYEE_COURIER`.

## Что умеет
- Авторизация через существующий backend SIARSP (`POST /employee/login`, сессионная cookie).
- Просмотр собственных задач (`GET /employee/courier/deliveryTasks/mobile/myDeliveryTasks`).
- Для задач в статусе `IN_TRANSIT` — отправка координат (`POST /employee/courier/deliveryTasks/mobile/updateLocation/{id}`).

## Что нужно установить
1. **Android Studio Koala** или новее (скачивать только Android Studio, IntelliJ IDEA не нужна).
2. Android SDK Platform 34 (предложит сама Android Studio).
3. JDK 17 (обычно встроена в Android Studio).

## Первый запуск
1. Запустите backend SIARSP (Spring Boot) на `http://localhost:8080`.
2. Откройте папку `mobile-android` в Android Studio: `File -> Open`.
3. Дождитесь `Gradle Sync`.
4. Подключите телефон по USB (с включенной отладкой) **или** создайте эмулятор Android.
5. Нажмите **Run**.

## Важная настройка адреса backend
В `MainActivity.kt` задано:
- `BASE_URL = "http://10.0.2.2:8080/"` — это правильно для **эмулятора Android**.

Если запускаете на **реальном телефоне**, замените адрес на IP вашего компьютера в локальной сети, например:
- `http://192.168.1.50:8080/`

И запустите Spring Boot так, чтобы он слушал сеть (например `server.address=0.0.0.0`).

## Сборка APK для установки вручную
В Android Studio:
1. `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`
2. После сборки нажмите ссылку **locate**.
3. Готовый файл обычно лежит в:
   `mobile-android/app/build/outputs/apk/debug/app-debug.apk`
4. Передайте APK на телефон и установите вручную.

CLI-вариант из папки `mobile-android`:
```bash
gradle :app:assembleDebug
```
(или `./gradlew :app:assembleDebug`, если добавите Gradle Wrapper)

## Тестовый сценарий
1. Войдите сотрудником с ролью `ROLE_EMPLOYEE_COURIER`.
2. Нажмите "Обновить" — должны подгрузиться активные задачи.
3. Введите latitude/longitude.
4. У задачи со статусом `IN_TRANSIT` нажмите "Отправить мои координаты".
5. Проверьте на backend, что у `DeliveryTask` обновились `currentLatitude/currentLongitude`.
