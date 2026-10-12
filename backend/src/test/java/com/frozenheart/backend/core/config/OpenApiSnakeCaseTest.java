package com.frozenheart.backend.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiSnakeCaseTest {

    @Test
    @DisplayName("Kiểm tra ModelResolver sinh schema Swagger chuẩn snake_case đồng bộ với Jackson")
    void testSwaggerSchemaGeneratesSnakeCase() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        ModelResolver modelResolver = new ModelResolver(objectMapper);
        ModelConverterContextImpl context = new ModelConverterContextImpl(modelResolver);

        Schema<?> loginSchema = context.resolve(new AnnotatedType(LoginRequest.class));
        assertNotNull(loginSchema);
        assertNotNull(loginSchema.getProperties());
        assertTrue(loginSchema.getProperties().containsKey("device_id"),
                "Swagger Schema phải chứa thuộc tính snake_case 'device_id'");
        assertFalse(loginSchema.getProperties().containsKey("deviceId"),
                "Swagger Schema không được chứa thuộc tính camelCase 'deviceId'");
        assertTrue(loginSchema.getProperties().containsKey("device_name"),
                "Swagger Schema phải chứa thuộc tính snake_case 'device_name'");
        assertFalse(loginSchema.getProperties().containsKey("deviceName"),
                "Swagger Schema không được chứa thuộc tính camelCase 'deviceName'");
        assertTrue(loginSchema.getProperties().containsKey("os_version"),
                "Swagger Schema phải chứa thuộc tính snake_case 'os_version'");
        assertFalse(loginSchema.getProperties().containsKey("osVersion"),
                "Swagger Schema không được chứa thuộc tính camelCase 'osVersion'");
        assertTrue(loginSchema.getProperties().containsKey("app_version"),
                "Swagger Schema phải chứa thuộc tính snake_case 'app_version'");
        assertFalse(loginSchema.getProperties().containsKey("appVersion"),
                "Swagger Schema không được chứa thuộc tính camelCase 'appVersion'");

        Schema<?> profileSchema = context.resolve(new AnnotatedType(UpdateProfileRequest.class));
        assertNotNull(profileSchema);
        assertNotNull(profileSchema.getProperties());
        assertTrue(profileSchema.getProperties().containsKey("full_name"),
                "Swagger Schema phải chứa thuộc tính snake_case 'full_name'");
        assertFalse(profileSchema.getProperties().containsKey("fullName"),
                "Swagger Schema không được chứa thuộc tính camelCase 'fullName'");
        assertTrue(profileSchema.getProperties().containsKey("avatar_url"),
                "Swagger Schema phải chứa thuộc tính snake_case 'avatar_url'");
        assertFalse(profileSchema.getProperties().containsKey("avatarUrl"),
                "Swagger Schema không được chứa thuộc tính camelCase 'avatarUrl'");
        assertTrue(profileSchema.getProperties().containsKey("date_of_birth"),
                "Swagger Schema phải chứa thuộc tính snake_case 'date_of_birth'");
        assertFalse(profileSchema.getProperties().containsKey("dateOfBirth"),
                "Swagger Schema không được chứa thuộc tính camelCase 'dateOfBirth'");
    }
}
