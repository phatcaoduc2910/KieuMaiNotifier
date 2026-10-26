# Chạy Kiều Mai Notifier bằng Docker

Docker Compose chạy hai container: `api` chứa Spring Boot và Java 21; `db` chứa PostgreSQL 18. pgAdmin là công cụ quản lý PostgreSQL. Bạn có thể tiếp tục dùng pgAdmin trên Windows để quản lý database trong container.

Để publish image lên GitHub Container Registry hoặc tải image đã build, xem [hướng dẫn GHCR](GHCR.md). `compose.yaml` build từ source trên máy; `compose.ghcr.yaml` chạy image đã publish. Hai tệp dùng cùng tên project và volume, nên là hai cách chạy thay thế cho cùng hệ thống.

Cấu hình này dành cho phát triển và benchmark trên máy cá nhân. Các cổng chỉ được mở tại `127.0.0.1`. Database Docker có volume riêng, không dùng thư mục dữ liệu của PostgreSQL đã cài trên Windows.

## 1. Khởi động lần đầu

Mở Docker Desktop và chờ Linux engine sẵn sàng. Mở PowerShell tại thư mục dự án:

```powershell
docker version
docker compose version
```

Nếu chưa có `.env`, tạo từ mẫu:

```powershell
Copy-Item .env.example .env
notepad .env
```

Đặt `POSTGRES_PASSWORD` và `JWT_SECRET` bằng giá trị riêng. `JWT_SECRET` cần ít nhất 32 byte; dùng chuỗi ASCII ngẫu nhiên dài ít nhất 64 ký tự. `.env` đã được loại khỏi Git và Docker build context. Nếu `.env` đã tồn tại, chỉnh tệp hiện có thay vì ghi đè.

Khởi động cả hai container:

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

Build lần đầu tải Java và Maven dependencies nên có thể mất vài phút. Dockerfile chạy Maven `verify`, bao gồm bộ test, rồi đóng gói JAR; không cần cài Java hoặc Maven trên Windows. Build dừng nếu có test thất bại.

Compose đợi PostgreSQL vượt qua healthcheck rồi mới khởi động API. Chờ log `Started NotifierApplication`, sau đó nhấn Ctrl+C để dừng xem log; container vẫn chạy.

Kiểm tra API:

```powershell
Invoke-RestMethod 'http://localhost:8080/reports?page=0&size=20'
```

Database mới sẽ có dữ liệu rỗng. Hibernate tạo/cập nhật bảng theo cấu hình phát triển `ddl-auto=update`. Dữ liệu PostgreSQL cũ không tự chuyển vào container.

Nếu cổng `8080` đang được backend IntelliJ sử dụng, dừng backend đó hoặc đổi `APP_PORT=8081` trong `.env`, rồi chạy lại Compose. Khi đó API ở `http://localhost:8081`.

## 2. Kết nối bằng pgAdmin đang có

Trong pgAdmin, chọn **Register > Server**, đặt tên như `KieuMai Docker`, rồi điền tab **Connection**:

| Trường | Giá trị mặc định |
| --- | --- |
| Host name/address | `localhost` |
| Port | `5434` |
| Maintenance database | `kieumai_notifier` |
| Username | `postgres` |
| Password | Giá trị `POSTGRES_PASSWORD` trong `.env` |

Nếu đổi tên database, user hoặc cổng trong `.env`, dùng giá trị tương ứng. Kết nối pgAdmin cũ tại `localhost:5433` vẫn trỏ đến PostgreSQL trên Windows.

API trong Docker kết nối `db:5432`, vì `db` là tên dịch vụ trên mạng Compose. `localhost` bên trong container API là chính container API. Cổng `5434` chỉ dùng từ máy Windows để vào database container.

Các biến `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` khởi tạo PostgreSQL khi volume còn trống. Sửa các biến này sau khi database đã có dữ liệu không tự đổi tên database, tạo user hoặc đổi mật khẩu. Muốn đổi mật khẩu, thực hiện trong PostgreSQL rồi cập nhật `.env` cho khớp.

## 3. Chuyển dữ liệu PostgreSQL cũ vào Docker

Giữ database gốc để đối chiếu. Không sao chép trực tiếp thư mục dữ liệu PostgreSQL trên Windows vào Linux container. Dùng backup/restore.

Cách đơn giản với pgAdmin:

