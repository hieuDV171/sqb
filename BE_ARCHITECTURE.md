# 🏛️ KIẾN TRÚC TỔNG THỂ BACKEND (BE_ARCHITECTURE.md)

> **Dự án:** School Social Network & Crowdsourced Question Bank (SQB)  
> **Phiên bản:** 1.0.0-SNAPSHOT  
> **Trạng thái:** Backend cung cấp 126 endpoint REST, WebSocket STOMP và 23 module.
> **Tài liệu đặc tả API đi kèm:** [BACKEND_API_SPECIFICATION.md](file:///c:/Users/ADMIN/IdeaProjects/sqb/BACKEND_API_SPECIFICATION.md)

---

## 1. TỔNG QUAN HỆ THỐNG & TECH STACK

Hệ thống Backend được xây dựng theo mô hình **Modular Monolith** kết hợp dịch vụ AI phụ trợ (**Sidecar Embedding Service**), phục vụ cả 2 mảng nghiệp vụ: Mạng xã hội trường đại học và Ngân hàng câu hỏi crowdsourcing có gamification.

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React SPA / Mobile)                         │
└───────────────────────┬───────────────────────────────┬───────────────────────┘
                        │ HTTP / REST (JSON)            │ WebSocket (STOMP)
                        ▼                               ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT 4 APPLICATION (Java 25)                     │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────────────┐  │
│  │ Security & JWT  │  │ Global Exception │  │ Spring WebSocket & STOMP     │  │
│  └─────────────────┘  └──────────────────┘  └──────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        23 DOMAIN BUSINESS MODULES                       │  │
│  │ Auth, User, Post, Chat, Session, Review, Practice, Gamification, Exam...│  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────────────┐  │
│  │ Spring Data JPA │  │ Spring Data Redis│  │ Cloudflare R2 S3 SDK         │  │
│  └─────────────────┘  └──────────────────┘  └──────────────────────────────┘  │
└───────────┬─────────────────────┬─────────────────────┬───────────────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌───────────────────────┐ ┌───────────────┐ ┌───────────────────────────────────┐
│ PostgreSQL + PgVector │ │ Redis Cache   │ │ Cloudflare R2 Storage (S3 API)    │
│ (Relational Data &    │ │ (Token, Cache,│ │ (Images, Attachments,             │
│  Vector Embeddings)   │ │  Rate-limit)  │ │  Exported Documents)              │
└───────────────────────┘ └───────────────┘ └───────────────────────────────────┘
            ▲
            │ Cosine Similarity Search
            │
