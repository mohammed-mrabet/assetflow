package com.mrabet.assetflow.plm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.mrabet.assetflow.common.BusinessRuleException;

class BomRevisionTest {

    private final Item motor = new Item("ASM-MOT-075", "Motoréducteur", ItemType.ASSEMBLY, 0);
    private final Item bearing = new Item("PRT-BRG-6204", "Roulement", ItemType.PART, 4);
    private final Item seal = new Item("PRT-SEAL-20", "Joint", ItemType.PART, 2);

    @Test
    void cycleDeVieBrouillonDiffuseObsolete() {
        BomRevision a = BomRevision.firstDraft(motor);
        assertEquals("A", a.getRevision());
        a.addLine(bearing, 2);
        a.release(Instant.parse("2027-01-10T10:00:00Z"));
        assertEquals(RevisionStatus.RELEASED, a.getStatus());
        assertThrows(BusinessRuleException.class, () -> a.addLine(seal, 1));

        BomRevision b = a.newDraftFrom();
        assertEquals("B", b.getRevision());
        assertEquals(RevisionStatus.DRAFT, b.getStatus());
        assertEquals(1, b.getLines().size());
        b.addLine(seal, 1);
        a.makeObsolete();
        b.release(Instant.parse("2027-02-10T10:00:00Z"));
        assertEquals(RevisionStatus.OBSOLETE, a.getStatus());
        assertEquals(2, b.getLines().size());
        assertEquals(1, a.getLines().size());
    }

    @Test
    void reglesDeSaisie() {
        BomRevision draft = BomRevision.firstDraft(motor);
        assertThrows(BusinessRuleException.class, () -> draft.release(Instant.now()));
        assertThrows(BusinessRuleException.class, () -> draft.addLine(motor, 1));
        assertThrows(BusinessRuleException.class, () -> draft.addLine(bearing, 0));
        draft.addLine(bearing, 2);
        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> draft.addLine(bearing, 1));
        assertTrue(e.getMessage().contains("déjà"));
        draft.removeLine("PRT-BRG-6204");
        assertEquals(0, draft.getLines().size());
        assertThrows(BusinessRuleException.class, () -> BomRevision.firstDraft(bearing));
    }

    @Test
    void numerotationDesRevisions() {
        assertEquals("B", BomRevision.nextRevision("A"));
        assertEquals("AA", BomRevision.nextRevision("Z"));
        assertEquals("AB", BomRevision.nextRevision("AA"));
        assertEquals("BA", BomRevision.nextRevision("AZ"));
    }

    @Test
    void stockDesPieces() {
        bearing.receive(3);
        assertTrue(bearing.isBelowMinimum());
        bearing.receive(2);
        assertEquals(5, bearing.getStockQuantity());
        assertThrows(BusinessRuleException.class, () -> motor.receive(1));
        assertThrows(BusinessRuleException.class, () -> bearing.receive(-1));
    }
}
