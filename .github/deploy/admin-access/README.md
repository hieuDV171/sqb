# Chuyển quản trị SQB sang admin.hieudv.id.vn

Thiết kế được chốt ngày 18/11/2026 trên Ubuntu 24.04.1. Luồng Access/Tunnel và cutover đã được kiểm tra trên production ngày 19/11/2026. VPS dùng chung với các project khác: không thay toàn bộ `/etc/caddy/Caddyfile` bằng một bản trong repository.

Cấu hình SQB được tách thành `admin-listener.caddy` và `public-site.caddy`. Script `ensure-caddy.py` mặc định chỉ kiểm tra; `--apply` chỉ cài file SQB còn thiếu và thêm một dòng import nếu cần. Cấu hình đã đúng, kể cả block inline từ bản cutover cũ, thì không sửa file và không reload. Script không chạy trong CI/CD và chưa được chạy trên VPS.

```text
Trình duyệt → Cloudflare Access → Tunnel (kiểm tra JWT Access)
             → HTTP 127.0.0.1:9080 → Caddy → 127.0.0.1:8085
                                             Spring /api/v1
```

Chỉ hostname admin đi qua Tunnel này. Giữ nguyên DNS và đường phục vụ `hieudv.id.vn`, `www.hieudv.id.vn`, `snsqb.duckdns.org`, các hostname `topic*`, `cam*` và R2. Không mở cổng 9080 hoặc 8085 ra Internet. Chạy cloudflared native trên VPS, không chạy container với localhost khác network namespace.

## 1. Chuẩn bị ứng dụng

Mã frontend mới dùng cùng bản build tại `/var/www/sqb-frontend/dist` cho cả hai hostname. API và SockJS tự chọn origin hiện tại trên các hostname production, dù biến build cũ còn trỏ về domain gốc. Workflow đọc `${{ vars.* }}`: có thể dùng Repository Variables đã có, không bắt buộc tạo bản trùng trong Environment `production`. Đặt các giá trị sau tại nơi đang quản lý; nếu có cả hai cấp, đồng bộ giá trị để tránh phụ thuộc thời điểm nạp biến và thứ tự ưu tiên:

```text
VITE_API_BASE_URL=/api/v1
FRONTEND_BASE_URL=https://hieudv.id.vn
```

Giữ health check workflow ở domain công khai để không cần service token Access trong CI. Không đưa service token Access vào frontend hoặc biến `VITE_*`.

`FRONTEND_BASE_URL=https://hieudv.id.vn` giữ nguyên nếu đã có. `VITE_API_BASE_URL` đổi từ URL tuyệt đối của domain gốc sang `/api/v1` khi phát hành nguồn mới; chỉ sửa Variables không tự build/deploy lại frontend.

`docker-compose.prod.yml` mới bật `APP_COOKIE_SECURE=true` và thêm origin admin vào `APP_CORS_ALLOWED_ORIGINS`. `server.forward-headers-strategy=framework` đã có trong application.yaml. Listener Caddy mới đặt Host/X-Forwarded-Host admin và X-Forwarded-Proto HTTPS khi proxy tới backend.

Chuẩn bị các biến ở bước này; triển khai backend/Compose và frontend mới sau bước 5 khi Access và Tunnel đã sẵn sàng. Không cần schema mới hoặc Liquibase. Một frontend artifact phục vụ hai origin; cookie refresh vẫn host-only và localStorage riêng. Quản trị viên đăng nhập SQB lại tại hostname admin. Không chuyển JWT trong URL và không mở Domain cookie ra `.hieudv.id.vn`.

## 2. Tạo Access trước khi công bố hostname

1. Mở Cloudflare Zero Trust, chọn gói Free và đặt team name, ví dụ `sqb-hieu` nếu còn trống. Team name được dùng cho trang `<team>.cloudflareaccess.com`.
2. Trong **Zero Trust → Integrations → Identity providers**, thêm phương thức đăng nhập. Để nghiệm thu ban đầu có thể dùng **One-time PIN** gửi tới email; email OTP không mặc nhiên tương đương MFA. Nếu dùng IdP/MFA, cấu hình và kiểm tra chính sách tương ứng.
   - Nếu đã có **Cloudflare**, có thể dùng ngay tài khoản Cloudflare thay cho OTP. Mở provider để kiểm tra **Restrict to account members**; provider tự tạo cho tổ chức mới mặc định bật tùy chọn này. Chỉ người thuộc account đó xác thực được khi bật, và vẫn phải qua policy ứng dụng. Với quản trị viên bên ngoài chỉ cần truy cập SQB, có thể thêm OTP/IdP khác thay vì cấp quyền thành viên quản lý tài khoản Cloudflare.
