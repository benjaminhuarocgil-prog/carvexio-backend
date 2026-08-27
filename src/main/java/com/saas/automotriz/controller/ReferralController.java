package com.saas.automotriz.controller;
import com.saas.automotriz.model.Referral;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.ReferralRepository;
import com.saas.automotriz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.util.Map;
@RestController @RequestMapping("/api/referrals") @RequiredArgsConstructor
public class ReferralController {
 private static final String ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; private static final int GOAL=20;
 private final UserRepository users; private final ReferralRepository referrals; private final SecureRandom random=new SecureRandom();
 @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal User user) { User saved=ensureCode(user); long count=referrals.countByReferrer(saved); return Map.of("code",saved.getReferralCode(),"referrals",count,"goal",GOAL,"points",points(saved)); }
 @PostMapping("/claim") public ResponseEntity<?> claim(@AuthenticationPrincipal User user,@RequestParam String code) { User referred=ensureCode(user); if(referrals.existsByReferred(referred)) return ResponseEntity.ok(Map.of("claimed",false)); User referrer=users.findByReferralCode(code.toUpperCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Código de referido inválido.")); if(referrer.getId().equals(referred.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No puedes usar tu propio enlace."); Referral referral=new Referral(); referral.setReferrer(referrer); referral.setReferred(referred); referrals.save(referral); referrer.setRewardPoints(points(referrer)+1); users.save(referrer); return ResponseEntity.ok(Map.of("claimed",true)); }
 private User ensureCode(User user) { if(user.getReferralCode()!=null) return user; for(int i=0;i<10;i++){ String code=generate(); if(users.findByReferralCode(code).isEmpty()){ user.setReferralCode(code); return users.save(user); }} throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo generar el código."); }
 private String generate(){ StringBuilder s=new StringBuilder(); for(int i=0;i<10;i++) s.append(ALPHABET.charAt(random.nextInt(ALPHABET.length()))); return s.toString(); }
 private int points(User user){ return user.getRewardPoints()==null?0:user.getRewardPoints(); }
}
