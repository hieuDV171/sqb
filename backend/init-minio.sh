#!/bin/sh
# Sau này lên Production phải tạo rule tự hết hạn sau 1 ngày cho MinIO
# Hoặc dùng các dịch vụ lưu trữ cloud thì có thể cấu hình trên console
# Thêm alias và tự động tạo rule
mc alias set prodminio "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
mc mb --ignore-existing prodminio/sqb
mc ilm add prodminio/sqb --tags "status=temp" --expire-days 1