// assets/js/productos.js
// ===========================
// Manejo de listado de productos y agregado al carrito
// ===========================

document.addEventListener('DOMContentLoaded', async () => {
  const grid = document.getElementById('productosGrid');
  const spinner = document.getElementById('productosSpinner');
  const alertBox = document.getElementById('productosAlert');
  const DEFAULT_IMG = 'assets/img/cafe_default_img.jpg';

  try {
    // Cargar productos desde API
    const resp = await fetch(`${window.API_BASE_URL}/productos`);
    if (!resp.ok) throw new Error('HTTP ' + resp.status);
    const productos = await resp.json();

    if (!Array.isArray(productos) || productos.length === 0) {
      grid.innerHTML = '<p class="text-muted">No hay productos disponibles.</p>';
      return;
    }

    const frag = document.createDocumentFragment();
    productos.forEach(p => {
      const col = document.createElement('div');
      col.className = 'col-12 col-sm-6 col-md-4 col-lg-3';
      col.innerHTML = `
        <div class="card h-100 shadow-sm" data-id-producto="${p.idProducto ?? ''}">
          <img
            src="${DEFAULT_IMG}"
            class="card-img-top"
            alt="${escapeHtml(p?.nombre ?? 'Producto')}"
            loading="lazy"
            style="width:100%; height: 160px; object-fit: cover;"
          >
          <div class="card-body d-flex flex-column">
            <h5 class="card-title mb-1">${escapeHtml(p.nombre ?? '')}</h5>
            <p class="card-text text-muted small mb-2">${escapeHtml(p.descripcion ?? '')}</p>
            <div class="mt-auto">
              <div class="fw-semibold">Precio: $${formatPrice(p.precio)}</div>
              <div class="text-${(p.stock ?? 0) > 0 ? 'success' : 'danger'} small">Stock: ${p.stock ?? 0}</div>

              <!-- Controles de cantidad -->
              <div class="input-group input-group-sm mt-2">
                <button class="btn btn-outline-secondary btn-decrease" type="button">−</button>
                <input type="number" class="form-control text-center cantidad-input" value="1" min="1" max="${p.stock ?? 1}">
                <button class="btn btn-outline-secondary btn-increase" type="button">+</button>
              </div>

              <button class="btn btn-primary btn-sm w-100 mt-2 btn-agregar" ${(p.stock ?? 0) > 0 ? '' : 'disabled'}>Agregar</button>
            </div>
          </div>
        </div>
      `;
      frag.appendChild(col);
    });
    grid.appendChild(frag);

    // Delegación de eventos
    grid.addEventListener('click', async (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;

      const card = btn.closest('.card');
      if (!card) return;

      // Aumentar o disminuir cantidad
      const input = card.querySelector('.cantidad-input');
      if (btn.classList.contains('btn-decrease')) {
        input.value = Math.max(1, parseInt(input.value) - 1);
        return;
      }
      if (btn.classList.contains('btn-increase')) {
        input.value = Math.min(parseInt(input.max), parseInt(input.value) + 1);
        return;
      }

      // Agregar al carrito
      if (btn.classList.contains('btn-agregar')) {
        const idProducto = Number(card.dataset.idProducto);
        const cantidad = Number(input.value);

        btn.disabled = true;
        btn.textContent = 'Agregando...';

        try {
          const resp = await fetch(`${window.API_BASE_URL}/carrito/items`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ idProducto, cantidad })
          });

          if (!resp.ok) throw new Error('HTTP ' + resp.status);
          const carrito = await resp.json();

          window.actualizarBadgeCarrito?.(carrito);
          window.renderCarrito?.(carrito);

          btn.textContent = '✓ Agregado';
          setTimeout(() => {
            btn.textContent = 'Agregar';
            btn.disabled = false;
          }, 1000);
        } catch (err) {
          console.error(err);
          alert('No se pudo agregar al carrito. Verifica el stock disponible.');
          btn.textContent = 'Agregar';
          btn.disabled = false;
        }
      }
    });

  } catch (e) {
    console.error(e);
    alertBox.classList.remove('d-none');
  } finally {
    spinner.classList.add('d-none');
  }

  // ===========================
  // Funciones auxiliares
  // ===========================
  function formatPrice(v) {
    const n = Number(v);
    return Number.isFinite(n) ? n.toFixed(2) : '0.00';
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, m => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[m]));
  }
});
