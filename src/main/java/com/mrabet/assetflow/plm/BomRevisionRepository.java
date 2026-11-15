package com.mrabet.assetflow.plm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BomRevisionRepository extends JpaRepository<BomRevision, Long> {

    Optional<BomRevision> findFirstByItem_IdAndStatus(Long itemId, RevisionStatus status);

    Optional<BomRevision> findFirstByItem_IdOrderByIdDesc(Long itemId);

    boolean existsByItem_IdAndStatus(Long itemId, RevisionStatus status);

    /** Révisions dans un état donné qui contiennent un article (cas d'emploi). */
    List<BomRevision> findByStatusAndLines_Child_Id(RevisionStatus status, Long childId);
}
