## Triển khai production SQB

Push lên `master` để chạy quy trình production.

- Frontend tự build và triển khai theo `frontend/**` trong workflow riêng.
- Workflow **Deploy SQB Production Stack** chỉ build image backend hoặc AI sidecar nếu mã của service đó thay đổi. Nếu cùng lần push có Compose mới, workflow triển khai file đó cùng digest vừa build. Nếu chỉ đổi Compose, workflow dùng lại image đã triển khai.
- Các mặc định không nhạy cảm như giới hạn upload, ngưỡng R2 và model AI nằm trong `docker-compose.prod.yml`. Biến trùng tên trong `/opt/sqb/.env.prod` sẽ ghi đè mặc định. Khi chuyển một giá trị sang quản lý theo Git, kiểm tra giá trị trong Compose trước khi bỏ dòng ghi đè trên VPS.
- Bí mật ứng dụng nằm trong `/opt/sqb/.env.prod`. GitHub Actions Secrets cấp thông tin SSH cho workflow triển khai. Các biến `VITE_*` dùng trong mã frontend được đưa vào bundle trình duyệt.

### Bật tự áp dụng `.env.prod` trên VPS

Sau khi lượt Stack đầu tiên với workflow mới chạy thành công, đăng nhập VPS bằng tài khoản SSH mà GitHub Actions dùng để deploy. Tài khoản này cần đọc `.env.prod` và dùng được Docker. Chạy các lệnh sau một lần.

```bash
cd /opt/sqb
chmod 600 .env.prod
sudo install -m 0644 .deploy-tools/sqb-production-env@.path .deploy-tools/sqb-production-env@.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now "sqb-production-env@$(id -un).path"
sudo systemctl start "sqb-production-env@$(id -un).service"
```

Các lần lưu `.env.prod` sau đó sẽ dùng image hiện có để áp dụng các thay đổi ảnh hưởng đến backend hoặc AI sidecar. Nên lưu các thay đổi liên quan trong cùng một lần. Kiểm tra kết quả bằng `sudo journalctl -u "sqb-production-env@$(id -un).service" -n 40 --no-pager`.

VPS giữ một bản env đã áp dụng tại `/opt/sqb/.deploy-images/applied.env` với quyền `0600`. Khi readiness thất bại, script dùng bản đó để thử khôi phục service ứng dụng và báo lỗi qua systemd. File bạn vừa chỉnh vẫn là bản đang chờ áp dụng. Workflow Git dùng bản env đã áp dụng để lần deploy sau không dùng lại một chỉnh sửa đang lỗi.

Các thay đổi env ảnh hưởng đến PostgreSQL, Redis hoặc hạ tầng khác cần được phối hợp trong workflow Stack thủ công. Có thể chạy **Run workflow** và để trống `services` khi khởi tạo hoặc khôi phục toàn stack. Khi chọn một nhóm service và áp dụng env đang chờ, nhóm đó phải bao gồm các service chịu ảnh hưởng từ lần chỉnh env. Thay đổi trực tiếp GitHub Actions Variables trong giao diện cần kích hoạt lại workflow thích hợp.
