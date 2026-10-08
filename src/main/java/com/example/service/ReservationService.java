package com.example.service;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.StatutReservation;
import com.example.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {
    private final EntityManagerFactory emf;
    private final Clock clock;

    public ReservationService(EntityManagerFactory emf) {
        this(emf, Clock.systemDefaultZone());
    }

    public ReservationService(EntityManagerFactory emf, Clock clock) {
        this.emf = emf;
        this.clock = clock;
    }

    public Reservation reserver(Long salleId, Long utilisateurId, LocalDateTime debut, LocalDateTime fin) {
        verifierCreneau(debut, fin);
        EntityManager em = emf.createEntityManager();
        try {
em.getTransaction().begin();
// Serialise les réservations concurrentes pour une même salle.
Salle salle = em.find(Salle.class, salleId, LockModeType.PESSIMISTIC_WRITE);
Utilisateur utilisateur = em.find(Utilisateur.class, utilisateurId);
            if (salle == null || utilisateur == null) {
        throw new IllegalArgumentException("Salle ou utilisateur introuvable.");
            }
                    if (!Boolean.TRUE.equals(salle.getDisponible())) {
        throw new IllegalArgumentException("Cette salle est indisponible.");
            }
Long conflits = em.createQuery(
                "SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :salleId " +
                        "AND r.statut = :statut AND r.debut < :fin AND r.fin > :debut", Long.class)
        .setParameter("salleId", salleId)
        .setParameter("statut", StatutReservation.CONFIRMEE)
        .setParameter("debut", debut)
        .setParameter("fin", fin)
        .getSingleResult();
            if (conflits > 0) {
        throw new IllegalArgumentException("Cette salle est déjà réservée sur ce créneau.");
            }
Reservation reservation = new Reservation(salle, utilisateur, debut, fin);
            em.persist(reservation);
            em.getTransaction().commit();
            return reservation;
        } catch (RuntimeException e) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
                em.close();
        }
                }

public void annuler(Long reservationId) {
    EntityManager em = emf.createEntityManager();
    try {
        em.getTransaction().begin();
        Reservation reservation = em.find(Reservation.class, reservationId, LockModeType.PESSIMISTIC_WRITE);
        if (reservation == null) {
            throw new IllegalArgumentException("Réservation introuvable.");
        }
        reservation.annuler();
        em.getTransaction().commit();
    } catch (RuntimeException e) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        throw e;
    } finally {
        em.close();
    }
}

public List<Reservation> lister() {
    EntityManager em = emf.createEntityManager();
    try {
        return em.createQuery("SELECT r FROM Reservation r ORDER BY r.debut DESC", Reservation.class)
                .getResultList();
    } finally {
        em.close();
    }
}

private void verifierCreneau(LocalDateTime debut, LocalDateTime fin) {
    if (debut == null || fin == null) {
        throw new IllegalArgumentException("Le début et la fin sont obligatoires.");
    }
    if (!debut.isAfter(LocalDateTime.now(clock))) {
        throw new IllegalArgumentException("Le début doit être dans le futur.");
    }
    if (!fin.isAfter(debut)) {
        throw new IllegalArgumentException("La fin doit être après le début.");
    }
    if (Duration.between(debut, fin).compareTo(Duration.ofHours(8)) > 0) {
        throw new IllegalArgumentException("Une réservation ne peut pas dépasser 8 heures.");
    }
}
}