┌───────────┴───────────────────────────────────────────────────────────────────┐
│                 FASTAPI EMBEDDING SIDECAR (Python 3.11+)                      │
│             Sentence-BERT (Text 384 dims) + CLIP (Image 512 dims)             │
└───────────────────────────────────────────────────────────────────────────────┘
```

### Chi tiết Tech Stack:
- **Core Framework:** Spring Boot 4 + Java 25.
- **Build Tool:** Gradle (Groovy DSL).
- **Database:** PostgreSQL 16+ kích hoạt extension `pgvector` và `pg_trgm`.
- **Database Migration:** Liquibase (Lưu ý: Không viết migration thủ công, code ở Entity trước rồi chạy `liquibase:diff`).
- **Cache & Session Management:** Redis 7+ (Lưu trữ Refresh Token, Rate Limiting, Metrics counters).
- **Object Storage:** Cloudflare R2 S3-compatible (Zero egress fees, lưu trữ ảnh đề bài, ảnh đáp án, avatar, tài liệu xuất PDF/Word).
- **Realtime Communication:** Spring WebSocket với STOMP broker.
- **AI / Embedding Engine:** Python FastAPI Sidecar chạy model Sentence-BERT & CLIP local.

---

## 2. TRIẾT LÝ THIẾT KẾ CSDL (DATABASE PHILOSOPHY)

### 2.1. Chiến lược Định danh Thực thể (Identity Strategy)
- **Khóa chính (PK):** Toàn bộ thực thể dùng `BIGSERIAL` / Sequence tăng tự động để đạt hiệu năng đánh index B-Tree và ghi đĩa tối ưu.
- **Mã hiển thị (Composite Display Code):** Không dùng mã có nghĩa (Meaningful ID) làm PK. Thay vào đó, sinh mã hiển thị thân thiện ở cột riêng biệt:
  - `Session.sessionCode`: `{subjectCode}_{authorCode}_{timestamp}_{sessionId}`
  - `Question.questionCode`: `{subjectCode}_{authorCode}_{questionId}`

---

## 3. CÁC CƠ CHẾ HẠ TẦNG DÙNG CHUNG (CROSS-CUTTING CONCERNS)

### 3.1. Envelope Phản hồi Chuẩn hóa (Global Response Envelope)
Toàn bộ 126 endpoints đều bọc dữ liệu trong cấu trúc `GlobalResponse<T>`:
```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```
- Khi xảy ra ngoại lệ nghiệp vụ: `GlobalExceptionHandler` bắt `AppException` và trả về mã lỗi đặc thù trong enum `ResponseCode` (HTTP Status code tương ứng 400, 401, 403, 404, 409, 500).
- Format JSON: Cấu hình Jackson toàn hệ thống serialize/deserialize dạng `snake_case`.

### 3.2. Chuẩn Phân trang Con trỏ (Cursor-Based Pagination)
Không dùng Offset pagination (`page`, `size`) vì hiệu năng kém khi bảng dữ liệu lớn và dễ bị trùng/sót bản ghi khi có dữ liệu mới chèn vào.
- **Tham số đầu vào:** `after` (ID của phần tử cuối trang trước), `limit` (số lượng phần tử cần lấy, mặc định 10-20, max 50).
- **Tham số đầu ra (`CursorPaginationDto`):**
  ```json
  "pagination": {
    "after": 1045,
    "has_next": true
  }
  ```

### 3.3. Quy trình Xử lý Media Tạm & Vĩnh viễn (R2 Lifecycle Pipeline via Redis)
Để tránh rác bộ nhớ Cloudflare R2 do người dùng upload ảnh nhưng hủy form nộp bài:
1. Client xin Pre-signed URL $\rightarrow$ Backend cấp Presigned PUT không kèm tag, đồng thời lưu `objectKey` vào Redis ZSet `sqb:media:pending` với Score = `Instant.now().toEpochMilli()`.
2. Client upload trực tiếp payload lên Cloudflare R2.
3. Khi người dùng nộp phiên thành công, Backend **BẮT BUỘC** gọi:
   ```java
   mediaService.confirmMediaPermanent(List.of(objectKey, ...));
   ```
   Hàm này sẽ xóa `objectKey` khỏi Redis ZSet `sqb:media:pending`.
4. Nếu người dùng hủy form hoặc thoát trang (file mồ côi), tác vụ dọn dẹp ngầm `StorageReconciliationCronJob` định kỳ hàng đêm sẽ quét các key có Score quá 24 giờ và gọi R2 Batch `DeleteObjects` để xóa sạch khỏi Cloudflare R2.

### 3.4. Phòng ngừa Triệt để Lỗi Truy vấn N+1 (N+1 Query Prevention Rule)
- Toàn bộ quan hệ `@ManyToOne`, `@OneToOne` phải đặt `fetch = FetchType.LAZY`.
- Mọi câu query lấy danh sách cha-con (ví dụ: Session $\rightarrow$ Subject, Question $\rightarrow$ OwnedMedias) bắt buộc dùng `JOIN FETCH` hoặc `LEFT JOIN FETCH`.
- Đối với quan hệ phân tán (như kiểm tra trạng thái `UserAnswer` và `UserRating` của người dùng hiện tại trên danh sách câu hỏi): Tuyệt đối không lặp trong vòng for, gom danh sách `questionIds` và truy vấn batch bằng mệnh đề `IN`.

---

## 4. CÁC PHÂN HỆ NGHIỆP VỤ CỐT LÕI (CORE DOMAIN SUBSYSTEMS)

### 4.1. Ngân hàng Câu hỏi & Phiên Đề xuất (Submissions & Review)
- **Vòng đời của Session:**
  - `PENDING`: Phiên vừa được sinh viên đề xuất. Quyền kiểm soát thuộc về Sinh viên (có thể sửa nội dung, xóa câu hỏi, cập nhật đáp án).
  - `REVIEWING`: Giảng viên bắt đầu tiếp nhận duyệt. Phiên bị khóa sửa/xóa với sinh viên.
  - `RESOLVED`: Giảng viên đã hoàn tất duyệt. Phiên chính thức được public ra toàn trường để cộng đồng tương tác (làm bài, chấm sao, bình luận).
- **Vòng đời của Question:** `PENDING` $\rightarrow$ `APPROVED` (đưa vào ngân hàng chính thức) hoặc `REJECTED` (từ chối, có lý do từ GV).
- **Bảo mật Luyện tập (Question Practice Security):**
  - Khi người dùng lấy danh sách câu hỏi trong phiên để luyện tập (`GET /sessions/{sessionId}/questions`):
    - Cờ `isCorrect` trong `options` bị xóa (`null`) để chống gian lận.
    - Đáp án đúng (`correctAnswer`) và lời giải (`explanation`) chỉ được trả về nếu người dùng **đã từng nộp câu trả lời** (`myInteraction.answered == true`).

### 4.2. Hệ thống Phát hiện Trùng lặp 3 Tầng (3-Tier Duplicate Detection)
Chạy bất đồng bộ (`@Async`) ngay sau khi phiên được nộp:
1. **Tầng 1 (Rule-based):** So sánh chính xác mã câu hỏi, text hash, tập hợp đáp án.
2. **Tầng 2 (Trigram Similarity):** Dùng toán tử `%` và hàm `similarity()` của PostgreSQL extension `pg_trgm` trên cột `content` (ngưỡng tương đồng $\ge 0.75$).
3. **Tầng 3 (Vector Embedding):**
   - Gọi Sidecar Python để tính vector embedding của văn bản đề bài (Sentence-BERT, 384 chiều) và ảnh (CLIP, 512 chiều).
   - Truy vấn Cosine Distance trên PostgreSQL PgVector (`<=>` operator) với index `HNSW`.
   - Kết quả cảnh báo trùng lặp được lưu vào trường JSONB `duplicateWarnings` của câu hỏi để giảng viên xem xét khi duyệt.

### 4.3. Phân hệ Mạng Xã Hội (Social Subsystem)
- **Bài viết & Bảng tin:** Post hỗ trợ text, video, multi-image. Feed hiển thị theo cơ chế tổng hợp bài viết từ bạn bè, người đang follow và bài viết công khai của các môn học đang tham gia.
- **Tương tác Like/Reaction:** Thiết kế dạng Toggle API (`POST /posts/{id}/like`) idempotent.
- **Bình luận Phân cấp:** Hỗ trợ reply bình luận cha thông qua `parent_id`.
- **Đồ thị Quan hệ:** Hỗ trợ Friend Request (2 chiều, cần đồng ý) và Follow (1 chiều). Chặn người dùng (Block) tách biệt rõ mã lỗi: `2304` (USER_IS_BLOCKED) và `2309` (ALREADY_BLOCKED).

### 4.4. Tin nhắn Thời gian Thực (Realtime Messaging)
- **Giao thức:** WebSocket STOMP qua endpoint `/api/v1/ws`.
- **Điểm đến (Destinations):**
  - Gửi tin: `/app/chat.sendMessage`
  - Báo đang nhập: `/app/chat.typing`
  - Đánh dấu đã đọc: `/app/chat.markRead`
  - Nhận tin nhắn phòng: `/topic/conversations/{conversationId}`
  - Nhận thông báo cá nhân: `/user/queue/notifications`
- **Read Receipts:** Tiếp cận lai (Hybrid) - Cột `last_read_message_id` trên `conversation_participants` phục vụ đếm số tin chưa đọc cực nhanh, kết hợp bảng chi tiết `read_receipts` cho group chat.

### 4.5. Cơ chế Gamification & Dự đoán (Gamification Engine)
- Hệ thống vinh danh sinh viên đóng góp câu hỏi chất lượng:
  - Tích điểm kinh nghiệm (XP) và Điểm uy tín (Reputation).
  - Điểm danh liên tục (Daily streak).
  - Thợ săn lỗi (Error Hunter): Sinh viên phát hiện câu hỏi trong ngân hàng có sai sót và được GV xác nhận sẽ nhận điểm thưởng lớn.
  - Các minigame dự đoán (Games 1-6) theo mùa giải học kỳ.
  - Cửa hàng vật phẩm ảo (Cosmetic Shop): Mua khung avatar, danh hiệu hồ sơ (Profile Pin).

---

## 5. BẢN GHI CÁC QUYẾT ĐỊNH KIẾN TRÚC (ARCHITECTURE DECISION RECORDS - ADR)

Mỗi khi có quyết định thiết kế hoặc thay đổi kiến trúc quan trọng, **BẮT BUỘC** phải ghi nhận thêm một bản ghi ADR vào mục này.

### [ADR-001] Áp dụng Phân trang 2 Tầng Không Preview cho Phiên Đề Xuất
- **Ngày quyết định:** 21/09/2026
- **Bối cảnh:** API ban đầu trả về mảng phẳng các câu hỏi đề xuất, không gom theo phiên. Ý tưởng gộp cả phiên kèm câu hỏi gây nguy cơ bùng nổ payload (10 phiên có thể chứa 200 câu hỏi, gây giật lag UI và nghẽn mạng).
- **Quyết định:**
  - Tách thành 2 API độc lập:
    1. `GET /api/v1/users/{userId}/sessions`: Phân trang theo Session, chỉ trả về metadata phiên (không kèm câu hỏi). Nếu `userId == currentUserId`, tự động chuyển tiếp lấy đầy đủ phiên mọi trạng thái của chính mình; nếu là người khác, chỉ lấy phiên `RESOLVED`.
    2. `GET /api/v1/sessions/{sessionId}/questions`: Trả về danh sách câu hỏi trong phiên để người dùng làm bài và tương tác.
- **Hệ quả:** UI không bao giờ bị nhảy giao diện (CLS), kiểm soát chặt chẽ dung lượng mạng, tải trang mượt mà.

### [ADR-002] Ẩn Thông tin Đáp án Khách quan khi Luyện tập
- **Ngày quyết định:** 21/09/2026
- **Bối cảnh:** Khi sinh viên luyện tập câu hỏi của người khác, nếu trả về cờ `isCorrect` hoặc `explanation` thì sinh viên có thể xem Inspect Element để biết đáp án.
- **Quyết định:**
  - Ở API công khai, Backend tự động lọc mảng `options` và set `isCorrect = null`.
  - Chỉ khi người dùng thực hiện gọi `POST /questions/{id}/answer`, câu trả lời được ghi nhận vào `UserAnswer`, thì trường `correctAnswer` và `explanation` mới được mở khóa trả về cho người dùng đó.
- **Hệ quả:** Đảm bảo tính trung thực và công bằng cho hệ thống học tập.

### [ADR-003] Không dùng Partitioning cho Bảng Dữ Liệu
- **Ngày quyết định:** Giai đoạn thiết kế ban đầu
- **Bối cảnh:** Cân nhắc phân vùng (partition) bảng `questions` và `posts` theo thời gian hoặc môn học.
- **Quyết định:** Không áp dụng partitioning.
- **Lý do:** Quy mô dữ liệu trường đại học dự kiến < 10 triệu bản ghi. B-Tree index và PgVector HNSW của PostgreSQL đã đáp ứng tốc độ phản hồi < 50ms, tránh over-engineering gây phức tạp bảo trì.

### [ADR-004] Chuẩn hóa Ranh giới Controller & Đóng gói Phân quyền Phiên vào Tầng Service
- **Ngày quyết định:** 21/09/2026
- **Bối cảnh:** Controller `QuestionInteractionController` ban đầu chứa logic nghiệp vụ `currentUserId.equals(userId) ? status : SessionStatus.RESOLVED`, vi phạm nguyên tắc Thin Controller. Đồng thời tồn tại các DTO cũ trùng lặp (`UserQuestionsResponse`) và dead code (`getUserProposedQuestions`, `findUserQuestionsWithCursor`).
- **Quyết định:**
  - Chuyển toàn bộ logic kiểm tra quyền sở hữu và gán `effectiveStatus` vào bên trong `SessionServiceImpl.getUserSubmissions()`.
  - Giữ Controller làm tầng giao tiếp mỏng (Thin Controller) thuần túy.
  - Gom các DTO câu hỏi luyện tập trực tiếp vào `SessionQuestionsResponse.PracticeQuestionDto`, xóa bỏ hoàn toàn `UserQuestionsResponse` và các method thừa.
- **Hệ quả:** Code tầng Controller cực kỳ ngắn gọn và an toàn, triệt tiêu nguy cơ rò rỉ dữ liệu phiên chưa duyệt ra ngoài, loại bỏ nợ kỹ thuật DTO trùng lặp.

### [ADR-005] Đổi Tên và Nâng Cấp Embedding Service thành AI Sidecar (Embeddings + BTProp Audit)
- **Ngày quyết định:** 30/09/2026
- **Bối cảnh:** Trước đây thư mục `embedding-service` chỉ phục vụ sinh vector (`/embed/text`, `/embed/image`). Khi tích hợp thuật toán kiểm soát ảo giác Cây Niềm Tin BTProp (NAACL 2025), service này mở rộng thành bộ đồng xử lý AI chuyên dụng bằng Python. Cần cấu trúc lại cấu hình và định danh cho chuẩn mực kiến trúc.
- **Quyết định:**
  - Đổi tên thư mục `embedding-service/` $\rightarrow$ `ai-sidecar/`.
  - Thay thế trực tiếp cấu hình `embedding.service` thành `ai.sidecar` trong `application.yaml` (`AI_SIDECAR_URL`, timeout 45s).
  - Xóa bỏ `EmbeddingServiceProperties` và `EmbeddingConfig`, thay bằng `AiSidecarProperties` và `AiSidecarConfig`.
  - Hợp nhất client phía Java thành `AiSidecarClientService` đảm nhận cả tạo Embedding và gọi BTProp Audit (`POST /api/v1/ai/btprop/audit`).
  - Viết lại `BtpropHallucinationCheckerService` gọi trực tiếp sang AI Sidecar, tích hợp cơ chế Graceful Degradation (Fallback UNVERIFIED nếu Sidecar bận/timeout).
- **Hệ quả:** Thống nhất toàn bộ luồng giao tiếp AI ngoại vi qua một Sidecar chuẩn REST, hỗ trợ kiểm soát ảo giác đa môn học và câu hỏi nhiều đáp án đúng, đảm bảo hiệu năng cao cho Spring Boot.

### [ADR-006] Chuyển đổi Lưu trữ Object Storage từ MinIO sang Cloudflare R2 & Quản lý Pending Media bằng Redis Sorted Set
- **Ngày quyết định:** 01/10/2026
- **Bối cảnh:** 
  - Chuyển đổi hệ thống từ MinIO tự host sang dịch vụ đám mây Cloudflare R2 nhằm tận dụng ưu điểm 0đ phí truyền dữ liệu ra ngoài (Zero Egress Fees) và tính sẵn sàng cao trên toàn cầu.
  - Cloudflare R2 không hỗ trợ S3 Object Tagging (`PutObjectTagging`, `x-amz-tagging`). Do đó, cơ chế dọn dẹp file tạm cũ dựa trên MinIO ILM (`status=temp`) bị vô hiệu hóa và gây lỗi `501 NotImplemented` khi upload.
- **Quyết định:**
  - Thay thế toàn bộ cấu hình `MINIO_*` thành `R2_*` (`R2Properties`, `R2Config`, `R2HealthIndicator`). Xóa container MinIO khỏi `docker-compose-dev.yml`.
  - Loại bỏ hoàn toàn các lời gọi Object Tagging (`setObjectTags`, `.tags()`, `x-amz-tagging`).
  - Sử dụng **Redis Sorted Set (`sqb:media:pending`)** để quản lý hàng đợi tệp tạm:
    - Khi Client xin Presigned URL hoặc Upload trực tiếp: Lưu `objectKey` vào Redis ZSet với Score là `Instant.now().toEpochMilli()`.
    - Khi Form được submit thành công: Gọi `confirmMediaPermanent(keys)` để xóa key khỏi Redis ZSet.
    - Tác vụ ngầm `StorageReconciliationCronJob` định kỳ hàng ngày (03:30 AM) quét các key có Score quá 24 giờ để gọi Cloudflare R2 batch `DeleteObjects` dọn sạch file mồ côi.
  - Chuẩn hóa hàm `buildPublicUrl()` hỗ trợ trực tiếp R2 Public Domain (`https://pub-xxx.r2.dev/{objectKey}` hoặc Custom Domain) không chứa bucket name ở giữa đường dẫn.
