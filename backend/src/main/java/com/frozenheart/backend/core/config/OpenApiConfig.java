package com.frozenheart.backend.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình OpenAPI 3.0 (Swagger) cho toàn bộ hệ thống SQB.
 * Hỗ trợ xác thực tập trung Bearer JWT Token cho tất cả 129+ REST Endpoints.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        String description = """
                Tài liệu đặc tả và kiểm thử tương tác toàn bộ 143+ REST API & Giao thức WebSocket STOMP của nền tảng SQB (Social Network Student-Generated Question Bank).

                ---

                ### 🔑 Hướng dẫn xác thực:
                1. Gọi API `/auth/login` để lấy JWT `access_token`.
                2. Nhấn nút **Authorize** ở góc phải trên cùng màn hình, dán token vào ô `Value` (không cần gõ thêm chữ `Bearer `).
                3. Nhấn **Authorize** để tự động gắn Header Authorization cho mọi API cần kiểm thử.

                ---

                ### 📦 Cấu trúc phản hồi chuẩn (GlobalResponse Envelope):
                Tất cả các REST API trong hệ thống đều trả về theo định dạng vỏ bọc thống nhất:
                ```json
                {
                  "code": "0000",
                  "message": "Thành công",
                  "data": { ... Payload nghiệp vụ ... }
                }
                ```

                ---

                ### 📡 Đặc tả giao thức Realtime WebSocket STOMP (Chat & Trực tuyến):
                - **Endpoint kết nối Handshake:** `ws://<domain>/api/v1/ws` (Client có thể dùng thư viện `@stomp/stompjs` hoặc `sockjs-client`).
                - **Header xác thực khi CONNECT:** `Authorization: Bearer <jwt_access_token>`.
                - **Tiền tố Application (Client -> Server):** `/app`
                - **Tiền tố Broker (Server -> Client):** `/user` (kênh riêng tư) và `/topic` (kênh nhóm/cộng đồng)

                #### 1. Kênh Client gửi lên Server (Publish):
                - **`/app/chat.send`**: Gửi tin nhắn mới trong hội thoại.
                  - *Payload (`SendWsMessageRequestDto`):* `conversationId` (Long), `content` (String), `messageType` (TEXT/IMAGE/...), `mediaUrls` (List), `replyToMessageId` (Long).
                - **`/app/chat.read`**: Báo đã đọc tin nhắn trong cuộc hội thoại.
                  - *Payload (`ReadWsMessageRequestDto`):* `conversationId` (Long), `lastReadMessageId` (Long).
                - **`/app/chat.typing`**: Phát sự kiện đang gõ / dừng gõ tin nhắn.
                  - *Payload (`TypingWsRequestDto`):* `conversationId` (Long), `isTyping` (Boolean).

                #### 2. Kênh Client đăng ký lắng nghe (Subscribe):
                - **`/user/queue/messages`**: Lắng nghe tin nhắn cá nhân 1-1, thông báo hệ thống và biên nhận đã đọc (`WsMessageBroadcastDto` / `WsReadReceiptBroadcastDto`).
                - **`/topic/conversations.{conversationId}`**: Lắng nghe tin nhắn nhóm và sự kiện thành viên đang soạn tin (`WsMessageBroadcastDto` / `WsTypingBroadcastDto`).
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Hệ Thống Mạng Xã Hội Học Tập & Ngân Hàng Đề Thi - SQB HUST")
                        .description(description)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Đội ngũ phát triển SQB HUST")
                                .email("bangiaohuongmuadong@gmail.com"))
                        .license(new License()
                                .name("Bản quyền thuộc về Đồng Văn Hiếu và thầy Nguyễn Tiến Thành")
                                .url("https://github.com/hieuDV171")))
                .servers(List.of(
                        new Server().url("/api/v1").description("API Gateway Base URL (/api/v1)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập chuỗi JWT Access Token nhận được sau khi đăng nhập thành công.")));
    }
}
