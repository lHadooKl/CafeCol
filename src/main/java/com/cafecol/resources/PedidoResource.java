package com.cafecol.resources;

import com.cafecol.entities.Pedido;
import com.cafecol.service.PedidoService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PedidoResource {

    @EJB
    private PedidoService pedidoService;

    // 🔹 Crear pedido con detalles
    @POST
    public Response create(Pedido p, @Context UriInfo uriInfo) {
        try {
            // Validar datos mínimos
            if (p.getCliente() == null || p.getCliente().getIdCliente() == 0)
                throw new IllegalArgumentException("Debe especificarse un cliente válido.");
            if (p.getDetalles() == null || p.getDetalles().isEmpty())
                throw new IllegalArgumentException("Debe incluir al menos un detalle de pedido.");

            Pedido creado = pedidoService.create(p);

            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(creado.getIdPedido()))
                    .build();

            return Response.created(location)
                    .entity(new ApiResponse("Pedido creado correctamente", creado))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Datos inválidos", e.getMessage()))
                    .build();

        } catch (RuntimeException ex) {
            ex.printStackTrace(); // 🔍 Esto mostrará la excepción real en la consola del servidor
            return Response.serverError()
                    .entity(new ApiError("No se pudo crear el pedido", ex.getMessage()))
                    .build();

        }
    }

    // 🔹 Actualizar pedido existente
    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Pedido p) {
        try {
            p.setIdPedido(id);
            Pedido actualizado = pedidoService.update(p);
            return Response.ok(actualizado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo actualizar el pedido", ex.getMessage()))
                    .build();
        }
    }

    // 🔹 Obtener todos los pedidos
    @GET
    public List<Pedido> findAll() {
        return pedidoService.findAll();
    }

    // 🔹 Buscar pedido por ID
    @GET
    @Path("{id}")
    public Response findById(@PathParam("id") int id) {
        Pedido p = pedidoService.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Pedido no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        return Response.ok(p).build();
    }

    // 🔹 Eliminar pedido
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        Pedido existing = pedidoService.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Pedido no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        try {
            pedidoService.delete(id);
            return Response.noContent().build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo eliminar el pedido", ex.getMessage()))
                    .build();
        }
    }

    // 🔹 Buscar pedidos por cliente
    @GET
    @Path("cliente/{idCliente}")
    public List<Pedido> findByCliente(@PathParam("idCliente") int idCliente) {
        return pedidoService.findByCliente(idCliente);
    }

    // ----------------------------
    // 🔹 Clases auxiliares de respuesta JSON
    // ----------------------------

    public static class ApiResponse {
        private String mensaje;
        private Pedido pedido;

        public ApiResponse(String mensaje, Pedido pedido) {
            this.mensaje = mensaje;
            this.pedido = pedido;
        }

        public String getMensaje() { return mensaje; }
        public Pedido getPedido() { return pedido; }
    }

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
