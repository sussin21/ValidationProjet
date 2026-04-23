package com.example.app.dao;

import com.example.app.entities.Commande;
import com.example.app.entities.Produit;
import com.example.app.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeDAO implements IDAO<Commande> {

    private Connection connection;

    public CommandeDAO() {
        connection = MyDatabase.getInstance().getConnection();
        try {
            ensureTable();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void add(Commande commande) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        String sql = "INSERT INTO commande (quantite, date_commande, etat, acheteur, prix_total, reference_commande, produit_id) VALUES (?, NOW(), ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, commande.getQuantite());
        ps.setString(2, commande.getEtat());
        ps.setString(3, commande.getAcheteur());
        ps.setDouble(4, commande.getPrixTotal());
        ps.setString(5, commande.getReferenceCommande());
        if (commande.getProduit() != null) {
            ps.setInt(6, commande.getProduit().getId());
        } else {
            ps.setNull(6, Types.INTEGER);
        }
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            commande.setId(rs.getInt(1));
        }
    }

    @Override
    public void update(Commande commande) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        String sql = "UPDATE commande SET quantite = ?, etat = ?, acheteur = ?, prix_total = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, commande.getQuantite());
        ps.setString(2, commande.getEtat());
        ps.setString(3, commande.getAcheteur());
        ps.setDouble(4, commande.getPrixTotal());
        ps.setInt(5, commande.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        String sql = "DELETE FROM commande WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Commande> select() throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom_produit FROM commande c LEFT JOIN produit p ON c.produit_id = p.id ORDER BY c.date_commande DESC";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Commande> findByAcheteur(String acheteur) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commande WHERE acheteur = ? ORDER BY date_commande DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, acheteur);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Commande> findByEtat(String etat) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commande WHERE etat = ? ORDER BY date_commande DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, etat);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public Commande findByReference(String reference) throws SQLException {
        Connection conn = requireConnection();
        ensureTable();
        String sql = "SELECT * FROM commande WHERE reference_commande = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, reference);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSet(rs);
        }
        return null;
    }

    private Commande mapResultSet(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setId(rs.getInt("id"));
        commande.setQuantite(rs.getInt("quantite"));
        commande.setEtat(rs.getString("etat"));
        commande.setAcheteur(rs.getString("acheteur"));
        commande.setPrixTotal(rs.getDouble("prix_total"));
        commande.setReferenceCommande(rs.getString("reference_commande"));

        Produit produit = new Produit();
        produit.setId(rs.getInt("produit_id"));
        try {
            produit.setNom(rs.getString("nom_produit"));
        } catch (SQLException ignored) {
        }
        commande.setProduit(produit);

        return commande;
    }

    private Connection requireConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = MyDatabase.getConnection();
            }
        } catch (SQLException e) {
            connection = null;
        }

        if (connection == null) {
            throw new SQLException("Connexion MySQL indisponible pour les commandes.");
        }

        return connection;
    }

    private void ensureTable() throws SQLException {
        Connection conn = requireConnection();
        String sql = "CREATE TABLE IF NOT EXISTS commande ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "quantite INT NOT NULL,"
                + "date_commande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "etat VARCHAR(50) NOT NULL,"
                + "acheteur VARCHAR(255) NOT NULL,"
                + "prix_total DECIMAL(10,2) NOT NULL,"
                + "reference_commande VARCHAR(100) NOT NULL UNIQUE,"
                + "produit_id INT NULL,"
                + "INDEX idx_commande_produit (produit_id),"
                + "CONSTRAINT fk_commande_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE SET NULL"
                + ")";
        Statement st = conn.createStatement();
        st.execute(sql);
    }
}