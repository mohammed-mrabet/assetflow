package com.mrabet.assetflow.asset;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public final class AssetDtos {

    private AssetDtos() {
    }

    public record CreateAssetRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 120) String location,
            @NotNull Criticality criticality,
            @NotNull @PastOrPresent LocalDate commissionedOn,
            String modelItemCode) {
    }

    public record AssetView(Long id, String code, String name, String location, Criticality criticality,
                            AssetStatus status, LocalDate commissionedOn, String modelItemCode) {
        static AssetView of(Asset a) {
            String model = a.getModelItem() == null ? null : a.getModelItem().getCode();
            return new AssetView(a.getId(), a.getCode(), a.getName(), a.getLocation(), a.getCriticality(),
                    a.getStatus(), a.getCommissionedOn(), model);
        }
    }
}
