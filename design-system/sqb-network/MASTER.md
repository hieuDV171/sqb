# SQB Network — Design System Master File (MASTER.md)

> **QUY TẮC BẮT BUỘC:** Mọi màn hình, component hay tính năng UI mới khi xây dựng đều phải tuân thủ các tokens, quy chuẩn màu sắc, typography và micro-interactions được định nghĩa trong file này. Nếu có file ghi đè trong `design-system/sqb-network/pages/[tên-trang].md`, trang đó sẽ tuân theo các quy tắc riêng tại file đó.

---

**Dự án:** SQB Network (Mạng xã hội học thuật & Ngân hàng đề thi sinh viên HUST)  
**Phong cách chủ đạo:** **Modern Academic & Gamified Social** (Kết hợp giữa sự thanh lịch, chuẩn mực của Linear/Notion và tính sống động, hấp dẫn của Discord/Duolingo).  
**Triết lý UI/UX:** Content-first, phân cấp thị giác rõ ràng, micro-interactions tinh tế (150-250ms), chống mỏi mắt khi học tập đêm muộn, hỗ trợ mượt mà cả Light/Dark mode.

---

## 1. Hệ Thống Màu Sắc (Color Palette & Semantic Tokens)

### 1.1 Bảng màu cốt lõi (Core Theme)

| Token | Light Mode Hex | Dark Mode Hex | Ý nghĩa & Vị trí sử dụng |
|---|---|---|---|
| `--color-primary` | `#4F46E5` (Indigo 600) | `#6366F1` (Indigo 500) | Màu nhận diện thương hiệu, nút CTA chính, active state |
| `--color-primary-hover` | `#4338CA` (Indigo 700) | `#4F46E5` (Indigo 600) | Trạng thái hover cho nút chính |
| `--color-secondary` | `#0284C7` (Sky 600) | `#38BDF8` (Sky 400) | Điểm nhấn học thuật, links, thông tin bổ trợ |
| `--color-background` | `#F8FAFC` (Slate 50) | `#0B0F19` (Deep Navy) | Màu nền tổng thể toàn trang |
| `--color-surface` | `#FFFFFF` | `#111827` (Gray 900) | Nền Card bài viết, Sidebar, Dialog, Popover |
| `--color-surface-hover` | `#F1F5F9` (Slate 100) | `#1F2937` (Gray 800) | Hover trên danh sách, menu item |
| `--color-border` | `#E2E8F0` (Slate 200) | `#1E293B` (Slate 800) | Đường phân chia, viền card tinh tế |
| `--color-foreground` | `#0F172A` (Slate 900) | `#F8FAFC` (Slate 50) | Màu chữ tiêu đề chính, văn bản quan trọng |
| `--color-muted` | `#64748B` (Slate 500) | `#94A3B8` (Slate 400) | Màu chữ phụ, metadata, timestamp, placeholder |
| `--color-destructive` | `#EF4444` (Red 500) | `#F87171` (Red 400) | Báo lỗi, nút xoá, thu hồi tin nhắn |

### 1.2 Bảng màu Gamification & Bục Vinh Danh (Podium & Rewards)

| Cấp bậc / Vật phẩm | Gradient / Màu chủ đạo | Ứng dụng cụ thể |
|---|---|---|
| **Hạng 1 (Gold / Cúp vàng)** | `linear-gradient(135deg, #FDE68A 0%, #F59E0B 50%, #D97706 100%)` | Bục số 1, Cúp vàng Top 1 kỳ (300 xu), vòng hào quang Top 1 |
| **Hạng 2 (Silver / Cúp bạc)** | `linear-gradient(135deg, #F1F5F9 0%, #94A3B8 50%, #64748B 100%)` | Bục số 2, Cúp bạc Top 2 kỳ (180 xu) |
| **Hạng 3 (Bronze / Cúp đồng)**| `linear-gradient(135deg, #FFEDD5 0%, #FB923C 50%, #C2410C 100%)` | Bục số 3, Cúp đồng Top 3 kỳ (90 xu) |
| **Đồng Xu (Coin Currency)** | `#EAB308` (Amber 500 / Vàng ánh kim) | Icon xu thưởng, giá vật phẩm trong Shop, số dư |
| **Điểm cống hiến (Points)** | `#8B5CF6` (Purple 500) | Điểm đóng góp đề thi, điểm xếp hạng học kỳ |
| **Huy hiệu Huyền Thoại** | `linear-gradient(135deg, #A855F7 0%, #EC4899 100%)` | Viền badge cấp cao nhất, avatar frame hiếm |

