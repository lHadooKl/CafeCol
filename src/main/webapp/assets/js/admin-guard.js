// assets/js/admin-guard.js
(function () {
  const btnLogout = document.getElementById('btnLogout');

  const segs = window.location.pathname.split('/').filter(Boolean);
  const ctx = segs.length > 0 ? `/${segs[0]}` : '';

  function clearSessionCookie() {
    const pathGuess = segs.length > 0 ? `/${segs[0]}` : '/';
    document.cookie = `JSESSIONID=; Path=${pathGuess}; Expires=Thu, 01 Jan 1970 00:00:00 GMT`;
    document.cookie = `JSESSIONID=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT`;
  }

    async function checkSession() {
      try {
        const resp = await fetch(`${ctx}/api/auth/session`, { credentials: 'same-origin' });
        if (!resp.ok) throw new Error('HTTP ' + resp.status);
        const data = await resp.json();
        const esAdmin = data && data.autenticado && data.rol && data.rol.toUpperCase() === 'ADMIN';
        if (!esAdmin) {
          window.location.replace(`${ctx}/login.html`);
        }
      } catch (_) {
        window.location.replace(`${ctx}/login.html`);
      }
    }


    btnLogout?.addEventListener('click', async () => {
      try {
        await fetch(`${ctx}/api/auth/logout`, { method: 'POST', credentials: 'same-origin' });
      } catch (_) {}
      clearSessionCookie();
      localStorage.clear();

      // Previene volver con el botón "Atrás"
      window.history.pushState(null, '', `${ctx}/index.html`);
      window.location.replace(`${ctx}/index.html`);
    });

  
    window.addEventListener("pageshow", function (event) {
      if (event.persisted) {
        window.location.reload();
      }
    });
  
  

  checkSession();
})();