- **Hệ quả:** Hệ thống vận hành trơn tru trên Cloudflare R2 với chi phí tối ưu, không phát sinh file rác mồ côi, giữ nguyên 100% mã nguồn Frontend mà không phải thay đổi luồng upload.
### [ADR-007] Đồng bộ Schema Swagger UI (Springdoc ModelResolver) với Chiến lược snake_case của Jackson
- **Ngày quyết định:** 12/10/2026
- **Bối cảnh:** Server đã cấu hình `spring.jackson.property-naming-strategy: SNAKE_CASE` trong `application.yaml` để tiếp nhận và trả về JSON chuẩn `snake_case`. Tuy nhiên, mặc định `springdoc-openapi` không tự động thừa kế Jackson naming strategy cho các model schema khi phân giải POJO. Điều này khiến Swagger UI (`/docs`) hiển thị Schema và Request/Response Example dưới dạng `camelCase`, gây hiểu lầm cho người gọi API (Client web/mobile/Postman) rằng phải truyền dữ liệu dạng camelCase.
- **Quyết định:**
  - Đăng ký Bean `ModelResolver` trong `OpenApiConfig` tự khởi tạo một instance Jackson 2 `ObjectMapper` (package `com.fasterxml.jackson`) và cấu hình `PropertyNamingStrategies.SNAKE_CASE`. Cách tiếp cận này giúp Swagger-Core (vốn dựa trên Jackson 2) sinh schema `snake_case` chính xác tuyệt đối mà hoàn toàn độc lập, không xung đột hay phụ thuộc vào `JsonMapper` (package `tools.jackson` của Jackson 3) mà Spring Boot 4 đang sử dụng trong toàn bộ hệ thống.
  - Thêm cảnh báo nổi bật trên giao diện Swagger UI: Các API dành cho admin không cần gọi và không được gọi.
  - Rà soát và chuẩn hóa toàn bộ mô tả trong các annotation `@Operation`, `@Parameter`, `@Schema`, bảng đặc tả STOMP, và tài liệu `BACKEND_API_SPECIFICATION.md` về thống nhất chuẩn `snake_case`.
  - Bổ sung kiểm thử đơn vị `OpenApiSnakeCaseTest` đảm bảo tính toàn vẹn của Swagger Schema.
- **Hệ quả:** Toàn bộ Swagger UI hiển thị chính xác 100% định dạng `snake_case`, triệt tiêu hoàn toàn sự sai lệch giữa tài liệu đặc tả và hành vi runtime của hệ thống, bảo đảm cách ly an toàn giữa Jackson 2 và Jackson 3.

### [ADR-008] Giới hạn Tài nguyên Production và Ngừng Distributed Monitoring trên Hosting Dùng chung
- **Ngày quyết định:** 27/10/2026
- **Bối cảnh:** Hosting 5.8 GiB RAM dùng chung nhiều dự án bị global OOM; kernel liên tục kill `sqb-backend`. JVM từng dùng ZGC với `MaxRAMPercentage=75.0` nhưng container không có memory limit. AI sidecar chạy hai Uvicorn workers và mỗi worker nạp riêng cả text/image model. Tempo, Prometheus và các container monitoring đã ngừng sử dụng nhưng backend vẫn khởi tạo exporter và thử gửi trace tới Tempo.
- **Quyết định:**
  - Backend dùng G1GC với heap cố định `-Xms256m -Xmx640m`, `ExitOnOutOfMemoryError`, container RAM limit 1152 MiB và tổng RAM + swap limit 1280 MiB.
  - AI sidecar chỉ chạy một Uvicorn worker để mỗi model chỉ được nạp một lần trong production.
  - Loại Prometheus registry, OpenTelemetry bridge/exporter và starter OpenTelemetry khỏi runtime; tắt thu thập Micrometer metrics và production chỉ expose Actuator `health/info`.
  - Correlation ID tiếp tục được tạo/nhận qua HTTP header và MDC, không phụ thuộc distributed tracing.
  - Workflow production không còn đồng bộ hoặc chứa bước khởi động monitoring stack.
- **Hệ quả:** SQB có ngân sách RAM rõ ràng và giảm nguy cơ làm cạn tài nguyên toàn host; đổi lại G1 có pause dài hơn ZGC, heap nhỏ làm GC thường xuyên hơn và một AI worker giảm khả năng xử lý nhiều inference đồng thời. Hệ thống không còn Prometheus metrics/distributed traces, nhưng health endpoint, application log và correlation ID vẫn hoạt động để chẩn đoán cơ bản.

### [ADR-009] Gắn Phiên Đề xuất Trực tiếp với Lớp Học phần
- **Ngày quyết định:** 27/10/2026
- **Bối cảnh:** `Session` chỉ lưu `subject_id`, nên không xác định được phiên thuộc học kỳ/lớp/GV nào. Điều này cho phép tạo phiên bằng enrollment của kỳ cũ và làm hàng đợi duyệt không thể giới hạn chính xác theo phân công giảng viên.
- **Quyết định:** Xóa quan hệ trực tiếp `Session.subject`; thêm duy nhất `Session.courseClass` (`course_class_id NOT NULL`, `FetchType.LAZY`). Môn học, học kỳ và giảng viên được suy ra qua `CourseClass`. API tạo/cập nhật nhận `course_class_id`; backend bắt buộc lớp thuộc học kỳ active và kiểm tra enrollment/phân công. Hàng đợi duyệt của lecturer lọc theo `courseClass.lecturer`, admin không bị lọc. Các truy vấn đọc dùng `JOIN FETCH courseClass → subject/semester/lecturer` để tránh N+1.
- **Hệ quả:** Mỗi phiên có ngữ cảnh lớp/kỳ/GV không mơ hồ; lớp lịch sử vẫn có thể hiển thị nhưng không dùng để tạo phiên. Đây là breaking schema/API change, cần changeset Liquibase sinh từ entity và reset/reimport dữ liệu test. Không lưu thêm `semester_id` hay `subject_id` trên `sessions`, tránh dữ liệu dư thừa có thể lệch nhau.

