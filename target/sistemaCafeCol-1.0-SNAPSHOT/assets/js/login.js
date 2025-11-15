// assets/js/login.js
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const alertBox = document.getElementById('alert');
  const btn = document.getElementById('btnLogin');

  const segs = window.location.pathname.split('/').filter(Boolean);
  const ctx = segs.length > 0 ? `/${segs[0]}` : '';

  const withLoading = (loading) => {
    if (loading) {
      btn.disabled = true;
      btn.dataset.prev = btn.textContent;
      btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Ingresando...`;
    } else {
      btn.disabled = false;
      btn.textContent = btn.dataset.prev || 'Entrar';
    }
  };

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    alertBox.classList.add('d-none');

    const nombreUsuario = document.getElementById('usuario').value.trim();
    const contrasena = document.getElementById('contrasena').value; // id sin ñ

    if (!nombreUsuario || !contrasena) {
      alertBox.textContent = 'Ingresa usuario y contraseña';
      alertBox.classList.remove('d-none');
      return;
    }

    withLoading(true);
    try {
      const resp = await fetch(`${ctx}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ nombreUsuario, contrasena }) // clave sin ñ
      });

      if (resp.status === 401) {
        alertBox.textContent = 'Credenciales inválidas';
        alertBox.classList.remove('d-none');
        return;
      }
      if (!resp.ok) {
        alertBox.textContent = `Error ${resp.status}`;
        alertBox.classList.remove('d-none');
        return;
      }

      const data = await resp.json();
      localStorage.setItem('idUsuario', String(data.idUsuario));
      localStorage.setItem('nombreUsuario', data.nombreUsuario);
      const rol = (data.rol || '').toUpperCase();
      window.location.href = rol === 'ADMIN' ? 'pages/index_admin.html' : 'index.html';
    } catch (_) {
      alertBox.textContent = 'No se pudo iniciar sesión';
      alertBox.classList.remove('d-none');
    } finally {
      withLoading(false);
    }
  });
});
