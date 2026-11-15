package com.mrabet.assetflow.kpi;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mrabet.assetflow.asset.Asset;
import com.mrabet.assetflow.asset.AssetService;
import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.workorder.WorkOrderRepository;
import com.mrabet.assetflow.workorder.WorkOrderStatus;
import com.mrabet.assetflow.workorder.WorkOrderType;

@Service
public class KpiService {

    /** Toutes les pannes comptent, sauf les déclarations annulées. */
    private static final EnumSet<WorkOrderStatus> COUNTED = EnumSet.complementOf(EnumSet.of(WorkOrderStatus.CANCELLED));

    private final AssetService assets;
    private final WorkOrderRepository workOrders;
    private final Clock clock;

    public KpiService(AssetService assets, WorkOrderRepository workOrders, Clock clock) {
        this.assets = assets;
        this.workOrders = workOrders;
        this.clock = clock;
    }

    public record AssetKpis(String assetCode, LocalDate from, LocalDate to, ReliabilityKpis kpis) {
    }

    /** Indicateurs du jour {@code from} inclus au jour {@code to} inclus (limité à maintenant). */
    @Transactional(readOnly = true)
    public AssetKpis forAsset(Long assetId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("La date de fin doit être postérieure ou égale à la date de début");
        }
        Asset asset = assets.asset(assetId);
        Instant start = from.atStartOfDay(clock.getZone()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        Instant now = clock.instant();
        if (end.isAfter(now)) {
            end = now;
        }
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("La période demandée est dans le futur");
        }
        List<ReliabilityKpis.Failure> failures = workOrders
                .findByAsset_IdAndTypeAndStatusIn(assetId, WorkOrderType.CORRECTIVE, COUNTED).stream()
                .filter(w -> w.isAssetStopped() && w.getFailureAt() != null)
                .map(w -> new ReliabilityKpis.Failure(w.getFailureAt(), w.getCompletedAt()))
                .toList();
        return new AssetKpis(asset.getCode(), from, to, ReliabilityKpis.compute(failures, start, end));
    }
}