### [ADR-010] Lưu Kết quả Từ chối trên Từng Câu hỏi bằng Mã Chuẩn hóa
- **Ngày quyết định:** 29/10/2026
- **Bối cảnh:** API từ chối nhận một ghi chú chung nhưng không lưu, trong khi API chi tiết của sinh viên không trả trạng thái/lý do theo câu hỏi. Ép giảng viên nhập văn bản ở mọi lần từ chối cũng tạo ma sát lớn khi phải duyệt hàng chục hoặc hàng trăm câu.
- **Quyết định:** Lưu `rejectionCode` và `rejectionReason` trực tiếp trên `Question`, bên cạnh `Question.status`. `rejectionCode` là enum chuẩn hóa phục vụ thao tác chọn nhanh/thống kê; `rejectionReason` là ghi chú tùy chọn và chỉ bắt buộc với mã `OTHER`. Dùng chính `GET /sessions/my-submissions/{id}` để trả kết quả theo câu hỏi, không tạo API kết quả riêng. Toàn bộ API kiểm duyệt đặt dưới namespace `/lecturer/**`; danh sách dùng tên `reviewable` vì gồm cả `PENDING` và `REVIEWING`. Bộ lọc dùng `course_class_id`, không dùng `subject_id`, để không trộn các lớp hoặc học kỳ cùng môn. Mọi endpoint duyệt đều kiểm tra role và phân công `courseClass.lecturer`; truy vấn batch phải fetch `Session → CourseClass → Lecturer` để tránh N+1.
- **Hệ quả:** FE chỉ cần một nút Từ chối với progressive disclosure, sinh viên nhận được phản hồi cụ thể, client cũ vẫn tương thích qua `UNSPECIFIED`. Phê duyệt hoặc biên tập lại phải xóa lý do cũ; câu còn chờ khi chốt phiên dùng `AUTO_REJECTED_ON_COMPLETION`. Cần sinh changeset Liquibase từ entity cho hai cột mới; không sửa changeset thủ công.

### [ADR-011] Tách Live Search khỏi Ghi Lịch sử và Thống kê Trending
- **Ngày quyết định:** 30/10/2026
- **Bối cảnh:** `GET /search` trước đây vừa đọc Elasticsearch vừa ghi PostgreSQL và tăng Redis ZSet. Vì Frontend gọi endpoint này sau mỗi lần debounce, hệ thống lưu các đoạn từ khóa chưa gõ xong (`a`, `nguy`, `nguye...`) và dữ liệu automation có thể chiếm Top Trending. Đây cũng là side effect không phù hợp với ngữ nghĩa HTTP GET.
- **Quyết định:** `GET /search` trở thành read-only tuyệt đối. Thêm `POST /search/saved` để ghi nhận duy nhất khi người dùng xác nhận bằng Enter, “Xem tất cả” hoặc chọn kết quả. Trending dùng các Redis Sorted Set theo ngày trong namespace `trending_searches:v2:*`, tổng hợp cửa sổ 7 ngày; mỗi user chỉ đóng góp một lượt cho cùng từ khóa mỗi ngày thông qua Redis Set có TTL. UI desktop dùng search popover neo dưới Header; mobile dùng dialog gọn, cả hai hỗ trợ click ngoài, Escape, focus return và đủ Loading/Empty/Error/Success.
- **Hệ quả:** Không còn lịch sử từ khóa dang dở; số Trending phản ánh lượt tìm kiếm có chủ đích trong SQB và không kế thừa dữ liệu automation bị ô nhiễm ở key cũ. Live search vẫn phản hồi nhanh bằng debounce mà không gây ghi dữ liệu. Không thay đổi schema CSDL và không cần Liquibase changeset; Backend/Frontend phải deploy đồng bộ vì có endpoint mới.

### [ADR-012] Cho phép Nhiều Thành viên Cùng Trỏ tới Một Mốc Đã đọc trong Hội thoại
- **Ngày quyết định:** 02/11/2026
- **Bối cảnh:** `UserParticipant.lastReadMessage` được ánh xạ `@OneToOne`, khiến Hibernate sinh unique constraint trên `user_participants.last_read_message_id`. Trong hội thoại, nhiều thành viên có thể hợp lệ cùng đọc tới một message; khi đó REST tải tin nhắn và WebSocket read-receipt đồng thời vi phạm unique constraint và trả lỗi 500.
- **Quyết định:** Đổi quan hệ thành `@ManyToOne(fetch = FetchType.LAZY)` và giữ nguyên khóa ngoại `last_read_message_id → messages.id`. Không lưu bản sao message, không query trong vòng lặp; mỗi participant vẫn có đúng một mốc đọc, còn một message có thể là mốc đọc của nhiều participant.
- **Hệ quả:** Read-receipt hoạt động đúng cho hội thoại trực tiếp và nhóm, đồng thời tuân thủ invariant mọi `ManyToOne` phải LAZY. Schema phải bỏ unique constraint `uc_user_participants_last_read_message` nhưng giữ foreign key/index phục vụ lookup; cần sinh và review Liquibase changeset từ Entity trước khi deploy Backend.

### [ADR-013] Tái thiết kế Frontend theo Campus Intelligence và Theo dõi API theo Phase
- **Ngày quyết định:** 06/11/2026
- **Bối cảnh:** Giao diện cũ trộn một ngôn ngữ visual cho ba ngữ cảnh có mật độ và mục tiêu khác nhau: mạng xã hội, học/luyện câu hỏi và quản trị. Tài liệu API đồng thời chứa các liên kết FE cũ theo số dòng, nhanh chóng sai lệch khi refactor và tạo cảm giác endpoint đã được kiểm chứng dù UI mới chưa sử dụng.
- **Quyết định:** Xây dựng lại Frontend theo design system `Campus Intelligence`, dùng token ba tầng Primitive → Semantic → Component và một App Shell chung. Social Mode ưu tiên content/feed; Study Mode giảm chrome và tăng khả năng đọc; Management Mode tăng mật độ dữ liệu. Mobile dùng bottom navigation tối đa 5 đích và panel “Thêm”; desktop dùng sidebar phân nhóm theo role. Toàn bộ liên kết FE cũ trong `BACKEND_API_SPECIFICATION.md` được reset; chỉ endpoint đã được gọi và kiểm chứng trong một phase tái cấu trúc mới được đánh dấu lại bằng phase và đường dẫn FE hiện hành.
- **Hệ quả:** UI có một thương hiệu thống nhất nhưng không ép feed, luyện tập và admin dùng cùng mật độ. Việc theo dõi API phản ánh trạng thái tái triển khai thực tế thay vì lịch sử cũ. Migration được thực hiện theo module, giữ nguyên contract Backend mặc định; thay đổi nghiệp vụ hoặc API phát sinh phải được quyết định riêng. Không thay đổi Entity/schema/Liquibase từ ADR này.

### [ADR-014] Onboarding Bắt buộc cho Tài khoản do Admin Import và Xác thực Email Tự phục vụ
- **Ngày quyết định:** 06/11/2026
- **Bối cảnh:** Tài khoản tạo qua bulk import và import Excel trước đây được đánh dấu `verified = true`, `profile_completed = true` dù dùng mật khẩu tạm và chưa chứng minh quyền sở hữu email. Hai API OTP cũ nằm dưới `/auth/admin/**`, nên người dùng thường không thể tự xác thực từ Settings; đồng thời login chặn toàn bộ tài khoản chưa xác thực nên không thể hiển thị hướng dẫn khắc phục sau đăng nhập.
- **Quyết định:** Tài khoản mới từ `/auth/admin/bulk-import` và `/admin/course-classes/import-excel` mặc định hai cờ bằng `false`. Login cho phép tài khoản chưa xác thực nhưng vẫn chặn tài khoản inactive. Frontend dùng `profile_completed = false` làm cổng onboarding bắt buộc đổi mật khẩu; `POST /auth/change-password` đặt cờ thành `true`. Bổ sung hai API authenticated `/auth/email-verification/request` và `/auth/email-verification/confirm`; email luôn lấy từ JWT, client chỉ gửi OTP. Chuẩn hóa security để chỉ login/refresh là public, các endpoint `/auth/**` còn lại yêu cầu xác thực và `/auth/admin/**` tiếp tục yêu cầu Admin.
- **Hệ quả:** Người dùng có thể đăng nhập bằng mật khẩu tạm, buộc thiết lập mật khẩu riêng và tự xác thực đúng email mà không thể chỉ định tài khoản khác. `profile_completed` không còn được bật bởi thao tác sửa tên hồ sơ. Không thay đổi Entity/schema/Liquibase; Redis tiếp tục giữ OTP 2 phút và resend lock 60 giây.

### [ADR-015] Phiên Onboarding Hạn chế theo Thứ tự OTP trước Mật khẩu
- **Ngày quyết định:** 06/11/2026
- **Bối cảnh:** Chỉ chặn bằng modal Frontend không ngăn tài khoản chưa xác thực gọi trực tiếp API, STOMP hoặc giữ quyền Admin/Lecturer. Mật khẩu tạm của luồng import có thể suy ra từ ngày sinh/MSSV, nên cho phép đổi mật khẩu trước khi chứng minh quyền sở hữu email tạo rủi ro chiếm tài khoản.
- **Quyết định:** Dẫn xuất state machine từ hai cờ hiện hữu theo thứ tự `EMAIL_VERIFICATION_REQUIRED` → `PASSWORD_CHANGE_REQUIRED` → `ACTIVE`. Login ở hai trạng thái đầu chỉ cấp JWT `ONBOARDING` 10 phút với duy nhất `ROLE_ONBOARDING`, không cấp/lưu Refresh Token. Security chỉ cho token này gọi gửi/xác nhận OTP, đổi mật khẩu và logout; mọi REST nghiệp vụ trả `ACCOUNT_ONBOARDING_REQUIRED`, STOMP CONNECT bị từ chối. Đổi mật khẩu lần đầu chỉ hợp lệ sau khi `verified = true`; hoàn tất xong phải đăng nhập lại để nhận cặp token đầy đủ. Access Token thường giảm từ 24 giờ xuống 15 phút. OTP có TTL 2 phút, cooldown gửi lại 60 giây và tối đa 5 lần nhập sai.
- **Hệ quả:** Biết mật khẩu tạm chưa đủ để truy cập dữ liệu hoặc chiếm tài khoản; Admin/Lecturer chưa hoàn tất không giữ quyền đặc biệt. Không cần API, Entity, schema hay Liquibase mới; contract login bổ sung `token_type` và `account_state`. Phiên onboarding hết hạn không thể refresh và người dùng phải đăng nhập lại.

