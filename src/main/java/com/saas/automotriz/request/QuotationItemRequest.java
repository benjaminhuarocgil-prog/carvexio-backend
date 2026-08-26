package com.saas.automotriz.request;

import lombok.Data;

@Data
public class QuotationItemRequest {
    private String description;
    private Integer quantity;
    private Double unitPrice;
}
