# 📦 SQB Frontend Features Architecture (Domain-Driven Modules)

Cấu trúc thư mục `features/` được tổ chức bám sát theo 23 Module của Backend đã được đặc tả tại `BACKEND_API_SPECIFICATION.md`:

```
src/features/
├── auth/           # Module 1 & 2: Xác thực, Đăng nhập, OTP, 2FA, Thiết bị (Phase 1)
├── profile/        # Module 3, 5, 6, 7: Hồ sơ cá nhân, Bạn bè, Follow, Block (Phase 2)
├── feed/           # Module 4, 8, 9, 10, 14: Bảng tin, Bài viết, Bình luận, Tương tác (Phase 3)
├── session/        # Module 15: Đề xuất câu hỏi sinh viên & Lịch sử nộp (Phase 4)
├── lecturer/       # Module 16: Workspace duyệt đề xuất câu hỏi của Giảng viên (Phase 5)
├── practice/       # Module 17: Luyện tập trắc nghiệm & Tương tác câu hỏi (Phase 6)
├── chat/           # Module 11: Realtime STOMP WebSocket Chat 1-1 & Nhóm (Phase 7)
├── gamification/   # Module 18, 19, 23: BXH, XP/Level, Shop Avatar Frames, Dự đoán (Phase 8)
├── exam/           # Module 21, 22: Sinh đề thi, Xuất đề thi PDF/Word (Phase 9)
├── notification/   # Module 12: Realtime STOMP Push Notifications (Phase 10)
├── search/         # Module 13: Tìm kiếm toàn cục đa thực thể (Phase 10)
└── admin/          # Module 20: Quản trị người dùng & Báo cáo vi phạm (Phase 10)
```

## Chuẩn Quy ước trong từng Feature (Feature Internal Anatomy):
Mỗi thư mục feature tuân thủ 4 thành phần con:
- `api/`: Các hàm gọi API (sử dụng `axiosClient`) và custom React Query hooks (`useQuery`, `useMutation`).
- `components/`: Các component giao diện thuộc về nghiệp vụ của feature đó.
- `types/`: Kiểu dữ liệu TypeScript đặc tả cho Request/Response và state nội bộ.
- `hooks/`: Các custom hook xử lý business logic độc lập với UI.
