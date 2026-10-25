# Module report trên nhánh report

Module này bổ sung model, entity, repository, DTO, validation và API tạo/đọc/xóa phản ánh trên nền `beta` tại commit `6765215`. Logic tạo incident từ report nằm trong `ReportService`; các file của module incident, auth và subscription giữ nguyên so với mốc này.

## Trạng thái API

Tất cả API bên dưới yêu cầu đăng nhập. ID là số nguyên `Long`; đường dẫn chưa có tiền tố `/api/v1`.

| API | Trạng thái hiện tại |
| --- | --- |
| `POST /reports` | Lưu report, trả `201`; đủ 3 report trong nhóm thì tạo incident và liên kết cả nhóm. |
| `GET /reports` | Đọc report chưa xóa; lọc kết hợp theo `wardId`, `type`, `incidentId`; có phân trang. |
| `GET /me/reports` | Cùng bộ lọc, chỉ trả report thuộc tài khoản đang đăng nhập. |
| `DELETE /reports/{reportId}` | Chỉ chủ sở hữu được xóa mềm, trả `204` không có body. |

## Tạo incident từ 3 report

- Hai report đầu được lưu với `incidentId=null`. Mỗi report lưu riêng `wardId` và `type`, nên vẫn xem, lọc và xóa được trước khi có incident.
- Gom report cùng phường, cùng loại và trong bán kính 500 m tính từ tọa độ report đầu tiên của nhóm. Chọn nhóm gần nhất; bằng khoảng cách thì chọn ID nhóm nhỏ nhất. Tâm nhóm không dịch chuyển khi có report mới.
- Report hợp lệ thứ 3 tạo một incident `ACTIVE`, rồi cập nhật `incidentId` cho toàn bộ report chưa xóa của nhóm trong cùng giao dịch. Đếm số report, không yêu cầu 3 tài khoản khác nhau. Không tự tạo bản ghi xác nhận.
- Report thứ 4 trở đi tiếp tục gắn vào incident đó; không tạo incident mới theo từng lô 3. Report mới gia hạn incident thêm 4 giờ tính từ lúc xử lý, phù hợp thời hạn hiện có của module incident.
- Incident đã kết thúc hoặc hết hạn không nhận report mới. Report cùng vị trí bắt đầu nhóm chờ mới; đủ 3 lại tạo incident mới. Nhóm chưa đủ 3 hiện chưa có quy tắc hết hạn riêng.
- Xóa report đang chờ khiến report đó không còn được tính vào ngưỡng 3. Xóa report sau khi đã tạo incident không xóa hoặc hạ trạng thái incident.
- Khóa database theo phường và loại ngăn nhiều request đồng thời tạo incident trùng. Tọa độ chính xác của tâm gom được lưu trong bảng riêng `report_groups`; mapping tọa độ trong entity incident của module khác giữ nguyên.
- Các report cũ chưa thuộc `report_groups` vẫn đọc/xóa được; không tự động gom lại dữ liệu lịch sử. Khi cần chuyển dữ liệu cũ vào nhóm, phải chốt quy tắc chuyển đổi riêng.

Cấu hình tùy chọn: `notifier.report.grouping-radius-meters=500`, `notifier.report.incident-lifetime-seconds=14400`. Ngưỡng tạo incident cố định là 3 report.

## Dữ liệu và quyền truy cập

- Request tạo report giữ các trường `wardId`, `type`, `description`, `latitude`, `longitude`, `observedAt`; hỗ trợ thêm `locationDescription` và `imageUrls` tùy chọn.
- Loại phản ánh: `FLOOD`, `TRAFFIC_JAM`, `OTHER`. Tọa độ phải hợp lệ; thời điểm quan sát có múi giờ và không nằm trong tương lai. Tối đa 5 URL ảnh HTTPS.
- Client không được gửi `userId` hay trường ngoài schema để thay danh tính từ token.
- Danh sách trả `{items, page, size, totalElements, totalPages}`; mặc định `page=0`, `size=20`, tối đa 100; sắp xếp `createdAt DESC, id DESC`.
- `wardId` hiện chỉ là bộ lọc: không có dữ liệu phù hợp trả trang rỗng. Chưa tích hợp kiểm tra danh mục phường. `incidentId` không tồn tại trả `404 INCIDENT_NOT_FOUND` qua repository hiện có.
- Report đã xóa bị ẩn khỏi cả hai danh sách. Xóa report của người khác trả `403 REPORT_NOT_OWNED`; report không tồn tại hoặc đã xóa trả `404 REPORT_NOT_FOUND`.
- Xóa mềm không sửa incident, thời hạn hay xác nhận. Dữ liệu ảnh được lưu trong `report_images` theo thứ tự gửi.

## Database đã có dữ liệu

Nếu database SQL Server đã được tạo từ bản report trước, chạy [report-pending-incidents.sql](sql/report-pending-incidents.sql) trước khi khởi động phiên bản mới. Script cho phép `reports.incident_id` nhận `NULL` và điền `wardId`/`type` từ incident cho report cũ; không sửa bảng incident.

Hibernate với cấu hình `ddl-auto=update` hiện có sẽ tạo các bảng `report_groups`, `report_grouping_locks` và quan hệ nhóm của report. Không chỉ dựa vào `ddl-auto=update` để gỡ ràng buộc `NOT NULL` trên cột có sẵn. Database tạo mới không cần script này.

## Kiểm thử

Chạy bằng JDK 21:

```powershell
.\mvnw.cmd clean verify -B -ntp
```

`ReportIntegrationTests` dùng H2 để kiểm tra ngưỡng 3, liên kết report cũ và report tiếp theo, điều kiện gom, sự cố kết thúc/hết hạn, gửi đồng thời, đọc/lọc/phân trang, quyền sở hữu, xóa mềm, validation và đăng nhập. Chưa chạy migration hoặc kiểm chứng trên SQL Server thật.
