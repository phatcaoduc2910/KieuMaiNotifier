# Module report theo schema sáu bảng

Cấu trúc database và cách khởi tạo được mô tả trong [DATABASE.md](DATABASE.md).

Report đầu tạo incident `PENDING` và liên kết bằng `reports.incident_id NOT NULL`. Đủ ba report chưa xóa cùng incident thì trạng thái chuyển sang `ACTIVE`. Tâm incident giữ tọa độ report đầu, gom trong bán kính 500 m, cùng phường và loại. Report mới gia hạn thêm bốn giờ; incident đóng/hết hạn không nhận report mới.

Khóa row phường trong cùng transaction thay cho `report_groups` và `report_grouping_locks`. Không còn bảng xác nhận riêng hoặc bảng ảnh. Request report không còn `imageUrls` và `locationDescription`.

API tạo/đọc/xóa report giữ đường dẫn cũ. Entity và repository của `report_updates` đã có trong persistence; chưa thêm API bổ sung diễn biến.

Script `docs/sql/report-pending-incidents.sql` là tài liệu lịch sử dành cho phiên bản SQL Server cũ, không dùng cho schema PostgreSQL hiện tại. Migration tạo schema mới nằm tại `src/main/resources/db/migration/V1__create_notifier_schema.sql`.

Phần lọc 1 km quanh user không nằm trong module này. Cấu hình bán kính 500 m hiện có chỉ phục vụ gom report thành incident.
