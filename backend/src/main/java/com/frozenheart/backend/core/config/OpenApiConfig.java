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
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ Thống Mạng Xã Hội Học Tập & Ngân Hàng Đề Thi - SQB HUST")
                        .description("Tài liệu đặc tả và kiểm thử tương tác toàn bộ 129+ REST API của nền tảng SQB (Social Network Student-Generated Question Bank).\n\n"
                                + "### 🔑 Hướng dẫn xác thực:\n"
                                + "1. Gọi API `/auth/login` để lấy JWT `access_token`.\n"
                                + "2. Nhấn nút **Authorize** màu xanh ở góc phải trên cùng màn hình, dán token vào ô `Value` (không cần gõ thêm chữ `Bearer `).\n"
                                + "3. Nhấn **Authorize** để tự động gắn Header Authorization cho mọi API cần kiểm thử.")
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
