package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.Gender;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.socialgraph.follow.repository.FollowRepository;
import com.frozenheart.backend.modules.socialgraph.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileGenderTest {
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final UserService service = new UserServiceImpl(profiles, mock(UserGamificationRepository.class),
            mock(FollowRepository.class), mock(FriendshipRepository.class), mock(UserRepository.class),
            mock(MediaService.class), mock(ApplicationEventPublisher.class));

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(Gender.class)
    void savesGenderAndRespectsProfileVisibility(Gender gender) {
        var owner = User.builder().id(1L).email("student@example.edu").active(true).build();
        var profile = UserProfile.builder().userId(owner.getId()).user(owner).build();
        when(profiles.findByUserIdWithUser(owner.getId())).thenReturn(Optional.of(profile));

        signIn(owner.getId());
        assertThat(service.updateMyProfile(UpdateProfileRequest.builder().gender(gender).build()).gender())
                .isEqualTo(gender);
        verify(profiles).save(profile);
        assertThat(service.getMyProfile().gender()).isEqualTo(gender);

        signIn(2L);
        assertThat(service.getUserProfile(owner.getId()).gender())
                .isEqualTo(gender == Gender.PREFER_NOT_TO_SAY ? null : gender);
    }

    private static void signIn(long userId) {
        var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(userId).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
