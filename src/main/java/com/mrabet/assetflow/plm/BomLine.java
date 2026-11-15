package com.mrabet.assetflow.plm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bom_line")
public class BomLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomRevision bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_item_id", nullable = false)
    private Item child;

    @Column(nullable = false)
    private int quantity;

    protected BomLine() {
    }

    BomLine(BomRevision bom, Item child, int quantity) {
        this.bom = bom;
        this.child = child;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public BomRevision getBom() { return bom; }
    public Item getChild() { return child; }
    public int getQuantity() { return quantity; }
}
