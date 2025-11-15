package com.cafecol.resources;

import com.cafecol.entities.Cliente;
import com.cafecol.service.ClienteService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteResource {

    @EJB
    private ClienteService clienteService;

    @POST
    public Response create(Cliente c, @Context UriInfo uriInfo) {
        try {
            Cliente creado = clienteService.create(c);
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(creado.getIdCliente()))
                    .build();
            return Response.created(location).entity(creado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo crear el cliente", ex.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Cliente c) {
        try {
            c.setIdCliente(id);
            Cliente actualizado = clienteService.update(c);
            return Response.ok(actualizado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo actualizar el cliente", ex.getMessage()))
                    .build();
        }
    }

    @GET
    public List<Cliente> findAll() {
        return clienteService.findAll();
    }

    @GET
    @Path("{id}")
    public Response findById(@PathParam("id") int id) {
        Cliente c = clienteService.findById(id);
        if (c == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Cliente no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        return Response.ok(c).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        Cliente existing = clienteService.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Cliente no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        try {
            clienteService.delete(id);
            return Response.noContent().build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo eliminar el cliente", ex.getMessage()))
                    .build();
        }
    }

    // Búsqueda por nombre: /clientes/buscar?nombre=ana
    @GET
    @Path("buscar")
    public Response findByNombre(@QueryParam("nombre") String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Parámetro inválido", "El parámetro 'nombre' es requerido"))
                    .build();
        }
        List<Cliente> resultados = clienteService.findByNombre(nombre);
        return Response.ok(resultados).build();
    }

    // DTO simple para errores de API
    public static class ApiError {
        private String mensaje;
        private String detalle;

        public ApiError() { }

        public ApiError(String mensaje, String detalle) {
            this.mensaje = mensaje;
            this.detalle = detalle;
        }

        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }

        public String getDetalle() { return detalle; }
        public void setDetalle(String detalle) { this.detalle = detalle; }
    }
}
