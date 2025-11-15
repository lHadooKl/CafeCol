// assets/js/admin-stats.js
(function () {
  const elTotalPedidos = document.getElementById('kpiTotalPedidos');
  const elTotalVentas  = document.getElementById('kpiTotalVentas');
  const elUltimo       = document.getElementById('kpiFechaUltimo');

  // Formato de moneda en COP
  const nfCOP = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0
  });

  // Formato de fecha en Colombia (zona fija para evitar desfases)
  const dfCO = new Intl.DateTimeFormat('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: 'America/Bogota'
  });

  // Normaliza posibles URLs como "/index_admin.html/api/pedidos" -> "/api/pedidos"
  function normalizeApiUrl(u) {
    try {
      const url = new URL(u, window.location.origin);
      url.pathname = url.pathname.replace(/\/[^/]*\.html(?=\/api\b)/, '');
      return url.toString().replace(/\/+$/, '');
    } catch {
      return u;
    }
  }

  // Candidatos sin tocar api-config.js
  function candidatesPedidos() {
    const origin = window.location.origin;
    const list = [];
    if (window?.PEDIDOS_API?.getAll) {
      list.push(String(window.PEDIDOS_API.getAll));
      list.push(normalizeApiUrl(String(window.PEDIDOS_API.getAll)));
    }
    list.push(new URL('/api/pedidos', origin).toString().replace(/\/+$/, ''));
    return Array.from(new Set(list));
  }

  // GET fresco con cache-busting
  async function fetchJsonFresh(url) {
    const href = url.includes('?') ? `${url}&t=${Date.now()}` : `${url}?t=${Date.now()}`;
    const resp = await fetch(href, { cache: 'no-store' });
    if (!resp.ok) throw new Error('HTTP ' + resp.status);
    return await resp.json();
  }

  // Convierte respuesta a arreglo (plano o paginado)
  function coerceArray(data) {
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    if (data && Array.isArray(data.items)) return data.items;
    return [];
  }

  // Total por pedido: usa p.total o suma detalles.subtotal
  function totalPedido(p) {
    const t = Number(p?.total);
    if (Number.isFinite(t)) return t;
    const detalles = Array.isArray(p?.detalles) ? p.detalles : [];
    return detalles.reduce((acc, d) => acc + (Number(d?.subtotal) || 0), 0);
  }

  // Parse seguro: 'YYYY-MM-DD' como fecha local para evitar salto por UTC
  function parseFechaPedido(f) {
    if (typeof f === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(f)) {
      const [y, m, d] = f.split('-').map(Number);
      return new Date(y, m - 1, d); // local midnight
    }
    return new Date(f); // ISO con hora → dejar que JS maneje
  }

  async function cargarKPIs() {
    const urls = candidatesPedidos();
    let pedidos = [];
    let lastErr = null;

    for (const url of urls) {
      try {
        const data = await fetchJsonFresh(url);
        pedidos = coerceArray(data);
        break;
      } catch (e) {
        lastErr = e;
      }
    }

    if (!Array.isArray(pedidos)) pedidos = [];

    const totalPedidos = pedidos.length;
    const totalVentas = pedidos.reduce((acc, p) => acc + totalPedido(p), 0);
    const ultimoTime = pedidos
      .map(p => parseFechaPedido(p?.fecha))
      .filter(d => Number.isFinite(d?.getTime()))
      .reduce((max, d) => Math.max(max, d.getTime()), 0);

    elTotalPedidos.textContent = String(totalPedidos);
    elTotalVentas.textContent  = nfCOP.format(totalVentas);
    elUltimo.textContent       = ultimoTime ? dfCO.format(new Date(ultimoTime)) : '—';
  }

  if (document.readyState !== 'loading') {
    cargarKPIs();
  } else {
    document.addEventListener('DOMContentLoaded', cargarKPIs);
  }
})();
