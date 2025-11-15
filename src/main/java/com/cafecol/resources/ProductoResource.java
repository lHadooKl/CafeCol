package com.cafecol.resources;

import com.cafecol.entities.Producto;
import com.cafecol.service.ProductoService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @EJB
    private ProductoService productoService;

    @POST
    public Response create(Producto p, @Context UriInfo uriInfo) {
        try {
            Producto creado = productoService.create(p);
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(creado.getIdProducto()))
                    .build();
            return Response.created(location).entity(creado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo crear el producto", ex.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Producto p) {
        try {
            p.setIdProducto(id);
            Producto actualizado = productoService.update(p);
            return Response.ok(actualizado).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo actualizar el producto", ex.getMessage()))
                    .build();
        }
    }

    @GET
    public List<Producto> findAll() {
        return productoService.findAll();
    }

    @GET
    @Path("{id}")
    public Response findById(@PathParam("id") int id) {
        Producto p = productoService.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Producto no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        return Response.ok(p).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        Producto existing = productoService.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Producto no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        try {
            productoService.delete(id);
            return Response.noContent().build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("No se pudo eliminar el producto", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("buscar")
    public Response findByNombre(@QueryParam("nombre") String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Parámetro inválido", "El parámetro 'nombre' es requerido"))
                    .build();
        }
        List<Producto> resultados = productoService.findByNombre(nombre);
        return Response.ok(resultados).build();
    }

    @POST
    @Path("{id}/stock")
    public Response disminuirStock(@PathParam("id") int id, StockUpdateRequest req) {
        if (req == null || req.getCantidadVendida() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Parámetro inválido", "cantidadVendida debe ser > 0"))
                    .build();
        }
        Producto p = productoService.findById(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Producto no encontrado", "ID " + id + " no existe"))
                    .build();
        }
        productoService.actualizarStock(id, req.getCantidadVendida());
        Producto actualizado = productoService.findById(id);
        return Response.ok(actualizado).build();
    }

    public static class StockUpdateRequest {
        private int cantidadVendida;
        public int getCantidadVendida() { return cantidadVendida; }
        public void setCantidadVendida(int cantidadVendida) { this.cantidadVendida = cantidadVendida; }
    }

    public static class ApiError {
        private String mensaje;
        private String detalle;
        public ApiError() { }
        public ApiError(String mensaje, String detalle) { this.mensaje = mensaje; this.detalle = detalle; }
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        public String getDetalle() { return detalle; }
        public void setDetalle(String detalle) { this.detalle = detalle; }
    }
}
