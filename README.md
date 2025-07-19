# Account Balance REST API

**Spring Boot 3.5.3** • H2 Database • Caffeine Cache • Lombok • Docker • Idempotency • Swagger (OpenAPI 3)

## 📖 Описание

REST API для управления балансом счетов с поддержкой:
- Транзакций (депозит, снятие) в разных валютах (USD, EUR, BYN, RUB) 
- http://localhost:8080/h2-console база данных H2, логин: `sa`, пароль: пусто. (URL: jdbc:h2:mem:db)
- Конвертация валют (по хардкод-курсам)
- Баланс только в USD, рассчитывается по всем операциям
- История транзакций по каждому счету
- Идемпотентность операций через заголовок `Idempotency-Key`
- Кэширование баланса через Caffeine
- Полная OpenAPI-документация (Swagger UI) http://localhost:8080/swagger-ui/index.html

## 🚀 Быстрый старт

**Требования:**
- Java 17+
- Gradle 8+
- Docker (опционально)

### 1. Клонировать и собрать проект

```bash
git clone https://github.com/PavelAlkhin/finance_io
cd <папка_проекта>
./gradlew clean build



