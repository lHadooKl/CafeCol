package com.cafecol.resources;

import com.cafecol.entities.Usuario;
import com.cafecol.service.UsuarioService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.util.List;

@Path("/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @EJB
    private UsuarioService usuarioService;

    @Context
    private UriInfo uriInfo;

    // Utils básicos para normalizar
    private String nn(String s) { return s == null ? "" : s.trim(); }
    private String up(String s) { return nn(s).toUpperCase(); }

    @GET
    public List<Usuario> listar() {
        return usuarioService.findAll();
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) {
        Usuario u = usuarioService.findById(id);
        if (u == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(u).build();
    }

    @GET
    @Path("by-username/{nombreUsuario}")
    public Response obtenerPorNombre(@PathParam("nombreUsuario") String nombreUsuario) {
        Usuario u = usuarioService.findByNombreUsuario(nombreUsuario);
        if (u == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(u).build();
    }

    // Nuevo: búsqueda por correo
    @GET
    @Path("by-email/{correo}")
    public Response obtenerPorCorreo(@PathParam("correo") String correo) {
        Usuario u = usuarioService.findByCorreo(correo);
        if (u == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(u).build();
    }

    @POST
    public Response crear(Usuario u) {
        try {
            // Normalización mínima (el servicio también valida, esto mejora mensajes rápidos)
            u.setNombreUsuario(nn(u.getNombreUsuario()));
            u.setRol(up(u.getRol()));
            u.setNombre(nn(u.getNombre()));
            u.setApellido(nn(u.getApellido()));
            u.setCorreo(nn(u.getCorreo()));
            if (nn(u.getEstado()).isEmpty()) u.setEstado("ACTIVO");

            // Chequeos de unicidad previos para mejor UX
            if (usuarioService.findByNombreUsuario(u.getNombreUsuario()) != null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("El nombre de usuario ya existe").build();
            }
            if (usuarioService.findByCorreo(u.getCorreo()) != null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("El correo ya está registrado").build();
            }

            Usuario creado = usuarioService.create(u);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            return Response.created(ub.path(String.valueOf(creado.getIdUsuario())).build())
                           .entity(creado)
                           .build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, Usuario u) {
        try {
            Usuario existente = usuarioService.findById(id);
            if (existente == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Normalización mínima
            if (u.getNombreUsuario() != null) u.setNombreUsuario(nn(u.getNombreUsuario()));
            if (u.getRol() != null) u.setRol(up(u.getRol()));
            if (u.getNombre() != null) u.setNombre(nn(u.getNombre()));
            if (u.getApellido() != null) u.setApellido(nn(u.getApellido()));
            if (u.getCorreo() != null) u.setCorreo(nn(u.getCorreo()));
            if (u.getEstado() != null) u.setEstado(up(nn(u.getEstado())));

            // Unicidad nombreUsuario si cambia
            if (u.getNombreUsuario() != null && !u.getNombreUsuario().equals(existente.getNombreUsuario())) {
                Usuario otro = usuarioService.findByNombreUsuario(u.getNombreUsuario());
                if (otro != null && otro.getIdUsuario() != id) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("El nombre de usuario ya existe").build();
                }
            }
            // Unicidad correo si cambia
            if (u.getCorreo() != null && !u.getCorreo().equals(existente.getCorreo())) {
                Usuario otroCorreo = usuarioService.findByCorreo(u.getCorreo());
                if (otroCorreo != null && otroCorreo.getIdUsuario() != id) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("El correo ya está registrado").build();
                }
            }

            u.setIdUsuario(id);
            Usuario actualizado = usuarioService.update(u);
            return Response.ok(actualizado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) {
        try {
            Usuario existente = usuarioService.findById(id);
            if (existente == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            usuarioService.delete(id);
            return Response.noContent().build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
    }
}
