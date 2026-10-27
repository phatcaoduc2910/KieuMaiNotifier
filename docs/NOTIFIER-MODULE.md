# Module theo dõi phường

Schema và cách khởi tạo database xem [DATABASE.md](DATABASE.md). Module dùng bảng `subscriptions`, đọc `wards` và `incidents`, không ghi vào `incidents`.

| Method | Đường dẫn | Body | Kết quả | Lỗi |
| --- | --- | --- | --- | --- |
| `POST` | `/me/subscriptions` | `{"wardId": 1}` | `201` `{id, wardId, createdAt}` | `400`, `401`, `404 WARD_NOT_FOUND`, `409 ALREADY_SUBSCRIBED` |
| `GET` | `/me/subscriptions` | | `200` `[{id, wardId, createdAt}]` | `401` |
| `DELETE` | `/me/subscriptions/{wardId}` | | `204` | `401`, `404 SUBSCRIPTION_NOT_FOUND` |
| `GET` | `/wards` | | `200` `[{id, code, name, provinceName}]` theo tỉnh rồi tên | `401` |
| `GET` | `/me/feed` | | `200` `[{incidentId, wardId, type, title, latitude, longitude, createdAt, expiresAt}]` | `401` |

Mọi API cần JWT. `userId` lấy từ token qua `CurrentUserProvider`, client không gửi. Lỗi trả cùng định dạng với module report (`timestamp, status, code, message, path, fieldErrors`).

Một user theo dõi mỗi phường một lần. Service kiểm tra trước khi lưu; hai request đồng thời lọt qua bước này thì ràng buộc `uk_subscriptions_user_ward` chặn và API vẫn trả `409`.

`/me/feed` trả các incident `ACTIVE` thuộc những phường user đang theo dõi, mới nhất trước. Feed được truy vấn lại mỗi lần gọi nên không có bảng thông báo, incident hết hạn tự rời khỏi feed. Không lưu trạng thái đã đọc. Module không dùng `incidents.alerted_at`.

Phần tìm incident quanh vị trí user không nằm trong module này.

`/wards` cho client lấy `wardId` để gọi `POST /me/subscriptions`.

Kiểm thử: `FeedServiceTest`, `WardServiceTest` (bản giả, không cần Spring) và `NotifierIntegrationTests` (HTTP + JWT trên H2).