### 1.3 Role Badges (Huy hiệu Phân quyền Trường học)
- **Giảng viên (`TEACHER`)**: Nền `#ECFDF5` viền `#A7F3D0` chữ `#059669` (Emerald tag) kèm icon `GraduationCap`.
- **Bác sĩ/Chuyên gia (`DOCTOR`)**: Nền `#F0F9FF` viền `#BAE6FD` chữ `#0284C7` (Sky tag) kèm icon `Stethoscope`.
- **Sinh viên (`STUDENT`)**: Nền `#EEF2FF` viền `#C7D2FE` chữ `#4F46E5` (Indigo tag).

---

## 2. Typography & Phông Chữ

Dự án sử dụng phông chữ Sans-serif hiện đại, tối ưu cho màn hình độ phân giải cao và giảm mệt mỏi thị giác khi đọc tài liệu học thuật:

- **Font Family Chính:** `Geist Sans`, `Plus Jakarta Sans`, `Inter`, sans-serif.
- **Font Code / Toán học:** `Geist Mono`, `JetBrains Mono`, monospace (dùng cho công thức và snippet bài giảng).

### Kích thước & Phân cấp chữ (Type Scale)

| Cấp độ | Kích thước / Line-height | Font-weight | Ứng dụng |
|---|---|---|---|
| **Display H1** | `28px` - `32px` (leading-tight) | `700` (Bold) | Tiêu đề trang chính, Tên bảng xếp hạng lớn |
| **Heading H2** | `20px` - `24px` (leading-snug) | `600` (Semibold) | Tiêu đề Card, Tên môn học, Section Header |
| **Subheading H3**| `16px` - `18px` (leading-normal) | `600` (Semibold) | Tên người dùng, Tiêu đề câu hỏi, Tên vật phẩm |
| **Body Standard**| `14px` - `15px` (leading-relaxed)| `400` / `500` | Nội dung bài viết, bình luận, chat, mô tả |
| **Caption / Meta**| `12px` - `13px` (leading-normal) | `400` / `500` | Timestamp, số lượng like/share, nhãn role, hints |

---

## 3. Spacing, Bo Góc (Radius) & Shadow

- **Grid Spacing Scale:** Bội số của `4px` (`gap-2: 8px`, `gap-3: 12px`, `gap-4: 16px`, `gap-6: 24px`, `gap-8: 32px`).
- **Bo góc (Border Radius):**
  - Khung Card & Dialog chính: `rounded-2xl` (`16px`)
  - Nút bấm, Ô nhập liệu (Input): `rounded-xl` (`12px`)
  - Tags, Badges, Chips: `rounded-full` hoặc `rounded-lg` (`8px`)
  - Avatar người dùng: `rounded-full`
- **Đổ bóng (Box Shadow):**
  - Card thường: `shadow-sm hover:shadow-md transition-shadow duration-200`
  - Floating Header/Dock: `backdrop-blur-md bg-white/80 dark:bg-gray-900/80 shadow-sm border-b`
  - Modal / Popover: `shadow-xl border border-slate-200/80 dark:border-slate-800`
  - Podium Glow (Top 1): `shadow-[0_0_30px_rgba(245,158,11,0.25)]`

---

## 4. Quy Chuẩn Thành Phần (Component Blueprint)

