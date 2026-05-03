package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.repository.projection.HubSearchResultProjection;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.locationtech.jts.geom.Point;
import java.util.List;
import java.util.UUID;

@Repository
public interface HubRepository extends JpaRepository<Hub, UUID> {
	List<Hub> findByStatus(HubStatus status);

	long countByStatus(HubStatus status);

	@Query(value = """
			SELECT * FROM hubs h
			WHERE (CAST(:status AS text) IS NULL OR h.status::text = CAST(:status AS text))
			AND (
				CAST(:keyword AS text) IS NULL
				OR LOWER(h.name) LIKE LOWER('%' || CAST(:keyword AS text) || '%')
				OR LOWER(h.address) LIKE LOWER('%' || CAST(:keyword AS text) || '%')
			)
			ORDER BY h.created_at DESC
			""", nativeQuery = true)
	List<Hub> searchHubs(@Param("status") String status, @Param("keyword") String keyword);

	@Query(value = "SELECT h.id, h.name, h.address, h.phone_number as \"phoneNumber\", " +
			"h.image_url as \"imageUrl\", h.status, h.operating_hours as \"operatingHours\", " +
			"h.created_at as \"createdAt\", h.updated_at as \"updatedAt\", " +
			"ST_Y(h.location::geometry) as \"latitude\", " +
			"ST_X(h.location::geometry) as \"longitude\", " +
			"ST_Distance(h.location, CAST(:point AS geography)) as \"distanceInMeters\" " +
			"FROM hubs h WHERE (:status IS NULL OR h.status::text = :status) " +
			"AND ST_DWithin(h.location, CAST(:point AS geography), :radius) " +
			"ORDER BY \"distanceInMeters\" ASC", nativeQuery = true)
	List<HubSearchResultProjection> findByStatusAndLocationWithinRadius(@Param("status") String status, @Param("point") Point point,
			@Param("radius") double radius);
}
