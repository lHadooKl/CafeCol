(function () {
  /* -------------------------------------------------------
     HOOK GLOBAL PARA INTERCEPTAR FETCH
  ---------------------------------------------------------*/
  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input, init = {}) => {
    const res = await originalFetch(input, init).catch(e => {
      console.error("Fetch interceptado → error:", e);
      throw e;
    });

    try {
      const url = typeof input === "string" ? input : input?.url || "";
      const method = (init?.method || "GET").toUpperCase();

      if (
        url.includes(`${window.API_BASE_URL}/carrito`) &&
        res.ok &&
        method !== "GET"
      ) {
        queueMicrotask(() =>
          document.dispatchEvent(new Event("carritoActualizado"))
        );
      }
    } catch (err) {
      console.warn("Error en intercept:", err);
    }

    return res;
  };


  /* -------------------------------------------------------
     ELEMENTOS DOM Y HELPERS
  ---------------------------------------------------------*/
  const $ = (sel) => document.querySelector(sel);

  const lista         = $("#carritoLista");
  const totalEl       = $("#carritoTotal");
  const alertBox      = $("#carritoAlert");
  const badge         = $("#cartBadge");
  const btnVaciar     = $("#btnVaciar");
  const btnCheckout   = $("#btnCheckout");
  const resumenList   = $("#resumenCarritoList");
  const resumenTotal  = $("#resumenCarritoTotal");
  const modalCliente  = $("#modalCliente");
  const offcanvas     = $("#offcanvasCarrito");

  const formatPrice = v => (isFinite(+v) ? (+v).toFixed(2) : "0.00");
  const escapeHtml = s =>
    String(s || "").replace(/[&<>"']/g, m =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m])
    );

  window.CarritoUI = {
    refresh: cargarCarrito,
    notify: () => document.dispatchEvent(new Event("carritoActualizado"))
  };

  document.addEventListener("carritoActualizado", cargarCarrito);


  /* -------------------------------------------------------
     FETCH DEL CARRITO (CENTRALIZADO)
  ---------------------------------------------------------*/
  async function fetchCarrito() {
    const url = `${window.API_BASE_URL}/carrito?t=${Date.now()}`;
    const r = await fetch(url, { cache: "no-store" });

    if (!r.ok) throw new Error("HTTP " + r.status);
    if (r.status === 204) return { items: [], total: 0 };

    return await r.json();
  }


  /* -------------------------------------------------------
     RENDER PRINCIPAL DEL CARRITO
  ---------------------------------------------------------*/
  function renderCarrito(data) {
    lista.innerHTML = "";

    if (!data.items?.length) {
      lista.innerHTML = `<li class="list-group-item text-muted">Tu carrito está vacío.</li>`;
      totalEl.textContent = "$0.00";
      return;
    }

    const frag = document.createDocumentFragment();

    for (const it of data.items) {
      const li = document.createElement("li");
      li.className =
        "list-group-item d-flex justify-content-between align-items-start";
      li.dataset.idItem = it.idItem;
      li.dataset.cantidad = it.cantidad;
      li.dataset.subtotal = it.subtotal;

      li.innerHTML = `
        <div class="me-auto">
          <div class="fw-semibold">${escapeHtml(it.nombre)}</div>
          <small class="text-muted">${escapeHtml(it.descripcion)}</small>
          <div class="small mt-1">
            Precio: $${formatPrice(it.precioUnitario)} · Cant: ${it.cantidad} · Subtotal: $${formatPrice(it.subtotal)}
          </div>
        </div>
        <button class="btn btn-sm btn-outline-danger ms-3" data-action="quitar">
          Quitar
        </button>`;

      frag.appendChild(li);
    }

    lista.appendChild(frag);
    totalEl.textContent = `$${formatPrice(data.total)}`;
  }


  /* -------------------------------------------------------
     RENDER RESUMEN (MODAL)
  ---------------------------------------------------------*/
  function renderResumen(data) {
    if (!resumenList) return;
    resumenList.innerHTML = "";

    if (!data.items?.length) {
      resumenList.innerHTML = `<li class="list-group-item text-muted">Sin productos.</li>`;
      resumenTotal.textContent = "$0.00";
      return;
    }

    const frag = document.createDocumentFragment();

    for (const it of data.items) {
      const li = document.createElement("li");
      li.className =
        "list-group-item d-flex justify-content-between align-items-center";
      li.innerHTML = `
        <span>${escapeHtml(it.nombre)} × ${it.cantidad}</span>
        <span>$${formatPrice(it.subtotal)}</span>`;
      frag.appendChild(li);
    }

    resumenList.appendChild(frag);
    resumenTotal.textContent = `$${formatPrice(data.total)}`;
  }


  /* -------------------------------------------------------
     BADGE
  ---------------------------------------------------------*/
  function actualizarBadge(data) {
    const count = data.items?.reduce((acc, it) => acc + it.cantidad, 0) || 0;
    if (badge) badge.textContent = String(count);
  }


  /* -------------------------------------------------------
     CARGAR CARRITO
  ---------------------------------------------------------*/
  async function cargarCarrito() {
    try {
      const data = await fetchCarrito();
      renderCarrito(data);
      actualizarBadge(data);

      if (modalCliente?.classList.contains("show")) {
        renderResumen(data);
      }

      alertBox?.classList.add("d-none");

    } catch (err) {
      console.error("Error al cargar carrito:", err);
      alertBox?.classList.remove("d-none");
    }
  }


  /* -------------------------------------------------------
     QUITAR ITEM
  ---------------------------------------------------------*/
  async function quitarItem(id) {
    try {
      await fetch(`${window.API_BASE_URL}/carrito/items/${id}`, {
        method: "DELETE",
        cache: "no-store"
      });

      cargarCarrito();
    } catch (err) {
      console.error("Error quitando item:", err);
      alert("No se pudo quitar el producto.");
    }
  }


  /* -------------------------------------------------------
     VACIAR CARRITO
  ---------------------------------------------------------*/
  async function vaciarCarrito() {
    try {
      await fetch(`${window.API_BASE_URL}/carrito/vaciar`, {
        method: "DELETE",
        cache: "no-store"
      });

      cargarCarrito();
    } catch (err) {
      console.error("Error vaciando carrito:", err);
      alert("No se pudo vaciar el carrito.");
    }
  }


  /* -------------------------------------------------------
     CHECKOUT (ahora: solo dispara ePayco, NO crea pedido)
  ---------------------------------------------------------*/
  async function hacerCheckout() {
    // Conservamos la función para compatibilidad (no la usamos para crear pedido antes del pago)
    const idCliente = Number(localStorage.getItem("clienteId"));
    if (!idCliente) return alert("Debes registrar un cliente primero.");

    try {
      const carrito = await fetchCarrito();
      if (!carrito.items?.length) return alert("Tu carrito está vacío.");

      // Si por alguna razón necesitas crear un pedido localmente, puedes hacerlo aquí.
      // Pero en el flujo con ePayco, NO guardamos el pedido desde el front.
      alert("Preparado para pagar con ePayco.");
    } catch (err) {
      console.error("Checkout:", err);
      alert("No se pudo completar la acción.");
    }
  }


  /* -------------------------------------------------------
     EVENTOS
  ---------------------------------------------------------*/
  lista?.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action='quitar']");
    if (!btn) return;

    const li = btn.closest("li");
    const id = Number(li.dataset.idItem);

    quitarItem(id);
  });

  btnVaciar?.addEventListener("click", () => {
    if (confirm("¿Vaciar el carrito?")) vaciarCarrito();
  });

  btnCheckout?.addEventListener("click", async () => {
    try {
      const carrito = await fetchCarrito();

      // Validacion Carrito vacío
      if (!carrito.items || carrito.items.length === 0) {
        const modalVacioEl = document.getElementById("modalCarritoVacio");
        if (modalVacioEl && window.bootstrap?.Modal) {
          const modalVacio = bootstrap.Modal.getOrCreateInstance(modalVacioEl);
          modalVacio.show();
        }
        return;
      }

      // ✔️ Si NO existe cliente → abrir modalCliente
      if (!localStorage.getItem("clienteId")) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalCliente);
        modal.show();

        // Cuando el cliente se registre, el flujo puede volver a pulsar el botón de pagar
        document.addEventListener(
          "clienteCreado",
          () => {
            // Después de crear cliente, lanzar el botón de ePayco para iniciar pago
            const btn = document.getElementById("btnPagarEpayco");
            if (btn) btn.click();
          },
          { once: true }
        );
        return;
      }

      // ✔️ Cliente existe → lanzamos el flujo de ePayco (botón dedicado)
      const btnPagar = document.getElementById("btnPagarEpayco");
      if (btnPagar) {
        btnPagar.click();
      } else {
        // Si no existe el botón (por algún motivo), mostramos mensaje
        alert("Botón de pago no disponible. Intente recargar la página.");
      }

    } catch (err) {
      console.error("Error en checkout:", err);
      alert("No se pudo iniciar el proceso de compra.");
    }
  });


  offcanvas?.addEventListener("shown.bs.offcanvas", cargarCarrito);

  modalCliente?.addEventListener("shown.bs.modal", async () => {
    try {
      renderResumen(await fetchCarrito());
    } catch {
      resumenList.innerHTML =
        `<li class="list-group-item text-muted">Error al cargar resumen.</li>`;
      resumenTotal.textContent = "$0.00";
    }
  });

  document.addEventListener("DOMContentLoaded", cargarCarrito);
})();
