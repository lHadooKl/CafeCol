package com.cafecol.service;

import com.cafecol.entities.Usuario;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.PersistenceException;
import java.util.List;

@Stateless
public class UsuarioService {

    @PersistenceContext(unitName = "jpaSistemaCafeCol")
    private EntityManager em;

    public Usuario create(Usuario u) {
        try {
            em.persist(u);
            em.flush();
            return u;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al crear usuario: " + ex.getMessage(), ex);
        }
    }

    public Usuario update(Usuario u) {
        try {
            return em.merge(u);
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al actualizar usuario: " + ex.getMessage(), ex);
        }
    }

    public List<Usuario> findAll() {
        return em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
    }

    public Usuario findById(int id) {
        return em.find(Usuario.class, id);
    }

    public void delete(int id) {
        try {
            Usuario u = em.find(Usuario.class, id);
            if (u != null) {
                em.remove(u);
                em.flush();
            }
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al eliminar usuario: " + ex.getMessage(), ex);
        }
    }

    public Usuario findByNombreUsuario(String nombreUsuario) {
        TypedQuery<Usuario> q = em.createQuery(
            "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario", Usuario.class);
        q.setParameter("nombreUsuario", nombreUsuario);
        List<Usuario> result = q.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
