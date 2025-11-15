package com.cafecol.service;

import com.cafecol.entities.*;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class PedidoService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    @EJB
    private CarritoService carritoService;

    // Crear pedido DESDE FRONT con detalles libres (sin exigir producto)
    public Pedido create(Pedido pedido) {
        try {
            if (pedido == null || pedido.getCliente() == null || pedido.getCliente().getIdCliente() == 0) {
                throw new IllegalArgumentException("El cliente es obligatorio para crear el pedido.");
            }
            if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
                throw new IllegalArgumentException("Debe incluir al menos un detalle de pedido.");
            }

            // 1) Adjuntar cliente administrado
            Cliente cliente = em.find(Cliente.class, pedido.getCliente().getIdCliente());
            if (cliente == null) {
                throw new IllegalArgumentException("No se encontró el cliente con ID: " + pedido.getCliente().getIdCliente());
            }

            // 2) Datos base
            pedido.setCliente(cliente);
            if (pedido.getFecha() == null) pedido.setFecha(LocalDate.now());
            // Respetar estado del payload si viene; default PENDIENTE
            if (pedido.getEstado() == null || pedido.getEstado().isBlank()) pedido.setEstado("PENDIENTE");

            // Asegurar colección inicializada para evitar NPE
            if (pedido.getDetalles() == null) pedido.setDetalles(new ArrayList<>());

            // 3) Enlazar detalles y calcular total (sin exigir producto)
            double total = 0.0;
            for (DetallePedido d : pedido.getDetalles()) {
                if (d == null) continue;
                if (d.getCantidad() <= 0.0) d.setCantidad(1);
                if (d.getPrecioUnitario() < 0.0) d.setPrecioUnitario( 0.0);

                // Si VIENE producto en el JSON y deseas forzar precio del producto, puedes resolverlo aquí
                if (d.getProducto() != null && d.getProducto().getIdProducto() != 0) {
                    Producto prod = em.find(Producto.class, d.getProducto().getIdProducto());
                    if (prod == null) {
                        throw new IllegalArgumentException("No se encontró el producto con ID: " + d.getProducto().getIdProducto());
                    }
                    // NO descuentes stock en este flujo “libre” a menos que lo definas explícitamente
                    // d.setProducto(prod);
                    // d.setPrecioUnitario(prod.getPrecio());
                }

                d.setSubtotal(d.getPrecioUnitario() * d.getCantidad());
                d.setPedido(pedido); // enlazar al padre
                total += d.getSubtotal();
            }
            pedido.setTotal(total);

            // 4) Persistencia
            em.persist(pedido);     // requiere cascade en @OneToMany o persistir detalles luego
            // Si no tienes cascade, persiste manualmente:
            for (DetallePedido d : pedido.getDetalles()) {
                em.persist(d);
            }

            em.flush(); // surfacing de errores de constraint
            return pedido;

        } catch (Exception e) {
            e.printStackTrace();
            throw new PersistenceException("No se pudo crear el pedido: " + e.getMessage(), e);
        }
    }

   

    // Actualizar pedido (parcial y seguro)
    public Pedido update(Pedido pedido) {
      try {
        Pedido existente = em.find(Pedido.class, pedido.getIdPedido());
        if (existente == null) {
          throw new IllegalArgumentException("No se encontró el pedido con ID: " + pedido.getIdPedido());
        }

        // Estado (opcional)
        if (pedido.getEstado() != null && !pedido.getEstado().isBlank()) {
          String nuevo = pedido.getEstado().trim().toUpperCase();
          // set permitido (ajústalo a tus reglas reales)
          java.util.Set<String> permitidos = java.util.Set.of("PENDIENTE","ACEPTADO","GESTIONADO","CERRADO","PAGADO");
          if (!permitidos.contains(nuevo)) {
            throw new IllegalArgumentException("Estado inválido: " + nuevo);
          }
          // regla opcional: no reabrir cerrado
          String actual = existente.getEstado() == null ? "" : existente.getEstado().toUpperCase();
          if ("CERRADO".equals(actual) && !"CERRADO".equals(nuevo)) {
            throw new IllegalStateException("No se puede cambiar un pedido en estado CERRADO");
          }
          existente.setEstado(nuevo);
        }

        // Total (opcional): respeta 0.0 si llega, usa null-check
        if (pedido.getTotal() != null) {
          existente.setTotal(pedido.getTotal());
        }

        // No tocar cliente/detalles aquí

        em.flush();
        return existente;

      } catch (Exception e) {
        throw new jakarta.persistence.PersistenceException("No se pudo actualizar el pedido: " + e.getMessage(), e);
      }
    }


    // Listar
    public List<Pedido> findAll() {
        TypedQuery<Pedido> query = em.createQuery("SELECT p FROM Pedido p ORDER BY p.idPedido DESC", Pedido.class);
        return query.getResultList();
    }

    // Buscar
    public Pedido findById(int id) {
        return em.find(Pedido.class, id);
    }

    // Eliminar
    public void delete(int id) {
        try {
            Pedido pedido = em.find(Pedido.class, id);
            if (pedido == null) {
                throw new IllegalArgumentException("No se encontró el pedido con ID: " + id);
            }
            // si tienes orphanRemoval en detalles, bastará con remove del pedido
            em.remove(pedido);
        } catch (Exception e) {
            throw new PersistenceException("No se pudo eliminar el pedido: " + e.getMessage(), e);
        }
    }

    // Por cliente
    public List<Pedido> findByCliente(int idCliente) {
        TypedQuery<Pedido> query = em.createQuery(
            "SELECT p FROM Pedido p WHERE p.cliente.idCliente = :idCliente ORDER BY p.fecha DESC",
            Pedido.class
        );
        query.setParameter("idCliente", idCliente);
        return query.getResultList();
    }

    // Crear pedido desde CARRITO (con productos y control de stock)
    public Pedido crearPedidoDesdeCarritoId(int idCliente, int idCarrito) {
        try {
            Cliente cliente = em.find(Cliente.class, idCliente);
            if (cliente == null) {
                throw new IllegalArgumentException("No existe el cliente con ID " + idCliente);
            }
            Carrito carrito = em.find(Carrito.class, idCarrito);
            if (carrito == null || carrito.getItems() == null || carrito.getItems().isEmpty()) {
                throw new IllegalStateException("El carrito está vacío o no existe.");
            }

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setFecha(LocalDate.now());
            pedido.setEstado("PAGADO"); // este flujo suele usarse en confirmación aprobada

            if (pedido.getDetalles() == null) pedido.setDetalles(new ArrayList<>());

            double total = 0.0;

            for (CarritoItem item : carrito.getItems()) {
                if (item == null || item.getProducto() == null) continue;

                Producto producto = em.find(Producto.class, item.getProducto().getIdProducto());
                if (producto == null) {
                    throw new IllegalArgumentException("No existe producto ID " + item.getProducto().getIdProducto());
                }
                if (item.getCantidad() > producto.getStock()) {
                    throw new IllegalStateException("Stock insuficiente para: " + producto.getNombre());
                }

                // Descontar stock
                producto.setStock(producto.getStock() - item.getCantidad());
                em.merge(producto);

                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProducto(producto);
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(producto.getPrecio());
                detalle.setSubtotal(producto.getPrecio() * item.getCantidad());

                total += detalle.getSubtotal();
                pedido.getDetalles().add(detalle);
            }

            pedido.setTotal(total);

            em.persist(pedido);
            em.flush();

            for (DetallePedido d : pedido.getDetalles()) {
                em.persist(d);
            }

            return pedido;

        } catch (Exception e) {
            e.printStackTrace();
            throw new PersistenceException("Error creando pedido desde el carrito: " + e.getMessage(), e);
        }
    }
}
