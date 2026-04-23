# 📋 Questions & Réponses - Examen Oral

## **PARTIE 1: AI CHATBOT**

### Q1: Expliquez le fonctionnement du chatbot que vous avez implémenté ?
**R:** Le chatbot est basé sur l'API Google Gemini 2.5 Flash. Voici le workflow:
1. L'utilisateur pose une question dans l'interface boutique
2. Le système collecte le contexte: nom client, numéro commande, type de problème
3. ChatbotService construit un prompt enrichi avec le catalogue de produits depuis la DB
4. Envoi HTTP vers l'API Gemini avec timeout 30 secondes
5. Parsing JSON de la réponse via regex
6. Affichage de la réponse en temps réel via Task JavaFX asynchrone

### Q2: Quelle technologie d'IA avez-vous utilisée et pourquoi ?
**R:** Google Gemini 2.5 Flash car:
- **Rapide**: Réponses < 1 seconde pour support client
- **Économique**: Model gratuit avec limite généreuse
- **Fiable**: 99.9% uptime Google infrastructure
- **Context-aware**: Peut utiliser le catalogue produits pour réponses précises
- **Alternative locale**: Fallback rule-based si API indisponible

### Q3: Comment gérez-vous les erreurs et timeouts du chatbot ?
**R:** 
- Try-catch sur HttpClient avec timeout 30s
- Si API échoue → fallback rule-based (regex patterns pour FAQ)
- Si timeout → message "Service indisponible, essayez plus tard"
- Logging complet dans console pour debug
- Queue asynchrone pour ne pas bloquer l'UI

### Q4: Avez-vous implémenté l'historique de conversation ?
**R:** Non, pour cette itération. C'est une limitation connue. Améliorations futures:
- Stocker messages dans DB avec userId + timestamp
- Implémenter context sliding window (derniers 5 messages)
- Ajouter sentiment analysis pour détection problèmes critiques

### Q5: Comment le chatbot accède aux données produits ?
**R:** Via ProduitService qui requête la DB MySQL. Le prompt inclut:
```
Catalogue disponible:
- [Nom]: Prix €X, Stock: Y
- ...
```
Cela permet au chatbot de faire des recommandations précises.

---

## **PARTIE 2: PRÉDICTION DE STOCK (RISK PREDICTION)**

### Q6: Expliquez l'algorithme de prédiction de stock que vous utilisez ?
**R:** **Régression Linéaire** sur données historiques 30 jours:
- Collecte: Derniers 30 jours de ventes par produit
- Formule: `y = mx + b` (slope + intercept)
- Calcul m: $m = \frac{n\sum xy - \sum x \sum y}{n\sum x^2 - (\sum x)^2}$
- Calcul b: $b = \frac{\sum y - m\sum x}{n}$
- **Prédiction**: `Demande_demain = m(jour_31) + b`
- **Stock recommandé**: Prédiction × 14 jours (buffer)

### Q7: Comment calculez-vous le score de confiance ?
**R:** Score basé sur 3 pénalités:

1. **Volatilité** (variance des données):
   - Formule: `√variance × 5` (max 40%)
   - Ventes très variables = moins fiable

2. **Données insuffisantes**:
   - Si < 5 jours ventes: -25%
   - Pas assez d'historique pour bon fitting

3. **Demande nulle/négative**:
   - Si prédiction ≤ 0: -20%
   - Produit probablement discontinué

**Score final**: `100 - Pénalités` (min 5%, max 100%)

Exemple:
- Volatilité: 30%, Données: 10%, Demande: 0% → Score = **60%**

### Q8: Comment classifiez-vous le risque de stock ?
**R:** 3 niveaux basés sur stock actuel vs recommandé:

| Niveau | Condition | Action |
|--------|-----------|--------|
| **CRITIQUE** 🔴 | Stock ≤ 3 | Alerte immédiate |
| **FAIBLE** 🟡 | 3 < Stock ≤ 10 | Surveillance |
| **SAIN** 🟢 | Stock > 10 | Normal |

Exemple: Produit XYZ
- Stock actuel: 5
- Prédiction jour 31: 8
- Recommandé: 8 × 14 = 112
- **Risque**: FAIBLE (mais acheter 100+ unités)

### Q9: Comment les prédictions sont-elles stockées et mises à jour ?
**R:** Table `stock_prediction` MySQL:
```sql
CREATE TABLE stock_prediction (
    id INT PRIMARY KEY,
    product_id INT,
    predicted_demand FLOAT,
    recommended_stock INT,
    confidence_score INT,
    risk_level VARCHAR(50),
    created_at TIMESTAMP
);
```
- Mises à jour: Quotidiennes (via bouton "Calculer prédictions")
- Historique: Conservé pour trend analysis
- Retention: 90 jours glissants

### Q10: Que se passe-t-il si les données historiques sont insuffisantes ?
**R:** 
- Si < 5 jours: Pénalité -25% sur confiance
- Si 0 ventes: Prédiction = 0, risque CRITIQUE
- Recommandation: Commencer avec stock minimal (10 unités)
- Attendre 30 jours avant bonnes prédictions

