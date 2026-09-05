package fr.cnrs.opentheso.v2.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entité JPA v2 sur la table {@code users}.
 * Nom d'entité Hibernate distinct ({@code V2User}) pour coexister avec le legacy.
 */
@Getter
@Setter
@Entity(name = "V2User")
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id_user", nullable = false)
    private Integer id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "mail", nullable = false)
    private String mail;

    @Column(name = "alertmail", nullable = false)
    private Boolean alertMail;

    @Column(name = "issuperadmin", nullable = false)
    private Boolean superAdmin;

    @Column(name = "apikey")
    private String apiKey;

    @Column(name = "key_never_expire", nullable = false)
    private Boolean keyNeverExpire;

    @Column(name = "key_expires_at")
    private LocalDate keyExpiresAt;

    public UserEntity() {
        // Constructeur sans argument requis par JPA.
    }
}
