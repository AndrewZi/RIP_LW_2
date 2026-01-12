# Sensor Reactive System - Microservices Architecture

Полнофункциональная микросервисная система на Spring Boot с реактивным программированием. Две независимые Services (Client и Server) работают как отдельные Docker контейнеры и взаимодействуют через REST API.

## Архитектура

```
┌─────────────────────────────────────────────────────┐
│                   Docker Network                     │
│                                                       │
│  ┌──────────────────┐    ┌────────────────────┐    │
│  │  Service A       │    │   Service B        │    │
│  │  (Client)        │    │   (Server)         │    │
│  │  Port: 8080      │───│   Port: 8081       │    │
│  │ /api/client/*    │    │ /api/sensors/*     │    │
│  └──────────────────┘    │                    │    │
│                          │ Generates Data     │    │
│                          └────────────────────┘    │
│                                    │                │
│                                    ▼                │
│                          ┌─────────────────┐       │
│                          │  PostgreSQL     │       │
│                          │  Port: 5432     │       │
│                          └─────────────────┘       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Технический стек

- **Spring Boot 3.4.0** с WebFlux для реактивности
- **Java 17+**
- **PostgreSQL** для основной БД
- **Docker & Docker Compose** для контейнеризации
- **Lombok** для снижения boilerplate кода
- **Maven** для управления зависимостями
- **SLF4J** для логирования
- **Flyway** для миграций БД
- **R2DBC** для асинхронного доступа к БД
- **Reactor** для реактивного программирования

## Быстрый старт

### С использованием Docker Compose (Рекомендуется)

```bash
# Построить Docker образы для обоих сервисов и запустить контейнеры
make docker-build
make docker-up

# Просмотреть логи всех сервисов
make logs

# Просмотреть логи конкретного сервиса
make logs-a      # Service A (Client)
make logs-b      # Service B (Server)
make logs-db     # PostgreSQL

# Остановить контейнеры
make docker-down
```

## Доступные Makefile команды

| Команда | Описание |
|---------|---------|
| `make help` | Показать все доступные команды |
| `make build` | Собрать оба сервиса (Service A и Service B) |
| `make build-service-a` | Собрать только Service A (Client) |
| `make build-service-b` | Собрать только Service B (Server) |
| `make clean` | Очистить build артефакты и Docker образы |
| `make docker-build` | Построить Docker образы для обоих сервисов |
| `make docker-up` | Запустить все контейнеры (Service A, B, PostgreSQL) |
| `make docker-down` | Остановить и удалить контейнеры |
| `make logs` | Просмотреть логи всех сервисов |
| `make logs-a` | Просмотреть логи Service A (Client) |
| `make logs-b` | Просмотреть логи Service B (Server) |
| `make logs-db` | Просмотреть логи PostgreSQL |
| `make ps` | Список запущенных контейнеров |
| `make health` | Проверить health статус обоих сервисов |
| `make test-endpoints` | Протестировать API endpoints |
| `make reset-db` | Сбросить базу данных |

## API Endpoints

### Service B (Server) - Генератор потока датчиков
**Адрес:** `http://localhost:8081`

#### Получить поток одного датчика
```bash
curl -N "http://localhost:8081/api/sensors/stream?sensorId=1&limit=5"
```

**Параметры:**
- `sensorId` (Long, опционально) - ID датчика для потока
- `limit` (Integer, опционально) - Максимальное количество элементов (по умолчанию 10)

**Ответ (NDJSON - Newline Delimited JSON):**
```json
{"sensor_id":1,"timestamp":1734447600000,"temperature":22.5,"humidity":55.0,"pressure":1013.2,"value":45.3,"anomaly":false}
{"sensor_id":1,"timestamp":1734447601000,"temperature":22.6,"humidity":55.1,"pressure":1013.3,"value":45.4,"anomaly":false}
{"sensor_id":1,"timestamp":1734447602000,"temperature":22.7,"humidity":55.2,"pressure":1013.4,"value":45.5,"anomaly":true}
```

#### Получить поток нескольких датчиков
```bash
curl -N "http://localhost:8081/api/sensors/stream/multi?sensorCount=3&limit=10"
```

**Параметры:**
- `sensorCount` (Integer, опционально) - Количество датчиков для потока (по умолчанию 5)
- `limit` (Integer, опционально) - Максимальное количество элементов на датчик (по умолчанию 20)

### Service A (Client) - API Gateway к Service B
**Адрес:** `http://localhost:8080`

Service A выступает как прокси/API Gateway и перенаправляет запросы к Service B с поддержкой retry логики.

#### Получить поток через клиента (single sensor)
```bash
curl -N "http://localhost:8080/api/client/sensors?sensorId=1&limit=5"
```

Эта команда будет перенаправлена на Service B: `http://service-b:8080/api/sensors/stream?sensorId=1&limit=5`

#### Получить поток через клиента (multiple sensors)
```bash
curl -N "http://localhost:8080/api/client/sensors/multi?sensorCount=3&limit=10"
```

## Примеры логов

