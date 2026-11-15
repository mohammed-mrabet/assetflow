package com.mrabet.assetflow.asset;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mrabet.assetflow.asset.AssetDtos.AssetView;
import com.mrabet.assetflow.asset.AssetDtos.CreateAssetRequest;
import com.mrabet.assetflow.kpi.KpiService;
import com.mrabet.assetflow.kpi.KpiService.AssetKpis;
import com.mrabet.assetflow.plm.PlmDtos.RequirementView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assets;
    private final KpiService kpis;

    public AssetController(AssetService assets, KpiService kpis) {
        this.assets = assets;
        this.kpis = kpis;
    }

    @PostMapping
    public ResponseEntity<AssetView> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetView created = assets.create(request);
        return ResponseEntity.created(URI.create("/api/assets/" + created.id())).body(created);
    }

    @GetMapping
    public List<AssetView> list() {
        return assets.list();
    }

    @GetMapping("/{id}")
    public AssetView get(@PathVariable Long id) {
        return assets.get(id);
    }

    @PostMapping("/{id}/retire")
    public AssetView retire(@PathVariable Long id) {
        return assets.retire(id);
    }

    @GetMapping("/{id}/spare-parts")
    public List<RequirementView> spareParts(@PathVariable Long id) {
        return assets.spareParts(id);
    }

    @GetMapping("/{id}/kpis")
    public AssetKpis kpis(@PathVariable Long id,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return kpis.forAsset(id, from, to);
    }
}
