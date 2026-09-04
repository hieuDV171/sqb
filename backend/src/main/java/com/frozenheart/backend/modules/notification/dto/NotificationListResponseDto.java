package com.frozenheart.backend.modules.notification.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDto {

    private List<NotificationDto> items;

    private CursorPaginationDto pagination;

    private int unreadCount;
}
