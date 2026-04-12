package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AidRequestItemJpaRepository extends JpaRepository<AidRequestItem, UUID> {
    List<AidRequestItem> findByAidRequestId(UUID aidRequestId);

    List<AidRequestItem> findByAidRequestIdIn(List<UUID> aidRequestIds);

    /**
     * Aggregates aid request items by item category for detail payload.
     *
     * Runtime DB may not expose quantity/description/created_at on aid_request_items,
     * so item quantity is derived from COUNT(*) per item_category_id.
     */
    @Query(value = """
        select
            ari.item_category_id as itemCategoryId,
            ic.name as categoryName,
            ic.unit as unit,
            count(*) as itemCount
        from aid_request_items ari
        left join item_categories ic
            on ic.id = ari.item_category_id
            and ic.parent_id is not null
        where ari.aid_request_id = :aidRequestId
        group by ari.item_category_id, ic.name, ic.unit
        order by ic.name asc nulls last
        """, nativeQuery = true)
    List<AidRequestItemDetailProjection> findDetailRowsByAidRequestId(@Param("aidRequestId") UUID aidRequestId);

    interface AidRequestItemDetailProjection {
        UUID getItemCategoryId();

        String getCategoryName();

        String getUnit();

        Long getItemCount();
    }
}
