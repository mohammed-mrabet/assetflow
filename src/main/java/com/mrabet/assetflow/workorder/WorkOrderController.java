package com.mrabet.assetflow.workorder;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mrabet.assetflow.workorder.WorkOrderDtos.CompleteRequest;
import com.mrabet.assetflow.workorder.WorkOrderDtos.ConsumePartRequest;
import com.mrabet.assetflow.workorder.WorkOrderDtos.DeclareFailureRequest;
import com.mrabet.assetflow.workorder.WorkOrderDtos.ScheduleRequest;
import com.mrabet.assetflow.workorder.WorkOrderDtos.WorkOrderView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WorkOrderView> declareFailure(@Valid @RequestBody DeclareFailureRequest request) {
        WorkOrderView created = service.declareFailure(request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + created.id())).body(created);
    }

    @GetMapping
    public List<WorkOrderView> list(@RequestParam(required = false) Set<WorkOrderStatus> status,
                                    @RequestParam(required = false) Long assetId) {
        return service.list(status, assetId);
    }

    @GetMapping("/{id}")
    public WorkOrderView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/schedule")
    public WorkOrderView schedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return service.schedule(id, request.date());
    }

    @PostMapping("/{id}/start")
    public WorkOrderView start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/parts")
    public WorkOrderView consume(@PathVariable Long id, @Valid @RequestBody ConsumePartRequest request) {
        return service.consume(id, request.itemCode(), request.quantity());
    }

    @PostMapping("/{id}/complete")
    public WorkOrderView complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest request) {
        return service.complete(id, request.report());
    }

    @PostMapping("/{id}/close")
    public WorkOrderView close(@PathVariable Long id) {
        return service.close(id);
    }

    @PostMapping("/{id}/cancel")
    public WorkOrderView cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