### Q11: Pourquoi Régression Linéaire et pas Machine Learning avancé ?
**R:** 
**Avantages RL:**
- Simple à implémenter et comprendre
- Pas dépendance librairie ML externe
- Rapide calcul (O(n) complexity)
- Interprétabilité: On voit m et b

**Limitations:**
- Suppose tendance linéaire (peut être exponentielle/cyclique)
- Sensible aux outliers
- Pas capture saisonnalité

**Améliorations futures:**
- Polynomial Regression
- Prophet (Facebook) pour saisonnalité
- LSTM Neural Networks

---

## **PARTIE 3: AUTOMATISATION**

### Q12: Décrivez le système d'automatisation que vous avez implémenté ?
**R:** Deux types d'événements automatisés:

**Type 1: STOCK_ALERT**
- Déclenche quand stock < 10
- Description: "Stock faible pour [Produit]"
- Génère notification automatique

**Type 2: RESTOCK**
- Déclenche si recommandé_stock > stock_actuel
- Génère commande d'achat suggestion
- Priorité basée sur urgence

### Q13: Comment gérez-vous la génération des alertes ?
**R:** Via `ShopAutomationEventService.generateAlerts()`:
1. Requête DB: `SELECT * FROM produits WHERE stock < 10`
2. Pour chaque produit:
   - Créer ShopAutomationEvent (type STOCK_ALERT)
   - Déterminer sévérité (CRITIQUE si ≤3)
   - Insérer en DB
3. Afficher dans ListView (max 20 derniers)
4. Horodatage: Timestamp auto MySQL

### Q14: Les alertes sont-elles envoyées par mail automatiquement ?
**R:** Pas vraiment "automatique" encore. Workflow actuel:
1. Générer alertes (UI button)
2. Admin révise dans ListView
3. Admin clique "Préparer mail alerte"
4. Pré-remplissage template mail
5. Admin clique "Envoyer" (MailService)

**Improvement**: Cron job MySQL pour auto-trigger nuit

### Q15: Comment intégrez-vous les prédictions dans l'automatisation ?
**R:** 
```
Pour chaque produit:
IF prédiction.stock_recommandé > produit.stock_actuel THEN
    CREATE ShopAutomationEvent (type=RESTOCK)
    SET quantité_suggérée = recommandé - actuel
    SET priorité = HIGH si risque==CRITIQUE
    INSERT notification
END IF
```

Cela crée automatiquement suggestions de réapprovisionnement intelligentes.

### Q16: Avez-vous implémenté des règles métier complexes ?
**R:** Oui, les alertes considèrent:
- **Stock critique**: Alerte rouge
- **Trend négatif**: Si pente < 0 (régression)
- **Delai livraison**: Optionnel (not yet)
- **Saisonnalité**: Basique (28 jours de buffer)

Exemple règle:
```
IF stock < 5 AND trend_slope < -1 THEN
    priority = "URGENT"
    email_to = ["manager@shop.com", "warehouse@shop.com"]
END IF
```

---

## **PARTIE 4: INTÉGRATION GLOBALE**

### Q17: Comment les 3 features (Chat, Prédiction, Automation) travaillent ensemble ?
**R:** Architecture en couches:

```
┌─────────────────────────────────────┐
│      JavaFX UI (TabPane)            │
├─────────────────────────────────────┤
│    ShopBackendController            │
│  - Handles UI events                │
│  - Orchestrates services            │
├─────────────────────────────────────┤
│    Service Layer                    │
│  - ChatbotService                   │
│  - StockPredictionService           │
│  - ShopAutomationEventService       │
│  - ShopAnalyticsService             │
├─────────────────────────────────────┤
│    DAO Layer (Data Access)          │
│  - ProduitDAO                       │
│  - StockPredictionDAO               │
│  - ShopAutomationEventDAO           │
├─────────────────────────────────────┤
│    MySQL Database                   │
│  - produits                         │
│  - stock_prediction                 │
│  - shop_automation_event            │
│  - commandes                        │
└─────────────────────────────────────┘
```

**Flux workflow:**
1. Admin click "Calculer prédictions"
2. StockPredictionService → linear regression sur sales history
3. Résultats → stock_prediction table
4. ShopAutomationEventService lit les prédictions
5. Génère alertes/restock events
6. UI affiche dans ListView
7. Si client click chatbot → ChatbotService contextualise avec ces données

### Q18: Comment assurez-vous la performance avec ces features?
**R:** 
- **Async/Threading**: Toutes opérations longues en JavaFX Task
- **Caching**: Produits loadés en mémoire au startup
- **Indexing DB**: stock_prediction indexed sur product_id
- **Pagination**: ListView 20 derniers events (pas tous)
- **API Timeout**: 30 secondes max pour Gemini