### [ADR-016] Giới hạn thao tác FID theo tài khoản và thiết bị

- FID thuộc cặp `userId + deviceId`. Login và đồng bộ kiểm tra xung đột dưới khóa giao dịch PostgreSQL theo FID. Login bỏ qua FID xung đột để người dùng vẫn vào được hệ thống. Đồng bộ trả 409 và chỉ cập nhật thiết bị đang hoạt động. Khi đổi tài khoản trên trình duyệt dùng chung, logout tài khoản cũ giải phóng FID. Login mới chỉ ghi vào thiết bị của chính tài khoản đăng nhập.
- Hủy FID yêu cầu phiên đầy đủ và dùng một câu cập nhật có điều kiện theo cả cặp sở hữu lẫn FID đang lưu. Frontend xếp hàng callback chưa gửi được và gửi lại khi vào phiên `ACTIVE`.

### [ADR-017] Tách quyền xem hồ sơ khỏi quyền xem bài viết

- Hồ sơ thành viên vẫn truy cập được khi có quan hệ chặn. Backend là nguồn quyết định quyền đọc bài viết qua `GET /users/{userId}/posts` và endpoint chi tiết. Khi nhận `USER_IS_BLOCKED`, FE thay phần bài viết bằng biểu tượng Cấm và thông báo, không hiển thị các bài đã lưu trong cache cho trạng thái này.
- Hồ sơ chính chủ dùng `GET /posts/me`; hồ sơ thành viên dùng API theo tác giả và tuân theo phạm vi công khai/bạn bè. Phân trang dùng cursor hiện hữu.

### [ADR-018] Rút lời mời đang chờ và xử lý tranh chấp với chấp nhận

- Rút lời mời là hành động của người gửi trên quan hệ `PENDING`, tách khỏi từ chối và hủy kết bạn. Quan hệ theo dõi quản lý riêng.
- Các thao tác thay đổi quan hệ hiện hữu lấy khóa ghi trên bản ghi trước khi kiểm tra trạng thái. Nếu rút thành công trước, thao tác chấp nhận không còn lời mời. Nếu chấp nhận trước, thao tác rút trả 409.

### [ADR-019] Dùng chung luồng làm mới phiên web cho REST và STOMP

- Trong mỗi tab, REST và STOMP chờ chung một lần gọi `POST /auth/refresh` nếu access token đã hết hạn hoặc còn tối đa 5 giây. Refresh vẫn dùng cookie HttpOnly và chỉ chấp nhận access token mới khớp `userId` và `deviceId` của phiên đang chờ.
- STOMP dùng token hợp lệ trước mỗi `CONNECT`. Nếu broker từ chối xác thực, client thử phục hồi một lần trước khi dừng. Lỗi mạng tạm thời giữ phiên và STOMP thử lại sau 5 giây. Khi refresh bị từ chối, frontend kết thúc phiên tương ứng và đưa người dùng về đăng nhập.

### [ADR-020] Trạng thái cảm xúc của người học trong API luyện tập

- `GET /sessions/{sessionId}/questions` bổ sung `my_interaction.my_reaction`. Backend lấy cảm xúc của người dùng hiện tại bằng một truy vấn theo toàn bộ ID câu hỏi của phiên.
- Frontend cập nhật lạc quan trạng thái và số đếm của đúng câu hỏi, đối chiếu với kết quả `POST /react` và hoàn tác hai trường này khi yêu cầu thất bại. Bình luận thành công cập nhật số đếm trên câu hỏi trong cache. Thẻ phiên ở bảng tin hiển thị số đếm của target `SESSION`.

### [ADR-021] Ẩn kết quả duyệt từng câu trong luyện tập công khai

- Luyện tập phiên đã xét duyệt bao gồm cả câu được duyệt và câu bị từ chối. API luyện tập và tìm kiếm công khai ẩn trạng thái duyệt từng câu. Kết quả chi tiết và tổng số được duyệt thuộc luồng riêng của người nộp hoặc giảng viên.
- Phiên mới và yêu cầu thay thế danh sách câu hỏi cần ít nhất ba câu. Phiên cũ vẫn truy cập được. Sau khi trả lời, người học xem lại phương án và kết quả đã lưu. Nếu câu hỏi đã được cập nhật, FE hiển thị riêng phương án cũ.

### [ADR-022] Lưu báo lỗi và lượt chúc mừng độc lập

- Báo lỗi dùng bảng `question_error_reports`. Các truy vấn đánh giá sao bỏ qua bản ghi báo lỗi cũ. Game 5 đọc báo lỗi mới và đọc bản ghi cũ nếu cần. Khi người dùng chấm sao lên bản ghi báo lỗi cũ, Backend sao lưu báo lỗi trước khi cập nhật sao.
- Lượt chúc mừng dùng phản ứng `BADGE_AWARD` theo `feed_id`. Backend khóa sự kiện khi ghi và lấy số đếm theo lô khi dựng bảng tin. Người nhận và người bị chặn không thể gửi lượt chúc mừng đó.

### [ADR-023] Hoàn tất chuyển dữ liệu báo lỗi sang bảng riêng

- Sau khi áp dụng changeset 09-02, dừng tiến trình ghi và chạy `backend/scripts/migrate-question-reports.sql` trên PostgreSQL. Script chuyển báo lỗi theo khóa người dùng/câu hỏi, giữ bản có sẵn ở bảng đích, tính lại thống kê sao của các câu liên quan rồi bỏ cột `user_ratings.is_error` trong cùng giao dịch.
- Backend chỉ đọc báo lỗi từ `question_error_reports`, kể cả Game 5. API chấm sao từ chối các trường ngoài hợp đồng để payload báo lỗi cũ không trở thành lượt chấm 1 sao.

### [ADR-024] Kết thúc phiên cục bộ ngay khi đăng xuất

- Frontend giữ access token của phiên cần thu hồi cho yêu cầu nền, xóa trạng thái và cache cục bộ, ngắt STOMP rồi về trang đăng nhập. Nếu đang có lượt làm mới, yêu cầu thu hồi đợi lần xoay token đó kết thúc. Nếu backend trả 401 cho phiên ACCESS, frontend có thể dùng cookie để xin token cùng người dùng và thiết bị rồi thử thu hồi lại.
- Yêu cầu đăng nhập mới trong cùng tab đợi tác vụ thu hồi kết thúc để tránh thao tác trên phiên vừa được tạo. Backend vẫn thu hồi refresh token và vô hiệu hóa thiết bị dựa trên danh tính trong access token.

### [ADR-025] Lấy bộ đếm bài viết và đích luyện tập từ PostgreSQL khi tìm kiếm

- Elasticsearch xếp hạng và phân trang theo các bản ghi khớp. PostgreSQL trả bộ đếm hiện tại cho các ID bài viết trong trang và kiểm tra quyền xem, bao gồm trạng thái xóa, chặn và bạn bè.
- PostgreSQL trả ID phiên theo lô cho các câu hỏi thuộc phiên đã xét duyệt. Cả câu được duyệt lẫn câu bị từ chối có thể xuất hiện và cùng dùng hợp đồng phản hồi công khai không có trạng thái duyệt từng câu.

### [ADR-026] Chốt kết quả duyệt phiên bằng thao tác riêng

- Trong `REVIEWING`, phê duyệt, từ chối và biên tập chỉ ghi quyết định hiện tại. Tiến độ 100% vẫn cho phép đổi quyết định. API chi tiết trả `session_status` để FE xác định trạng thái phiên và quyền thao tác.
- Chốt phiên là giao dịch chuyển sang `RESOLVED`, từ chối câu còn chờ và ghi nhận thưởng, thông báo và sự kiện đồng bộ. Các thao tác ghi khóa hàng phiên theo thứ tự ID trước khi đọc câu hỏi. Yêu cầu sửa sau chốt trả `409 SESSION_REVIEW_FINALIZED`. Gọi chốt lại cùng phiên giữ nguyên kết quả đã ghi.
- Tạo đề, đếm ngân hàng và lưu câu chính thức cuối kỳ dựa trên câu được duyệt trong phiên `RESOLVED`. Tìm kiếm và luyện tập công khai tiếp tục bao gồm cả hai nhóm câu sau chốt và ẩn quyết định từng câu. Job AI chỉ tự chốt phiên `PENDING` và kiểm tra lại trạng thái khi lấy khóa.

