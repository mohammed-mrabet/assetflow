package com.mrabet.assetflow.maintenance;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class MaintenanceDtos {

    private MaintenanceDtos() {
    }

    public record CreatePlanRequest(
            @NotNull Long assetId,
            @NotBlank @Size(max = 120) String title,
            @Positive int intervalDays,
            LocalDate lastDoneOn) {
    }

    public record PlanView(Long id, String assetCode, String title, int intervalDays, LocalDate lastDoneOn,
                           LocalDate nextDueOn, boolean active) {
        static PlanView of(PreventivePlan p) {
            return new PlanView(p.getId(), p.getAsset().getCode(), p.getTitle(), p.getIntervalDays(),
                    p.getLastDoneOn(), p.nextDueOn(), p.isActive());
        }
    }

    public record GenerationResult(LocalDate runDate, int horizonDays, List<Long> createdWorkOrderIds) {
    }
}
