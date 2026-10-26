// El service worker solo sirve la página "sin conexión"; no cachea datos ni llamadas a /api.
if ('serviceWorker' in navigator) {
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js').catch(function () {});
  });
}
