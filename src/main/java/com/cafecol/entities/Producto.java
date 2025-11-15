package com.cafecol.entities;

import jakarta.persistence.*;
import jakarta.json.bind.annotation.JsonbTransient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idProducto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false)
    private int stock;

    @JsonbTransient
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
    private List<CarritoItem> carritoItems = new ArrayList<>();

    @JsonbTransient
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detallesPedido = new ArrayList<>();

    // Getters y Setters
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public List<CarritoItem> getCarritoItems() { return carritoItems; }
    public void setCarritoItems(List<CarritoItem> carritoItems) { this.carritoItems = carritoItems; }

    public List<DetallePedido> getDetallesPedido() { return detallesPedido; }
    public void setDetallesPedido(List<DetallePedido> detallesPedido) { this.detallesPedido = detallesPedido; }
}
