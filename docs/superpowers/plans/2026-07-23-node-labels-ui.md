# Node Labels UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Anka Build Cloud **Node Labels** repeatable UI into a 2-column accordion with an essentials grid, progressive “Show more”, and visible drag reorder — without changing field binding, CasC, or the Labels API.

**Architecture:** Keep `f:repeatable field="templates"` and `AnkaCloudSlaveTemplate/config.jelly` as the form source of truth. Restructure that jelly into summary hooks + essentials + Show more. Drive accordion/collapse, header chips, and new-chunk defaults from an extended `AnkaMgmtCloud` JS adjunct. Scope layout CSS under `.anka-build-plugin-templates-list` so `CreateDynamicAnkaNodeStep` (which only shares the template CSS adjunct, not this jelly) is unaffected by the grid/accordion.

**Tech Stack:** Jenkins Jelly (`f:*`), Stapler adjuncts (CSS/JS), existing `anka-mgmt-cloud-config.js` / `.css`, Maven/`JenkinsRule` for regression smoke.

**Spec:** `docs/superpowers/specs/2026-07-23-node-labels-ui-design.md`  
**Mockup:** `docs/mockups/node-labels-option-c-v2-drag.html`

## Global Constraints

- No CasC / Labels API / `AnkaCloudSlaveTemplate` Java field renames or removals.
- Preserve Stapler `name="_.…"` bindings for all existing fields.
- Jenkins baseline remains as in `pom.xml` (`jenkins.baseline` 2.528 / `jenkins.version` `${jenkins.baseline}.3`).
- Multiple labels may stay expanded; existing labels load collapsed; newly added labels start expanded.
- Essentials (expanded): Target Label, VM Template, Tag, Launch Method, VCPU, VRAM, Max nodes, Executors.
- Drag reorder must persist as `templates` list order on Save.
- MVN needs `export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null | head -1)` when running tests locally.
- Never add `Co-authored-by: Cursor` to commits.

## File map

| File | Responsibility |
|------|----------------|
| `src/main/resources/.../AnkaCloudSlaveTemplate/config.jelly` | Field order: essentials vs Show more markup wrappers |
| `src/main/resources/.../AnkaCloudSlaveTemplate/anka-cloud-slave-template-config.css` | Essentials grid + Show more styles (safe for shared adjunct) |
| `src/main/resources/.../AnkaMgmtCloud/anka-mgmt-cloud-config.css` | 2-col grid, expanded span, accordion chrome, drag header (scoped to templates list) |
| `src/main/resources/.../AnkaMgmtCloud/anka-mgmt-cloud-config.js` | Accordion toggle, chip sync, new-chunk expanded, replace pink `.label` bar behavior |
| `docs/superpowers/specs/2026-07-23-node-labels-ui-design.md` | Source of truth (already committed) |
| `.gitignore` | Ignore `.superpowers/` brainstorm sessions |

`CreateDynamicAnkaNodeStep/config.jelly` is **not** rewritten; it has its own fields and only includes the template CSS adjunct.

---

### Task 1: Ignore brainstorm artifacts + jelly structure (essentials / Show more)

**Files:**
- Modify: `.gitignore`
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/config.jelly`
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/anka-cloud-slave-template-config.css`

**Interfaces:**
- Consumes: existing `f:entry` fields and names in `config.jelly`
- Produces: wrappers `.anka-label-template`, `.anka-label-summary` (empty shell for JS), `.anka-label-body`, `.anka-label-essentials`, `.anka-label-show-more` (+ toggle button markup)

- [ ] **Step 1: Add `.superpowers/` to `.gitignore`**

Append:

```gitignore
.superpowers/
```

- [ ] **Step 2: Restructure `AnkaCloudSlaveTemplate/config.jelly`**

Replace the flat field list with this structure (keep every existing `f:entry` / block; only regroup). Exact target shape:

