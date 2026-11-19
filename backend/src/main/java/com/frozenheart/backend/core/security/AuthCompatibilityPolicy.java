package com.frozenheart.backend.core.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthCompatibilityPolicy {
    public static final String WARNING_MESSAGE = "Thành công. CẢNH BÁO: RT JSON tại các endpoint auth cũ chỉ hỗ trợ tạm thời và sẽ bị loại bỏ trong bản phát hành sau giai đoạn bảo vệ. Client dùng phiên cũ phải đăng nhập lại theo contract hiện hành; xem hướng dẫn đầu Swagger.";

    private final boolean legacyJsonEnabled;

    public AuthCompatibilityPolicy(@Value("${app.auth.legacy-json-enabled:false}") boolean legacyJsonEnabled) {
        this.legacyJsonEnabled = legacyJsonEnabled;
    }

    public boolean isLegacyJsonEnabled() {
        return legacyJsonEnabled;
    }

    public String swaggerNotice() {
        return legacyJsonEnabled
                ? "> ⚠️ **CHẾ ĐỘ TƯƠNG THÍCH RT JSON TẠM THỜI: ĐANG BẬT.** `/auth/login` nhận platform non-WEB và `/auth/refresh` nhận RT body khi không có cookie. Login/refresh qua luồng này vẫn thành công (code 1000) nhưng message có cảnh báo. Có thể chặn nhánh JSON bằng `AUTH_LEGACY_JSON_ENABLED=false`; tắt cờ không xóa mã tương thích hay việc nhận RT thiếu claim kênh qua cookie WEB. Sau bảo vệ sẽ phát hành bản loại bỏ toàn bộ tương thích xác thực cũ, như kế hoạch bên dưới.\n\n"
                : "> **CHẾ ĐỘ TƯƠNG THÍCH RT JSON TẠM THỜI: ĐÃ TẮT.** `/auth/login` chỉ WEB; `/auth/refresh` chỉ cookie. Native dùng `/auth/native/*`.\n\n";
    }

    @PostConstruct
    void reportCompatibilityMode() {
        if (legacyJsonEnabled) {
            log.warn("Temporary auth JSON compatibility is enabled. AUTH_LEGACY_JSON_ENABLED=false blocks the JSON branch only; after the defense period, release a backend version removing LEGACY_JSON, its flag/code and refresh-token fallbacks for a missing channel claim. Old sessions must log in again; standard WEB/NATIVE flows remain supported.");
        }
    }
}
