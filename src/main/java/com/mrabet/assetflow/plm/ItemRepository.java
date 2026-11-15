package com.mrabet.assetflow.plm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByCode(String code);

    List<Item> findAllByOrderByCodeAsc();

    @Query("select i from Item i where i.type = :type and i.stockQuantity < i.minStock order by i.code")
    List<Item> findBelowMinimum(@Param("type") ItemType type);
}
