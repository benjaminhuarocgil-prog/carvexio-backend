package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "quotation_items")
public class QuotationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    private String description;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
