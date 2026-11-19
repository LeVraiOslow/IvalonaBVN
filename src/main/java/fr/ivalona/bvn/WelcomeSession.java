package fr.ivalona.bvn;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fenêtre d'accueil ouverte à l'arrivée d'un nouveau joueur.
 * Chaque joueur ne peut la "réclamer" qu'une seule fois (set claimed).
 */
record WelcomeSession(UUID newcomer, String name, long expiresAt, Set<UUID> claimed) {

    WelcomeSession(UUID newcomer, String name, long expiresAt) {
        this(newcomer, name, expiresAt, new HashSet<>());
    }

    boolean expired(long now) {
        return now >= expiresAt;
    }
}
