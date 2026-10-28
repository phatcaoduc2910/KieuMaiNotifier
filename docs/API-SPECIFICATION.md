# Đặc tả API — Kiều Mai Notifier (OpenAPI 3.1.0)

> Tự động quét từ mã nguồn Spring Boot bằng `springdoc-openapi-starter-webmvc-ui` (`v3.1.1`).
> - File gốc JSON: `docs/openapi.json` (`/v3/api-docs`)
> - File gốc YAML: `docs/openapi.yaml` (`/v3/api-docs.yaml`)
> - Giao diện trực quan: `http://localhost:8080/swagger-ui/index.html`

## 1. Thông tin chung & Xác thực
- **Base URL**: `http://localhost:8080`
- **Cơ chế xác thực (`bearerAuth`)**: HTTP Bearer JWT (`Authorization: Bearer <token>`)
- **API công khai (không cần token)**:
  - `POST /auth/register`
  - `POST /auth/login`
- **Tất cả API còn lại**: Bắt buộc gửi kèm JWT Header.

---

## 2. Danh mục 16 Endpoints theo Controller

### 2.1. Auth (`AuthController`)
| Method | Path | Auth | Request Body | Response | Mô tả |
|---|---|---|---|---|---|
| `POST` | `/auth/register` | Public | `RegisterRequest` | `201 Created` / `409 Conflict` (`String`) | Đăng ký tài khoản mới |
| `POST` | `/auth/login` | Public | `LoginRequest` | `200 OK` / `401 Unauthorized` (`LoginResponse`) | Đăng nhập nhận JWT token |

### 2.2. Thông tin cá nhân (`MeController`)
| Method | Path | Auth | Tham số | Response | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/me` | Bearer JWT | Không | `200 OK` (`MeResponse`) | Lấy `userId` và `username` của user hiện tại |

### 2.3. Danh mục Phường/Xã (`WardController`)
| Method | Path | Auth | Tham số | Response | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/wards` | Bearer JWT | Không | `200 OK` (`List<WardResponse>`) | Danh sách các phường/xã (tự động seed 12 phường Hà Nội khi khởi động) |

### 2.4. Đăng ký theo dõi Phường/Xã (`SubscriptionController`)
| Method | Path | Auth | Tham số / Body | Response | Mô tả |
|---|---|---|---|---|---|
| `POST` | `/me/subscriptions` | Bearer JWT | Body: `SubscribeRequest` | `201 Created` (`SubscriptionResponse`) | Đăng ký nhận cảnh báo tại 1 phường |
| `GET` | `/me/subscriptions` | Bearer JWT | Không | `200 OK` (`List<SubscriptionResponse>`) | Danh sách các phường đang theo dõi |
| `DELETE` | `/me/subscriptions/{wardId}` | Bearer JWT | Path: `wardId` (`long`) | `204 No Content` | Huỷ theo dõi phường |

### 2.5. Báo cáo sự cố (`ReportController`)
| Method | Path | Auth | Tham số / Body | Response | Mô tả |
|---|---|---|---|---|---|
| `POST` | `/reports` | Bearer JWT | Body: `CreateReportRequest` | `201 Created` (`ReportDto`) | Tạo báo cáo mới, tự động gom nhóm bán kính `1000m` & kích hoạt sự cố khi đủ `>= 3` người dùng khác nhau |
| `GET` | `/reports` | Bearer JWT | Query: `wardId`, `type`, `incidentId`, `page=0`, `size=20` | `200 OK` (`PageResponse<ReportDto>`) | Danh sách báo cáo (có lọc & phân trang) |
| `GET` | `/me/reports` | Bearer JWT | Query: `wardId`, `type`, `incidentId`, `page=0`, `size=20` | `200 OK` (`PageResponse<ReportDto>`) | Danh sách báo cáo do chính user hiện tại gửi |
| `DELETE` | `/reports/{reportId}` | Bearer JWT | Path: `reportId` (`long`) | `204 No Content` | Xoá mềm báo cáo của chính mình (`deleted_at`) |
| `POST` | `/reports/{reportId}/updates` | Bearer JWT | Path: `reportId`, Body: `CreateReportUpdateRequest` | `201 Created` (`ReportUpdateDto`) | Thêm cập nhật bổ sung cho báo cáo của chính mình |
| `GET` | `/reports/{reportId}/updates` | Bearer JWT | Path: `reportId` (`long`) | `200 OK` (`List<ReportUpdateDto>`) | Xem lịch sử cập nhật của 1 báo cáo |

