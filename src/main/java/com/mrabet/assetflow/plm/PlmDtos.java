package com.mrabet.assetflow.plm;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Objets échangés par l'API PLM (les entités ne sortent jamais de la couche service). */
public final class PlmDtos {

    private PlmDtos() {
    }

    public record CreateItemRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull ItemType type,
            @PositiveOrZero int minStock) {
    }

    public record QuantityRequest(@Positive int quantity) {
    }

    public record AddLineRequest(@NotBlank String childCode, @Positive int quantity) {
    }

    public record ItemView(Long id, String code, String name, ItemType type, int stockQuantity, int minStock, boolean belowMinimum) {
        static ItemView of(Item i) {
            return new ItemView(i.getId(), i.getCode(), i.getName(), i.getType(), i.getStockQuantity(), i.getMinStock(), i.isBelowMinimum());
        }
    }

    public record BomLineView(String childCode, String childName, ItemType childType, int quantity) {
    }

    public record BomView(Long id, String itemCode, String revision, RevisionStatus status, Instant releasedAt, List<BomLineView> lines) {
        static BomView of(BomRevision b) {
            List<BomLineView> lines = b.getLines().stream()
                    .map(l -> new BomLineView(l.getChild().getCode(), l.getChild().getName(), l.getChild().getType(), l.getQuantity()))
                    .toList();
            return new BomView(b.getId(), b.getItem().getCode(), b.getRevision(), b.getStatus(), b.getReleasedAt(), lines);
        }
    }

    /** Besoin issu de l'éclatement : quantité totale pour une unité de l'ensemble, et stock disponible. */
    public record RequirementView(String code, String name, ItemType type, long quantity, int stockQuantity, boolean belowMinimum) {
    }

    public record WhereUsedView(String parentCode, String parentName, String revision) {
    }
}