### 4.1 Nút bấm (Button Standards)
- **Primary:** Nền Indigo, chữ trắng, bo góc `rounded-xl`, chiều cao tối thiểu `40px` (trên desktop) và `44px` (cho touch target mobile).
- **Secondary / Outline:** Nền trong suốt hoặc Slate-100, viền mảnh Slate-200, hover sáng nhẹ.
- **Ghost:** Không viền, chỉ đổi nền khi hover (dùng cho icon buttons ở Header/Chat).
- **Quy tắc:** Mọi button bấm được đều phải có hiệu ứng active: `active:scale-[0.98] transition-transform duration-100`.

### 4.2 Card Bài viết (Post Card)
- Header: Avatar kèm khung trang trí (nếu user có trang bị), Tên người dùng, Role Badge, Thời gian đăng + Privacy icon (Công khai/Trường học).
- Body: Text nội dung có format, hỗ trợ mở rộng "Xem thêm", kèm hình ảnh dạng Grid hoặc video bài giảng.
- Action Bar: Like (Trái tim có animation nhảy nhẹ), Comment, Share, Bookmark, Thống kê lượt xem.

### 4.3 Bục Vinh Danh Học Kỳ (Podium Component)
- **Bố cục 3 bậc:** Bậc 2 bên trái (Silver, thấp nhì), Bậc 1 chính giữa (Gold, cao nhất, có vương miện/hào quang), Bậc 3 bên phải (Bronze, thấp nhất).
- Trên đỉnh mỗi cột: Avatar học sinh kèm huy hiệu thứ hạng, Tên, Số xu nhận thưởng (+300🪙 / +180🪙 / +90🪙) hiển thị nổi bật với badge mạ vàng kim.

### 4.4 Header Bar (Thanh trạng thái HUD)
- Logo SQB Network + Tên trường.
- Thanh tìm kiếm thông minh (`Ctrl + K`).
- **HUD Ví sinh viên**: Hiển thị số Dư Xu (🪙 `320`) và Điểm Cống hiến (⚡ `1,450`), click mở lịch sử giao dịch ví.
- Chuông thông báo (kèm chấm đỏ real-time).
- Profile Menu: Avatar có khung, dropdown cài đặt tài khoản, quản lý thiết bị, đăng xuất.

---

## 5. Bốn Trạng Thái Bắt Buộc Của Mọi Màn Hình (The 4 States Rule)

Mỗi khi AI dựng bất kỳ tính năng nào, tuyệt đối không chỉ dựng màn hình có sẵn dữ liệu:
1. **Loading State:** Sử dụng Skeleton UI tương đồng 1:1 với kích thước và hình dáng của Card thật (không dùng spinner xoay tròn giữa trang).
2. **Empty State:** Khi chưa có dữ liệu (chưa có bài viết, giỏ đồ trống, chưa có bạn bè), phải có hình minh họa thân thiện + một thông điệp cổ vũ + 1 nút hành động (CTA: "Tạo bài viết đầu tiên", "Khám phá câu hỏi").
3. **Populated State:** Trạng thái đầy đủ dữ liệu, mượt mà, phân trang cursor mượt mà (`after`).
4. **Error State:** Khi API trả về lỗi hoặc mất mạng, hiển thị card thông báo rõ ràng kèm nút "Thử lại" (Retry).

---

## 6. Pre-delivery Checklist (Bộ tiêu chí kiểm định trước khi hoàn thành UI)
- [ ] Sử dụng icon SVG từ `lucide-react`, tuyệt đối không dùng Emoji thô sơ làm icon chức năng.
- [ ] Mọi phần tử click được đều có `cursor-pointer` và hiệu ứng hover êm ái (150-250ms).
- [ ] Độ tương phản chữ đạt chuẩn WCAG AA (tối thiểu 4.5:1).
- [ ] Hỗ trợ Responsive toàn diện (Mobile 375px, Tablet 768px, Desktop 1024px+).
- [ ] Tuân thủ triệt để cấu trúc biến CSS đã định nghĩa trong file này.
