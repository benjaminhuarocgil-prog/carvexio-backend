package com.saas.automotriz.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuotationDTO {
    private Long id;
    private Long bookingId;
    private String clientName;
    private String clientPhone;
    private String serviceName;
    private String vehicleDescription;
    private String diagnosis;
    private String status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private List<QuotationItemDTO> items;
}
