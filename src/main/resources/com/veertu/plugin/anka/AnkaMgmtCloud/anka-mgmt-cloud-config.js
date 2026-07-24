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

  function summaryControl(chunk, key) {
    var root = chunk.querySelector('.anka-summary-source[data-anka-summary="' + key + '"]');
    if (!root) return null;
    return root.querySelector('input, select, textarea');
  }

  /**
   * Template display name from select option text ("Name(uuid)" → "Name").
   * Falls back to UUID when options are not loaded yet or only ids are present.
   */
  function templateDisplayName(select) {
    if (!select) return '';
    var optText = selectedOptionText(select);
    if (optText) {
      var match = optText.match(/^(.*)\(([^)]+)\)\s*$/);
      if (match) {
        var name = String(match[1]).trim();
        if (name) return name;
        return String(match[2]).trim();
      }
      // Ignore the empty "Choose Vm template" placeholder
      if (/^choose\b/i.test(optText)) return '';
      return optText;
    }
    return text(select);
  }

  function tagDisplayName(select) {
    var value = text(select);
    if (value) return value;
    var optText = selectedOptionText(select);
    if (!optText || /^choose\b/i.test(optText) || /leave empty for latest/i.test(optText)) {
      return '(latest tag)';
    }
    return optText;
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

    var ourHandle = summary.querySelector('.anka-label-summary__drag');
    if (ourHandle) {
      // Ensure Sortable can find exactly one handle in this chunk.
      ourHandle.classList.add('dd-handle');
      ourHandle.setAttribute('title', 'Drag to reorder');
      ourHandle.setAttribute('aria-label', 'Drag to reorder');
      // Jenkins may inject <svg>/img into .dd-handle — strip those so we only show ⋮⋮
      while (ourHandle.firstChild) {
        ourHandle.removeChild(ourHandle.firstChild);
      }
      ourHandle.appendChild(document.createTextNode('⋮⋮'));
      ourHandle.removeAttribute('hidden');
      ourHandle.style.cssText = '';
    }

    // Remove ALL other drag handles in this label chunk (header + absolute leftovers).
    // Do not touch nested env-var repeatables inside .anka-label-body.
    var body = chunk.querySelector('.anka-label-body');
    chunk.querySelectorAll('.dd-handle, .repeated-chunk__header').forEach(function (el) {
      if (ourHandle && (el === ourHandle || ourHandle.contains(el))) return;
      if (body && body.contains(el)) return;
      if (el.classList.contains('repeated-chunk__header')) {
        el.setAttribute('hidden', 'hidden');
        el.style.cssText = 'display:none!important;height:0!important;width:0!important;overflow:hidden!important;';
        return;
      }
      if (el.parentNode) {
        el.parentNode.removeChild(el);
      }
    });

    chunk.style.padding = '0';
    chunk.style.paddingLeft = '0';

    placeDeleteFooter(chunk);
  }

  function findLabelDeleteWrap(chunk, footer) {
    var direct =
      chunk.querySelector(':scope > .show-if-only') ||
      chunk.querySelector(':scope > .anka-label-delete-source') ||
      chunk.querySelector('.anka-label-summary__delete');
    if (direct) return direct;
    if (footer) {
      var inFooter = footer.querySelector('.show-if-only, .anka-label-delete, .anka-label-delete-source');
      if (inFooter) return inFooter;
    }
    // Fallback: top-level repeatable-delete not nested in another repeated-chunk
    var buttons = chunk.querySelectorAll('.repeatable-delete');
    for (var i = 0; i < buttons.length; i++) {
      var btn = buttons[i];
      var nestedChunk = btn.closest('.repeated-chunk');
      if (nestedChunk && nestedChunk !== chunk) continue;
      if (footer && footer.contains(btn)) return btn.parentElement;
      return btn.closest('.show-if-only, .anka-label-delete-source') || btn.parentElement;
    }
    return null;
  }

  function placeDeleteFooter(chunk) {
    var body = chunk.querySelector('.anka-label-body');
    var footer = body && body.querySelector('.anka-label-delete-footer');
    if (!footer) return;

    var deleteWrap = findLabelDeleteWrap(chunk, footer);
    if (deleteWrap && !footer.contains(deleteWrap)) {
      footer.appendChild(deleteWrap);
    }
    if (deleteWrap) {
      deleteWrap.classList.add('anka-label-delete');
      deleteWrap.classList.remove('anka-label-summary__delete');
    }

    var deleteBtn = footer.querySelector('button.repeatable-delete, .repeatable-delete, button');
    if (!deleteBtn) return;

    // Kill Jenkins floating-X positioning even before CSS loads
    deleteBtn.style.position = 'static';
    deleteBtn.style.right = 'auto';
    deleteBtn.style.top = 'auto';
    deleteBtn.style.left = 'auto';

    if (!deleteBtn.getAttribute('data-anka-delete-labeled')) {
      deleteBtn.setAttribute('data-anka-delete-labeled', '1');
      deleteBtn.setAttribute('title', 'Delete this Node Label');
      deleteBtn.setAttribute('aria-label', 'Delete this Node Label');
      deleteBtn.textContent = 'Delete';
    }

    if (deleteBtn.getAttribute('data-anka-delete-confirm') === '1') return;
    deleteBtn.setAttribute('data-anka-delete-confirm', '1');
    deleteBtn.addEventListener('click', function (e) {
      var labelInput = summaryControl(chunk, 'label') ||
        chunk.querySelector('.anka-label-essentials input[name$=".label"], input[name$=".label"], input[name="_.label"]');
      var labelName = text(labelInput) || 'this Node Label';
      var ok = window.confirm('Delete Node Label "' + labelName + '"?');
      if (!ok) {
        e.preventDefault();
        e.stopImmediatePropagation();
        e.stopPropagation();
      }
    }, true);
  }

  function syncSummary(chunk) {
    var summary = chunk.querySelector('.anka-label-summary');
    if (!summary) return;

    var labelInput = summaryControl(chunk, 'label') ||
      chunk.querySelector('.anka-label-essentials input[name$=".label"], .anka-label-essentials input[name="_.label"]');
    var templateSelect = summaryControl(chunk, 'masterVmId') ||
      chunk.querySelector('.anka-label-essentials select[name$=".masterVmId"], .anka-label-essentials select[name="_.masterVmId"]');
    var tagSelect = summaryControl(chunk, 'tag') ||
      chunk.querySelector('.anka-label-essentials select[name$=".tag"], .anka-label-essentials select[name="_.tag"]');
    var capInput = summaryControl(chunk, 'instanceCapacity') ||
      chunk.querySelector('.anka-label-essentials input[name$=".instanceCapacity"], .anka-label-essentials input[name="_.instanceCapacity"]');

    var nameEl = summary.querySelector('.anka-label-summary__name');
    var templateEl = summary.querySelector('.anka-label-summary__template');
    var tagEl = summary.querySelector('.anka-label-summary__tag');
    var launchEl = summary.querySelector('.anka-label-summary__launch');
    var capEl = summary.querySelector('.anka-label-summary__cap');
    var chevron = summary.querySelector('.anka-label-summary__chevron');

    // Prefer live form values; keep server-rendered text if the control is not ready yet.
    var name = text(labelInput);
    if (!name && nameEl) name = String(nameEl.textContent || '').trim();
    if (!name) name = '(unnamed)';

    var template = templateDisplayName(templateSelect);
    // Avoid keeping a stale UUID from first paint once the named option is available
    if (!template && templateEl) {
      var existing = String(templateEl.textContent || '').trim();
      if (existing && existing !== '—') template = existing;
    }
    if (!template) template = '—';

    var tag = tagSelect ? tagDisplayName(tagSelect) : '(latest tag)';

    var launch = launchMethod(chunk) || '—';
    var cap = capacityLabel(text(capInput));

    if (nameEl) nameEl.textContent = name;
    if (templateEl) templateEl.textContent = template;
    if (tagEl) {
      tagEl.textContent = tag;
      tagEl.hidden = false;
    }
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

  function scheduleSummaryResync(chunk) {
    // Jenkins fills f:select options asynchronously after first paint.
    [0, 100, 400, 1200].forEach(function (ms) {
      setTimeout(function () {
        if (document.contains(chunk)) syncSummary(chunk);
      }, ms);
    });
  }

  function setExpanded(chunk, expanded) {
    // data-* survives Jenkins repeatableSupport.update() className replacement
    chunk.setAttribute('data-anka-expanded', expanded ? 'true' : 'false');
    syncSummary(chunk);
  }

  function setShowMore(chunk, open) {
    chunk.setAttribute('data-anka-show-more', open ? 'true' : 'false');
  }

  function syncShowMoreButton(chunk) {
    var toggle = chunk.querySelector('.anka-label-show-more__toggle');
    var panel = chunk.querySelector('.anka-label-show-more__panel');
    if (!toggle || !panel) return;
    var open = chunk.getAttribute('data-anka-show-more') === 'true';
    if (open) {
      panel.removeAttribute('hidden');
      toggle.setAttribute('aria-expanded', 'true');
      toggle.textContent = 'Hide Advanced Options';
    } else {
      panel.setAttribute('hidden', 'hidden');
      toggle.setAttribute('aria-expanded', 'false');
      toggle.textContent = 'Show Advanced Options';
    }
  }

  function isLabelChunk(chunk) {
    return !!(chunk && chunk.querySelector('.anka-label-summary'));
  }

  // Delegated handlers survive Jenkins cloning the master template (which can
  // copy data-anka-bound flags and skip per-element listeners on new cards).
  function bindDelegatedInteractions(root) {
    if (!root || root.getAttribute('data-anka-delegate') === '1') return;
    root.setAttribute('data-anka-delegate', '1');

    root.addEventListener('click', function (e) {
      var showMore = e.target.closest('.anka-label-show-more__toggle');
      if (showMore && root.contains(showMore)) {
        e.preventDefault();
        e.stopPropagation();
        var showChunk = showMore.closest('.repeated-chunk');
        if (!isLabelChunk(showChunk)) return;
        var panel = showChunk.querySelector('.anka-label-show-more__panel');
        if (!panel) return;
        var open = panel.hasAttribute('hidden');
        setShowMore(showChunk, open);
        syncShowMoreButton(showChunk);
        return;
      }

      var summary = e.target.closest('.anka-label-summary');
      if (!summary || !root.contains(summary)) return;
      if (e.target.closest('.dd-handle, .anka-label-summary__drag, .anka-label-delete, .anka-label-delete-footer, a, button, input, select, textarea')) {
        return;
      }
      var chunk = summary.closest('.repeated-chunk');
      if (!isLabelChunk(chunk)) return;
      setExpanded(chunk, !isExpanded(chunk));
    });

    root.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      var summary = e.target.closest('.anka-label-summary');
      if (!summary || e.target !== summary || !root.contains(summary)) return;
      e.preventDefault();
      var chunk = summary.closest('.repeated-chunk');
      if (!isLabelChunk(chunk)) return;
      setExpanded(chunk, !isExpanded(chunk));
    });

    root.addEventListener('input', function (e) {
      var chunk = e.target.closest('.repeated-chunk');
      if (isLabelChunk(chunk)) syncSummary(chunk);
    });
    root.addEventListener('change', function (e) {
      var chunk = e.target.closest('.repeated-chunk');
      if (isLabelChunk(chunk)) syncSummary(chunk);
    });
  }

  function enhanceChunk(chunk, opts) {
    opts = opts || {};
    if (!isLabelChunk(chunk) || chunk.classList.contains('to-be-removed')) return;
    composeHeader(chunk);
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
    syncShowMoreButton(chunk);
    scheduleSummaryResync(chunk);
    chunk.setAttribute('data-anka-initialized', '1');
  }

  function enhanceAll(defaultExpanded) {
    var chunks = document.querySelectorAll(LIST + ' .repeated-chunk');
    chunks.forEach(function (chunk) {
      enhanceChunk(chunk, { expanded: defaultExpanded });
    });
  }

  function isDraggingLabels(root) {
    var container = root && root.querySelector('.repeated-container');
    return !!(container && container.getAttribute('data-anka-dragging') === '1');
  }

  /**
   * Track drag via the handle only. Never re-register Sortable (that blocked dragging).
   */
  function trackLabelDragging(root) {
    var container = root.querySelector('.repeated-container');
    if (!container || container.getAttribute('data-anka-drag-track') === '1') return;
    container.setAttribute('data-anka-drag-track', '1');

    function clearDragging() {
      container.removeAttribute('data-anka-dragging');
      document.documentElement.style.removeProperty('--anka-label-drag-width');
    }

    container.addEventListener('pointerdown', function (e) {
      if (!e.target.closest('.dd-handle, .anka-label-summary__drag')) return;
      var chunk = e.target.closest('.repeated-chunk');
      if (chunk) {
        var width = Math.round(chunk.getBoundingClientRect().width);
        if (width > 0) {
          document.documentElement.style.setProperty('--anka-label-drag-width', width + 'px');
        }
      }
      container.setAttribute('data-anka-dragging', '1');
    });

    document.addEventListener('pointerup', clearDragging);
    document.addEventListener('pointercancel', clearDragging);
  }

  function findSortableInstance(el) {
    if (!el) return null;
    for (var key in el) {
      try {
        var value = el[key];
        // Sortable stores itself on the element (expando); require option() + toArray().
        if (
          value &&
          value.el === el &&
          typeof value.option === 'function' &&
          typeof value.toArray === 'function'
        ) {
          return value;
        }
      } catch (err) { /* ignore */ }
    }
    return null;
  }

  /**
   * Keep the 2-col grid visible. Kill swap loops by cancelling (return false)
   * disagreeing / rapid onMove events — returning 1/-1 is not reliable in
   * Jenkins' Sortable build.
   */
  function enableGridFriendlySwap(container) {
    if (!container || container.getAttribute('data-anka-invert-swap') === '1') return;
    var attempts = 0;
    var lastMoveDecision = null;
    var lastAcceptedMoveAt = 0;
    var MOVE_DEBOUNCE_MS = 280;

    function wantInsertAfter(evt, originalEvent) {
      var related = evt && evt.related;
      if (!related || !originalEvent || typeof originalEvent.clientX !== 'number') {
        return null;
      }
      var rect = related.getBoundingClientRect();
      var y = originalEvent.clientY;
      var sameRow = y >= rect.top - 12 && y <= rect.bottom + 12;
      if (!sameRow) return null;

      var ratio = (originalEvent.clientX - rect.left) / Math.max(rect.width, 1);
      if (lastMoveDecision && lastMoveDecision.related === related) {
        // Stick until the pointer clearly crosses into the other half.
        if (lastMoveDecision.after) {
          return ratio > 0.28;
        }
        return ratio >= 0.72;
      }
      return ratio >= 0.5;
    }

    function tryApply() {
      var sortable = findSortableInstance(container);
      if (sortable) {
        try {
          sortable.option('invertSwap', true);
          sortable.option('swapThreshold', 0.65);
          sortable.option('fallbackOnBody', false);
          container.setAttribute('data-anka-invert-swap', '1');

          ['onStart', 'onEnd', 'onChange', 'onMove'].forEach(function (hook) {
            var prev = sortable.option(hook);
            sortable.option(hook, function (evt, originalEvent) {
              if (hook === 'onStart') {
                lastMoveDecision = null;
                lastAcceptedMoveAt = 0;
              }
              if (hook === 'onChange') {
                lastAcceptedMoveAt = Date.now();
              }
              if (hook === 'onEnd') {
                lastMoveDecision = null;
                lastAcceptedMoveAt = 0;
              }
              if (hook === 'onMove') {
                if (typeof prev === 'function') {
                  var prevResult = prev.call(this, evt, originalEvent);
                  if (prevResult === false) return false;
                }
                var now = Date.now();
                if (lastAcceptedMoveAt && now - lastAcceptedMoveAt < MOVE_DEBOUNCE_MS) {
                  return false;
                }
                var wantAfter = wantInsertAfter(evt, originalEvent);
                if (wantAfter === null) {
                  if (
                    evt &&
                    evt.related &&
                    lastMoveDecision &&
                    lastMoveDecision.related === evt.related &&
                    lastMoveDecision.after !== !!evt.willInsertAfter &&
                    now - lastMoveDecision.t < MOVE_DEBOUNCE_MS
                  ) {
                    return false;
                  }
                  if (evt && evt.related) {
                    lastMoveDecision = {
                      related: evt.related,
                      after: !!evt.willInsertAfter,
                      t: now
                    };
                  }
                  return;
                }
                lastMoveDecision = {
                  related: evt.related,
                  after: wantAfter,
                  t: now
                };
                if (wantAfter !== !!evt.willInsertAfter) {
                  return false;
                }
                return;
              }
              if (typeof prev === 'function') return prev.call(this, evt, originalEvent);
            });
          });
        } catch (e) { /* ignore */ }
        return;
      }
      if (++attempts < 40) {
        setTimeout(tryApply, 50);
      }
    }
    tryApply();
  }

  function watchForNewChunks(root) {
    if (!root || typeof MutationObserver === 'undefined') return;
    var observer = new MutationObserver(function (mutations) {
      // Reordering moves nodes; never recompose mid-drag (that stacks/overlaps cards).
      if (isDraggingLabels(root)) return;
      mutations.forEach(function (m) {
        m.addedNodes.forEach(function (node) {
          if (!(node instanceof HTMLElement)) return;
          var chunks = [];
          if (node.classList.contains('repeated-chunk')) chunks.push(node);
          node.querySelectorAll && node.querySelectorAll('.repeated-chunk').forEach(function (c) {
            chunks.push(c);
          });
          chunks.forEach(function (chunk) {
            if (!isLabelChunk(chunk) || chunk.classList.contains('to-be-removed')) return;
            if (!isInitialized(chunk)) {
              enhanceChunk(chunk, { expanded: true });
            } else {
              // Jenkins may re-inject header/delete after update(); recompose.
              composeHeader(chunk);
              syncSummary(chunk);
              syncShowMoreButton(chunk);
            }
          });
        });
      });
    });
    observer.observe(root, { childList: true, subtree: true });
  }

  function enableDragDrop(root) {
    var container = root.querySelector('.repeated-container');
    if (!container) return;

    // Jenkins wires Sortable when with-drag-drop is present (f:repeatable @header).
    var alreadyEnabled = container.classList.contains('with-drag-drop');
    container.classList.add('with-drag-drop');
    if (!alreadyEnabled && typeof window.registerSortableDragDrop === 'function') {
      window.registerSortableDragDrop(container);
    }
    container.setAttribute('data-anka-sortable', '1');
    trackLabelDragging(root);
    enableGridFriendlySwap(container);
  }

  function onReady(fn) {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', fn);
    } else {
      fn();
    }
  }

  onReady(function () {
    var root = document.querySelector(LIST);
    if (!root) return;
    bindDelegatedInteractions(root);
    // Existing labels: collapsed. Newly added chunks: expanded via observer.
    enhanceAll(false);
    enableDragDrop(root);
    watchForNewChunks(root);
  });
})();
