# Hướng dẫn lấy code và chạy project

## 1. Chuẩn bị

- Cài Git.
- Cài Docker Desktop và mở ứng dụng trước khi chạy các lệnh Docker.
- Có quyền truy cập repository nếu repository đang ở chế độ riêng tư.
- Để chạy image có sẵn, cần đợi workflow của dự án publish image lên GHCR thành công. Nếu image riêng tư, cần được cấp quyền tải và đăng nhập GHCR.

Không cần cài Java hoặc Maven trên máy nếu chỉ chạy bằng Docker.

## 2. Lấy code lần đầu

Mở PowerShell tại thư mục muốn lưu project, sau đó chạy:

```powershell
git clone --branch main https://github.com/phatcaoduc2910/KieuMaiNotifier.git
cd KieuMaiNotifier
```

Mở thư mục `KieuMaiNotifier` bằng IntelliJ IDEA hoặc VS Code.

Lần đầu tải project dùng `git clone`. Những lần cập nhật sau dùng `git pull`.

## 3. Tạo cấu hình trên máy

Tại thư mục gốc project, tạo `.env` nếu chưa có:

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item .env.example .env
}
```

Mở `.env` và thay các giá trị mẫu:

- `POSTGRES_PASSWORD`: mật khẩu riêng cho database trên máy.
- `JWT_SECRET`: chuỗi bí mật ngẫu nhiên có độ dài ít nhất 32 byte.

Có thể giữ nguyên các giá trị còn lại để dùng cấu hình mặc định. Không commit `.env` lên Git.

## 4. Chạy backend từ image đã phát hành

Sau khi lưu `.env`, chạy:

```powershell
docker compose -f compose.ghcr.yaml pull
docker compose -f compose.ghcr.yaml up -d
```

Các lệnh này tải image có sẵn và khởi động backend cùng PostgreSQL. Nhóm không cần build lại Java hoặc viết lại Dockerfile.

Với các cổng mặc định trong `.env`:

- Backend: <http://localhost:8080>.
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>.
- PostgreSQL từ máy host: `localhost:5434`.

Kiểm tra trạng thái và log:

```powershell
docker compose -f compose.ghcr.yaml ps
docker compose -f compose.ghcr.yaml logs -f api
```

Nhấn `Ctrl+C` để thoát chế độ xem log; các container vẫn chạy.

Database mới không chứa dữ liệu từ máy người phát triển. Có thể tạo tài khoản qua `POST /auth/register`, sau đó đăng nhập qua `POST /auth/login` để lấy token gọi các API yêu cầu xác thực.
