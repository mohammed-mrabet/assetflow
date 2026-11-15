package com.mrabet.assetflow.workorder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.AssetService;
import com.mrabet.assetflow.common.NotFoundException;
import com.mrabet.assetflow.plm.Item;
import com.mrabet.assetflow.plm.ItemRepository;
import com.mrabet.assetflow.workorder.WorkOrderDtos.DeclareFailureRequest;
import com.mrabet.assetflow.workorder.WorkOrderDtos.WorkOrderView;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrders;
    private final AssetService assets;
    private final ItemRepository items;
    private final Clock clock;

    public WorkOrderService(WorkOrderRepository workOrders, AssetService assets, ItemRepository items, Clock clock) {
        this.workOrders = workOrders;
        this.assets = assets;
        this.items = items;
        this.clock = clock;
    }

    @Transactional
    public WorkOrderView declareFailure(DeclareFailureRequest request) {
        Asset asset = assets.asset(request.assetId());
        Instant now = clock.instant();
        Instant failureAt = request.failureAt() != null ? request.failureAt() : now;
        WorkOrder wo = WorkOrder.corrective(asset, request.priority(), request.description(),
                request.assetStopped(), failureAt, now);
        return WorkOrderView.of(workOrders.save(wo));
    }

    @Transactional(readOnly = true)
    public List<WorkOrderView> list(Set<WorkOrderStatus> statuses, Long assetId) {
        List<WorkOrder> found;
        if (assetId != null) {
            found = workOrders.findByAsset_IdOrderByIdDesc(assetId).stream()
                    .filter(w -> statuses == null || statuses.isEmpty() || statuses.contains(w.getStatus()))
                    .toList();
        } else if (statuses != null && !statuses.isEmpty()) {
            found = workOrders.findByStatusInOrderByIdDesc(statuses);
        } else {
            found = workOrders.findAllByOrderByIdDesc();
        }
        return found.stream().map(WorkOrderView::of).toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderView get(Long id) {
        return WorkOrderView.of(workOrder(id));
    }

    @Transactional
    public WorkOrderView schedule(Long id, LocalDate date) {
        WorkOrder wo = workOrder(id);
        wo.schedule(date);
        return WorkOrderView.of(wo);
    }

    @Transactional
    public WorkOrderView start(Long id) {
        WorkOrder wo = workOrder(id);
        wo.start(clock.instant());
        return WorkOrderView.of(wo);
    }

    @Transactional
    public WorkOrderView consume(Long id, String itemCode, int quantity) {
        WorkOrder wo = workOrder(id);
        Item item = items.findByCode(itemCode).orElseThrow(() -> new NotFoundException("Article inconnu : " + itemCode));
        wo.consume(item, quantity);
        return WorkOrderView.of(wo);
    }

    @Transactional
    public WorkOrderView complete(Long id, String report) {
        WorkOrder wo = workOrder(id);
        wo.complete(report, clock.instant(), LocalDate.now(clock));
        return WorkOrderView.of(wo);
    }

    @Transactional
    public WorkOrderView close(Long id) {
        WorkOrder wo = workOrder(id);
        wo.close();
        return WorkOrderView.of(wo);
    }

    @Transactional
    public WorkOrderView cancel(Long id) {
        WorkOrder wo = workOrder(id);
        wo.cancel();
        return WorkOrderView.of(wo);
    }

    private WorkOrder workOrder(Long id) {
        return workOrders.findById(id).orElseThrow(() -> new NotFoundException("Ordre de travail inconnu : " + id));
    }
}