```xml
<j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:f="/lib/form" xmlns:c="/lib/credentials">
  <st:adjunct includes="com.veertu.plugin.anka.AnkaCloudSlaveTemplate.anka-cloud-slave-template-config"/>

  <div class="anka-label-template">
    <div class="anka-label-summary" role="button" tabindex="0" aria-expanded="false">
      <span class="anka-label-summary__chevron" aria-hidden="true">▸</span>
      <span class="anka-label-summary__name"></span>
      <span class="anka-label-summary__template chip"></span>
      <span class="anka-label-summary__launch chip"></span>
      <span class="anka-label-summary__cap chip"></span>
    </div>

    <div class="anka-label-body">
      <div class="anka-cloud-slave-template-hidden">
        <!-- existing cloudName entry unchanged -->
      </div>

      <div class="anka-label-essentials">
        <!-- Target Label (field=label) -->
        <!-- Anka VM Template (field=masterVmId) -->
        <!-- Anka VM Template's Tag (field=tag) -->
        <!-- Launch Method (entire existing f:entry + radioBlocks + java shared fields) -->
        <!-- Anka VCPU -->
        <!-- Anka VRAM -->
        <!-- Maximum Allowed Nodes/Agents (field=instanceCapacity) -->
        <!-- Allowed Executors (field=numberOfExecutors) -->
      </div>

      <div class="anka-label-show-more">
        <button type="button" class="anka-label-show-more__toggle" aria-expanded="false">
          Show more
        </button>
        <div class="anka-label-show-more__panel" hidden="hidden">
          <!-- Jenkins Node Name Template -->
          <!-- Node/Agent Description -->
          <!-- Anka VM Workspace Path -->
          <!-- Keep Alive on Error -->
          <!-- Environment Variables block -->
          <!-- hr + Anka VM Template/Tag Creation section (existing) -->
          <!-- f:advanced block (existing) -->
        </div>
      </div>
    </div>
  </div>
</j:jelly>
```

**Launch Method note (matches spec intent):** Keep the full Launch Method `f:entry` (radios + nested SSH/JNLP + Java options/path) inside `.anka-label-essentials` so Jenkins radioBlock show/hide keeps working. In Task 3 CSS/JS, when Show more is closed, hide only the *nested* launch details (`.anka-launch-method .radio-block-start ~ …` / nested form containers and `.anka-launch-method-shared`) while leaving the SSH/JNLP radio titles visible in essentials. If that selector is fragile on the baseline, fall back to leaving the full launch block in essentials (still acceptable) and document the fallback in the PR.

Move out of essentials into Show more panel: `nameTemplate`, `templateDescription`, `remoteFS`, `keepAliveOnError`, environments, save-image section, `f:advanced`.

Remove the pink-bar placeholder `<label class="label"></label>` (header chips replace it).

- [ ] **Step 3: Add essentials / Show more CSS to template adjunct**

Append to `anka-cloud-slave-template-config.css`:

```css
.anka-label-template {
  display: block;
}

.anka-label-essentials {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem 1rem;
  align-items: start;
}

@media (max-width: 1100px) {
  .anka-label-essentials {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .anka-label-essentials {
    grid-template-columns: 1fr;
  }
}

/* Launch method spans full essentials row so radios stay usable */
.anka-label-essentials > .jenkins-form-item:has(.anka-launch-method),
.anka-label-essentials > .form-group:has(.anka-launch-method) {
  grid-column: 1 / -1;
}

.anka-label-show-more {
  margin-top: 0.75rem;
}

.anka-label-show-more__toggle {
  display: block;
  width: 100%;
  text-align: left;
  background: #faf7ff;
  border: 1px dashed #c9b3e8;
  border-radius: 6px;
  color: #5e2e9b;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
}

.anka-label-show-more__panel {
  margin-top: 0.75rem;
}

.anka-label-show-more__panel[hidden] {
  display: none !important;
}
```

