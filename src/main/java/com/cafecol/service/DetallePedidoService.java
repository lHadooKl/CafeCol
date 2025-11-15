package com.cafecol.service;

import com.cafecol.entities.DetallePedido;
import com.cafecol.entities.Pedido;
import com.cafecol.entities.Producto;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;

@Stateless
public class DetallePedidoService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    public DetallePedido create(DetallePedido d) {
        try {
            if (d.getPedido() != null && d.getPedido().getIdPedido() != 0) {
                d.setPedido(em.find(Pedido.class, d.getPedido().getIdPedido()));
            }
            if (d.getProducto() != null && d.getProducto().getIdProducto() != 0) {
                d.setProducto(em.find(Producto.class, d.getProducto().getIdProducto()));
            }
            em.persist(d);
            em.flush();
            return d;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al crear detalle de pedido: " + ex.getMessage(), ex);
        }
    }

    public DetallePedido update(DetallePedido d) {
        try {
            return em.merge(d);
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al actualizar detalle de pedido: " + ex.getMessage(), ex);
        }
    }

    public List<DetallePedido> findAll() {
        return em.createQuery("SELECT d FROM DetallePedido d", DetallePedido.class).getResultList();
    }

    public DetallePedido findById(int id) {
        return em.find(DetallePedido.class, id);
    }

    public void delete(int id) {
        try {
            DetallePedido d = em.find(DetallePedido.class, id);
            if (d != null) {
                em.remove(d);
                em.flush();
            }
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al eliminar detalle de pedido: " + ex.getMessage(), ex);
        }
    }

    public List<DetallePedido> findByPedido(int idPedido) {
        return em.createQuery(
            "SELECT d FROM DetallePedido d WHERE d.pedido.idPedido = :idPedido", DetallePedido.class)
            .setParameter("idPedido", idPedido)
            .getResultList();
    }
}
