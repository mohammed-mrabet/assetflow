package com.mrabet.assetflow.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mrabet.assetflow.maintenance.MaintenanceDtos.GenerationResult;

/** Déclenchement automatique quotidien (expression cron configurable, « - » pour désactiver). */
@Component
public class PreventiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(PreventiveScheduler.class);

    private final PreventiveMaintenanceService service;

    public PreventiveScheduler(PreventiveMaintenanceService service) {
        this.service = service;
    }

    @Scheduled(cron = "${assetflow.scheduler.cron}")
    public void run() {
        GenerationResult result = service.generateDueWorkOrders();
        log.info("Maintenance préventive : {} ordre(s) de travail créé(s) pour le {}",
                result.createdWorkOrderIds().size(), result.runDate());
    }
}
