# Module report trên nhánh report

Module này bổ sung model, entity, repository, DTO, validation và API đọc/xóa phản ánh trên nền `beta` tại commit `6765215`. Các file của module incident, auth và subscription giữ nguyên so với mốc này.

## Trạng thái API

Tất cả API bên dưới yêu cầu đăng nhập. ID là số nguyên `Long`; đường dẫn chưa có tiền tố `/api/v1`.

| API | Trạng thái hiện tại |
| --- | --- |
| `POST /reports` | Kiểm tra request và danh tính; request hợp lệ trả `501 INCIDENT_INTEGRATION_PENDING`, chưa ghi dữ liệu. |
| `GET /reports` | Đọc report chưa xóa; lọc kết hợp theo `wardId`, `type`, `incidentId`; có phân trang. |
| `GET /me/reports` | Cùng bộ lọc, chỉ trả report thuộc tài khoản đang đăng nhập. |
| `DELETE /reports/{reportId}` | Chỉ chủ sở hữu được xóa mềm, trả `204` không có body. |

Tạo report đang chờ chốt cách tích hợp với module incident của Dũng. Chưa triển khai tạo/gom incident, gia hạn, xác nhận, diễn biến hoặc thông báo trong module report. API tạo không trả thành công giả và không tạo report thiếu liên kết incident.

## Dữ liệu và quyền truy cập

- Request tạo report giữ các trường `wardId`, `type`, `description`, `latitude`, `longitude`, `observedAt`; hỗ trợ thêm `locationDescription` và `imageUrls` tùy chọn.
- Loại phản ánh: `FLOOD`, `TRAFFIC_JAM`, `OTHER`. Tọa độ phải hợp lệ; thời điểm quan sát có múi giờ và không nằm trong tương lai. Tối đa 5 URL ảnh HTTPS.
- Client không được gửi `userId` hay trường ngoài schema để thay danh tính từ token.
- Danh sách trả `{items, page, size, totalElements, totalPages}`; mặc định `page=0`, `size=20`, tối đa 100; sắp xếp `createdAt DESC, id DESC`.
- `wardId` hiện chỉ là bộ lọc: không có dữ liệu phù hợp trả trang rỗng. Chưa tích hợp kiểm tra danh mục phường. `incidentId` không tồn tại trả `404 INCIDENT_NOT_FOUND` qua repository hiện có.
- Report đã xóa bị ẩn khỏi cả hai danh sách. Xóa report của người khác trả `403 REPORT_NOT_OWNED`; report không tồn tại hoặc đã xóa trả `404 REPORT_NOT_FOUND`.
- Xóa mềm không sửa incident, thời hạn hay xác nhận. Dữ liệu ảnh được lưu trong `report_images` theo thứ tự gửi.

## Kiểm thử

Chạy bằng JDK 21:

```powershell
.\mvnw.cmd clean verify -B -ntp
```

`ReportIntegrationTests` dùng H2 và dữ liệu fixture để kiểm tra đọc/lọc/phân trang, quyền sở hữu, xóa mềm, validation, yêu cầu đăng nhập và phản hồi `501` không ghi dữ liệu. Test không triển khai nghiệp vụ incident. Chưa kiểm chứng trên SQL Server thật.
