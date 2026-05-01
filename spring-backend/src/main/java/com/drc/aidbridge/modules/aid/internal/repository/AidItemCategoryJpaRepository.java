package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AidItemCategoryJpaRepository extends JpaRepository<AidItemCategory, UUID> {
    List<AidItemCategory> findByIsLeafTrue();
    List<AidItemCategory> findByIsLeafFalse();
    List<AidItemCategory> findByParentIdAndIsLeafTrueOrderBySortOrderAscNameAsc(UUID parentId);
    boolean existsByParentIdAndNameIgnoreCase(UUID parentId, String name);

    @Query("""
            SELECT child FROM AidItemCategory child
            WHERE child.parentId = :parentId
              AND child.isLeaf = true
              AND (:keyword IS NULL OR LOWER(child.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY child.sortOrder ASC, child.name ASC
            """)
    List<AidItemCategory> searchLeafSubCategories(@Param("parentId") UUID parentId,
                                                  @Param("keyword") String keyword);

    @Query("""
            SELECT COALESCE(MAX(child.sortOrder), 0)
            FROM AidItemCategory child
            WHERE child.parentId = :parentId
            """)
    Integer findMaxSortOrderByParentId(@Param("parentId") UUID parentId);

    @Query("""
            SELECT parent FROM AidItemCategory parent
            WHERE parent.id = :id
              AND (
                  parent.isLeaf = false
                  OR EXISTS (
                      SELECT child.id FROM AidItemCategory child
                      WHERE child.parentId = parent.id
                  )
              )
            """)
    Optional<AidItemCategory> findParentCategoryById(@Param("id") UUID id);

    @Query("""
            SELECT parent FROM AidItemCategory parent
            WHERE parent.name IN :names
            AND (
                parent.isLeaf = false
                OR EXISTS (
                    SELECT child.id FROM AidItemCategory child
                    WHERE child.parentId = parent.id
                )
            )
            """)
    List<AidItemCategory> findParentCategoriesByNames(@Param("names") Collection<String> names);
}
