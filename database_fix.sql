-- ============================================
-- Missing tables for shop functionality
-- ============================================

USE midgar37;

-- Produit table (if not exists)
CREATE TABLE IF NOT EXISTS produit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom_produit VARCHAR(255) NOT NULL,
    description TEXT,
    prix DECIMAL(10,2) NOT NULL,
    type_produit VARCHAR(100),
    quantite_disponible INT DEFAULT 0,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_produit (type_produit)
);

-- Commande table (if not exists)
CREATE TABLE IF NOT EXISTS commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    produit_id INT NOT NULL,
    quantite INT NOT NULL,
    prix_total DECIMAL(10,2) NOT NULL,
    date_commande TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acheteur VARCHAR(255),
    etat VARCHAR(50) DEFAULT 'confirmee',
    reference_commande VARCHAR(100),
    INDEX idx_produit_id (produit_id),
    INDEX idx_date_commande (date_commande),
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
);

-- StockPrediction table (if not exists)
CREATE TABLE IF NOT EXISTS stock_prediction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    predicted_demand DECIMAL(10,2) NOT NULL,
    current_stock INT NOT NULL,
    recommended_stock INT NOT NULL,
    confidence DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at)
);

-- ============================================
-- Sample data for testing (optional)
-- ============================================

INSERT IGNORE INTO produit (nom_produit, description, prix, type_produit, quantite_disponible) VALUES
('Excalibur', 'The legendary sword of legends', 999.99, 'Weapon', 5),
('Mithril Armor', 'Unbreakable armor from ancient times', 749.99, 'Armor', 3),
('Magic Wand', 'A wand for casting powerful spells', 199.99, 'Accessory', 12),
('Invisibility Cloak', 'Become invisible to all', 599.99, 'Accessory', 2),
('Shield of Protection', 'A shield that protects from all harm', 449.99, 'Armor', 8);

INSERT IGNORE INTO commande (produit_id, quantite, prix_total, acheteur, etat, reference_commande) VALUES
(1, 1, 999.99, 'admin', 'livree', 'CMD-001'),
(2, 2, 1499.98, 'user', 'confirmee', 'CMD-002'),
(3, 1, 199.99, 'testuser', 'en attente', 'CMD-003');