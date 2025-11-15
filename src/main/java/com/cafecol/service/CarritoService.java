package com.cafecol.service;

import com.cafecol.entities.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class CarritoService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    // 🔹 Crear carrito para un cliente registrado
    public Carrito crearCarritoParaCliente(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("Cliente no puede ser nulo");

        Carrito carrito = new Carrito();
        carrito.setCliente(cliente);
        carrito.setEstado("ABIERTO");

        em.persist(carrito);
        em.flush();
        return carrito;
    }

    // 🔹 Crear carrito anónimo (sin cliente)
    public Carrito crearCarritoAnonimo() {
        Carrito carrito = new Carrito();
        carrito.setEstado("ABIERTO");

        em.persist(carrito);
        em.flush();
        return carrito;
    }

    // 🔹 Buscar carrito por ID
    public Carrito buscarCarritoPorId(int id) {
        return em.find(Carrito.class, id);
    }

    // 🔹 Buscar carrito activo de un cliente
    public Carrito buscarCarritoActivoPorCliente(int idCliente) {
        TypedQuery<Carrito> query = em.createQuery(
            "SELECT c FROM Carrito c WHERE c.estado = 'ABIERTO' AND c.cliente.idCliente = :id",
            Carrito.class
        );
        query.setParameter("id", idCliente);
        List<Carrito> resultados = query.getResultList();
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    // 🔹 MÉTODO NUEVO → EpaycoResource lo necesita
    public Carrito obtenerCarritoPorCliente(int idCliente) {
        return buscarCarritoActivoPorCliente(idCliente);
    }

    // 🔹 Agregar producto al carrito
    public Carrito agregarItem(int idCarrito, int idProducto, int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");

        Carrito carrito = em.find(Carrito.class, idCarrito);
        if (carrito == null) throw new IllegalArgumentException("Carrito no existe");

        Producto producto = em.find(Producto.class, idProducto);
        if (producto == null) throw new IllegalArgumentException("Producto no existe");

        CarritoItem item = obtenerItem(carrito, producto);
        int nuevaCantidad = cantidad + (item == null ? 0 : item.getCantidad());

        if (nuevaCantidad > producto.getStock())
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());

        if (item == null) {
            item = new CarritoItem();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(producto.getPrecio());
            em.persist(item);
            carrito.getItems().add(item);
        } else {
            item.setCantidad(nuevaCantidad);
            em.merge(item);
        }

        em.flush();
        return carrito;
    }

    // 🔹 Actualizar cantidad de un item
    public Carrito actualizarCantidad(int idCarrito, int idItem, int cantidad) {
        Carrito carrito = em.find(Carrito.class, idCarrito);
        if (carrito == null) throw new IllegalArgumentException("Carrito no existe");

        CarritoItem item = em.find(CarritoItem.class, idItem);
        if (item == null || item.getCarrito().getIdCarrito() != idCarrito)
            throw new IllegalArgumentException("Item no pertenece al carrito");

        if (cantidad <= 0) {
            carrito.getItems().remove(item);
            em.remove(item);
        } else {
            if (cantidad > item.getProducto().getStock())
                throw new IllegalStateException("Stock insuficiente para el producto");
            item.setCantidad(cantidad);
            em.merge(item);
        }

        em.flush();
        return carrito;
    }

    // 🔹 Eliminar un item del carrito
    public Carrito eliminarItem(int idCarrito, int idItem) {
        Carrito carrito = em.find(Carrito.class, idCarrito);
        if (carrito == null) throw new IllegalArgumentException("Carrito no existe");

        CarritoItem item = em.find(CarritoItem.class, idItem);
        if (item != null && item.getCarrito().getIdCarrito() == idCarrito) {
            carrito.getItems().remove(item);
            em.remove(item);
        }

        em.flush();
        return carrito;
    }

    // 🔹 Vaciar el carrito
    public Carrito vaciarCarrito(int idCarrito) {
        Carrito carrito = em.find(Carrito.class, idCarrito);
        if (carrito != null && !carrito.getItems().isEmpty()) {
            em.createQuery("DELETE FROM CarritoItem ci WHERE ci.carrito.idCarrito = :id")
              .setParameter("id", idCarrito)
              .executeUpdate();

            em.refresh(carrito);
        }
        return carrito;
    }

    // 🔹 MÉTODO NUEVO → Construye un Pedido a partir del carrito (usado en pasarela ePayco)
    public Pedido generarPedidoDesdeCarrito(Carrito carrito) {

        Pedido pedido = new Pedido();
        pedido.setCliente(carrito.getCliente());
        pedido.setFecha(LocalDate.now());
        pedido.setEstado("Pendiente");

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;

        for (CarritoItem item : carrito.getItems()) {
            DetallePedido dp = new DetallePedido();
            dp.setPedido(pedido);
            dp.setProducto(item.getProducto());
            dp.setCantidad(item.getCantidad());
            dp.setPrecioUnitario(item.getPrecioUnitario());
            dp.setSubtotal(item.getCantidad() * item.getPrecioUnitario());

            total += dp.getSubtotal();
            detalles.add(dp);
        }

        pedido.setDetalles(detalles);
        pedido.setTotal(total);

        return pedido;
    }

    // 🔹 Método auxiliar para buscar item existente
    private CarritoItem obtenerItem(Carrito carrito, Producto producto) {
        for (CarritoItem it : carrito.getItems()) {
            if (it.getProducto().getIdProducto() == producto.getIdProducto())
                return it;
        }
        return null;
    }
    // 🔹 CHECKOUT COMPLETO → llamado desde el CarritoResource
    public Pedido checkout(int idCarrito, Cliente cliente) {

        Carrito carrito = em.find(Carrito.class, idCarrito);
        if (carrito == null) {
            throw new IllegalArgumentException("Carrito no existe");
        }

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new IllegalStateException("El carrito está vacío");
        }

        // Crear nuevo pedido
        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDate.now());
        pedido.setEstado("Pagado"); // o “Pendiente” según tu lógica

        // Asignar cliente (puede ser null = invitado)
        pedido.setCliente(cliente);

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;

        for (CarritoItem item : carrito.getItems()) {

            // Descontar stock
            Producto p = item.getProducto();
            if (item.getCantidad() > p.getStock()) {
                throw new IllegalStateException(
                    "Stock insuficiente para: " + p.getNombre()
                );
            }
            p.setStock(p.getStock() - item.getCantidad());
            em.merge(p);

            // Crear detalle del pedido
            DetallePedido dp = new DetallePedido();
            dp.setPedido(pedido);
            dp.setProducto(p);
            dp.setCantidad(item.getCantidad());
            dp.setPrecioUnitario(item.getPrecioUnitario());
            dp.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
            detalles.add(dp);

            total += dp.getSubtotal();
        }

        pedido.setDetalles(detalles);
        pedido.setTotal(total);

        // Persistir pedido
        em.persist(pedido);

        // Cerrar carrito
        carrito.setEstado("CERRADO");
        em.merge(carrito);

        // Vaciar items del carrito
        em.createQuery("DELETE FROM CarritoItem ci WHERE ci.carrito.idCarrito = :id")
            .setParameter("id", idCarrito)
            .executeUpdate();

        em.flush();

        return pedido;
    }

    
}
