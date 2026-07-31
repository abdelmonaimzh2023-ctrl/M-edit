// bridge.js – ربط JavaScript مع Kotlin
window.callNative = function(method, ...args) {
  if (window.AndroidBridge && typeof window.AndroidBridge[method] === 'function') {
    return window.AndroidBridge[method](...args);
  } else {
    console.warn('[Bridge] AndroidBridge not available, method:', method);
    return null;
  }
};

// استقبال من Kotlin
window.onFilePicked = function(type, path) {
  console.log('[Bridge] onFilePicked', type, path);
  const event = new CustomEvent('bridge:filePicked', { detail: { type, path } });
  window.dispatchEvent(event);
};

window.onVideoLoaded = function(path, durationMs, width, height) {
  const event = new CustomEvent('bridge:videoLoaded', { detail: { path, durationMs, width, height } });
  window.dispatchEvent(event);
};

window.onExportProgress = function(percent, statusText) {
  const event = new CustomEvent('bridge:exportProgress', { detail: { percent, statusText } });
  window.dispatchEvent(event);
};

window.onExportComplete = function(outputPath) {
  const event = new CustomEvent('bridge:exportComplete', { detail: { outputPath } });
  window.dispatchEvent(event);
};

window.onExportError = function(message) {
  const event = new CustomEvent('bridge:exportError', { detail: { message } });
  window.dispatchEvent(event);
};

window.onOperationComplete = function(opName, dataJson) {
  console.log('[Bridge] operation completed:', opName);
};
