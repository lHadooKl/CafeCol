document.addEventListener("DOMContentLoaded", () => {
  // Chart.js
  let chartInstance = null;
  function renderChart(conteoEstados) {
        const estadosOrder = ["PENDIENTE", "ACEPTADO", "GESTIONADO", "CERRADO"];
        const labels = estadosOrder;
        const counts = estadosOrder.map(est => conteoEstados[est] || 0);

        const maxPedidos = Math.max(...counts, 1);
        const maxY = maxPedidos;

        const ctx = document.getElementById("chartPedidosEstado")?.getContext("2d");
        if (!ctx) return;

        if (chartInstance) chartInstance.destroy();

        chartInstance = new Chart(ctx, {
          type: "bar",
          data: {
            labels,
            datasets: [{
              label: "",
              data: counts,
              backgroundColor: [
                "rgba(108,117,125,0.90)", // Pendiente
                "rgba(23,162,184,0.88)", // Aceptado
                "rgba(255,193,7,0.90)", // Gestionado
                "rgba(40,167,69,0.86)"  // Cerrado
              ],
              borderRadius: 8,
              maxBarThickness: 50
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
              tooltip: { backgroundColor: "#181818", titleColor: "#FFF", bodyColor: "#FFF" }
            },
            scales: {
              y: {
                beginAtZero: true,
                max: maxY,
                ticks: { color: "#b4b4b4", stepSize: 2, font: { size: 12 } },
                grid: { color: "rgba(255,255,255,0.07)" }
              },
              x: {
                title: { display: false },
                ticks: {
                  display: true,      // Mostrar nombres
                  color: "#fff",      // Color blanco
                  font: { size: 12 }  // Tamaño de fuente recomendado
                },
                grid: { display: false }
              }
            }
          }
        });
      }

  // Elementos de los contenedores por estado
  const cardsPendiente   = document.getElementById("cardsPendiente");
  const cardsAceptado    = document.getElementById("cardsAceptado");
  const cardsGestionado  = document.getElementById("cardsGestionado");
  const cardsCerrado     = document.getElementById("cardsCerrado");

  const nfCOP = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });

  function badgeForEstado(estado){
    const e = (estado || "").toUpperCase();
    if (e === "PENDIENTE") return "bg-secondary";
    if (e === "ACEPTADO") return "bg-info";
    if (e === "GESTIONADO") return "bg-warning text-dark";
    if (e === "CERRADO") return "bg-success";
    return "bg-secondary";
  }
  function clean(s){ return String(s ?? "").trim(); }

  // Modal y gestión de detalles igual que antes...
  // (copiar aquí las funciones abrirGestion, guardarEstado, toggleGuia...)

  let pedidoActual = null;
  const modalEl = document.getElementById("modalPedido");
  const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
  const formGestion = document.getElementById("formGestion");
  const selEstado = document.getElementById("selEstado");
  const grupoGuia = document.getElementById("grupoGuia");
  const numGuia = document.getElementById("numGuia");
  const pId = document.getElementById("pId");
  const pCliente = document.getElementById("pCliente");
  const pFecha = document.getElementById("pFecha");
  const pTotal = document.getElementById("pTotal");
  const pEstado = document.getElementById("pEstado");
  const detalleBody = document.getElementById("detalleTableBody");

  async function cargarPedidos() {
    cardsPendiente.innerHTML = "";
    cardsAceptado.innerHTML = "";
    cardsGestionado.innerHTML = "";
    cardsCerrado.innerHTML = "";
    try {
      const res = await fetch(PEDIDOS_API.getAll, { credentials: "same-origin" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const pedidos = await res.json();

      // Conteo por estado para gráfica
      const conteoEstados = {PENDIENTE:0, ACEPTADO:0, GESTIONADO:0, CERRADO:0};
        if (Array.isArray(pedidos)) {
          for (const p of pedidos) {
            const estado = (p.estado || "PENDIENTE").toUpperCase();
            if (conteoEstados.hasOwnProperty(estado)) {
              conteoEstados[estado]++;
            }
          }
        }
        renderChart(conteoEstados);


      // Clasifica tarjetas por estado
      if (!Array.isArray(pedidos) || pedidos.length === 0) {
        cardsPendiente.innerHTML = "<div class='col-12 text-center text-muted'>No hay pedidos.</div>";
        return;
      }

      for (const p of pedidos) {
        const card = document.createElement("div");
        card.className = "col";
        const cliente = p.cliente ? `${p.cliente.nombre} ${p.cliente.apellido}`.trim() : "—";
        const total = nfCOP.format(p.total || 0);
        const estado = clean(p.estado);
        card.innerHTML = `
          <div class="card h-100">
            <div class="card-body d-flex flex-column">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <h5 class="card-title mb-0">Pedido #${p.idPedido}</h5>
                <span class="badge ${badgeForEstado(estado)}">${estado || "—"}</span>
              </div>
              <p class="mb-1"><span class="text-muted">Cliente:</span> ${cliente}</p>
              <p class="mb-3"><span class="text-muted">Total:</span> ${total}</p>
              <div class="mt-auto">
                <button class="btn btn-primary w-100" data-action="gestionar" data-id="${p.idPedido}">
                  Gestionar
                </button>
              </div>
            </div>
          </div>
        `;
        switch (estado.toUpperCase()) {
          case "ACEPTADO":    cardsAceptado.appendChild(card);  break;
          case "GESTIONADO":  cardsGestionado.appendChild(card); break;
          case "CERRADO":     cardsCerrado.appendChild(card);   break;
          default:            cardsPendiente.appendChild(card);
        }
      }
      // Oculta sección si vacía
      toggleSection("seccionPendiente","cardsPendiente");
      toggleSection("seccionAceptado","cardsAceptado");
      toggleSection("seccionGestionado","cardsGestionado");
      toggleSection("seccionCerrado","cardsCerrado");
    } catch (err) {
      cardsPendiente.innerHTML = "<div class='col-12 text-center text-danger'>No se pudieron cargar los pedidos.</div>";
      renderChart({});
    }
  }

  function toggleSection(sectionId, cardContainerId) {
    const sec = document.getElementById(sectionId);
    const cont = document.getElementById(cardContainerId);
    sec.style.display = cont.children.length === 0 ? "none" : "";
  }

  async function abrirGestion(idPedido) {
    try {
      const res = await fetch(PEDIDOS_API.getById(idPedido), { credentials: "same-origin" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const p = await res.json();
      pedidoActual = p;
      pId.textContent = p.idPedido;
      const cliente = p.cliente ? `${p.cliente.nombre} ${p.cliente.apellido}`.trim() : "—";
      pCliente.textContent = cliente;
      pFecha.textContent = p.fecha || "—";
      pTotal.textContent = nfCOP.format(p.total || 0);
      pEstado.textContent = p.estado || "—";
      pEstado.className = `badge ${badgeForEstado(p.estado)}`;

      // Detalle
      detalleBody.innerHTML = "";
      const detalles = Array.isArray(p.detalles) ? p.detalles : [];
      if (detalles.length === 0) {
        detalleBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">Sin detalles.</td></tr>`;
      } else {
        const frag = document.createDocumentFragment();
        for (const d of detalles) {
          const tr = document.createElement("tr");
          const nombre = d.producto?.nombre || `Producto ${d.producto?.idProducto ?? ""}`;
          tr.innerHTML = `
            <td>${nombre}</td>
            <td class="text-end">${d.cantidad}</td>
            <td class="text-end">${nfCOP.format(d.precioUnitario || 0)}</td>
            <td class="text-end">${nfCOP.format(d.subtotal || 0)}</td>
          `;
          frag.appendChild(tr);
        }
        detalleBody.appendChild(frag);
      }

      // Estado y guía
      formGestion.classList.remove("was-validated");
      selEstado.value = (p.estado || "").toUpperCase();
      numGuia.value = "";
      toggleGuia();

      modal.show();
    } catch (err) {
      alert("No se pudo abrir el pedido.");
    }
  }

  function toggleGuia() {
    const val = (selEstado.value || "").toUpperCase();
    const show = val === "GESTIONADO";
    grupoGuia.classList.toggle("d-none", !show);
    if (!show) numGuia.setCustomValidity("");
  }

  async function guardarEstado() {
    formGestion.classList.add("was-validated");
    if ((selEstado.value || "").toUpperCase() === "GESTIONADO") {
      const guia = clean(numGuia.value);
      if (guia.length < 4) {
        numGuia.setCustomValidity("Ingresa un número de guía válido (mín. 4).");
        numGuia.reportValidity();
        return;
      }
      numGuia.setCustomValidity("");
    }
    if (!formGestion.checkValidity()) return;

    const id = pedidoActual.idPedido;
    const nuevoEstado = (selEstado.value || "").toUpperCase();
    try {
      const res = await fetch(PEDIDOS_API.updateEstado(id), {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({
          estado: nuevoEstado,
          guia: (nuevoEstado === "GESTIONADO" ? clean(numGuia.value) : undefined)
        })
      });
      if (!res.ok) {
        const txt = await res.text().catch(()=> "");
        throw new Error(`No se pudo actualizar el estado (HTTP ${res.status}): ${txt}`);
      }

      if (nuevoEstado === "GESTIONADO") {
        const correo = pedidoActual?.cliente?.correo || "";
        const nombre = pedidoActual?.cliente ? `${pedidoActual.cliente.nombre} ${pedidoActual.cliente.apellido}`.trim() : "";
        try {
          const rMail = await fetch(`${API_BASE_URL}/notificaciones/pedido-gestionado`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify({
              idPedido: id,
              guia: clean(numGuia.value),
              correo,
              nombre
            })
          });
        } catch {}
      }

      await cargarPedidos();
      modal.hide();
    } catch (err) {
      alert(err.message);
    }
  }

  // Eventos
  const seccionPedidos = document.querySelector("main");
  seccionPedidos.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action='gestionar']");
    if (!btn) return;
    abrirGestion(btn.dataset.id);
  });

  selEstado.addEventListener("change", toggleGuia);
  document.getElementById("btnGuardarEstado").addEventListener("click", guardarEstado);

  cargarPedidos();
});
