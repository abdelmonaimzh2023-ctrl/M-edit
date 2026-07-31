// bridge.js – ربط محسن مع احتياط كامل
(function() {
  // تأكد من وجود AndroidBridge، وإذا لم يوجد، نصنع كائنًا وهميًا
  if (typeof window.AndroidBridge === 'undefined') {
    window.AndroidBridge = {
      openFilePicker: function(type) {
        console.warn('[Bridge] AndroidBridge غير متاح، افتح منتقي الملفات يدويًا');
        // نثير حدثًا لنعلم الواجهة بأنها تحتاج لاستخدام input file احتياطي
        var event = new CustomEvent('bridge:fallbackPicker', { detail: { type: type } });
        window.dispatchEvent(event);
      },
      trimVideo: function() { console.warn('trimVideo not available'); },
      changeSpeed: function() { console.warn('changeSpeed not available'); },
      exportVideo: function() { console.warn('exportVideo not available'); },
      // ... باقي الدوال الفارغة
    };
  }

  window.callNative = function(method) {
    var args = Array.prototype.slice.call(arguments, 1);
    if (window.AndroidBridge && typeof window.AndroidBridge[method] === 'function') {
      return window.AndroidBridge[method].apply(window.AndroidBridge, args);
    } else {
      console.warn('[Bridge] Method not found: ' + method);
    }
  };

  // استقبال من Kotlin
  window.onFilePicked = function(type, path) {
    var event = new CustomEvent('bridge:filePicked', { detail: { type: type, path: path } });
    window.dispatchEvent(event);
  };

  window.onExportProgress = function(percent, statusText) {
    var event = new CustomEvent('bridge:exportProgress', { detail: { percent: percent, statusText: statusText } });
    window.dispatchEvent(event);
  };

  window.onExportComplete = function(outputPath) {
    var event = new CustomEvent('bridge:exportComplete', { detail: { outputPath: outputPath } });
    window.dispatchEvent(event);
  };

  window.onExportError = function(message) {
    var event = new CustomEvent('bridge:exportError', { detail: { message: message } });
    window.dispatchEvent(event);
  };
})();
