package com.mrabet.assetflow.plm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.mrabet.assetflow.common.BusinessRuleException;

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

/**
 * Révision de nomenclature (BOM) d'un ensemble : A, B, C...
 * Cycle de vie : DRAFT (modifiable) → RELEASED (en vigueur, figée) → OBSOLETE (remplacée).
 */
@Entity
@Table(name = "bom_revision")
public class BomRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 5)
    private String revision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RevisionStatus status;

    @Column(name = "released_at")
    private Instant releasedAt;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomLine> lines = new ArrayList<>();

    protected BomRevision() {
    }

    private BomRevision(Item item, String revision) {
        this.item = item;
        this.revision = revision;
        this.status = RevisionStatus.DRAFT;
    }

    /** Première révision d'un ensemble. */
    public static BomRevision firstDraft(Item item) {
        if (item.getType() != ItemType.ASSEMBLY) {
            throw new BusinessRuleException(item.getCode() + " est une pièce : seule un ensemble a une nomenclature");
        }
        return new BomRevision(item, "A");
    }

    /** Nouvelle révision brouillon reprenant les lignes de celle-ci (demande de modification). */
    public BomRevision newDraftFrom() {
        BomRevision draft = new BomRevision(item, nextRevision(revision));
        for (BomLine line : lines) {
            draft.lines.add(new BomLine(draft, line.getChild(), line.getQuantity()));
        }
        return draft;
    }

    public void addLine(Item child, int quantity) {
        requireDraft();
        if (quantity <= 0) {
            throw new BusinessRuleException("La quantité d'une ligne de nomenclature doit être positive");
        }
        if (Objects.equals(child.getCode(), item.getCode())) {
            throw new BusinessRuleException("Un ensemble ne peut pas se contenir lui-même");
        }
        boolean duplicate = lines.stream().anyMatch(l -> l.getChild().getCode().equals(child.getCode()));
        if (duplicate) {
            throw new BusinessRuleException(child.getCode() + " figure déjà dans la révision " + revision);
        }
        lines.add(new BomLine(this, child, quantity));
    }

    public void removeLine(String childCode) {
        requireDraft();
        boolean removed = lines.removeIf(l -> l.getChild().getCode().equals(childCode));
        if (!removed) {
            throw new BusinessRuleException(childCode + " ne figure pas dans la révision " + revision);
        }
    }

    public void release(Instant now) {
        requireDraft();
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Impossible de diffuser une nomenclature vide");
        }
        status = RevisionStatus.RELEASED;
        releasedAt = now;
    }

    public void makeObsolete() {
        if (status != RevisionStatus.RELEASED) {
            throw new BusinessRuleException("Seule une révision diffusée peut devenir obsolète");
        }
        status = RevisionStatus.OBSOLETE;
    }

    private void requireDraft() {
        if (status != RevisionStatus.DRAFT) {
            throw new BusinessRuleException("La révision " + revision + " est " + status + " : elle n'est plus modifiable");
        }
    }

    /** A → B → ... → Z → AA → AB ... */
    static String nextRevision(String current) {
        char[] chars = current.toCharArray();
        int i = chars.length - 1;
        while (i >= 0) {
            if (chars[i] < 'Z') {
                chars[i]++;
                return new String(chars);
            }
            chars[i] = 'A';
            i--;
        }
        return "A" + new String(chars);
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public String getRevision() { return revision; }
    public RevisionStatus getStatus() { return status; }
    public Instant getReleasedAt() { return releasedAt; }
    public List<BomLine> getLines() { return Collections.unmodifiableList(lines); }
}
