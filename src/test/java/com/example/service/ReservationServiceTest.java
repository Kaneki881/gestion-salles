package com.example.service;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.StatutReservation;
import com.example.model.Utilisateur;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private EntityManagerFactory emf;
    private ReservationService reservations;
    private SalleService salles;
    private UtilisateurService utilisateurs;
    private Salle salle;
    private Utilisateur utilisateur;
    private final LocalDateTime debut = LocalDateTime.of(2026, 10, 9, 10, 0);

    @Before
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("gestion-salles");
        Clock clock = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC);
        reservations = new ReservationService(emf, clock);
        salles = new SalleService(emf);
        utilisateurs = new UtilisateurService(emf);
        salle = salles.save(new Salle("Salle Test", 20));
        utilisateur = utilisateurs.save(new Utilisateur("Martin", "Alice", "alice@example.com"));
    }

    @After
    public void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
    }

    @Test
    public void reserveAndCancelFreesTheSlot() {
        Reservation first = reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut.plusHours(1));
        assertNotNull(first.getId());
        assertEquals(1, reservations.lister().size());

        reservations.annuler(first.getId());
        assertEquals(StatutReservation.ANNULEE, reservations.lister().get(0).getStatut());
        Reservation second = reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut.plusHours(1));
        assertNotNull(second.getId());
    }

    @Test
    public void overlappingSlotIsRejectedButAdjacentSlotIsAllowed() {
        reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut.plusHours(1));
        expectInvalid(() -> reservations.reserver(salle.getId(), utilisateur.getId(),
                debut.plusMinutes(30), debut.plusHours(2)));
        reservations.reserver(salle.getId(), utilisateur.getId(), debut.plusHours(1), debut.plusHours(2));
        assertEquals(2, reservations.lister().size());
    }

    @Test
    public void unavailableRoomAndInvalidSlotsAreRejected() {
        salle.setDisponible(false);
        salles.update(salle);
        expectInvalid(() -> reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut.plusHours(1)));
        salle.setDisponible(true);
        salles.update(salle);

        expectInvalid(() -> reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut));
        expectInvalid(() -> reservations.reserver(salle.getId(), utilisateur.getId(), debut, debut.plusHours(9)));
        expectInvalid(() -> reservations.reserver(salle.getId(), utilisateur.getId(),
                LocalDateTime.of(2026, 10, 8, 9, 0), LocalDateTime.of(2026, 10, 8, 10, 0)));
    }

    @Test
    public void unknownEntitiesAreRejected() {
        expectInvalid(() -> reservations.reserver(999L, utilisateur.getId(), debut, debut.plusHours(1)));
        expectInvalid(() -> reservations.reserver(salle.getId(), 999L, debut, debut.plusHours(1)));
        expectInvalid(() -> reservations.annuler(999L));
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected business rule failure.
        }
    }
}