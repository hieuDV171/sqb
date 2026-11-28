# Campus Intelligence — Chuẩn UX/UI của SQB

SQB giúp sinh viên trao đổi trong cộng đồng học đường, tìm và luyện câu hỏi theo môn, rồi đóng góp câu hỏi để giảng viên duyệt. Campus Intelligence dùng chung nhận diện và thuật ngữ nghiệp vụ trên các client. Người dùng cần biết họ đang ở đâu, thao tác nào khả dụng và dữ liệu nào đã được lưu.

## Ba chế độ sử dụng

| Chế độ | Ưu tiên | Mẫu từ Webclient |
| --- | --- | --- |
| **Social** | Đặt nội dung và người đăng ở trung tâm. Hành động xã hội đi cùng nội dung. Khi cập nhật bảng tin, giữ vị trí đọc để người dùng tự chọn lúc xem bài mới. | [Thẻ Bảng tin](../../webclient/src/features/feed/components/PostFeedCard.tsx), [cập nhật Bảng tin](../../webclient/src/features/feed/hooks/useActivityFeed.ts) |
| **Study** | Ưu tiên môn học, nội dung câu hỏi và tiến độ. Hiển thị kết quả và lời giải theo dữ liệu máy chủ sau khi trả lời. Giữ bộ lọc khi quay về kho. | [Kho luyện tập](../../webclient/src/pages/session/PracticeCatalogPage.tsx), [màn luyện tập](../../webclient/src/pages/session/PracticeWorkspacePage.tsx), [thẻ câu hỏi](../../webclient/src/features/practice/components/PracticeQuestionCard.tsx) |
| **Management** | Đưa phạm vi xử lý, bộ lọc và trạng thái lên trước. Nêu rõ tác động khi yêu cầu xác nhận. Phiên đã chốt hiển thị ở chế độ chỉ đọc. | [Hàng đợi giảng viên](../../webclient/src/pages/lecturer/PendingReviewsPage.tsx), [màn duyệt và chốt phiên](../../webclient/src/pages/lecturer/ReviewWorkspacePage.tsx), [báo cáo quản trị](../../webclient/src/pages/admin/AdminReportsPage.tsx) |

Dùng nhất quán các tên tiếng Việt như **Ngân hàng câu hỏi**, **Kho luyện tập**, **Đề xuất câu hỏi** và **Chốt phiên**. Giữ ngữ cảnh khi quay lại danh sách hoặc chuyển từ danh sách vào chi tiết. Điều hướng hiển thị theo vai trò, quyền thao tác và phạm vi dữ liệu tuân theo backend. [Điều hướng theo vai trò của Webclient](../../webclient/src/layouts/navigation.ts)

## Nhận diện và token

Dùng ba tầng token. Giá trị gốc được ánh xạ thành vai trò ngữ nghĩa, rồi được dùng trong quy cách thành phần. Blue/Navy tạo nhận diện SQB. Các màu học tập, thành tích và phần thưởng có nghĩa nhất quán trong cả hai chế độ sáng/tối. Component lấy màu từ theme theo vai trò.

| Vai trò | Sáng | Tối | Cách dùng |
| --- | --- | --- | --- |
| Primary | `#2563EB` | `#60A5FA` | Hành động chính, liên kết, đích điều hướng đang chọn |
| Academic | `#0F766E` | `#2DD4BF` | Môn học và trạng thái học tập |
| Gamification | `#7C3AED` | `#A78BFA` | Thành tích, cấp độ và huy hiệu |
| Reward | `#D97706` | `#FBBF24` | Xu và phần thưởng |
| Background | `#F6F8FB` | `#0B1220` | Nền ứng dụng |
| Surface | `#FFFFFF` | `#121B2B` | Thẻ và panel, tương ứng `--card` trên web |
| Foreground | `#0F172A` | `#F1F5F9` | Nội dung chính |

