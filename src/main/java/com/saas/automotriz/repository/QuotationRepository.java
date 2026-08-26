package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByBusinessOrderByCreatedAtDesc(Business business);
}
