package com.frozenheart.backend.core.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.frozenheart.backend.core.entity.user.User;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    // Phải cộng thêm tiền tố ROLE_, bởi vì Security quản lý role theo cách này
    // Nếu không thêm, sử dụng hasRole() sẽ luôn cho kết quả sai, vì trong CSDL chỉ
    // lưu role dạng không có tiền tố ROLE_
    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public @NonNull String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive() && user.isVerified();
    }
}
