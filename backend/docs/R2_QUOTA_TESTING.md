# Kiểm thử hạn mức R2 mà không tải file lớn

SQB có hai lớp kiểm tra độc lập:

- **Từng file:** presign media tối đa 15 MiB theo DTO, sau đó kiểm tra theo mục đích (avatar 2 MiB, cover 4 MiB, document 15 MiB, ảnh thông thường 5 MiB). Video có trần mặc định 2 GiB, cấu hình qua `LECTURE_VIDEO_MAX_FILE_SIZE`.
- **Toàn tài khoản:** khi `R2_STORAGE_QUOTA_ENABLED=true`, cảnh báo ở 9.000.000.000 byte và chặn nếu dung lượng quan sát + phần giữ chỗ chưa được tính + lượt ghi mới vượt 9.500.000.000 byte. Dùng chung cho các đường ghi của SQB, có Redis lock để điều phối nhiều lượt ghi.

## 1. Test local không gọi Cloudflare

Chạy từ thư mục `backend`, với Java 25 và Gradle/dependency đã có trong cache:

```powershell
.\gradlew.bat test --offline --no-daemon `
  --tests com.frozenheart.backend.modules.media.service.R2StorageQuotaTest `
  --tests com.frozenheart.backend.modules.media.service.impl.MediaPresignQuotaTest `
  --tests com.frozenheart.backend.modules.media.service.impl.R2AccountUsageTest `
  --tests com.frozenheart.backend.modules.media.service.LectureVideoUploadServiceTest
```

