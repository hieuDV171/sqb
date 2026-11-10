package com.frozenheart.backend.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import com.frozenheart.backend.modules.conversation.dto.LastMessageDto;
import com.frozenheart.backend.modules.conversation.dto.MessageDto;
import com.frozenheart.backend.modules.conversation.dto.ws.SendWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.TypingWsRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.WsTypingBroadcastDto;
import com.frozenheart.backend.modules.follow.dto.FollowResponseDto;
import com.frozenheart.backend.modules.follow.dto.UnfollowResponseDto;
import com.frozenheart.backend.modules.user.dto.Relationship;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiSnakeCaseTest {

    @Test
    void chatTypingAndRevokedFlagsUseTheFrontendWireNames() {
        var mapper = JsonMapper.builder()
                .propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
                .build();
        var converters = new ArrayList<MessageConverter>();
        new WebSocketConfig(null, null, mapper).configureMessageConverters(converters);
        var stomp = new CompositeMessageConverter(converters);
        var send = assertInstanceOf(SendWsMessageRequestDto.class, stomp.fromMessage(
                MessageBuilder.withPayload("""
                        {"conversation_id":10,"reply_to_message_id":25}
                        """.getBytes(StandardCharsets.UTF_8)).build(), SendWsMessageRequestDto.class));
        assertEquals(10L, send.getConversationId());
        assertEquals(25L, send.getReplyToMessageId());
        for (boolean typing : new boolean[] {true, false}) {
            var frame = MessageBuilder.withPayload("""
                    {"conversation_id":10,"typing":%b}
                    """.formatted(typing).getBytes(StandardCharsets.UTF_8)).build();
            var request = assertInstanceOf(TypingWsRequestDto.class, stomp.fromMessage(frame, TypingWsRequestDto.class));
            assertEquals(10L, request.conversationId());
            assertEquals(typing, request.typing());
            var outgoing = stomp.toMessage(new WsTypingBroadcastDto(
                    ChatWsEventType.TYPING_STATUS, request.conversationId(), null, request.typing(), null), null);
            assertNotNull(outgoing);
            var event = mapper.readTree(assertInstanceOf(byte[].class, outgoing.getPayload()));
            assertEquals(10L, event.get("conversation_id").asLong());
            assertEquals(typing, event.get("typing").asBoolean());
            assertFalse(event.has("is_typing"));
            var message = mapper.valueToTree(MessageDto.builder().revoked(typing).build());
            var lastMessage = mapper.valueToTree(LastMessageDto.builder().revoked(typing).build());
            assertEquals(typing, message.get("revoked").asBoolean());
            assertEquals(typing, lastMessage.get("revoked").asBoolean());
            assertFalse(message.has("is_revoked"));
            assertFalse(lastMessage.has("is_revoked"));
        }
    }

    @Test
    void profileAndFollowPayloadsKeepTheirExistingWireNames() {
        var mapper = JsonMapper.builder()
                .propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
                .build();
        var relationship = mapper.valueToTree(Relationship.builder().isFollowed(true).build());
        assertTrue(relationship.get("is_followed").asBoolean());
        assertFalse(relationship.get("is_following").asBoolean());
        var follow = mapper.valueToTree(FollowResponseDto.builder().isFollowing(true).build());
        var unfollow = mapper.valueToTree(UnfollowResponseDto.builder().isFollowing(false).build());
        assertTrue(follow.get("following").asBoolean());
        assertFalse(unfollow.get("following").asBoolean());
    }

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
