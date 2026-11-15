package com.mrabet.assetflow.workorder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class WorkOrderDtos {

    private WorkOrderDtos() {
    }

    /** Déclaration de panne. failureAt est facultatif (par défaut : maintenant). */
    public record DeclareFailureRequest(
            @NotNull Long assetId,
            @NotNull Priority priority,
            @NotBlank @Size(max = 500) String description,
            boolean assetStopped,
            Instant failureAt) {
    }

    public record ScheduleRequest(@NotNull LocalDate date) {
    }

    public record ConsumePartRequest(@NotBlank String itemCode, @Positive int quantity) {
    }

    public record CompleteRequest(@NotBlank @Size(max = 1000) String report) {
    }

    public record PartUsage(String itemCode, int quantity) {
    }

    public record WorkOrderView(Long id, String assetCode, WorkOrderType type, Priority priority, WorkOrderStatus status,
                                String description, boolean assetStopped, Instant failureAt, Instant createdAt,
                                LocalDate plannedFor, Instant startedAt, Instant completedAt, String report,
                                long downtimeMinutes, List<PartUsage> parts) {
        static WorkOrderView of(WorkOrder w) {
            List<PartUsage> parts = w.getParts().stream()
                    .map(p -> new PartUsage(p.getItem().getCode(), p.getQuantity()))
                    .toList();
            return new WorkOrderView(w.getId(), w.getAsset().getCode(), w.getType(), w.getPriority(), w.getStatus(),
                    w.getDescription(), w.isAssetStopped(), w.getFailureAt(), w.getCreatedAt(), w.getPlannedFor(),
                    w.getStartedAt(), w.getCompletedAt(), w.getReport(), w.downtime().toMinutes(), parts);
        }
    }
}