Trên Linux dùng `./gradlew` và nối dòng bằng `\`. `--offline` áp dụng cho dependency resolution; wrapper vẫn cần bản Gradle đã cài/cache. Nếu wrapper chưa có distribution, chuẩn bị Gradle từ nguồn chính thức trước hoặc gọi trực tiếp bản Gradle đã cài. Không cần `bootRun`, Docker, PostgreSQL, Redis thật, token hoặc bucket R2.

Các test trên tạo đối tượng service trực tiếp; S3, Redis và Cloudflare REST đều là mock. Test ký URL dùng endpoint/credential giả và region cố định, không gửi HTTP. Kích thước hơn 10 GB chỉ là số nguyên trong metadata; stream thử tương ứng chứa **0 byte**. Test xác nhận storage không được gọi khi từ chối. Vì không có request R2 nên không phát sinh usage/bill R2 từ các test này.

| Trường hợp | Kết quả cần có |
| --- | --- |
| Khai báo một document 10.000.000.001 byte | `INVALID_PARAMETER_VALUE`, không giữ chỗ hoặc ký URL |
| Tổng đã dùng 9,4 GB, xin thêm đúng 100 MB | Giữ chỗ thành công, tổng dự kiến đúng 9,5 GB |
| Sau đó xin thêm 1 byte | `STORAGE_QUOTA_EXCEEDED`, không thêm giữ chỗ |
| Xin ghi hơn 10 GB qua direct PUT, presign hoặc multipart | Từ chối cả ba đường, không gọi storage |
| Hai bucket xin 300 MB đồng thời trong khi chỉ còn 500 MB | Chỉ một yêu cầu thành công |
| Chưa có snapshot, snapshot quá 15 phút và API usage lỗi, hoặc Redis lỗi | `STORAGE_QUOTA_UNAVAILABLE`, không ghi |
| Multipart hết hạn nhưng abort thất bại | Vẫn giữ chỗ đến khi abort thành công |
| Đọc inventory | Cộng nhiều trang, các bucket khác trong tài khoản và multipart từ metrics |
| Ký URL media/phần video | `X-Amz-SignedHeaders` chứa `content-length;host` |

Kết quả service tương ứng HTTP `507`/mã `1017` khi đầy và `503`/mã `1018` khi không thể kiểm tra quota. Unit test không chứng minh cấu hình deploy, Redis phân tán thực hoặc việc R2 thực sự kiểm tra chữ ký; các bước dưới kiểm tra các phần đó.

## 2. Xác nhận cấu hình đang chạy, không upload

Code và Compose mặc định quota tổng **tắt**. Bản ghi triển khai ngày 15/11/2026 trong `TASK_TRACKER.md` đã ghi nhận quota bật và log `R2 quota initialized`, nhưng cần kiểm tra lại container hiện hành sau khi thay env/deploy.

Tại thư mục triển khai trên VPS, hai lệnh chỉ đọc dưới đây chỉ in ba giá trị không phải bí mật và log quota:

```sh
docker compose -f docker-compose.prod.yml exec -T backend sh -c 'printf "enabled=%s\nwarning=%s\nlimit=%s\n" "$R2_STORAGE_QUOTA_ENABLED" "$R2_STORAGE_WARNING_BYTES" "$R2_STORAGE_LIMIT_BYTES"'
docker compose -f docker-compose.prod.yml logs --since 24h backend | grep -E 'R2 quota initialized|R2 quota reconciliation failed|R2 storage near quota' | tail -n 20
```

Mong đợi `enabled=true`, warning `9000000000`, limit `9500000000`, và đối soát thành công sau khởi động. Không có log initialization trong 24h không tự chứng minh quota tắt: backend chỉ log initialization khi tạo snapshot đầu tiên, Redis có thể đã giữ snapshot cũ. Khi điều tra sâu hơn, kiểm tra độ mới của `observedAt` trong Redis bằng công cụ nội bộ, không xóa/chỉnh state để ép test trên production. Không xuất toàn bộ env hoặc giá trị token.

Để kiểm tra hợp đồng API giới hạn từng file, có thể chỉ gửi JSON xin presign với `file_size=10000000001`, không gửi body file và không thực hiện PUT. DTO phải trả HTTP 400 và không cấp URL. Ca này kiểm tra giới hạn từng file; nó không thay thế ca quota tổng 507 trong bộ test local.

## 3. Kiểm tra chữ ký R2 thật bằng file vài KiB (tùy chọn)

Chỉ làm khi cần nghiệm thu lớp provider; ưu tiên bucket test thuộc Standard storage và phiên backend/Redis test riêng. Các thao tác R2 thật được tính usage dù file rất nhỏ; không hứa chi phí bằng 0 nếu tài khoản đã hết free tier. Không cần thay ngưỡng 9,5 GB hoặc upload file GB.

1. Chuẩn bị hai file local, kích thước chính xác **1.024 byte** và **2.048 byte**.
2. Xin một presign mới cho document 1.024 byte; gửi đúng file 1.024 byte. Mong đợi HTTP 200.
3. Xin presign khác cho document 1.024 byte, key mới; gửi file **2.048 byte**. Để HTTP client tự đặt `Content-Length=2048` theo body, không tự khai header sai với body. Mong đợi HTTP 403/chữ ký không khớp, không xuất hiện object mới.
4. Nếu bước 3 vẫn thành công, dừng nghiệm thu: đã tìm thấy lỗi với tối đa 2 KiB dữ liệu ở lượt sai, không cần thử file lớn. Không mở rộng kích thước thử.
5. Dọn đúng các object test qua công cụ quản trị storage. Không lưu presigned URL/credential vào log, repo hoặc ảnh chụp. Các object thử không gắn vào bài viết/hồ sơ nên không xác nhận permanent.

Hai lượt PUT chỉ gửi tổng cộng 3.072 byte. Không kiểm thử multipart bằng part 1 KiB: video có quy tắc part riêng. Bộ test local đã kiểm tra chữ ký part và giới hạn video; nghiệm thu multipart thật là một ca riêng có ngân sách rõ ràng.

## Phạm vi bảo vệ chi phí

- Quota byte của SQB không giới hạn số request Class A/B và không phải trần hóa đơn Cloudflare.
- Upload từ Dashboard, script hoặc ứng dụng khác không qua lớp giữ chỗ SQB có thể làm tài khoản vượt ngưỡng giữa các lần đối soát. Dùng bộ credentials và đường ghi được kiểm soát để giới hạn phạm vi.
- R2 Standard miễn phí **10 GB-month/tháng**, không phải giới hạn 10 GB cho một file. Dung lượng tính theo trung bình đỉnh lưu trữ mỗi ngày; xóa object không hoàn tác usage đã phát sinh. Free tier không áp dụng cho Infrequent Access.

Nguồn: [R2 pricing](https://developers.cloudflare.com/r2/pricing/), [R2 presigned URLs](https://developers.cloudflare.com/r2/api/s3/presigned-urls/), [R2 limits](https://developers.cloudflare.com/r2/platform/limits/). Thiết kế SQB hiện có: ADR-062 trong `ARCHITECTURE.md`.
