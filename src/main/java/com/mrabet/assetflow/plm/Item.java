package com.mrabet.assetflow.plm;

import com.mrabet.assetflow.common.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Article PLM : un ensemble (avec nomenclature) ou une pièce (avec stock de rechange). */
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    protected Item() {
    }

    public Item(String code, String name, ItemType type, int minStock) {
        if (minStock < 0) {
            throw new BusinessRuleException("Le stock minimum ne peut pas être négatif");
        }
        this.code = code;
        this.name = name;
        this.type = type;
        this.minStock = type == ItemType.PART ? minStock : 0;
    }

    public void receive(int quantity) {
        requirePart();
        if (quantity <= 0) {
            throw new BusinessRuleException("La quantité reçue doit être positive");
        }
        stockQuantity += quantity;
    }

    public void consume(int quantity) {
        requirePart();
        if (quantity <= 0) {
            throw new BusinessRuleException("La quantité consommée doit être positive");
        }
        if (quantity > stockQuantity) {
            throw new BusinessRuleException("Stock insuffisant pour " + code + " : " + stockQuantity + " disponible(s), " + quantity + " demandé(s)");
        }
        stockQuantity -= quantity;
    }

    public boolean isBelowMinimum() {
        return type == ItemType.PART && stockQuantity < minStock;
    }

    private void requirePart() {
        if (type != ItemType.PART) {
            throw new BusinessRuleException(code + " est un ensemble : seules les pièces sont gérées en stock");
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getStockQuantity() { return stockQuantity; }
    public int getMinStock() { return minStock; }
}
