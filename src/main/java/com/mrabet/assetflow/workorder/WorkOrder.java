package com.mrabet.assetflow.workorder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.maintenance.PreventivePlan;
import com.mrabet.assetflow.plm.Item;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Ordre de travail (OT) de maintenance : toutes les règles de son cycle de vie sont ici. */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PreventivePlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "asset_stopped", nullable = false)
    private boolean assetStopped;

    @Column(name = "failure_at")
    private Instant failureAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "planned_for")
    private LocalDate plannedFor;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(length = 1000)
    private String report;

    /** Verrouillage optimiste : deux techniciens ne peuvent pas modifier le même OT en même temps. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderPart> parts = new ArrayList<>();

    protected WorkOrder() {
    }

    private WorkOrder(Asset asset, WorkOrderType type, Priority priority, String description, Instant now) {
        this.asset = asset;
        this.type = type;
        this.priority = priority;
        this.description = description;
        this.status = WorkOrderStatus.CREATED;
        this.createdAt = now;
    }

    /** Panne déclarée. Si l'équipement est à l'arrêt, il passe à l'état DOWN. */
    public static WorkOrder corrective(Asset asset, Priority priority, String description,
                                       boolean assetStopped, Instant failureAt, Instant now) {
        if (failureAt.isAfter(now)) {
            throw new BusinessRuleException("La date de panne ne peut pas être dans le futur");
        }
        WorkOrder wo = new WorkOrder(asset, WorkOrderType.CORRECTIVE, priority, description, now);
        wo.failureAt = failureAt;
        wo.assetStopped = assetStopped;
        if (assetStopped) {
            asset.markDown();
        }
        return wo;
    }

    /** OT généré par un plan préventif, déjà planifié à sa date d'échéance. */
    public static WorkOrder preventive(PreventivePlan plan, LocalDate dueOn, Instant now) {
        WorkOrder wo = new WorkOrder(plan.getAsset(), WorkOrderType.PREVENTIVE, Priority.P3, plan.getTitle(), now);
        wo.plan = plan;
        wo.moveTo(WorkOrderStatus.PLANNED);
        wo.plannedFor = dueOn;
        return wo;
    }

    public void schedule(LocalDate date) {
        moveTo(WorkOrderStatus.PLANNED);
        this.plannedFor = date;
    }

    public void start(Instant now) {
        moveTo(WorkOrderStatus.IN_PROGRESS);
        this.startedAt = now;
    }

    /** Sortie de pièce du stock, uniquement pendant l'intervention. */
    public void consume(Item item, int quantity) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Les pièces ne peuvent être consommées que pendant l'intervention");
        }
        item.consume(quantity);
        parts.add(new WorkOrderPart(this, item, quantity));
    }

    public void complete(String report, Instant now, LocalDate today) {
        if (report == null || report.isBlank()) {
            throw new BusinessRuleException("Le compte rendu d'intervention est obligatoire");
        }
        moveTo(WorkOrderStatus.DONE);
        this.report = report;
        this.completedAt = now;
        if (assetStopped) {
            asset.markInService();
        }
        if (plan != null) {
            plan.recordDone(today);
        }
    }

    public void close() {
        moveTo(WorkOrderStatus.CLOSED);
    }

    public void cancel() {
        moveTo(WorkOrderStatus.CANCELLED);
        if (assetStopped) {
            asset.markInService();
        }
    }

    /** Durée d'arrêt de l'équipement (de la panne à la fin de la réparation), pour les indicateurs. */
    public Duration downtime() {
        if (type != WorkOrderType.CORRECTIVE || failureAt == null || completedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(failureAt, completedAt);
    }

    private void moveTo(WorkOrderStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("Transition interdite : " + status + " → " + target);
        }
        status = target;
    }

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public PreventivePlan getPlan() { return plan; }
    public WorkOrderType getType() { return type; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public boolean isAssetStopped() { return assetStopped; }
    public Instant getFailureAt() { return failureAt; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getPlannedFor() { return plannedFor; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getReport() { return report; }
    public List<WorkOrderPart> getParts() { return Collections.unmodifiableList(parts); }
}
