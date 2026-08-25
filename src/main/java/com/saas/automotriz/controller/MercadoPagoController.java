package com.saas.automotriz.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.OrderStatus;
import com.saas.automotriz.model.Order;
import com.saas.automotriz.model.OrderItem;
import com.saas.automotriz.model.Plan;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.OrderRepository;
import com.saas.automotriz.repository.PlanRepository;
import com.saas.automotriz.repository.UserRepository;
import com.saas.automotriz.request.OrderRequest;
import com.saas.automotriz.request.PlanPaymentRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mercadopago.exceptions.MPApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*") // Permite peticiones desde el frontend
public class MercadoPagoController {

    private static final String PLAN_REFERENCE_PREFIX = "PLAN:";

    private final OrderRepository orderRepository;
    private final PlanRepository planRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public MercadoPagoController(OrderRepository orderRepository, PlanRepository planRepository,
                                  BusinessRepository businessRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${app.backend-url:}")
    private String backendUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        // Inicializa el SDK con tu token apenas inicia el servidor
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @PostMapping("/create-preference")
    public String createPreference(@RequestBody OrderRequest order) {
        try {
            Order persistedOrder = order.getOrderId() == null ? null : orderRepository.findById(order.getOrderId()).orElse(null);
            if (persistedOrder == null) return "Error: pedido no encontrado";
            // 1. Forzamos el uso del token directamente en la petición (más seguro)
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PreferenceClient client = new PreferenceClient();

            // 2. Agregamos el item con la moneda explícita (PEN para Soles)
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Pedido #" + persistedOrder.getId() + " - " + persistedOrder.getBusiness().getName())
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(persistedOrder.getPaidAmount() == null ? persistedOrder.getTotalAmount() : persistedOrder.getPaidAmount()))
                    .currencyId("PEN") // <-- IMPORTANTE: Cambia a "ARS", "MXN", etc. según tu país
                    .build();
            items.add(item);

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(
                            PreferenceBackUrlsRequest.builder()
                                    .success(frontendUrl + "/cliente/compras")
                                    .failure(frontendUrl + "/cliente/compras")
                                    .pending(frontendUrl + "/cliente/compras")
                                    .build()
                    );

            // 3. Vinculamos la preferencia al pedido, para que el webhook sepa qué Order marcar como pagado
            if (order.getOrderId() != null) {
                requestBuilder.externalReference(String.valueOf(order.getOrderId()));
            }
            // 4. URL pública donde Mercado Pago avisará cuando el pago se complete
            if (backendUrl != null && !backendUrl.isBlank()) {
                requestBuilder.notificationUrl(backendUrl + "/api/payments/webhook");
            }
            // Comenta esta línea de abajo un momento si sigue fallando
            // .autoReturn("approved")

            // 5. Crear la preferencia
            Preference preference = client.create(requestBuilder.build(), options);

            return preference.getId();

        } catch (MPApiException e) {
            // ESTO NOS DARÁ EL ERROR REAL DE MERCADO PAGO
            System.err.println("Código de error MP: " + e.getApiResponse().getStatusCode());
            System.err.println("Respuesta de MP: " + e.getApiResponse().getContent());

            return "Error de Mercado Pago: " + e.getApiResponse().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error genérico: " + e.getMessage();
        }
    }

    // Crea la preferencia de pago para que un negocio se suscriba a un Plan
    @PostMapping("/create-plan-preference")
    public String createPlanPreference(@AuthenticationPrincipal User user, @RequestBody PlanPaymentRequest request) {
        try {
            Business business = businessRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new RuntimeException("No tienes un negocio registrado"));
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PreferenceClient client = new PreferenceClient();

            // El precio se toma del Plan guardado en el backend, nunca de lo que mande el cliente
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Suscripción Plan: " + plan.getName())
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(plan.getPrice()))
                    .currencyId("PEN")
                    .build();
            items.add(item);

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(
                            PreferenceBackUrlsRequest.builder()
                                    .success(frontendUrl + "/empresa/plan")
                                    .failure(frontendUrl + "/empresa/plan")
                                    .pending(frontendUrl + "/empresa/plan")
                                    .build()
                    )
                    .externalReference(PLAN_REFERENCE_PREFIX + business.getId() + ":" + plan.getId());

            if (backendUrl != null && !backendUrl.isBlank()) {
                requestBuilder.notificationUrl(backendUrl + "/api/payments/webhook");
            }

            Preference preference = client.create(requestBuilder.build(), options);
            return preference.getId();

        } catch (MPApiException e) {
            System.err.println("Código de error MP: " + e.getApiResponse().getStatusCode());
            System.err.println("Respuesta de MP: " + e.getApiResponse().getContent());
            return "Error de Mercado Pago: " + e.getApiResponse().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error genérico: " + e.getMessage();
        }
    }

    // Mercado Pago llama esta URL automáticamente cuando un pago cambia de estado
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId) {

        String eventType = topic != null ? topic : type;
        String paymentId = id != null ? id : dataId;

        if (!"payment".equals(eventType) || paymentId == null) {
            return ResponseEntity.ok().build();
        }

        try {
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId), options);

            String externalReference = payment.getExternalReference();
            String status = payment.getStatus(); // approved, pending, rejected, etc.


            if (externalReference != null && "approved".equals(status)) {
                if (externalReference.startsWith(PLAN_REFERENCE_PREFIX)) {
                    // Formato: PLAN:{businessId}:{planId}
                    String[] parts = externalReference.substring(PLAN_REFERENCE_PREFIX.length()).split(":");
                    if (parts.length == 2) {
                        Long businessId = Long.parseLong(parts[0]);
                        Long planId = Long.parseLong(parts[1]);
                        businessRepository.findById(businessId).ifPresent(business ->
                                planRepository.findById(planId).ifPresent(plan -> {
                                    business.setPlan(plan);
                                    businessRepository.save(business);
                                })
                        );
                    }
                } else {
                    Long orderId = Long.parseLong(externalReference);
                    orderRepository.findById(orderId).ifPresent(o -> {
                        if (o.getStatus() == OrderStatus.PENDING) {
                            o.setStatus(OrderStatus.PAID);
                            grantRewardPoints(o);
                            orderRepository.save(o);
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    private void grantRewardPoints(Order order) {
        if (Boolean.TRUE.equals(order.getRewardPointsGranted())) return;
        int points = order.getItems().stream()
                .mapToInt(item -> pointsForProduct(item.getPriceAtPurchase()) * item.getQuantity()).sum();
        User client = order.getClient();
        client.setRewardPoints((client.getRewardPoints() == null ? 0 : client.getRewardPoints()) + points);
        order.setRewardPointsGranted(true);
        userRepository.save(client);
    }

    private int pointsForProduct(double price) {
        if (price < 50) return 5;
        if (price < 100) return 10;
        if (price < 500) return 25;
        if (price < 1000) return 60;
        return 120;
    }
}

