package com.mrabet.assetflow.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.Criticality;
import com.mrabet.assetflow.common.BusinessRuleException;

class PreventivePlanTest {

    private final Asset asset = new Asset("PUMP-02", "Pompe", null, Criticality.B, LocalDate.of(2026, 12, 1), null);

    @Test
    void premiereEcheanceCalculeeDepuisLaMiseEnService() {
        PreventivePlan plan = new PreventivePlan(asset, "Contrôle", 60, null);
        assertEquals(LocalDate.of(2027, 1, 30), plan.nextDueOn());
    }

    @Test
    void echeanceDansLHorizon() {
        PreventivePlan plan = new PreventivePlan(asset, "Graissage", 30, LocalDate.of(2027, 1, 1)); // échéance 31/01
        assertFalse(plan.isDue(LocalDate.of(2027, 1, 20), 7));
        assertTrue(plan.isDue(LocalDate.of(2027, 1, 24), 7));
        assertTrue(plan.isDue(LocalDate.of(2027, 2, 15), 0)); // en retard
        plan.deactivate();
        assertFalse(plan.isDue(LocalDate.of(2027, 2, 15), 0));
    }

    @Test
    void equipementReformeNeGenerePlusDePreventif() {
        PreventivePlan plan = new PreventivePlan(asset, "Graissage", 30, LocalDate.of(2027, 1, 1));
        asset.retire();
        assertFalse(plan.isDue(LocalDate.of(2027, 3, 1), 0));
    }

    @Test
    void realisationAnterieureNeReculePasLaDate() {
        PreventivePlan plan = new PreventivePlan(asset, "Graissage", 30, LocalDate.of(2027, 2, 1));
        plan.recordDone(LocalDate.of(2027, 1, 15));
        assertEquals(LocalDate.of(2027, 2, 1), plan.getLastDoneOn());
        assertThrows(BusinessRuleException.class, () -> new PreventivePlan(asset, "x", 0, null));
    }
}
