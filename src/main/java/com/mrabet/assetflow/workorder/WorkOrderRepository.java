package com.mrabet.assetflow.workorder;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findAllByOrderByIdDesc();

    List<WorkOrder> findByAsset_IdOrderByIdDesc(Long assetId);

    List<WorkOrder> findByStatusInOrderByIdDesc(Collection<WorkOrderStatus> statuses);

    boolean existsByPlan_IdAndStatusIn(Long planId, Collection<WorkOrderStatus> statuses);

    List<WorkOrder> findByAsset_IdAndTypeAndStatusIn(Long assetId, WorkOrderType type, Collection<WorkOrderStatus> statuses);
}
