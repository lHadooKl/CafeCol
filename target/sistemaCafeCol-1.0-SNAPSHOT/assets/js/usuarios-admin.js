// assets/js/usuarios-admin.js
document.addEventListener("DOMContentLoaded", () => {
  const tablaBody = document.querySelector("#tablaUsuarios tbody");
  const btnNuevoUsuario = document.getElementById("btnNuevoUsuario");

  // Modal + formulario
  const modalEl = document.getElementById("modalUsuario");
  const modal = bootstrap.Modal.getOrCreateInstance(modalEl); // [web:1][web:4]
  const form = document.getElementById("formUsuario");
  const titleEl = document.getElementById("modalUsuarioLabel");

  // Inputs
  const inId = document.getElementById("idUsuario");
  const inNombreUsuario = document.getElementById("nombreUsuario");
  const inContrasena = document.getElementById("contrasena");
  const inRol = document.getElementById("rol");

  const inNombre = document.getElementById("nombre");
  const inApellido = document.getElementById("apellido");
  const inCorreo = document.getElementById("correo");
  const inEstado = document.getElementById("estado");

  const clean = (s) => String(s ?? "").trim();

  function resetFormToCreate() {
    form.reset();
    form.classList.remove("was-validated");
    inId.value = "";
    inContrasena.required = true;   // crear: obligatoria
    titleEl.textContent = "Nuevo usuario";
  }

  function setFormToEdit(u) {
    form.classList.remove("was-validated");
    inId.value = u.idUsuario;
    inNombreUsuario.value = clean(u.nombreUsuario);
    inRol.value = clean(u.rol);

    inNombre.value = clean(u.nombre);
    inApellido.value = clean(u.apellido);
    inCorreo.value = clean(u.correo);
    inEstado.value = clean(u.estado);

    inContrasena.value = "";
    inContrasena.required = false;  // editar: opcional
    titleEl.textContent = `Editar usuario #${u.idUsuario}`;
  }

  async function cargarUsuarios() {
    tablaBody.innerHTML = `<tr><td colspan="7">Cargando...</td></tr>`;
    try {
      const res = await fetch(USUARIOS_API.getAll, { credentials: "same-origin" }); // cookies de sesión [web:142][web:135]
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const usuarios = await res.json();

      tablaBody.innerHTML = "";
      if (!Array.isArray(usuarios) || usuarios.length === 0) {
        tablaBody.innerHTML = `<tr><td colspan="7">No hay usuarios.</td></tr>`;
        return;
      }
      const frag = document.createDocumentFragment();
      for (const u of usuarios) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${u.idUsuario}</td>
          <td>${clean(u.nombreUsuario)}</td>
          <td>${clean(u.nombre)} ${clean(u.apellido)}</td>
          <td>${clean(u.correo)}</td>
          <td><span class="badge ${u.rol === 'ADMIN' ? 'bg-primary' : 'bg-secondary'}">${u.rol}</span></td>
          <td><span class="badge ${u.estado === 'ACTIVO' ? 'bg-success' : 'bg-secondary'}">${u.estado}</span></td>
          <td>
            <button class="btn btn-sm btn-warning me-1" data-id="${u.idUsuario}" data-action="editar">Editar</button>
            <button class="btn btn-sm btn-danger" data-id="${u.idUsuario}" data-action="eliminar">Eliminar</button>
          </td>
        `;
        frag.appendChild(tr);
      }
      tablaBody.appendChild(frag);
    } catch (err) {
      console.error(err);
      tablaBody.innerHTML = `<tr><td colspan="7" class="text-danger">Error al cargar usuarios.</td></tr>`;
    }
  }

  async function openUsuarioModal(id) {
    try {
      if (!id) {
        resetFormToCreate();
        modal.show(); // [web:1][web:4]
        return;
      }
      // Editar: cargar datos
      const res = await fetch(USUARIOS_API.getById(id), { credentials: "same-origin" });
      if (!res.ok) throw new Error(`Usuario ${id} no encontrado`);
      const u = await res.json();
      setFormToEdit(u);
      modal.show();
    } catch (err) {
      console.error(err);
      alert("No se pudo abrir el formulario de usuario.");
    }
  }

  async function eliminarUsuario(id) {
    if (!confirm("¿Seguro que deseas eliminar este usuario?")) return;
    try {
      const res = await fetch(USUARIOS_API.delete(id), { method: "DELETE", credentials: "same-origin" });
      if (!(res.status === 204 || res.ok)) throw new Error("No se pudo eliminar");
      await cargarUsuarios();
    } catch (err) {
      console.error(err);
      alert("Error al eliminar el usuario");
    }
  }

  // Submit del modal (crear/editar)
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    form.classList.add("was-validated");

    const isEdit = !!inId.value;
    if (!form.checkValidity()) return; // validación HTML5 [web:179]

    const payload = {
      nombreUsuario: clean(inNombreUsuario.value),
      rol: clean(inRol.value),
      nombre: clean(inNombre.value),
      apellido: clean(inApellido.value),
      correo: clean(inCorreo.value),
      estado: clean(inEstado.value)
    };

    const pass = inContrasena.value;
    if (!isEdit) {
      if (!pass) {
        inContrasena.setCustomValidity("Campo requerido");
        inContrasena.reportValidity();
        setTimeout(() => inContrasena.setCustomValidity(""), 1000);
        return;
      }
      payload.contrasena = pass;
    } else if (pass) {
      payload.contrasena = pass;
    }

    const method = isEdit ? "PUT" : "POST";
    const url = isEdit ? USUARIOS_API.update(inId.value) : USUARIOS_API.create;

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        const txt = await res.text();
        throw new Error(`Error al guardar (HTTP ${res.status}): ${txt}`);
      }
      modal.hide(); // [web:14]
      await cargarUsuarios();
    } catch (err) {
      console.error(err);
      alert(err.message);
    }
  });

  // Clicks
  btnNuevoUsuario?.addEventListener("click", () => openUsuarioModal(null));

  document.getElementById("tablaUsuarios").addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const id = btn.dataset.id;
    const action = btn.dataset.action;
    if (action === "editar") openUsuarioModal(id);
    if (action === "eliminar") eliminarUsuario(id);
  });

  cargarUsuarios();
});
