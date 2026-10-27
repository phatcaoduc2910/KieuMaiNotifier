Hướng dẫn chạy backend và PostgreSQL bằng Docker Compose: [docs/DOCKER.md](docs/DOCKER.md).

Hướng dẫn publish image lên GitHub Container Registry và chạy image đã build: [docs/GHCR.md](docs/GHCR.md).

```mermaid
erDiagram
    USERS ||--o{ REPORTS : "gui"
    USERS ||--o{ SUBSCRIPTIONS : "dang_ky"
    USERS ||--o{ REPORT_UPDATES : "bo_sung"
    WARDS ||--o{ INCIDENTS : "co"
    WARDS ||--o{ SUBSCRIPTIONS : "duoc_theo_doi"
    INCIDENTS ||--o{ REPORTS : "gom"
    REPORTS ||--o{ REPORT_UPDATES : "co_dien_bien"

    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar display_name
        numeric latitude "nullable"
        numeric longitude "nullable"
        timestamptz created_at
    }

    WARDS {
        bigint id PK
        varchar code UK
        varchar name
        varchar province_name
    }

    INCIDENTS {
        bigint id PK
        bigint ward_id FK
        varchar type
        varchar title
        numeric latitude
        numeric longitude
        varchar status
        timestamptz started_at
        timestamptz expires_at
        timestamptz alerted_at "nullable"
        timestamptz created_at
    }

    REPORTS {
        bigint id PK
        bigint user_id FK
        bigint incident_id FK
        text description
        numeric latitude
        numeric longitude
        timestamptz observed_at
        timestamptz created_at
        timestamptz deleted_at "nullable"
    }

    SUBSCRIPTIONS {
        bigint id PK
        bigint user_id FK
        bigint ward_id FK
        timestamptz created_at
    }

    REPORT_UPDATES {
        bigint id PK
        bigint report_id FK
        bigint user_id FK
        text content
        timestamptz created_at
    }
```
