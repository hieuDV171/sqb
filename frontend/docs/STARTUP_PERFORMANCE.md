# Kiểm tra tải trang đầu — 19/11/2026

## Điểm nghẽn xác nhận được

Phần tử `p` hoặc `h1` được đánh dấu LCP là nội dung lớn xuất hiện trong viewport; dấu đánh dấu không chứng minh chính thao tác render đoạn chữ gây chậm.

Luồng cũ:

```text
HTML/CSS/JS → refresh cookie → /users/me → AppLayout JS → page JS → API nội dung → paint
```

- Gate chặn render đến khi refresh và hồ sơ hoàn tất; hai lớp lazy/Suspense khiến mã khung và trang tải nối tiếp. Hook nội dung chỉ chạy sau đó.
- Login/account setup được import ngay trong entry, kéo Firebase vào khởi động dù đang mở bảng tin.
- Hồ sơ bootstrap không có trong query cache, nên Header/ô tạo bài gọi `/users/me` thêm lần nữa.
- Có nhiều chunk dùng chung nhỏ; độ trễ mỗi lượt tải ảnh hưởng rõ trên mạng giới hạn kết nối. Thử preload trước khi auth xong cho thấy có thể tranh kết nối với request phiên; không dùng cách này.

Điều hướng SPA dùng lại phiên, mã JS và cache nên nhanh hơn tải tài liệu lần đầu. Chuyển route thông thường không tương đương một lần đo LCP navigation mới.

## Bản sửa

- Dùng chung loader giữa React.lazy và `routeModules.ts`; sau khi phiên/hồ sơ hợp lệ, bắt đầu tải mã khung và trang được yêu cầu cùng lúc. Giữ route guards.
- Lazy login/account setup với Suspense/error boundary; Firebase Push của layout import sau khi layout mount, dọn listener khi unmount.
- Gom icon và Base UI primitives dùng chung bằng `build.rolldownOptions.output.codeSplitting.groups`, giữ các route/video lazy. Giảm số request nhỏ trên đường tải trang đầu.
- Đưa hồ sơ đã xác nhận vào cache `['userProfile', 'me']` trước khi mở gate. Không persist token/hồ sơ.
- Màn hình khôi phục dùng logo sách SQB, nền theo theme, chuyển động transform/opacity và live status; hỗ trợ reduced motion.
- User Timing cục bộ: `sqb:session-lock-wait`, `sqb:auth-refresh`, `sqb:session-profile`, `sqb:session-restore`; không gửi telemetry hoặc ghi credential.

Việc gom nhóm tăng JS được HTML nạp trực tiếp từ **553,3 lên 651,2 kB** (gzip **177,1 lên 211,0 kB**), vì UI dùng chung được chuyển về một nhóm tải sớm. Đổi lại, giảm số lượt tải nhỏ trước khi trang sẵn sàng; đây là tối ưu chi phí request, không phải giảm kích thước entry.

## Kiểm chứng và giới hạn

Fixture `.tmp/startup-performance/server.mjs` phục vụ hai bản build với dữ liệu tổng hợp trên localhost; không đăng nhập/ghi dữ liệu production. Điều kiện chung: JS thêm 120 ms/request, refresh 450 ms, hồ sơ 350 ms, feed 300 ms, gzip, no-store, HTTP/1.1. JSON và ảnh ở `.tmp/startup-performance/`.

Fixture xác nhận `/users/me` giảm từ hai lần xuống một lần; không có lỗi JavaScript trong lượt đo bảng tin. Kết quả mô phỏng không đại diện cho HTTP/2/HTTP/3 hoặc tốc độ VPS.

Một cặp đo cùng viewport **1280 × 900**, tab visible, trước/sau bản sửa cuối:

| Chỉ số | Trước | Sau |
|---|---:|---:|
| LCP | 4.588 ms | 3.444 ms |
| Nội dung feed có trong DOM | 4.533 ms | 3.376 ms |
| Request JS trong 8 giây đầu | 85 | 47 |
| Tổng JS đã tải, gzip | 352,2 kB | 340,6 kB |
| `/users/me` | 2 | 1 |

Đây là một cặp đo mô phỏng, không phải benchmark nhiều mẫu. Hai JSON chốt ở `.tmp/startup-performance/before-final.json` và `after-final.json`; bản thử preload sớm đã bị loại khỏi triển khai cuối.

Chưa có trace đầy đủ của phiên production để phân bổ chính xác 3,86 s và 2,63 s trong ảnh người dùng. Không kết luận có N+1/DB chậm từ phần tử LCP. Lượt sửa không thêm backend query/entity/schema/Liquibase hoặc đổi upload.

## Đo sau triển khai

1. Mở trực tiếp `/feed` hoặc `/shop`, chọn **Record and reload**; giữ cùng viewport/throttling và so sánh nhiều lượt.
2. Kiểm tra document TTFB, CSS/font/JS, long tasks và bốn mốc `sqb:*`. `session-restore` bao gồm lock + refresh + profile; không cộng nó thêm vào các phần con.
3. Network cần một `/users/me` lúc bootstrap; mã khung và trang bắt đầu cùng đợt sau profile. Đối chiếu thời điểm API nội dung trả về với LCP breakdown để phân biệt server, tải mã và main thread.

Tham khảo: [Google — Optimize LCP](https://web.dev/articles/optimize-lcp), [Vite — Async Chunk Loading Optimization](https://vite.dev/guide/features.html#async-chunk-loading-optimization), [Rolldown — Manual Code Splitting](https://rolldown.rs/in-depth/manual-code-splitting).
