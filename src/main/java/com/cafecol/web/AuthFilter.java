// src/main/java/com/cafecol/web/AuthFilter.java
package com.cafecol.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String cp = request.getContextPath();
    String path = request.getRequestURI();

    boolean publico =
      path.startsWith(cp + "/index.html") ||
      path.equals(cp + "/") ||
      path.startsWith(cp + "/api/productos") ||
      path.startsWith(cp + "/api/carrito") ||
      path.startsWith(cp + "/api/auth/login") ||
      path.startsWith(cp + "/api/auth/logout") ||
      path.startsWith(cp + "/login.html") ||
      path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") ||
      path.endsWith(".jpg") || path.endsWith(".ico") || path.endsWith(".svg") || path.endsWith(".webp");

    boolean adminOnly =
      path.startsWith(cp + "/api/usuarios") ||
      path.startsWith(cp + "/usuarios") ||
      path.startsWith(cp + "/admin") ||
      path.startsWith(cp + "/index_admin.html") ||
      path.startsWith(cp + "/productos-admin.html") ||
      path.startsWith(cp + "/pedidos-admin.html");

    if (publico || !adminOnly) {
      chain.doFilter(req, res);
      return;
    }

    HttpSession session = request.getSession(false);
    boolean logueado = (session != null && session.getAttribute("usuarioId") != null);
    String rol = (session != null && session.getAttribute("usuarioRol") != null)
                  ? String.valueOf(session.getAttribute("usuarioRol")) : null;

    if (!logueado) {
      response.sendRedirect(cp + "/login.html");
      return;
    }
    if (rol == null || !rol.equalsIgnoreCase("ADMIN")) {
      if (path.startsWith(cp + "/api/")) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido a ADMIN");
      } else {
        response.sendRedirect(cp + "/index.html");
      }
      return;
    }

    chain.doFilter(req, res);
  }
}