- Dùng thang khoảng cách bội số 4. Mốc tham chiếu trên web là bo góc 12 px cho điều khiển và 16 px cho thẻ. Phân cấp bằng nền và đường viền trước khi thêm bóng.
- Kiểu chữ có cấp bậc rõ. Geist Variable là font Webclient, với cỡ thân bài tham chiếu 16 px. Nội dung học thuật dài có khoảng cách dòng thoáng. Trên Flutter, dùng `TextTheme` và kiểm tra tiếng Việt có dấu ở cỡ chữ phóng to.
- Webclient dùng CSS và Tailwind, Flutter ánh xạ vai trò màu sang `ColorScheme` và bổ sung vai trò riêng của SQB bằng theme. Dùng các cặp màu nền và chữ đủ tương phản trong cả hai chế độ. Mã màu trong bảng biểu thị vai trò, còn trạng thái cần chữ hoặc biểu tượng đi kèm màu.

[Token gốc](../../webclient/src/styles/tokens/primitives.css) · [Token ngữ nghĩa](../../webclient/src/styles/tokens/semantic.css) · [Token thành phần](../../webclient/src/styles/tokens/components.css) · [Kiểu chữ và theme web](../../webclient/src/index.css) · [Dấu hiệu nhận diện SQB](../../webclient/src/components/common/BrandMark.tsx)

## Trạng thái và tương tác

| Trạng thái | Hành vi cần có |
| --- | --- |
| Tải | Phản hồi ngay khi đang tải. Skeleton giữ chỗ theo cấu trúc nội dung. Khi tải thêm, giữ các mục đang hiển thị. |
| Trống | Nêu rõ chưa có dữ liệu hay bộ lọc hiện tại không có kết quả. Đưa ra bước tiếp theo phù hợp nếu có. |
| Lỗi | Đặt thông báo gần vùng hoặc trường bị ảnh hưởng. Với thao tác có thể lặp lại, cung cấp **Thử lại** và giữ nội dung người dùng đã nhập. |
| Thành công | Hiện dữ liệu, trạng thái đã lưu và bước tiếp theo. Khi tải thêm thất bại, giữ trang hiện có và cho thử lại phần tải thêm. |

Khi dữ liệu nhập sai, hiển thị lỗi bằng tiếng Việt ngay dưới trường hoặc nhóm trường liên quan. Liên kết thông báo với trường nhập cho trình đọc màn hình và đưa tiêu điểm đến trường lỗi đầu tiên khi gửi form. Hiển thị lỗi gửi, lưu hoặc tải dữ liệu trong form, vùng thao tác hoặc màn hình đang bị ảnh hưởng. Giữ nội dung để người dùng sửa và thử lại. [Form Webclient](../../webclient/src/components/ui/form.tsx) · [Lỗi thao tác](../../webclient/src/components/ui/action-error.tsx) · [Lỗi tải dữ liệu](../../webclient/src/components/ui/error-state.tsx)

Màn luyện tập giữ đáp án và kết quả do máy chủ trả về. Nội dung câu hỏi, lời giải và dữ liệu tiến độ cần đọc được khi phóng to. Form có nhãn hiển thị, nút chỉ có biểu tượng có tên truy cập được và thao tác đang gửi có phản hồi. Tôn trọng cài đặt giảm chuyển động. [Ví dụ trạng thái tải và tải lại](../../webclient/src/pages/session/PracticeCatalogPage.tsx) · [Ví dụ khôi phục kết quả](../../webclient/src/features/practice/components/PracticeQuestionCard.tsx)

## Điều chỉnh theo nền tảng

| Nền tảng | Cách áp dụng và nghiệm thu |
| --- | --- |
| **Webclient** | Điều hướng dùng sidebar hoặc thanh dưới tùy chiều rộng, URL giữ ngữ cảnh bộ lọc và liên kết chi tiết. Kiểm tra ở 375, 768, 1024 và 1440 px, giao diện sáng/tối và thao tác bàn phím. Focus cần nhìn thấy và trở về vị trí hợp lý sau khi đóng hộp thoại. |
| **MobileClient** | Ưu tiên tác vụ đang làm, điều hướng Quay lại theo ngữ cảnh và vùng chạm từ 48 dp trên Android. Bố trí hành động để safe area hoặc bàn phím không che. Kiểm tra TalkBack, chữ phóng to, giao diện tối và nối lại sau khi ứng dụng về nền. |

Khi bổ sung một quy tắc dùng chung, cập nhật tài liệu này cùng phần triển khai kiểm chứng được trên client liên quan. Các quyết định riêng theo nền tảng nằm trong hướng dẫn hoặc roadmap của module. [ADR-013 và ADR-101](../../ARCHITECTURE.md)
