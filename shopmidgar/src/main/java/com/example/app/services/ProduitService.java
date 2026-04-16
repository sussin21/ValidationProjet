package com.example.app.services;

import com.example.app.entities.Produit;
import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.example.app.dao.ProduitDAO;

public class ProduitService implements IService<Produit> {

    private final ProduitDAO produitDAO;

    public ProduitService() {
        produitDAO = new ProduitDAO();
    }

    @Override
    public void add(Produit produit) throws SQLException {
        validateProduit(produit);
        produitDAO.add(produit);
    }

    @Override
    public void update(Produit produit) throws SQLException {
        validateProduit(produit);
        produitDAO.update(produit);
    }

    @Override
    public void delete(int id) throws SQLException {
        produitDAO.delete(id);
    }

    @Override
    public List<Produit> select() throws SQLException {
        return produitDAO.select();
    }

    public List<Produit> searchProduits(String search, String type, String sortBy) throws SQLException {
        String cleanSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String cleanType = (type == null || type.trim().isEmpty() || "Tous les types".equalsIgnoreCase(type.trim()))
                ? null
                : type.trim();
        String cleanSort = (sortBy == null || sortBy.trim().isEmpty()) ? "date" : sortBy.trim();

        return produitDAO.searchProduits(cleanSearch, cleanType, cleanSort);
    }

    public List<String> getProductTypes() throws SQLException {
        List<String> types = produitDAO.getProductTypes();
        if (types == null) {
            return Collections.emptyList();
        }
        return types;
    }

    public List<Produit> findByIds(Collection<Integer> ids) throws SQLException {
        return produitDAO.findByIds(ids);
    }

    private void validateProduit(Produit produit) {
        if (produit == null) {
            throw new IllegalArgumentException("Le produit est obligatoire.");
        }

        String nom = produit.getNom() == null ? "" : produit.getNom().trim();
        String type = produit.getType() == null ? "" : produit.getType().trim();

        if (nom.isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire.");
        }
        if (nom.length() > 255) {
            throw new IllegalArgumentException("Le nom du produit ne doit pas dépasser 255 caractères.");
        }
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Le type de produit est obligatoire.");
        }
        if (type.length() > 100) {
            throw new IllegalArgumentException("Le type de produit ne doit pas dépasser 100 caractères.");
        }
        if (Double.isNaN(produit.getPrix()) || produit.getPrix() < 0) {
            throw new IllegalArgumentException("Le prix doit être supérieur ou égal à 0.");
        }
        if (produit.getQuantiteDisponible() < 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure ou égale à 0.");
        }

        produit.setNom(nom);
        produit.setType(type);
    }
}