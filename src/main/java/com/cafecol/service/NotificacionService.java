package com.cafecol.service;

import jakarta.ejb.Stateless;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Stateless
public class NotificacionService {

    // Cambia esto a tus datos de SMTP reales:
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587; // o 465 para SSL
    private static final String SMTP_USERNAME = "tu_cuenta@gmail.com"; // tu correo de envío
    private static final String SMTP_PASSWORD = "tu_password_app"; // usa clave de aplicación

    private static final String FROM_ALIAS = "CafeCol";
    private static final String FROM_EMAIL = SMTP_USERNAME;

    public void enviarCorreoGestionado(String correo, String nombre, int idPedido, String guia) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        String asunto = "CafeCol - Tu pedido #" + idPedido + " está siendo gestionado";
        String cuerpo =
        "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 400px; color: #181613; background: #f7f3eb; border-radius:12px; padding:32px 16px; box-shadow:0 4px 20px rgba(0,0,0.,.06);\">" +
            "<h2 style=\"color:#c9a46a; margin-top:0; margin-bottom:12px; letter-spacing:0.02em;\">¡Hola " + nombre + "!</h2>" +
            "<p style=\"font-size: 1.1em; margin-bottom:14px;\">" +
              "Tu pedido <strong style='color:#915f1b;'>#" + idPedido + "</strong> está siendo <b>gestionado</b>." +
            "</p>" +
            "<div style=\"background: #fffbe9; border: 1px solid #f0e6d2; border-radius:8px; padding:18px 16px; margin-bottom:14px;\">" +
              "<span style=\"color:#a18454; font-size:1em;\">Número de guía:</span><br>" +
              "<span style=\"display:inline-block; margin-top:6px;color:#181613; font-size:1.2em; font-weight:bold;\">" + guia + "</span>" +
            "</div>" +
            "<p>Te notificaremos cuando realicemos el envío a tu dirección registrada.</p>" +
            "<p style=\"margin-top:24px; color:#6d625a; font-size:.99em;\">Gracias por confiar en <strong style=\"color:#c9a46a;\">CafeCol</strong>.</p>" +
            "<hr style=\"border:0; border-top:1.5px solid #eee; margin:28px 2px 8px 2px;\">" +
            "<div style=\"font-size: 0.92em; color:#b3ada5; text-align:center;\">No respondas a este correo automático.</div>" +
        "</div>";


        Message mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(FROM_EMAIL, FROM_ALIAS));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(correo));
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        Transport.send(mensaje);
    }
}
