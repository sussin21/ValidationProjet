package com.example.app.controllers;

import com.example.app.dao.CommandeDAO;
import com.example.app.entities.Commande;
import com.example.app.entities.Produit;
import com.example.app.services.ProduitService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopBackendController extends BaseController {

    @FXML private TableView<Produit> productTable;
    @FXML private TableColumn<Produit, Number> colProductId;
    @FXML private TableColumn<Produit, String> colProductName;
    @FXML private TableColumn<Produit, String> colProductType;
    @FXML private TableColumn<Produit, Number> colProductPrice;
    @FXML private TableColumn<Produit, Number> colProductStock;
    @FXML private TableColumn<Produit, String> colProductDesc;

    @FXML private TextField productSearchField;
    @FXML private ComboBox<String> productSortCombo;
    @FXML private TextField productNameField;
    @FXML private TextField productTypeField;
    @FXML private TextField productPriceField;
    @FXML private TextField productStockField;
    @FXML private TextArea productDescField;

    @FXML private TableView<Commande> orderTable;
    @FXML private TableColumn<Commande, Number> colOrderId;
    @FXML private TableColumn<Commande, String> colOrderRef;
    @FXML private TableColumn<Commande, String> colOrderBuyer;
    @FXML private TableColumn<Commande, String> colOrderProduct;
    @FXML private TableColumn<Commande, Number> colOrderQty;
    @FXML private TableColumn<Commande, Number> colOrderTotal;
    @FXML private TableColumn<Commande, String> colOrderState;

    @FXML private TextField orderSearchField;
    @FXML private ComboBox<String> orderSortCombo;
    @FXML private TextField orderBuyerField;
    @FXML private ComboBox<Produit> orderProductCombo;
    @FXML private TextField orderQtyField;
    @FXML private ComboBox<String> orderStateCombo;
    @FXML private TextField orderTotalField;

    private final ProduitService produitService = new ProduitService();
    private final CommandeDAO commandeDAO = new CommandeDAO();

    private final ObservableList<Produit> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Commande> allOrders = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTables();
        setupCombos();
        setupSelectionBinding();

        reloadProducts();
        reloadOrders();
    }

    private void setupTables() {
        colProductId.setCellValueFactory(c -> c.getValue().idProperty());
        colProductName.setCellValueFactory(c -> c.getValue().nomProperty());
        colProductType.setCellValueFactory(c -> c.getValue().typeProperty());
        colProductPrice.setCellValueFactory(c -> c.getValue().prixProperty());
        colProductStock.setCellValueFactory(c -> c.getValue().quantiteDisponibleProperty());
        colProductDesc.setCellValueFactory(c -> c.getValue().descriptionProperty());

        colOrderId.setCellValueFactory(c -> c.getValue().idProperty());
        colOrderRef.setCellValueFactory(c -> c.getValue().referenceCommandeProperty());
        colOrderBuyer.setCellValueFactory(c -> c.getValue().acheteurProperty());
        colOrderProduct.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduit() != null && c.getValue().getProduit().getNom() != null
                        ? c.getValue().getProduit().getNom()
                        : ""
        ));
        colOrderQty.setCellValueFactory(c -> c.getValue().quantiteProperty());
        colOrderTotal.setCellValueFactory(c -> c.getValue().prixTotalProperty());
        colOrderState.setCellValueFactory(c -> c.getValue().etatProperty());
    }

    private void setupCombos() {
        productSortCombo.setItems(FXCollections.observableArrayList(
                "Récent", "Nom (A-Z)", "Prix ↑", "Prix ↓", "Stock ↓"
        ));
        productSortCombo.setValue("Récent");

        orderSortCombo.setItems(FXCollections.observableArrayList(
                "Récent", "Référence", "Montant ↑", "Montant ↓", "Quantité ↓"
        ));
        orderSortCombo.setValue("Récent");

        orderStateCombo.setItems(FXCollections.observableArrayList(
                "EN_ATTENTE", "CONFIRMEE", "EXPEDIEE", "ANNULEE"
        ));
        orderStateCombo.setValue("EN_ATTENTE");

        orderProductCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produit produit) {
                return produit == null ? "" : produit.getNom() + " (stock: " + produit.getQuantiteDisponible() + ")";
            }

            @Override
            public Produit fromString(String string) {
                return null;
            }
        });
    }

    private void setupSelectionBinding() {
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;
            productNameField.setText(selected.getNom());
            productTypeField.setText(selected.getType());
            productPriceField.setText(String.valueOf(selected.getPrix()));
            productStockField.setText(String.valueOf(selected.getQuantiteDisponible()));
            productDescField.setText(selected.getDescription());
        });

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) return;
            orderBuyerField.setText(selected.getAcheteur());
            orderQtyField.setText(String.valueOf(selected.getQuantite()));
            orderStateCombo.setValue(selected.getEtat());
            orderTotalField.setText(String.valueOf(selected.getPrixTotal()));

            if (selected.getProduit() != null) {
                Produit selectedProduct = allProducts.stream()
                        .filter(p -> p.getId() == selected.getProduit().getId())
                        .findFirst().orElse(null);
                orderProductCombo.setValue(selectedProduct);
            }
        });
    }

    private void reloadProducts() {
        try {
            allProducts.setAll(produitService.select());
            orderProductCombo.setItems(FXCollections.observableArrayList(allProducts));
            applyProductFilterSort();
        } catch (SQLException e) {
            showError("Erreur produits", e.getMessage());
        }
    }

    private void reloadOrders() {
        try {
            allOrders.setAll(commandeDAO.select());
            applyOrderFilterSort();
        } catch (SQLException e) {
            showError("Erreur commandes", e.getMessage());
        }
    }

    @FXML
    private void onProductFilterChanged() {
        applyProductFilterSort();
    }

    private void applyProductFilterSort() {
        String query = normalize(productSearchField.getText());
        String sort = productSortCombo.getValue();

        List<Produit> filtered = allProducts.stream()
                .filter(p -> {
                    if (query.isEmpty()) return true;
                    return normalize(p.getNom()).contains(query) || normalize(p.getType()).contains(query);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<Produit> comparator = Comparator.comparingInt(Produit::getId).reversed();
        if ("Nom (A-Z)".equals(sort)) comparator = Comparator.comparing(p -> normalize(p.getNom()));
        if ("Prix ↑".equals(sort)) comparator = Comparator.comparingDouble(Produit::getPrix);
        if ("Prix ↓".equals(sort)) comparator = Comparator.comparingDouble(Produit::getPrix).reversed();
        if ("Stock ↓".equals(sort)) comparator = Comparator.comparingInt(Produit::getQuantiteDisponible).reversed();

        filtered.sort(comparator);
        productTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onOrderFilterChanged() {
        applyOrderFilterSort();
    }

    private void applyOrderFilterSort() {
        String query = normalize(orderSearchField.getText());
        String sort = orderSortCombo.getValue();

        List<Commande> filtered = allOrders.stream()
                .filter(c -> {
                    if (query.isEmpty()) return true;
                    String productName = c.getProduit() != null ? c.getProduit().getNom() : "";
                    return normalize(c.getReferenceCommande()).contains(query)
                            || normalize(c.getAcheteur()).contains(query)
                            || normalize(c.getEtat()).contains(query)
                            || normalize(productName).contains(query);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<Commande> comparator = Comparator.comparingInt(Commande::getId).reversed();
        if ("Référence".equals(sort)) comparator = Comparator.comparing(c -> normalize(c.getReferenceCommande()));
        if ("Montant ↑".equals(sort)) comparator = Comparator.comparingDouble(Commande::getPrixTotal);
        if ("Montant ↓".equals(sort)) comparator = Comparator.comparingDouble(Commande::getPrixTotal).reversed();
        if ("Quantité ↓".equals(sort)) comparator = Comparator.comparingInt(Commande::getQuantite).reversed();

        filtered.sort(comparator);
        orderTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void createProduct() {
        try {
            Produit produit = buildProductFromForm(null);
            produitService.add(produit);
            clearProductForm();
            reloadProducts();
            showAlert("Succès", "Produit ajouté.");
        } catch (Exception e) {
            showError("Saisie produit", e.getMessage());
        }
    }

    @FXML
    private void updateProduct() {
        Produit selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Produit", "Sélectionne un produit à modifier.");
            return;
        }

        try {
            Produit produit = buildProductFromForm(selected.getId());
            produitService.update(produit);
            clearProductForm();
            reloadProducts();
            reloadOrders();
            showAlert("Succès", "Produit modifié.");
        } catch (Exception e) {
            showError("Saisie produit", e.getMessage());
        }
    }

    @FXML
    private void deleteProduct() {
        Produit selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Produit", "Sélectionne un produit à supprimer.");
            return;
        }

        try {
            produitService.delete(selected.getId());
            clearProductForm();
            reloadProducts();
            reloadOrders();
            showAlert("Succès", "Produit supprimé.");
        } catch (Exception e) {
            showError("Suppression produit", e.getMessage());
        }
    }

    @FXML
    private void clearProductForm() {
        productTable.getSelectionModel().clearSelection();
        productNameField.clear();
        productTypeField.clear();
        productPriceField.clear();
        productStockField.clear();
        productDescField.clear();
    }

    private Produit buildProductFromForm(Integer id) {
        String name = safe(productNameField.getText());
        String type = safe(productTypeField.getText());
        String desc = safe(productDescField.getText());

        if (name.isBlank()) throw new IllegalArgumentException("Nom produit obligatoire.");
        if (type.isBlank()) throw new IllegalArgumentException("Type produit obligatoire.");

        double prix;
        int stock;
        try {
            prix = Double.parseDouble(safe(productPriceField.getText()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Prix invalide.");
        }
        try {
            stock = Integer.parseInt(safe(productStockField.getText()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Stock invalide.");
        }

        if (prix < 0) throw new IllegalArgumentException("Prix >= 0 requis.");
        if (stock < 0) throw new IllegalArgumentException("Stock >= 0 requis.");

        Produit p = new Produit();
        if (id != null) p.setId(id);
        p.setNom(name);
        p.setType(type);
        p.setDescription(desc);
        p.setPrix(prix);
        p.setQuantiteDisponible(stock);
        return p;
    }

    @FXML
    private void createOrder() {
        try {
            Commande c = buildOrderFromForm(null);
            commandeDAO.add(c);
            clearOrderForm();
            reloadOrders();
            showAlert("Succès", "Commande ajoutée.");
        } catch (Exception e) {
            showError("Saisie commande", e.getMessage());
        }
    }

    @FXML
    private void updateOrder() {
        Commande selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Commande", "Sélectionne une commande à modifier.");
            return;
        }

        try {
            Commande c = buildOrderFromForm(selected.getId());
            c.setReferenceCommande(selected.getReferenceCommande());
            commandeDAO.update(c);
            clearOrderForm();
            reloadOrders();
            showAlert("Succès", "Commande modifiée.");
        } catch (Exception e) {
            showError("Saisie commande", e.getMessage());
        }
    }

    @FXML
    private void deleteOrder() {
        Commande selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Commande", "Sélectionne une commande à supprimer.");
            return;
        }

        try {
            commandeDAO.delete(selected.getId());
            clearOrderForm();
            reloadOrders();
            showAlert("Succès", "Commande supprimée.");
        } catch (Exception e) {
            showError("Suppression commande", e.getMessage());
        }
    }

    @FXML
    private void clearOrderForm() {
        orderTable.getSelectionModel().clearSelection();
        orderBuyerField.clear();
        orderQtyField.clear();
        orderTotalField.clear();
        orderProductCombo.setValue(null);
        orderStateCombo.setValue("EN_ATTENTE");
    }

    private Commande buildOrderFromForm(Integer id) {
        String buyer = safe(orderBuyerField.getText());
        Produit product = orderProductCombo.getValue();
        String state = orderStateCombo.getValue();

        if (buyer.isBlank()) throw new IllegalArgumentException("Acheteur obligatoire.");
        if (product == null) throw new IllegalArgumentException("Produit obligatoire.");
        if (state == null || state.isBlank()) throw new IllegalArgumentException("État obligatoire.");

        int qty;
        try {
            qty = Integer.parseInt(safe(orderQtyField.getText()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Quantité invalide.");
        }
        if (qty <= 0) throw new IllegalArgumentException("Quantité > 0 requise.");

        double total;
        String totalRaw = safe(orderTotalField.getText());
        if (totalRaw.isBlank()) {
            total = product.getPrix() * qty;
        } else {
            try {
                total = Double.parseDouble(totalRaw);
            } catch (Exception e) {
                throw new IllegalArgumentException("Prix total invalide.");
            }
        }
        if (total < 0) throw new IllegalArgumentException("Prix total >= 0 requis.");

        Commande c = new Commande();
        if (id != null) c.setId(id);
        c.setAcheteur(buyer);
        c.setProduit(product);
        c.setQuantite(qty);
        c.setEtat(state);
        c.setPrixTotal(total);
        c.setDateCommande(LocalDateTime.now());
        if (id == null) {
            c.setReferenceCommande("ADM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        }
        return c;
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @FXML
    private void goBackShop() {
        navigateTo("/shop");
    }
}
