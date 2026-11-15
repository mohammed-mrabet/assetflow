package com.mrabet.assetflow.kpi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Indicateurs de fiabilité sur une période [from, to[ :
 * MTTR (temps moyen de réparation), MTBF (temps moyen de bon fonctionnement entre pannes), disponibilité.
 */
public record ReliabilityKpis(int failures, double downtimeHours, double mttrHours, double mtbfHours, double availability) {

    /** Une panne : début de l'arrêt et fin de la réparation. */
    public record Failure(Instant failureAt, Instant repairedAt) {
    }

    public static ReliabilityKpis compute(List<Failure> failures, Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("La fin de période doit être postérieure au début");
        }
        double periodHours = hours(Duration.between(from, to));
        int count = 0;
        double downtime = 0;
        for (Failure f : failures) {
            if (f.failureAt().isBefore(from) || !f.failureAt().isBefore(to)) {
                continue; // panne hors période
            }
            count++;
            Instant end = f.repairedAt() == null || f.repairedAt().isAfter(to) ? to : f.repairedAt();
            downtime += hours(Duration.between(f.failureAt(), end));
        }
        double uptime = periodHours - downtime;
        double mttr = count == 0 ? 0 : downtime / count;
        double mtbf = count == 0 ? uptime : uptime / count;
        return new ReliabilityKpis(count, round(downtime), round(mttr), round(mtbf), round(uptime / periodHours));
    }

    private static double hours(Duration d) {
        return d.toSeconds() / 3600.0;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
