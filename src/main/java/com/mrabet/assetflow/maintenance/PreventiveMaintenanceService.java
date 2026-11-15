package com.mrabet.assetflow.maintenance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.AssetService;
import com.mrabet.assetflow.maintenance.MaintenanceDtos.CreatePlanRequest;
import com.mrabet.assetflow.maintenance.MaintenanceDtos.GenerationResult;
import com.mrabet.assetflow.maintenance.MaintenanceDtos.PlanView;
import com.mrabet.assetflow.workorder.WorkOrder;
import com.mrabet.assetflow.workorder.WorkOrderRepository;
import com.mrabet.assetflow.workorder.WorkOrderStatus;

@Service
public class PreventiveMaintenanceService {

    private final PreventivePlanRepository plans;
    private final WorkOrderRepository workOrders;
    private final AssetService assets;
    private final Clock clock;
    private final int horizonDays;

    public PreventiveMaintenanceService(PreventivePlanRepository plans, WorkOrderRepository workOrders,
                                        AssetService assets, Clock clock,
                                        @Value("${assetflow.scheduler.horizon-days:7}") int horizonDays) {
        this.plans = plans;
        this.workOrders = workOrders;
        this.assets = assets;
        this.clock = clock;
        this.horizonDays = horizonDays;
    }

    @Transactional
    public PlanView create(CreatePlanRequest request) {
        Asset asset = assets.asset(request.assetId());
        PreventivePlan plan = new PreventivePlan(asset, request.title(), request.intervalDays(), request.lastDoneOn());
        return PlanView.of(plans.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanView> list() {
        return plans.findAllByOrderByIdAsc().stream().map(PlanView::of).toList();
    }

    /**
     * Crée un OT préventif pour chaque plan arrivé à échéance.
     * Idempotent : aucun nouvel OT si le plan a déjà un OT ouvert.
     */
    @Transactional
    public GenerationResult generateDueWorkOrders() {
        LocalDate today = LocalDate.now(clock);
        List<Long> created = new ArrayList<>();
        for (PreventivePlan plan : plans.findByActiveTrue()) {
            if (!plan.isDue(today, horizonDays)) {
                continue;
            }
            if (workOrders.existsByPlan_IdAndStatusIn(plan.getId(), WorkOrderStatus.OPEN)) {
                continue;
            }
            WorkOrder wo = workOrders.save(WorkOrder.preventive(plan, plan.nextDueOn(), clock.instant()));
            created.add(wo.getId());
        }
        return new GenerationResult(today, horizonDays, created);
    }
}
