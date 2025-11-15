package com.cafecol.resources;

import com.cafecol.service.NotificacionService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    @EJB
    private NotificacionService notificacionService;

    @POST
    @Path("pedido-gestionado")
    public Response pedidoGestionado(EmailNotificacionDTO dto) {
        try {
            notificacionService.enviarCorreoGestionado(dto.correo, dto.nombre, dto.idPedido, dto.guia);
            return Response.ok().entity("{\"mensaje\": \"Correo enviado\"}").build();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"mensaje\": \"No se pudo enviar el correo\"}").build();
        }
    }

    public static class EmailNotificacionDTO {
        public String correo;
        public String nombre;
        public int idPedido;
        public String guia;
    }
}
