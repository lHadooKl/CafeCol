// assets/js/home-hero.js
(function () {
  const inner = document.getElementById('heroInner');
  const indicators = document.getElementById('heroIndicators');
  const toastEl = document.getElementById('toastBienvenida');

  // Configuración de carrusel (ms)
  const HERO_INTERVAL = 4000;

  async function getHeroImages() {

    // Fallback estático
    return [
      { src: 'assets/img/heroes/hero1.jpg', alt: 'Café de premium', caption: 'Café premium' },
      { src: 'assets/img/heroes/hero2.jpg', alt: 'Tueste fresco', caption: 'Tueste fresco a diario' },
      { src: 'assets/img/heroes/hero3.jpg', alt: 'Disfruta', caption: 'Disfruta lo original' },
      { src: 'assets/img/heroes/hero4.jpg', alt: 'Experiencia CafeCol', caption: 'Una experiencia para tus sentidos' }
    ];
  }

  // 2) Construye atributos responsive para <img>
  function buildImgTag(img, isFirst) {
    const src1280 = img.src1280 || null;
    const src1920 = img.src1920 || img.src;
    const src2560 = img.src2560 || null;

    let srcset = '';
    if (src1280) srcset += `${src1280} 1280w`;
    if (src1920) srcset += (srcset ? ', ' : '') + `${src1920} 1920w`;
    if (src2560) srcset += (srcset ? ', ' : '') + `${src2560} 2560w`;

    const sizes = '(max-width: 576px) 100vw, (max-width: 1200px) 90vw, 1200px';
    const loading = isFirst ? 'eager' : 'lazy';
    const decoding = isFirst ? 'sync' : 'async';

    if (srcset) {
      return `
        <img
          class="img-fluid d-block w-100 hero-img"
          src="${src1920}"
          srcset="${srcset}"
          sizes="${sizes}"
          alt="${img.alt ?? ''}"
          loading="${loading}"
          decoding="${decoding}"
        >
      `;
    }
    return `
      <img
        class="img-fluid d-block w-100 hero-img"
        src="${img.src}"
        alt="${img.alt ?? ''}"
        loading="${loading}"
        decoding="${decoding}"
      >
    `;
  }

  // 3) Renderiza carrusel con altura uniforme (CSS controla el tamaño)
  function renderCarousel(images) {
    if (!inner || !indicators) return;
    inner.innerHTML = '';
    indicators.innerHTML = '';

    images.forEach((img, i) => {
      const isActive = i === 0 ? ' active' : '';

      // Indicador
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.setAttribute('data-bs-target', '#heroCarousel');
      btn.setAttribute('data-bs-slide-to', String(i));
      btn.setAttribute('aria-label', `Slide ${i + 1}`);
      if (i === 0) {
        btn.className = 'active';
        btn.setAttribute('aria-current', 'true');
      }
      indicators.appendChild(btn);

      // Item (puedes personalizar intervalo por slide si quieres)
      const item = document.createElement('div');
      item.className = 'carousel-item' + isActive;
      // Ejemplo de intervalo individual:
      // item.setAttribute('data-bs-interval', i === 0 ? '6000' : String(HERO_INTERVAL));

      item.innerHTML = `
        ${buildImgTag(img, i === 0)}
        ${img.caption ? `
          <div class="carousel-caption d-none d-md-block">
            <h5>${img.caption}</h5>
          </div>` : ''}
      `;
      inner.appendChild(item);
    });
  }

  // 4) Instancia/forza opciones del carrusel (intervalo, autoplay, etc.)
  function setupCarousel() {
    const el = document.getElementById('heroCarousel');
    if (!el || !window.bootstrap?.Carousel) return;

    // Si ya existe instancia, úsala; si no, crea una nueva
    const instance = bootstrap.Carousel.getInstance(el) || new bootstrap.Carousel(el, {
      interval: HERO_INTERVAL,  // tiempo entre slides
      ride: 'carousel',         // auto-play
      pause: false,             // no pausar al hover (opcional)
      touch: true,              // gestos touch
      keyboard: true            // control con teclado
    });

  }

  // 5) Toast de bienvenida
  function showWelcomeToast() {
    if (!toastEl || !window.bootstrap?.Toast) return;
    const toast = bootstrap.Toast.getOrCreateInstance(toastEl, { autohide: true, delay: 2000 });
    toast.show();
  }

  // 6) Init
  async function init() {
    try {
      const images = await getHeroImages();
      if (Array.isArray(images) && images.length > 0) {
        renderCarousel(images);
        setupCarousel();
      }
    } catch (e) {
      console.warn('No se pudieron cargar imágenes del hero:', e);
    } finally {
      showWelcomeToast();
    }
  }

  if (document.readyState !== 'loading') init();
  else document.addEventListener('DOMContentLoaded', init);
})();