3. Vào **Access controls → Applications → Add an application → Self-hosted**.
4. Đặt tên `SQB Administration`, public hostname **admin.hieudv.id.vn**, **để Path trống** để bảo vệ cả hostname, session đề xuất 8 giờ.
5. Tạo policy **Allow**, Include **Emails** và điền từng email quản trị được cấp quyền. Không dùng Everyone, toàn bộ `@gmail.com`, hoặc Bypass. Chọn đúng login method ở bước 2.
6. Lưu ứng dụng và ghi lại **Application Audience (AUD) Tag** cùng team name. Đây không phải JWT đăng nhập SQB.
7. Không cần bật **Return 401 Response** trong phần **Service Authentication failure**: tùy chọn đó chỉ áp dụng khi request bị Service Auth policy chặn, trong khi ứng dụng này dùng Allow theo danh tính người dùng. Tùy chọn **Return 401 response for non-browser traffic** tại **Access controls → Access settings → Cloudflare One Client authentication** cũng không cần cho mô hình web không dùng client này. Frontend admin mới gửi `X-Requested-With: XMLHttpRequest`; theo tài liệu AJAX, header này yêu cầu Access trả 401 khi phiên Access hết hạn. Khi có 401/403 ngoài envelope Spring, frontend yêu cầu tải lại trang để xác thực Access, không refresh hoặc xóa phiên SQB và không tự gửi lại thao tác ghi. Cần kiểm chứng phản hồi thật sau khi deploy frontend mới.

