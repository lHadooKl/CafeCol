// assets/js/productos-admin.js
document.addEventListener("DOMContentLoaded", () => {
  const tabla = document.querySelector("#tablaProductos tbody");
  const form = document.getElementById("formProducto");
  const inputId = document.getElementById("idProducto");
  const inputNombre = document.getElementById("nombre");
  const inputPrecio = document.getElementById("precio");
  const inputStock = document.getElementById("stock");
  const inputDescripcion = document.getElementById("descripcion");

  function formatPrecio(v) {
    const n = Number(v);
    return isFinite(n)
      ? n.toLocaleString("es-CO", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      : "0,00";
  }

  async function cargarProductos() {
    tabla.innerHTML = `<tr><td colspan="6">Cargando...</td></tr>`;
    try {
      const res = await fetch(PRODUCTOS_API.getAll, { credentials: "same-origin" });
      if (!res.ok) throw new Error(`Error HTTP ${res.status}`);
      const productos = await res.json();

      tabla.innerHTML = "";
      if (!Array.isArray(productos) || productos.length === 0) {
        tabla.innerHTML = `<tr><td colspan="6">No hay productos registrados.</td></tr>`;
        return;
      }

      for (const p of productos) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${p.idProducto}</td>
          <td>${p.nombre}</td>
          <td>$${formatPrecio(p.precio)}</td>
          <td>${p.stock}</td>
          <td>${p.descripcion || "-"}</td>
          <td>
            <button class="btn btn-sm btn-warning me-1" data-id="${p.idProducto}" data-action="editar">Editar</button>
            <button class="btn btn-sm btn-danger" data-id="${p.idProducto}" data-action="eliminar">Eliminar</button>
          </td>
        `;
        tabla.appendChild(tr);
      }
    } catch (err) {
      console.error(err);
      tabla.innerHTML = `<tr><td colspan="6" class="text-danger">Error al cargar productos.</td></tr>`;
    }
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const producto = {
      nombre: inputNombre.value.trim(),
      precio: Number(inputPrecio.value),
      stock: Number.parseInt(inputStock.value),
      descripcion: inputDescripcion.value.trim()
    };

    const id = inputId.value;
    const method = id ? "PUT" : "POST";
    const url = id ? PRODUCTOS_API.update(id) : PRODUCTOS_API.create;

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify(producto)
      });

      if (!res.ok) throw new Error(`Error al guardar producto (HTTP ${res.status})`);
      await cargarProductos();
      form.reset();
      inputId.value = "";
    } catch (err) {
      console.error(err);
      alert(`${err.message}`);
    }
  });

  async function editarProducto(id) {
    try {
      const res = await fetch(PRODUCTOS_API.getById(id), { credentials: "same-origin" });
      if (!res.ok) throw new Error(`Producto ${id} no encontrado`);
      const p = await res.json();

      inputId.value = p.idProducto;
      inputNombre.value = p.nombre;
      inputPrecio.value = p.precio;
      inputStock.value = p.stock;
      inputDescripcion.value = p.descripcion || "";

      inputNombre.focus();
    } catch (err) {
      console.error(err);
      alert("Error al cargar el producto para editar");
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

  tabla.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const id = btn.dataset.id;
    const action = btn.dataset.action;
    if (action === "editar") editarProducto(id);
    if (action === "eliminar") eliminarProducto(id);
  });

  cargarProductos();
});
