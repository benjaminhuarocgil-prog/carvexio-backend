package com.saas.automotriz.model;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Data @Entity @Table(name = "referrals", uniqueConstraints = @UniqueConstraint(columnNames = "referred_id"))
public class Referral {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name = "referrer_id", nullable = false) private User referrer;
 @OneToOne @JoinColumn(name = "referred_id", nullable = false) private User referred;
 @CreationTimestamp private LocalDateTime createdAt;
}
