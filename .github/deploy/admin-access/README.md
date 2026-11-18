# Chuyển quản trị SQB sang admin.hieudv.id.vn

Thiết kế được chốt ngày 18/11/2026, dựa trên Ubuntu 24.04.1 và Caddyfile người dùng cung cấp. Các file trong thư mục này là cấu hình chuẩn bị để áp dụng; chưa được cài lên VPS.

```text
Trình duyệt → Cloudflare Access → Tunnel (kiểm tra JWT Access)
             → HTTP 127.0.0.1:9080 → Caddy → 127.0.0.1:8085
                                             Spring /api/v1
```

Chỉ hostname admin đi qua Tunnel này. Giữ nguyên DNS và đường phục vụ `hieudv.id.vn`, `www.hieudv.id.vn`, `snsqb.duckdns.org`, các hostname `topic*`, `cam*` và R2. Không mở cổng 9080 hoặc 8085 ra Internet. Chạy cloudflared native trên VPS, không chạy container với localhost khác network namespace.

## 1. Chuẩn bị ứng dụng

Mã frontend mới dùng cùng bản build tại `/var/www/sqb-frontend/dist` cho cả hai hostname. API và SockJS tự chọn origin hiện tại trên các hostname production, dù biến build cũ còn trỏ về domain gốc. Đặt GitHub Environment `production` → Variables:

```text
VITE_API_BASE_URL=/api/v1
FRONTEND_BASE_URL=https://hieudv.id.vn
```

Giữ health check workflow ở domain công khai để không cần service token Access trong CI. Không đưa service token Access vào frontend hoặc biến `VITE_*`.

`docker-compose.prod.yml` mới bật `APP_COOKIE_SECURE=true` và thêm origin admin vào `APP_CORS_ALLOWED_ORIGINS`. `server.forward-headers-strategy=framework` đã có trong application.yaml. Listener Caddy mới đặt Host/X-Forwarded-Host admin và X-Forwarded-Proto HTTPS khi proxy tới backend.

Chuẩn bị các biến ở bước này; triển khai backend/Compose và frontend mới sau bước 5 khi Access và Tunnel đã sẵn sàng. Không cần schema mới hoặc Liquibase. Một frontend artifact phục vụ hai origin; cookie refresh vẫn host-only và localStorage riêng. Quản trị viên đăng nhập SQB lại tại hostname admin. Không chuyển JWT trong URL và không mở Domain cookie ra `.hieudv.id.vn`.

## 2. Tạo Access trước khi công bố hostname

1. Mở Cloudflare Zero Trust, chọn gói Free và đặt team name, ví dụ `sqb-hieu` nếu còn trống. Team name được dùng cho trang `<team>.cloudflareaccess.com`.
2. Trong **Zero Trust → Integrations → Identity providers**, thêm phương thức đăng nhập. Để nghiệm thu ban đầu có thể dùng **One-time PIN** gửi tới email; email OTP không mặc nhiên tương đương MFA. Nếu dùng IdP/MFA, cấu hình và kiểm tra chính sách tương ứng.
3. Vào **Access controls → Applications → Add an application → Self-hosted**.
4. Đặt tên `SQB Administration`, public hostname **admin.hieudv.id.vn**, **để Path trống** để bảo vệ cả hostname, session đề xuất 8 giờ.
5. Tạo policy **Allow**, Include **Emails** và điền từng email quản trị được cấp quyền. Không dùng Everyone, toàn bộ `@gmail.com`, hoặc Bypass. Chọn đúng login method ở bước 2.
6. Lưu ứng dụng và ghi lại **Application Audience (AUD) Tag** cùng team name. Đây không phải JWT đăng nhập SQB.
7. Bật **Return 401 response for non-browser traffic** nếu có trong Advanced settings. Frontend admin gửi `X-Requested-With: XMLHttpRequest` để nhận lỗi xác thực rõ ràng cho AJAX thay vì tự đi theo trang đăng nhập HTML. Khi có 401/403 ngoài envelope Spring, frontend yêu cầu tải lại trang để xác thực Access, không refresh hoặc xóa phiên SQB và không tự gửi lại thao tác ghi.

