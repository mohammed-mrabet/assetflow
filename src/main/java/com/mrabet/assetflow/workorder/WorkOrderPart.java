package com.mrabet.assetflow.workorder;

import com.mrabet.assetflow.plm.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pièce de rechange consommée pendant une intervention. */
@Entity
@Table(name = "work_order_part")
public class WorkOrderPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    protected WorkOrderPart() {
    }

    WorkOrderPart(WorkOrder workOrder, Item item, int quantity) {
        this.workOrder = workOrder;
        this.item = item;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
}