### [ADR-027] Ghi chú và tương tác trên bài vinh danh đóng góp

- Chốt phiên có câu được duyệt tạo bài công khai `QUESTION_APPROVED_NOTIFICATION`. Bài thuộc loại khác không trả ghi chú cũ trong API. Quyền ghi chú ban đầu dựa trên `session.reviewer` và đã được thay bằng ADR-029.
- Sự kiện `SESSION_RESOLVED_APPROVED` giữ khóa phiên trong cơ sở dữ liệu. API bảng tin tra bài công khai theo lô bằng `posts.session_id` và trả `target_type = POST`, `target_id` là ID bài viết, `content.session_id` là ID phiên để luyện tập. Sự kiện thiếu bài liên kết hiển thị ở dạng thông báo. Cảm xúc và bình luận trên bài dùng chung dữ liệu với trang chi tiết, còn tương tác `SESSION` dành riêng cho bộ câu hỏi.
- API quản lý thiết bị chỉ thu hồi hoặc xóa thiết bị khác với `device_id` trong JWT hiện tại. Người dùng kết thúc phiên hiện tại bằng luồng đăng xuất tài khoản.

### [ADR-028] Thảo luận bộ câu hỏi trong màn luyện tập

- Khung thảo luận dùng bình luận `SESSION` và ID phiên. Màn luyện tập và API tương tác phiên dùng cùng quyền truy cập. Phiên `RESOLVED` mở cho người dùng đã xác thực. Phiên chưa chốt chỉ mở cho người đề xuất.
- Phản hồi phải thuộc cùng đối tượng với bình luận cha. API đọc phản hồi trực tiếp kiểm tra quyền từ bình luận cha. Bản xem trước phản hồi, số phản hồi và hồ sơ tác giả được lấy theo lô cho mỗi trang.
- Thẻ bộ câu hỏi và khung thảo luận dùng chung cảm xúc `SESSION`. Danh sách phiên trả `my_reaction` theo lô, màn luyện tập trả `react_count` và `my_reaction` ở cấp phiên. FE cập nhật cache theo ID phiên và đối soát với bộ đếm trả về từ `POST /react`.

### [ADR-029] Quyền ghi chú theo lớp học phần của phiên được vinh danh

- Tài khoản `LECTURER` có thể ghi chú trên bài `QUESTION_APPROVED_NOTIFICATION` của sinh viên khi giảng viên phụ trách lớp học phần của phiên `RESOLVED` gắn với bài (`post.session.courseClass.lecturer`). Điều kiện áp dụng cho cả phiên do AI chốt.
- Bảng tin và API bài viết tra quyền theo lô. API lưu kiểm tra lại cùng điều kiện trước khi cập nhật. Mỗi bài có một ghi chú chung, hiển thị tên giảng viên cập nhật gần nhất.

### [ADR-030] Dùng mã sinh viên trên bài vinh danh đóng góp

- `SESSION_RESOLVED_APPROVED` và `QUESTION_APPROVED_NOTIFICATION` chỉ dùng mã từ `AnonymizerUtil` làm tên hiển thị trong `actor` và `author`. Hai phản hồi không chứa ID tài khoản, ảnh hoặc khung ảnh của sinh viên. Bài viết thường và tên người ghi chú giữ quy tắc cũ.
- Chuyển đổi ở tầng phản hồi nên áp dụng cho bài đã tạo, không cần đổi schema. ID phiên tiếp tục dùng để luyện tập; mã và ID phiên có thể đối chiếu với danh sách bộ câu hỏi công khai trên hồ sơ thành viên.

### [ADR-031] Kho luyện tập chung theo môn học

- `GET /sessions/practice-catalog` trả phiên đã chốt theo môn, từ khóa và con trỏ. DTO chỉ chứa dữ liệu cần để luyện tập, không gồm danh tính người nộp, lớp học phần hoặc kết quả duyệt. Cảm xúc được tải theo lô trên trang trả về.
- `GET /users/{userId}/sessions` chỉ dành cho tài khoản trùng `userId`. Luồng chính chủ dùng `/sessions/my-submissions`, còn giảng viên dùng API duyệt có phân quyền theo lớp. Liên kết web theo hồ sơ cũ dẫn về kho chung.
- Bài vinh danh giữ mã sinh viên cố định và liên kết luyện tập. Thay đổi chặn đối chiếu qua danh sách phiên gắn với tên thật trong hồ sơ.

### [ADR-032] Tra phiên bằng mã đầy đủ

- Tìm kiếm chung tra mã phiên đầy đủ từ PostgreSQL theo ID và đối chiếu toàn bộ mã. Điều kiện đọc là phiên đã chốt hoặc tài khoản gọi chính là người nộp. Phiên chưa chốt không cần được lập chỉ mục tìm kiếm.
- API phiên của chính mình áp dụng từ khóa trước khi phân trang. Cả ba màn tìm phiên chấp nhận dấu `#` ở đầu mã. Lượt tra mã đầy đủ không được ghi vào lịch sử mới hoặc gợi ý thịnh hành.

### [ADR-033] Nối các trang thông báo trên frontend

- Trang thông báo và menu xem nhanh dùng cùng API con trỏ. Trang nối kết quả theo `pagination.after`, còn menu hiển thị trang đầu với giới hạn riêng.
- Đánh dấu đã đọc cập nhật các trang đã lưu trong cả hai chế độ và làm mới dữ liệu sau khi API phản hồi. Thông báo đẩy khi đang mở ứng dụng yêu cầu tải lại danh sách.

### [ADR-034] Định tuyến hàng đợi STOMP theo ID tài khoản

- STOMP xác thực bằng access token và dùng ID trong JWT làm tên principal. ID này trùng với khóa mà dịch vụ chat dùng khi gửi tin mới và biên nhận đã đọc qua `convertAndSendToUser`. JWT và quyền của người dùng vẫn được giữ trong đối tượng xác thực.
- Client đăng ký `/user/queue/messages` cho phiên của mình. Interceptor từ chối đích gửi hoặc đăng ký ngoài danh sách của chat, bao gồm hàng đợi vật lý và đăng ký theo ký tự đại diện. Các URL STOMP của client không đổi.

### [ADR-035] Xác thực PIN kho ẩn theo phiên đăng nhập

- Cặp access token và refresh token có cùng `sessionId`. Mỗi lần đăng nhập cấp ID mới. Khi xoay refresh token, Redis chỉ ghi đè nếu token được trình vẫn là token hiện tại. Token cũ chưa có ID nhận ID ở lần refresh tiếp theo.
- Redis giữ quyền mở kho tối đa 15 phút. Máy chủ kiểm tra quyền theo tài khoản và `sessionId`, đồng thời đối chiếu phiên với refresh token còn hiệu lực. Khóa thủ công xóa quyền của phiên gọi. Đăng xuất hoặc đăng nhập lại làm quyền của phiên trước mất hiệu lực.
- REST kiểm tra quyền trước khi đọc danh sách, lịch sử và thao tác trên hội thoại ẩn. STOMP đưa cả sự kiện đang gõ vào hàng đợi cá nhân. Broker kiểm tra quyền mở kho cho từng socket nhận dữ liệu ẩn. Việc trích dẫn tin nhắn chỉ áp dụng trong cùng hội thoại.

### [ADR-036] Cập nhật thu hồi tin nhắn qua STOMP

- API thu hồi cho mọi người phát `MESSAGE_REVOKED` sau khi giao dịch lưu hoàn tất. Event chỉ có ID hội thoại và ID tin nhắn. Dùng cùng bộ phát hàng đợi cá nhân và kiểm tra quyền kho ẩn như tin mới.
- Giữ thứ tự xử lý các frame đến và đi trong mỗi kết nối. STOMP dùng chung cấu hình JSON `snake_case` với REST. Hai chiều của trạng thái đang nhập dùng `typing`, còn trạng thái thu hồi trong lịch sử và danh sách hội thoại dùng `revoked`. Frontend dùng sự kiện thu hồi để cập nhật tin nhắn, trích dẫn và bản xem trước hội thoại ngay trong cache.

### [ADR-037] Yêu cầu đồng ý trước khi tham gia nhóm chat qua quan hệ theo dõi

- Tạo nhóm và thêm thành viên kiểm tra quan hệ theo lô. Bạn bè tham gia trực tiếp. Người theo dõi hoặc đang được theo dõi nhận bản ghi trong `group_invitations`. Chỉ tài khoản nhận lời mời có thể chấp nhận hoặc từ chối.
- Lời mời giữ chỗ tối đa 7 ngày trong giới hạn 200 chỗ. Khi người gửi rời nhóm hoặc bị xóa, hệ thống hủy các lời mời họ đã gửi. API chấp nhận kiểm tra lại quyền, hạn và quan hệ chặn, rồi cập nhật thành viên và xóa lời mời trong một giao dịch.
- Khóa bản ghi hội thoại khi thay đổi thành viên hoặc xử lý lời mời để tuần tự hóa việc giữ chỗ, chấp nhận và rời nhóm. Lịch sử chat yêu cầu tư cách thành viên đang hoạt động.

