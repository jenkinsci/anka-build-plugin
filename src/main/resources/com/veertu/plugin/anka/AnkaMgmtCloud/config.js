function validateAnkaMgmtUrl() {
  var ankaMgmtUrl = document.getElementById('ankaMgmtUrl').value;
  var errorElement = document.getElementById('ankaMgmtUrlError');
  if (ankaMgmtUrl.includes('http://') || ankaMgmtUrl.includes('https://')) {
    errorElement.innerHTML = '';
    errorElement.classList.add('hidden');
  } else {
    errorElement.innerHTML = 'The Anka Build Cloud Controller URL must include http:// or https://';
    errorElement.classList.remove('hidden');
    return false;
  }
  return true;
}

(function () {
  var ankaMgmtUrlInput = document.getElementById('ankaMgmtUrl');
  if (ankaMgmtUrlInput) {
    ankaMgmtUrlInput.addEventListener('blur', validateAnkaMgmtUrl);
  }

  document.querySelectorAll('.anka-build-plugin-templates-list .repeated-chunk').forEach(function (chunk, index) {
    chunk.id = 'repeatedChunk' + index;
  });
  document.querySelectorAll('.anka-build-plugin-templates-list .repeated-chunk').forEach(function (chunk, index) {
    var labelInput = chunk.querySelector('input[name="_.label"]');
    if (labelInput) {
      window['repeatedChunkLabel' + index] = labelInput.value;
      var label = chunk.querySelector('.label');
      if (label) {
        label.innerHTML = 'Label: ' + labelInput.value;
      }
      labelInput.addEventListener('blur', function () {
        window['repeatedChunkLabel' + index] = labelInput.value;
        if (label) {
          label.innerHTML = 'Label: ' + labelInput.value;
        }
      });
    }
  });
})();
