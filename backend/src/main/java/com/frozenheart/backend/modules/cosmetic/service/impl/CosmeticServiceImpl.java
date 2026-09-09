package com.frozenheart.backend.modules.cosmetic.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent.EntityType;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.cosmetic.*;
import com.frozenheart.backend.core.entity.user.CoinTransactionTargetType;
import com.frozenheart.backend.core.entity.user.CoinTransactionType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.cosmetic.constant.CosmeticSortBy;
import com.frozenheart.backend.modules.cosmetic.dto.*;
import com.frozenheart.backend.modules.cosmetic.repository.CosmeticItemRepository;
import com.frozenheart.backend.modules.cosmetic.repository.UserCosmeticRepository;
import com.frozenheart.backend.modules.cosmetic.service.CosmeticService;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.UserCurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosmeticServiceImpl implements CosmeticService {

    private final CosmeticItemRepository cosmeticItemRepository;
    private final UserCosmeticRepository userCosmeticRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserCurrencyService userCurrencyService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public MyInventoryResponseDto getMyInventory(CosmeticType type, CosmeticRarity rarity, boolean onlyUnlocked,
            Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        Long cursor = (after != null && after > 0) ? after : null;
        List<UserCosmetic> userCosmetics = userCosmeticRepository.findInventoryCursor(currentUserId, type, rarity,
                cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (userCosmetics.size() > pageSize) {
            hasNext = true;
            userCosmetics = userCosmetics.subList(0, pageSize);
            nextCursor = userCosmetics.getLast().getCosmetic().getId();
        }

        // 1. Tính toán Summary (Tổng mở khóa, theo Rarity, % hoàn thành)
        int totalUnlocked = userCosmeticRepository.countByIdUserId(currentUserId);
        Instant now = Instant.now();
        int totalActiveSystemItems = cosmeticItemRepository.countActiveItems(now);

        // 2 chữ số sau dấu phẩy
        double completionPercent = (totalActiveSystemItems > 0)
                ? Math.round(((double) totalUnlocked / totalActiveSystemItems) * 10000.0) / 100.0
                : 0.0;

        Map<String, Integer> byRarity = new LinkedHashMap<>();
        for (CosmeticRarity r : CosmeticRarity.values()) {
            byRarity.put(r.name(), 0);
        }
        List<Object[]> rarityCounts = userCosmeticRepository.countByUserIdGroupByRarity(currentUserId);
        for (Object[] row : rarityCounts) {
            CosmeticRarity r = (CosmeticRarity) row[0];
            Long count = (Long) row[1];
            if (r != null) {
                byRarity.put(r.name(), count.intValue());
            }
        }

        CosmeticSummaryDto summary = CosmeticSummaryDto.builder()
                .totalUnlocked(totalUnlocked)
                .byRarity(byRarity)
                .collectionCompletionPercent(completionPercent)
                .build();

        // 2. Lấy danh sách đang trang bị
        List<UserCosmetic> equippedCosmetics = userCosmeticRepository.findEquippedCosmetics(currentUserId);
        Map<String, EquippedItemDto> currentlyEquipped = new LinkedHashMap<>();
        for (UserCosmetic uc : equippedCosmetics) {
            CosmeticItem c = uc.getCosmetic();
            currentlyEquipped.put(c.getType().name(), EquippedItemDto.builder()
                    .cosmeticId(c.getId())
                    .name(c.getName())
                    .assetUrl(c.getAssetUrl())
                    .build());
        }

        // 3. Mapping danh sách inventory
        List<InventoryItemDto> inventory = userCosmetics.stream().map(uc -> {
            CosmeticItem c = uc.getCosmetic();
            return InventoryItemDto.builder()
                    .cosmeticId(c.getId())
                    .name(c.getName())
                    .description(c.getDescription())
                    .type(c.getType())
                    .rarity(c.getRarity())
                    .assetUrl(c.getAssetUrl())
                    .unlockedAt(uc.getAcquireAt())
                    .acquireMethod(uc.getAcquireMethod())
                    .availableUntil(c.getAvailableUtil())
                    .build();
        }).collect(Collectors.toList());

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return MyInventoryResponseDto.builder()
                .summary(summary)
                .currentlyEquipped(currentlyEquipped)
                .inventory(inventory)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CosmeticShopResponseDto getShop(CosmeticType type, CosmeticSortBy sortBy, Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        // Lấy số dư tiền tệ qua UserCurrencyService
        double userPoints = userCurrencyService.getBalance(currentUserId);

        // Lấy danh sách ID các item đã sở hữu để đánh dấu isOwned
        List<UserCosmetic> ownedItems = userCosmeticRepository.findInventoryCursor(currentUserId, null, null, null,
                PageRequest.of(0, 500));
        Set<Long> ownedIds = ownedItems.stream().map(uc -> uc.getCosmetic().getId()).collect(Collectors.toSet());

        Instant now = Instant.now();
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<CosmeticItem> shopItems;
        CosmeticSortBy sort = (sortBy != null) ? sortBy : CosmeticSortBy.NEWEST;
        Long cursor = (after != null && after > 0) ? after : null;

        switch (sort) {
            case PRICE_ASC -> shopItems = cosmeticItemRepository.findShopItemsPriceAsc(type, now, pageable);
            case PRICE_DESC -> shopItems = cosmeticItemRepository.findShopItemsPriceDesc(type, now, pageable);
            case RARITY -> shopItems = cosmeticItemRepository.findShopItemsRarity(type, now, pageable);
            default -> shopItems = cosmeticItemRepository.findShopItemsNewest(type, now, cursor, pageable);
        }

        boolean hasNext = false;
        Long nextCursor = null;

        if (shopItems.size() > pageSize) {
            hasNext = true;
            shopItems = shopItems.subList(0, pageSize);
            nextCursor = shopItems.getLast().getId();
        }

        List<ShopItemDto> items = shopItems.stream().map(c -> mapToShopItemDto(c, ownedIds))
                .collect(Collectors.toList());

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextCursor)
                .hasNext(hasNext)
                .build();

        return CosmeticShopResponseDto.builder()
                .userPoints(userPoints)
                .items(items)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public ShopItemDto buyCosmetic(Long cosmeticId) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        CosmeticItem cosmetic = cosmeticItemRepository.findById(cosmeticId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Vật phẩm không tồn tại"));

        Instant now = Instant.now();
        if (cosmetic.getAvailableUtil() != null && cosmetic.getAvailableUtil().isBefore(now)) {
            throw new AppException(ResponseCode.COSMETIC_EXPIRED, "Vật phẩm đã hết hạn mở bán");
        }

        if (userCosmeticRepository.existsByIdUserIdAndIdCosmeticId(currentUserId, cosmeticId)) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED, "Bạn đã sở hữu vật phẩm này rồi");
        }

        // Trừ tiền qua UserCurrencyService và lưu vết sổ cái CoinTransaction
        userCurrencyService.deduct(currentUserId, cosmetic.getPrice(),
                CoinTransactionType.BUY_COSMETIC,
                "Mua vật phẩm trang trí: " + cosmetic.getName(),
                CoinTransactionTargetType.COSMETIC_ITEM,
                cosmeticId);

        User user = userRepository.getReferenceById(currentUserId);
        UserCosmetic userCosmetic = UserCosmetic.builder()
                .id(new UserCosmeticId(currentUserId, cosmeticId))
                .user(user)
                .cosmetic(cosmetic)
                .acquireMethod(CosmeticAcquireMethod.SHOP_PURCHASE)
                .acquireAt(now)
                .build();

        userCosmeticRepository.save(userCosmetic);

        return mapToShopItemDto(cosmetic, Set.of(cosmeticId));
    }

    @Override
    @Transactional
    public EquipCosmeticResponseDto equipCosmetic(EquipCosmeticRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        UserCosmetic targetUc = userCosmeticRepository.findByUserIdAndCosmeticId(currentUserId, request.getCosmeticId())
                .orElseThrow(() -> new AppException(ResponseCode.COSMETIC_NOT_OWNED,
                        "Bạn chưa sở hữu vật phẩm này trong túi đồ"));

        CosmeticItem targetItem = targetUc.getCosmetic();
        CosmeticType type = targetItem.getType();

        // 1. Tháo vật phẩm cũ đang trang bị cùng loại (nếu có)
        Optional<UserCosmetic> currentEquippedOpt = userCosmeticRepository.findEquippedItemByType(currentUserId, type);
        Map<String, Object> previousItem = null;

        if (currentEquippedOpt.isPresent()) {
            UserCosmetic oldUc = currentEquippedOpt.get();
            if (!Objects.equals(oldUc.getCosmetic().getId(), targetItem.getId())) {
                oldUc.setEquippedAt(null);
                userCosmeticRepository.save(oldUc);
                previousItem = new LinkedHashMap<>();
                previousItem.put("cosmetic_id", oldUc.getCosmetic().getId());
                previousItem.put("name", oldUc.getCosmetic().getName());
            }
        }

        // 2. Trang bị vật phẩm mới
        Instant now = Instant.now();
        targetUc.setEquippedAt(now);
        userCosmeticRepository.save(targetUc);

        // 3. Đồng bộ hóa URL vào UserProfile
        UserProfile profile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        Map<String, String> profileUpdated = new LinkedHashMap<>();

        if (profile != null) {
            if (type == CosmeticType.AVATAR_FRAME) {
                profile.setAvatarFrameUrl(targetItem.getAssetUrl());
                profileUpdated.put("avatar_frame_url", targetItem.getAssetUrl());
            } else if (type == CosmeticType.CHAT_BUBBLE) {
                profile.setChatBubbleUrl(targetItem.getAssetUrl());
                profileUpdated.put("chat_bubble_url", targetItem.getAssetUrl());
            }
            userProfileRepository.save(profile);
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntityType.USER, currentUserId));
        }

        return EquipCosmeticResponseDto.builder()
                .cosmeticId(targetItem.getId())
                .name(targetItem.getName())
                .type(targetItem.getType())
                .assetUrl(targetItem.getAssetUrl())
                .equippedAt(now)
                .previousItem(previousItem)
                .profileUpdated(profileUpdated)
                .build();
    }

    @Override
    @Transactional
    public UnequipCosmeticResponseDto unequipCosmetic(UnequipCosmeticRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        UserCosmetic targetUc = userCosmeticRepository.findByUserIdAndCosmeticId(currentUserId, request.getCosmeticId())
                .orElseThrow(() -> new AppException(ResponseCode.COSMETIC_NOT_OWNED,
                        "Bạn chưa sở hữu vật phẩm này trong túi đồ"));

        if (targetUc.getEquippedAt() == null) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Vật phẩm này hiện không được trang bị");
        }

        CosmeticItem item = targetUc.getCosmetic();
        targetUc.setEquippedAt(null);
        userCosmeticRepository.save(targetUc);

        // Đồng bộ xóa URL khỏi UserProfile
        UserProfile profile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        Map<String, String> profileUpdated = new LinkedHashMap<>();

        if (profile != null) {
            if (item.getType() == CosmeticType.AVATAR_FRAME) {
                profile.setAvatarFrameUrl(null);
                profileUpdated.put("avatar_frame_url", null);
            } else if (item.getType() == CosmeticType.CHAT_BUBBLE) {
                profile.setChatBubbleUrl(null);
                profileUpdated.put("chat_bubble_url", null);
            }
            userProfileRepository.save(profile);
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntityType.USER, currentUserId));
        }

        Map<String, Object> unequippedItem = new LinkedHashMap<>();
        unequippedItem.put("cosmetic_id", item.getId());
        unequippedItem.put("name", item.getName());

        return UnequipCosmeticResponseDto.builder()
                .slot(item.getType().name())
                .unequippedItem(unequippedItem)
                .profileUpdated(profileUpdated)
                .build();
    }

    private ShopItemDto mapToShopItemDto(CosmeticItem c, Set<Long> ownedIds) {
        return ShopItemDto.builder()
                .cosmeticId(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .type(c.getType())
                .rarity(c.getRarity())
                .assetUrl(c.getAssetUrl())
                .price(c.getPrice())
                .originalPrice(c.getOriginalPrice())
                .availableUntil(c.getAvailableUtil())
                .isOwned(ownedIds.contains(c.getId()))
                .build();
    }
}
