// ===========================
// Archivo: assets/js/api-config.js
// Descripción: Configuración centralizada de endpoints de API
// ===========================
(function () {
  const segs = window.location.pathname.split('/').filter(Boolean);
  const ctx = segs.length > 0 ? `/${segs[0]}` : '';
  const origin = window.location.origin;

  // Base API dinámica
  window.API_BASE_URL = `${origin}${ctx}/api`;

  // ===========================
  // Autenticación
  // ===========================
  window.AUTH_API = {
    login: `${API_BASE_URL}/auth/login`,
    logout: `${API_BASE_URL}/auth/logout`
  };

  // ===========================
  // Usuarios
  // ===========================
  window.USUARIOS_API = {
    getAll: `${API_BASE_URL}/usuarios`,
    getById: (id) => `${API_BASE_URL}/usuarios/${id}`,
    create: `${API_BASE_URL}/usuarios`,
    update: (id) => `${API_BASE_URL}/usuarios/${id}`,
    delete: (id) => `${API_BASE_URL}/usuarios/${id}`
  };

  // ===========================
  // Productos
  // ===========================
  window.PRODUCTOS_API = {
    getAll: `${API_BASE_URL}/productos`,
    getById: (id) => `${API_BASE_URL}/productos/${id}`,
    create: `${API_BASE_URL}/productos`,
    update: (id) => `${API_BASE_URL}/productos/${id}`,
    delete: (id) => `${API_BASE_URL}/productos/${id}`
  };

  // ===========================
  // Clientes
  // ===========================
  window.CLIENTES_API = {
    getAll: `${API_BASE_URL}/clientes`,
    getById: (id) => `${API_BASE_URL}/clientes/${id}`,
    create: `${API_BASE_URL}/clientes`,
    update: (id) => `${API_BASE_URL}/clientes/${id}`,
    delete: (id) => `${API_BASE_URL}/clientes/${id}`,
    buscarPorNombre: (nombre) => `${API_BASE_URL}/clientes/buscar?nombre=${encodeURIComponent(nombre)}`
  };

  // ===========================
  // Pedidos
  // ===========================
  window.PEDIDOS_API = {
    getAll: `${API_BASE_URL}/pedidos`,
    getById: (id) => `${API_BASE_URL}/pedidos/${id}`,
    create: `${API_BASE_URL}/pedidos`,
    update: (id) => `${API_BASE_URL}/pedidos/${id}`,
    delete: (id) => `${API_BASE_URL}/pedidos/${id}`,
    // Nuevo: actualizar solo estado (y opcionalmente guía si se soporta)
    updateEstado: (id) => `${API_BASE_URL}/pedidos/${id}/estado`
  };

  // ===========================
  // Notificaciones (correo)
  // ===========================
  window.NOTIFICACIONES_API = {
    // Nuevo: correo al pasar pedido a GESTIONADO
    pedidoGestionado: `${API_BASE_URL}/notificaciones/pedido-gestionado`
  };

  // ===========================
  // Carrito
  // ===========================
  window.CARRITO_API = {
    get: `${API_BASE_URL}/carrito`,
    addItem: `${API_BASE_URL}/carrito/items`,
    removeItem: (id) => `${API_BASE_URL}/carrito/items/${id}`,
    vaciar: `${API_BASE_URL}/carrito/vaciar`,
    checkout: `${API_BASE_URL}/carrito/checkout`
  };
})();
