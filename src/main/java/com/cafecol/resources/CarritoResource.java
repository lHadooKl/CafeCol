package com.cafecol.resources;

import com.cafecol.entities.*;
import com.cafecol.service.CarritoService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Path("/carrito")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarritoResource {

    @EJB private CarritoService carritoService;
    @EJB private com.cafecol.service.ClienteService clienteService; // crea si no existe

    @GET
    public Response getCarrito(@Context HttpServletRequest req) {
        Carrito car = resolverCarrito(req);
        return Response.ok(toDTO(car)).build();
    }

    public static class AddItemReq { public int idProducto; public int cantidad; }

    @POST @Path("/items")
    public Response agregarItem(AddItemReq body, @Context HttpServletRequest req) {
        if (body == null || body.cantidad <= 0) return Response.status(400).entity(msg("Cantidad inválida")).build();
        Carrito car = resolverCarrito(req);
        car = carritoService.agregarItem(car.getIdCarrito(), body.idProducto, body.cantidad);
        return Response.ok(toDTO(car)).build();
    }

    public static class UpdateItemReq { public int cantidad; }

    @PATCH @Path("/items/{idItem}")
    public Response actualizarItem(@PathParam("idItem") int idItem, UpdateItemReq body, @Context HttpServletRequest req) {
        if (body == null || body.cantidad < 0) return Response.status(400).entity(msg("Cantidad inválida")).build();
        Carrito car = resolverCarrito(req);
        car = carritoService.actualizarCantidad(car.getIdCarrito(), idItem, body.cantidad);
        return Response.ok(toDTO(car)).build();
    }

    @DELETE @Path("/items/{idItem}")
    public Response eliminarItem(@PathParam("idItem") int idItem, @Context HttpServletRequest req) {
        Carrito car = resolverCarrito(req);
        car = carritoService.eliminarItem(car.getIdCarrito(), idItem);
        return Response.ok(toDTO(car)).build();
    }

    @POST @Path("/checkout")
    public Response checkout(@Context HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Cliente cliente = null;
        if (s != null && s.getAttribute("clienteId") != null) {
            int idCliente = (Integer) s.getAttribute("clienteId");
            cliente = clienteService.findById(idCliente);
        }
        Carrito car = resolverCarrito(req);
        Pedido pedido = carritoService.checkout(car.getIdCarrito(), cliente);
        if (s != null) s.removeAttribute("carritoId");
        return Response.ok(pedido).build();
    }

    // Helpers
    private Carrito resolverCarrito(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        Integer carritoId = (Integer) s.getAttribute("carritoId");
        Carrito car = null;
        if (carritoId != null) {
            car = carritoService.buscarCarritoPorId(carritoId);
        }
        if (car == null) {
            Integer clienteId = (Integer) s.getAttribute("clienteId");
            if (clienteId != null) {
                Carrito existente = carritoService.buscarCarritoActivoPorCliente(clienteId);
                if (existente != null) {
                    car = existente;
                } else {
                    Cliente c = clienteService.findById(clienteId);
                    car = carritoService.crearCarritoParaCliente(c);
                }
            } else {
                car = carritoService.crearCarritoAnonimo();
            }
            s.setAttribute("carritoId", car.getIdCarrito());
        }
        return car;
    }
    
    @DELETE
    @Path("/vaciar") // El endpoint que faltaba
    public Response vaciarCarrito(@Context HttpServletRequest req) {
        Carrito car = resolverCarrito(req);
        car = carritoService.vaciarCarrito(car.getIdCarrito()); // Necesitas este método en tu CarritoService
        return Response.ok(toDTO(car)).build();
    }


    private static java.util.Map<String,String> msg(String m){ return java.util.Collections.singletonMap("mensaje", m); }

    // DTOs simples
    public static class CarritoDTO {
        public int idCarrito;
        public String estado;
        public java.util.List<ItemDTO> items;
        public double total;
    }
    public static class ItemDTO {
        public int idItem;
        public int idProducto;
        public String nombre;
        public String descripcion;
        public double precioUnitario;
        public int cantidad;
        public double subtotal;
        public int stockDisponible;
    }
    private CarritoDTO toDTO(Carrito c) {
        CarritoDTO dto = new CarritoDTO();
        dto.idCarrito = c.getIdCarrito();
        dto.estado = c.getEstado();
        dto.items = c.getItems().stream().map(ci -> {
            ItemDTO it = new ItemDTO();
            it.idItem = ci.getIdItem();
            it.idProducto = ci.getProducto().getIdProducto();
            it.nombre = ci.getProducto().getNombre();
            it.descripcion = ci.getProducto().getDescripcion();
            it.precioUnitario = ci.getPrecioUnitario();
            it.cantidad = ci.getCantidad();
            it.subtotal = ci.getCantidad() * ci.getPrecioUnitario();
            it.stockDisponible = ci.getProducto().getStock();
            return it;
        }).collect(Collectors.toList());
        dto.total = dto.items.stream().mapToDouble(x -> x.subtotal).sum();
        return dto;
    }
}
