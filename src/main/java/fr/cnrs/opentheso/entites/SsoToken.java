package fr.cnrs.opentheso.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sso_tokens")
@Getter
@Setter
public class SsoToken {

    @Id
    @Column(name = "token", updatable = false, nullable = false)
    private UUID token;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;
}