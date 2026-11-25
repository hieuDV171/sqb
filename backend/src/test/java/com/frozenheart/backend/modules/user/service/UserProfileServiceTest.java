package com.frozenheart.backend.modules.user.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.user.Gender;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.socialgraph.follow.repository.FollowRepository;
import com.frozenheart.backend.modules.socialgraph.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {
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

    @ParameterizedTest
    @CsvSource({"Asia/Ho_Chi_Minh, Asia/Ho_Chi_Minh", "Asia/Tokyo, Asia/Tokyo"})
    void savesSupportedTimezoneId(String input, String expected) {
        var profile = profileWithTimezone("UTC");
        signIn(profile.getUserId());

        var result = service.updateMyProfile(UpdateProfileRequest.builder().timezone(" " + input + " ").build());

        assertThat(result.timezone()).isEqualTo(expected);
        assertThat(profile.getTimezone()).isEqualTo(expected);
    }

    @Test
    void rejectsLegacyTimezoneIdInProfileUpdate() {
        var profile = profileWithTimezone("UTC");
        signIn(profile.getUserId());

        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> service.updateMyProfile(UpdateProfileRequest.builder().timezone(" Asia/Saigon ").build()))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.INVALID_PARAMETER_VALUE));
        assertThat(profile.getTimezone()).isEqualTo("UTC");
    }

    @Test
    void returnsCanonicalTimezoneBeforeSavingLegacyProfile() {
        var profile = profileWithTimezone("Asia/Saigon");
        signIn(2L);
        assertThat(service.getUserProfile(profile.getUserId()).timezone()).isEqualTo(Time.DEFAULT_TIMEZONE);

        signIn(profile.getUserId());
        assertThat(service.getMyProfile().timezone()).isEqualTo(Time.DEFAULT_TIMEZONE);
        assertThat(profile.getTimezone()).isEqualTo("Asia/Saigon");

        service.updateMyProfile(UpdateProfileRequest.builder().bio("Đã cập nhật").build());

        assertThat(profile.getTimezone()).isEqualTo(Time.DEFAULT_TIMEZONE);
        verify(profiles).save(profile);
    }

    private UserProfile profileWithTimezone(String timezone) {
        var owner = User.builder().id(1L).active(true).build();
        var profile = UserProfile.builder().userId(owner.getId()).user(owner).timezone(timezone).build();
        when(profiles.findByUserIdWithUser(owner.getId())).thenReturn(Optional.of(profile));
        return profile;
    }

    private static void signIn(long userId) {
        var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(userId).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
