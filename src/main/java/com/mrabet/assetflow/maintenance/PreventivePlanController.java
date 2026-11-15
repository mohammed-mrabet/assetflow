package com.mrabet.assetflow.maintenance;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mrabet.assetflow.maintenance.MaintenanceDtos.CreatePlanRequest;
import com.mrabet.assetflow.maintenance.MaintenanceDtos.GenerationResult;
import com.mrabet.assetflow.maintenance.MaintenanceDtos.PlanView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/preventive-plans")
public class PreventivePlanController {

    private final PreventiveMaintenanceService service;

    public PreventivePlanController(PreventiveMaintenanceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PlanView> create(@Valid @RequestBody CreatePlanRequest request) {
        PlanView created = service.create(request);
        return ResponseEntity.created(URI.create("/api/preventive-plans/" + created.id())).body(created);
    }

    @GetMapping
    public List<PlanView> list() {
        return service.list();
    }

    /** Déclenchement manuel de la génération (le même traitement tourne chaque matin). */
    @PostMapping("/generate")
    public GenerationResult generate() {
        return service.generateDueWorkOrders();
    }
}
