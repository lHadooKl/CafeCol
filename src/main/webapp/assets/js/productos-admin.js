// assets/js/productos-admin.js
document.addEventListener("DOMContentLoaded", () => {
  const tablaBody = document.querySelector("#tablaProductos tbody");
  const btnNuevo = document.getElementById("btnNuevoProducto");

  // Modal + form
  const modalEl = document.getElementById("modalProducto");
  const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
  const form = document.getElementById("formProducto");
  const titleEl = document.getElementById("modalProductoLabel");

  // Inputs
  const inId = document.getElementById("idProducto");
  const inNombre = document.getElementById("nombre");
  const inPrecio = document.getElementById("precio");
  const inStock = document.getElementById("stock");
  const inDesc = document.getElementById("descripcion");

  const clean = (s) => String(s ?? "").trim();
  const toNumber = (v) => Number(v);

  function formatPrecio(v) {
    const n = Number(v);
    return Number.isFinite(n)
      ? n.toLocaleString("es-CO", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      : "0,00";
  }

  function resetFormToCreate() {
    form.reset();
    form.classList.remove("was-validated");
    // Limpia mensajes custom
    inPrecio.setCustomValidity("");
    inStock.setCustomValidity("");
    inId.value = "";
    titleEl.textContent = "Nuevo producto";
  }

  function setFormToEdit(p) {
    form.classList.remove("was-validated");
    inPrecio.setCustomValidity("");
    inStock.setCustomValidity("");

    inId.value = p.idProducto;
    inNombre.value = clean(p.nombre);
    inPrecio.value = p.precio;
    inStock.value = p.stock;
    inDesc.value = clean(p.descripcion || "");
    titleEl.textContent = `Editar producto #${p.idProducto}`;
  }

  async function cargarProductos() {
    tablaBody.innerHTML = `<tr><td colspan="6">Cargando...</td></tr>`;
    try {
      const res = await fetch(PRODUCTOS_API.getAll, { credentials: "same-origin" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const productos = await res.json();

      tablaBody.innerHTML = "";
      if (!Array.isArray(productos) || productos.length === 0) {
        tablaBody.innerHTML = `<tr><td colspan="6">No hay productos registrados.</td></tr>`;
        return;
      }

      const frag = document.createDocumentFragment();
      for (const p of productos) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${p.idProducto}</td>
          <td>${clean(p.nombre)}</td>
          <td>$${formatPrecio(p.precio)}</td>
          <td>${p.stock}</td>
          <td>${clean(p.descripcion) || "-"}</td>
          <td>
            <button class="btn btn-sm btn-warning me-1" data-id="${p.idProducto}" data-action="editar">Editar</button>
            <button class="btn btn-sm btn-danger" data-id="${p.idProducto}" data-action="eliminar">Eliminar</button>
          </td>
        `;
        frag.appendChild(tr);
      }
      tablaBody.appendChild(frag);
    } catch (err) {
      console.error(err);
      tablaBody.innerHTML = `<tr><td colspan="6" class="text-danger">Error al cargar productos.</td></tr>`;
    }
  }

  async function openProductoModal(id) {
    try {
      if (!id) {
        resetFormToCreate();
        modal.show();
        return;
      }
      const res = await fetch(PRODUCTOS_API.getById(id), { credentials: "same-origin" });
      if (!res.ok) throw new Error(`Producto ${id} no encontrado`);
      const p = await res.json();
      setFormToEdit(p);
      modal.show();
    } catch (err) {
      console.error(err);
      alert("No se pudo abrir el formulario de producto.");
    }
  }

  async function eliminarProducto(id) {
    if (!confirm("¿Seguro que deseas eliminar este producto?")) return;
    try {
      const res = await fetch(PRODUCTOS_API.delete(id), { method: "DELETE", credentials: "same-origin" });
      if (!res.ok) throw new Error("No se pudo eliminar el producto");
      await cargarProductos();
    } catch (err) {
      console.error(err);
      alert("Error al eliminar el producto");
    }
  }

  // Validación de números no negativos
  function validarNumeros() {
    let ok = true;

    const precio = toNumber(inPrecio.value);
    if (!Number.isFinite(precio) || precio < 0) {
      inPrecio.setCustomValidity("El precio no puede ser negativo");
      ok = false;
    } else {
      inPrecio.setCustomValidity("");
    }

    const stock = parseInt(inStock.value, 10);
    if (!Number.isInteger(stock) || stock < 0) {
      inStock.setCustomValidity("El stock no puede ser negativo");
      ok = false;
    } else {
      inStock.setCustomValidity("");
    }

    return ok;
  }

  // Feedback inmediato al cambiar precio/stock
  inPrecio.addEventListener("input", () => {
    validarNumeros();
    if (form.classList.contains("was-validated")) inPrecio.reportValidity();
  });
  inStock.addEventListener("input", () => {
    validarNumeros();
    if (form.classList.contains("was-validated")) inStock.reportValidity();
  });

  // Submit del modal (crear/editar)
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    form.classList.add("was-validated");

    // Reglas HTML5 + reglas personalizadas
    if (!validarNumeros() || !form.checkValidity()) {
      // Muestra tooltips nativos si algo falla
      inPrecio.reportValidity();
      inStock.reportValidity();
      return;
    }

    const isEdit = !!inId.value;
    const payload = {
      nombre: clean(inNombre.value),
      precio: toNumber(inPrecio.value),
      stock: parseInt(inStock.value, 10),
      descripcion: clean(inDesc.value)
    };

    const method = isEdit ? "PUT" : "POST";
    const url = isEdit ? PRODUCTOS_API.update(inId.value) : PRODUCTOS_API.create;

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        const txt = await res.text().catch(() => "");
        throw new Error(`Error al guardar (HTTP ${res.status}): ${txt}`);
      }
      modal.hide();
      await cargarProductos();
    } catch (err) {
      console.error(err);
      alert(err.message);
    }
  });

  // Clicks
  btnNuevo?.addEventListener("click", () => openProductoModal(null));

  document.getElementById("tablaProductos").addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const id = btn.dataset.id;
    const action = btn.dataset.action;
    if (action === "editar") openProductoModal(id);
    if (action === "eliminar") eliminarProducto(id);
  });

  cargarProductos();
});
