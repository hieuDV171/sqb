# Lộ trình MobileClient

Beta đầu tiên phục vụ sinh viên trên Android. Luồng chính đi từ đăng nhập và thiết lập tài khoản đến chọn môn, luyện tập và mở lại tiến độ đã lưu. Baseline hiện tại là ứng dụng mẫu Flutter với widget test bộ đếm. [Chuẩn UX/UI SQB](../docs/clients/DESIGN.md) · [Hướng dẫn agent](AGENTS.md) · [Mã nguồn hiện tại](lib/main.dart) · [Widget test hiện tại](test/widget_test.dart)

## 1. Nền tảng Android

- Dựng theme sáng/tối, điều hướng cho khách, tài khoản đang thiết lập và sinh viên đã đăng nhập. Dùng chung các widget tải, trống, lỗi và thành công. Kiểm tra nút Quay lại, bàn phím, safe area, chữ phóng to và TalkBack.
- Gửi yêu cầu POST đến `/api/v1/auth/native/login`, `/api/v1/auth/native/refresh` và `/api/v1/auth/native/logout` theo hợp đồng native. Login gửi `platform: ANDROID` và `device_id` ổn định cho từng lượt cài đặt. Dùng access token trong bộ nhớ và lưu refresh token của tài khoản ACTIVE trong kho bảo mật của hệ điều hành. Dùng chung một lần refresh cho các yêu cầu đồng thời. Khi refresh thành công, thay cả hai token ở nơi lưu tương ứng và thử lại yêu cầu nghiệp vụ tối đa một lần. Khi refresh token bị từ chối, quay về đăng nhập.
- Dùng `account_state` để dẫn người dùng đến xác thực email trước, rồi đổi mật khẩu nếu được yêu cầu. OTP và đổi mật khẩu dùng các endpoint chung dưới `/api/v1/auth`. Token ONBOARDING có phạm vi giới hạn. Đăng nhập lại sau khi hoàn tất thiết lập để lấy phiên ACTIVE. Khi đăng xuất, dọn token và dữ liệu phiên cục bộ.

[NativeAuthController](../backend/src/main/java/com/frozenheart/backend/modules/auth/controller/NativeAuthController.java) · [AuthController](../backend/src/main/java/com/frozenheart/backend/modules/auth/controller/AuthController.java) · [Luồng thiết lập tài khoản trên web](../webclient/src/pages/auth/AccountSetupPage.tsx)

## 2. Beta học tập cho sinh viên

| Bước | Hành vi cần có | Hợp đồng tham chiếu |
| --- | --- | --- |
| Chọn môn và tìm bộ câu hỏi | Lọc theo môn, tải thêm từ con trỏ API và giữ bộ lọc khi quay lại từ màn làm bài. Giữ các mục đã tải nếu lỗi ở trang kế tiếp. | `GET /api/v1/subjects`. `GET /api/v1/sessions/practice-catalog` nhận `subject_id` và `after`. [SubjectController](../backend/src/main/java/com/frozenheart/backend/modules/session/controller/SubjectController.java) · [SessionController](../backend/src/main/java/com/frozenheart/backend/modules/session/controller/SessionController.java) |
| Luyện tập | Mở bộ câu hỏi, chọn và gửi đáp án. Hiển thị đúng/sai và lời giải theo phản hồi của máy chủ. Khi yêu cầu gửi chưa rõ kết quả do mất mạng, đọc lại dữ liệu trước khi cho người dùng gửi lại. | `GET /api/v1/sessions/{sessionId}/questions`, `POST /api/v1/questions/{questionId}/answer`. [QuestionInteractionController](../backend/src/main/java/com/frozenheart/backend/modules/session/controller/QuestionInteractionController.java) |
| Mở lại tiến độ | Khôi phục đáp án và kết quả đã lưu qua API. Nếu máy chủ báo câu hỏi đã thay đổi từ lần trả lời, thể hiện trạng thái đó. Hiển thị tiến độ theo dữ liệu của tài khoản hiện tại. | `my_interaction` và các trường người dùng được phép xem trong `hidden_fields`. [Mẫu màn luyện tập](../webclient/src/pages/session/PracticeWorkspacePage.tsx) · [Mẫu thẻ câu hỏi](../webclient/src/features/practice/components/PracticeQuestionCard.tsx) |

### Nghiệm thu beta

| Mức | Điều kiện | Trạng thái |
| --- | --- | --- |
| Triển khai | Có luồng đăng nhập, OTP, đổi mật khẩu, khôi phục và đăng xuất cho sinh viên. Có kho luyện tập, gửi đáp án, xem lời giải và mở lại tiến độ. | Chưa bắt đầu |
| Kiểm thử tự động | `flutter analyze` và `flutter test` đạt. Có kiểm thử cho refresh đồng thời, phiên hết hạn, phân trang và khôi phục đáp án của tài khoản. | Chưa nghiệm thu beta |
| Thiết bị thật | Dùng tài khoản sinh viên mới và tài khoản ACTIVE trên API môi trường thử nghiệm. Kiểm tra OTP sai hoặc hết hạn, từ chối thao tác ngoài quyền, đổi tài khoản, tải trang thứ hai, lỗi tải thêm, mất mạng lúc gửi, mở lại app để đối chiếu tiến độ, giao diện tối, chữ phóng to và TalkBack. | Chưa nghiệm thu beta |

Khi hoàn thành từng mức, ghi bản build hoặc commit, phiên bản API, môi trường, thiết bị và kết quả vào bàn giao. Với lỗi chặn nghiệm thu, ghi tình huống và bước cần kiểm tra lại.

## 3. Sau beta

1. Bảng tin và hồ sơ cá nhân.
2. Đề xuất câu hỏi của sinh viên và luồng duyệt của giảng viên.
3. Thông báo và tin nhắn.
4. Thành tích và phần thưởng.

Nghiệm thu iOS ở đợt sau Android. Đánh giá nhu cầu thao tác quản trị trên điện thoại sau khi các luồng chính ổn định.
