package com.saas.automotriz.controller;

import com.saas.automotriz.dto.QuotationDTO;
import com.saas.automotriz.dto.QuotationItemDTO;
import com.saas.automotriz.model.*;
import com.saas.automotriz.repository.BookingRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.QuotationRepository;
import com.saas.automotriz.request.QuotationItemRequest;
import com.saas.automotriz.request.QuotationRequest;
import com.saas.automotriz.service.QuotationPdfService;
import com.saas.automotriz.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {
    private final QuotationRepository quotationRepository;
    private final BookingRepository bookingRepository;
    private final BusinessRepository businessRepository;
    private final QuotationPdfService quotationPdfService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    @Transactional(readOnly = true)
    public List<QuotationDTO> list(@AuthenticationPrincipal User user) {
        Business business = businessFor(user);
        return quotationRepository.findByBusinessOrderByCreatedAtDesc(business).stream().map(this::toDTO).toList();
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public List<QuotationDTO> listForClient(@AuthenticationPrincipal User user) {
        return quotationRepository.findByClientAndSentToClientTrueOrderBySentAtDesc(user).stream().map(this::toDTO).toList();
    }

    @PostMapping(value = "/diagnosis-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, String>> uploadDiagnosisPhoto(@AuthenticationPrincipal User user,
                                                                                @RequestParam("file") MultipartFile file) {
        businessFor(user);
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes subir imágenes del diagnóstico.");
        }
        return ResponseEntity.ok(java.util.Map.of("url", cloudinaryService.uploadImage(file)));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<QuotationDTO> create(@AuthenticationPrincipal User user, @RequestBody QuotationRequest request) {
        Business business = businessFor(user);
        Quotation quotation = new Quotation();
        quotation.setBusiness(business);
        fillQuotation(quotation, business, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(quotationRepository.save(quotation)));
    }

    @PutMapping("/{id}")
    @Transactional
    public QuotationDTO update(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody QuotationRequest request) {
        Business business = businessFor(user);
        Quotation quotation = owned(id, business);
        if (quotation.getStatus() == QuotationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una cotización aprobada no se puede modificar.");
        }
        fillQuotation(quotation, business, request);
        return toDTO(quotationRepository.save(quotation));
    }

    @PostMapping("/{id}/approve")
    public QuotationDTO approve(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Quotation quotation = owned(id, businessFor(user));
        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            quotation.setStatus(QuotationStatus.APPROVED);
            quotation.setApprovedAt(LocalDateTime.now());
            quotationRepository.save(quotation);
        }
        return toDTO(quotation);
    }

    @PostMapping("/{id}/send")
    public QuotationDTO sendToClient(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Quotation quotation = owned(id, businessFor(user));
        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primero debes aprobar la cotización.");
        }
        quotation.setSentToClient(true);
        quotation.setSentAt(LocalDateTime.now());
        return toDTO(quotationRepository.save(quotation));
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> receipt(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Quotation quotation = owned(id, businessFor(user));
        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primero debes aprobar la cotización para generar la boleta.");
        }
        byte[] pdf = quotationPdfService.generateReceipt(quotation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=boleta-cotizacion-" + quotation.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/my/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> receiptForClient(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada."));
        if (!quotation.isSentToClient() || !quotation.getClient().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta boleta.");
        }
        return pdfResponse(quotation);
    }

    private void fillQuotation(Quotation quotation, Business business, QuotationRequest request) {
        if (request.getBookingId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona la cita del cliente.");
        if (request.getDiagnosis() == null || request.getDiagnosis().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el diagnóstico encontrado.");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agrega al menos un ítem a la cotización.");

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada."));
        if (!booking.getBusiness().getId().equals(business.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cotizar una cita de otro negocio.");

        quotation.setBooking(booking);
        quotation.setClient(booking.getClient());
        quotation.setDiagnosis(request.getDiagnosis().trim());
        quotation.setDiagnosisPhotoUrls(request.getDiagnosisPhotoUrls() == null ? new ArrayList<>()
                : request.getDiagnosisPhotoUrls().stream().filter(url -> url != null && !url.isBlank()).toList());
        quotation.getItems().clear();
        List<QuotationItem> items = new ArrayList<>();
        double total = 0.0;
        for (QuotationItemRequest requestItem : request.getItems()) {
            if (requestItem.getDescription() == null || requestItem.getDescription().isBlank()
                    || requestItem.getQuantity() == null || requestItem.getQuantity() < 1
                    || requestItem.getUnitPrice() == null || requestItem.getUnitPrice() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada ítem debe tener descripción, cantidad y precio válidos.");
            }
            QuotationItem item = new QuotationItem();
            item.setQuotation(quotation);
            item.setDescription(requestItem.getDescription().trim());
            item.setQuantity(requestItem.getQuantity());
            item.setUnitPrice(requestItem.getUnitPrice());
            item.setSubtotal(item.getQuantity() * item.getUnitPrice());
            total += item.getSubtotal();
            items.add(item);
        }
        quotation.setItems(items);
        quotation.setTotalAmount(total);
    }

    private Business businessFor(User user) {
        return businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes un negocio registrado."));
    }

    private Quotation owned(Long id, Business business) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada."));
        if (!quotation.getBusiness().getId().equals(business.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta cotización.");
        return quotation;
    }

    private QuotationDTO toDTO(Quotation quote) {
        QuotationDTO dto = new QuotationDTO();
        dto.setId(quote.getId());
        dto.setBookingId(quote.getBooking() == null ? null : quote.getBooking().getId());
        dto.setClientName(quote.getClient().getName());
        dto.setClientPhone(quote.getClient().getPhone());
        dto.setServiceName(quote.getBooking() == null || quote.getBooking().getService() == null ? "Servicio" : quote.getBooking().getService().getName());
        if (quote.getBooking() != null && quote.getBooking().getVehicle() != null) {
            dto.setVehicleDescription(quote.getBooking().getVehicle().getVehicleType() + " · " + quote.getBooking().getVehicle().getPlate());
        }
        dto.setDiagnosis(quote.getDiagnosis());
        dto.setStatus(quote.getStatus().name());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setCreatedAt(quote.getCreatedAt());
        dto.setApprovedAt(quote.getApprovedAt());
        dto.setSentToClient(quote.isSentToClient());
        dto.setSentAt(quote.getSentAt());
        dto.setDiagnosisPhotoUrls(quote.getDiagnosisPhotoUrls());
        dto.setItems(quote.getItems().stream().map(item -> {
            QuotationItemDTO itemDto = new QuotationItemDTO();
            itemDto.setId(item.getId()); itemDto.setDescription(item.getDescription()); itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice()); itemDto.setSubtotal(item.getSubtotal());
            return itemDto;
        }).toList());
        return dto;
    }

    private ResponseEntity<byte[]> pdfResponse(Quotation quotation) {
        byte[] pdf = quotationPdfService.generateReceipt(quotation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=boleta-cotizacion-" + quotation.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
