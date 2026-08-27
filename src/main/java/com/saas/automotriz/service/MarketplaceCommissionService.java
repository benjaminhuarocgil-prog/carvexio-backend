package com.saas.automotriz.service;

import com.saas.automotriz.model.Order;
import com.saas.automotriz.model.PlatformSettings;
import com.saas.automotriz.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Calculates and snapshots the marketplace split when a payment is confirmed. */
@Service
@RequiredArgsConstructor
public class MarketplaceCommissionService {

    private final PlatformSettingsRepository platformSettingsRepository;

    public void applyTo(Order order) {
        int rate = platformSettingsRepository.findById(1L)
                .map(PlatformSettings::getMarketplaceCommissionRate)
                .filter(value -> value >= 20 && value <= 40)
                .orElse(20);
        double paidAmount = order.getPaidAmount() == null
                ? safeAmount(order.getTotalAmount())
                : order.getPaidAmount();
        double commission = roundMoney(paidAmount * rate / 100.0);

        order.setPlatformCommissionRate(rate);
        order.setPlatformCommissionAmount(commission);
        order.setBusinessPayoutAmount(roundMoney(paidAmount - commission));
    }

    private double safeAmount(Double amount) {
        return amount == null ? 0.0 : amount;
    }

    private double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
