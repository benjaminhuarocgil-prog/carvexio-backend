package com.saas.automotriz.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlatformNotificationRequest {
    @NotBlank(message = "Escribe el aviso para los negocios.")
    @Size(max = 2000, message = "El aviso no puede superar 2000 caracteres.")
    private String message;
}
