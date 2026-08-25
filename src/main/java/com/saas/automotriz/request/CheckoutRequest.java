package com.saas.automotriz.request;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    private String address;     // Dirección de envío o recojo
    private String phone;       // Teléfono de contacto
    private String notes;       // Notas extra
    private List<Long> itemIds; // IDs específicos de los productos a comprar (opcional)
}
