package com.stockguard.data.dto.catalog.response;

import com.stockguard.data.entity.CatalogProduct;
import com.stockguard.util.CatalogCompleteness;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * One row of the admin "needs manual enrichment" backlog: a catalog
 * product that barcode-lookup enrichment could not complete, with the
 * individual gaps flagged so the admin knows what to search for.
 */
@Getter
@Builder
public class IncompleteCatalogProductDTO {

    private Long id;
    private String name;
    private String barcode;
    private String imageUrl;
    private Long suggestedSellPrice;
    private String externalSource;

    // What the admin should search for manually
    private boolean missingTitle;
    private boolean missingPrice;
    private boolean missingImage;

    // When this row last surfaced in a user barcode scan (null = never
    // since the column was introduced)
    private LocalDateTime lastLookupAt;

    public static IncompleteCatalogProductDTO from(CatalogProduct p) {
        return IncompleteCatalogProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .barcode(p.getBarcode())
                .imageUrl(p.getImageUrl())
                .suggestedSellPrice(p.getSuggestedSellPrice())
                .externalSource(p.getExternalSource())
                .missingTitle(CatalogCompleteness.titleMissing(p))
                .missingPrice(CatalogCompleteness.sellPriceMissing(p))
                .missingImage(CatalogCompleteness.imageMissing(p))
                .lastLookupAt(p.getLastLookupAt())
                .build();
    }
}
