// assets/js/admin-top-productos.js
(function () {
  const canvas = document.getElementById('chartTopProductos');
  const wrapper = document.getElementById('chartTopProductosWrapper');
  const emptyEl = document.getElementById('topProductosEmpty');
  const rbUnidades = document.getElementById('metricUnidades');
  const rbValor    = document.getElementById('metricValor');
  const selectTopN = document.getElementById('selectTopN');

  if (!canvas) return;

  // Paleta verde consistente con el tema
  const GREEN_BG = 'rgba(20, 184, 106, 0.6)';   // #14b86a con 60% [coincide con CSS]
  const GREEN_BORDER = 'rgba(20, 184, 106, 1)';

  function normalizeApiUrl(u) {
    try {
      const url = new URL(u, window.location.origin);
      url.pathname = url.pathname.replace(/\/[^/]*\.html(?=\/api\b)/, '');
      return url.toString().replace(/\/+$/, '');
    } catch { return u; }
  }

  function candidatesDetalles() {
    const origin = window.location.origin;
    const list = [];
    if (window?.API_BASE_URL) {
      list.push(`${window.API_BASE_URL}/detalles-pedido`);
      list.push(normalizeApiUrl(`${window.API_BASE_URL}/detalles-pedido`));
    }
    list.push(new URL('/api/detalles-pedido', origin).toString().replace(/\/+$/, ''));
    return Array.from(new Set(list));
  }

  async function fetchJsonFresh(url) {
    const href = url.includes('?') ? `${url}&t=${Date.now()}` : `${url}?t=${Date.now()}`;
    const resp = await fetch(href, { cache: 'no-store' });
    if (!resp.ok) throw new Error('HTTP ' + resp.status);
    return await resp.json();
  }

  function coerceArray(data) {
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    if (data && Array.isArray(data.items)) return data.items;
    return [];
  }

  function aggregateByProduct(detalles) {
    const acc = new Map();
    for (const d of detalles) {
      const prod = d?.producto || {};
      const id   = prod?.idProducto ?? d?.idProducto ?? d?.productoId ?? null;
      if (id == null) continue;
      const nombre = prod?.nombre || d?.nombreProducto || `Producto ${id}`;
      const cant   = Number(d?.cantidad) || 0;
      const subtotal = Number(d?.subtotal);
      const precio  = Number(d?.precioUnitario);
      const valor = Number.isFinite(subtotal) ? subtotal : (Number.isFinite(precio) ? precio * cant : 0);
      if (!acc.has(id)) acc.set(id, { id, nombre, unidades: 0, valor: 0 });
      const row = acc.get(id);
      row.unidades += cant;
      row.valor    += valor;
    }
    return Array.from(acc.values());
  }

  const nfCOP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  function setWrapperHeightForRows(wrapperEl, rowsCount) {
    if (!wrapperEl) return;
    const base = 80;
    const perRow = 36;
    const maxH = 600;
    const minH = 220;
    const h = Math.max(minH, Math.min(maxH, base + perRow * rowsCount));
    wrapperEl.style.height = `${h}px`;
  }

  let chart;
  function renderChart(rows, metric = 'unidades', topN = 10) {
    const sorted = rows
      .slice()
      .sort((a, b) => (b[metric] || 0) - (a[metric] || 0))
      .slice(0, topN);

    const labels = sorted.map(r => r.nombre);
    const data   = sorted.map(r => Math.round(r[metric] || 0));

    setWrapperHeightForRows(wrapper, labels.length);
    emptyEl?.classList.toggle('d-none', data.length > 0);

    const datasetLabel = metric === 'valor' ? 'Valor (COP)' : 'Unidades';

    const cfg = {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: datasetLabel,
          data,
          backgroundColor: GREEN_BG,   // siempre verde
          borderColor: GREEN_BORDER,
          borderWidth: 1,
          maxBarThickness: 32
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        resizeDelay: 100,
        scales: {
          x: {
            beginAtZero: true,
            ticks: {
              color: '#e8ecf1', // texto claro sobre oscuro
              precision: 0,
              callback: (value) => (metric === 'valor' ? nfCOP.format(value) : value)
            },
            grid: {
              color: 'rgba(255,255,255,0.08)'
            }
          },
          y: {
            ticks: {
              color: '#e8ecf1',
              autoSkip: false
            },
            grid: {
              color: 'rgba(255,255,255,0.08)'
            }
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const v = ctx.parsed.x;
                return metric === 'valor' ? nfCOP.format(v) : `${v} und`;
              }
            }
          },
          legend: { display: false }
        }
      }
    };

    if (chart) {
      chart.data.labels = labels;
      chart.data.datasets[0].label = datasetLabel;
      chart.data.datasets[0].data = data;
      chart.update();
    } else {
      chart = new Chart(canvas, cfg);
    }
  }

  async function init() {
    const urls = candidatesDetalles();
    let detalles = [];
    for (const u of urls) {
      try { detalles = coerceArray(await fetchJsonFresh(u)); break; }
      catch { /* probar siguiente */ }
    }
    const rows = aggregateByProduct(detalles);
    const metric = rbValor?.checked ? 'valor' : 'unidades';
    const topN = Number(selectTopN?.value || 10);
    renderChart(rows, metric, topN);

    rbUnidades?.addEventListener('change', () => renderChart(rows, 'unidades', Number(selectTopN.value)));
    rbValor?.addEventListener('change', () => renderChart(rows, 'valor', Number(selectTopN.value)));
    selectTopN?.addEventListener('change', () => {
      const m = rbValor.checked ? 'valor' : 'unidades';
      renderChart(rows, m, Number(selectTopN.value));
    });
  }

  if (document.readyState !== 'loading') init();
  else document.addEventListener('DOMContentLoaded', init);
})();
