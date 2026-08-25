package com.stockguard.data.dto;

import com.stockguard.data.entity.UserInvoiceProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInvoiceItemDTO {

    // Server-side UserProduct id (device resolves local → serverId before pushing)
    private Long productId;
    private Integer quantity;
    private Long priceAtSale;
    private Long costPriceAtTransaction;
    private Long discount;
    private Long total;

    public static UserInvoiceItemDTO from(UserInvoiceProduct item) {
        return new UserInvoiceItemDTO(
                item.getProductId(),
                item.getQuantity(),
                item.getPriceAtSale(),
                item.getCostPriceAtTransaction(),
                item.getDiscount(),
                item.getTotal()
        );
    }

    public UserInvoiceProduct toEntity() {
        UserInvoiceProduct item = new UserInvoiceProduct();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPriceAtSale(priceAtSale);
        item.setCostPriceAtTransaction(costPriceAtTransaction);
        item.setDiscount(discount != null ? discount : 0L);
        item.setTotal(total);
        return item;
    }
}