- [ ] **Step 4: Build to ensure jelly packages**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null | head -1)
mvn -Daether.remoteRepositoryFilter.prefixes=false -DskipTests -Dspotbugs.skip=true -Dinvoker.skip=true clean package
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add .gitignore \
  src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/config.jelly \
  src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/anka-cloud-slave-template-config.css
git commit -m "$(cat <<'EOF'
Restructure Node Label template form into essentials and Show more.

Group common fields for the accordion redesign while preserving Stapler field names and bindings.
EOF
)"
```

---

### Task 2: Cloud list layout — 2-column grid, accordion chrome, drag header

**Files:**
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css`

**Interfaces:**
- Consumes: `.anka-build-plugin-templates-list .repeated-chunk`, `.anka-label-*` from Task 1
- Produces: CSS classes/state hooks `.is-expanded`, `.anka-label-summary`, visible `.repeated-chunk__header` / `.dd-handle`

- [ ] **Step 1: Replace templates-list layout rules**

In `anka-mgmt-cloud-config.css`, replace the block from `.anka-build-plugin-templates-list .repeated-container` through `.repeated-chunk__header { display: none !important; }` with:

```css
.anka-build-plugin-templates-list .repeated-container {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  align-items: start;
}

@media (max-width: 900px) {
  .anka-build-plugin-templates-list .repeated-container {
    grid-template-columns: 1fr;
  }
}

.anka-build-plugin-templates-list .repeated-chunk {
  border: 1px solid #60259F;
  background-color: #FFF;
  padding: 0 !important;
  margin: 0;
  color: #5e2e9b;
  flex: unset;
  flex-basis: unset;
  min-width: 0;
  border-radius: 8px;
  overflow: hidden;
}

.anka-build-plugin-templates-list .repeated-chunk.is-expanded {
  grid-column: 1 / -1;
}

.anka-build-plugin-templates-list .repeated-chunk:nth-child(odd) {
  background-color: #f2e8fd;
}

.anka-build-plugin-templates-list .repeated-chunk button:not(.repeatable-delete) {
  background-color: #FFF;
  color: #5e2e9b !important;
}

/* Jenkins drag header — was hidden; restyle as grab affordance */
.anka-build-plugin-templates-list .repeated-chunk__header {
  display: flex !important;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.75rem;
  background: #efe6fa;
  border-bottom: 1px solid #d4c5e8;
  cursor: grab;
}

.anka-build-plugin-templates-list .repeated-chunk__header .dd-handle {
  font-size: 1.1rem;
  margin: 0;
  cursor: grab;
}

.anka-build-plugin-templates-list .anka-label-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem 0.6rem;
  padding: 0.65rem 0.75rem;
  background: #f2e8fd;
  cursor: pointer;
  user-select: none;
}

.anka-build-plugin-templates-list .anka-label-summary__name {
  font-weight: 700;
  min-width: 6rem;
}

.anka-build-plugin-templates-list .anka-label-summary .chip {
  display: inline-block;
  background: #fff;
  border: 1px solid #d4c5e8;
  border-radius: 3px;
  padding: 0.1rem 0.4rem;
  font-size: 0.75rem;
  color: #555;
}

.anka-build-plugin-templates-list .repeated-chunk:not(.is-expanded) .anka-label-body {
  display: none;
}

.anka-build-plugin-templates-list .repeated-chunk.is-expanded .anka-label-summary__chevron::before {
  /* chevron text swapped in JS; keep layout stable */
}

.anka-build-plugin-templates-list .anka-label-body {
  padding: 0.75rem 1rem 1rem;
}

.anka-build-plugin-templates-list .show-if-only {
  padding: 0 0.75rem 0.75rem;
  text-align: right;
}

/* Legacy pink bar removed from jelly; hide if any leftover */
.anka-build-plugin-templates-list .repeated-chunk > .label {
  display: none;
}
```

Also ensure `.repeatable-add` still spans nicely (keep existing pink Add button rules).

