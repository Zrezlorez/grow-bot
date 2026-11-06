# Grow Bot

Телеграм- и Discord-бот с чат-игрой: игроки растут, используют способности, соревнуются в
топах и могут объединить профили обеих платформ в один аккаунт.

## Возможности

- Одна кодовая база, две платформы: Telegram long polling и Discord slash-команды.
- Рост с кулдаунами, критическими успехами, неудачами и бонусами за активность.
- Способности с кулдаунами: джекпот, молитва, черепаха, fuck, замедление и перевод.
- Временные статусы за повторное использование способностей (торговец, жрец, лудомания и др.).
- Топы в беседах и стили роста (деревья или «женские силы»).
- Привязка Telegram ↔ Discord по короткому коду.
- Учёт активности в сообщениях и голосовых каналах Discord.
- Админ-инструменты для просмотра и изменения игроков.
- Миграции Flyway и модель данных на PostgreSQL.
- Юнит-тесты основной игровой логики.

## Стек

- Java 21
- Spring Boot 4.0.6 (Spring Data JPA, Flyway, Validation)
- PostgreSQL 16
- Telegram Bots 9.5.0
- JDA 6.4.1 (Discord)
- Maven, Lombok
- JUnit 5, Mockito, AssertJ

## Команды

| Команда | Описание |
|---|---|
| `/grow` | Вырастить свой показатель |
| `/profile [user]` | Показать свой профиль или профиль другого игрока |
| `/top` | Топ беседы; в личных сообщениях — глобальный топ |
| `/style [name]` | Показать или изменить стиль роста беседы/сервера |
| `/link <code>` | Объединить профили Telegram и Discord в один аккаунт |
| `/help` | Список доступных команд |
| `/jackpot` | Увеличить шансы джекпота и неудачи |
| `/pray` | Уменьшить свой шанс неудачи |
| `/turtle` | Бесплатно увеличить свой рост |
| `/fuck <user>` | Увеличить шанс неудачи цели |
| `/slow <user>` | Замедлить рост цели |
| `/transfer <user> <value>` | Перевести часть роста игроку с меньшим размером, с комиссией |
| `/admin ...` | Админ-команды для игроков и конфигурации |

В Telegram команды отправляются как `/команда`. В Discord они зарегистрированы как slash-команды.

## Быстрый старт

### Docker Compose

1. Создать файл окружения:

   ```bash
   cp .env.example .env
   ```

2. Заполнить как минимум `TELEGRAM_TOKEN` или `DISCORD_TOKEN` и данные для подключения к БД.

3. Запустить бота и PostgreSQL:

   ```bash
   docker compose up --build
   ```

### Локальный запуск

Запустить PostgreSQL, задать переменные окружения из `.env.example`, затем выполнить:

```bash
mvn spring-boot:run
```

При старте приложение автоматически применяет миграции Flyway.

## Конфигурация

Основные переменные окружения:

| Переменная | Назначение |
|---|---|
| `TELEGRAM_TOKEN` | Токен Telegram-бота; пустое значение отключает Telegram |
| `DISCORD_TOKEN` | Токен Discord-бота; пустое значение отключает Discord |
| `SPRING_DATASOURCE_URL` | JDBC URL, например `jdbc:postgresql://localhost:5432/dn` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Настройки контейнера PostgreSQL для Compose |
| `ADMIN_TELEGRAM_USER_IDS` | Telegram user ID администраторов через запятую |
| `ADMIN_DISCORD_USER_IDS` | Discord user ID администраторов через запятую |
| `PROXY_ENABLED`, `PROXY_HOST`, `PROXY_PORT`, `PROXY_USERNAME`, `PROXY_PASSWORD` | Необязательный HTTP-прокси |
| `CLOUD_CONFIG_ENABLED`, `GITHUB_OWNER`, `GITHUB_REPO`, `GITHUB_PATH`, `GITHUB_REF`, `GITHUB_TOKEN` | Необязательная загрузка Spring-конфигурации из GitHub |

Игровой баланс (кулдауны, шансы роста, комиссии, лимиты) находится в
`src/main/resources/application.yml` в секции `game.*`.

## Тесты

```bash
mvn test
```

Тесты покрывают регистрацию и валидацию команд, расчёт комиссии за перевод, статусы
игроков, серии повторных действий, конвертацию стилей роста, форматирование утилит и
вывод админ-команд.

## Структура проекта

```text
src/main/java/com/litovskiy/
  bot/          адаптеры Telegram и Discord
  config/       конфигурация приложения и свойств
  entity/       JPA-сущности и перечисления
  repository/   репозитории Spring Data
  service/      команды, способности, активность, привязка, логирование
  util/         общие утилиты
src/main/resources/
  application.yml
  db/migration/ миграции Flyway
src/test/java/  юнит-тесты
```

## Лицензия

[MIT](LICENSE)
