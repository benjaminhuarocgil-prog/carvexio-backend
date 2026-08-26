package com.saas.automotriz.request;

import lombok.Data;
import java.util.List;

@Data
public class QuotationRequest {
    private Long bookingId;
    private String diagnosis;
    private List<QuotationItemRequest> items;
}
