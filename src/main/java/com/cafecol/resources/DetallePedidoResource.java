/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cafecol.resources;

import com.cafecol.entities.DetallePedido;
import com.cafecol.service.DetallePedidoService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("/detalles-pedido")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DetallePedidoResource {

    @EJB
    private DetallePedidoService detalleService;

    @POST
    public Response create(DetallePedido d, @Context UriInfo uriInfo) {
        try {
            DetallePedido creado = detalleService.create(d);
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(creado.getIdDetalle()))
                    .build();
            return Response.created(location).entity(creado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo crear el detalle", ex.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, DetallePedido d) {
        try {
            // Asegura que el ID de la ruta prevalezca
            d.setIdDetalle(id);
            DetallePedido actualizado = detalleService.update(d);
            return Response.ok(actualizado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo actualizar el detalle", ex.getMessage()))
                    .build();
        }
    }

    @GET
    public List<DetallePedido> findAll() {
        return detalleService.findAll();
    }

    @GET
    @Path("{id}")
    public Response findById(@PathParam("id") int id) {
        DetallePedido d = detalleService.findById(id);
        if (d == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Detalle no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        return Response.ok(d).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        DetallePedido existing = detalleService.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Detalle no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        try {
            detalleService.delete(id);
            return Response.noContent().build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo eliminar el detalle", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("pedido/{idPedido}")
    public List<DetallePedido> findByPedido(@PathParam("idPedido") int idPedido) {
        return detalleService.findByPedido(idPedido);
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
