package com.saas.automotriz.controller;

import com.saas.automotriz.dto.PlatformNotificationDTO;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.BusinessNotificationRecipient;
import com.saas.automotriz.repository.BusinessNotificationRecipientRepository;
import com.saas.automotriz.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business-notifications")
@RequiredArgsConstructor
public class BusinessNotificationController {
    private final BusinessRepository businessRepository;
    private final BusinessNotificationRecipientRepository recipientRepository;

    @GetMapping
    public ResponseEntity<List<PlatformNotificationDTO>> getMyNotifications(@AuthenticationPrincipal com.saas.automotriz.model.User user) {
        Business business = getBusiness(user.getId());
        return ResponseEntity.ok(recipientRepository.findByBusinessOrderByNotificationCreatedAtDesc(business).stream()
                .map(this::toDTO)
                .toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal com.saas.automotriz.model.User user) {
        Business business = getBusiness(user.getId());
        return ResponseEntity.ok(Map.of("count", recipientRepository.countByBusinessAndDismissedFalse(business)));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@AuthenticationPrincipal com.saas.automotriz.model.User user, @PathVariable Long id) {
        Business business = getBusiness(user.getId());
        BusinessNotificationRecipient recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada."));
        if (!recipient.getBusiness().getId().equals(business.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        recipient.setDismissed(true);
        recipientRepository.save(recipient);
        return ResponseEntity.noContent().build();
    }

    private Business getBusiness(Long userId) {
        return businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Negocio no encontrado."));
    }

    private PlatformNotificationDTO toDTO(BusinessNotificationRecipient recipient) {
        PlatformNotificationDTO dto = new PlatformNotificationDTO();
        dto.setId(recipient.getId());
        dto.setMessage(recipient.getNotification().getMessage());
        dto.setCommissionRate(recipient.getNotification().getCommissionRate());
        dto.setCreatedAt(recipient.getNotification().getCreatedAt());
        dto.setDismissed(Boolean.TRUE.equals(recipient.getDismissed()));
        return dto;
    }
}
