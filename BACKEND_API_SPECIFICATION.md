# 📡 TÀI LIỆU TOÀN DIỆN 100% API BACKEND DÀNH CHO CLIENT

> **Dự án:** School Social Network & Crowdsourced Question Bank (SQB)  
> **Phiên bản:** 1.0.0-SNAPSHOT  
> **Server Base URL:** `http://localhost:8080/api/v1`  
> **WebSocket Handshake URL:** `http://localhost:8080/api/v1/ws`  
> **Quy ước đặt tên (Naming Convention):** `snake_case` cho toàn bộ Request & Response JSON (theo Jackson config).  
> **Kiến trúc phản hồi (Response Envelope):** Toàn bộ API trả về cấu trúc chuẩn `GlobalResponse<T>`.

---

## 📑 MỤC LỤC & BẢNG THỐNG KÊ TỔNG THỂ

Hệ thống cung cấp **129 REST HTTP Endpoints**, **1 Spring Error Dispatcher**, **3 Inbound STOMP Destinations (kèm 2 Outbound Broadcast Topics)**, và **6 Actuator/Swagger Monitoring Endpoints**.

|      STT      | Phân hệ (Module)                                                                                      |  Số lượng Endpoint   | Controller phụ trách                                                                                                                                                                      |
|:-------------:|:------------------------------------------------------------------------------------------------------|:--------------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|       1       | [Xác thực & Ủy quyền (Auth)](#1-xác-thực--ủy-quyền-auth)                                              |          4           | [AuthController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/auth/controller/AuthController.java)                                  |
|       2       | [Quản trị người dùng (Admin Auth)](#2-quản-trị-người-dùng-admin-auth)                                 |          5           | [AdminController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/auth/controller/AdminController.java)                                |
|       3       | [Hồ sơ người dùng (User Profile)](#3-hồ-sơ-người-dùng-user-profile)                                   |          4           | [UserController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/user/controller/UserController.java)                                  |
|       4       | [Bài viết & Video Posts (Post & Feed)](#4-bài-viết--video-posts-post--feed)                           |          8           | [PostController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/post/controller/PostController.java)                                  |
|       5       | [Bình luận (Comments)](#5-bình-luận-comments)                                                         |          4           | [CommentController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/socialinteraction/controller/CommentController.java)               |
|       6       | [Cảm xúc (Reactions)](#6-cảm-xúc-reactions)                                                           |          1           | [ReactController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/socialinteraction/controller/ReactController.java)                   |
|       7       | [Bạn bè (Friendships)](#7-bạn-bè-friendships)                                                         |          7           | [FriendshipController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/friendship/controller/FriendshipController.java)                |
|       8       | [Theo dõi (Follows)](#8-theo-dõi-follows)                                                             |          5           | [FollowController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/follow/controller/FollowController.java)                            |
|       9       | [Chặn người dùng (Blocks)](#9-chặn-người-dùng-blocks)                                                 |          3           | [BlockController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/block/controller/BlockController.java)                               |
|      10       | [Hội thoại chat & Kho ẩn (Conversations)](#10-hội-thoại-chat--kho-ẩn-conversations)                   |          14          | [ConversationController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/ConversationController.java)          |
|      11       | [Tin nhắn (Messages)](#11-tin-nhắn-messages)                                                          |          2           | [MessageController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/MessageController.java)                    |
|      12       | [WebSocket STOMP Thời gian thực (Realtime Chat)](#12-websocket-stomp-thời-gian-thực-realtime-chat)    | 3 Inbound + 2 Topics | [ChatWebSocketController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/ChatWebSocketController.java)        |
|      13       | [Thông báo & Cấu hình Push (Notifications)](#13-thông-báo--cấu-hình-push-notifications)               |          5           | [NotificationController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/notification/controller/NotificationController.java)          |
|      14       | [Bảng tin hoạt động (Activity Feed)](#14-bảng-tin-hoạt-động-activity-feed)                            |          2           | [ActivityFeedController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/activityfeed/controller/ActivityFeedController.java)          |
|      15       | [Ngân hàng câu hỏi - Phiên đề xuất (Submissions)](#15-ngân-hàng-câu-hỏi---phiên-đề-xuất-submissions)  |          6           | [SessionController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/SessionController.java)                         |
|      16       | [Duyệt & Biên tập câu hỏi (Question Review)](#16-duyệt--biên-tập-câu-hỏi-question-review)             |          6           | [QuestionReviewController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/QuestionReviewController.java)           |
|      17       | [Luyện tập & Đánh giá câu hỏi (Question Practice)](#17-luyện-tập--đánh-giá-câu-hỏi-question-practice) |          5           | [QuestionInteractionController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/QuestionInteractionController.java) |
|      18       | [Quản lý Lớp học phần (Course Classes)](#18-quản-lý-lớp-học-phần-course-classes)                      |          7           | [CourseClassController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/CourseClassController.java)                 |
|      19       | [Quản trị Môn học (Admin Subjects)](#19-quản-trị-môn-học-admin-subjects)                              |          2           | [AdminSubjectController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/AdminSubjectController.java)               |
|      20       | [Quản trị Học kỳ (Admin Semesters)](#20-quản-trị-học-kỳ-admin-semesters)                              |          4           | [AdminSemesterController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/AdminSemesterController.java)             |
|      21       | [Tạo & Xuất đề thi (Exams)](#21-tạo--xuất-đề-thi-exams)                                               |          4           | [ExamController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/exam/controller/ExamController.java)                                  |
|      22       | [Xuất ngân hàng câu hỏi (Lecturer Export)](#22-xuất-ngân-hàng-câu-hỏi-lecturer-export)                |          1           | [QuestionExportController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/exam/controller/QuestionExportController.java)              |
|      23       | [Gamification - Dự đoán & BXH & Điểm danh](#23-gamification---dự-đoán--bxh--điểm-danh)                |          11          | [GamificationController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/gamification/controller/GamificationController.java)          |
|      24       | [Quản trị Bảng xếp hạng (Admin Leaderboards)](#24-quản-trị-bảng-xếp-hạng-admin-leaderboards)          |          1           | [AdminLeaderboardController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/gamification/controller/AdminLeaderboardController.java)  |
|      25       | [Huy hiệu thành tích (Badges)](#25-huy-hiệu-thành-tích-badges)                                        |          4           | [BadgeController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/badge/controller/BadgeController.java)                               |
|      26       | [Quản trị Huy hiệu (Admin Badges)](#26-quản-trị-huy-hiệu-admin-badges)                                |          5           | [AdminBadgeController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/badge/controller/AdminBadgeController.java)                     |
|      27       | [Vật phẩm trang trí & Cửa hàng (Cosmetics)](#27-vật-phẩm-trang-trí--cửa-hàng-cosmetics)               |          5           | [CosmeticController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/cosmetic/controller/CosmeticController.java)                      |
|      28       | [Tải lên Media & MinIO (Medias)](#28-tải-lên-media--minio-medias)                                     |          3           | [MediaController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/media/controller/MediaController.java)                               |
|      29       | [Quản lý Thiết bị & Đăng xuất (Devices)](#29-quản-lý-thiết-bị--đăng-xuất-devices)                     |          3           | [DeviceController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/device/controller/DeviceController.java)                            |
|      30       | [Báo cáo vi phạm (Reports)](#30-báo-cáo-vi-phạm-reports)                                              |          1           | [ReportController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/report/controller/ReportController.java)                            |
|      31       | [Tìm kiếm toàn cục (Search)](#31-tìm-kiếm-toàn-cục-search)                                            |          3           | [SearchController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/search/controller/SearchController.java)                            |
|      32       | [Tích hợp Trí tuệ Nhân tạo (AI Integration)](#32-tích-hợp-trí-tuệ-nhân-tạo-ai-integration)            |          5           | [AiIntegrationController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/ai/controller/AiIntegrationController.java)                  |
|      33       | [Xử lý lỗi hệ thống & Monitoring (System)](#33-xử-lý-lỗi-hệ-thống--monitoring-system)                 |          7           | [CustomErrorController.java](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/core/exception/CustomErrorController.java), Actuator, Swagger          |
| **Tổng cộng** | **TẤT CẢ PHÂN HỆ**                                                                                    |       **135+**       | **33 Controller Classes**                                                                                                                                                                 |

---

## 🛠️ CẤU TRÚC PHẢN HỒI CHUẨN TOÀN HỆ THỐNG (GLOBAL SCHEMAS)

### 1. Phản hồi chung (`GlobalResponse<T>`)
```json
{
  "code": "SUCCESS",
  "message": "Thành công",
  "data": {  }
}
```

### 2. Phân trang theo con trỏ (`CursorResponse<T>`)
```json
{
  "items": [  ],
  "pagination": {
    "before": 105,
    "after": 86,
    "has_prev": true,
    "has_next": true
  }
}
```

---

# CHI TIẾT TOÀN BỘ CÁC API

---

## 1. Xác thực & Ủy quyền (Auth)
Phụ trách bởi Controller: [`AuthController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/auth/controller/AuthController.java)  
Đường dẫn gốc: `/api/v1/auth`

### 1.1. Đăng nhập hệ thống
- **Method & URL:** `POST /api/v1/auth/login`
- **Frontend Client:** ✅ [`authService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/authService.ts#L6-L8)
- **Quyền hạn:** Công khai (`PermitAll`)
- **Mô tả:** Người dùng đăng nhập bằng tài khoản và mật khẩu, trả về Access Token, Refresh Token (hoặc Cookie) và thông tin cơ bản.
- **Request Body (`LoginRequest`)**
  ```
- **Response Data (`AuthResponse`)**

### 1.2. Đăng xuất hệ thống
- **Method & URL:** `POST /api/v1/auth/logout`
- **Frontend Client:** ✅ [`authService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/authService.ts#L10-L12)
- **Quyền hạn:** Đã đăng nhập (`Authenticated`)
- **Mô tả:** Đăng xuất tài khoản, thu hồi token và session của thiết bị hiện tại.
- **Request Body:** Không có.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 1.3. Đổi mật khẩu
- **Method & URL:** `POST /api/v1/auth/change-password`
- **Frontend Client:** ✅ [`authService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/authService.ts#L14-L16)
- **Quyền hạn:** Đã đăng nhập (`Authenticated`)
- **Mô tả:** Người dùng đổi mật khẩu cá nhân.
- **Request Body (`ChangePasswordRequest`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 1.4. Làm mới Access Token
- **Method & URL:** `POST /api/v1/auth/refresh`
- **Frontend Client:** ✅ [`authService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/authService.ts#L18-L22)
- **Quyền hạn:** Công khai (`PermitAll`)
- **Mô tả:** Cấp mới Access Token khi token cũ hết hạn thông qua Refresh Token truyền qua Body hoặc Cookie.
- **Request Body (`RefreshTokenRequest` - tùy chọn nếu dùng Cookie)**
  ```
- **Response Data (`RefreshTokenResponse`)**

---

## 2. Quản trị người dùng (Admin Auth)
Phụ trách bởi Controller: [`AdminController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/auth/controller/AdminController.java)  
Đường dẫn gốc: `/api/v1/auth/admin`  
**Yêu cầu quyền hạn chung:** Quyền quản trị viên (`ROLE_ADMIN`)

### 2.1. Đăng ký tài khoản người dùng mới (Admin)
- **Method & URL:** `POST /api/v1/auth/admin/register`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts#L65-L76)
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Quản trị viên khởi tạo tài khoản mới cho sinh viên, giảng viên.
- **Request Body (`RegisterRequest`)**
  ```
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 2.2. Xác thực kích hoạt tài khoản bằng mã OTP
- **Method & URL:** `POST /api/v1/auth/admin/verify`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts#L81-L90)
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Xác thực mã OTP gửi về email người dùng khi kích hoạt tài khoản.
- **Request Body (`VerifyOtpRequest`)**
- **Response Data (`AuthResponse`):** Trả về thông tin phiên làm việc và Token.

### 2.3. Gửi lại mã OTP kích hoạt
- **Method & URL:** `POST /api/v1/auth/admin/resend-verify`
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Gửi lại mã OTP kích hoạt vào hòm thư sinh viên/giảng viên.
- **Request Body (`ResendOtpRequest`)**
  ```
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 2.4. Import người dùng hàng loạt (Bulk Import)
- **Method & URL:** `POST /api/v1/auth/admin/bulk-import`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts#L40-L47)
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Nhập dữ liệu danh sách người dùng từ mảng JSON (tạo hàng loạt tài khoản).
- **Request Body (`BulkImportRequest`)**
- **Response Data (`BulkImportResult`):** Thống kê số lượng thành công, thất bại và danh sách lỗi.

### 2.5. Admin đặt lại mật khẩu cho người dùng
- **Method & URL:** `POST /api/v1/auth/admin/reset-password`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts#L53-L60)
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Quản trị viên trực tiếp đặt lại mật khẩu mới cho người dùng.
- **Request Body (`AdminResetPasswordRequest`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 3. Hồ sơ người dùng (User Profile)
Phụ trách bởi Controller: [`UserController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/user/controller/UserController.java)  
Đường dẫn gốc: `/api/v1/users`

### 3.1. Lấy hồ sơ người dùng hiện tại
- **Method & URL:** `GET /api/v1/users/me`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L117-L125)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Lấy toàn bộ thông tin chi tiết của người dùng đang đăng nhập (kèm role, điểm tích lũy, danh hiệu, cosmetic trang bị).
- **Response Data (`ProfileResponse`):** Thông tin người dùng, avatar, background, bio, phone, address, vai trò.

### 3.2. Lấy hồ sơ người dùng theo User ID
- **Method & URL:** `GET /api/v1/users/{userId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L127-L135)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Xem trang cá nhân công khai của người dùng khác.
- **Path Variables:** `userId` (Long) - ID người dùng cần xem.
- **Response Data (`ProfileResponse`):** Thông tin cá nhân, quan hệ bạn bè/follow hiện tại.

### 3.3. Cập nhật hồ sơ cá nhân
- **Method & URL:** `PUT /api/v1/users/me`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L137-L164)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Cập nhật tên, bio, avatar, ảnh bìa, thông tin liên lạc.
- **Request Body (`UpdateProfileRequest`)**
- **Response Data (`ProfileResponse`):** Hồ sơ sau khi cập nhật.

### 3.4. Xóa/Vô hiệu hóa tài khoản cá nhân
- **Method & URL:** `DELETE /api/v1/users/me`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L166-L172)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Người dùng yêu cầu xóa hoặc vô hiệu hóa tài khoản của chính mình.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 4. Bài viết & Video Posts (Post & Feed)
Phụ trách bởi Controller: [`PostController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/post/controller/PostController.java)  
Đường dẫn gốc: `/api/v1`

### 4.1. Tạo bài viết mới
- **Method & URL:** `POST /api/v1/posts`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L19-L21)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Tạo bài đăng mới (văn bản, ảnh/video đính kèm, gắn thẻ môn học, quyền riêng tư).
- **Request Body (`CreatePostRequest`)**
- **Response Data (`PostResponseDto`):** Chi tiết bài viết vừa tạo.

### 4.2. Lấy danh sách bài viết của tôi
- **Method & URL:** `GET /api/v1/posts/me`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L27-L34)
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `after` (Long, optional): Cursor ID của bài viết trước.
  - `limit` (Integer, default: 10): Số lượng bài viết lấy ra.
- **Response Data (`CursorResponse<PostResponseDto>`):** Danh sách bài viết cá nhân kèm phân trang cursor.

### 4.3. Lấy bài viết của người dùng khác
- **Method & URL:** `GET /api/v1/users/{userId}/posts`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L40-L51)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `userId` (Long) - ID người dùng.
- **Query Params:** `after` (Long), `limit` (Integer, default: 10).
- **Response Data (`CursorResponse<PostResponseDto>`)**

### 4.4. Lấy chi tiết một bài viết
- **Method & URL:** `GET /api/v1/posts/{id}`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L57-L59)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID bài viết.
- **Response Data (`PostResponseDto`):** Chi tiết bài viết, tác giả, số like, số comment, trạng thái like của tôi.

### 4.5. Chỉnh sửa bài viết
- **Method & URL:** `PUT /api/v1/posts/{id}`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L65-L70)
- **Quyền hạn:** `Authenticated` (Chính chủ)
- **Path Variables:** `id` (Long) - ID bài viết.
- **Request Body (`UpdatePostRequest`):** Nội dung bài viết, media, quyền riêng tư mới.
- **Response Data (`UpdatePostResponseDto`):** Bài viết sau cập nhật.

### 4.6. Giảng viên thêm / sửa ghi chú chuyên môn cho bài viết
- **Method & URL:** `PUT /api/v1/posts/{id}/lecturer-notes`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L76-L81)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Path Variables:** `id` (Long) - ID bài viết.
- **Request Body (`UpdateLecturerNoteRequest`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 4.7. Xóa bài viết
- **Method & URL:** `DELETE /api/v1/posts/{id}`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L87-L89)
- **Quyền hạn:** `Authenticated` (Chính chủ hoặc Admin)
- **Path Variables:** `id` (Long) - ID bài viết.
- **Response Data (`DeletePostResponseDto`)**

### 4.8. Lấy danh sách video ngắn (TikTok / Reels Feed)
- **Method & URL:** `GET /api/v1/video-posts`
- **Frontend Client:** ✅ [`postService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/postService.ts#L95-L103)
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `subject_id` (Long, optional): Lọc theo môn học.
  - `after` (Long, optional): Cursor phân trang.
  - `limit` (Integer, default: 2): Số lượng video ngắn mỗi lần lướt.
- **Response Data (`CursorResponse<VideoPostItemDto>`)**

---

## 5. Bình luận (Comments)
Phụ trách bởi Controller: [`CommentController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/socialinteraction/controller/CommentController.java)  
Đường dẫn gốc: `/api/v1/comments`

### 5.1. Tạo bình luận mới
- **Method & URL:** `POST /api/v1/comments`
- **Frontend Client:** ✅ [`commentService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/commentService.ts#L24-L26)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Bình luận cho bài viết (`target_type: POST`) hoặc trả lời một bình luận khác (`target_type: COMMENT`).
- **Request Body (`CreateCommentRequest`)**
- **Response Data (`CommentResponseDto`)**

### 5.2. Lấy danh sách bình luận (Cursor Pagination)
- **Method & URL:** `GET /api/v1/comments`
- **Frontend Client:** ✅ [`commentService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/commentService.ts#L32-L50)
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `target_type` (Enum: `POST`, `COMMENT`, optional): Loại đối tượng tương tác.
  - `target_id` (Long, optional): ID đối tượng.
  - `parent_comment_id` (Long, optional): ID bình luận cha (nếu lấy reply).
  - `sort` (String, default: `"newest"`): Kiểu sắp xếp (`"newest"`, `"oldest"`).
  - `after` (Long, optional): Cursor phân trang.
  - `limit` (Integer, default: 20): Giới hạn mỗi trang.
- **Response Data (`CursorResponse<CommentResponseDto>`)**

### 5.3. Cập nhật nội dung bình luận
- **Method & URL:** `PUT /api/v1/comments/{commentId}`
- **Frontend Client:** ✅ [`commentService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/commentService.ts#L56-L64)
- **Quyền hạn:** `Authenticated` (Chính chủ)
- **Path Variables:** `commentId` (Long) - ID bình luận.
- **Request Body (`UpdateCommentRequest`)**
  ```
- **Response Data (`CommentResponseDto`)**

### 5.4. Xóa bình luận
- **Method & URL:** `DELETE /api/v1/comments/{commentId}`
- **Frontend Client:** ✅ [`commentService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/commentService.ts#L70-L72)
- **Quyền hạn:** `Authenticated` (Chính chủ hoặc tác giả bài viết)
- **Path Variables:** `commentId` (Long) - ID bình luận.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 6. Cảm xúc (Reactions)
Phụ trách bởi Controller: [`ReactController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/socialinteraction/controller/ReactController.java)  
Đường dẫn gốc: `/api/v1`

### 6.1. Thả hoặc Hủy cảm xúc (Toggle React)
- **Method & URL:** `POST /api/v1/react`
- **Frontend Client:** ✅ [`reactService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/reactService.ts#L24-L26)
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Thả cảm xúc (`LIKE`, `LOVE`, `WOW`) vào bài viết hoặc bình luận. Bấm lại cùng loại cảm xúc sẽ tự động hủy (Toggle).
- **Request Body (`ReactRequest`)**
- **Response Data (`ReactResponseDto`)**

---

## 7. Bạn bè (Friendships)
Phụ trách bởi Controller: [`FriendshipController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/friendship/controller/FriendshipController.java)  
Đường dẫn gốc: `/api/v1/friendships`

### 7.1. Gửi lời mời kết bạn
- **Method & URL:** `POST /api/v1/friendships/request`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L305-L312)
- **Quyền hạn:** `Authenticated`
- **Request Body (`SendFriendRequestDto`)**
- **Response Data (`FriendshipResponseDto`)**

### 7.2. Chấp nhận lời mời kết bạn
- **Method & URL:** `POST /api/v1/friendships/accept`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L327-L334)
- **Quyền hạn:** `Authenticated`
- **Request Body (`AcceptFriendRequestDto`)**
- **Response Data (`FriendshipResponseDto`)**

### 7.3. Từ chối lời mời kết bạn
- **Method & URL:** `POST /api/v1/friendships/decline`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L350-L355)
- **Quyền hạn:** `Authenticated`
- **Request Body (`DeclineFriendRequestDto`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 7.4. Hủy kết bạn (Unfriend)
- **Method & URL:** `DELETE /api/v1/friendships/{friendId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L360-L365)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `friendId` (Long) - ID của người bạn muốn hủy kết bạn.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 7.5. Lấy danh sách bạn bè hiện tại
- **Method & URL:** `GET /api/v1/friendships`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L175-L184)
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `after` (Long, optional): Cursor phân trang.
  - `limit` (Integer, default: 20): Số lượng bản ghi.
- **Response Data (`FriendListResponseDto`):** Danh sách bạn bè kèm phân trang.

### 7.6. Lấy danh sách lời mời kết bạn đã nhận
- **Method & URL:** `GET /api/v1/friendships/requests/received`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L227-L236)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`FriendRequestReceivedListResponseDto`)**

### 7.7. Lấy danh sách lời mời kết bạn đã gửi đi
- **Method & URL:** `GET /api/v1/friendships/requests/sent`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L266-L275)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`FriendRequestSentListResponseDto`)**

---

## 8. Theo dõi (Follows)
Phụ trách bởi Controller: [`FollowController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/follow/controller/FollowController.java)  
Đường dẫn gốc: `/api/v1`

### 8.1. Theo dõi người dùng
- **Method & URL:** `POST /api/v1/follows/{targetUserId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L369-L374)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `targetUserId` (Long) - ID người dùng muốn follow.
- **Response Data (`FollowResponseDto`)**

### 8.2. Hủy theo dõi người dùng
- **Method & URL:** `DELETE /api/v1/follows/{targetUserId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L388-L393)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `targetUserId` (Long) - ID người dùng muốn hủy follow.
- **Response Data (`UnfollowResponseDto`)**

### 8.3. Lấy danh sách những người mình đang theo dõi (Following)
- **Method & URL:** `GET /api/v1/follows/following`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L406-L415)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`FollowingListResponseDto`)**

### 8.4. Lấy danh sách người đang theo dõi mình (Followers)
- **Method & URL:** `GET /api/v1/follows/followers`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L458-L467)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`FollowerListResponseDto`)**

### 8.5. Lấy số liệu thống kê quan hệ của người dùng
- **Method & URL:** `GET /api/v1/users/{userId}/relationship-stats`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L510-L517)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `userId` (Long) - ID người dùng.
- **Response Data (`RelationshipStatsResponseDto`):** Số lượng bạn bè, số follower, số following.

---

## 9. Chặn người dùng (Blocks)
Phụ trách bởi Controller: [`BlockController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/block/controller/BlockController.java)  
Đường dẫn gốc: `/api/v1/blocks`

### 9.1. Chặn người dùng
- **Method & URL:** `POST /api/v1/blocks/{targetUserId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L541-L546)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `targetUserId` (Long) - ID người bị chặn.
- **Response Data (`BlockResponseDto`)**

### 9.2. Bỏ chặn người dùng
- **Method & URL:** `DELETE /api/v1/blocks/{targetUserId}`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L558-L563)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `targetUserId` (Long) - ID người muốn bỏ chặn.
- **Response Data (`UnblockResponseDto`)**

### 9.3. Lấy danh sách những người đã bị chặn
- **Method & URL:** `GET /api/v1/blocks`
- **Frontend Client:** ✅ [`userService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/userService.ts#L572-L580)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`BlockedListResponseDto`)**

---

## 10. Hội thoại chat & Kho ẩn (Conversations)
Phụ trách bởi Controller: [`ConversationController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/ConversationController.java)  
Đường dẫn gốc: `/api/v1`

### 10.1. Lấy danh sách cuộc hội thoại của tôi
- **Method & URL:** `GET /api/v1/conversations`
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`ConversationListResponseDto`):** Danh sách cuộc hội thoại, tin nhắn cuối cùng, số tin chưa đọc.

### 10.2. Tạo cuộc hội thoại 1-1 (Direct Chat)
- **Method & URL:** `POST /api/v1/conversations/direct`
- **Quyền hạn:** `Authenticated`
- **Request Body (`CreateDirectConversationDto`)**
- **Response Data (`ConversationDetailDto`)**

### 10.3. Tạo cuộc hội thoại nhóm (Group Chat)
- **Method & URL:** `POST /api/v1/conversations/group`
- **Quyền hạn:** `Authenticated`
- **Request Body (`CreateGroupConversationDto`)**
- **Response Data (`ConversationDetailDto`)**

### 10.4. Ẩn cuộc hội thoại vào kho bí mật
- **Method & URL:** `PATCH /api/v1/conversations/{id}/hide`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID cuộc hội thoại.
- **Response Data:** `GlobalResponse<Void>` (`message: "Đã ẩn cuộc hội thoại vào kho bí mật"`).

### 10.5. Bỏ ẩn cuộc hội thoại khỏi kho bí mật
- **Method & URL:** `PATCH /api/v1/conversations/{id}/unhide`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID cuộc hội thoại.
- **Response Data:** `GlobalResponse<Void>` (`message: "Đã bỏ ẩn cuộc hội thoại"`).

### 10.6. Cài đặt / Đổi mã PIN kho chat ẩn
- **Method & URL:** `POST /api/v1/users/me/hidden-chat-pin`
- **Quyền hạn:** `Authenticated`
- **Request Body (`SetHiddenChatPinDto`)**
- **Response Data:** `GlobalResponse<Void>` (`message: "Cài đặt mã PIN kho ẩn trò chuyện thành công"`).

### 10.7. Mở khóa kho chat ẩn bằng mã PIN
- **Method & URL:** `POST /api/v1/conversations/hidden/unlock`
- **Quyền hạn:** `Authenticated`
- **Request Body (`UnlockHiddenChatDto`)**
- **Response Data:** `GlobalResponse<Void>` (`message: "Mở khóa kho ẩn trò chuyện thành công"`).

### 10.8. Lấy danh sách cuộc trò chuyện trong kho ẩn
- **Method & URL:** `GET /api/v1/conversations/hidden`
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`ConversationListResponseDto`)**

### 10.9. Cập nhật thông tin nhóm trò chuyện
- **Method & URL:** `PUT /api/v1/conversations/{id}`
- **Quyền hạn:** `Authenticated` (Admin nhóm)
- **Path Variables:** `id` (Long) - ID nhóm chat.
- **Request Body (`UpdateGroupInfoDto`):** Tên nhóm mới, avatar URL mới.
- **Response Data (`ConversationDetailDto`)**

### 10.10. Lấy danh sách thành viên trong nhóm
- **Method & URL:** `GET /api/v1/conversations/{id}/members`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID nhóm chat.
- **Response Data:** `GlobalResponse<List<ConversationMemberDto>>`

### 10.11. Thêm thành viên vào nhóm chat
- **Method & URL:** `POST /api/v1/conversations/{id}/members`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID nhóm chat.
- **Request Body (`AddGroupMembersRequestDto`)**
- **Response Data (`AddGroupMembersResponseDto`)**

### 10.12. Xóa thành viên khỏi nhóm chat
- **Method & URL:** `DELETE /api/v1/conversations/{id}/members/{userId}`
- **Quyền hạn:** `Authenticated` (Admin nhóm)
- **Path Variables:**
  - `id` (Long): ID nhóm chat.
  - `userId` (Long): ID thành viên bị kick.
- **Response Data:** `GlobalResponse<Void>` (`message: "Xóa thành viên khỏi nhóm thành công"`).

### 10.13. Cập nhật vai trò thành viên nhóm chat
- **Method & URL:** `PUT /api/v1/conversations/{id}/members/{userId}/role`
- **Quyền hạn:** `Authenticated` (Trưởng nhóm / Group Owner)
- **Path Variables:**
  - `id` (Long): ID nhóm chat.
  - `userId` (Long): ID thành viên cần cấp/hạ quyền.
- **Request Body (`UpdateMemberRoleRequestDto`)**
- **Response Data:** `GlobalResponse<Void>` (`message: "Cập nhật vai trò thành viên thành công"`).

### 10.14. Rời khỏi nhóm chat
- **Method & URL:** `POST /api/v1/conversations/{id}/leave`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID nhóm chat.
- **Request Body (`LeaveGroupRequestDto`, optional)**
- **Response Data (`LeaveGroupResponseDto`)**

---

## 11. Tin nhắn (Messages)
Phụ trách bởi Controller: [`MessageController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/MessageController.java)  
Đường dẫn gốc: `/api/v1`

### 11.1. Lấy danh sách tin nhắn trong cuộc hội thoại (Cursor Pagination)
- **Method & URL:** `GET /api/v1/conversations/{id}/messages`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID cuộc hội thoại.
- **Query Params:**
  - `after` (Long, optional): Con trỏ ID tin nhắn.
  - `limit` (Integer, default: 20): Số lượng tin nhắn lấy ra.
- **Response Data (`MessageListResponseDto`):** Danh sách tin nhắn, người gửi, đính kèm, tin nhắn trả lời.

### 11.2. Xóa / Thu hồi tin nhắn
- **Method & URL:** `DELETE /api/v1/messages/{id}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID tin nhắn.
- **Request Body (`DeleteMessageRequestDto`)**
- **Response Data (`DeleteMessageResponseDto`)**

---

## 12. WebSocket STOMP Thời gian thực (Realtime Chat)
Phụ trách bởi Controller: [`ChatWebSocketController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/conversation/controller/ChatWebSocketController.java)  
Cấu hình: [`WebSocketConfig`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/core/config/WebSocketConfig.java)

### 12.1. Handshake & Kết nối STOMP
- **Endpoint:** `GET /api/v1/ws` (Hỗ trợ SockJS fallback)
- **Xác thực:** Qua Header `Authorization: Bearer <access_token>` tại bước CONNECT frame qua [`WebSocketAuthInterceptor`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/core/security/WebSocketAuthInterceptor.java).

### 12.2. Inbound STOMP Mappings (Client gửi lên Server)
1. **Gửi tin nhắn realtime:**
   - **Destination:** `/app/chat.send`
   - **Payload (`SendWsMessageRequestDto`)**
2. **Đánh dấu đã đọc tin nhắn:**
   - **Destination:** `/app/chat.read`
   - **Payload (`ReadWsMessageRequestDto`)**
3. **Gửi trạng thái đang gõ phím (Typing Indicator):**
   - **Destination:** `/app/chat.typing`
   - **Payload (`TypingWsRequestDto`)**

### 12.3. Outbound STOMP Subscriptions (Server đẩy về Client)
1. **Nhận tin nhắn mới & thông báo đã đọc cá nhân:**
   - **Client Subscribe:** `/user/queue/messages`
   - **Payload Đẩy về:**
     - Sự kiện `NEW_MESSAGE` (`WsMessageBroadcastDto`): Tin nhắn mới kèm thông tin sender.
     - Sự kiện `MESSAGE_READ` (`WsReadReceiptBroadcastDto`): Người đọc và thời điểm đọc.
2. **Nhận trạng thái đang gõ phím theo cuộc hội thoại:**
   - **Client Subscribe:** `/topic/conv.{conversationId}.typing`
   - **Payload Đẩy về (`WsTypingBroadcastDto`):** Người đang gõ và cờ `is_typing`.

---

## 13. Thông báo & Cấu hình Push (Notifications)
Phụ trách bởi Controller: [`NotificationController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/notification/controller/NotificationController.java)  
Đường dẫn gốc: `/api/v1`

### 13.1. Lấy danh sách thông báo của tôi (Cursor Pagination)
- **Method & URL:** `GET /api/v1/notifications`
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`NotificationListResponseDto`):** Danh sách thông báo, phân loại (hệ thống, tương tác, điểm thưởng, kết bạn), trạng thái đã đọc.

### 13.2. Đánh dấu 1 thông báo là đã đọc
- **Method & URL:** `POST /api/v1/notifications/{id}/read`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID thông báo.
- **Response Data (`ReadNotificationResponseDto`)**

### 13.3. Đánh dấu tất cả thông báo là đã đọc
- **Method & URL:** `POST /api/v1/notifications/read-all`
- **Quyền hạn:** `Authenticated`
- **Response Data:** `GlobalResponse<Void>` (`message: "Đã đánh dấu đọc tất cả thông báo"`).

### 13.4. Lấy cài đặt nhận thông báo đẩy (Push Settings)
- **Method & URL:** `GET /api/v1/settings/push`
- **Quyền hạn:** `Authenticated`
- **Response Data (`PushSettingsResponseDto`):** Trạng thái bật/tắt thông báo cho từng danh mục, chế độ không làm phiền.

### 13.5. Cập nhật cài đặt nhận thông báo đẩy
- **Method & URL:** `PUT /api/v1/settings/push`
- **Quyền hạn:** `Authenticated`
- **Request Body (`UpdatePushSettingsRequestDto`):** Cấu hình bật/tắt các loại thông báo.
- **Response Data (`PushSettingsResponseDto`)**

---

## 14. Bảng tin hoạt động (Activity Feed)
Phụ trách bởi Controller: [`ActivityFeedController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/activityfeed/controller/ActivityFeedController.java)  
Đường dẫn gốc: `/api/v1`

### 14.1. Lấy dòng thời gian hoạt động hệ thống (Activity Feeds)
- **Method & URL:** `GET /api/v1/activity-feeds`
- **Frontend Client:** ✅ [`feedService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/feedService.ts#L11-L21)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 20).
- **Response Data (`CursorResponse<ActivityFeedItemDto>`):** Dòng sự kiện (ai vừa đóng góp câu hỏi, ai vừa thăng hạng, ai vừa nhận huy hiệu mới).

### 14.2. Đếm số lượng hoạt động mới kể từ thời điểm `since`
- **Method & URL:** `GET /api/v1/activity-feeds/new-count`
- **Frontend Client:** ✅ [`feedService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/feed/services/feedService.ts#L27-L34)
- **Quyền hạn:** `Authenticated`
- **Query Params:** `since` (Long, epoch milliseconds).
- **Response Data (`NewFeedCountResponseDto`):** Số lượng tin mới xuất hiện để hiển thị badge hoặc nút "Có tin mới".

---

## 15. Ngân hàng câu hỏi - Phiên đề xuất (Submissions)
Phụ trách bởi Controller: [`SessionController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/SessionController.java)  
Đường dẫn gốc: `/api/v1/sessions`

### 15.1. Đề xuất phiên nộp câu hỏi mới (Crowdsourcing)
- **Method & URL:** `POST /api/v1/sessions/propose`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L20-L28)
- **Quyền hạn:** `Authenticated` (Sinh viên)
- **Mô tả:** Sinh viên đề xuất một phiên gồm danh sách các câu hỏi trắc nghiệm tự tạo hoặc từ AI hỗ trợ.
- **Request Body (`ProposeSessionRequest`)**
- **Response Data (`ProposeSessionResponse`):** Chi tiết phiên đề xuất vừa tạo kèm mã phiên hiển thị.

### 15.2. Lấy danh sách môn học sinh viên đang tham gia
- **Method & URL:** `GET /api/v1/sessions/my-enrolled-subjects`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L34-L39)
- **Quyền hạn:** `Authenticated`
- **Response Data:** `GlobalResponse<List<SubjectResponse>>`

### 15.3. Lấy danh sách các phiên nộp câu hỏi của tôi
- **Method & URL:** `GET /api/v1/sessions/my-submissions`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L46-L61)
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `after` (Long, optional)
  - `limit` (Integer, optional)
  - `subject_id` (Long, optional): Lọc theo môn học
  - `status` (Enum: `PENDING`, `REVIEWING`, `RESOLVED`, optional)
- **Response Data (`SubmissionsResponse`):**
  - `contents`: Danh sách các phiên nộp gồm: `sessionId`, `sessionCode`, `title`, `content`, `subjectId`, `subjectName`, `subjectCode`, `questionCounts`, `createdAt`, `reactCount`, `commentCount`.
  - `pagination`: Thông tin con trỏ `after`, `hasNext`.

### 15.4. Lấy chi tiết phiên nộp câu hỏi của tôi
- **Method & URL:** `GET /api/v1/sessions/my-submissions/{sessionId}`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L67-L74)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `sessionId` (Long)
- **Response Data (`MySubmissionDetailResponse`):** Thông tin phiên (`sessionId`, `sessionCode`, `title`, `content`, `subjectId`, `subjectName`, `subjectCode`, `commentCount`, `reactCount`, `reviewedByLecturer`, `reviewedAt`, `createdAt`) và toàn bộ danh sách câu hỏi trong phiên kèm phản hồi duyệt từ giảng viên.

### 15.5. Cập nhật phiên nộp câu hỏi (khi ở trạng thái DRAFT)
- **Method & URL:** `PUT /api/v1/sessions/my-submissions/{sessionId}`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L80-L89)
- **Quyền hạn:** `Authenticated` (Chính chủ)
- **Path Variables:** `sessionId` (Long)
- **Request Body (`UpdateSubmissionSessionRequest`):** Tiêu đề, nội dung, danh sách câu hỏi cập nhật.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 15.6. Xóa phiên nộp câu hỏi
- **Method & URL:** `DELETE /api/v1/sessions/my-submissions/{sessionId}`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/session/services/sessionService.ts#L95-L102)
- **Quyền hạn:** `Authenticated` (Chính chủ, chỉ khi DRAFT)
- **Path Variables:** `sessionId` (Long)
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 16. Duyệt & Biên tập câu hỏi (Question Review)
Phụ trách bởi Controller: [`QuestionReviewController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/QuestionReviewController.java)  
Đường dẫn gốc: `/api/v1`

### 16.1. Lấy danh sách các phiên nộp đang chờ duyệt
- **Method & URL:** `GET /api/v1/sessions/pending`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L18-L34)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Query Params:**
  - `after` (Long, optional)
  - `limit` (Integer, optional)
  - `subject_id` (Long, optional)
  - `sort_by` (String, optional)
- **Response Data (`PendingSessionsResponse`)**

### 16.2. Lấy chi tiết phiên nộp để giảng viên duyệt
- **Method & URL:** `GET /api/v1/sessions/{sessionId}`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L36-L47)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Path Variables:** `sessionId` (Long)
- **Response Data (`SessionDetailReviewResponse`):** Chi tiết phiên, thống kê trùng lặp, câu hỏi kèm đáp án.

### 16.3. Phê duyệt câu hỏi vào ngân hàng chính thức
- **Method & URL:** `POST /api/v1/sessions/approve`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L49-L60)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Request Body (`ApproveQuestionsRequest`)**
- **Response Data (`ApproveQuestionsResponse`)**

### 16.4. Từ chối câu hỏi trong phiên
- **Method & URL:** `POST /api/v1/sessions/reject`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L62-L73)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Request Body (`RejectQuestionsRequest`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 16.5. Giảng viên trực tiếp chỉnh sửa câu hỏi
- **Method & URL:** `PUT /api/v1/questions/{questionId}`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L75-L87)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Path Variables:** `questionId` (Long)
- **Request Body (`EditQuestionRequest`):** Nội dung mới, các lựa chọn A/B/C/D mới, giải thích đáp án.
- **Response Data (`EditQuestionResponse`)**

### 16.6. Hoàn tất quá trình đánh giá phiên nộp
- **Method & URL:** `POST /api/v1/sessions/{sessionId}/complete-review`
- **Frontend Client:** ✅ [`reviewService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/review/services/reviewService.ts#L89-L99)
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Path Variables:** `sessionId` (Long)
- **Mô tả:** Chuyển trạng thái phiên sang hoàn tất, cộng điểm thưởng gamification cho sinh viên tương ứng số câu được duyệt.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 17. Luyện tập & Đánh giá câu hỏi (Question Practice)
Phụ trách bởi Controller: [`QuestionInteractionController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/QuestionInteractionController.java)  
Đường dẫn gốc: `/api/v1`

### 17.1. Lấy danh sách các phiên đề xuất của người dùng (Gói theo phiên)
- **Method & URL:** `GET /api/v1/users/{userId}/sessions`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L19-L35)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `userId` (Long)
- **Query Params:**
  - `after` (Long, optional)
  - `limit` (Integer, optional)
  - `subject_id` (Long, optional): Lọc theo môn học
  - `status` (Enum: `PENDING`, `REVIEWING`, `RESOLVED`, optional, chỉ áp dụng khi xem chính mình)
- **Mô tả:** Lấy danh sách các phiên đề xuất phục vụ người dùng xem và chọn phiên để tương tác:
  - **Nếu `userId == currentUserId` (Chính chủ):** Tự động chuyển hướng/ủy quyền sang logic `GET /api/v1/sessions/my-submissions` để lấy đầy đủ các phiên của chính mình.
  - **Nếu `userId != currentUserId` (Cộng đồng xem):** Hệ thống chỉ trả về các phiên đã được duyệt (`RESOLVED`), vì chỉ phiên đã RESOLVED mới thuộc quyền kiểm soát của cộng đồng để tương tác (làm bài, chấm sao, bình luận).
- **Response Data (`SubmissionsResponse`):** Danh sách các phiên tóm tắt (`sessionId`, `sessionCode`, `title`, `content`, `subjectId`, `subjectName`, `subjectCode`, `questionCounts`, `createdAt`, `reactCount`, `commentCount`) kèm thông tin phân trang con trỏ.

### 17.2. Lấy danh sách câu hỏi trong phiên để làm bài & tương tác
- **Method & URL:** `GET /api/v1/sessions/{sessionId}/questions`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L41-L47)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `sessionId` (Long)
- **Mô tả:** Khi người dùng chọn 1 phiên đề xuất cụ thể, API này trả về toàn bộ danh sách câu hỏi trong phiên đó để làm bài tập, chấm điểm sao, thảo luận:
  - Chỉ cho phép tương tác nếu phiên đã ở trạng thái `RESOLVED` (hoặc người gọi là chính chủ đề xuất).
  - Cờ `isCorrect` trong danh sách đáp án (`options`) bị ẩn để đảm bảo tính khách quan khi làm bài.
  - Trường `correctAnswer` và `explanation` chỉ được hiển thị nếu người dùng hiện tại **đã từng trả lời** câu hỏi này (`myInteraction.answered == true`).
  - Trả về cờ `myInteraction` (`answered`, `rated`) để UI hiển thị trạng thái đã làm / đã chấm sao.
- **Response Data (`SessionQuestionsResponse`):** Thông tin phiên (`sessionId`, `sessionCode`, `title`, `content`, `subjectId`, `subjectName`, `subjectCode`, `createdAt`, `totalQuestions`) và mảng `questions` (`List<UserQuestionItemDto>`).

### 17.3. Trả lời câu hỏi luyện tập (Kiểm tra đúng/sai)
- **Method & URL:** `POST /api/v1/questions/{questionId}/answer`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L53-L61)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Request Body (`AnswerQuestionRequest`)**
- **Response Data (`AnswerQuestionResponse`):** Kết quả `is_correct`, đáp án chính xác, lời giải chi tiết.

### 17.4. Lấy thống kê về câu hỏi (Tỷ lệ đúng, độ khó thực tế)
- **Method & URL:** `GET /api/v1/questions/{questionId}/statistics`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L67-L73)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Response Data (`QuestionStatisticsResponse`):** Tổng lượt làm, số lượt đúng, độ chính xác (%), xếp loại độ khó.

### 17.5. Đánh giá chất lượng câu hỏi & Báo lỗi (Feedback / Rating)
- **Method & URL:** `POST /api/v1/questions/{questionId}/rate`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L79-L87)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Request Body (`RateQuestionRequest`)**
- **Response Data (`RateQuestionResponse`)**

### 17.6. Lấy danh sách đánh giá của câu hỏi
- **Method & URL:** `GET /api/v1/questions/{questionId}/ratings`
- **Frontend Client:** ✅ [`practiceService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/features/practice/services/practiceService.ts#L93-L107)
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Query Params:** `after` (Long), `limit` (Integer).
- **Response Data (`QuestionRatingsResponse`)**

---

## 18. Quản lý Lớp học phần (Course Classes)
Phụ trách bởi Controller: [`CourseClassController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/CourseClassController.java)  
Đường dẫn gốc: `/api/v1/lecturer/course-classes`  
**Yêu cầu quyền hạn chung:** Giảng viên hoặc Quản trị viên (`ROLE_LECTURER`, `ROLE_ADMIN`)

### 18.1. Tạo lớp học phần mới
- **Method & URL:** `POST /api/v1/lecturer/course-classes`
- **Request Body (`CreateCourseClassRequest`)**
- **Response Data (`CourseClassResponse`)**

### 18.2. Lấy danh sách lớp học phần theo học kỳ kích hoạt
- **Method & URL:** `GET /api/v1/lecturer/course-classes`
- **Response Data:** `GlobalResponse<List<CourseClassResponse>>`

### 18.3. Lấy danh sách lớp học phần do giảng viên hiện tại phụ trách
- **Method & URL:** `GET /api/v1/lecturer/course-classes/my-classes`
- **Response Data:** `GlobalResponse<List<CourseClassResponse>>`

### 18.4. Lấy thông tin chi tiết một lớp học phần
- **Method & URL:** `GET /api/v1/lecturer/course-classes/{courseClassId}`
- **Path Variables:** `courseClassId` (Long)
- **Response Data (`CourseClassResponse`)**

### 18.5. Thêm danh sách sinh viên vào lớp học phần
- **Method & URL:** `POST /api/v1/lecturer/course-classes/{courseClassId}/students`
- **Path Variables:** `courseClassId` (Long)
- **Request Body (`AssignStudentsRequest`):** Danh sách `student_ids` hoặc danh sách mã sinh viên.
- **Response Data (`AssignStudentsResponse`)**

### 18.6. Lấy danh sách sinh viên trong lớp học phần
- **Method & URL:** `GET /api/v1/lecturer/course-classes/{courseClassId}/students`
- **Path Variables:** `courseClassId` (Long)
- **Response Data:** `GlobalResponse<List<ClassStudentResponse>>`

### 18.7. Import danh sách lớp và sinh viên từ file Excel
- **Method & URL:** `POST /api/v1/lecturer/course-classes/import-excel`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts#L16-L35)
- **Content-Type:** `multipart/form-data`
- **Form Data Parameters:**
  - `file` (MultipartFile): Tệp tin Excel (.xlsx) chứa danh sách sinh viên.
  - `lecturerId` (Long, optional): ID giảng viên phân công.
- **Response Data (`ExcelImportClassResult`):** Số lượng bản ghi thêm thành công và cảnh báo.

### 18.8. Lấy danh sách giảng viên đang hoạt động trong hệ thống
- **Method & URL:** `GET /api/v1/lecturer/course-classes/lecturers`
- **Frontend Client:** ✅ [`adminService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/adminService.ts)
- **Response Data (`List<LecturerSummaryResponse>`):**
  - `id` (Long): User ID của giảng viên
  - `fullName` (String): Họ và tên giảng viên
  - `email` (String): Email
  - `studentLecturerCode` (String): Mã cán bộ giảng viên
  - `schoolFaculty` (String): Khoa / Viện trực thuộc

---

## 19. Quản trị Môn học (Admin Subjects)
Phụ trách bởi Controller: [`AdminSubjectController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/AdminSubjectController.java)  
Đường dẫn gốc: `/api/v1/admin/subjects`  
**Yêu cầu quyền hạn:** Quản trị viên (`ROLE_ADMIN`)

### 19.1. Tạo môn học mới
- **Method & URL:** `POST /api/v1/admin/subjects`
- **Request Body (`CreateSubjectRequest`)**
- **Response Data (`SubjectResponse`)**

### 19.2. Lấy danh sách tất cả môn học
- **Method & URL:** `GET /api/v1/admin/subjects`
- **Response Data:** `GlobalResponse<List<SubjectResponse>>`

---

## 20. Quản trị Học kỳ (Admin Semesters)
Phụ trách bởi Controller: [`AdminSemesterController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/session/controller/AdminSemesterController.java)  
Đường dẫn gốc: `/api/v1/admin/semesters`  
**Yêu cầu quyền hạn:** Quản trị viên (`ROLE_ADMIN`)

### 20.1. Tạo học kỳ mới
- **Method & URL:** `POST /api/v1/admin/semesters`
- **Request Body (`CreateSemesterRequest`)**
- **Response Data (`SemesterResponse`)**

### 20.2. Lấy danh sách tất cả học kỳ
- **Method & URL:** `GET /api/v1/admin/semesters`
- **Response Data:** `GlobalResponse<List<SemesterResponse>>`

### 20.3. Kích hoạt một học kỳ (Active Semester)
- **Method & URL:** `PUT /api/v1/admin/semesters/{semesterId}/activate`
- **Path Variables:** `semesterId` (Long)
- **Response Data (`SemesterResponse`):** Trạng thái học kỳ kích hoạt.

### 20.4. Hủy kích hoạt tất cả học kỳ
- **Method & URL:** `PUT /api/v1/admin/semesters/deactive-all`
- **Response Data:** `GlobalResponse<String>` (`data: "Đã hủy kích hoạt toàn bộ học kỳ"`).

---

## 21. Tạo & Xuất đề thi (Exams)
Phụ trách bởi Controller: [`ExamController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/exam/controller/ExamController.java)  
Đường dẫn gốc: `/api/v1/exams`

### 21.1. Tạo đề thi tự động từ ngân hàng câu hỏi
- **Method & URL:** `POST /api/v1/exams/generate`
- **Quyền hạn:** `Authenticated`
- **Request Body (`GenerateExamRequest`)**
- **Response Data (`ExamDetailResponse`):** Đề thi được tạo kèm các câu hỏi đã chọn ngẫu nhiên theo trọng số.

### 21.2. Lấy chi tiết đề thi theo ID
- **Method & URL:** `GET /api/v1/exams/{examId}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `examId` (Long)
- **Response Data (`ExamDetailResponse`)**

### 21.3. Lấy danh sách đề thi của tôi
- **Method & URL:** `GET /api/v1/exams/my-exams`
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `after` (Long, optional)
  - `limit` (Integer, default: 10)
  - `subject_id` (Long, optional)
- **Response Data (`ExamListResponse`)**

### 21.4. Xuất file đề thi (PDF / DOCX)
- **Method & URL:** `GET /api/v1/exams/{examId}/export`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `examId` (Long)
- **Query Params:**
  - `format` (String, default: `"pdf"`): Định dạng tệp (`"pdf"` hoặc `"docx"`).
  - `include_answer_key` (Boolean, default: `false`): Kèm đáp án cuối trang.
  - `paper_size` (String, default: `"A4"`): Khổ giấy.
- **Response Data (`ExportResponse`):** URL tải file tạm thời hoặc Base64 download.

---

## 22. Xuất ngân hàng câu hỏi (Lecturer Export)
Phụ trách bởi Controller: [`QuestionExportController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/exam/controller/QuestionExportController.java)  
Đường dẫn gốc: `/api/v1`

### 22.1. Giảng viên xuất toàn bộ câu hỏi môn học thành tài liệu
- **Method & URL:** `GET /api/v1/lecturer/questions/export`
- **Quyền hạn:** `ROLE_LECTURER`, `ROLE_ADMIN`
- **Query Params:**
  - `subject_id` (Long, bắt buộc): ID môn học.
  - `format` (String, default: `"pdf"`): Định dạng file xuất.
  - `include_answer` (Boolean, default: `true`): Kèm đáp án đúng.
- **Response Data (`ExportResponse`):** Đường link tải file tài liệu.

---

## 23. Gamification - Dự đoán & BXH & Điểm danh
Phụ trách bởi Controller: [`GamificationController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/gamification/controller/GamificationController.java)  
Đường dẫn gốc: `/api/v1`

### 23.1. Lấy danh sách lớp học phần để dự đoán (Game 1)
- **Method & URL:** `GET /api/v1/games/prediction/game-1/classes`
- **Quyền hạn:** `Authenticated`
- **Response Data:** `GlobalResponse<List<MyCourseClassPredictionDto>>`

### 23.2. Tham gia dự đoán số sinh viên tham gia nộp câu hỏi (Game 1)
- **Method & URL:** `POST /api/v1/games/prediction/participants`
- **Quyền hạn:** `Authenticated`
- **Request Body (`Game1PredictionRequest`)**
- **Response Data (`GamePredictionResponse`)**

### 23.3. Tham gia dự đoán số lượng câu hỏi được duyệt trong phiên (Game 2)
- **Method & URL:** `POST /api/v1/games/prediction/approved-questions`
- **Frontend Client:** ✅ [`sessionService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/sessionService.ts#L113-L124)
- **Quyền hạn:** `Authenticated`
- **Request Body (`Game2PredictionRequest`)**
- **Response Data (`GamePredictionResponse`)**

### 23.4. Tham gia dự đoán quy mô ngân hàng câu hỏi cuối kỳ (Game 4)
- **Method & URL:** `POST /api/v1/games/prediction/bank-size`
- **Quyền hạn:** `Authenticated`
- **Request Body (`Game4PredictionRequest`)**
- **Response Data (`GamePredictionResponse`)**

### 23.5. Lấy phiên nộp đang mở để dự đoán kết quả (Game 6)
- **Method & URL:** `GET /api/v1/games/prediction/active-session`
- **Quyền hạn:** `Authenticated`
- **Response Data (`Game6ActiveSessionResponse`)**

### 23.6. Gửi dự đoán kết quả duyệt phiên (Game 6)
- **Method & URL:** `POST /api/v1/games/prediction/submit`
- **Quyền hạn:** `Authenticated`
- **Request Body (`Game6SubmitRequest`)**
- **Response Data (`GamePredictionResponse`)**

### 23.7. Xem danh sách các lượt dự đoán của tôi
- **Method & URL:** `GET /api/v1/games/my-predictions`
- **Quyền hạn:** `Authenticated`
- **Query Params:** `after` (Long), `limit` (Integer, default: 10).
- **Response Data (`MyPredictionsResponse`)**

### 23.8. Giảng viên xử lý khiếu nại báo lỗi câu hỏi (Game 5 - Error Hunter)
- **Method & URL:** `POST /api/v1/questions/{questionId}/ratings/{ratingUserId}/review-error`
- **Quyền hạn:** `Authenticated` (Giảng viên / Admin)
- **Path Variables:** `questionId` (Long), `ratingUserId` (Long).
- **Request Body (`ReviewErrorRequest`):** Xác nhận có lỗi hay không, cộng điểm thợ săn lỗi.
- **Response Data:** `GlobalResponse<String>`

### 23.9. Quản trị viên tổng kết mùa giải gamification học kỳ
- **Method & URL:** `POST /api/v1/admin/semesters/finalize-semester`
- **Quyền hạn:** `ROLE_ADMIN`
- **Mô tả:** Trao huy hiệu top leaderboard, khóa sổ điểm học kỳ.
- **Response Data:** `GlobalResponse<String>`

### 23.10. Xem Bảng xếp hạng người dùng (Leaderboard)
- **Method & URL:** `GET /api/v1/games/leaderboard`
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `period` (Enum: `SEMESTER`, `MONTH`, `WEEK`, default: `SEMESTER`)
  - `subjectId` (Long, optional): Lọc theo môn học cụ thể
  - `after` (Long, optional)
  - `before` (Long, optional)
  - `limit` (Integer, default: 20)
- **Response Data (`LeaderboardResponse`):** Danh sách thứ hạng, điểm tích lũy, huy hiệu đeo của từng sinh viên.

### 23.11. Điểm danh nhận thưởng hàng ngày (Daily Check-in)
- **Method & URL:** `POST /api/v1/games/check-in`
- **Quyền hạn:** `Authenticated`
- **Ghi chú:** Hỗ trợ `@Idempotent` chống spam liên tục.
- **Response Data (`CheckInResponse`):** Số điểm thưởng nhận được, số ngày điểm danh liên tiếp (streak).

---

## 24. Quản trị Bảng xếp hạng (Admin Leaderboards)
Phụ trách bởi Controller: [`AdminLeaderboardController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/gamification/controller/AdminLeaderboardController.java)  
Đường dẫn gốc: `/api/v1/admin/leaderboards`  
**Yêu cầu quyền hạn:** Quản trị viên (`ROLE_ADMIN`)

### 24.1. Tái tạo / Tính toán lại Bảng xếp hạng
- **Method & URL:** `POST /api/v1/admin/leaderboards/rebuild`
- **Request Body (`RebuildLeaderboardRequest`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 25. Huy hiệu thành tích (Badges)
Phụ trách bởi Controller: [`BadgeController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/badge/controller/BadgeController.java)  
Đường dẫn gốc: `/api/v1/badges`

### 25.1. Lấy danh sách tất cả các huy hiệu trong hệ thống
- **Method & URL:** `GET /api/v1/badges`
- **Quyền hạn:** `Authenticated`
- **Response Data:** `GlobalResponse<List<BadgeResponseDto>>`

### 25.2. Lấy danh sách huy hiệu của chính mình
- **Method & URL:** `GET /api/v1/badges/me`
- **Quyền hạn:** `Authenticated`
- **Response Data:** `GlobalResponse<List<UserBadgeResponseDto>>`: Danh sách huy hiệu đã đạt, ngày đạt, tiến độ hiện tại.

### 25.3. Lấy danh sách huy hiệu của người dùng khác
- **Method & URL:** `GET /api/v1/badges/users/{userId}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `userId` (Long)
- **Response Data:** `GlobalResponse<List<UserBadgeResponseDto>>`

### 25.4. Lấy thông tin chi tiết một huy hiệu
- **Method & URL:** `GET /api/v1/badges/{badgeId}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `badgeId` (Long)
- **Response Data:** `GlobalResponse<BadgeResponseDto>`

---

## 26. Quản trị Huy hiệu (Admin Badges)
Phụ trách bởi Controller: [`AdminBadgeController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/badge/controller/AdminBadgeController.java)  
Đường dẫn gốc: `/api/v1/admin/badges`  
**Yêu cầu quyền hạn:** Quản trị viên (`ROLE_ADMIN`)

### 26.1. Admin lấy danh sách toàn bộ huy hiệu (kèm cấu hình)
- **Method & URL:** `GET /api/v1/admin/badges`
- **Response Data:** `GlobalResponse<List<BadgeResponseDto>>`

### 26.2. Tạo huy hiệu mới
- **Method & URL:** `POST /api/v1/admin/badges`
- **Request Body (`CreateBadgeRequestDto`):** Tên huy hiệu, mô tả, icon URL, điều kiện mở khóa.
- **Response Data (`BadgeResponseDto`)**

### 26.3. Cập nhật thông tin huy hiệu
- **Method & URL:** `PUT /api/v1/admin/badges/{badgeId}`
- **Path Variables:** `badgeId` (Long)
- **Request Body (`UpdateBadgeRequestDto`)**
- **Response Data (`BadgeResponseDto`)**

### 26.4. Xóa huy hiệu
- **Method & URL:** `DELETE /api/v1/admin/badges/{badgeId}`
- **Path Variables:** `badgeId` (Long)
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 26.5. Trao huy hiệu thủ công cho người dùng
- **Method & URL:** `POST /api/v1/admin/badges/{badgeId}/grant`
- **Path Variables:** `badgeId` (Long)
- **Request Body (`ManualGrantBadgeRequestDto`)**
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 27. Vật phẩm trang trí & Cửa hàng (Cosmetics)
Phụ trách bởi Controller: [`CosmeticController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/cosmetic/controller/CosmeticController.java)  
Đường dẫn gốc: `/api/v1/cosmetics`

### 27.1. Lấy kho đồ cá nhân (My Inventory)
- **Method & URL:** `GET /api/v1/cosmetics/my-inventory`
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `type` (Enum: `AVATAR_FRAME`, `PROFILE_PIN`, `CHAT_BUBBLE`, optional)
  - `rarity` (Enum: `COMMON`, `RARE`, `EPIC`, `LEGENDARY`, optional)
  - `only_unlocked` (Boolean, default: `true`)
  - `after` (Long, optional)
  - `limit` (Integer, default: 20)
- **Response Data (`MyInventoryResponseDto`):** Danh sách vật phẩm đang sở hữu, trạng thái trang bị.

### 27.2. Xem danh mục cửa hàng vật phẩm (Cosmetic Shop)
- **Method & URL:** `GET /api/v1/cosmetics/shop`
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `type` (Enum, optional)
  - `sort_by` (Enum: `NEWEST`, `PRICE_ASC`, `PRICE_DESC`, default: `NEWEST`)
  - `after` (Long, optional)
  - `limit` (Integer, default: 20)
- **Response Data (`CosmeticShopResponseDto`):** Các vật phẩm có thể mua bằng điểm tích lũy.

### 27.3. Mua vật phẩm từ cửa hàng bằng điểm
- **Method & URL:** `POST /api/v1/cosmetics/{id}/buy`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID vật phẩm.
- **Ghi chú:** Hỗ trợ `@Idempotent` chống trừ điểm 2 lần.
- **Response Data (`ShopItemDto`):** Vật phẩm mua thành công và số điểm còn lại.

### 27.4. Trang bị / Sử dụng vật phẩm
- **Method & URL:** `PUT /api/v1/cosmetics/equip`
- **Quyền hạn:** `Authenticated`
- **Request Body (`EquipCosmeticRequestDto`)**
- **Response Data (`EquipCosmeticResponseDto`)**

### 27.5. Tháo gỡ vật phẩm trang trí
- **Method & URL:** `PUT /api/v1/cosmetics/unequip`
- **Quyền hạn:** `Authenticated`
- **Request Body (`UnequipCosmeticRequestDto`)**
- **Response Data (`UnequipCosmeticResponseDto`)**

---

## 28. Tải lên Media & MinIO (Medias)
Phụ trách bởi Controller: [`MediaController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/media/controller/MediaController.java)  
Đường dẫn gốc: `/api/v1/medias`

### 28.1. Tải lên tệp tin trực tiếp qua Backend
- **Method & URL:** `POST /api/v1/medias/upload`
- **Frontend Client:** ✅ [`mediaService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/mediaService.ts#L77-L94)
- **Quyền hạn:** `Authenticated`
- **Content-Type:** `multipart/form-data`
- **Form Data:**
  - `file` (MultipartFile, tối đa 40MB): File ảnh, video, tài liệu.
  - `purpose` (Enum: `AVATAR`, `COVER`, `POST`, `CHAT`, `QUESTION`): Mục đích tải lên.
- **Response Data (`MediaUploadResponse`):** URL đối tượng trên MinIO, object key, metadata.

### 28.2. Xin cấp Presigned URL để client tự upload lên S3/MinIO
- **Method & URL:** `POST /api/v1/medias/presign`
- **Frontend Client:** ✅ [`mediaService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/mediaService.ts#L18-L43)
- **Quyền hạn:** `Authenticated`
- **Request Body (`PresignMediaRequest`)**
- **Response Data (`PresignMediaResponse`):** Presigned PUT URL có thời hạn để client upload thẳng lên MinIO.

### 28.3. Xác thực tệp tin tải lên (Verify uploaded files)
- **Method & URL:** `POST /api/v1/medias/verify`
- **Frontend Client:** ✅ [`mediaService.ts`](file:///c:/Users/ADMIN/IdeaProjects/sqb/frontend/src/services/mediaService.ts#L127-L135)
- **Quyền hạn:** `Authenticated`
- **Request Body (`MediaVerifyRequest`):** Danh sách các URLs đã upload.
- **Response Data (`MediaVerifyResponse`):** Danh sách các tệp bị thiếu hoặc chưa lưu vĩnh viễn.

---

## 29. Quản lý Thiết bị & Đăng xuất (Devices)
Phụ trách bởi Controller: [`DeviceController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/device/controller/DeviceController.java)  
Đường dẫn gốc: `/api/v1/devices`

### 29.1. Đăng xuất từ xa khỏi một thiết bị
- **Method & URL:** `POST /api/v1/devices/{deviceId}/logout`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `deviceId` (String) - Mã định danh thiết bị.
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

### 29.2. Lấy danh sách các thiết bị đang đăng nhập tài khoản
- **Method & URL:** `GET /api/v1/devices`
- **Quyền hạn:** `Authenticated`
- **Response Data (`DeviceListResponse`):** Danh sách thiết bị (tên, IP, hệ điều hành, thời điểm đăng nhập cuối).

### 29.3. Xóa thiết bị khỏi danh sách đã tin cậy
- **Method & URL:** `DELETE /api/v1/devices/{deviceId}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `deviceId` (String)
- **Response Data:** `GlobalResponse<Void>` (`data: null`).

---

## 30. Báo cáo vi phạm (Reports)
Phụ trách bởi Controller: [`ReportController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/report/controller/ReportController.java)  
Đường dẫn gốc: `/api/v1`

### 30.1. Gửi báo cáo vi phạm nội dung (Polymorphic Report)
- **Method & URL:** `POST /api/v1/report`
- **Quyền hạn:** `Authenticated`
- **Mô tả:** Báo cáo bài viết, bình luận, tin nhắn, câu hỏi hoặc người dùng có hành vi vi phạm chuẩn mực.
- **Request Body (`CreateReportRequestDto`)**
- **Response Data (`ReportResponseDto`)**

---

## 31. Tìm kiếm toàn cục (Search)
Phụ trách bởi Controller: [`SearchController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/search/controller/SearchController.java)  
Đường dẫn gốc: `/api/v1`

### 31.1. Tìm kiếm toàn cục đa thực thể
- **Method & URL:** `GET /api/v1/search`
- **Quyền hạn:** `Authenticated`
- **Query Params:**
  - `query` (String, optional): Từ khóa tìm kiếm.
  - `type` (Enum: `ALL`, `USER`, `POST`, `QUESTION`, `COURSE_CLASS`, default: `ALL`)
  - `after` (Long, optional): Cursor phân trang.
  - `limit` (Integer, default: 10): Số lượng kết quả.
- **Response Data (`GlobalSearchResponseDto`):** Kết quả phân loại theo User, Bài viết, Câu hỏi ôn tập.

### 31.2. Lấy lịch sử tìm kiếm cá nhân & Top từ khóa thịnh hành
- **Method & URL:** `GET /api/v1/search/saved`
- **Quyền hạn:** `Authenticated`
- **Response Data (`SavedAndTrendingSearchesResponseDto`):** Danh sách từ khóa đã tìm gần đây và Top trending trong trường.

### 31.3. Xóa một từ khóa khỏi lịch sử tìm kiếm cá nhân
- **Method & URL:** `DELETE /api/v1/search/saved/{id}`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `id` (Long) - ID bản ghi tìm kiếm đã lưu.
- **Response Data (`DeleteSavedSearchResponseDto`)**

---

## 32. Tích hợp Trí tuệ Nhân tạo (AI Integration)
Phụ trách bởi Controller: [`AiIntegrationController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/modules/ai/controller/AiIntegrationController.java)  
Đường dẫn gốc: `/api/v1`

### 32.1. Yêu cầu AI tinh chỉnh chất lượng câu hỏi
- **Method & URL:** `POST /api/v1/questions/{questionId}/ai-refine`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Request Body (`AiRefineRequest`)**
- **Response Data (`AiRefineResponse`):** Câu hỏi đã được AI chỉnh sửa, kèm phân tích lý do cải tiến.

### 32.2. Trò chuyện với Trợ lý AI (AI Chatbot Assistant)
- **Method & URL:** `POST /api/v1/admin/ai/chat`
- **Quyền hạn:** `ROLE_ADMIN`
- **Request Body (`AiChatRequest`)**
- **Response Data (`AiChatResponse`):** Phản hồi văn bản từ AI Agent.

### 32.3. Lấy lịch sử hội thoại AI theo phiên
- **Method & URL:** `GET /api/v1/admin/ai/chat/history`
- **Quyền hạn:** `ROLE_ADMIN`
- **Query Params:**
  - `sessionId` (String, bắt buộc): Mã phiên chat.
  - `after` (Long, optional)
  - `limit` (Integer, default: 20)
- **Response Data (`CursorResponse<AiChatHistoryResponse>`)**

### 32.4. Lấy danh sách các phiên trò chuyện AI
- **Method & URL:** `GET /api/v1/admin/ai/chat/sessions`
- **Quyền hạn:** `ROLE_ADMIN`
- **Query Params:** `after` (Long, optional), `limit` (Integer, default: 20).
- **Response Data (`CursorResponse<AiChatSessionSummaryResponse>`)**

### 32.5. Áp dụng kết quả AI tinh chỉnh vào câu hỏi chính thức
- **Method & URL:** `POST /api/v1/questions/{questionId}/ai-apply`
- **Quyền hạn:** `Authenticated`
- **Path Variables:** `questionId` (Long)
- **Request Body (`AiApplyRequest`):** Dữ liệu câu hỏi sau khi đã đồng ý với đề xuất của AI.
- **Response Data (`AiApplyResponse`)**

---

## 33. Xử lý lỗi hệ thống & Monitoring (System)
Phụ trách bởi: [`CustomErrorController`](file:///c:/Users/ADMIN/IdeaProjects/sqb/backend/src/main/java/com/frozenheart/backend/core/exception/CustomErrorController.java), Spring Actuator, Springdoc Swagger

### 33.1. Điểm phân phối lỗi hệ thống chung (Error Dispatcher)
- **Method & URL:** `ALL /api/v1/error`
- **Quyền hạn:** `PermitAll`
- **Mô tả:** Đón bắt các lỗi cấp Servlet/Web (404 Not Found, 405 Method Not Allowed, 401 Unauthorized, 403 Forbidden, 500 Internal Error) và chuyển hóa sang định dạng `GlobalResponse` chuẩn.

### 33.2. Giám sát tình trạng ứng dụng (Actuator Health Check)
- **Method & URL:** `GET /api/v1/actuator/health`
- **Quyền hạn:** `PermitAll`
- **Mô tả:** Kiểm tra sức khỏe hệ thống (Database PostgreSQL, Redis Cache, MinIO Storage, Disk Space).

### 33.3. Thông tin ứng dụng (Actuator Info)
- **Method & URL:** `GET /api/v1/actuator/info`
- **Quyền hạn:** `PermitAll`

### 33.4. Xuất số liệu Prometheus (Metrics Exporter)
- **Method & URL:** `GET /api/v1/actuator/prometheus`
- **Quyền hạn:** `PermitAll` (Trong môi trường production nên cấu hình IP nội bộ cho Prometheus Scraper).

### 33.5. Chỉ số hiệu năng (Actuator Metrics)
- **Method & URL:** `GET /api/v1/actuator/metrics`
- **Quyền hạn:** `PermitAll`

### 33.6. Giao diện Swagger UI tương tác API
- **Method & URL:** `GET /api/v1/swagger-ui/index.html` (hoặc `/api/v1/swagger-ui/**`)
- **Quyền hạn:** `PermitAll`
- **Mô tả:** Web UI OpenAPI để chạy thử nghiệm các API trực tiếp trên trình duyệt.

### 33.7. Tài liệu đặc tả OpenAPI JSON
- **Method & URL:** `GET /api/v1/v3/api-docs`
- **Quyền hạn:** `PermitAll`

---

> 💡 **Lưu ý dành cho Frontend Developer:**
> 1. Khi gọi API cần upload file, sử dụng header `Content-Type: multipart/form-data` hoặc xin presigned URL qua `/api/v1/medias/presign` để upload trực tiếp lên MinIO.
> 2. Phân trang theo dạng con trỏ (`cursor-based pagination`): Truyền `after=<id_cuối_cùng>` của trang hiện tại vào request tiếp theo, KHÔNG dùng offset phân trang truyền thống.
> 3. Để duy trì kết nối realtime, client cần kết nối SockJS/STOMP vào `/api/v1/ws` kèm token trong frame `CONNECT`, sau đó lắng nghe trên `/user/queue/messages` và `/topic/conv.{id}.typing`.
