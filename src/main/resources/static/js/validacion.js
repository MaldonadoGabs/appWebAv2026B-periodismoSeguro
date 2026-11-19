/*
 * Validacion en el cliente (requeridos, formato, longitud, coincidencia de claves) con mensajes claros en espanol.
 * Es solo comodidad: el servidor vuelve a validar todo y nunca se confia en esta pantalla.
 * Sin librerias ni recursos externos.
 */
(function () {
  function mensajeDe(campo) {
    var v = campo.validity;
    if (v.customError) return campo.validationMessage;
    if (v.valueMissing) return campo.type === 'radio' ? 'Elige una opción.' : 'Completa este campo.';
    if (v.typeMismatch && campo.type === 'email') return 'Escribe un correo válido, por ejemplo nombre@dominio.com.';
    if (v.tooShort) return 'Escribe al menos ' + campo.getAttribute('minlength') + ' caracteres.';
    if (v.tooLong) return 'Escribe como máximo ' + campo.getAttribute('maxlength') + ' caracteres.';
    if (v.patternMismatch) return campo.getAttribute('title') || 'El formato no es válido.';
    return '';
  }

  // El mensaje propio se pone al fallar y se quita en cuanto la persona corrige el campo
  document.addEventListener('invalid', function (ev) {
    var campo = ev.target;
    if (!campo || !campo.setCustomValidity) return;
    campo.setCustomValidity('');
    var mensaje = mensajeDe(campo);
    if (mensaje) campo.setCustomValidity(mensaje);
  }, true);

  document.addEventListener('input', function (ev) {
    var campo = ev.target;
    if (campo && campo.setCustomValidity && campo.id !== 'confirmacion') campo.setCustomValidity('');
  });

  function vigilarClaves(formulario) {
    var clave = formulario.querySelector('#clave');
    var confirmacion = formulario.querySelector('#confirmacion');
    if (!clave || !confirmacion) return;
    function comparar() {
      confirmacion.setCustomValidity(
        confirmacion.value && confirmacion.value !== clave.value ? 'Las contraseñas no coinciden.' : '');
    }
    clave.addEventListener('input', comparar);
    confirmacion.addEventListener('input', comparar);
  }

  function iniciar() {
    var formularios = document.querySelectorAll('form');
    for (var i = 0; i < formularios.length; i++) vigilarClaves(formularios[i]);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }
})();
