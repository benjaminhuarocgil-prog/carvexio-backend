package com.saas.automotriz.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private Long reservasHoy;
    private Long reservasPendientes;
    private Long reservasCompletadas;

    private Long pedidosTotales;
    private Long pedidosPendientes;
    private Long pedidosCompletados;

    private Double ingresosServicios;
    /** Venta cobrada antes de descontar la comisión de plataforma. */
    private Double ventasProductosBrutas;
    /** Comisión retenida por la plataforma en las ventas de productos. */
    private Double comisionMarketplace;
    /** Importe neto de productos que corresponde al taller. */
    private Double ingresosProductos;
    private Double ingresosTotal;
    private Integer commissionRate;

    private Long clientesTotal;
    private String servicioMasSolicitado;
    private String productoMasVendido;
}
