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

    @POST
    public Response crear(Usuario u) {
        try {
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
