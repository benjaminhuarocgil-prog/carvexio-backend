package com.saas.automotriz.repository;
import com.saas.automotriz.model.Referral;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReferralRepository extends JpaRepository<Referral, Long> { long countByReferrer(User referrer); boolean existsByReferred(User referred); }