### [ADR-038] Cập nhật ngày khai giảng độc lập với trạng thái học kỳ

- Admin đặt hoặc sửa ngày khai giảng của học kỳ hiện có qua API riêng. Dữ liệu dùng cột `semesters.start_date` hiện có.
- Hibernate chỉ ghi các thuộc tính thay đổi. Luồng kích hoạt, sửa ngày và chốt sổ cập nhật entity được đọc trong giao dịch để việc sửa ngày và đổi trạng thái không ghi đè lẫn nhau.
- Hủy cache học kỳ hiện tại sau khi giao dịch đổi ngày hoặc trạng thái commit. Lần đọc sau lấy lại trạng thái và ngày khai giảng đã lưu.

### [ADR-039] Bù lượt Game 1 bị lỡ lịch chốt

- Lượt Game 1 đặt cho ngày hôm sau theo giờ Việt Nam và chỉ đến hạn khi ngày mục tiêu đã kết thúc. Tác vụ khởi chạy một lần mỗi đêm lúc 00:05 và ngừng mở lô mới từ 00:20. Nếu bỏ lỡ lịch hoặc còn lượt tồn, lần chạy kế tiếp sẽ xử lý tiếp.
- Mỗi lô chọn ngày mục tiêu quá hạn cũ nhất, khóa tối đa 500 lượt đang chờ và đếm người nộp theo lô cho các lớp của đúng ngày đó. Tác vụ tiếp tục sau lô dưới 500 lượt và dừng khi lô rỗng. Mỗi lô chuyển lượt `PENDING` sang `RESOLVED` và cộng thưởng trong giao dịch riêng.
- Lịch sử trả cả `created_at` và `target_date` để giao diện phân biệt ngày đặt với ngày dự đoán.

### [ADR-040] Chấm Game 3 theo hai mốc sau khi chốt phiên

- Chốt phiên thủ công hoặc tự động ghi một lượt `GAME_3_AUTHOR_CONFIDENCE` vào bảng `predictions` của học kỳ đang mở nếu phiên có ít nhất 10 câu. Lượt lưu độ tự tin của 10 câu đầu theo `displayOrder` (dùng ID khi trùng hoặc thiếu thứ tự), gồm cả câu bị từ chối. Phiên đã chốt trước khi triển khai giữ nguyên kết quả và điểm cũ.
- Cron lúc 00:30 giờ Việt Nam đọc các lượt đến hạn theo lô tối đa 500 lượt. Mốc đầu là 14 ngày kể từ lúc chốt phiên. Mỗi câu cần ít nhất 3 người đánh giá khác tác giả, lấy trung bình thang 0–4. Chênh tối đa 0,5 là khớp. Năm câu khớp đem lại 0,25 điểm công khai, sáu câu lệch kết thúc với kết quả thua. Nếu chưa kết luận, `targetDate` chuyển sang mốc 28 ngày. Khi hết hạn, lượt chưa phân định được ghi `RESOLVED` kèm `actualData.outcome = INSUFFICIENT_RATINGS`.
- Chốt sổ chấm các lượt còn chờ của học kỳ ngay trong giao dịch chốt sổ, kể cả các lượt chưa đến mốc 14 ngày. Các lần chấm chỉ xét đánh giá được tạo trước mốc tương ứng hoặc thời điểm chốt sổ nếu đến trước. `user_ratings` lưu điểm mới nhất của mỗi người, nên một lần sửa điểm trên đánh giá đã có có thể được đọc nếu tác vụ chạy sau mốc.
- Đăng ký, chấm đêm và chốt sổ dùng khóa hàng học kỳ. Trạng thái kết quả, điểm và xu commit cùng giao dịch. Chốt sổ tính hạng từ `point_histories` để tính cả điểm vừa cộng, còn Redis nhận sự kiện sau commit theo đúng ID học kỳ. Dùng các trường hiện có, không cần Liquibase.

### [ADR-041] Lưu số liệu tại lần chốt kết quả Game 3

- Khi lượt chuyển sang `RESOLVED`, lưu `questionEvaluations` trong `actualData` cùng kết quả và `resolvedAt`. Mỗi câu có ID, mã câu, thứ tự, độ tự tin, điểm trung bình, số người đánh giá hợp lệ và kết luận. Các số liệu này lấy từ cùng truy vấn dùng để tính kết quả, gồm cả câu có 0 đánh giá.
- Lịch sử lấy số liệu đã lưu cho chủ lượt. Chỉnh sửa đánh giá về sau chỉ cập nhật điểm cộng đồng. Lượt đã chốt bằng phiên bản trước chỉ hiển thị số liệu tổng hợp đã ghi. Giữ nguyên API và schema.

### [ADR-042] Cập nhật vai trò nhóm chat qua STOMP

- Đổi vai trò lấy cùng khóa hàng hội thoại với thao tác thành viên để tuần tự hóa chuyển quyền Tù trưởng. Khi đổi vai trò hoặc khi Tù trưởng rời nhóm và có người kế nhiệm, phát `GROUP_ROLES_CHANGED` sau commit đến thành viên đang hoạt động qua `/user/queue/messages`, dùng cùng kiểm tra PIN cho hội thoại ẩn.
- Sự kiện chỉ mang ID hội thoại. Frontend tải lại truy vấn thành viên của nhóm để lấy cả vai trò mới và quyền thao tác theo vai trò. Khi STOMP kết nối lại, làm mới các truy vấn thành viên đang mở.

### [ADR-043] Ghi nhận và đồng bộ khi thành viên bị mời ra khỏi nhóm

- `user_participants.left_at` xác định tư cách thành viên đã kết thúc. Cột boolean nullable `removed_from_group` ghi nhận thao tác mời ra. Xóa cờ khi tự rời hoặc tham gia lại qua thêm trực tiếp hay chấp nhận lời mời. Bản ghi cũ có `left_at` và chưa có lý do dùng thông báo chung.
- Sau commit phát `GROUP_MEMBER_REMOVED` qua hàng đợi cá nhân tới thành viên đang tham gia và người vừa bị mời ra, giữ kiểm tra PIN của từng người nhận. Frontend làm mới danh sách hội thoại, thành viên và gợi ý của nhóm. Phía người bị mời ra tải lại quyền truy cập từ API lịch sử.
- API lịch sử và thành viên dùng HTTP 403 với mã `2413` khi bị mời ra và `2414` khi tư cách đã kết thúc mà thiếu dấu hiệu bị mời ra. Giao diện thay khu vực tin nhắn bằng thông báo tương ứng. Nhóm ẩn vẫn yêu cầu mở kho trước khi trả lý do.

### [ADR-044] Đồng bộ khi có thành viên mới trong nhóm chat

- Thêm trực tiếp và chấp nhận lời mời phát `GROUP_MEMBERS_ADDED` sau commit khi có thành viên mới hoặc tham gia lại. Lời mời còn chờ chưa làm tăng số thành viên. Dùng hàng đợi cá nhân và kiểm tra PIN hiện có, gồm cả người vừa tham gia.
- Client tải lại danh sách hội thoại, thành viên và gợi ý của nhóm. Tiêu đề chat và hộp thành viên lấy số lượng từ cùng truy vấn thành viên khi đã tải. Với người tham gia lại đang mở nhóm, sự kiện làm mới truy vấn lịch sử từng báo lỗi hoặc còn chờ.

### [ADR-045] Đọc học kỳ hiện tại từ PostgreSQL

- `CurrentSemesterService` đọc trực tiếp trạng thái học kỳ. Index `idx_semesters_single_active` trong changeset 07-02 bảo đảm tối đa một dòng `active = true`. Kích hoạt flush trong giao dịch để ánh xạ xung đột đúng index thành HTTP 409. Đóng kỳ lấy cùng khóa hàng với chốt sổ và Game 3, rồi cập nhật đúng dòng đã kiểm tra.
- Khi chưa có kỳ hoạt động, API xem bảng xếp hạng và danh sách lớp theo kỳ hiện tại trả rỗng. Thao tác đồng bộ bảng xếp hạng, tạo lớp theo kỳ hiện tại, nhập lớp và đề xuất yêu cầu một kỳ hoạt động. Lớp lịch sử đọc trạng thái từ học kỳ đã join trong truy vấn lớp.
- Sổ điểm và `PointAddedEvent` dùng cùng ID học kỳ. Game 1 lấy kỳ của lớp theo lô, Game 2 và thưởng duyệt lấy kỳ của phiên, Game 3 và chốt sổ dùng kỳ đã xác định trong giao dịch. Game 5 và Game 6 lấy kỳ tại lúc xử lý. Listener Redis bỏ qua sự kiện không có kỳ và chỉ dùng kỳ ghi trên sự kiện.

### [ADR-046] Cập nhật thông báo trong ứng dụng qua STOMP

- Các luồng tạo thông báo nội bộ lưu qua `NotificationWriter`. Sau khi commit, dịch vụ gửi `NOTIFICATIONS_CHANGED` tới hàng đợi cá nhân `/user/queue/notifications`. Khi lưu theo lô, mỗi tài khoản nhận một tín hiệu cho lô đó. Định tuyến dùng ID tài khoản đã xác thực và payload chỉ có loại sự kiện.
- Frontend dùng lại kết nối STOMP của ứng dụng để tải lại danh sách và số chưa đọc khi có tín hiệu hoặc khi kết nối lại. REST vẫn cung cấp dữ liệu phân trang và trạng thái đã đọc. Luồng Firebase Cloud Messaging tiếp tục phục vụ thông báo đẩy theo cài đặt của người dùng.

