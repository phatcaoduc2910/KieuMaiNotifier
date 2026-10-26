# Publish Kiều Mai Notifier lên GitHub Container Registry

Hướng dẫn này áp dụng cho repository `phatcaoduc2910/KieuMaiNotifier`, nhánh `main`. GitHub Actions build image API cho Linux AMD64 và ARM64, sau đó publish vào GHCR. PostgreSQL dùng image chính thức `postgres:18`; dữ liệu nằm trong volume riêng.

Image dự kiến sau khi publish thành công:

```text
ghcr.io/phatcaoduc2910/kieumainotifier:latest
```

## 1. Các tệp đã chuẩn bị

- `Dockerfile`: build ứng dụng Java 21 thành image.
- `.dockerignore`: loại cấu hình và thông tin kết nối trên máy khỏi build context.
- `.github/workflows/docker-publish.yml`: build và publish image tự động.
- `compose.yaml`: build và chạy từ source trên máy phát triển.
- `compose.ghcr.yaml`: chạy API từ image GHCR cùng PostgreSQL, không cần build lại Java.
- `.env.example`: mẫu cấu hình cho từng máy; `.env` thật không đưa lên GitHub.
- `src/main/resources/application-docker.properties`: cấu hình kết nối PostgreSQL cho container.

Workflow dùng `GITHUB_TOKEN` do GitHub cấp tự động với quyền `packages: write`. Không cần tạo token thủ công hoặc đưa mật khẩu database/JWT lên GitHub Actions. Các thông tin đó chỉ cần khi chạy container.

## 2. Commit và push từ máy phát triển

Chạy PowerShell tại thư mục dự án, kiểm tra đang ở nhánh `main`:

```powershell
git branch --show-current
```

Stage các tệp Docker/GHCR cùng dependency PostgreSQL trong `pom.xml`:

```powershell
git add Dockerfile .dockerignore compose.yaml compose.ghcr.yaml .env.example .gitignore
git add .github/workflows/docker-publish.yml src/main/resources/application.properties src/main/resources/application-docker.properties
git add pom.xml README.md docs/DOCKER.md docs/GHCR.md
git diff --cached
```

Kiểm tra diff trước khi commit. Không đưa `.env`, bản backup database, mật khẩu hoặc JWT secret vào commit. `application.properties` đã dùng biến môi trường; thông tin kết nối riêng chỉ nằm trong `.env` hoặc `application-local.properties` ở gốc dự án, đều được Git bỏ qua. Nếu bạn đã stage những thay đổi khác trước đó, chúng cũng xuất hiện trong diff và cần được kiểm tra. Image trên GitHub được build từ code đã commit; các sửa đổi cục bộ khác, như thay đổi `SecurityConfig.java`, không tự đi vào image.

Sau khi kiểm tra:

```powershell
git commit -m "Add Docker image publishing to GHCR"
git push -u origin main
```

Các lệnh trên được cung cấp để bạn tự chạy. Việc tạo file workflow trên máy chưa làm GitHub Actions chạy và chưa xuất bản image.

## 3. Theo dõi lần publish đầu tiên

