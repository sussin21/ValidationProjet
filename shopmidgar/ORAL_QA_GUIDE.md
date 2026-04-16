# Guide Oral — Midgar JavaFX (Questions Prof + Réponses)

Ce document est une fiche de révision rapide pour l’oral.

---

## 1) Définitions clés (ce que ton ami t’a donné)

## Maven
- **C’est quoi ?** Un outil d’automatisation de build pour projets Java.
- **Rôle principal** : gérer dépendances, compilation, packaging, exécution de tâches.
- **Fichier central** : `pom.xml`.

### Phases Maven importantes
- `clean` : supprime anciens builds (`target/`).
- `compile` : compile le code source.
- `test` : exécute les tests.
- `package` : génère un JAR/WAR.
- `install` : installe l’artefact en local Maven repo.

Exemple que tu peux dire :
> "J’utilise Maven pour gérer JavaFX et MySQL via le `pom.xml`, puis je lance `mvnw.cmd -DskipTests compile` ou `mvnw.cmd javafx:run`."

---

## Stage / Scene (JavaFX)
- **Stage** = la fenêtre de l’application.
- **Scene** = le contenu affiché dans la fenêtre (layout + controls).
- Dans ce projet, le changement d’écrans se fait via `SceneManager`.

---

## ObservableList
- Liste JavaFX **observable** : l’UI se met à jour automatiquement quand la liste change.
- Très utilisée pour `TableView`, `ListView`, `ComboBox`.

Exemple court :
> "Quand je change les éléments d’une `ObservableList`, la table se rafraîchit sans recharger toute la vue."

---

## Lambda (`->`)
- Ce n’est **pas** une abréviation, c’est la syntaxe des expressions lambda Java.
- Sert à écrire des callbacks plus courts.

Exemple :
```java
button.setOnAction(e -> navigateTo("/shop"));
```

---

## `pom.xml`
- Fichier de configuration Maven.
- Contient :
  - dépendances (`javafx-controls`, `javafx-fxml`, `mysql-connector`),
  - plugins (`maven-compiler-plugin`, `javafx-maven-plugin`),
  - version Java.

---

## JDBC (pas "gdbc")
- **JDBC = Java Database Connectivity**.
- API Java standard pour communiquer avec base SQL (MySQL ici).
- Objets clés : `Connection`, `PreparedStatement`, `ResultSet`.

Exemple oral :
> "J’utilise JDBC avec des requêtes préparées (`PreparedStatement`) pour éviter les erreurs et limiter les risques d’injection SQL."

---

## 2) Architecture du projet (à expliquer vite)

Le projet suit une structure proche MVC :
- **View (FXML)** : interface graphique.
- **Controller** : logique UI et navigation.
- **Service/DAO** : logique métier + accès base.
- **Entity** : modèles de données (`Produit`, `Commande`, etc.).
- **Utils** : helpers (navigation, DB, session, panier).

---

## 3) “Où est-ce dans le code ?” (très demandée en oral)

## Point d’entrée app
- `src/main/java/com/example/app/MainApp.java`

## Navigation entre pages
- `src/main/java/com/example/app/utils/SceneManager.java`

## Connexion base de données
- `src/main/java/com/example/app/utils/MyDatabase.java`

## Gestion panier global
- `src/main/java/com/example/app/utils/CartService.java`

## Boutique front (liste produits, recherche, tri, ajout panier)
- `src/main/java/com/example/app/controllers/ShopController.java`
- Vue : `src/main/resources/com/monapp/view/shop/index.fxml`

## Panier
- `src/main/java/com/example/app/controllers/ShopCartController.java`
- Vue : `src/main/resources/com/monapp/view/shop/cart.fxml`

## Checkout / commande
- `src/main/java/com/example/app/controllers/ShopCheckoutController.java`
- Vue : `src/main/resources/com/monapp/view/shop/checkout.fxml`

## Backend shop (CRUD produits + CRUD commandes)
- `src/main/java/com/example/app/controllers/ShopBackendController.java`
- Vue : `src/main/resources/com/monapp/view/shop/backend.fxml`

## Liste des commandes (front)
- `src/main/java/com/example/app/controllers/ShopOrdersController.java`
- Vue : `src/main/resources/com/monapp/view/shop/commandes/list.fxml`

## DAO Produits
- `src/main/java/com/example/app/dao/ProduitDAO.java`

## DAO Commandes
- `src/main/java/com/example/app/dao/CommandeDAO.java`

## Service Produits
- `src/main/java/com/example/app/services/ProduitService.java`

## Navbar partagée
- `src/main/resources/com/monapp/view/partials/header.fxml`
- Contrôleur : `src/main/java/com/example/app/controllers/HeaderController.java`

---

## 4) Questions probables + réponses prêtes

## Q: Pourquoi Maven ?
R: "Pour standardiser le build et gérer automatiquement les dépendances JavaFX/MySQL via `pom.xml`."

## Q: Pourquoi JavaFX ?
R: "Parce qu’on a un projet desktop avec UI riche, navigation multi-vues et binding simple avec les données."

## Q: Comment vous gérez la navigation ?
R: "Avec un `SceneManager` central qui mappe route logique → fichier FXML."

## Q: Où est la logique CRUD produits ?
R: "Dans `ShopBackendController` pour l’UI et `ProduitService/ProduitDAO` pour les opérations DB."

## Q: Où est le contrôle de saisie ?
R: "Dans les contrôleurs (validation de champs avant insert/update), par exemple dans `ShopBackendController` et `ShopCheckoutController`."

## Q: Comment vous faites recherche/tri ?
R: "On filtre et trie les listes en mémoire pour l’affichage (`ObservableList`), et côté produits on a aussi des méthodes de recherche en DAO."

## Q: Comment le stock est mis à jour ?
R: "Quand on ajoute au panier, on décrémente côté UI puis on persiste en base via `ProduitService.update`."

## Q: Pourquoi DAO + Service ?
R: "Pour séparer accès DB (DAO) et logique métier (Service), ce qui rend le code plus maintenable."

## Q: C’est quoi la différence entre `PreparedStatement` et `Statement` ?
R: "`PreparedStatement` paramètre la requête proprement, évite concaténation de strings, plus sûr et plus propre."

## Q: Où configurer JavaFX runtime ?
R: "Soit avec `javafx:run` via Maven, soit en config Run IntelliJ avec `--module-path ... --add-modules ...`."

---

## 5) Mini pitch de 30 secondes (si on te demande “résume ton travail”)

> "On a migré la partie shop web vers JavaFX desktop. J’ai implémenté le front shop avec recherche/tri, panier, checkout, commandes, et un backend dédié pour CRUD produits/commandes avec validation. Techniquement, on utilise Maven, JavaFX FXML, architecture Controller/Service/DAO, JDBC MySQL, et navigation centralisée via `SceneManager`."

---

## 6) Conseils oral (important)

- Réponds **court puis précis**.
- Quand il demande "où ?", donne **fichier exact** + rôle en une phrase.
- Si trou de mémoire : reviens à la logique "UI (FXML) / Controller / Service / DAO / DB".
- Corrige proprement le terme : **JDBC** (pas GDBC).

Bonne chance 🔥
