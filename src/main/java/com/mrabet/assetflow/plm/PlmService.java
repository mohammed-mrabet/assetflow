package com.mrabet.assetflow.plm;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mrabet.assetflow.common.BusinessRuleException;
import com.mrabet.assetflow.common.NotFoundException;
import com.mrabet.assetflow.plm.PlmDtos.AddLineRequest;
import com.mrabet.assetflow.plm.PlmDtos.BomView;
import com.mrabet.assetflow.plm.PlmDtos.CreateItemRequest;
import com.mrabet.assetflow.plm.PlmDtos.ItemView;
import com.mrabet.assetflow.plm.PlmDtos.RequirementView;
import com.mrabet.assetflow.plm.PlmDtos.WhereUsedView;

@Service
public class PlmService {

    private final ItemRepository items;
    private final BomRevisionRepository boms;
    private final Clock clock;

    public PlmService(ItemRepository items, BomRevisionRepository boms, Clock clock) {
        this.items = items;
        this.boms = boms;
        this.clock = clock;
    }

    @Transactional
    public ItemView createItem(CreateItemRequest request) {
        if (items.findByCode(request.code()).isPresent()) {
            throw new BusinessRuleException("L'article " + request.code() + " existe déjà");
        }
        Item item = items.save(new Item(request.code(), request.name(), request.type(), request.minStock()));
        return ItemView.of(item);
    }

    @Transactional(readOnly = true)
    public List<ItemView> listItems() {
        return items.findAllByOrderByCodeAsc().stream().map(ItemView::of).toList();
    }

    @Transactional
    public ItemView receiveStock(String code, int quantity) {
        Item item = item(code);
        item.receive(quantity);
        return ItemView.of(item);
    }

    @Transactional(readOnly = true)
    public List<ItemView> belowMinimum() {
        return items.findBelowMinimum(ItemType.PART).stream().map(ItemView::of).toList();
    }

    /** Ouvre une nouvelle révision brouillon : A pour un nouvel ensemble, sinon copie de la révision en vigueur. */
    @Transactional
    public BomView createDraft(String itemCode) {
        Item item = item(itemCode);
        if (boms.existsByItem_IdAndStatus(item.getId(), RevisionStatus.DRAFT)) {
            throw new BusinessRuleException("Une révision brouillon est déjà ouverte pour " + itemCode);
        }
        BomRevision draft = boms.findFirstByItem_IdAndStatus(item.getId(), RevisionStatus.RELEASED)
                .map(BomRevision::newDraftFrom)
                .orElseGet(() -> BomRevision.firstDraft(item));
        return BomView.of(boms.save(draft));
    }

    @Transactional
    public BomView addLine(Long bomId, AddLineRequest request) {
        BomRevision bom = bom(bomId);
        Item child = item(request.childCode());
        if (BomExplosion.wouldCreateCycle(bom.getItem().getId(), child.getId(), releasedLinks())) {
            throw new BusinessRuleException("Ajouter " + child.getCode() + " sous " + bom.getItem().getCode() + " créerait une boucle");
        }
        bom.addLine(child, request.quantity());
        return BomView.of(bom);
    }

    @Transactional
    public BomView removeLine(Long bomId, String childCode) {
        BomRevision bom = bom(bomId);
        bom.removeLine(childCode);
        return BomView.of(bom);
    }

    /** Diffusion : la révision devient la référence, la précédente devient obsolète. */
    @Transactional
    public BomView release(Long bomId) {
        BomRevision bom = bom(bomId);
        Long itemId = bom.getItem().getId();
        for (BomLine line : bom.getLines()) {
            if (BomExplosion.wouldCreateCycle(itemId, line.getChild().getId(), releasedLinks())) {
                throw new BusinessRuleException("La diffusion créerait une boucle via " + line.getChild().getCode());
            }
        }
        boms.findFirstByItem_IdAndStatus(itemId, RevisionStatus.RELEASED).ifPresent(BomRevision::makeObsolete);
        bom.release(clock.instant());
        return BomView.of(bom);
    }

    @Transactional(readOnly = true)
    public BomView releasedBom(String itemCode) {
        Item item = item(itemCode);
        return boms.findFirstByItem_IdAndStatus(item.getId(), RevisionStatus.RELEASED)
                .map(BomView::of)
                .orElseThrow(() -> new NotFoundException("Aucune nomenclature diffusée pour " + itemCode));
    }

    @Transactional(readOnly = true)
    public List<RequirementView> explode(String itemCode) {
        return explode(item(itemCode).getId());
    }

    /** Éclatement multiniveau de la révision diffusée : quantités cumulées pour une unité. */
    @Transactional(readOnly = true)
    public List<RequirementView> explode(Long itemId) {
        Map<Long, Long> totals = BomExplosion.totalQuantities(itemId, releasedLinks());
        return items.findAllById(totals.keySet()).stream()
                .map(i -> new RequirementView(i.getCode(), i.getName(), i.getType(), totals.get(i.getId()), i.getStockQuantity(), i.isBelowMinimum()))
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WhereUsedView> whereUsed(String itemCode) {
        Item item = item(itemCode);
        return boms.findByStatusAndLines_Child_Id(RevisionStatus.RELEASED, item.getId()).stream()
                .map(b -> new WhereUsedView(b.getItem().getCode(), b.getItem().getName(), b.getRevision()))
                .toList();
    }

    private Function<Long, List<BomExplosion.Link>> releasedLinks() {
        return itemId -> boms.findFirstByItem_IdAndStatus(itemId, RevisionStatus.RELEASED)
                .map(b -> b.getLines().stream()
                        .map(l -> new BomExplosion.Link(itemId, l.getChild().getId(), l.getQuantity()))
                        .toList())
                .orElse(List.of());
    }

    Item item(String code) {
        return items.findByCode(code).orElseThrow(() -> new NotFoundException("Article inconnu : " + code));
    }

    private BomRevision bom(Long id) {
        return boms.findById(id).orElseThrow(() -> new NotFoundException("Nomenclature inconnue : " + id));
    }
}
