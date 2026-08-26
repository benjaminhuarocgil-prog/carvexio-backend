package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class QuotationItemDTO {
    private Long id;
    private String description;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
