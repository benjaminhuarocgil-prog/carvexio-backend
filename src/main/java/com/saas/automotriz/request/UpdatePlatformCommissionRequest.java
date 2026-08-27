package com.saas.automotriz.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePlatformCommissionRequest {
    @NotNull(message = "Indica el porcentaje de comisión.")
    @Min(value = 20, message = "La comisión mínima es 20%.")
    @Max(value = 40, message = "La comisión máxima es 40%.")
    private Integer commissionRate;
}
