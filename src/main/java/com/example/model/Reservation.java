package com.example.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime debut;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutReservation statut = StatutReservation.CONFIRMEE;

    protected Reservation() {
    }

    public Reservation(Salle salle, Utilisateur utilisateur, LocalDateTime debut, LocalDateTime fin) {
        this.salle = salle;
        this.utilisateur = utilisateur;
        this.debut = debut;
        this.fin = fin;
    }

    public Long getId() { return id; }
    public Salle getSalle() { return salle; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public LocalDateTime getDebut() { return debut; }
    public LocalDateTime getFin() { return fin; }
    public StatutReservation getStatut() { return statut; }

    public void annuler() {
        this.statut = StatutReservation.ANNULEE;
    }
}