### [ADR-047] Ngừng cung cấp trợ lý chat AI cho Admin

- Gỡ ba API gửi câu hỏi, đọc lịch sử và liệt kê phiên tại `/admin/ai/chat`, cùng mã xử lý riêng cho chức năng này. Mã của tính năng có thể tra trong lịch sử Git.
- Gỡ mapping của `ai_chat_sessions` và `ai_chat_messages` khỏi JPA. Hai bảng và dữ liệu vẫn còn cho đến khi áp dụng changeset do người vận hành tạo. Scheduler duyệt tự động và luồng tinh chỉnh, áp dụng đề xuất AI cho câu hỏi tiếp tục sử dụng dịch vụ AI hiện tại.

### [ADR-048] Quyền xem video bài giảng theo môn học

- Người dùng xem được bài `LEARNING_VIDEO` nếu đã ghi danh vào một lớp học phần của môn, là người đăng hoặc là Admin. Ghi danh được xét theo môn, gồm cả lớp ở kỳ trước. API danh sách và Bảng tin áp dụng quyền trước khi phân trang.
- Chi tiết, bài viết hồ sơ, kết quả tìm kiếm và tương tác trên bài giảng cũng kiểm tra quyền. Quyền này kiểm soát dữ liệu do API cung cấp. Tệp trên URL công khai của R2 vẫn truy cập được nếu đã có URL.

### [ADR-049] Con trỏ phân trang cho hội thoại và danh sách quản lý

- Hội thoại thường và kho ẩn sắp xếp theo thời điểm tin cuối giảm dần, dùng ID hội thoại khi trùng thời điểm và xếp nhóm chưa có tin xuống cuối. API trả `next_cursor` chứa thời điểm đầy đủ và ID. Danh sách bạn bè, lời mời kết bạn, theo dõi và chặn dùng cùng định dạng với ID của người ở đầu kia. Các API vẫn nhận `after` dạng mili giây để tương thích client cũ.
- Lịch sử dự đoán, Đề thi và Phiên cần duyệt nhận `before` hoặc `after`. Chiều ngược lấy các ID liền trước theo thứ tự tăng dần rồi đảo khi trả kết quả. Backend áp dụng cùng bộ lọc và quyền ở cả hai chiều, trả con trỏ cùng `has_prev` và `has_next`. Bảng xếp hạng tiếp tục dùng con trỏ theo thứ hạng.
- Cửa hàng giữ `after` là ID vật phẩm. Khi sắp theo giá hoặc độ hiếm, backend dùng giá hoặc thứ tự độ hiếm của vật phẩm mốc cùng ID để tiếp tục. Thứ tự độ hiếm từ cao xuống thấp là LEGENDARY, EPIC, RARE, COMMON. Trạng thái sở hữu được truy vấn một lần theo ID các vật phẩm trên trang.

### [ADR-050] Xóa tài khoản sau 30 ngày

- `DELETE /users/me` ghi hạn xóa vào `users.deletion_due_at` và trả lại cùng hạn khi gửi lại yêu cầu. Trong 30 ngày chờ, tài khoản vẫn hoạt động bình thường. Settings đọc trạng thái bằng `GET /users/me/deletion` và hủy bằng `DELETE /users/me/deletion` trước khi hết hạn.
- Tác vụ nền kiểm tra mỗi phút và xử lý tài khoản đã đến hạn theo lô. Lập lịch, hủy và xử lý hết hạn đều khóa hàng `User`. Hồ sơ, điểm và thiết bị được đọc hoặc cập nhật theo lô. Khi xử lý thành công, tài khoản bị vô hiệu hóa, hồ sơ được ẩn danh và bản ghi tài khoản được giữ cho các dữ liệu tham chiếu.
- Sau commit, backend phát sự kiện gỡ hồ sơ khỏi tìm kiếm, dọn ảnh đại diện và ảnh bìa, đóng kết nối STOMP trên tiến trình BE hiện tại. JWT cho REST và STOMP được đối chiếu với ID, email và trạng thái hoạt động của tài khoản. Refresh token mới cũng mang ID tài khoản. Token cũ được chuyển sang định dạng mới khi làm mới.

### [ADR-051] Áp dụng bản AI trong phiên duyệt và quản trị huy hiệu

- Giảng viên phụ trách lớp và Admin xem bản tinh chỉnh cùng kết quả BTProp trong phiên đang mở. Cảnh báo hoặc kết quả chưa kiểm chứng cần xác nhận tự rà soát. BE khóa phiên khi áp dụng, kiểm tra bản đề xuất còn chờ và câu hỏi chưa được sửa sau khi tạo bản AI. Tệp ảnh đáp án lấy từ câu hỏi gốc theo khóa lựa chọn.
- Admin được trao thủ công mọi huy hiệu đang hoạt động. Vô hiệu hóa giữ các lượt đã trao. Danh mục và màn Admin lấy số người đạt theo lô; JSON tiêu chí ghi duy nhất trường phân loại `type`.
- Hồ sơ dùng tải trực tiếp cho ảnh đại diện và ảnh bìa. Đề xuất phiên dùng presign và kiểm tra tồn tại theo lô trước khi nộp. Khi lưu nghiệp vụ, BE xác nhận tệp vĩnh viễn. API thống kê quan hệ giữ tương thích và được đánh dấu deprecated.

### [ADR-052] Ngừng trao và thu hồi huy hiệu

- `Badge.active` điều khiển việc xét và trao mới. GV/SV thấy huy hiệu đã ngừng trao ở danh mục và bộ sưu tập. Admin có thể kích hoạt lại qua API cập nhật huy hiệu hiện có.
- `UserBadge.revokedAt` ghi nhận lượt bị thu hồi và giữ cặp khóa người dùng/huy hiệu. Luồng tự động bỏ qua mọi cặp khóa đã có, kể cả lượt bị thu hồi. Admin trao lại bằng cách đặt `revokedAt` về null và ghi thời điểm trao mới. Thu hồi cập nhật số đếm hồ sơ, ẩn sự kiện cũ khỏi Bảng tin và tạo thông báo `BADGE_REVOKED` cho tài khoản đang hoạt động bằng `NotificationWriter`. STOMP báo thay đổi sau commit. Thông báo đã gửi và ảnh của sự kiện lịch sử vẫn được lưu.
- Màn Admin tìm người trao và người đang giữ huy hiệu bằng truy vấn phân trang theo ID. Form lấy danh mục môn học qua `/subjects`; BE kiểm tra ID và dùng mã môn chính thức. Biểu tượng tải trực tiếp lên R2 qua `BADGE_ICON` và được xác nhận vĩnh viễn khi lưu huy hiệu.
- `Badge.badgeTriggerEvent` chọn sự kiện xét thưởng và `Badge.criteria` lưu JSONB theo bộ đánh giá tương ứng. BE kiểm tra cặp cấu hình khi tạo, sửa tiêu chí hoặc kích hoạt lại. Cấu hình đã lưu được dùng từ sự kiện tiếp theo, dựa trên dữ liệu tích lũy của người phát sinh sự kiện. Huy hiệu thứ hạng chỉ xét Top 10 toàn trường khi chốt học kỳ. Tiêu chí thứ hạng theo môn đã lưu từ trước không được xét bằng kết quả toàn trường.

### [ADR-053] Triển khai Backend và AI sidecar độc lập

- `deploy-backend.yml` và `deploy-ai-sidecar.yml` theo dõi thư mục tương ứng trên `master`, dùng cache Buildx riêng và triển khai image theo digest. Hai workflow cập nhật từng service bằng `up -d --no-deps` trong cùng project Compose `sqb`. VPS kiểm tra readiness của Spring hoặc `/health` của sidecar rồi lưu image đã sẵn sàng trong `/opt/sqb/.deploy-images/`.
- Dùng workflow thủ công **Deploy SQB Production Stack** khi cài stack lần đầu hoặc đổi `docker-compose.prod.yml`. Để trống `services` để áp dụng toàn stack, hoặc nhập danh sách service bị ảnh hưởng cách nhau bằng dấu cách. Workflow dùng các image đã sẵn sàng nếu không chọn digest khác và áp dụng theo thứ tự hạ tầng, sidecar, BE. Lần đầu chuyển từ cấu hình cũ, script lấy digest từ image của container hiện có. Stack mới hoàn toàn dùng các image trong `.env.prod`. VPS dùng `flock` để cập nhật stack không chồng lên hai deploy service.
- Khi đổi hợp đồng HTTP, triển khai sidecar hỗ trợ hợp đồng cũ và mới, chuyển BE, rồi bỏ hợp đồng cũ sau khi hết nhu cầu rollback. Có thể chạy từng workflow với tùy chọn `deploy` tắt để xuất image và chọn cặp digest cụ thể trong workflow Stack. Sidecar mới vẫn phải phục vụ BE đang chạy trong lúc chuyển phiên bản.
