// assets/js/navbar-session.js
(function () {
  const btnLogin = document.getElementById('btnLogin');
  const btnLogout = document.getElementById('btnLogout');

  const segs = window.location.pathname.split('/').filter(Boolean);
  const ctx = segs.length > 0 ? `/${segs[0]}` : '';

  function clearSessionCookie() {
    const pathGuess = segs.length > 0 ? `/${segs[0]}` : '/';
    document.cookie = `JSESSIONID=; Path=${pathGuess}; Expires=Thu, 01 Jan 1970 00:00:00 GMT`;
    document.cookie = `JSESSIONID=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT`;
  }

  fetch(`${ctx}/api/auth/session`, { credentials: 'same-origin' })
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (data && data.autenticado) {
        btnLogin?.classList.add('d-none');
        btnLogout?.classList.remove('d-none');
      } else {
        btnLogin?.classList.remove('d-none');
        btnLogout?.classList.add('d-none');
      }
    })
    .catch(() => { btnLogout?.classList.add('d-none'); });

  btnLogout?.addEventListener('click', async () => {
    try { await fetch(`${ctx}/api/auth/logout`, { method: 'POST', credentials: 'same-origin' }); } catch (_) {}
    clearSessionCookie();
    localStorage.removeItem('idUsuario');
    localStorage.removeItem('nombreUsuario');
    window.location.href = 'index.html';
  });
})();
