package com.mrabet.assetflow.workorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.AssetStatus;
import com.mrabet.assetflow.asset.Criticality;
import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.maintenance.PreventivePlan;
import com.mrabet.assetflow.plm.Item;
import com.mrabet.assetflow.plm.ItemType;

class WorkOrderTest {

    private static final Instant T0 = Instant.parse("2027-02-01T08:00:00Z");

    private static Asset asset() {
        return new Asset("CONV-01", "Convoyeur", "Ligne 1", Criticality.A, LocalDate.of(2024, 3, 1), null);
    }

    @Test
    void panneAvecArretMetEquipementHorsServicePuisLeRemetEnService() {
        Asset asset = asset();
        WorkOrder wo = WorkOrder.corrective(asset, Priority.P1, "Bande déchirée", true, T0, T0.plusSeconds(60));
        assertEquals(AssetStatus.DOWN, asset.getStatus());

        wo.start(T0.plusSeconds(600));
        wo.complete("Bande remplacée", T0.plusSeconds(7200), LocalDate.of(2027, 2, 1));

        assertEquals(WorkOrderStatus.DONE, wo.getStatus());
        assertEquals(AssetStatus.IN_SERVICE, asset.getStatus());
        assertEquals(Duration.ofHours(2), wo.downtime());
    }

    @Test
    void transitionsInterdites() {
        WorkOrder wo = WorkOrder.corrective(asset(), Priority.P2, "Bruit anormal", false, T0, T0);
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> wo.complete("rapport", T0, LocalDate.of(2027, 2, 1)));
        assertTrue(e.getMessage().contains("CREATED → DONE"));

        wo.start(T0);
        assertThrows(BusinessRuleException.class, wo::cancel);
        wo.complete("Roulement graissé", T0.plusSeconds(60), LocalDate.of(2027, 2, 1));
        wo.close();
        assertThrows(BusinessRuleException.class, () -> wo.start(T0));
    }

    @Test
    void compteRenduObligatoire() {
        WorkOrder wo = WorkOrder.corrective(asset(), Priority.P2, "Fuite", false, T0, T0);
        wo.start(T0);
        assertThrows(BusinessRuleException.class, () -> wo.complete("  ", T0, LocalDate.of(2027, 2, 1)));
        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
    }

    @Test
    void panneDansLeFuturRefusee() {
        assertThrows(BusinessRuleException.class,
                () -> WorkOrder.corrective(asset(), Priority.P1, "x", true, T0.plusSeconds(3600), T0));
    }

    @Test
    void consommationDePiecesUniquementPendantIntervention() {
        Item bearing = new Item("PRT-BRG-6204", "Roulement", ItemType.PART, 2);
        bearing.receive(5);
        WorkOrder wo = WorkOrder.corrective(asset(), Priority.P2, "Roulement HS", false, T0, T0);

        assertThrows(BusinessRuleException.class, () -> wo.consume(bearing, 1));
        wo.start(T0);
        wo.consume(bearing, 2);
        assertEquals(3, bearing.getStockQuantity());
        assertEquals(1, wo.getParts().size());

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> wo.consume(bearing, 10));
        assertTrue(e.getMessage().contains("Stock insuffisant"));
        assertEquals(3, bearing.getStockQuantity());
    }

    @Test
    void annulationRemetEquipementEnService() {
        Asset asset = asset();
        WorkOrder wo = WorkOrder.corrective(asset, Priority.P1, "Arrêt d'urgence déclenché", true, T0, T0);
        wo.cancel();
        assertEquals(WorkOrderStatus.CANCELLED, wo.getStatus());
        assertEquals(AssetStatus.IN_SERVICE, asset.getStatus());
    }

    @Test
    void otPreventifPlanifieEtMiseAJourDuPlan() {
        Asset asset = asset();
        PreventivePlan plan = new PreventivePlan(asset, "Graissage", 30, LocalDate.of(2027, 1, 1));
        WorkOrder wo = WorkOrder.preventive(plan, plan.nextDueOn(), T0);

        assertEquals(WorkOrderType.PREVENTIVE, wo.getType());
        assertEquals(WorkOrderStatus.PLANNED, wo.getStatus());
        assertEquals(LocalDate.of(2027, 1, 31), wo.getPlannedFor());

        wo.start(T0);
        wo.complete("Graissage effectué", T0.plusSeconds(1800), LocalDate.of(2027, 2, 1));
        assertEquals(LocalDate.of(2027, 2, 1), plan.getLastDoneOn());
        assertEquals(LocalDate.of(2027, 3, 3), plan.nextDueOn());
        assertEquals(Duration.ZERO, wo.downtime());
    }

    @Test
    void matriceDesTransitions() {
        assertTrue(WorkOrderStatus.CREATED.canMoveTo(WorkOrderStatus.PLANNED));
        assertTrue(WorkOrderStatus.PLANNED.canMoveTo(WorkOrderStatus.CANCELLED));
        assertFalse(WorkOrderStatus.IN_PROGRESS.canMoveTo(WorkOrderStatus.CANCELLED));
        assertFalse(WorkOrderStatus.DONE.canMoveTo(WorkOrderStatus.IN_PROGRESS));
        for (WorkOrderStatus target : WorkOrderStatus.values()) {
            assertFalse(WorkOrderStatus.CLOSED.canMoveTo(target));
            assertFalse(WorkOrderStatus.CANCELLED.canMoveTo(target));
        }
    }
}
