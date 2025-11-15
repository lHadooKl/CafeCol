package com.cafecol.service;

import com.cafecol.entities.Cliente;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.util.List;

@Stateless
public class ClienteService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    public Cliente create(Cliente c) {
        try {
            em.persist(c);
            em.flush();
            return c;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al crear el cliente: " + ex.getMessage(), ex);
        }
    }

    public Cliente update(Cliente c) {
        try {
            Cliente actual = em.find(Cliente.class, c.getIdCliente());
            if (actual == null) {
                throw new IllegalArgumentException("Cliente no existe: id=" + c.getIdCliente());
            }
            Cliente merged = em.merge(c);
            em.flush();
            return merged;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al actualizar el cliente: " + ex.getMessage(), ex);
        }
    }

    public List<Cliente> findAll() {
        return em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
    }

    public Cliente findById(int id) {
        return em.find(Cliente.class, id);
    }

    public void delete(int id) {
        try {
            Cliente c = em.find(Cliente.class, id);
            if (c != null) {
                em.remove(c);
                em.flush();
            }
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al eliminar el cliente: " + ex.getMessage(), ex);
        }
    }

    public List<Cliente> findByNombre(String nombre) {
        TypedQuery<Cliente> query = em.createQuery(
                "SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(:nombre)", Cliente.class);
        query.setParameter("nombre", "%" + nombre + "%");
        return query.getResultList();
    }

    public Cliente findByCorreo(String correo) {
        TypedQuery<Cliente> query = em.createQuery(
                "SELECT c FROM Cliente c WHERE c.correo = :correo", Cliente.class);
        query.setParameter("correo", correo);
        List<Cliente> l = query.getResultList();
        return l.isEmpty() ? null : l.get(0);
    }
}