- [ ] **Step 2: Manual visual check checklist (write into PR notes; no code)**

After later JS lands, verify on a wide window:
1. Two collapsed labels sit side by side  
2. Expanded label spans full width  
3. Drag handle row is visible at top of each chunk  

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css
git commit -m "$(cat <<'EOF'
Lay out Node Labels as a two-column accordion grid.

Show Jenkins drag headers again and span expanded label cards across the full row.
EOF
)"
```

---

### Task 3: Accordion behavior + header chip sync + Show more toggle

**Files:**
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.js`

**Interfaces:**
- Consumes: DOM under `.anka-build-plugin-templates-list .repeated-chunk` with `.anka-label-summary`, inputs `_.label`, `_.masterVmId` / selects, `_.tag`, `_.instanceCapacity`, launch radios `launchMethod`
- Produces: chunk class `is-expanded`; summary text content; Show more `aria-expanded` / `hidden` on panel; MutationObserver for repeatable add

- [ ] **Step 1: Replace the templates-list IIFE in `anka-mgmt-cloud-config.js`**

Keep `validateAnkaMgmtUrl` as-is. Replace the second IIFE (the `repeated-chunk` / `.label` logic) with:

```javascript
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
    var checked = chunk.querySelector('input[type="radio"][name$="launchMethod"]:checked');
    if (!checked) {
      checked = chunk.querySelector('.anka-launch-method input[type="radio"]:checked');
    }
    if (!checked) return '';
    return String(checked.value || '').toLowerCase() === 'jnlp' ? 'JNLP' : 'SSH';
  }

  function capacityLabel(raw) {
    if (raw === '' || raw === '0') return 'unlimited';
    return 'cap ' + raw;
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
    if (launchEl) launchEl.textContent = launch;
    if (capEl) capEl.textContent = cap;

    var expanded = chunk.classList.contains('is-expanded');
    summary.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    if (chevron) chevron.textContent = expanded ? '▾' : '▸';
  }

  function setExpanded(chunk, expanded) {
    chunk.classList.toggle('is-expanded', expanded);
    syncSummary(chunk);
  }

  function bindShowMore(chunk) {
    var toggle = chunk.querySelector('.anka-label-show-more__toggle');
    var panel = chunk.querySelector('.anka-label-show-more__panel');
    if (!toggle || !panel || toggle.dataset.ankaBound === '1') return;
    toggle.dataset.ankaBound = '1';
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
      chunk.classList.toggle('anka-show-more-open', open);
    });
  }

  function bindSummaryToggle(chunk) {
    var summary = chunk.querySelector('.anka-label-summary');
    if (!summary || summary.dataset.ankaBound === '1') return;
    summary.dataset.ankaBound = '1';
    function toggle() {
      setExpanded(chunk, !chunk.classList.contains('is-expanded'));
    }
    summary.addEventListener('click', function (e) {
      if (e.target.closest('a, button, input, select, textarea, label')) return;
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
    bindSummaryToggle(chunk);
    bindShowMore(chunk);
    bindFieldSync(chunk);
    if (typeof opts.expanded === 'boolean') {
      setExpanded(chunk, opts.expanded);
    } else if (!chunk.classList.contains('is-expanded') && !chunk.classList.contains('anka-label-initialized')) {
      setExpanded(chunk, false);
    } else {
      syncSummary(chunk);
    }
    chunk.classList.add('anka-label-initialized');
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
            if (!chunk.classList.contains('anka-label-initialized')) {
              enhanceChunk(chunk, { expanded: true });
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
```

If launch nested-detail hiding is implemented, add CSS:

```css
.anka-build-plugin-templates-list .repeated-chunk:not(.anka-show-more-open) .anka-launch-method .form-container,
.anka-build-plugin-templates-list .repeated-chunk:not(.anka-show-more-open) .anka-launch-method-shared {
  display: none;
}
```

