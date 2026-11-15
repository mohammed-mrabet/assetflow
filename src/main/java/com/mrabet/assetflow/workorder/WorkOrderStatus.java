package com.mrabet.assetflow.workorder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un ordre de travail :
 * CREATED → PLANNED → IN_PROGRESS → DONE → CLOSED, annulation possible avant le démarrage.
 */
public enum WorkOrderStatus {
    CREATED, PLANNED, IN_PROGRESS, DONE, CLOSED, CANCELLED;

    public static final Set<WorkOrderStatus> OPEN = EnumSet.of(CREATED, PLANNED, IN_PROGRESS);
    public static final Set<WorkOrderStatus> FINISHED = EnumSet.of(DONE, CLOSED);

    public boolean canMoveTo(WorkOrderStatus target) {
        return switch (this) {
            case CREATED -> target == PLANNED || target == IN_PROGRESS || target == CANCELLED;
            case PLANNED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == DONE;
            case DONE -> target == CLOSED;
            case CLOSED, CANCELLED -> false;
        };
    }

    public boolean isOpen() {
        return OPEN.contains(this);
    }
}
