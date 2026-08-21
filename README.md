# My Blog Backend

Backend для блога на Java 21 и Spring MVC. Приложение предоставляет REST API для работы с постами, тегами, комментариями, лайками и изображениями постов. Данные хранятся в PostgreSQL через Spring JDBC, изображения — в файловой системе.

Приложение собирается в WAR и запускается в Tomcat.

## Модули

- `my-blog-back-app-bom` — единое управление версиями зависимостей.
- `my-blog-back-api` — контракты REST-контроллеров и DTO.
- `my-blog-back-impl` — реализация контроллеров, сервисы, JDBC-репозитории, модели, конфигурация и файловое хранилище. Результат сборки — WAR.
- Корневой `pom.xml` — Maven-агрегатор и общие настройки Java 21.

## Схема базы данных

Приложение использует схему PostgreSQL `my_blog`, которая при отсутствии создаётся автоматически из `my-blog-back-impl/src/main/resources/schema.sql`.

```mermaid
erDiagram
    POSTS ||--o{ COMMENTS : ""
    POSTS ||--o{ POST_TAGS : ""
    TAGS ||--o{ POST_TAGS : ""

    POSTS {
        BIGSERIAL id PK
        TEXT title
        TEXT text
        BIGINT likes_count
        TEXT image_path
    }

    COMMENTS {
        BIGSERIAL id PK
        TEXT text
        BIGINT post_id FK
    }

    TAGS {
        BIGSERIAL id PK
        TEXT name UK
    }

    POST_TAGS {
        BIGINT post_id PK, FK
        BIGINT tag_id PK, FK
    }
```

Один пост может иметь несколько комментариев и тегов. Связь постов с тегами реализована через таблицу `post_tags`. Поле `tags.name` уникально. При удалении поста удаляются его комментарии и связи с тегами; при удалении тега удаляются его связи с постами.

## Требования

- JDK 21;
- Maven 3.9+;
- PostgreSQL;
- Tomcat 11;
- Docker — только для интеграционных тестов с Testcontainers.

## Переменные окружения

| Переменная | Обязательна | Описание / значение по умолчанию |
| --- | --- | --- |
| `BLOG_DATASOURCE_URL` | да | JDBC URL, например `jdbc:postgresql://localhost:5432/postgres` |
| `BLOG_DATASOURCE_USERNAME` | да | Пользователь PostgreSQL |
| `BLOG_DATASOURCE_PASSWORD` | да | Пароль PostgreSQL |
| `BLOG_DATASOURCE_SCHEMA` | нет | Схема БД, по умолчанию `my_blog` |
| `BLOG_IMAGE_STORAGE_DIRECTORY` | нет | Каталог изображений, по умолчанию `./data/images` Используйте абсолютный путь для каталога изображений.|

При старте приложение выполняет `schema.sql` и создаёт недостающие объекты. 

## Сборка и тесты

Полная сборка с тестами:

```bash
mvn clean package
```

Для интеграционных тестов должен работать Docker. Собрать WAR без тестов можно с помощью:

```bash
mvn clean package -DskipTests
```

Готовый файл появится в `my-blog-back-impl/target/my-blog-back-impl-1.0-SNAPSHOT.war`.

## Запуск и деплой в Tomcat

Перед запуском передайте переменные окружения процессу Tomcat. Для постоянной конфигурации их можно добавить в `$CATALINA_BASE/bin/setenv.sh`:

```bash
export BLOG_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
export BLOG_DATASOURCE_USERNAME=blog
export BLOG_DATASOURCE_PASSWORD=change-me
export BLOG_DATASOURCE_SCHEMA=my_blog
export BLOG_IMAGE_STORAGE_DIRECTORY=/var/lib/my-blog/images
```

Соберите и скопируйте WAR:

```bash
mvn clean package -DskipTests
cp my-blog-back-impl/target/my-blog-back-impl-1.0-SNAPSHOT.war \
  "$CATALINA_BASE/webapps/api.war"
"$CATALINA_BASE/bin/catalina.sh" run
```

Имя `api.war` задаёт context path `/api`, поэтому endpoint постов будет доступен по адресу `http://localhost:8080/api/posts`. Для остановки используйте `"$CATALINA_BASE/bin/catalina.sh" stop`.