and hide radioBlock nested bodies when Show more is closed — only if manual QA shows the nested blocks are identifiable without breaking credential selects. Prefer shipping with full launch in essentials if selectors are unreliable.

- [ ] **Step 2: Package + unit smoke (descriptor still loads)**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null | head -1)
mvn -Daether.remoteRepositoryFilter.prefixes=false -Dtest=AnkaMgmtCloudDescriptorTest,ConfigurationAsCodeTest test
```

Expected: tests **PASS** (CasC/cloud descriptor unchanged).

- [ ] **Step 3: Manual QA against acceptance criteria**

With `hpi:run` or local Jenkins + built `.hpi`:

1. ≥2 labels → collapsed cards two-up on wide viewport  
2. Expand one → full width; expand second → both stay open  
3. Essentials show the eight fields; other fields behind Show more  
4. Reload config page → labels collapsed; Add → new label expanded  
5. Drag reorder via header handle → Save → reopen → order persisted  
6. Header chips update when label/template/tag/launch/capacity change  
7. Save a label with SSH creds + env var + advanced priority → reload → values intact  

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.js \
  src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css
git commit -m "$(cat <<'EOF'
Add Node Labels accordion toggles and summary chips.

Sync collapsed headers from form fields and expand newly added label rows by default.
EOF
)"
```

---

### Task 4: Polish, delete button placement, README pointer (optional but recommended)

**Files:**
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/config.jelly` (only if delete button / repeatable wrapper needs a class)
- Modify: `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css` (spacing for delete)
- Modify: `README.md` — one line under docs pointing to the design spec **only if** README already links similar docs; otherwise skip README (YAGNI)

**Interfaces:**
- Consumes: Task 2–3 UI
- Produces: Delete control not colliding with drag handle; no functional API docs required

- [ ] **Step 1: Ensure delete control remains usable**

In `AnkaMgmtCloud/config.jelly` the repeatable already has:

```xml
<div class="show-if-only ">
  <f:repeatableDeleteButton />
</div>
```

If delete ends up inside the hidden collapsed body, move that div to sit after `.anka-label-summary` by adjusting include wrapper — prefer CSS so delete stays visible when collapsed:

```css
.anka-build-plugin-templates-list .repeated-chunk:not(.is-expanded) .show-if-only {
  display: block;
  padding: 0.5rem 0.75rem 0.75rem;
}
```

If the delete button is inside `config.jelly` body only, add a small delete row in `AnkaMgmtCloud/config.jelly` **outside** the include but inside the repeatable (keep `f:repeatableDeleteButton` once only — do not duplicate).

- [ ] **Step 2: Re-run CasC + descriptor tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null | head -1)
mvn -Daether.remoteRepositoryFilter.prefixes=false -Dtest=AnkaMgmtCloudDescriptorTest,ConfigurationAsCodeTest,AnkaLabelsApiTest test
```

Expected: **PASS**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/config.jelly \
  src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css \
  README.md
git commit -m "$(cat <<'EOF'
Polish Node Labels delete control for collapsed accordion cards.

Keep remove accessible without expanding a label.
EOF
)"
```

(Skip `README.md` in `git add` if unchanged.)

---

## Spec coverage check

| Spec requirement | Task |
|------------------|------|
| 2-column grid; expanded full width | Task 2 |
| Independent multi-expand | Task 3 |
| Essentials 8 fields | Task 1 |
| Show more grouping + defaults | Task 1 + 3 |
| Drag reorder visible/persist | Task 2 (+ Jenkins repeatable) |
| Header chips | Task 3 |
| No API/CasC/field regressions | Task 3–4 tests |
| CreateDynamicAnkaNodeStep unaffected | Scoped CSS under templates-list; step has own jelly |

## Plan self-review

- No TBD placeholders remain; launch nested-hide has an explicit fallback.
- Selectors use `name$=".field"` to tolerate Jenkins repeatable prefixes.
- Existing pink `.label` JS removed/replaced intentionally in Task 3.
