package com.frozenheart.backend.modules.friendship.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendListResponseDto {
    private List<FriendDto> items;
    private CursorPaginationDto pagination;

    private int totalFriends;
}
