# SsuBench

REST API платформа для размещения заданий, откликов исполнителей и перевода виртуальных баллов.

## Стек

- Java 25
- Spring Boot
- PostgreSQL
- JDBI
- JWT
- bcrypt
- Docker
- SQL-миграции

## О проекте

Платформа поддерживает основной сценарий:

1. Заказчик создаёт и публикует задание.
2. Исполнители откликаются на задание.
3. Заказчик выбирает одного исполнителя.
4. Исполнитель помечает задание как выполненное.
5. Заказчик подтверждает выполнение.
6. После подтверждения баллы списываются у заказчика и начисляются исполнителю.

Роли в системе:

- `CUSTOMER` — заказчик
- `EXECUTOR` — исполнитель
- `ADMIN` — администратор

## Основные сущности

- `User` — пользователь
- `Task` — задача
- `Bid` — отклик на задачу
- `Payment` — фиксация перевода баллов

## Бизнес-правила

- У задачи может быть только один подтверждённый `Bid`.
- Только выбранный исполнитель может пометить задачу как выполненную.
- Только заказчик может подтвердить выполнение.
- Подтверждение выполнения выполняется атомарно в транзакции.
- Если у заказчика недостаточно баллов, подтверждение невозможно.
- Задачи можно отменять, но подтверждённые задачи отменить нельзя.

## Архитектура

Проект разделён на слои:

- `controller` — HTTP-эндпоинты
- `service` — бизнес-логика
- `repo` — доступ к базе данных

Дополнительно используются:

- `security` — JWT, извлечение текущего пользователя, обработчики доступа
- `middleware` — `request_id` и логирование запросов
- `api.error` — единый формат ошибок
- `configuration` — Spring/JDBI/Transaction/DataSource конфигурация

## Аутентификация и безопасность

- Аутентификация реализована через JWT.
- Токен передаётся в заголовке:

```bash
Authorization: Bearer <token>
```

- Пароли хранятся только в bcrypt.
- Доступ к большинству эндпоинтов требует аутентификации.

## Формат ошибок

Все ошибки возвращаются в едином JSON-формате `ApiErrorResponse` со следующими полями:

- `timestamp`
- `status`
- `error`
- `code`
- `message`
- `path`
- `requestId`
- `details`

## Эндпоинты

### Auth

- `POST /auth/register`
- `POST /auth/login`

### Users

- `GET /users/{id}`
- `GET /users`
- `POST /users`
- `PUT /users/{id}`
- `POST /users/{id}/balance`
- `DELETE /users/{id}`
- `POST /users/{id}/block`
- `POST /users/{id}/unblock`

### Tasks

- `POST /tasks`
- `PUT /tasks/{id}`
- `POST /tasks/{id}/publish`
- `GET /tasks/{id}`
- `GET /tasks`
- `POST /tasks/{id}/cancel`

### Bids

- `POST /bids`
- `GET /bids/task/{taskId}`
- `POST /bids/{bidId}/select`
- `POST /bids/task/{taskId}/complete`

### Payments

- `POST /payments/confirm`

## Статусы

### `TaskStatus`
- `CREATED`
- `PUBLISHED`
- `IN_PROGRESS`
- `DONE`
- `CONFIRMED`
- `CANCELLED`

### BidStatus

- `PENDING`
- `ACCEPTED`
- `REJECTED`

## Переменные окружения

Проект читает конфигурацию из `.env` и `application.yaml`.

Пример .env:

```env
FLYWAY_URL=
POSTGRES_URL=
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
JWT_SECRET=
HTTP_TIMEOUT_CONNECTION=
HTTP_TIMEOUT_KEEP_ALIVE=
HTTP_SHUTDOWN_TIMEOUT=
HTTP_TIMEOUT_REQUEST=
```

## Запуск

Подготовить окружение, заполнить .env:

```bash
cp .env.example .env
```

Запустить докер:

```bash
docker compose up
```

Миграции применятся автоматически.

Запустить приложение:

```bash
./gradlew test
./gradlew bootRun
```

## Примеры curl

API доступен по адресу `http://localhost:8080`

### Регистрация 

```bash
curl -X POST "$BASE_URL/auth/register?email=$CUSTOMER_EMAIL&password=$PASSWORD&role=CUSTOMER"

curl -X POST "$BASE_URL/auth/register?email=$EXECUTOR_EMAIL&password=$PASSWORD&role=EXECUTOR"

curl -X POST "$BASE_URL/auth/register?email=$ADMIN_EMAIL&password=$PASSWORD&role=ADMIN"
```

### Вход 

```bash
export CUSTOMER_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$CUSTOMER_EMAIL&password=$PASSWORD"
)

export EXECUTOR_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$EXECUTOR_EMAIL&password=$PASSWORD"
)

export ADMIN_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$ADMIN_EMAIL&password=$PASSWORD"
)
```

### Создание задачи

```bash
export TASK_ID=$(
  curl -s -X POST "$BASE_URL/tasks?title=Подготовить%20отчёт&description=Собрать%20отчёт%20по%20тестированию&reward=50" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"
)
```

### Публикация задачи

```bash
curl -X POST "$BASE_URL/tasks/$TASK_ID/publish" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Отклик исполнителя на задачу

```bash
export BID_ID=$(
  curl -s -X POST "$BASE_URL/bids?taskId=$TASK_ID" \
    -H "Authorization: Bearer $EXECUTOR_TOKEN"
)
```

### Список откликов по задаче

```bash
curl -X GET "$BASE_URL/bids/task/$TASK_ID?limit=20&offset=0" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Выбор исполнителя

```bash
curl -X POST "$BASE_URL/bids/$BID_ID/select?taskId=$TASK_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Отметка о выполнении

```bash
curl -X POST "$BASE_URL/bids/task/$TASK_ID/complete" \
  -H "Authorization: Bearer $EXECUTOR_TOKEN"
```

### Подтверждение выполнения и перевод баллов

```bash
curl -X POST "$BASE_URL/payments/confirm?taskId=$TASK_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Получение списка задач

```bash
curl -X GET "$BASE_URL/tasks?limit=20&offset=0" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Cписок пользователей

```bash
curl -X GET "$BASE_URL/users?limit=20&offset=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Блокировка и разблокировка пользователя

```bash
curl -X POST "$BASE_URL/users/<USER_ID>/block" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST "$BASE_URL/users/<USER_ID>/unblock" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## OpenAPI

Описание API: `openapi.yaml`

Swagger после запуска доступен по адресу `http://localhost:8080/swagger-ui/index.html#/`
