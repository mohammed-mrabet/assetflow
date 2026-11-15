package com.mrabet.assetflow.plm;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mrabet.assetflow.plm.PlmDtos.AddLineRequest;
import com.mrabet.assetflow.plm.PlmDtos.BomView;
import com.mrabet.assetflow.plm.PlmDtos.CreateItemRequest;
import com.mrabet.assetflow.plm.PlmDtos.ItemView;
import com.mrabet.assetflow.plm.PlmDtos.QuantityRequest;
import com.mrabet.assetflow.plm.PlmDtos.RequirementView;
import com.mrabet.assetflow.plm.PlmDtos.WhereUsedView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PlmController {

    private final PlmService plm;

    public PlmController(PlmService plm) {
        this.plm = plm;
    }

    @PostMapping("/items")
    public ResponseEntity<ItemView> createItem(@Valid @RequestBody CreateItemRequest request) {
        ItemView created = plm.createItem(request);
        return ResponseEntity.created(URI.create("/api/items/" + created.code())).body(created);
    }

    @GetMapping("/items")
    public List<ItemView> listItems() {
        return plm.listItems();
    }

    @GetMapping("/items/below-minimum")
    public List<ItemView> belowMinimum() {
        return plm.belowMinimum();
    }

    @PostMapping("/items/{code}/receipts")
    public ItemView receive(@PathVariable String code, @Valid @RequestBody QuantityRequest request) {
        return plm.receiveStock(code, request.quantity());
    }

    @PostMapping("/items/{code}/bom-revisions")
    public ResponseEntity<BomView> createDraft(@PathVariable String code) {
        BomView draft = plm.createDraft(code);
        return ResponseEntity.created(URI.create("/api/bom-revisions/" + draft.id())).body(draft);
    }

    @GetMapping("/items/{code}/bom")
    public BomView releasedBom(@PathVariable String code) {
        return plm.releasedBom(code);
    }

    @GetMapping("/items/{code}/explosion")
    public List<RequirementView> explode(@PathVariable String code) {
        return plm.explode(code);
    }

    @GetMapping("/items/{code}/where-used")
    public List<WhereUsedView> whereUsed(@PathVariable String code) {
        return plm.whereUsed(code);
    }

    @PostMapping("/bom-revisions/{id}/lines")
    public BomView addLine(@PathVariable Long id, @Valid @RequestBody AddLineRequest request) {
        return plm.addLine(id, request);
    }

    @DeleteMapping("/bom-revisions/{id}/lines/{childCode}")
    public BomView removeLine(@PathVariable Long id, @PathVariable String childCode) {
        return plm.removeLine(id, childCode);
    }

    @PostMapping("/bom-revisions/{id}/release")
    public BomView release(@PathVariable Long id) {
        return plm.release(id);
    }
}
