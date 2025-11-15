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

    // Utilidades simples
    private String nn(String s) { return s == null ? "" : s.trim(); }
    private String upper(String s) { return nn(s).toUpperCase(); }

    // Validaciones de dominio
    private void validarNuevo(Usuario u) {
        if (nn(u.getNombreUsuario()).length() < 4)
            throw new RuntimeException("El nombre de usuario debe tener al menos 4 caracteres");
        if (nn(u.getContrasena()).length() < 6)
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        if (nn(u.getNombre()).isEmpty())
            throw new RuntimeException("El nombre es obligatorio");
        if (nn(u.getApellido()).isEmpty())
            throw new RuntimeException("El apellido es obligatorio");
        if (nn(u.getCorreo()).isEmpty())
            throw new RuntimeException("El correo es obligatorio");
        if (nn(u.getRol()).isEmpty())
            throw new RuntimeException("El rol es obligatorio");
        if (nn(u.getEstado()).isEmpty())
            u.setEstado("ACTIVO");
        // Unicidad
        if (findByNombreUsuario(u.getNombreUsuario()) != null)
            throw new RuntimeException("El nombre de usuario ya existe");
        // Si decides manejar unicidad de correo a nivel app:
        // if (findByCorreo(u.getCorreo()) != null)
        //     throw new RuntimeException("El correo ya está registrado");
    }

    private void validarEdicion(Usuario existente, Usuario cambios) {
        if (existente == null)
            throw new RuntimeException("Usuario no existe");

        // Si cambia nombreUsuario, valida longitud y unicidad
        String nuevoUser = nn(cambios.getNombreUsuario());
        if (!nuevoUser.equals(nn(existente.getNombreUsuario()))) {
            if (nuevoUser.length() < 4)
                throw new RuntimeException("El nombre de usuario debe tener al menos 4 caracteres");
            Usuario otro = findByNombreUsuario(nuevoUser);
            if (otro != null && otro.getIdUsuario() != existente.getIdUsuario())
                throw new RuntimeException("El nombre de usuario ya existe");
        }

        // Campos obligatorios
        if (nn(cambios.getNombre()).isEmpty())
            throw new RuntimeException("El nombre es obligatorio");
        if (nn(cambios.getApellido()).isEmpty())
            throw new RuntimeException("El apellido es obligatorio");
        if (nn(cambios.getCorreo()).isEmpty())
            throw new RuntimeException("El correo es obligatorio");
        if (nn(cambios.getRol()).isEmpty())
            throw new RuntimeException("El rol es obligatorio");

        // Contraseña: si viene, valida longitud; si no, no se cambia
        if (cambios.getContrasena() != null && !cambios.getContrasena().isEmpty()) {
            if (cambios.getContrasena().length() < 6)
                throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }

        // Estado por defecto si llega vacío
        if (nn(cambios.getEstado()).isEmpty())
            cambios.setEstado(existente.getEstado() == null ? "ACTIVO" : existente.getEstado());

        // Si manejas unicidad de correo, valida cambio:
        // Usuario otroCorreo = findByCorreo(cambios.getCorreo());
        // if (otroCorreo != null && otroCorreo.getIdUsuario() != existente.getIdUsuario())
        //     throw new RuntimeException("El correo ya está registrado");
    }

    public Usuario create(Usuario u) {
        try {
            // Normalización
            u.setNombreUsuario(nn(u.getNombreUsuario()));
            u.setContrasena(nn(u.getContrasena())); // aquí podrías hashear
            u.setRol(upper(u.getRol()));
            u.setNombre(nn(u.getNombre()));
            u.setApellido(nn(u.getApellido()));
            u.setCorreo(nn(u.getCorreo()));
            u.setEstado(upper(nn(u.getEstado().isEmpty() ? "ACTIVO" : u.getEstado())));

            validarNuevo(u);

            em.persist(u);
            em.flush();
            return u;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al crear usuario: " + ex.getMessage(), ex);
        }
    }

    public Usuario update(Usuario u) {
        try {
            Usuario existente = em.find(Usuario.class, u.getIdUsuario());
            validarEdicion(existente, u);

            // Aplicar cambios campo a campo (evita sobrescribir con nulls)
            if (u.getNombreUsuario() != null) existente.setNombreUsuario(nn(u.getNombreUsuario()));
            if (u.getRol() != null) existente.setRol(upper(u.getRol()));
            if (u.getNombre() != null) existente.setNombre(nn(u.getNombre()));
            if (u.getApellido() != null) existente.setApellido(nn(u.getApellido()));
            if (u.getCorreo() != null) existente.setCorreo(nn(u.getCorreo()));
            if (u.getEstado() != null) existente.setEstado(upper(u.getEstado()));

            // Contraseña: solo si se envía
            if (u.getContrasena() != null && !u.getContrasena().isEmpty()) {
                existente.setContrasena(nn(u.getContrasena())); // aquí podrías hashear
            }

            return em.merge(existente);
        } catch (PersistenceException ex) {
            throw new RuntimeException("Error al actualizar usuario: " + ex.getMessage(), ex);
        }
    }

    public List<Usuario> findAll() {
        return em.createQuery("SELECT u FROM Usuario u ORDER BY u.idUsuario DESC", Usuario.class).getResultList();
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
        q.setParameter("nombreUsuario", nn(nombreUsuario));
        List<Usuario> result = q.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    // Si decides validar unicidad de correo a nivel servicio, descomenta y usa:
    public Usuario findByCorreo(String correo) {
        TypedQuery<Usuario> q = em.createQuery(
             "SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class);
         q.setParameter("correo", nn(correo));
         List<Usuario> result = q.getResultList();
         return result.isEmpty() ? null : result.get(0);
     }
}
