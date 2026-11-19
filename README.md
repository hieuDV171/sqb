# Học viện SQB

**SQB/SNSQB (Social Network Student-generated Question Bank)** là nền tảng học tập dành cho sinh viên và giảng viên có email được ADMIN hệ thống cung cấp, kết hợp mạng xã hội học đường với ngân hàng câu hỏi do cộng đồng đóng góp.

[Mở Học viện SQB](https://hieudv.id.vn/)

## Tính năng chính

- Trao đổi qua bảng tin, bình luận và tin nhắn. Xem video bài giảng theo môn học.
- Đóng góp câu hỏi theo lớp học phần và luyện tập với câu hỏi đã được giảng viên duyệt.
- Giảng viên tạo đề từ ngân hàng câu hỏi và xuất đề thi dưới dạng PDF hoặc Excel.

## Công nghệ

Frontend dùng React, TypeScript và Vite. Backend dùng Spring Boot, PostgreSQL và Redis, với dịch vụ AI hỗ trợ chạy trên FastAPI.

[Kiến trúc hệ thống](ARCHITECTURE.md) · [Tài liệu API (Swagger UI)](https://hieudv.id.vn/api/v1/docs)

- **TODO sau khi các nhóm bảo vệ xong:** phát hành bản backend loại bỏ toàn bộ tương thích xác thực cũ: luồng `LEGACY_JSON`, cờ `AUTH_LEGACY_JSON_ENABLED` cùng cấu hình/mã hỗ trợ, fallback nhận RT thiếu claim kênh qua cookie/body và cảnh báo/tài liệu/test dành riêng cho phần đã bỏ. Kiểm thử từ chối các phiên cũ và yêu cầu đăng nhập lại; giữ WEB cookie và `/auth/native/*` chuẩn. Tắt cờ hiện tại chỉ chặn nhánh JSON, chưa hoàn thành việc dọn code và không chặn fallback RT thiếu kênh qua cookie WEB; không có cơ chế tự xóa theo ngày.
