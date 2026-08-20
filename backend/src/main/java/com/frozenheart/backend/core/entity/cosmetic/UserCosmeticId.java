package com.frozenheart.backend.core.entity.cosmetic;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserCosmeticId implements Serializable {

    private Long userId;

    private Long cosmeticId;

}
