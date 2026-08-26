package com.saas.automotriz.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "quotations")
public class Quotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    private QuotationStatus status = QuotationStatus.DRAFT;

    private Double totalAmount = 0.0;
    private LocalDateTime approvedAt;
    private boolean sentToClient = false;
    private LocalDateTime sentAt;

    @ElementCollection
    @CollectionTable(name = "quotation_diagnosis_photos", joinColumns = @JoinColumn(name = "quotation_id"))
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private List<String> diagnosisPhotoUrls = new ArrayList<>();

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
