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
})();

(function () {
  var LIST = '.anka-build-plugin-templates-list';

  function text(el) {
    return el && el.value != null ? String(el.value).trim() : '';
  }

  function selectedOptionText(select) {
    if (!select || select.tagName !== 'SELECT' || select.selectedIndex < 0) {
      return text(select);
    }
    var opt = select.options[select.selectedIndex];
    return opt ? String(opt.text).trim() : text(select);
  }

  function launchMethod(chunk) {
    var root = chunk.querySelector('.anka-launch-method') || chunk;
    var jnlp = root.querySelector('input[type="radio"][value="jnlp"]');
    var ssh = root.querySelector('input[type="radio"][value="ssh"]');
    if (jnlp && (jnlp.checked || jnlp.getAttribute('checked') != null)) {
      return 'JNLP';
    }
    if (ssh && (ssh.checked || ssh.getAttribute('checked') != null)) {
      return 'SSH';
    }
    var checked = root.querySelector('input[type="radio"][name*="launchMethod"]:checked');
    if (!checked) {
      checked = root.querySelector('input[type="radio"]:checked');
    }
    if (!checked) {
      return '';
    }
    return String(checked.value || '').toLowerCase() === 'jnlp' ? 'JNLP' : 'SSH';
  }

  function capacityLabel(raw) {
    if (raw === '' || raw === '0') return 'unlimited';
    return 'cap ' + raw;
  }

  function isExpanded(chunk) {
    return chunk.getAttribute('data-anka-expanded') === 'true';
  }

  function isInitialized(chunk) {
    return chunk.getAttribute('data-anka-initialized') === '1';
  }

  function composeHeader(chunk) {
    var summary = chunk.querySelector('.anka-label-summary');
    if (!summary) return;

    var dragSlot = summary.querySelector('.anka-label-summary__drag-slot');
    var jenkinsHeader = chunk.querySelector('.repeated-chunk__header');
    // One real Jenkins handle only — do not also paint ⋮⋮ (that doubled the grip icons).
    // Only the top-level label-chunk handle — never steal nested env-var handles
    var jenkinsHandle =
      (jenkinsHeader && jenkinsHeader.querySelector('.dd-handle')) ||
      chunk.querySelector(':scope > .dd-handle');

    if (dragSlot) {
      if (jenkinsHandle && !dragSlot.contains(jenkinsHandle)) {
        dragSlot.textContent = '';
        dragSlot.appendChild(jenkinsHandle);
      }
      var handle = dragSlot.querySelector('.dd-handle');
      if (!handle) {
        handle = document.createElement('div');
        handle.className = 'dd-handle';
        handle.setAttribute('title', 'Drag to reorder');
        dragSlot.appendChild(handle);
      }
      handle.classList.add('anka-label-summary__drag');
      handle.setAttribute('title', 'Drag to reorder');
      handle.removeAttribute('hidden');
      handle.style.display = '';
    }

    if (jenkinsHeader) {
      jenkinsHeader.setAttribute('data-anka-header-hidden', '1');
      jenkinsHeader.setAttribute('hidden', 'hidden');
      jenkinsHeader.style.display = 'none';
    }

    // Hide stray top-level handles still outside the summary (not nested repeatables)
    [jenkinsHeader, chunk].forEach(function (root) {
      if (!root) return;
      Array.prototype.forEach.call(root.children || [], function (child) {
        if (child.classList && child.classList.contains('dd-handle') && !summary.contains(child)) {
          child.setAttribute('hidden', 'hidden');
          child.style.display = 'none';
        }
      });
    });

    var deleteSlot = summary.querySelector('.anka-label-summary__delete-slot');
    var deleteWrap = chunk.querySelector(':scope > .show-if-only');
    if (!deleteWrap) {
      deleteWrap = chunk.querySelector('.anka-label-summary__delete');
    }
    if (deleteSlot && deleteWrap && !deleteSlot.contains(deleteWrap)) {
      deleteSlot.appendChild(deleteWrap);
      deleteWrap.classList.add('anka-label-summary__delete');
    }
    var deleteBtn = deleteSlot && deleteSlot.querySelector('button, .repeatable-delete');
    if (deleteBtn && !deleteBtn.getAttribute('data-anka-delete-labeled')) {
      deleteBtn.setAttribute('data-anka-delete-labeled', '1');
      deleteBtn.setAttribute('title', 'Delete');
      deleteBtn.setAttribute('aria-label', 'Delete');
      if (!String(deleteBtn.textContent || '').trim() || String(deleteBtn.textContent).trim() === '×' || String(deleteBtn.textContent).trim() === 'x') {
        deleteBtn.textContent = 'Delete';
      }
    }
  }

  function syncSummary(chunk) {
    var summary = chunk.querySelector('.anka-label-summary');
    if (!summary) return;

    var labelInput = chunk.querySelector('input[name$=".label"], input[name="_.label"]');
    var templateSelect = chunk.querySelector('select[name$=".masterVmId"], select[name="_.masterVmId"]');
    var tagSelect = chunk.querySelector('select[name$=".tag"], select[name="_.tag"]');
    var capInput = chunk.querySelector('input[name$=".instanceCapacity"], input[name="_.instanceCapacity"]');

    var name = text(labelInput) || '(unnamed)';
    var template = selectedOptionText(templateSelect) || '—';
    var tag = selectedOptionText(tagSelect) || '—';
    var launch = launchMethod(chunk) || '—';
    var cap = capacityLabel(text(capInput));

    var nameEl = summary.querySelector('.anka-label-summary__name');
    var templateEl = summary.querySelector('.anka-label-summary__template');
    var launchEl = summary.querySelector('.anka-label-summary__launch');
    var capEl = summary.querySelector('.anka-label-summary__cap');
    var chevron = summary.querySelector('.anka-label-summary__chevron');

    if (nameEl) nameEl.textContent = name;
    if (templateEl) templateEl.textContent = template + ' / ' + tag;
    if (launchEl) {
      launchEl.textContent = launch;
      launchEl.classList.remove('chip--ssh', 'chip--jnlp');
      if (launch === 'SSH') launchEl.classList.add('chip--ssh');
      if (launch === 'JNLP') launchEl.classList.add('chip--jnlp');
    }
    if (capEl) capEl.textContent = cap;

    var expanded = isExpanded(chunk);
    summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    if (chevron) chevron.textContent = expanded ? '▾' : '▸';
  }

  function setExpanded(chunk, expanded) {
    // data-* survives Jenkins repeatableSupport.update() className replacement
    chunk.setAttribute('data-anka-expanded', expanded ? 'true' : 'false');
    syncSummary(chunk);
  }

  function setShowMore(chunk, open) {
    chunk.setAttribute('data-anka-show-more', open ? 'true' : 'false');
  }

  function bindShowMore(chunk) {
    var toggle = chunk.querySelector('.anka-label-show-more__toggle');
    var panel = chunk.querySelector('.anka-label-show-more__panel');
    if (!toggle || !panel || toggle.dataset.ankaBound === '1') return;
    toggle.dataset.ankaBound = '1';
    if (!chunk.hasAttribute('data-anka-show-more')) {
      setShowMore(chunk, !panel.hasAttribute('hidden'));
    }
    toggle.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      var open = panel.hasAttribute('hidden');
      if (open) {
        panel.removeAttribute('hidden');
        toggle.setAttribute('aria-expanded', 'true');
        toggle.textContent = 'Show less';
      } else {
        panel.setAttribute('hidden', 'hidden');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.textContent = 'Show more';
      }
      setShowMore(chunk, open);
    });
  }

  function bindSummaryToggle(chunk) {
    var summary = chunk.querySelector('.anka-label-summary');
    if (!summary || summary.dataset.ankaBound === '1') return;
    summary.dataset.ankaBound = '1';
    function toggle() {
      setExpanded(chunk, !isExpanded(chunk));
    }
    summary.addEventListener('click', function (e) {
      // Drag handle and delete must not toggle expand
      if (e.target.closest('.dd-handle, .anka-label-summary__drag-slot, .anka-label-summary__drag, .anka-label-summary__delete, .anka-label-summary__delete-slot, a, button, input, select, textarea')) {
        return;
      }
      toggle();
    });
    summary.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        toggle();
      }
    });
  }

  function bindFieldSync(chunk) {
    if (chunk.dataset.ankaSyncBound === '1') return;
    chunk.dataset.ankaSyncBound = '1';
    chunk.addEventListener('input', function () { syncSummary(chunk); });
    chunk.addEventListener('change', function () { syncSummary(chunk); });
  }

  function enhanceChunk(chunk, opts) {
    opts = opts || {};
    composeHeader(chunk);
    bindSummaryToggle(chunk);
    bindShowMore(chunk);
    bindFieldSync(chunk);
    if (typeof opts.expanded === 'boolean') {
      setExpanded(chunk, opts.expanded);
    } else if (!isExpanded(chunk) && !isInitialized(chunk)) {
      setExpanded(chunk, false);
    } else {
      syncSummary(chunk);
    }
    if (!chunk.hasAttribute('data-anka-show-more')) {
      setShowMore(chunk, false);
    }
    chunk.setAttribute('data-anka-initialized', '1');
  }

  function enhanceAll(defaultExpanded) {
    var chunks = document.querySelectorAll(LIST + ' .repeated-chunk');
    chunks.forEach(function (chunk) {
      enhanceChunk(chunk, { expanded: defaultExpanded });
    });
  }

  function watchForNewChunks() {
    var root = document.querySelector(LIST);
    if (!root || typeof MutationObserver === 'undefined') return;
    var observer = new MutationObserver(function (mutations) {
      mutations.forEach(function (m) {
        m.addedNodes.forEach(function (node) {
          if (!(node instanceof HTMLElement)) return;
          var chunks = [];
          if (node.classList.contains('repeated-chunk')) chunks.push(node);
          node.querySelectorAll && node.querySelectorAll('.repeated-chunk').forEach(function (c) {
            chunks.push(c);
          });
          chunks.forEach(function (chunk) {
            if (!isInitialized(chunk)) {
              enhanceChunk(chunk, { expanded: true });
            } else {
              // Jenkins may re-inject header/delete after update(); recompose.
              composeHeader(chunk);
              syncSummary(chunk);
            }
          });
        });
      });
    });
    observer.observe(root, { childList: true, subtree: true });
  }

  function onReady(fn) {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', fn);
    } else {
      fn();
    }
  }

  onReady(function () {
    // Existing labels: collapsed. Newly added chunks: expanded via observer.
    enhanceAll(false);
    watchForNewChunks();
  });
})();
