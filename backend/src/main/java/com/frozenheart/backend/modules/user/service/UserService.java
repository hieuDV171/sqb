package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.modules.user.dto.ProfileResponse;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;

public interface UserService {

    ProfileResponse getMyProfile();

    ProfileResponse getUserProfile(Long userId);

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    void deleteMyProfile();

}