Tài liệu: [Tạo ứng dụng Access](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/), [One-time PIN](https://developers.cloudflare.com/cloudflare-one/integrations/identity-providers/one-time-pin/), [phiên và AJAX](https://developers.cloudflare.com/cloudflare-one/access-controls/access-settings/session-management/).

## 3. Thêm listener Caddy nội bộ, giữ đường cũ để nghiệm thu

Từ máy có repository, chép hai file cấu hình lên VPS. Ví dụ PowerShell, thay địa chỉ SSH bằng thông tin đang dùng:

```powershell
$sqbServer = 'hieudv@YOUR_VPS_IP'
scp .github/deploy/admin-access/admin-listener.caddy "${sqbServer}:/tmp/sqb-admin.caddy"
scp .github/deploy/admin-access/Caddyfile.cutover "${sqbServer}:/tmp/sqb-Caddyfile.cutover"
```

Trên VPS:

```bash
sudo ss -ltnp '( sport = :9080 )'
sudo cp -an /etc/caddy/Caddyfile /etc/caddy/Caddyfile.before-admin-access
sudo install -o root -g root -m 0644 /tmp/sqb-admin.caddy /etc/caddy/sqb-admin.caddy
sudo nano /etc/caddy/Caddyfile
```

Lệnh ss đầu tiên phải xác nhận 9080 chưa bị dịch vụ khác dùng. Chỉ thêm một dòng ở cuối Caddyfile hiện hành:

```caddyfile
import /etc/caddy/sqb-admin.caddy
```

Sau đó:

```bash
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
curl -I -H 'Host: admin.hieudv.id.vn' http://127.0.0.1:9080/login
sudo ss -ltnp '( sport = :9080 )'
```

Trang login trả 200; listener phải chỉ là `127.0.0.1:9080`, không phải `0.0.0.0:9080` hay `[::]:9080`. HTTP loopback không dùng chứng chỉ public; TLS của trình duyệt kết thúc ở Cloudflare. Caddy vẫn giữ HTTPS cho các website cũ. Listener admin từ chối Origin khác `https://admin.hieudv.id.vn` nếu request có Origin, nên không mở quyền gọi API admin từ JavaScript của website public. Các request cùng origin và navigation không có Origin vẫn dùng được.

## 4. Tạo Tunnel trên Cloudflare

1. Vào **Networking → Tunnels → Create a tunnel**, đặt tên `sqb-admin`.
2. Chọn Linux/Ubuntu và đúng kiến trúc máy (`uname -m` trên VPS). Chạy lệnh cài chính thức Dashboard cung cấp và cài tunnel thành systemd service. Không gửi token connector vào chat/Git.
3. Đợi tunnel **Healthy**. Firewall phải cho cloudflared kết nối outbound tới Cloudflare (cổng 7844 TCP/UDP theo giao thức được dùng); không cần mở inbound cho tunnel.
4. Trong **Routes → Add route → Published application**, dùng:

| Trường | Giá trị |
|---|---|
| Public hostname | `admin.hieudv.id.vn` |
| Path | Trống |
| Service type | `HTTP` |
| Service URL | `127.0.0.1:9080` |
| HTTP Host Header | `admin.hieudv.id.vn` |
| Protect with Access | **Bật** |
| Team / AUD | Team và AUD của `SQB Administration` ở bước 2 |

5. Lưu route. Hostname admin phải dùng CNAME proxied của tunnel do Dashboard tạo. Nếu admin đã có A/AAAA/CNAME, xử lý bản ghi xung đột ở riêng hostname đó. Không tạo A về IP public VPS cho admin.

**Access policy và kiểm tra JWT tại connector phải có trước khi đưa vào sử dụng.** Nếu giao diện không cho thiết lập Protect with Access/AUD, dừng công bố route để cấu hình kiểm tra token theo tài liệu; không coi một header có tên `Cf-Access-Jwt-Assertion` là bằng chứng xác thực chữ ký.

Tài liệu: [tạo Tunnel](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/), [Host header và kiểm tra JWT](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/origin-parameters/).

## 5. Thêm origin admin vào R2 CORS

Trong **R2 → bucket → Settings → CORS policy**, thêm `https://admin.hieudv.id.vn` vào `AllowedOrigins` của các rule hiện có cho các bucket cần dùng. Giữ các origin/method/header cũ.

- `sqb-public`: luồng PUT bằng URL ký cho avatar, huy hiệu và media; cần cho origin admin dùng PUT với Content-Type, cùng các method/header mà rule hiện hành đã có.
- `sqb-secret`: nếu trình duyệt fetch bằng chứng hoặc file ký trực tiếp từ bucket, bổ sung origin admin vào rule GET/HEAD tương ứng. Không bật public access cho bucket này.
- Nếu custom domain media có rule Origin riêng, đồng bộ hostname admin. CORS không thay thế chữ ký URL hoặc quyền backend.

Không thay public URL media, bucket, object key hoặc quy trình confirm permanent. Kiểm tra tải một ảnh huy hiệu mới và xem bằng chứng/file xuất trước khi nghiệm thu.

Tài liệu: [R2 CORS](https://developers.cloudflare.com/r2/buckets/cors/).

## 6. Nghiệm thu admin rồi khóa đường cũ

Đưa thay đổi lên Git và triển khai Compose/backend cùng frontend qua các workflow hiện có trước khi nghiệm thu. Chưa áp dụng `Caddyfile.cutover` ở bước này. Bản build mới sẽ chuyển người dùng vào khu quản trị qua hostname mới, nên không phát hành trước khi các bước Access/Tunnel sẵn sàng.

Tắt VPN khi thử bằng trình duyệt:

- Mở `https://admin.hieudv.id.vn/`: thấy Access trước khi tải ứng dụng SQB.
- Email có quyền vào Access được; email ngoài danh sách bị từ chối. Sau Access vẫn cần đăng nhập tài khoản SQB có vai trò ADMIN.
- Trong DevTools, login/refresh/API quản trị đi tới `https://admin.hieudv.id.vn/api/v1/...`; SockJS đi tới cùng hostname tại `/api/v1/ws`. Không có request kèm JWT admin về API domain gốc.
- Refresh cookie Secure, HttpOnly, host admin, Path `/api/v1/auth`; không có Domain chia sẻ. Đăng nhập ở domain gốc không tự mang phiên sang admin.
- F5 trang chi tiết, phiên SQB hết hạn, phiên Access hết hạn, tải ảnh huy hiệu, bằng chứng, xuất file và thông báo/WebSocket đều được kiểm tra. FCM/service worker và quyền thông báo thuộc origin mới; xin quyền lại nếu muốn dùng.

Sau khi các ca trên qua, áp dụng **Caddyfile.cutover** (file đầy đủ đã giữ các site `topic*`, `cam*` theo cấu hình được cung cấp):

```bash
sudo caddy validate --config /tmp/sqb-Caddyfile.cutover --adapter caddyfile
sudo install -o root -g root -m 0644 /tmp/sqb-Caddyfile.cutover /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

Nếu file Caddy đang chạy đã có thay đổi khác kể từ bản cung cấp, ghép riêng block SQB và import, không ghi đè các thay đổi mới bằng file mẫu cũ.

Kết quả cuối:

- API `/api/v1/admin*`, `/api/v1/auth/admin*` trên cả ba hostname public trả 403, cả khi đi Tailscale hoặc localhost.
- Các trang `/admin` và `/admin/...` ở public chuyển sang hostname admin bằng redirect 302. React Router cũng đổi origin khi điều hướng nội bộ. Không chuyển token giữa origin.
- Những chức năng public/giảng viên và website khác giữ đường phục vụ cũ.

Thử ít nhất:

```bash
curl -I https://hieudv.id.vn/admin/users
curl -i https://hieudv.id.vn/api/v1/admin/semesters
curl -i https://www.hieudv.id.vn/api/v1/auth/admin/register
curl -i https://snsqb.duckdns.org/api/v1/admin/semesters
curl -I https://admin.hieudv.id.vn/
```

Ba API public phải 403; curl không có phiên Access không được nhận trang ứng dụng admin. Dùng `curl --resolve` với IP origin thật để xác nhận gọi thẳng public origin cũng bị chặn, kể cả giả mạo header Access. Không có public listener admin; giả Host/SNI không được tới backend quản trị. Kiểm tra riêng các website khác sau reload.

Xóa dòng hosts cũ cho domain gốc trên máy cá nhân một lần. Tailscale giữ cho SSH/Grafana và phục hồi VPS; không dùng để gọi ADMIN qua hostname public sau cutover.

## Khôi phục

Nếu listener mới bị lỗi trong lúc chuẩn bị, khôi phục bản backup rồi validate/reload:

```bash
sudo cp -a /etc/caddy/Caddyfile.before-admin-access /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

Nếu Access/Tunnel lỗi sau cutover, kiểm tra qua Tailscale/SSH trước. Khôi phục Caddyfile cũ chỉ là phương án chủ động tạm mở lại đường quản trị Tailscale trước đây; không xóa Access hoặc tắt kiểm tra JWT để sửa lỗi trên một hostname đã công bố. Có thể tạm gỡ route admin trong Dashboard trong khi khôi phục. Các bước trên không tự khôi phục image/backend hoặc frontend: dùng bản phát hành trước trong workflow nếu cần.
