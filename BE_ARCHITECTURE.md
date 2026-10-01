# 🏛️ KIẾN TRÚC TỔNG THỂ BACKEND (BE_ARCHITECTURE.md)

> **Dự án:** School Social Network & Crowdsourced Question Bank (SQB)  
> **Phiên bản:** 1.0.0-SNAPSHOT  
> **Trạng thái:** Backend đã hoàn thiện 100% 129 Endpoints, WebSocket STOMP và 23 Modules.  
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
Toàn bộ 129 endpoints đều bọc dữ liệu trong cấu trúc `GlobalResponse<T>`:
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


