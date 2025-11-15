package com.cafecol.resources;

import com.cafecol.entities.Cliente;
import com.cafecol.entities.Pedido;
import com.cafecol.service.CarritoService;
import com.cafecol.service.ClienteService;
import com.cafecol.service.PedidoService;

import jakarta.ejb.EJB;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

@Path("/epayco")
public class EpaycoResource {

    @EJB
    private PedidoService pedidoService;

    @EJB
    private CarritoService carritoService;

    @EJB
    private ClienteService clienteService;

    private static final Logger LOG = Logger.getLogger(EpaycoResource.class.getName());

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    @POST
    @Path("/confirmacion")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response recibirConfirmacion(
            // Firma y campos de validación
            @FormParam("x_signature") String x_signature,
            @FormParam("x_ref_payco") String x_ref_payco,
            @FormParam("x_transaction_id") String x_transaction_id,
            @FormParam("x_amount") String x_amount,
            @FormParam("x_currency_code") String x_currency_code,
            @FormParam("x_response") String x_response,
            @FormParam("x_cod_response") String x_cod_response,
            @FormParam("x_test_request") String x_test_request,

            // Extras definidos en el checkout
            @FormParam("x_extra1") String x_extra1, // idCliente o "0"
            @FormParam("x_extra2") String x_extra2, // idCarrito o payload según tu diseño

            // Datos del cliente reenviados por ePayco
            @FormParam("x_customer_name") String x_customer_name,
            @FormParam("x_customer_lastname") String x_customer_lastname,
            @FormParam("x_customer_email") String x_customer_email,
            @FormParam("x_customer_phone") String x_customer_phone,
            @FormParam("x_customer_address") String x_customer_address
    ) {

        try {
            LOG.info("=== CONFIRMACION EPAYCO ===");
            LOG.info("ref: " + x_ref_payco + " tx: " + x_transaction_id + " resp: " + x_response + " cod: " + x_cod_response);

            // 1) Validación mínima
            if (isBlank(x_signature) || isBlank(x_ref_payco) || isBlank(x_transaction_id) ||
                isBlank(x_amount) || isBlank(x_currency_code)) {
                LOG.warning("Campos de firma incompletos");
                return Response.status(400).entity("missing fields").build();
            }

            // 2) Validar firma: sha256(p_cust_id_cliente ^ p_key ^ x_ref_payco ^ x_transaction_id ^ x_amount ^ x_currency_code)
            String p_cust_id_cliente = System.getenv("EPAYCO_P_CUST_ID_CLIENTE");
            String p_key = System.getenv("EPAYCO_P_KEY");
            if (isBlank(p_cust_id_cliente) || isBlank(p_key)) {
                LOG.severe("Variables de entorno EPAYCO no configuradas");
                return Response.serverError().entity("server config error").build();
            }
            String toSign = String.join("^", p_cust_id_cliente, p_key, x_ref_payco, x_transaction_id, x_amount, x_currency_code);
            String localSignature = sha256Hex(toSign);
            if (!Objects.equals(localSignature, x_signature)) {
                LOG.warning("Firma inválida");
                return Response.status(400).entity("bad signature").build();
            }

            // 3) Solo continuar si aprobada
            boolean aprobada = "Aceptada".equalsIgnoreCase(String.valueOf(x_response)) ||
                               "1".equals(String.valueOf(x_cod_response));
            if (!aprobada) {
                LOG.info("Transacción no aprobada: " + x_response + " (" + x_cod_response + ")");
                return Response.ok("ignored").build();
            }

            // 4) Resolver/crear cliente
            int idClienteInt = 0;
            try { idClienteInt = Integer.parseInt(x_extra1); } catch (Exception ignore) {}
            Cliente cliente = null;
            if (idClienteInt > 0) {
                cliente = clienteService.findById(idClienteInt);
            }
            if (cliente == null) {
                cliente = new Cliente();
                cliente.setNombre((x_customer_name == null ? "" : x_customer_name).trim());
                cliente.setApellido((x_customer_lastname == null ? "" : x_customer_lastname).trim());
                cliente.setCorreo((x_customer_email == null ? "" : x_customer_email).trim().toLowerCase(Locale.ROOT));
                cliente.setTelefono((x_customer_phone == null ? "" : x_customer_phone).trim());
                cliente.setDireccion((x_customer_address == null ? "" : x_customer_address).trim());
                cliente = clienteService.create(cliente); // permitidos duplicados
                idClienteInt = cliente.getIdCliente();
                LOG.info("Cliente creado: " + idClienteInt);
            }

            // 5) x_extra2: usa aquí el ID del carrito (tu diseño actual)
            int idCarritoInt;
            try {
                idCarritoInt = Integer.parseInt(x_extra2);
            } catch (Exception e) {
                LOG.severe("x_extra2 inválido, se esperaba idCarrito");
                return Response.status(400).entity("invalid extra2").build();
            }

            // 6) Crear pedido desde carrito y vaciar carrito
            Pedido pedido = pedidoService.crearPedidoDesdeCarritoId(idClienteInt, idCarritoInt);
            carritoService.vaciarCarrito(idCarritoInt);

            // 7) (Opcional) adjunta metadatos del pago si tu entidad los soporta
            // pedido.setReferenciaPago(x_ref_payco);
            // pedido.setTransaccionId(x_transaction_id);
            // pedido.setMoneda(x_currency_code);
            // pedido.setTotal(Double.parseDouble(x_amount));
            // pedido.setEstado("APROBADO");
            // pedidoService.update(pedido);

            LOG.info("Pedido creado correctamente: " + pedido.getIdPedido());
            return Response.ok("OK").build();

        } catch (Exception e) {
            LOG.severe("Error procesando confirmación: " + e.getMessage());
            e.printStackTrace();
            return Response.serverError().entity("ERROR").build();
        }
    }
}
