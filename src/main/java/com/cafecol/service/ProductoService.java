package com.cafecol.service;

import com.cafecol.entities.Producto;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Stateless
public class ProductoService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    public Producto create(Producto p) {
        try {
            em.persist(p);
            em.flush();
            return p;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al crear producto: " + ex.getMessage(), ex);
        }
    }

    public Producto update(Producto p) {
        try {
            return em.merge(p);
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al actualizar producto: " + ex.getMessage(), ex);
        }
    }

    public List<Producto> findAll() {
        return em.createQuery("SELECT p FROM Producto p", Producto.class).getResultList();
    }

    public Producto findById(int id) {
        return em.find(Producto.class, id);
    }

    public void delete(int id) {
        try {
            Producto p = em.find(Producto.class, id);
            if (p != null) {
                em.remove(p);
                em.flush();
            }
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al eliminar producto: " + ex.getMessage(), ex);
        }
    }

    public void actualizarStock(int idProducto, int cantidadVendida) {
        Producto p = em.find(Producto.class, idProducto);
        if (p != null) {
            p.setStock(p.getStock() - cantidadVendida);
            em.merge(p);
        }
    }

    public List<Producto> findByNombre(String nombre) {
        TypedQuery<Producto> q = em.createQuery(
            "SELECT p FROM Producto p WHERE LOWER(p.nombre) LIKE LOWER(:nombre)", Producto.class);
        q.setParameter("nombre", "%" + nombre + "%");
        return q.getResultList();
    }
}
