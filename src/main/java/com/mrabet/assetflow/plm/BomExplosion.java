package com.mrabet.assetflow.plm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.mrabet.assetflow.common.BusinessRuleException;

/**
 * Calculs sur la structure des nomenclatures, indépendants de la base de données :
 * éclatement multiniveau (quantités cumulées) et détection de cycles.
 */
public final class BomExplosion {

    /** Lien parent → enfant avec quantité, tel que lu dans la révision diffusée du parent. */
    public record Link(long parentId, long childId, int quantity) {
    }

    private BomExplosion() {
    }

    /**
     * Quantité totale de chaque article nécessaire pour fabriquer une unité de {@code rootId}.
     * Exemple : convoyeur → 2 motoréducteurs → 2 roulements chacun = 4 roulements.
     */
    public static Map<Long, Long> totalQuantities(long rootId, Function<Long, List<Link>> linksOf) {
        Map<Long, Long> totals = new LinkedHashMap<>();
        explode(rootId, 1L, linksOf, new ArrayDeque<>(), totals);
        return totals;
    }

    private static void explode(long itemId, long multiplier, Function<Long, List<Link>> linksOf,
                                Deque<Long> path, Map<Long, Long> totals) {
        if (path.contains(itemId)) {
            throw new BusinessRuleException("Cycle détecté dans la nomenclature (article " + itemId + ")");
        }
        path.push(itemId);
        for (Link link : linksOf.apply(itemId)) {
            long quantity = Math.multiplyExact(multiplier, (long) link.quantity());
            totals.merge(link.childId(), quantity, Long::sum);
            explode(link.childId(), quantity, linksOf, path, totals);
        }
        path.pop();
    }

    /** Vrai si ajouter {@code childId} sous {@code parentId} créerait une boucle. */
    public static boolean wouldCreateCycle(long parentId, long childId, Function<Long, List<Link>> linksOf) {
        if (parentId == childId) {
            return true;
        }
        Deque<Long> toVisit = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        toVisit.push(childId);
        while (!toVisit.isEmpty()) {
            long current = toVisit.pop();
            if (current == parentId) {
                return true;
            }
            if (seen.add(current)) {
                for (Link link : linksOf.apply(current)) {
                    toVisit.push(link.childId());
                }
            }
        }
        return false;
    }
}
