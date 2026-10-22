# Kết quả gộp nhánh vào beta — 22/10/2026

## Phạm vi

Đã fetch từ `origin`, sau đó gộp các nhánh ngoài `main` vào `beta` local:

| Nhánh nguồn | Commit đã kiểm tra | Kết quả |
| --- | --- | --- |
| algorithm | `2331436` | Merge commit `32d4239`; giải quyết xung đột |
| notifier | `670e0b0` | Merge commit `3560e68`; không có xung đột |
| security | `345d118` | Đã có trong beta trước khi gộp |
| report | `74bc40d` | Đã có trong lịch sử beta trước khi gộp |

Không thực hiện merge trực tiếp từ `main` và không thay đổi con trỏ nhánh `main`. Những commit đã là tổ tiên của `notifier`, bao gồm commit README cũng có trên `main`, được giữ theo lịch sử Git của nhánh nguồn.

Đã tạo nhánh lưu mốc `codex/beta-before-merge-20261022` tại `345d118`. Các file tài liệu có sẵn chưa được Git theo dõi vẫn được giữ nguyên và không được đưa vào commit gộp. Chưa push lên GitHub.

## Xung đột và lỗi tích hợp đã sửa

Sáu file xung đột khi gộp `algorithm`:

- `pom.xml`: giữ bộ dependency đang hoạt động của beta; bộ này đã có JPA, MySQL, SQL Server và Lombok cần cho module sự cố.
- `AuthService.java`, `SecurityConfig.java`, `AuthController.java`: giữ implementation đăng nhập và JWT hiện có, thay vì các lớp khung trống từ nhánh nguồn.
- `UserRepository.java`: thống nhất dùng entity `persistence.entity.User` của module đăng nhập. Sửa `IncidentService` và quan hệ xác nhận cùng dùng entity này; bỏ entity `UserEntity` trùng mục đích từ nhánh algorithm.
- `application.properties`: giữ cấu hình SQL Server và JWT hiện có của beta, thay vì ghi đè bằng cấu hình MySQL của algorithm.

Test HTTP sau khi gộp phát hiện module sự cố vẫn yêu cầu `userId` từ query. Request có JWT nhưng không có query này thất bại; client cũng có thể truyền ID của tài khoản khác. Đã sửa các API chi tiết, xác nhận, thu hồi và cập nhật sự cố trong `IncidentController` để lấy ID từ danh tính xác thực của Spring Security và tài khoản trong `UserRepository`.

## Kiểm chứng

Lần chạy ban đầu biên dịch được nhưng test khởi động thất bại vì SQL Server tại `localhost:1433` từ chối kết nối. Đã bổ sung H2 ở scope `test` và profile `test`, để kiểm thử bằng database tạm trong bộ nhớ, không phụ thuộc database thật. Cấu hình chạy ứng dụng mặc định không bị thay đổi.

Trước khi sửa danh tính ở controller, bộ test có **5 lỗi / 16 test**. Sau khi sửa:

```powershell
$env:JAVA_HOME = 'C:\Users\Yuna\.jdks\ms-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd verify -B -ntp
```

Kết quả: **BUILD SUCCESS — 16 test, 0 failure, 0 error, 0 skipped**. Đã tạo `target/notifier-0.0.1-SNAPSHOT.jar`.

- 9 test tích hợp HTTP: đăng ký/đăng nhập rồi xác nhận bằng JWT; query userId không thể giả danh người khác; xác nhận trùng và thu hồi; ba người xác nhận kích hoạt sự cố theo hành vi hiện có; lọc danh sách; kết thúc sự cố; tác vụ hết hạn; chặn request chưa xác thực/token sai; từ chối tài khoản trùng và mật khẩu sai.
- 6 unit test của SubscriptionService: phường không tồn tại; đăng ký trùng; dữ liệu đăng ký mới; hủy đăng ký không tồn tại của người yêu cầu; hủy thành công; lấy danh sách theo userId. Repository được mock trong nhóm test này.
- 1 test khởi động Spring ApplicationContext.

Log kiểm tra nằm ở `target/beta-merge-verify.log`; báo cáo test ở `target/surefire-reports/`. Đây là file sinh tự động, không được commit.

## Phần còn thiếu và giới hạn kiểm tra

Những điểm dưới đây đã tồn tại trong code các nhánh nguồn; chưa triển khai lại nghiệp vụ trong lần gộp này:

1. **Database thật chưa được kiểm chứng.** SQL Server mặc định không chạy tại thời điểm kiểm tra. H2 ở chế độ tương thích SQL Server không chứng minh mọi hành vi trên SQL Server hoặc MySQL đều đúng.
2. **Module report chưa có chức năng.** `ReportController`, `ReportService`, `ReportRepository` và các lớp dữ liệu còn là khung trống. Nhánh `report` hiện chỉ trỏ tới commit khởi tạo dependency.
3. **Module subscription chưa được nối vào ứng dụng web.** `SubscriptionService` chưa được đăng ký bằng `@Service` hoặc cấu hình bean; `SubscriptionRepository` và `WardRepository` chỉ là interface, chưa có implementation; chưa có controller theo dõi. Unit test chỉ xác minh logic service với mock, không chứng minh API theo dõi đã hoạt động.
4. **Implementation sự cố chưa khớp toàn bộ draft API.** Code hiện có dùng `/incidents`, ID kiểu Long, chuyển PENDING thành ACTIVE khi đủ ba người và gia hạn bốn giờ. Draft tài liệu trong workspace dùng các đường dẫn dựa trên `/reports/{reportId}`, UUID và vòng đời khác. Lần gộp giữ nghiệp vụ nhánh nguồn để nhóm chốt contract riêng.
5. **Cần hoàn thiện kiểm tra trạng thái và xử lý lỗi.** `confirmIncident` chưa chặn xác nhận sự cố đã kết thúc/hết hạn; lỗi không tìm thấy còn ném RuntimeException; chưa có bộ xử lý lỗi tập trung. Chưa kiểm chứng tình huống ghi đồng thời.
6. **Phân quyền theo vai trò còn chưa hoàn chỉnh.** Đăng ký nhận role từ request; `JwtService` ghi cứng claim role là ADMIN, trong khi filter chỉ dựng Authentication từ username. Bộ test hiện kiểm tra danh tính và yêu cầu đăng nhập, chưa xác minh chính sách quyền theo role.

Kết quả build/test xác nhận các luồng được liệt kê ở trên hoạt động sau khi gộp; không có nghĩa toàn bộ 16 thao tác trong draft đã được triển khai hoặc toàn bộ nghiệp vụ không còn lỗi.