### Q19: Quels tests avez-vous effectués ?
**R:**
- Prédictions: Comparé réel vs prédit sur produits connus
- Chatbot: Requêtes multiples simultanées → vérifier async
- Alertes: Baissé stock produits → vérifier trigger
- Edge cases: 0 ventes, 1 vente, 100+ ventes
- Performance: 1000 prédictions en < 5 sec

### Q20: Quels sont les limitations et améliorations futures ?
**R:**

**Limitations actuelles:**
- Pas historique chatbot (pas IA "stateful")
- Prédictions linéaires (ignorent saisonnalité)
- Alertes manuelles (pas cron job)
- Pas ML models (simple regression)
- API Gemini dépendance externe

**Améliorations futures:**
1. **Chatbot V2**: Historique + memory + sentiment analysis
2. **Prédictions V2**: Prophet, LSTM, seasonal decomposition
3. **Automation V2**: Scheduled jobs, webhook integrations
4. **Analytics V2**: Real-time dashboard, WebSocket updates
5. **Multi-language**: i18n support pour support international
6. **Offline mode**: SQLite fallback si API down

---

## **PARTIE 5: ARCHITECTURE & DESIGN PATTERNS**

### Q21: Quels design patterns avez-vous utilisés ?
**R:**

1. **Service Pattern**: ChatbotService, StockPredictionService (business logic isolation)
2. **DAO Pattern**: Accès DB abstrait
3. **Singleton**: MyDatabase (une seule connexion)
4. **Strategy**: Différentes stratégies d'alertes (STOCK_ALERT vs RESTOCK)
5. **Observer/MVC**: JavaFX bindings automatiques
6. **Factory**: Services créés en contrôleur

### Q22: Comment gérez-vous les exceptions ?
**R:** Hiérarchie:
```
SQLException → try-catch → showError dialog
HttpClient exception → fallback + logging
JSON parsing error → default response
Timeout → retry logic ou fallback
```

Principe: Ne jamais crash l'app, toujours fallback gracieux.

### Q23: Comment avez-vous documenté votre code ?
**R:**
- Javadoc comments sur méthodes publiques
- Inline comments sur logique complexe (régression)
- README avec architecture diagram
- TECHNICAL_FEATURES_SUMMARY.md détaillé

---

## **PARTIE 6: REQUÊTE DB & PERFORMANCE**

### Q24: Écrivez la requête pour les alertes de stock ?
**R:**
```sql
SELECT p.id, p.name, p.stock, sp.recommended_stock, sp.confidence_score
FROM produits p
LEFT JOIN stock_prediction sp ON p.id = sp.product_id
WHERE p.stock < 10
ORDER BY p.stock ASC, sp.confidence_score DESC
LIMIT 20;
```

### Q25: Comment optimiseriez-vous les prédictions pour 10000 produits ?
**R:**
- Index sur product_id + created_at
- Batch processing (500 produits par lot)
- ExecutorService thread pool (4 threads)
- Cache à 1 heure
- Requête historical data en async
- Prédictions en background thread nuit

```java
// Pseudo-code
ExecutorService pool = Executors.newFixedThreadPool(4);
List<Future<StockPrediction>> futures = new ArrayList<>();
for (Produit p : produits) {
    futures.add(pool.submit(() -> predictStock(p)));
}
```

---

## **PARTIE 7: SÉCURITÉ**

### Q26: Comment sécurisez-vous l'API Gemini ?
**R:**
- API key en variable d'environnement (jamais hardcoded)
- Token refresh automatique
- Rate limiting (max 10 req/min)
- HTTPS only pour API calls

### Q27: Avez-vous validé les inputs utilisateur ?
**R:**
- Chatbot query: Trim + max 500 chars
- Produit création: Regex validation nom/type
- Stock: Integer only, > 0
- Prix: Double, 2 decimals
- Pas injection SQL (PreparedStatement partout)

---

## **TIPS POUR L'EXAMEN**

### À mémoriser:
✅ **Linear Regression formula**: m = Σ(xy) / Σ(x²)
✅ **3 confidence penalties**: Volatility, Data, Demand
✅ **3 risk levels**: Critique (≤3), Faible (3-10), Sain (>10)
✅ **Gemini API**: Google model utilisé, 30s timeout
✅ **Database tables**: stock_prediction, shop_automation_event
✅ **Design patterns**: Singleton, Service, DAO, Observer

### À préparer:
🎯 Diagrammes: Architecture, DB schema, class diagram
🎯 Exemples: Avec nombres réels (ex: produit avec 100€ ventes, prédiction 120€)
🎯 Code: Pouvoir montrer StockPredictionService.java
🎯 Logs: Avoir des captures d'alertes générées
🎯 Demo: Pouvoir générer une prédiction en live

### Questions piège possibles:
❓ "Pourquoi pas TensorFlow pour ML?" → Trop lourd, régression suffisante
❓ "Comment gérer données manquantes?" → Pénalité confiance, 5 jours minimum
❓ "Scalabilité 1M produits?" → Thread pool, batch processing, caching
❓ "Que se passe si API échoue?" → Fallback rule-based, logging

---

**Bonne chance! 🚀**
