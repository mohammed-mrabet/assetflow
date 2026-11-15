package com.mrabet.assetflow.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreventivePlanRepository extends JpaRepository<PreventivePlan, Long> {

    List<PreventivePlan> findByActiveTrue();

    List<PreventivePlan> findAllByOrderByIdAsc();
}
