package com.frozenheart.backend.modules.block.service;

import com.frozenheart.backend.modules.block.dto.*;

public interface BlockService {

    BlockResponseDto blockUser(Long targetUserId);

    UnblockResponseDto unblockUser(Long targetUserId);

    BlockedListResponseDto getBlockedUsers(Long after, Integer limit);
}
