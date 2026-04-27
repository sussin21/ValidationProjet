# 📋 Questions & Réponses - Examen Oral
**Examen pratique des 3 features: Chatbot IA, Prédiction Stock, Automatisation**

---

## 🤖 **AI CHATBOT - Questions Techniques**

### Q1: Où et comment est stockée la clé API Gemini ?
**R:** 
```java
// ChatbotService.java ligne 157
private String getApiKey() {
    return System.getenv("GEMINI_API_KEY");
}
```
- Stockée en **variable d'environnement** `GEMINI_API_KEY`
- Jamais hardcodée dans le code (sécurité)
- Configuration sur machine locale: `set GEMINI_API_KEY=sk_live_xxx`

### Q2: Décrivez l'architecture du chat avec Gemini ?
**R:**
```
Client Query (String)
        ↓
ChatbotService.chat(message, customerInfo)
        ↓
buildPrompt() → Contexte client + 5 produits top
        ↓
HttpRequest POST → https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash
        ↓
Response JSON → extractText() via Regex
        ↓
Result affichée en AsyncTask JavaFX
```

**Timeout**: 30 secondes
**Model**: `gemini-2.5-flash`
**Format requête**: JSON avec temperature=0.7, maxTokens=512

### Q3: Comment fonctionne le fallback si l'API échoue ?
**R:**
```java
// ChatbotService.java ligne 39-41
if (apiKey == null || apiKey.isBlank()) {
    return buildLocalSupportReply(message, customerInfo);
}
```
**Fallback rule-based** avec regex patterns:
- `"commande"` → Vérifiez suivi ou donnez numéro
- `"livraison"` → Indiquez numéro commande
- `"retour"` → Gardez preuve d'achat + contact support
- `"stock"` → Dites nom du produit
- Sinon → Réponse générique

### Q4: Montrez un exemple de prompt envoyé à Gemini ?
**R:**
```
Tu es l'assistant boutique Midgar. Réponds en français, courte, utile et poliment.

Contexte client:
- name: Ahmed
- order_id: 12345

Contexte produits:
- Excalibur | type: épée | prix: 500 | stock: 3
- Shield Dragon | type: bouclier | prix: 200 | stock: 15
- Spell Book | type: livre | prix: 50 | stock: 0

Message utilisateur: Est-ce que l'Excalibur est disponible ?
```---

## 📊 **STOCK PREDICTION - Algos & Formules**

### Q5: Écrivez la formule exacte de la régression linéaire ?
**R:** Pour les points (x₁,y₁) ... (xₙ,yₙ):

$$m = \frac{n\sum xy - \sum x \sum y}{n\sum x^2 - (\sum x)^2}$$

$$b = \frac{\sum y - m\sum x}{n}$$

$$\text{Prédiction}_{j+1} = m \times (n+1) + b$$

Exemple: 5 jours de ventes [10, 12, 15, 14, 18] → pente ≈ 2 → jour 6 prédiction ≈ 20

### Q6: Comment calculez-vous le score de confiance à 5 points ?
**R:** 3 pénalités appliquées (max 100%):

1. **Volatilité**: `variance = Σ(y - moyenne)² / n` → pénalité `√variance × 5` (max 40%)
2. **Données insuffisantes**: Si < 5 jours → pénalité 25%
3. **Demande nulle**: Si prédiction ≤ 0 → pénalité 20%

**Formule finale**: `confiance = clamp(100 - pénalités, 5, 100)`

Exemple: variance=50, données OK, demande positive → pénalité ≈ 35% → confiance = **65%**

### Q7: Comment classifiez-vous les risques ?
**R:** Basé sur `stock_actuel`:
```
SI stock ≤ 3  → CRITIQUE 🔴 (action immédiate)
SI stock ≤ 10 → FAIBLE 🟡 (monitoring)
SINON         → SAIN 🟢 (normal)
```

Exemple: Produit stock=2, recommandé=50 → **CRITIQUE + achat urgent 48 unités**

### Q8: Qu'est-ce qui a été modifié dans le service ?
**R:** Dans `StockPredictionService.java`:
- Ajout JavaDoc complet
- Documentation de l'algorithme de régression
- Classe commentée pour clarté du calcul de confiance
- Clarification des limites du modèle linéaire

---

## 🔄 **AUTOMATISATION - Implémentation Réelle**

### Q9: Avec quoi est construite l'automatisation ?
**R:** 3 composants:

1. **ShopAutomationEventService**
   - Crée événements `ShopAutomationEvent`
   - Deux types: `STOCK_ALERT` et `AUTO_RESTOCK`

2. **ShopAnalyticsService** 
   - `getLowStockProducts(threshold)` → filtre stock ≤ seuil
   - Requête SQL: `SELECT * FROM produit WHERE quantite ≤ threshold`

3. **ProduitService + MySQL**
   - Update stock: `produit.setQuantiteDisponible(5)`
   - `produitService.update(product)`

