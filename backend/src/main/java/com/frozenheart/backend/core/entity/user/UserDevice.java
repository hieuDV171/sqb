package com.frozenheart.backend.core.entity.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(name = "uc_user_devices_user_id_device_id", columnNames = { "user_id", "device_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 500)
    private String deviceId;

    @Column(length = 500)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private DevicePlatform platform;

    private String osVersion;

    private String screenSize; // XXXX*YYYY
    private String deviceName;
    private String appVersion;
    private boolean isActive;
    private Instant lastActiveAt;

    @Column(updatable = false)
    private Instant createdAt;

    // -----------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // -----------------------------

}
