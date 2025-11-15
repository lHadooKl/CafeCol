// src/main/java/com/cafecol/resources/AuthResource.java
package com.cafecol.resources;

import com.cafecol.entities.Usuario;
import com.cafecol.service.UsuarioService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @EJB
    private UsuarioService usuarioService;

    // Ajuste: usar 'contrasena' (sin ñ) como nombre de campo esperado en JSON
    public static class Credenciales {
        public String nombreUsuario;
        public String contrasena;
    }

    public static class LoginResponse {
        public int idUsuario;
        public String nombreUsuario;
        public String rol;

        public LoginResponse(Usuario u) {
            this.idUsuario = u.getIdUsuario();
            this.nombreUsuario = u.getNombreUsuario();
            this.rol = u.getRol();
        }
    }

    @POST
    @Path("/login")
    public Response login(Credenciales c, @Context HttpServletRequest req) {
        if (c == null || c.nombreUsuario == null || c.contrasena == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Credenciales inválidas\"}")
                    .build();
        }

        Usuario u = usuarioService.findByNombreUsuario(c.nombreUsuario);
        if (u == null || !u.getContrasena().equals(c.contrasena)) { // compara contra el campo almacenado
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Credenciales inválidas\"}")
                    .build();
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("usuarioId", u.getIdUsuario());
        session.setAttribute("usuarioNombre", u.getNombreUsuario());
        session.setAttribute("usuarioRol", u.getRol());

        return Response.ok(new LoginResponse(u)).build();
    }

    @GET
    @Path("/session")
    public Response session(@Context HttpServletRequest req) {
        HttpSession s = req.getSession(false);

        boolean autenticado = (s != null && s.getAttribute("usuarioId") != null);
        String nombre = (s != null) ? (String) s.getAttribute("usuarioNombre") : null;
        String rol = (s != null) ? (String) s.getAttribute("usuarioRol") : null;

        String json = String.format(
                "{\"autenticado\":%s,\"nombreUsuario\":%s,\"rol\":%s}",
                autenticado ? "true" : "false",
                nombre != null ? "\"" + nombre + "\"" : "null",
                rol != null ? "\"" + rol + "\"" : "null"
        );

        return Response.ok(json).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate(); 
        }
        return Response.noContent().build();
    }
}
