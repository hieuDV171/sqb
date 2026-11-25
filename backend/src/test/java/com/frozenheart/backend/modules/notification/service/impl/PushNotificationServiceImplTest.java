package com.frozenheart.backend.modules.notification.service.impl;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent.Delivery;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserDevice;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushNotificationServiceImplTest {

    @Test
    void includesExactNotificationInTargetUrlForOlderWorkers() throws Exception {
        var devices = mock(UserDeviceRepository.class);
        var firebase = mock(FirebaseMessaging.class);
        var service = new PushNotificationServiceImpl(devices, mock(UserPushSettingRepository.class),
                mock(UserProfileRepository.class), Optional.of(firebase));
        when(devices.findActiveDevicesByUserIds(List.of(1L))).thenReturn(List.of(UserDevice.builder()
                .user(User.builder().id(1L).build()).fid("test-fid").isActive(true).build()));
        when(firebase.sendEachForMulticast(any())).thenReturn(mock(BatchResponse.class));

        service.sendPushBatch(List.of(
                new Delivery(9007199254740993L, 1L, "Đã duyệt", "Chi tiết phiên", null,
                        NotificationTargetType.SESSION, 500L, "/sessions/500?tab=review&pushNotificationId=9#summary"),
                new Delivery(102L, 1L, "Thông báo mới", "Nội dung", null, null, null, null)),
                PushNotificationType.ACADEMIC);

        var sent = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase, times(2)).sendEachForMulticast(sent.capture());
        assertEquals(Map.of(
                "notificationId", "9007199254740993", "targetType", "SESSION", "targetId", "500",
                "targetUrl", "/sessions/500?tab=review&pushNotificationId=9007199254740993#summary",
                "pushType", "ACADEMIC"), ReflectionTestUtils.getField(sent.getAllValues().getFirst(), "data"));
        assertEquals(Map.of("notificationId", "102", "targetUrl", "/notifications?pushNotificationId=102",
                "pushType", "ACADEMIC"), ReflectionTestUtils.getField(sent.getAllValues().getLast(), "data"));
    }
}