### Service B - Успешная инициализация сервера
```
2024-12-17 20:15:30.123 [main] INFO  com.sensordata.SensorServerApplication - Starting SensorServerApplication
2024-12-17 20:15:32.456 [main] INFO  o.s.b.w.e.netty.NettyWebServer - Netty started on port(s): 8080
```

### Service A - Успешная инициализация клиента
```
2024-12-17 20:15:35.123 [main] INFO  com.sensordata.SensorClientApplication - Starting SensorClientApplication
2024-12-17 20:15:37.456 [main] INFO  com.sensordata.config.WebClientConfig - Creating WebClient with baseUrl=http://service-b:8080
2024-12-17 20:15:38.789 [main] INFO  o.s.b.w.e.netty.NettyWebServer - Netty started on port(s): 8080
```

### HTTP запрос через LoggingFilter
```
2024-12-17 20:15:35.123 [reactor-http-nio-2] INFO  com.sensordata.filter.LoggingFilter - >>> [REQUEST] method=GET, path=/api/sensors/stream, params={sensorId=[1], limit=[5]}
2024-12-17 20:15:40.456 [reactor-http-nio-2] INFO  com.sensordata.filter.LoggingFilter - <<< [RESPONSE] method=GET, path=/api/sensors/stream, status=200, duration=5123ms, signal=ON_COMPLETE
```

### Начало потока датчика
```
2024-12-17 20:15:35.234 [parallel-1] INFO  com.sensordata.service.SensorStreamService - Starting sensor stream for sensorId=1, limit=5
2024-12-17 20:15:35.345 [parallel-1] DEBUG com.sensordata.service.SensorStreamService - Emitting sensor data at tick=0
2024-12-17 20:15:35.456 [parallel-1] DEBUG com.sensordata.service.SensorClientService - Received sensor data: sensorId=1, value=45.3
```

### Завершение потока
```
2024-12-17 20:15:40.567 [parallel-1] INFO  com.sensordata.service.SensorStreamService - Sensor stream completed for sensorId=1
2024-12-17 20:15:40.678 [parallel-1] INFO  com.sensordata.service.SensorClientService - Sensor stream completed for sensorId=1
```

### Ошибка и обработка
```
2024-12-17 20:15:45.123 [reactor-http-nio-3] ERROR com.sensordata.service.SensorClientService - Error receiving sensor stream for sensorId=2: Connection refused
2024-12-17 20:15:45.234 [reactor-http-nio-3] WARN  com.sensordata.service.SensorClientService - Retrying sensor stream request, attempt=1
2024-12-17 20:15:46.345 [reactor-http-nio-3] WARN  com.sensordata.service.SensorClientService - Retrying sensor stream request, attempt=2
2024-12-17 20:15:47.456 [reactor-http-nio-3] ERROR com.sensordata.service.SensorClientService - Failed to fetch sensor stream after retries: Connection refused
```

**Жизненный цикл:**
1. **Subscription** - клиент подписывается на Flux
2. **Emission** - Flux начинает испускать элементы (в данном случае каждую секунду)
3. **Processing** - каждый элемент обрабатывается через операторы
4. **Completion** или **Error** или **Cancellation** - поток заканчивается одним из способов

## Неоптимальный код (будет оптимизирован в ЛР №3)

### Расположение
Основная неоптимальная логика находится в:
- **`src/main/java/com/sensordata/service/SensorDataGenerator.java`** - Главный класс с неоптимизированным кодом

## Переменные окружения

| Переменная | По умолчанию | Описание |
|-----------|-------------|---------|
| `DB_HOST` | localhost | Хост PostgreSQL |
| `DB_PORT` | 5432 | Порт PostgreSQL |
| `DB_NAME` | sensordb | Имя базы данных |
| `DB_USER` | postgres | Пользователь БД |
| `DB_PASSWORD` | postgres | Пароль БД |
| `SERVER_PORT` | 8080 | Порт приложения |
| `SENSOR_SERVER_URL` | http://localhost:8080 | URL сервера датчиков |

## Мониторинг и Управление

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Info
```bash
curl http://localhost:8080/actuator/info
```

### Очистить контейнеры и данные
```bash
make clean
```

### Проверить логи контейнера
```bash
make logs
```

## Тестирование

Используйте команду `make test-endpoints` для автоматического тестирования, или вручную:

### Тест 1: Service B - Простой поток одного датчика
```bash
curl -N -X GET "http://localhost:8081/api/sensors/stream?sensorId=1&limit=3"
```

### Тест 2: Service B - Поток нескольких датчиков
```bash
curl -N -X GET "http://localhost:8081/api/sensors/stream/multi?sensorCount=3&limit=5"
```

### Тест 3: Service A - Клиентский доступ (Single Sensor)
```bash
curl -N -X GET "http://localhost:8080/api/client/sensors?sensorId=1&limit=3"
```

### Тест 4: Service A - Клиентский доступ (Multiple Sensors)
```bash
curl -N -X GET "http://localhost:8080/api/client/sensors/multi?sensorCount=3&limit=5"
```

### Тест 5: Service A - Health Check
```bash
curl -X GET "http://localhost:8080/actuator/health"
```

### Тест 6: Service B - Health Check
```bash
curl -X GET "http://localhost:8081/actuator/health"
```

## Лицензия

MIT
