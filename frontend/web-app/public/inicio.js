// Se carga antes de mostrar la página: idioma y tema.
// En inglés o portugués la página queda oculta hasta que su script reemplaza los textos (así no se ve el español un
// instante). El tema elegido queda en este navegador (al ingresar, el de la cuenta); hasta que se elige uno se usa el
// del sistema (ver lib/tema.ts).
(function () {
  var raiz = document.documentElement;
  var idioma = 'es';
  try {
    var usuario = JSON.parse(sessionStorage.getItem('tfi.auth.user') || 'null');
    var elegido = (usuario && usuario.idioma) || localStorage.getItem('auditnet.idioma');
    if (elegido === 'en' || elegido === 'pt') idioma = elegido;
  } catch (e) {}
  raiz.lang = idioma;
  if (idioma !== 'es') {
    raiz.classList.add('i18n-pendiente');
    setTimeout(function () { raiz.classList.remove('i18n-pendiente'); }, 3000);
  }

  var sistemaOscuro = window.matchMedia('(prefers-color-scheme: dark)');
  function aplicarTema() {
    var tema = null;
    try { tema = localStorage.getItem('auditnet.tema'); } catch (e) {}
    var oscuro = tema === 'oscuro' || (tema !== 'claro' && sistemaOscuro.matches);
    raiz.setAttribute('data-tema', oscuro ? 'oscuro' : 'claro');
  }
  aplicarTema();
  sistemaOscuro.addEventListener('change', aplicarTema);
})();
