package com.mrabet.assetflow.asset;

import java.time.LocalDate;

import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.plm.Item;

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
import jakarta.persistence.Table;

/** Équipement physique suivi en maintenance, rattaché à un modèle PLM (sa nomenclature). */
@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Criticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Column(name = "commissioned_on", nullable = false)
    private LocalDate commissionedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_item_id")
    private Item modelItem;

    protected Asset() {
    }

    public Asset(String code, String name, String location, Criticality criticality,
                 LocalDate commissionedOn, Item modelItem) {
        this.code = code;
        this.name = name;
        this.location = location;
        this.criticality = criticality;
        this.commissionedOn = commissionedOn;
        this.modelItem = modelItem;
        this.status = AssetStatus.IN_SERVICE;
    }

    public void markDown() {
        requireNotRetired();
        status = AssetStatus.DOWN;
    }

    public void markInService() {
        requireNotRetired();
        status = AssetStatus.IN_SERVICE;
    }

    public void retire() {
        status = AssetStatus.RETIRED;
    }

    private void requireNotRetired() {
        if (status == AssetStatus.RETIRED) {
            throw new BusinessRuleException("L'équipement " + code + " est réformé");
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public Criticality getCriticality() { return criticality; }
    public AssetStatus getStatus() { return status; }
    public LocalDate getCommissionedOn() { return commissionedOn; }
    public Item getModelItem() { return modelItem; }
}
