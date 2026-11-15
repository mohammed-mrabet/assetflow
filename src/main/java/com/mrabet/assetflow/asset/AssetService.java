package com.mrabet.assetflow.asset;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mrabet.assetflow.asset.AssetDtos.AssetView;
import com.mrabet.assetflow.asset.AssetDtos.CreateAssetRequest;
import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.common.NotFoundException;
import com.mrabet.assetflow.plm.Item;
import com.mrabet.assetflow.plm.ItemRepository;
import com.mrabet.assetflow.plm.ItemType;
import com.mrabet.assetflow.plm.PlmDtos.RequirementView;
import com.mrabet.assetflow.plm.PlmService;

@Service
public class AssetService {

    private final AssetRepository assets;
    private final ItemRepository items;
    private final PlmService plm;

    public AssetService(AssetRepository assets, ItemRepository items, PlmService plm) {
        this.assets = assets;
        this.items = items;
        this.plm = plm;
    }

    @Transactional
    public AssetView create(CreateAssetRequest request) {
        if (assets.findByCode(request.code()).isPresent()) {
            throw new BusinessRuleException("L'équipement " + request.code() + " existe déjà");
        }
        Item model = null;
        if (request.modelItemCode() != null && !request.modelItemCode().isBlank()) {
            model = items.findByCode(request.modelItemCode())
                    .orElseThrow(() -> new NotFoundException("Modèle inconnu : " + request.modelItemCode()));
            if (model.getType() != ItemType.ASSEMBLY) {
                throw new BusinessRuleException("Le modèle d'un équipement doit être un ensemble avec nomenclature");
            }
        }
        Asset asset = new Asset(request.code(), request.name(), request.location(), request.criticality(),
                request.commissionedOn(), model);
        return AssetView.of(assets.save(asset));
    }

    @Transactional(readOnly = true)
    public List<AssetView> list() {
        return assets.findAllByOrderByCodeAsc().stream().map(AssetView::of).toList();
    }

    @Transactional(readOnly = true)
    public AssetView get(Long id) {
        return AssetView.of(asset(id));
    }

    @Transactional
    public AssetView retire(Long id) {
        Asset asset = asset(id);
        asset.retire();
        return AssetView.of(asset);
    }

    /** Pièces de rechange de l'équipement, déduites de la nomenclature diffusée de son modèle. */
    @Transactional(readOnly = true)
    public List<RequirementView> spareParts(Long id) {
        Asset asset = asset(id);
        if (asset.getModelItem() == null) {
            return List.of();
        }
        return plm.explode(asset.getModelItem().getId()).stream()
                .filter(r -> r.type() == ItemType.PART)
                .toList();
    }

    public Asset asset(Long id) {
        return assets.findById(id).orElseThrow(() -> new NotFoundException("Équipement inconnu : " + id));
    }
}
