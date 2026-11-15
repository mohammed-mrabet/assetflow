package com.mrabet.assetflow.plm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.plm.BomExplosion.Link;

class BomExplosionTest {

    // 1 convoyeur = 2 motoréducteurs + 12 rouleaux ; 1 motoréducteur = 2 roulements + 1 joint ; 1 rouleau = 2 roulements
    private static final long CONV = 1, MOTOR = 2, ROLLER = 3, BEARING = 4, SEAL = 5;

    private static Function<Long, List<Link>> structure(List<Link> links) {
        return id -> links.stream().filter(l -> l.parentId() == id).toList();
    }

    private static final List<Link> CONVEYOR = List.of(
            new Link(CONV, MOTOR, 2), new Link(CONV, ROLLER, 12),
            new Link(MOTOR, BEARING, 2), new Link(MOTOR, SEAL, 1),
            new Link(ROLLER, BEARING, 2));

    @Test
    void quantitesCumuleesSurPlusieursNiveaux() {
        Map<Long, Long> totals = BomExplosion.totalQuantities(CONV, structure(CONVEYOR));
        assertEquals(Long.valueOf(2), totals.get(MOTOR));
        assertEquals(Long.valueOf(12), totals.get(ROLLER));
        assertEquals(Long.valueOf(2), totals.get(SEAL));
        // 2 × 2 (motoréducteurs) + 12 × 2 (rouleaux) = 28 roulements
        assertEquals(Long.valueOf(28), totals.get(BEARING));
        assertFalse(totals.containsKey(CONV));
    }

    @Test
    void articleSansNomenclature() {
        assertTrue(BomExplosion.totalQuantities(BEARING, structure(CONVEYOR)).isEmpty());
    }

    @Test
    void detectionDeCycle() {
        assertTrue(BomExplosion.wouldCreateCycle(MOTOR, CONV, structure(CONVEYOR)));   // CONV contient déjà MOTOR
        assertTrue(BomExplosion.wouldCreateCycle(BEARING, BEARING, structure(CONVEYOR)));
        assertFalse(BomExplosion.wouldCreateCycle(CONV, SEAL, structure(CONVEYOR)));
        assertFalse(BomExplosion.wouldCreateCycle(ROLLER, SEAL, structure(CONVEYOR)));
    }

    @Test
    void cycleExistantRefuseALEclatement() {
        List<Link> broken = List.of(new Link(CONV, MOTOR, 1), new Link(MOTOR, CONV, 1));
        assertThrows(BusinessRuleException.class, () -> BomExplosion.totalQuantities(CONV, structure(broken)));
    }
}
