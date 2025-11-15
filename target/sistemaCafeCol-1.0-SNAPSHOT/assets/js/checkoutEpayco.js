// checkoutEpayco.js
document.addEventListener("DOMContentLoaded", () => {
  const btnPagar = document.getElementById("btnPagarEpayco");
  if (!btnPagar) return;

  // Bloqueo de doble click
  let pagoEnProgreso = false;

  // Helpers
  const postJson = async (url, body) => {
    const r = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (!r.ok) {
      const t = await r.text().catch(()=>"");
      throw new Error(`HTTP ${r.status} ${t}`);
    }
    return r.json();
  };

  const getCarrito = async () => {
    const r = await fetch(`${window.API_BASE_URL}/carrito?t=${Date.now()}`, { cache: "no-store" });
    if (!r.ok) throw new Error("No se pudo obtener el carrito");
    return r.json();
  };

  async function ensureCliente() {
    let idCliente = Number(localStorage.getItem("clienteId") || 0);
    if (idCliente > 0) return idCliente;

    // Datos visibles del modal
    const nombre = document.getElementById("nombre")?.value || "";
    const apellido = document.getElementById("apellido")?.value || "";
    const correo = document.getElementById("correo")?.value || "";
    const telefono = document.getElementById("telefono")?.value || "";
    const direccion = document.getElementById("direccion")?.value || "";

    // Crear cliente antes del pago
    const nuevo = await postJson(`${window.API_BASE_URL}/clientes`, {
      nombre,
      apellido,
      correo,
      telefono,
      direccion
    }); // Crea y retorna el cliente [web:2]

    idCliente = Number(nuevo.idCliente);
    if (!idCliente) throw new Error("Cliente inválido");
    localStorage.setItem("clienteId", String(idCliente));
    document.dispatchEvent(new Event("clienteCreado"));
    return idCliente;
  }

  async function crearPedidoPendienteConDetalles(idCliente, carrito) {
    if (!carrito.items || carrito.items.length === 0) {
      pagoEnProgreso = false;
      btnPagar.disabled = false;
      btnPagar.classList.remove("disabled");
      throw new Error("Carrito vacío");
    }

    const detalles = carrito.items.map(i => {
      if (!i.idProducto) {
        pagoEnProgreso = false;
        btnPagar.disabled = false;
        btnPagar.classList.remove("disabled");
        throw new Error(`Item sin idProducto: ${i?.nombre || "desconocido"}`);
      }
      return {
        cantidad: i.cantidad,
        precioUnitario: i.precioUnitario,
        descripcion: i.nombre,
        producto: { idProducto: i.idProducto }
      };
    });

    const referencia = "FAC-" + Date.now();

    // Crear pedido PENDIENTE con detalles
    const resp = await fetch(`${window.API_BASE_URL}/pedidos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        cliente: { idCliente },
        estado: "PENDIENTE",
        referencia,
        detalles
      })
    });
    if (!resp.ok) {
      pagoEnProgreso = false;
      btnPagar.disabled = false;
      btnPagar.classList.remove("disabled");
      const t = await resp.text().catch(()=>"");
      throw new Error(`No se pudo crear pedido: ${t}`);
    }
    const data = await resp.json();
    const pedido = data.pedido || data;
    if (!pedido?.idPedido) {
      pagoEnProgreso = false;
      btnPagar.disabled = false;
      btnPagar.classList.remove("disabled");
      throw new Error("Pedido no retornó idPedido");
    }
    return pedido;
  }

  btnPagar.addEventListener("click", async function (event) {
    event.preventDefault();
    event.stopPropagation();

    // Bloqueo de doble click
    if (pagoEnProgreso) return;
    pagoEnProgreso = true;
    btnPagar.disabled = true;
    btnPagar.classList.add("disabled");

    if (typeof ePayco === "undefined") {
      console.error("ePayco SDK NO cargó.");
      alert("Error interno: SDK de ePayco no cargado.");
      pagoEnProgreso = false;
      btnPagar.disabled = false;
      btnPagar.classList.remove("disabled");
      return;
    }

    try {
      const carrito = await getCarrito();
      if (!carrito.items || carrito.items.length === 0) {
        const modalVacioEl = document.getElementById("modalCarritoVacio");
        if (modalVacioEl && window.bootstrap?.Modal) {
          bootstrap.Modal.getOrCreateInstance(modalVacioEl).show();
        } else {
          alert("El carrito está vacío.");
        }
        pagoEnProgreso = false;
        btnPagar.disabled = false;
        btnPagar.classList.remove("disabled");
        return;
      }

      const total = Number(carrito.total || 0);
      if (!(isFinite(total) && total > 0)) {
        alert("Total inválido para procesar el pago.");
        pagoEnProgreso = false;
        btnPagar.disabled = false;
        btnPagar.classList.remove("disabled");
        return;
      }

      const idCliente = await ensureCliente();

      const pedido = await crearPedidoPendienteConDetalles(idCliente, carrito);

      const handler = ePayco.checkout.configure({
        key: "51632d542e706c9d149649cd51ce4cc7", // llave pública de pruebas
        test: true
      });

      const data = {
        name: "Compra en CafeCol",
        description: "Productos de Café",
        invoice: pedido.referencia || ("FAC-" + pedido.idPedido),
        currency: "COP",
        amount: Number(pedido.total || total),
        tax_base: "0",
        tax: "0",
        country: "CO",
        lang: "es",
        external: "true",
        response: `http://localhost:8080/sistemaCafeCol/response.html`,
        confirmation: `${window.API_BASE_URL}/epayco/confirmacion`,
        extra1: String(pedido.idPedido),
        extra2: String(idCliente)
      };

      // Cerrar modal Bootstrap antes de abrir ePayco para evitar overlay negro
      const modalClienteEl = document.getElementById("modalCliente");
      if (modalClienteEl && window.bootstrap?.Modal) {
        const instancia = bootstrap.Modal.getOrCreateInstance(modalClienteEl);
        modalClienteEl.addEventListener("hidden.bs.modal", () => {
          document.querySelectorAll(".modal-backdrop.show, .modal-backdrop").forEach(el => el.remove());
          document.body.classList.remove("modal-open");
          document.body.style.removeProperty("overflow");
          document.body.style.removeProperty("paddingRight");
          handler.open(data);
        }, { once: true });
        instancia.hide();
      } else {
        document.querySelectorAll(".modal-backdrop.show, .modal-backdrop").forEach(el => el.remove());
        document.body.classList.remove("modal-open");
        document.body.style.removeProperty("overflow");
        document.body.style.removeProperty("paddingRight");
        handler.open(data);
      }

      // Fallback: reactiva el botón después de 10 segundos por si el usuario cancela, ajusta según UX
      setTimeout(() => {
        pagoEnProgreso = false;
        btnPagar.disabled = false;
        btnPagar.classList.remove("disabled");
      }, 10000);

    } catch (err) {
      console.error("Error preparando ePayco:", err);
      alert(err?.message || "No se pudo iniciar el pago. Intenta nuevamente.");
      pagoEnProgreso = false;
      btnPagar.disabled = false;
      btnPagar.classList.remove("disabled");
    }
  });
});
