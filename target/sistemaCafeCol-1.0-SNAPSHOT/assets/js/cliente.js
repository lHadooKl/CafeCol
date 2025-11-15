// assets/js/cliente.js
document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("clienteForm");
  if (!form) return;

  // Spans de preview en el resumen
  const prevNombre = document.getElementById("prevNombre");
  const prevApellido = document.getElementById("prevApellido");
  const prevCorreo = document.getElementById("prevCorreo");
  const prevTelefono = document.getElementById("prevTelefono");
  const prevDireccion = document.getElementById("prevDireccion");

  // Inputs del formulario
  const inNombre = document.getElementById("nombre");
  const inApellido = document.getElementById("apellido");
  const inCorreo = document.getElementById("correo");
  const inTelefono = document.getElementById("telefono");
  const inDireccion = document.getElementById("direccion");

  function updatePreview() {
    if (prevNombre)   prevNombre.textContent   = inNombre?.value?.trim()   || "—";
    if (prevApellido) prevApellido.textContent = inApellido?.value?.trim() || "—";
    if (prevCorreo)   prevCorreo.textContent   = inCorreo?.value?.trim()   || "—";
    if (prevTelefono) prevTelefono.textContent = inTelefono?.value?.trim() || "—";
    if (prevDireccion)prevDireccion.textContent= inDireccion?.value?.trim()|| "—";
  }

  // Actualiza preview al escribir
  [inNombre, inApellido, inCorreo, inTelefono, inDireccion].forEach(inp => {
    inp?.addEventListener("input", updatePreview);
  }); // Evento input para reflejar cambios en vivo [web:68][web:71]

  // Pintar estado inicial (por si el modal se abre con campos ya llenos)
  updatePreview();

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const nuevoCliente = {
      nombre: inNombre.value.trim(),
      apellido: inApellido.value.trim(),
      correo: inCorreo.value.trim(),
      telefono: inTelefono.value.trim(),
      direccion: inDireccion.value.trim(),
    };

    try {
      const resp = await fetch(`${window.API_BASE_URL}/clientes`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(nuevoCliente),
      });

      if (!resp.ok) {
        const errorText = await resp.text();
        throw new Error(`Error al crear cliente: ${errorText}`);
      }

      const cliente = await resp.json();
      if (!cliente.idCliente) throw new Error("Respuesta del servidor inválida");

      localStorage.setItem("clienteId", String(cliente.idCliente));

      // Cerrar modal de forma segura
      const modalEl = document.getElementById("modalCliente");
      if (modalEl && window.bootstrap?.Modal) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.hide();
      } // getOrCreateInstance asegura instancia válida de modal [web:4]

      alert(`Cliente ${cliente.nombre} creado correctamente.`);
      document.dispatchEvent(new CustomEvent("clienteCreado", { detail: cliente }));
    } catch (err) {
      console.error("❌ Error:", err);
      alert("No se pudo registrar el cliente. Verifica los datos e inténtalo nuevamente.");
    }
  });
});
