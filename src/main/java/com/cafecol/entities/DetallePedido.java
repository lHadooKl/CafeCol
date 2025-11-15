package com.cafecol.entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

@Entity
@Table(name = "detalle_pedidos")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idDetalle;

    private int cantidad;
    private Double subtotal;

    // 🔹 Agregar este nuevo campo
    private Double precioUnitario;

    // Relación N:1 con Pedido
    @ManyToOne
    @JoinColumn(name = "idPedido")
    @JsonbTransient
    private Pedido pedido;

    // Relación N:1 con Producto
    @ManyToOne
    @JoinColumn(name = "idProducto")
    private Producto producto;

    public DetallePedido() {}

    public DetallePedido(int cantidad, Double subtotal, Double precioUnitario) {
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.precioUnitario = precioUnitario;
    }

    // Getters y Setters
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
}
