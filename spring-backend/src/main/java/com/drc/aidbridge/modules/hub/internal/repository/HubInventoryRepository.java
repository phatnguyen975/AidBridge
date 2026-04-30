package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HubInventoryRepository extends JpaRepository<HubInventory, UUID> {
    Optional<HubInventory> findByHubIdAndItemCategoryId(UUID hubId, UUID itemCategoryId);
    List<HubInventory> findAllByHubId(UUID hubId);

    @Query(value = """
            SELECT
                hi.id AS "inventoryId",
                child.id AS "itemCategoryId",
                child.name AS "name",
                child.unit AS "unit",
                child.icon_url AS "iconUrl",
                COALESCE(parent.id, child.id) AS "parentCategoryId",
                COALESCE(parent.name, child.name) AS "parentCategoryName",
                hi.current_quantity AS "currentQuantity",
                hi.low_stock_threshold AS "lowStockThreshold",
                hi.last_restocked_at AS "lastRestockedAt"
            FROM hub_inventories hi
            JOIN item_categories child ON child.id = hi.item_category_id
            LEFT JOIN item_categories parent ON parent.id = child.parent_id
            WHERE hi.hub_id = :hubId
              AND (
                  CAST(:parentCategoryId AS uuid) IS NULL
                  OR child.parent_id = CAST(:parentCategoryId AS uuid)
                  OR child.id = CAST(:parentCategoryId AS uuid)
              )
              AND (
                  :parentCategoryName IS NULL
                  OR LOWER(parent.name) = LOWER(:parentCategoryName)
                  OR LOWER(child.name) = LOWER(:parentCategoryName)
              )
              AND (:keyword IS NULL OR LOWER(child.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY COALESCE(parent.sort_order, 0) ASC, COALESCE(child.sort_order, 0) ASC, child.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM hub_inventories hi
            JOIN item_categories child ON child.id = hi.item_category_id
            LEFT JOIN item_categories parent ON parent.id = child.parent_id
            WHERE hi.hub_id = :hubId
              AND (
                  CAST(:parentCategoryId AS uuid) IS NULL
                  OR child.parent_id = CAST(:parentCategoryId AS uuid)
                  OR child.id = CAST(:parentCategoryId AS uuid)
              )
              AND (
                  :parentCategoryName IS NULL
                  OR LOWER(parent.name) = LOWER(:parentCategoryName)
                  OR LOWER(child.name) = LOWER(:parentCategoryName)
              )
              AND (:keyword IS NULL OR LOWER(child.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            nativeQuery = true)
    Page<StaffHubInventoryRow> searchStaffHubInventory(@Param("hubId") UUID hubId,
                                                       @Param("parentCategoryId") UUID parentCategoryId,
                                                       @Param("parentCategoryName") String parentCategoryName,
                                                       @Param("keyword") String keyword,
                                                       Pageable pageable);

    interface StaffHubInventoryRow {
        UUID getInventoryId();
        UUID getItemCategoryId();
        String getName();
        String getUnit();
        String getIconUrl();
        UUID getParentCategoryId();
        String getParentCategoryName();
        Integer getCurrentQuantity();
        Integer getLowStockThreshold();
        Instant getLastRestockedAt();
    }
}
