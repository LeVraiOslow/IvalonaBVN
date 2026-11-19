# IvalonaBVN

Plugin Paper 26.2 (Java 25) + Vault.

## Fonctionnement
- Un joueur rejoint le serveur **pour la première fois** -> tous les autres joueurs reçoivent un message cliquable les invitant à faire `/bvn`.
- `/bvn` (dans les 20 s) -> le chat affiche « Bienvenue <joueur> sur Ivalona ! » et le joueur reçoit la récompense via Vault.
- Chaque joueur ne peut faire `/bvn` qu'**une seule fois** par nouvel arrivant. Après 20 s, c'est terminé.
- Le nouveau joueur ne peut pas se souhaiter la bienvenue lui-même.

## Compilation
Prérequis : JDK 25 et Maven 3.9+.

    mvn clean package

Le jar est généré dans `target/IvalonaBVN-1.0.0.jar` -> à mettre dans `plugins/`.

## Prérequis serveur
Paper 26.2, Java 25, Vault et un plugin d'économie (ex. EssentialsX). Nexo est optionnel (glyphs).

## Configuration (`plugins/IvalonaBVN/config.yml`)
Durée, montant, messages. Les messages acceptent les couleurs Minecraft (`&b`, `&c`, `&l`, `&#ff8800`...),
le MiniMessage et les glyphs Nexo (`<glyph:dollars1>`), mélangeables. Un message qui commence par `&` doit être entre guillemets. `/bvn reload` recharge la config (permission `bvn.admin`).

## Permissions
- `bvn.use` (par défaut : tout le monde)
- `bvn.admin` (par défaut : op)

## Grade dans le message de bienvenue
`%sender%` (pseudo de celui qui fait /bvn), `%prefix%` et `%suffix%` (son grade, lu via Vault : LuckPerms doit être relié à Vault) sont utilisables dans le message `welcome`.
