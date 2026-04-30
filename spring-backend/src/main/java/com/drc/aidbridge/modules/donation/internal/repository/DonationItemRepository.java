package com.drc.aidbridge.modules.donation.internal.repository;

import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationItemRepository extends JpaRepository<DonationItem, UUID> {

    /**
     * Aggregates donation item summary per donation id in a single DB round-trip.
     */
    @Query(value = """
            select
                di.donation_id as donationId,
                coalesce(string_agg(distinct ic.name, ', ' order by ic.name), '') as itemSummary,
                count(di.id) as totalQuantity
            from donation_items di
            left join item_categories ic on ic.id = di.item_category_id
            where di.donation_id in :donationIds
            group by di.donation_id
            """, nativeQuery = true)
    List<DonationHistoryAggregateProjection> findHistoryAggregatesByDonationIds(@Param("donationIds") List<UUID> donationIds);

    List<DonationItem> findAllByDonationId(UUID donationId);

    @Query(value = """
            select count(di.id)
            from donation_items di
            inner join donations d on d.id = di.donation_id
            where d.hub_id = :hubId
              and d.status = 'RECEIVED'
            """, nativeQuery = true)
    Long countImportedQuantityByHubId(@Param("hubId") UUID hubId);

    interface DonationHistoryAggregateProjection {
        UUID getDonationId();

        String getItemSummary();

        Long getTotalQuantity();
    }
}
