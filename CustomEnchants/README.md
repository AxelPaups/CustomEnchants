# CustomEnchants — Plugin Paper 1.21.8

40 enchantements custom en français, répartis en 5 raretés, intégrés **nativement** à la table d'enchantement et à l'enclume grâce au registre Paper, avec un panel admin complet.

## Fonctionnalités

- **40 enchantements** : 12 combat, 6 arcs, 10 outils, 10 armures, 2 divers — noms et descriptions en français.
- **5 raretés** : Commun (blanc), Rare (bleu), Épique (violet), Légendaire (or), Mythique (rouge). Le Mythique est introuvable en table : uniquement dans les coffres générés ou via admin.
- **Table d'enchantement et enclume natives** : les enchants apparaissent comme les enchants vanilla, avec poids selon la rareté, conflits et fusion de livres gérés par le jeu.
- **Anti-cumul** : groupes exclusifs (un seul « gros dégât » par épée, un seul comportement de flèche, etc.), maximum 3 enchants custom par item, et coût XP progressif à l'enclume.
- **Panel admin** (`/ce panel`) : activer/désactiver chaque enchant indépendamment (clic gauche), se donner le livre ou l'item déjà enchanté au niveau choisi (clic droit), filtres par catégorie et rareté, tout activer/désactiver, reload.
- **Loot** : ~12 % de chance qu'un coffre généré contienne un livre custom (configurable).

## Obtenir le .jar (via GitHub Actions — rien à installer)

1. Crée un compte gratuit sur [github.com](https://github.com) si tu n'en as pas.
2. Crée un nouveau dépôt (bouton **New repository**), par exemple `CustomEnchants`, en **privé** si tu veux.
3. Sur la page du dépôt : **Add file → Upload files**, puis glisse-dépose **tout le contenu de ce dossier** (y compris les dossiers `src` et `.github`). Clique **Commit changes**.
   - Important : le dossier `.github` doit être uploadé, c'est lui qui contient la recette de compilation. S'il n'apparaît pas dans le glisser-déposer (dossier caché), utilise l'upload par dossier complet depuis l'explorateur.
4. Va dans l'onglet **Actions** du dépôt. Si un bandeau te demande d'activer les workflows, clique **Enable**. Le build `Build CustomEnchants` se lance automatiquement (sinon : bouton **Run workflow**).
5. Quand le build est vert (~2 min), clique dessus puis télécharge l'**artifact** `CustomEnchants` en bas de page. C'est un zip contenant `CustomEnchants-1.0.0.jar`.

## Installation sur le serveur

1. Mets `CustomEnchants-1.0.0.jar` dans le dossier `plugins/` du serveur **Paper 1.21.8**.
2. **Redémarre** le serveur (pas de /reload : le registre s'enregistre au démarrage).
3. C'est tout. Les enchants apparaissent en table et en enclume.

> ⚠️ Requiert Paper (ou un fork : Purpur, Pufferfish). Ne fonctionne pas sur Spigot pur.

## Commandes

| Commande | Permission | Rôle |
|---|---|---|
| `/ce panel` | `customenchants.admin` | Panel admin (toggle + give) |
| `/ce toggle <enchant>` | `customenchants.admin` | Activer/désactiver sans GUI |
| `/ce give <joueur> <enchant> <niveau> [livre\|item]` | `customenchants.admin` | Donner un livre ou un item enchanté |
| `/ce apply <enchant> <niveau>` | `customenchants.admin` | Enchanter l'item en main |
| `/ce info <enchant>` | `customenchants.use` | Détails d'un enchantement |
| `/ce list [rarete]` | `customenchants.use` | Liste des enchantements |
| `/ce reload` | `customenchants.admin` | Recharger la config |

Les permissions non attribuées sont réservées aux **opérateurs** par défaut. Utilise LuckPerms pour les donner à un grade.

## Configuration

- `config.yml` : limite d'enchants custom par item, multiplicateur de coût XP, chance de loot, paramètres de chaque effet (dégâts, chances, cooldowns), préfixe des messages.
- `states.yml` : état activé/désactivé de chaque enchantement (géré par le panel, ne pas éditer à chaud).

## Bon à savoir

- **Désactiver un enchantement** coupe son effet immédiatement et le rend non-obtenable (table, enclume, loot), mais il reste visible sur les items existants des joueurs — leurs items ne cassent jamais.
- Ne supprime jamais un enchantement du code d'une version à l'autre : les items qui le portent deviendraient invalides. Désactive-le à la place.
- L'excavation et le bûcheron passent par l'événement de casse standard : les protections de terrain (WorldGuard, Lands...) sont respectées.
- Sneak + casser = désactive temporairement l'excavation/bûcheron (pratique pour construire).

## Modifier / ajouter un enchantement

1 enchantement = 1 entrée dans `Enchants.java` (nom, description, rareté, cible, conflits) + son effet dans le listener adapté. Le registre, la table, l'enclume, le panel et les commandes le prennent en compte automatiquement.
