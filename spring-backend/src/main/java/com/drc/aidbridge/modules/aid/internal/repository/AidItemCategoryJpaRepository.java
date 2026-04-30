package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AidItemCategoryJpaRepository extends JpaRepository<AidItemCategory, UUID> {
    List<AidItemCategory> findByIsLeafTrue();
    List<AidItemCategory> findByIsLeafFalse();

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
