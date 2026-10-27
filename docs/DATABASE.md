# Database trên beta

Ứng dụng dùng PostgreSQL với sáu bảng nghiệp vụ: `users`, `wards`, `incidents`, `reports`, `subscriptions` và `report_updates`. Sơ đồ trong [README](../README.md) là mô hình hiện tại.

## Khởi tạo database mới

Yêu cầu JDK 21 và PostgreSQL đang chạy. Trong pgAdmin, kết nối database `postgres` và chạy riêng:

```sql
CREATE DATABASE kieumai_notifier_v2;
```

Tạo hoặc chỉnh `application-local.properties` ở thư mục gốc dự án:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/kieumai_notifier_v2
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
jwt.secret=YOUR_RANDOM_SECRET_AT_LEAST_32_BYTES
```

Đổi host, cổng và tài khoản theo PostgreSQL của bạn. Tệp này được Git bỏ qua. Chạy ứng dụng:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway tự chạy [V1__create_notifier_schema.sql](../src/main/resources/db/migration/V1__create_notifier_schema.sql) trước khi Hibernate kiểm tra Entity bằng `ddl-auto=validate`. Bảng kỹ thuật `flyway_schema_history` lưu lịch sử migration ngoài sáu bảng nghiệp vụ. Không chạy V1 bằng tay rồi bật Flyway trên cùng schema.

Với Docker, đặt `POSTGRES_DB=kieumai_notifier_v2` trong `.env` trước khi khởi tạo volume mới. Nếu volume đã tồn tại, đổi biến này không tạo database: tạo database mới trên server trước, rồi tạo lại container API theo [DOCKER.md](DOCKER.md). Giữ nguyên volume có dữ liệu.

## Database cũ

V1 dành cho database/schema trống, không tự chuyển đổi các bảng Hibernate cũ.

Nếu schema cũ đã có bảng nhưng chưa có lịch sử Flyway, ứng dụng dừng để tránh coi schema không tương thích là hợp lệ. Không bật `baseline-on-migrate`, không đổi lại `ddl-auto=update` và không xóa volume để vượt qua lỗi. Tạo database mới để chạy mô hình mới; giữ database cũ cho việc chuyển dữ liệu có đối chiếu.

Các điểm cần xử lý khi chuyển dữ liệu lịch sử:

- Đổi `users.user_name` và `pass_word` thành `username` và `password_hash`; bảo toàn password hash, không mã hóa hash lần nữa.
- Bỏ cột role khỏi mô hình tài khoản. Tài khoản hiện có quyền ứng dụng `USER`; request đăng ký cũ có `role` và `created_At` được bỏ qua.
- Chuẩn hóa timestamp thành thời điểm có múi giờ; cần xác định múi giờ gốc của dữ liệu cũ.
- Report chờ có `incident_id=null` cần được gắn với incident `PENDING` trước khi áp dụng khóa ngoại bắt buộc.
- Lưu trữ/đối chiếu riêng dữ liệu `report_groups`, `report_grouping_locks`, `incident_confirmations`, `report_images`, `location_description` và notification cũ. Phiên bản mới không tạo các bảng/cột đó.
- Nhập danh mục phường trước khi chuyển incident và subscription vì khóa ngoại được kiểm tra.

## Mapping và ràng buộc

- ID dùng `Long`, tự tăng bằng identity.
- Tọa độ dùng `BigDecimal` và `NUMERIC(10,7)`.
- `users.latitude`, `users.longitude` nullable khi chưa có vị trí; khi cập nhật phải có đủ cặp tọa độ. Database kiểm tra latitude trong [-90, 90], longitude trong [-180, 180].
- Thời gian dùng `OffsetDateTime` và `TIMESTAMP WITH TIME ZONE`.
- `username`, `wards.code` và cặp subscription `(user_id, ward_id)` có ràng buộc duy nhất.
- Có đủ bảy khóa ngoại. Mọi report phải tham chiếu incident; `alerted_at`, `deleted_at` được phép null.
- `report_updates` có Entity và repository để lưu nội dung, tác giả và thời điểm bổ sung.
- Subscription repository đã có adapter JPA, thay cho interface chưa có triển khai.

Chỉ bổ sung hai cột tọa độ user vào mô hình. Phần lọc vòng tròn 1 km và API cập nhật vị trí không nằm trong thay đổi này.

Schema mới không tự điền danh mục hành chính. Nhập danh mục thật vào `wards(code, name, province_name)` trước khi tạo report. Không dùng ID phường ở database cũ cho database mới nếu chưa có ánh xạ. Ví dụ dành riêng cho môi trường thử nghiệm:

```sql
INSERT INTO wards (code, name, province_name)
VALUES ('TEST-001', 'Phường thử nghiệm', 'Tỉnh thử nghiệm');
```

## Điều chỉnh code hiện có

Report đầu tạo incident `PENDING` và luôn có `incidentId`. Đủ ba report chưa xóa cùng sự cố thì trạng thái chuyển sang `ACTIVE`. Giữ quy tắc gom report cùng phường/loại trong 500 m và thời hạn bốn giờ. Khóa row phường trong transaction thay cho bảng gom/bảng khóa riêng.

Request report không còn `imageUrls` và `locationDescription`. Chi tiết incident trả `reportCount`; bỏ `confirmationCount`, `confirmedByMe` và các endpoint confirmation riêng. Những thay đổi này loại bỏ phụ thuộc vào các bảng không còn trong sơ đồ.

## Kiểm thử database

Chỉ chạy bộ kiểm thử database:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=DatabaseIntegrationTests" test
```

Mặc định dùng database H2 riêng ở chế độ PostgreSQL và chạy cùng migration Flyway. Bộ test kiểm tra:

1. Flyway tạo đúng sáu bảng nghiệp vụ và Hibernate map đúng sáu Entity.
2. Danh sách cột của từng bảng khớp sơ đồ.
3. Sinh ID và thời điểm tạo.
4. Đọc các quan hệ qua đủ bảy khóa ngoại.
5. Từ chối tham chiếu không tồn tại và xóa parent đang được dùng.
6. Ràng buộc duy nhất.
7. Các cột nullable và incident bắt buộc của report.
8. Độ chính xác và giá trị biên tọa độ.
9. Từ chối tọa độ sai hoặc thiếu một phần vị trí user.
10. Múi giờ và nội dung Unicode dạng text.
11. Lưu xóa mềm và truy vấn loại report đã xóa.
12. Adapter subscription thực sự ghi/đọc/xóa qua JPA.
13. Rollback toàn bộ transaction khi có lỗi khóa ngoại.

Để chạy trên PostgreSQL riêng, truyền các system property `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `spring.datasource.driver-class-name=org.postgresql.Driver`, `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect` và `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect` cho Maven.

Bộ kiểm thử xóa dữ liệu fixture giữa các test. Chỉ trỏ test vào database riêng, không dùng database chứa dữ liệu cần giữ.

## Kết quả kiểm tra ngày 27/10/2026

- PostgreSQL 18.6 trên cluster tạm riêng: 13/13 test database đạt; build JAR thành công.
- H2 ở chế độ PostgreSQL: 13/13 test database đạt.
- Phạm vi kiểm tra cuối cùng là database; không chạy lại toàn bộ API suite.