Tài liệu: [Tạo ứng dụng Access](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/), [One-time PIN](https://developers.cloudflare.com/cloudflare-one/integrations/identity-providers/one-time-pin/), [phiên và AJAX](https://developers.cloudflare.com/cloudflare-one/access-controls/access-settings/session-management/).

## 3. Thêm listener Caddy nội bộ, giữ đường cũ để nghiệm thu

Từ máy có repository, chép script và các phần cấu hình SQB lên VPS. Ví dụ PowerShell, thay địa chỉ SSH bằng thông tin đang dùng:

```powershell
$sqbServer = 'hieudv@YOUR_VPS_IP'
ssh $sqbServer 'mkdir -p /tmp/sqb-caddy-setup'
scp .github/deploy/admin-access/ensure-caddy.py .github/deploy/admin-access/admin-listener.caddy .github/deploy/admin-access/public-site.caddy "${sqbServer}:/tmp/sqb-caddy-setup/"
```

Trên VPS:

```bash
sudo ss -ltnp '( sport = :9080 )'
sudo python3 /tmp/sqb-caddy-setup/ensure-caddy.py --site admin
sudo python3 /tmp/sqb-caddy-setup/ensure-caddy.py --site admin --apply
```

Khi cài mới, cổng 9080 phải chưa bị dịch vụ khác dùng. Nếu listener SQB đã được cài, kiểm tra tiến trình Caddy và script sẽ xác nhận cấu hình hiện có. Khi thiếu listener, script tạo `/etc/caddy/sqb-admin.caddy`; nếu các import hiện có chưa nạp file này thì chỉ thêm dòng sau vào cuối Caddyfile:

```caddyfile
import /etc/caddy/sqb-admin.caddy
```

Script adapt/validate toàn bộ cấu hình trước reload. Cấu hình đã đủ thì không reload. Sau đó kiểm tra:

```bash
curl -I -H 'Host: admin.hieudv.id.vn' http://127.0.0.1:9080/login
sudo ss -ltnp '( sport = :9080 )'
```

Trang login trả 200; listener phải chỉ là `127.0.0.1:9080`, không phải `0.0.0.0:9080` hay `[::]:9080`. HTTP loopback không dùng chứng chỉ public; TLS của trình duyệt kết thúc ở Cloudflare. Caddy vẫn giữ HTTPS cho các website cũ. Listener admin từ chối Origin khác `https://admin.hieudv.id.vn` nếu request có Origin, nên không mở quyền gọi API admin từ JavaScript của website public. Các request cùng origin và navigation không có Origin vẫn dùng được.

## 4. Tạo Tunnel trên Cloudflare

1. Vào **Networking → Tunnels → Create a tunnel**, đặt tên `sqb-admin`.
2. Trên Dashboard chọn **Debian → 64-bit** cho Ubuntu 24.04 có `uname -m` là `x86_64` (gói amd64/x64). Ubuntu dùng gói Debian; không cần có nút Ubuntu riêng. Chạy lệnh cài chính thức Dashboard cung cấp trên VPS và cài tunnel thành systemd service. Không chọn Docker cho mô hình listener localhost này. Không gửi token connector vào chat/Git.
   - Ô **Install cloudflared** chứa nhiều lệnh thêm key/repo và cài gói. Chạy `sudo -v` riêng, nhập mật khẩu và đợi trở lại prompt trước khi dán các lệnh; kiểm tra `cloudflared --version` để xác nhận gói đã cài. Nếu paste gặp prompt sudo và không có log apt, không kết luận đã cài xong; chạy lại từng dòng sau khi xác thực sudo.
   - Sau khi có phiên bản, chỉ chạy lệnh **Install as service** và kiểm tra `sudo systemctl status cloudflared --no-pager`. Ô **Or, run tunnel (manual)** là phương án thay thế chạy foreground, không cần chạy thêm song song service.
3. Đợi tunnel **Healthy**. Firewall phải cho cloudflared kết nối outbound tới Cloudflare (cổng 7844 TCP/UDP theo giao thức được dùng); không cần mở inbound cho tunnel.
4. Trong **Routes → Add route → Published application**, dùng:

| Trường | Giá trị |
|---|---|
| Public hostname | `admin.hieudv.id.vn` |
| Path | Trống |
| Service type (nếu có ô riêng) | `HTTP` |
| Service URL | `http://127.0.0.1:9080`; nếu GUI tách riêng type HTTP thì ô URL dùng `127.0.0.1:9080` |
| HTTP Host Header | `admin.hieudv.id.vn` |
| Protect with Access | **Bật** |
| Team / AUD | Team và AUD của `SQB Administration` ở bước 2 |

Giao diện mới có thể chỉ hiện **Service URL**, không có Service type: phải nhập đủ tiền tố `http://`. Trong Additional application settings, mở **HTTP** để điền Host header và **Access** để bật kiểm tra JWT/team/AUD; TLS giữ mặc định vì origin là HTTP loopback.

5. Lưu route. Hostname admin phải dùng CNAME proxied của tunnel do Dashboard tạo. Nếu admin đã có A/AAAA/CNAME, xử lý bản ghi xung đột ở riêng hostname đó. Không tạo A về IP public VPS cho admin.

Phân biệt các bước: tạo ứng dụng Access chỉ khai báo hostname cần bảo vệ, không tự tạo DNS. Published application route của Tunnel qua Dashboard mới tự tạo bản ghi DNS trỏ tới tunnel. Zone gốc hiện dùng nameserver Cloudflare, nên quản lý bản ghi tại Cloudflare DNS, không tại DNS iNET. iNET vẫn là nơi đăng ký/gia hạn tên miền. Lệnh curl tới `127.0.0.1` với Host admin chỉ thử vhost Caddy cục bộ, không kiểm tra DNS/HTTPS/Access từ Internet.

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

Đưa thay đổi lên Git và triển khai Compose/backend cùng frontend qua các workflow hiện có trước khi nghiệm thu. Chưa chạy `--site public --apply` ở bước này. Bản build mới sẽ chuyển người dùng vào khu quản trị qua hostname mới, nên không phát hành trước khi các bước Access/Tunnel sẵn sàng.

Tắt VPN khi thử bằng trình duyệt:

- Mở `https://admin.hieudv.id.vn/`: thấy Access trước khi tải ứng dụng SQB.
- Email có quyền vào Access được; email ngoài danh sách bị từ chối. Sau Access vẫn cần đăng nhập tài khoản SQB có vai trò ADMIN.
- Trong DevTools, login/refresh/API quản trị đi tới `https://admin.hieudv.id.vn/api/v1/...`; SockJS đi tới cùng hostname tại `/api/v1/ws`. Không có request kèm JWT admin về API domain gốc.
- Refresh cookie Secure, HttpOnly, host admin, Path `/api/v1/auth`; không có Domain chia sẻ. Đăng nhập ở domain gốc không tự mang phiên sang admin.
- F5 trang chi tiết, phiên SQB hết hạn, phiên Access hết hạn, tải ảnh huy hiệu, bằng chứng, xuất file và thông báo/WebSocket đều được kiểm tra. FCM/service worker và quyền thông báo thuộc origin mới; xin quyền lại nếu muốn dùng.

Sau khi các ca trên qua, kiểm tra phần public của SQB:

```bash
sudo python3 /tmp/sqb-caddy-setup/ensure-caddy.py --site public
sudo python3 /tmp/sqb-caddy-setup/ensure-caddy.py --site public --apply
```

Trên VPS đã áp dụng cutover cũ, hai lệnh trên sẽ báo cấu hình đã có và không thay đổi gì. Không cần di chuyển block đang hoạt động chỉ để chạy script này.

Nếu gặp **Existing SQB host configuration differs**, script dừng mà không append hoặc ghi đè. Trường hợp chuyển từ block SQB dùng Tailscale cũ, cần chỉnh một lần bằng `sudoedit`: chỉ thay block của đúng ba hostname `hieudv.id.vn, www.hieudv.id.vn, snsqb.duckdns.org` bằng nội dung `public-site.caddy`, hoặc tách block đó ra `/etc/caddy/sqb-public.caddy` và thay vị trí cũ bằng `import /etc/caddy/sqb-public.caddy`. Không giữ cả block inline và import vì sẽ khai báo trùng. Nếu block có hostname/cấu hình của project khác, ghép lại có rà soát; không xóa toàn block tự động.

Khi chỉnh thủ công, chạy `sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` thành công rồi mới `sudo systemctl reload caddy`. Những lần cập nhật về sau chỉ sửa file SQB tương ứng; script không tự ghi đè file SQB đã tồn tại nhưng khác nội dung. Không có lệnh cài toàn bộ Caddyfile trong quy trình này.

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

Xóa dòng hosts cũ cho domain gốc trên máy cá nhân một lần. Web ADMIN không cần Tailscale. Nếu gỡ VPN, phải thử đường SSH/console thay thế trước; monitoring/Grafana hiện tạm dừng vì RAM, không tự khởi động stack này khi cài Caddy.

## An toàn trên VPS dùng chung và khôi phục

- Script so sánh route JSON từ Caddy adapter, gồm host, listen/bind và toàn bộ handler của site SQB; không chỉ grep một hostname hoặc dòng comment. Group ID do adapter tự sinh được chuẩn hóa để các site khác không gây báo khác giả.
- Site SQB đã đúng: giữ nguyên cả file gốc và file import, không reload. Thiếu site: tạo riêng file SQB, tận dụng import wildcard có sẵn hoặc append một import. File đích khác nội dung hoặc host SQB đã khai báo khác: dừng để người quản lý rà soát.
- Trước append có bản backup tham chiếu `.Caddyfile.before-sqb-*` ngay cạnh Caddyfile. Validation/reload lỗi: chỉ gỡ phần append của lần chạy đó và file SQB vừa tạo nếu chưa có ai sửa chúng. Không lấy backup cũ ghi đè toàn bộ cấu hình hiện tại.
- Script dùng advisory lock `/etc/caddy/.sqb-caddy.lock` và kiểm tra thay đổi trước khi ghi/reload. Quản trị viên khác cần phối hợp cùng lock hoặc tránh sửa/reload cùng lúc; lock không chặn được một người sửa trực tiếp bằng trình soạn thảo không dùng lock. Nếu phát hiện thay đổi đồng thời, giữ file hiện tại để rà soát, không phục hồi snapshot đè lên thay đổi mới.
- Không chạy formatter ghi lại toàn bộ Caddyfile và không restart Caddy; chỉ reload sau validation thành công. Các file import tương đối của project khác vẫn được đọc từ vị trí Caddyfile thật.

Nếu cần khôi phục thủ công, chỉ hoàn tác block/import hoặc file SQB vừa sửa sau khi đối chiếu cấu hình mới nhất, validate toàn bộ rồi reload. Không copy `Caddyfile.before-admin-access` hay bản backup cũ lên Caddyfile dùng chung.

Nếu Access/Tunnel lỗi sau cutover, kiểm tra cloudflared, listener và backend qua SSH/console. Không gỡ chặn ADMIN API trên hostname công khai hoặc tắt kiểm tra JWT để sửa lỗi. Có thể tạm gỡ route admin trong Dashboard trong khi khôi phục. Khôi phục app/backend/frontend dùng phiên bản trước qua workflow, độc lập với cấu hình của project khác.

Kiểm thử script trên máy phát triển có Caddy:

```bash
python3 .github/deploy/admin-access/test_ensure_caddy.py --caddy /path/to/caddy
```

Tài liệu: [Caddy import](https://caddyserver.com/docs/caddyfile/directives/import), [adapt/validate/reload](https://caddyserver.com/docs/command-line).
