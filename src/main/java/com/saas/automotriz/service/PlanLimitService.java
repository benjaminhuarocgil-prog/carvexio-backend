package com.saas.automotriz.service;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Plan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Limites del plan gratuito (Arranque). Un negocio sin plan asignado (plan == null,
// nunca eligio ninguno) se trata igual que el plan gratuito, para no dejar limites
// sin aplicar solo porque el negocio nunca entro a "Mi Plan".
@Service
public class PlanLimitService {

    public static final int FREE_MAX_BRANCHES = 1;
    public static final int FREE_MAX_PRODUCTS = 5;
    public static final int FREE_MAX_SERVICES = 5;
    public static final int FREE_MAX_BOOKINGS_PER_DAY = 2;

    public boolean isFreePlan(Business business) {
        Plan plan = business.getPlan();
        return plan == null || plan.getPrice() == null || plan.getPrice() <= 0;
    }

    public void checkBranchLimit(Business business, long currentActiveCount) {
        if (isFreePlan(business) && currentActiveCount >= FREE_MAX_BRANCHES) {
            throw limitReached("locales", FREE_MAX_BRANCHES);
        }
    }

    public void checkProductLimit(Business business, long currentActiveCount, int amountToAdd) {
        if (isFreePlan(business) && currentActiveCount + amountToAdd > FREE_MAX_PRODUCTS) {
            throw limitReached("productos", FREE_MAX_PRODUCTS);
        }
    }

    public void checkServiceLimit(Business business, long currentActiveCount, int amountToAdd) {
        if (isFreePlan(business) && currentActiveCount + amountToAdd > FREE_MAX_SERVICES) {
            throw limitReached("servicios", FREE_MAX_SERVICES);
        }
    }

    public void checkBookingDailyLimit(Business business, long currentCountForDay) {
        if (isFreePlan(business) && currentCountForDay >= FREE_MAX_BOOKINGS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El plan gratuito permite hasta " + FREE_MAX_BOOKINGS_PER_DAY
                            + " citas por día para este negocio. Vuelve a intentarlo otro día o pide al negocio mejorar su plan.");
        }
    }

    public void checkReportsAccess(Business business) {
        if (isFreePlan(business)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El plan gratuito no incluye el módulo de Reportes. Mejora tu plan para desbloquearlo.");
        }
    }

    public boolean hasCrmAccess(Business business) {
        Plan plan = business.getPlan();
        return plan != null && Boolean.TRUE.equals(plan.getHasCrm());
    }

    public void checkCrmDetailAccess(Business business) {
        if (!hasCrmAccess(business)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El plan gratuito no incluye el historial detallado de clientes (CRM). Mejora tu plan para desbloquearlo.");
        }
    }

    private ResponseStatusException limitReached(String recurso, int max) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Alcanzaste el límite de " + max + " " + recurso + " de tu plan gratuito. Mejora tu plan para agregar más.");
    }
}
