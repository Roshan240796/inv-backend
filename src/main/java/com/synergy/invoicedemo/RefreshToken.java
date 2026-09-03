package com.synergy.invoicedemo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    @ManyToOne(optional = false)
    private UserAccount user;
    protected RefreshToken() {}
    public RefreshToken(String tokenHash, Instant expiresAt, UserAccount user) { this.tokenHash = tokenHash; this.expiresAt = expiresAt; this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public UserAccount getUser() { return user; }
    public boolean isUsable() { return !revoked && expiresAt.isAfter(Instant.now()); }
    public void revoke() { revoked = true; }
}
