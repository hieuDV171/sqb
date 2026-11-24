package com.frozenheart.backend.modules.socialgraph.block.service;

import com.frozenheart.backend.modules.socialgraph.block.dto.*;

public interface BlockService {

    BlockResponseDto blockUser(Long targetUserId);

    UnblockResponseDto unblockUser(Long targetUserId);

    BlockedListResponseDto getBlockedUsers(Long after, String cursor, Integer limit);
}
