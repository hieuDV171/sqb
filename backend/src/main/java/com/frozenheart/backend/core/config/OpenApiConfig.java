package com.frozenheart.backend.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cấu hình OpenAPI 3.0 (Swagger) cho toàn bộ hệ thống SQB.
 * Hỗ trợ xác thực tập trung Bearer JWT Token cho tất cả 129+ REST Endpoints.
 * Tùy biến SwaggerIndexTransformer để tiêm CSS tùy chỉnh cho Swagger UI:
 * - Tách rời dứt khoát 2 nút Authorize và Close trong Modal Authorization
 * - Tối ưu khoảng cách typography, bảng biểu và inline code badges tránh chữ bị dính sát nhau.
 */
@Slf4j
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

                - **Endpoint kết nối Handshake:** `ws://<domain>/api/v1/ws` (Client có thể dùng `@stomp/stompjs` hoặc `sockjs-client`).
                - **Header xác thực khi CONNECT:** `Authorization: Bearer <jwt_access_token>`.
                - **Tiền tố Application (Client -> Server):** `/app`
                - **Tiền tố Broker (Server -> Client):** `/user` (kênh riêng tư) và `/topic` (kênh nhóm/cộng đồng)

                #### 1. Kênh Client gửi lên Server (Publish Destinations):

                | STOMP Destination | Mô tả nghiệp vụ | Payload DTO & Các trường chính |
                | :--- | :--- | :--- |
                | `/app/chat.send` | Gửi tin nhắn mới trong hội thoại | `SendWsMessageRequestDto`<br>• `conversationId` (Long)<br>• `content` (String)<br>• `messageType` (TEXT / IMAGE / FILE)<br>• `mediaUrls` (List&lt;String&gt;)<br>• `replyToMessageId` (Long) |
                | `/app/chat.read` | Báo đã đọc tin nhắn trong cuộc hội thoại | `ReadWsMessageRequestDto`<br>• `conversationId` (Long)<br>• `lastReadMessageId` (Long) |
                | `/app/chat.typing` | Phát sự kiện đang soạn / dừng soạn tin nhắn | `TypingWsRequestDto`<br>• `conversationId` (Long)<br>• `isTyping` (Boolean) |

                #### 2. Kênh Client đăng ký lắng nghe (Subscribe Topics / Queues):

                | STOMP Topic / Queue | Mô tả sự kiện | Payload DTO nhận được |
                | :--- | :--- | :--- |
                | `/user/queue/messages` | Lắng nghe tin nhắn cá nhân 1-1, thông báo hệ thống và biên nhận đã đọc | `WsMessageBroadcastDto` / `WsReadReceiptBroadcastDto` |
                | `/topic/conversations.{conversationId}` | Lắng nghe tin nhắn trao đổi trong nhóm và sự kiện thành viên đang soạn tin | `WsMessageBroadcastDto` / `WsTypingBroadcastDto` |
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

    /**
     * Tùy biến SwaggerIndexTransformer để tự động tiêm custom CSS vào Swagger UI.
     * Giải quyết triệt để vấn đề:
     * - authorizer-btn và close-btn dính sát vào nhau
     * - inline badges / text bị chật chội
     * Phương thức hoạt động đa tầng (3-layer guarantee):
     * 1. Tiêm động qua JavaScript vào swagger-initializer.js
     * 2. Tiêm thẻ <style> vào <head> của index.html
     * 3. Ghép trực tiếp vào swagger-ui.css
     */
    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider) {

        String customCss = loadCustomCss();

        return new SwaggerIndexPageTransformer(swaggerUiConfigProperties, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider) {
            @Override
            public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
                Resource transformed = super.transform(request, resource, transformerChain);
                if (customCss.isBlank()) {
                    return transformed;
                }

                String filename = resource.getFilename();
                String uri = request != null && request.getRequestURI() != null ? request.getRequestURI() : "";

                // 1. Tiêm CSS động vào swagger-initializer.js
                if ((filename != null && filename.endsWith("swagger-initializer.js")) || uri.endsWith("swagger-initializer.js")) {
                    byte[] bytes = transformed.getInputStream().readAllBytes();
                    String js = new String(bytes, StandardCharsets.UTF_8);
                    String injectedJs = "\n/* SQB Custom Swagger CSS Injection */\n"
                            + "(function() {\n"
                            + "  try {\n"
                            + "    var style = document.createElement('style');\n"
                            + "    style.id = 'sqb-swagger-custom-style';\n"
                            + "    style.type = 'text/css';\n"
                            + "    style.innerHTML = " + escapeJsonString(customCss) + ";\n"
                            + "    document.head.appendChild(style);\n"
                            + "  } catch (e) { console.error('Failed to inject SQB Swagger styles', e);\n }\n"
                            + "})();\n";
                    return new TransformedResource(transformed, (js + injectedJs).getBytes(StandardCharsets.UTF_8));
                }

                // 2. Tiêm thẻ <style> trực tiếp vào index.html trước </head>
                if ((filename != null && filename.endsWith("index.html")) || uri.endsWith("index.html")) {
                    byte[] bytes = transformed.getInputStream().readAllBytes();
                    String html = new String(bytes, StandardCharsets.UTF_8);
                    if (html.contains("</head>")) {
                        String styleTag = "\n<style id=\"sqb-swagger-custom-style-inline\">\n" + customCss + "\n</style>\n</head>";
                        html = html.replace("</head>", styleTag);
                        return new TransformedResource(transformed, html.getBytes(StandardCharsets.UTF_8));
                    }
                }

                // 3. Nối custom CSS trực tiếp vào cuối file swagger-ui.css
                if ((filename != null && filename.endsWith("swagger-ui.css")) || uri.endsWith("swagger-ui.css")) {
                    byte[] bytes = transformed.getInputStream().readAllBytes();
                    String css = new String(bytes, StandardCharsets.UTF_8);
                    css = css + "\n\n/* SQB Custom Overrides */\n" + customCss;
                    return new TransformedResource(transformed, css.getBytes(StandardCharsets.UTF_8));
                }

                return transformed;
            }
        };
    }

    private static String loadCustomCss() {
        try (InputStream is = OpenApiConfig.class.getResourceAsStream("/static/swagger-ui/custom.css")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Could not load /static/swagger-ui/custom.css for Swagger UI", e);
        }
        return "";
    }

    private static String escapeJsonString(String input) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