1. Mở [Actions của KieuMaiNotifier](https://github.com/phatcaoduc2910/KieuMaiNotifier/actions).
2. Nếu GitHub yêu cầu bật Actions, bật Actions cho repository.
3. Mở lần chạy **Build and publish Docker image** tương ứng với commit vừa push.
4. Chờ bước **Build and publish image** thành công. Lần đầu phải tải dependencies và build hai kiến trúc nên có thể mất vài phút.
5. Mở [Packages của tài khoản](https://github.com/phatcaoduc2910?tab=packages), chọn package `kieumainotifier` và kiểm tra tag `latest`.

Nếu workflow lỗi, mở step màu đỏ và đọc lỗi đầu tiên có liên quan. Lỗi `permission_denied: write_package` thường cần kiểm tra quyền Actions của repository và quyền ghi của repository đối với package đã tồn tại. Workflow đã khai báo `packages: write`.

Workflow hiện hoạt động như sau:

- Push nhánh `main`: build và publish các tag `latest`, `main` và `sha-...`.
- Push tag dạng `v1.0.0`: build và publish tag `1.0.0` cùng `sha-...`; không thay đổi `latest`.
- Pull request vào `main`: chỉ build kiểm tra, không đăng nhập GHCR hoặc publish.
- Chạy thủ công: dùng `workflow_dispatch`. Nút **Run workflow** chỉ có khi tệp workflow đã tồn tại trên nhánh mặc định của repository. Lần đầu với nhánh `main`, dùng sự kiện push ở bước 2.

Các action được ghim theo commit SHA; chú thích cạnh mỗi action ghi phiên bản major. Dockerfile chạy Maven `verify`; workflow chỉ publish khi bộ test và build image thành công.

Workflow hiện phát hành từ `main`. Nếu sau này chọn nhánh phát hành khác, cập nhật cả hai danh sách `branches` và điều kiện tạo tag `latest` cho khớp.

## 4. Chọn quyền tải image

Muốn người nhận tải mà không phải đăng nhập GitHub:

1. Mở package `kieumainotifier` trong tab **Packages** của tài khoản.
2. Chọn **Package settings**.
3. Tại **Danger Zone**, chọn **Change visibility**, rồi **Public** và xác nhận.

Public cho phép bất kỳ ai tải image. Theo GitHub, package đã chuyển sang Public không thể chuyển lại Private. Chỉ chọn Public khi bạn muốn công khai image. Bạn có thể giữ Private và cấp quyền đọc cho người nhận; khi đó người nhận cần đăng nhập GHCR trước khi pull.

## 5. Người nhận chạy ứng dụng

Chỉ thực hiện sau khi workflow đã publish thành công. Máy nhận cần Docker Desktop đang chạy, hoặc Docker Engine kèm Docker Compose. Không cần cài Java hay Maven trên máy nhận.

Tải repository bằng ZIP hoặc clone nhánh chứa các file Docker:

```powershell
git clone --branch main https://github.com/phatcaoduc2910/KieuMaiNotifier.git
cd KieuMaiNotifier
Copy-Item .env.example .env
```

Nếu `.env` đã tồn tại, giữ tệp đó và chỉnh sửa thay vì ghi đè. Đặt `POSTGRES_PASSWORD` và `JWT_SECRET` bằng giá trị riêng, với JWT secret ngẫu nhiên ít nhất 32 byte. `APP_IMAGE` mặc định là image `latest` ở trên.

Sau khi lưu `.env`, chạy:

```powershell
docker compose -f compose.ghcr.yaml pull
docker compose -f compose.ghcr.yaml up -d
docker compose -f compose.ghcr.yaml logs -f api
```

Khi API khởi động xong, kiểm tra:

```powershell
(Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/swagger-ui/index.html').StatusCode
```

Kết quả mong đợi là `200`. Với API yêu cầu xác thực, đăng nhập qua `POST /auth/login` bằng `username` và `password`, lấy trường `token` trong response và gửi `Authorization: Bearer <token>` khi gọi `/reports`. Database mới cần tạo tài khoản qua `/auth/register` trước. `401` từ endpoint yêu cầu đăng nhập không đồng nghĩa container bị lỗi.

pgAdmin trên máy nhận kết nối `localhost:5434`, database `kieumai_notifier`, user `postgres`, mật khẩu theo `.env`. Nếu đổi các giá trị trong `.env`, dùng giá trị đã đổi. Database mới được khởi tạo rỗng; dữ liệu của người phát triển không nằm trong image.

`compose.yaml` và `compose.ghcr.yaml` dùng cùng tên project và volume. Trên cùng một máy, chọn một cách chạy; khi chuyển cách chạy, database hiện có được giữ lại. Không chạy hai bản như hai hệ thống độc lập với cùng tên project.

## 6. Cập nhật ứng dụng

Người phát triển push thay đổi mới lên `main`, rồi đợi workflow publish xong. Người nhận cập nhật bằng:

```powershell
docker compose -f compose.ghcr.yaml pull api
docker compose -f compose.ghcr.yaml up -d api
```

Volume PostgreSQL được giữ lại. Với một bản phát hành cần cố định, đặt `APP_IMAGE=ghcr.io/phatcaoduc2910/kieumainotifier:1.0.0` sau khi tag đó đã được publish, rồi chạy lại các lệnh cập nhật. Việc nâng phiên bản ứng dụng có thay đổi schema vẫn cần kế hoạch migration và backup phù hợp.

## Tài liệu tham khảo

- [GitHub: Publishing Docker images](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images).
- [GitHub: Quyền và visibility của package](https://docs.github.com/en/packages/learn-github-packages/configuring-a-packages-access-control-and-visibility).
- [GitHub: Chạy workflow thủ công](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow).
