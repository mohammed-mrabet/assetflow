package com.mrabet.assetflow.maintenance;

import java.time.LocalDate;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.AssetStatus;
import com.mrabet.assetflow.common.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Plan de maintenance préventive à périodicité fixe (en jours). */
@Entity
@Table(name = "preventive_plan")
public class PreventivePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "last_done_on")
    private LocalDate lastDoneOn;

    @Column(nullable = false)
    private boolean active;

    protected PreventivePlan() {
    }

    public PreventivePlan(Asset asset, String title, int intervalDays, LocalDate lastDoneOn) {
        if (intervalDays <= 0) {
            throw new BusinessRuleException("La périodicité doit être d'au moins un jour");
        }
        this.asset = asset;
        this.title = title;
        this.intervalDays = intervalDays;
        this.lastDoneOn = lastDoneOn;
        this.active = true;
    }

    /** Prochaine échéance : dernière réalisation + période, ou mise en service + période. */
    public LocalDate nextDueOn() {
        LocalDate reference = lastDoneOn != null ? lastDoneOn : asset.getCommissionedOn();
        return reference.plusDays(intervalDays);
    }

    /** Dû si l'échéance tombe avant aujourd'hui + horizon, sur un plan actif d'un équipement non réformé. */
    public boolean isDue(LocalDate today, int horizonDays) {
        return active
                && asset.getStatus() != AssetStatus.RETIRED
                && !nextDueOn().isAfter(today.plusDays(horizonDays));
    }

    public void recordDone(LocalDate date) {
        if (lastDoneOn == null || date.isAfter(lastDoneOn)) {
            lastDoneOn = date;
        }
    }

    public void deactivate() {
        active = false;
    }

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public String getTitle() { return title; }
    public int getIntervalDays() { return intervalDays; }
    public LocalDate getLastDoneOn() { return lastDoneOn; }
    public boolean isActive() { return active; }
}
