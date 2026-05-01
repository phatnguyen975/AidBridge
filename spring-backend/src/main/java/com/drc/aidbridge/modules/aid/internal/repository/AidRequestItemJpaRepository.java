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
        * Runtime DB may not expose quantity/description columns on aid_request_items,
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
        where ari.aid_request_id = :aidRequestId
        group by ari.item_category_id, ic.name, ic.unit
        order by ic.name asc nulls last
        """, nativeQuery = true)
    List<AidRequestItemDetailProjection> findDetailRowsByAidRequestId(@Param("aidRequestId") UUID aidRequestId);

    /**
     * Returns 2-level aid categories (parent + child) from item_categories table.
     */
    @Query(value = """
        select
            p.id as parentId,
            p.name as parentName,
            c.id as childId,
            c.name as childName,
            c.unit as childUnit
        from item_categories p
        left join item_categories c on c.parent_id = p.id
        where p.parent_id is null
        order by p.sort_order asc nulls last, p.name asc, c.sort_order asc nulls last, c.name asc
        """, nativeQuery = true)
    List<AidCategoryProjection> findAidCategoryRows();

    @Query(value = """
        select count(ari.id)
        from aid_request_items ari
        inner join missions m on m.aid_request_id = ari.aid_request_id
        where m.hub_id = :hubId
          and m.mission_type = 'DELIVERY'
          and m.status = 'COMPLETED'
        """, nativeQuery = true)
    Long countExportedQuantityByHubId(@Param("hubId") UUID hubId);

    interface AidRequestItemDetailProjection {
        UUID getItemCategoryId();

        String getCategoryName();

        String getUnit();

        Long getItemCount();
    }

    interface AidCategoryProjection {
        UUID getParentId();

        String getParentName();

        UUID getChildId();

        String getChildName();

        String getChildUnit();
    }
}