### Q10: Comment fonctionnel le système d'alertes ?
**R:**
```java
// ShopAutomationEventService.java
public List<ShopAutomationEvent> generateStockAlerts(int threshold) {
    List<Produit> lowStock = analyticsService.getLowStockProducts(threshold);
    for (Produit p : lowStock) {
        createEvent("STOCK_ALERT", 
                    "Stock critique: " + p.getNom() + " (" + p.getQuantite() + ")",
                    "ACTIVE");
    }
}
```
**Seuil défaut**: 10 unités
**Trigger**: Manual (button "Générer alertes")
**Statut**: ACTIVE ou COMPLETED

### Q11: Décrivez le auto-restock à 5 unités ?
**R:**
```java
// Nouvelle méthode implémentée
public List<ShopAutomationEvent> autoRestockZeroStockProducts() {
    List<Produit> all = produitService.select();
    for (Produit p : all) {
        if (p.getQuantiteDisponible() == 0) {
            p.setQuantiteDisponible(5);
            produitService.update(p);  // UPDATE produits SET quantite=5 WHERE id=X
            createEvent("AUTO_RESTOCK", 
                        "AUTO-RESTOCK: " + p.getNom() + " (0→5)",
                        "COMPLETED");
        }
    }
}
```
**Logique**: Stock = 0 → Restock à 5 automatiquement
**Événement**: Tracé dans la table `shop_automation_event`

### Q12: Quelles requêtes SQL sont utilisées ?
**R:**
```sql
-- Alertes stock
SELECT id, nom_produit, quantite_disponible 
FROM produit 
WHERE quantite_disponible <= 10
ORDER BY quantite_disponible ASC;

-- Restock automatique
UPDATE produit 
SET quantite_disponible = 5 
WHERE quantite_disponible = 0;

-- Suivi des événements
SELECT * FROM shop_automation_event 
ORDER BY created_at DESC 
LIMIT 20;
```

### Q13: Quel fichier UI affiche l'automatisation ?
**R:** 
```xml
<!-- backend.fxml - Tab "Rapports & Automatisation" -->
<Tab text="Rapports &amp; Automatisation">
    <ListView fx:id="automationListView" prefHeight="300"/>
    <!-- Affiche derniers 20 événements -->
</Tab>
```
**Contrôleur**: `ShopBackendController.java`
**Méthodes**: 
- `generateAutomationAlerts()` → trigger alerts
- `reloadAutomationEvents()` → ListView update

---

## 📈 **ANALYTICS - Formules & Calculs**

### Q14: Montrez comment les analytics sont calculées ?
**R:** Via `ShopAnalyticsService`:

**1. Ventes 30 jours:**
```sql
SELECT SUM(prix_total) FROM commande 
WHERE DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 30 DAY)
```

**2. Nombre de commandes:**
```sql
SELECT COUNT(*) FROM commande 
WHERE DATE(created_at) >= DATE_SUB(NOW(), INTERVAL 30 DAY)
```

**3. Croissance %:**
$$\text{Croissance} = \frac{\text{Ventes30j} - \text{Ventes30jPrecedents}}{\text{Ventes30jPrecedents}} \times 100$$

**4. Panier moyen:**
$$\text{PanierMoyen} = \frac{\text{VentesTotales30j}}{\text{NbCommandes30j}}$$

### Q15: Comment segmentez-vous les clients ?
**R:**
```java
// ShopAnalyticsService.java ligne 140
Map<String, Integer> segments = countCustomerSegments();
```

**Logique:**
```sql
SELECT acheteur, COUNT(*) as order_count FROM commande GROUP BY acheteur
```

**Segmentation:**
```
VIP:        >= 5 commandes
REGULAR:    >= 2 et < 5 commandes  
OCCASIONAL: 1 commande
```

**Exemple**: Ahmed (7 commandes) → VIP, Jean (2 commandes) → REGULAR

### Q16: Qu'est-ce que le score de risque de stock ?
**R:** Calculé dans `ProductPerformanceRow`:
```
SI quantite <= 3  → "Critique"  🔴
SI quantite <= 10 → "Faible"    🟡
SINON             → "Sain"      🟢
```

**Tableau performance**: Affiche pour chaque produit:
- Demande (ventes derniers 30j)
- Marge estimée (prix × 0.25)
- Risque stock (basé sur quantité)
- Stock actuel

### Q17: Montrez la requête complète pour la performance des produits ?
**R:**
```sql
SELECT p.id, p.nom_produit, p.prix, p.quantite_disponible,
       COALESCE(SUM(c.quantite), 0) as sold_quantity,
       COALESCE(SUM(c.prix_total), 0) as revenue
FROM produit p 
LEFT JOIN commande c ON c.produit_id = p.id
GROUP BY p.id, p.nom_produit, p.prix, p.quantite_disponible
ORDER BY sold_quantity DESC
LIMIT 10;
```