### 2.6. Sự cố (`IncidentController`)
| Method | Path | Auth | Tham số / Body | Response | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/incidents` | Bearer JWT | Query: `wardId`, `type` | `200 OK` (`List<IncidentResponseDto>`) | Danh sách các sự cố đang `ACTIVE` |
| `GET` | `/incidents/{id}` | Bearer JWT | Path: `id` (`long`) | `200 OK` (`IncidentDetailDto`) | Chi tiết 1 sự cố |
| `POST` | `/incidents/{id}/updates` | Bearer JWT | Path: `id` (`long`) | `200 OK` | Đánh dấu sự cố kết thúc sớm (`RESOLVED`) |
| `GET` | `/incidents/feed` | Bearer JWT | Query: `userId` (`long`) | `200 OK` (`List<IncidentResponseDto>`) | Lấy danh sách sự cố `ACTIVE` theo `userId` |

### 2.7. Bảng tin cảnh báo (`FeedController`)
| Method | Path | Auth | Tham số | Response | Mô tả |
|---|---|---|---|---|---|
| `GET` | `/me/feed` | Bearer JWT | Không | `200 OK` (`List<FeedItemResponse>`) | Bảng tin các sự cố `ACTIVE` chưa hết hạn tại các phường user đang theo dõi |

---

## 3. Đặc tả Schema DTO & Ràng buộc dữ liệu (Validation)

### `RegisterRequest`
- `username` (`string`, bắt buộc, `maxLength: 100`)
- `password` (`string`, bắt buộc, `minLength: 8`, `maxLength: 128`)
- `displayName` (`string`, tuỳ chọn, `maxLength: 255`)

### `LoginRequest` / `LoginResponse`
- `LoginRequest`: `username` (`string`), `password` (`string`)
- `LoginResponse`: `token` (`string`), `message` (`string`)

### `CreateReportRequest`
- `wardId` (`int64`, bắt buộc, `> 0`)
- `type` (`string`, bắt buộc, khớp biểu thức: `FLOOD|TRAFFIC_JAM|OTHER`)
- `description` (`string`, bắt buộc, `maxLength: 2000`)
- `latitude` (`number`, bắt buộc, `-90 <= latitude <= 90`)
- `longitude` (`number`, bắt buộc, `-180 <= longitude <= 180`)
- `observedAt` (`date-time` ISO-8601, bắt buộc, không vượt quá thời điểm hiện tại + 5 phút)

### `CreateReportUpdateRequest`
- `content` (`string`, bắt buộc, không rỗng, `maxLength: 2000`)

### `SubscribeRequest`
- `wardId` (`int64`, bắt buộc, `> 0`)

### Các DTO phản hồi (`Response Schemas`)
- `ReportDto`: `id`, `userId`, `incidentId`, `wardId`, `type`, `description`, `latitude`, `longitude`, `observedAt`, `createdAt`
- `ReportUpdateDto`: `id`, `reportId`, `userId`, `content`, `createdAt`
- `PageResponseReportDto`: `items` (`List<ReportDto>`), `page`, `size`, `totalElements`, `totalPages`
- `IncidentResponseDto`: `id`, `wardId`, `type`, `status`, `latitude`, `longitude`, `title`, `reportCount`, `expiresAt`
- `IncidentDetailDto`: `id`, `wardId`, `type`, `status`, `latitude`, `longitude`, `title`, `reportCount`, `alertedAt`, `expiresAt`, `createdAt`
- `FeedItemResponse`: `incidentId`, `wardId`, `type`, `title`, `latitude`, `longitude`, `createdAt`, `expiresAt`
- `WardResponse`: `id`, `code`, `name`, `provinceName`
- `SubscriptionResponse`: `id`, `wardId`, `createdAt`
- `MeResponse`: `userId`, `username`