1. Tại kết nối PostgreSQL cũ (`localhost:5433`), chọn database `kieumai_notifier`, nhấn **Backup**, chọn **Format: Custom**, rồi lưu tệp `.dump`.
2. Dừng API Docker để tránh ghi dữ liệu trong lúc chuyển:

   ```powershell
   docker compose stop api
   ```

3. Tại kết nối `KieuMai Docker` (`localhost:5434`), tạo database mới tên `kieumai_notifier_import`, owner `postgres`. Dùng tên mới này để tránh xung đột với các bảng Hibernate đã tạo trong database mặc định.
4. Chọn database mới, nhấn **Restore**, chọn tệp `.dump`. Bật **Do not save > Owner** và **Privileges** nếu bản backup có role hoặc quyền từ máy cũ. Trong **Query Options**, bật **Single transaction** để lỗi restore không để lại dữ liệu dở dang. Giữ **Include CREATE DATABASE statement** và **Clean before restore** ở trạng thái tắt. Kiểm tra restore hoàn tất không có lỗi trước khi tiếp tục.
5. Kiểm tra bảng và số lượng bản ghi trên database mới. Đổi `.env` thành `POSTGRES_DB=kieumai_notifier_import`. Database đã được tạo tại bước 3; việc sửa `.env` chỉ chuyển cấu hình kết nối.
6. Chạy lại:

   ```powershell
   docker compose up -d --build
   ```

Nếu cần giữ mọi thay đổi mới nhất, dừng các ứng dụng đang ghi vào database cũ trước khi tạo bản backup cuối cùng. Backup không bao gồm những thay đổi phát sinh sau thời điểm snapshot.

Có thể tạo backup Custom bằng PowerShell thay cho pgAdmin. Đường dẫn sau phù hợp PostgreSQL 18 đang cài trên máy:

```powershell
New-Item -ItemType Directory -Force .\backups | Out-Null
& 'C:\Program Files\PostgreSQL\18\bin\pg_dump.exe' -h localhost -p 5433 -U postgres -d kieumai_notifier -Fc -f .\backups\kieumai_notifier.dump
```

Nhập mật khẩu PostgreSQL cũ khi được hỏi. Tệp trong `backups/` đã được loại khỏi Git và build context. Sao lưu tệp ra vị trí riêng nếu dữ liệu quan trọng.

## 4. Các lệnh thường dùng

```powershell
# Xem trạng thái, log và tài nguyên.
docker compose ps
docker compose logs --tail 100 api db
docker stats

# Build lại API sau khi sửa code.
docker compose up -d --build api

# Dừng và gỡ container, vẫn giữ database trong volume.
docker compose down

# Khởi động lại với dữ liệu cũ trong volume.
docker compose up -d
```

Không dùng `docker compose down -v` nếu muốn giữ dữ liệu: tùy chọn `-v` xóa volume database. Volume giúp giữ dữ liệu qua những lần tạo lại container; volume không thay thế bản backup.

Docker build dùng profile `docker` và biến môi trường do Compose cung cấp. `application.properties` trong Git chỉ chứa cấu hình mặc định và tham chiếu biến môi trường. Khi chạy bằng IntelliJ, đặt `SPRING_DATASOURCE_PASSWORD` và `JWT_SECRET`, hoặc tạo `application-local.properties` tại thư mục gốc dự án để ghi đè các thông số cục bộ. Tệp local này được Git bỏ qua và không được đưa vào Docker build context.

## 5. Benchmark từ Kaggle

Khi API Docker chạy tại `http://localhost:8080`, tunnel đã hướng dẫn vẫn dùng được:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Nếu đổi `APP_PORT`, đổi cổng của lệnh tunnel tương ứng. Dùng URL HTTPS của tunnel làm `host` trong Locust trên Kaggle. Theo dõi `docker stats` trên máy backend trong lúc đo. Giữ nguyên dữ liệu và giới hạn tài nguyên Docker Desktop giữa các lần đo; độ trễ đo từ Kaggle bao gồm cả mạng và tunnel.

## Tài liệu tham khảo

- [Networking trong Docker Compose](https://docs.docker.com/compose/how-tos/networking/).
- [Đợi database sẵn sàng bằng healthcheck](https://docs.docker.com/compose/how-tos/startup-order/).
- [PostgreSQL Official Image và volume PostgreSQL 18](https://hub.docker.com/_/postgres).
- [PostgreSQL pg_dump](https://www.postgresql.org/docs/18/app-pgdump.html).
- [Hộp thoại Restore của pgAdmin](https://www.pgadmin.org/docs/pgadmin4/latest/restore_dialog.html).
