package com.frozenheart.backend.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.frozenheart.backend.core.security.AuthCompatibilityPolicy;
import io.swagger.v3.core.jackson.ModelResolver;
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

import org.jspecify.annotations.NonNull;
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

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private final AuthCompatibilityPolicy authCompatibilityPolicy;

    public OpenApiConfig(AuthCompatibilityPolicy authCompatibilityPolicy) {
        this.authCompatibilityPolicy = authCompatibilityPolicy;
    }

    /**
     * Cấu hình ModelResolver của OpenAPI sinh schema chuẩn snake_case.
     * Tự khởi tạo ObjectMapper của Jackson 2 (com.fasterxml.jackson) cho Swagger-Core,
     * bảo đảm tương thích 100%, độc lập và không xung đột với JsonMapper (tools.jackson) của Jackson 3 trong Spring Boot 4.
     */
    @Bean
    public ModelResolver modelResolver() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new ModelResolver(objectMapper);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String description = """
                Tài liệu đặc tả và kiểm thử tương tác REST API & Giao thức WebSocket STOMP của nền tảng SQB (Social Network Student-Generated Question Bank).

                > ⚠️ **LƯU Ý CHO NHÓM CLIENT SINH VIÊN:** Không gọi các API dành cho admin. Browser chỉ được gọi trực tiếp từ origin trong allowlist của backend: các domain SQB/admin đã cấu hình và localhost theo cổng bên dưới. HTTP client native thông thường không chịu CORS của browser; vẫn phải xác thực và có quyền phù hợp.

                ### ✅ Client Web chạy localhost và client native bắt đầu ở đây

                - **Client chạy local:** cấu hình SQB cho phép HTTP `localhost` và `127.0.0.1` ở các cổng **3000, 3001, 5173, 5174**.
                  Nếu banner đầu Swagger báo tương thích **ĐANG BẬT**, client cũ có thể gọi trực tiếp `/auth/login` với platform non-WEB,
                  nhận AT/RT JSON và refresh qua body tại `/auth/refresh`, không phụ thuộc cookie khác site. Không gửi cookie cho nhánh JSON.
                - **Native Android/iOS/desktop:** HTTP client native thông thường không chịu CORS của browser; dùng `/auth/native/login`,
                  `/auth/native/refresh`, `/auth/native/logout`. Client cũ được dùng nhánh tương thích nêu trên trong khi cờ còn bật.
                  Flutter Web/JavaScript trong WebView vẫn có thể chịu quy tắc browser.
                - Sau khi nhận AT, API nghiệp vụ dùng chung `Authorization: Bearer <AT>`. AT hạn **15 phút**; refresh phải thay cả cặp token,
                  gom request đồng thời và retry nghiệp vụ tối đa một lần. Vẫn kiểm tra mật khẩu, trạng thái tài khoản, quyền và hạn mức.
                - Chỉ áp dụng khi server đã triển khai bản backend/Compose tương ứng. Domain Vercel/domain riêng, cổng local khác hoặc HTTPS local
                  cần đăng ký origin riêng; không mặc nhiên được cho phép. Upload trực tiếp R2 còn cần CORS bucket phù hợp.
                - WEB cookie của chính SQB vẫn Strict/HttpOnly; không dùng `platform=WEB` rồi kỳ vọng cookie tự hoạt động cross-site.

                ---

                ### 🔑 Hướng dẫn xác thực:
                1. Web/Flutter Web/PWA gọi `/auth/login`; Flutter native Android/iOS/desktop gọi `/auth/native/login` để lấy `access_token`.
                2. Nhấn nút **Authorize** ở góc phải trên cùng màn hình, dán token vào ô `Value` (không cần gõ thêm chữ `Bearer `).
                3. Nhấn **Authorize** để tự động gắn Header Authorization cho mọi API cần kiểm thử.

                ### 🌐 Client Web và client native dùng luồng nào?

                Các đường dẫn bên dưới có tiền tố `/api/v1`. Cả hai dùng chung tài khoản, phân quyền và API nghiệp vụ.

                | Client | Đăng nhập | Refresh | Logout | Nơi giữ refresh token |
                | :--- | :--- | :--- | :--- | :--- |
                | Web, trình duyệt điện thoại, PWA, Flutter Web | `/auth/login` với `platform=WEB` | `/auth/refresh`, không body, gửi cookie/credentials | `/auth/logout` với Bearer AT | Cookie HttpOnly, JSON RT null |
                | Flutter native Android/iOS, desktop | `/auth/native/login` với `ANDROID`, `IOS` hoặc `DESKTOP` | `/auth/native/refresh` với JSON RT | `/auth/native/logout` với Bearer AT | Secure storage trên thiết bị, JSON trả RT |

                ### ⚠️ Tương thích tạm thời cho client đã dùng contract cũ

                Chỉ khi banner đầu trang báo **ĐANG BẬT** (`AUTH_LEGACY_JSON_ENABLED=true`):
                - `/auth/login` còn nhận `platform=ANDROID/IOS/DESKTOP`, trả AT/RT JSON cho tài khoản ACTIVE; đây là lựa chọn transport, không chứng minh app native.
                - `/auth/refresh` vẫn ưu tiên cookie. Nếu không có cookie, body `{"refresh_token":"..."}` nhận RT LEGACY_JSON hoặc RT cũ thiếu claim kênh.
                - RT mới của nhánh này có kênh **LEGACY_JSON**. Không đổi RT đã ký WEB/NATIVE sang nhánh tương thích; không trộn cookie với body của tài khoản khác.
                - Login/refresh của nhánh JSON trả HTTP 200, `code=1000` và message **CẢNH BÁO**. Client xét code/status, không so message đúng bằng “Thành công”.
                - `/auth/logout` nhận AT LEGACY_JSON để thu hồi phiên, kể cả khi cờ đã tắt. Luồng chuẩn native mới vẫn dùng `/auth/native/*`.

                **Kế hoạch sau bảo vệ:** sẽ phát hành phiên bản backend loại bỏ toàn bộ cơ chế tương thích xác thực cũ,
                gồm luồng `LEGACY_JSON`, cờ/mã hỗ trợ và việc tiếp nhận RT thiếu claim kênh qua cookie lẫn body.
                Client dùng phiên cũ phải đăng nhập lại theo contract hiện hành. Luồng WEB cookie và `/auth/native/*` chuẩn tiếp tục được hỗ trợ.
                Khi đó, `/auth/login` chỉ nhận WEB; `/auth/refresh` chỉ nhận RT WEB qua cookie, không nhận RT body.
                Đây là kế hoạch cho bản phát hành sau bảo vệ, không tự thay đổi theo ngày. Hiện tại có thể chặn nhánh JSON bằng
                `AUTH_LEGACY_JSON_ENABLED=false`; thao tác này không xóa code/cờ hay fallback RT thiếu kênh qua cookie WEB.
                CORS vẫn cần origin phù hợp; chế độ này không tự cho phép mọi website. Giữ HTTPS, AT 15 phút và cookie WEB Strict/HttpOnly hiện hành.
                Client nhận RT JSON chịu trách nhiệm bảo vệ token; JavaScript có thể đọc token nếu XSS. Không log token/đưa token vào URL.

                **Ví dụ native login:**
                ```json
                {"email":"student@example.edu","password":"your-password","device_id":"a-unique-installation-uuid","platform":"ANDROID","device_name":"Flutter Android"}
                ```
                **Ví dụ native refresh:**
                ```json
                {"refresh_token":"the-latest-native-refresh-token"}
                ```
                Native thay cả AT và RT sau mỗi lần refresh thành công, chỉ chạy một refresh tại một thời điểm và không đưa token vào log/URL.
                Web giữ AT trong memory, dùng cookie để khôi phục sau reload; không lưu token vào localStorage/sessionStorage/IndexedDB.
                Token có kênh do server ký: RT WEB không đổi sang body/native; RT NATIVE không đổi sang cookie/web.
                `platform` chỉ là metadata thiết bị, không chứng minh app chính chủ. Tách endpoint không làm tăng quyền tài khoản.
                Hiện tại RT cũ thiếu claim kênh vẫn được migrate qua cookie WEB; khi bật tương thích còn được migrate qua body ở endpoint cũ.
                Bản phát hành dọn tương thích sau bảo vệ sẽ từ chối RT thiếu claim kênh ở cả hai đường, yêu cầu đăng nhập lại.
                Nếu `account_state` chưa ACTIVE, chỉ nhận AT `ONBOARDING`, không RT: gọi API xác minh email/đổi mật khẩu rồi đăng nhập lại đúng luồng.
                ONBOARDING AT cũng chỉ ở memory trên web; reload giữa bước thiết lập yêu cầu đăng nhập lại.
                Luồng cookie WEB không chứa RT JSON; nhánh tương thích non-WEB là ngoại lệ tạm thời đã nêu trên. Swagger Authorize dùng **access_token**, không dùng refresh_token.

                ---

                ### 📦 Cấu trúc phản hồi chuẩn (GlobalResponse Envelope):
                Các API JSON dùng vỏ bọc thống nhất (snake_case); endpoint trả 204 hoặc dữ liệu khác sẽ ghi riêng trong response:
                ```json
                {
                  "code": "1000",
                  "message": "Thành công",
                  "data": null
                }
                ```
                `data` theo schema của từng endpoint; có thể là object, danh sách, trang cursor hoặc null.
                ID, URL và token trong ví dụ chỉ minh họa: dùng ID thực từ API và URL upload do backend cấp khi kiểm thử.
                Với lỗi validation, data có thể chứa danh sách field/message. Đọc cả HTTP status và code; message có thể thay đổi/có cảnh báo.

                ---

                ### 📡 Đặc tả giao thức Realtime WebSocket STOMP (Chat & Trực tuyến):

                - **Endpoint SockJS:** dùng base URL HTTP(S), ví dụ `new SockJS('https://<domain>/api/v1/ws')`, qua `webSocketFactory` của `@stomp/stompjs`. Không truyền URL `ws://` vào SockJS; khi chạy local HTTP dùng `http://<host>:<port>/api/v1/ws`.
                - **Header xác thực khi CONNECT:** `Authorization: Bearer <jwt_access_token>`.
                - **Tiền tố Application (Client -> Server):** `/app`
                - **Tiền tố Broker (Server -> Client):** `/user` (kênh riêng tư) và `/topic` (kênh nhóm/cộng đồng)

                #### 1. Kênh Client gửi lên Server (Publish Destinations):

                | STOMP Destination | Mô tả nghiệp vụ | Payload DTO & Các trường chính (snake_case) |
                | :--- | :--- | :--- |
                | `/app/chat.send` | Gửi tin nhắn mới trong hội thoại | `SendWsMessageRequestDto`<br>• `conversation_id` (Long)<br>• `content` (String)<br>• `message_type` (TEXT / IMAGE / VIDEO / GIF)<br>• `media_urls` (List&lt;String&gt;)<br>• `reply_to_message_id` (Long, tùy chọn) |
                | `/app/chat.read` | Báo đã đọc tin nhắn trong cuộc hội thoại | `ReadWsMessageRequestDto`<br>• `conversation_id` (Long)<br>• `message_id` (Long) |
                | `/app/chat.typing` | Phát sự kiện đang soạn / dừng soạn tin nhắn | `TypingWsRequestDto`<br>• `conversation_id` (Long)<br>• `typing` (Boolean) |

                Ví dụ body của frame STOMP **SEND** (không phải HTTP POST). CONNECT bằng AT hợp lệ, SUBSCRIBE kênh nhận trước khi gửi;
                dùng ID hội thoại/tin nhắn thực mà tài khoản có quyền truy cập. Người gửi được xác định từ JWT.

                `/app/chat.send`:
                ```json
                {"conversation_id":42,"content":"Cùng trao đổi bài học nhé","message_type":"TEXT","media_urls":[],"reply_to_message_id":null}
                ```
                `/app/chat.read`:
                ```json
                {"conversation_id":42,"message_id":1005}
                ```
                `/app/chat.typing`:
                ```json
                {"conversation_id":42,"typing":true}
                ```

                #### 2. Kênh Client đăng ký lắng nghe (Subscribe Topics / Queues):

                | STOMP Topic / Queue | Mô tả sự kiện | Payload DTO nhận được (snake_case) |
                | :--- | :--- | :--- |
                | `/user/queue/messages` | Nhận tin nhắn, thu hồi, biên nhận đã đọc, trạng thái đang soạn và cập nhật nhóm. Sự kiện từ hội thoại ẩn chỉ đến các kết nối thuộc phiên đã mở kho. | `WsMessageBroadcastDto` / `WsMessageRevokedDto` / `WsReadReceiptBroadcastDto` / `WsTypingBroadcastDto` / các DTO cập nhật nhóm |

                Với tin nhắn trả lời, `NEW_MESSAGE` chứa `reply_to_message` cùng cấu trúc với lịch sử REST, gồm ID tin gốc, người gửi và `content_preview`. Nếu người nhận đã xóa tin gốc ở phía mình, bản trích dẫn chỉ báo tin gốc không còn hiển thị.

                Sau khi thu hồi với mọi người được lưu thành công, `MESSAGE_REVOKED` gửi `conversation_id` và `message_id` tới thành viên còn tham gia. Xóa tin ở phía mình không tạo sự kiện này.

                `GROUP_ROLES_CHANGED` gửi `conversation_id` sau khi giao dịch đổi vai trò nhóm thành công. Client tải lại danh sách thành viên của nhóm.

                `GROUP_MEMBER_REMOVED` gửi `conversation_id` và `removed_user_id` tới thành viên đang tham gia và người bị mời ra sau khi giao dịch hoàn tất.

                `GROUP_MEMBERS_ADDED` gửi `conversation_id` tới thành viên đang tham gia sau khi thêm trực tiếp hoặc sau khi người được mời chấp nhận vào nhóm.

                `GROUP_DETAILS_CHANGED` gửi `conversation_id` sau khi nhóm đổi tên hoặc có người rời nhóm. Khi rời nhóm, sự kiện có thêm `left_user_id` và được gửi cả tới người vừa rời.

                Client tải lại lịch sử qua REST khi nhận cập nhật nhóm. Lịch sử trả các dòng hệ thống với `message_type: SYSTEM` xen giữa các tin nhắn.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Hệ Thống Mạng Xã Hội Học Tập & Ngân Hàng Đề Thi - SQB")
                        .description(authCompatibilityPolicy.swaggerNotice() + description)
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
            public @NonNull Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
                Resource transformed = super.transform(request, resource, transformerChain);
                if (customCss.isBlank()) {
                    return transformed;
                }

                String filename = resource.getFilename();
                String uri = request.getRequestURI() != null ? request.getRequestURI() : "";

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
