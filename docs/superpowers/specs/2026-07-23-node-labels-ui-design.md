# Node Labels UI redesign

**Date:** 2026-07-23  
**Status:** Approved for planning  
**Scope:** Jenkins Anka Build Cloud → **Node Labels** (`AnkaCloudSlaveTemplate` repeatable list)

## Problem

The Node Labels area renders each template as a full form in a multi-column card grid (`flex-basis: 25%`). Every field is visible at once, so the page is tall, hard to scan, and cramped when several labels exist. Drag reorder exists in Jenkins `f:repeatable` but the plugin CSS hides the repeatable header, which makes reordering unclear.

## Goals

- Scan many labels without scrolling through every field
- Edit common settings in a compact, wide layout
- Keep rarely used settings behind progressive disclosure
- Allow drag reorder so admins can place two labels side by side in a 2-column grid
- Preserve form binding, validation, CasC, and Labels API (no schema changes)

## Non-goals

- Changing which fields exist or their names
- Labels HTTP API or Configuration as Code format changes
- Redesigning cloud header / Security sections (outside Node Labels)

## Chosen approach: Accordion + essentials grid (Option C)

Mockups: `docs/mockups/node-labels-option-c-v2-drag.html` (and companion brainstorm screens).

### Layout

- Labels use a **2-column CSS grid** when width allows; single column on narrow viewports.
- **Collapsed** cards occupy one grid cell (so two sit side by side).
- **Expanded** cards use `grid-column: 1 / -1` (full width) for comfortable editing.
- Multiple labels may stay expanded at once (independent accordions).
- Grid order is left→right, then next row — list order controls which pair appears side by side.

### Drag reorder

- Visible **drag handle** (⋮⋮) on the left of each card header (collapsed or expanded).
- Reorder updates the underlying `templates` list order and is saved with the cloud config (same as Jenkins `f:repeatable` drag).
- Only the handle initiates drag so it does not fight expand/collapse or text selection.

### Collapsed header

Always shows:

| Element | Source |
|--------|--------|
| Label name | Target Label |
| `template / tag` chip | Anka VM Template + Tag |
| Launch chip | SSH or JNLP |
| Capacity chip | Max Nodes/Agents (`unlimited` when `0`) |
| Delete | existing repeatable delete |

### Expanded body — essentials (always visible when expanded)

4-column grid (responsive wrap on smaller widths):

1. Target Label  
2. Anka VM Template  
3. Tag  
4. Launch Method  
5. VCPU  
6. VRAM  
7. Maximum Allowed Nodes/Agents  
8. Allowed Executors  

### Expanded body — Show more (collapsed by default)

In this order:

1. Jenkins Node Name Template, Node/Agent Description  
2. Anka VM Workspace Path, Keep Alive on Error  
3. Launch details (SSH credential / JNLP args, tunnel, override URL, Java options, Java path)  
4. Environment Variables  
5. Anka VM Template/Tag Creation (save image)  
6. Advanced (node group, priority, scheduling timeout, idle check timeout)

### Defaults

| State | Behavior |
|-------|----------|
| Page load (existing labels) | All **collapsed** |
| Newly added label | **Expanded**, essentials focused; Show more still collapsed |
| Show more | **Collapsed** until the user opens it |

## Implementation approach

### Keep

- `f:repeatable field="templates"` in `AnkaMgmtCloud/config.jelly`
- Existing `AnkaCloudSlaveTemplate` fields, descriptors, validation
- CasC / Labels API serialization unchanged

### Change

1. **`AnkaCloudSlaveTemplate/config.jelly`**  
   Restructure markup into: summary header hooks (or data attributes for JS), essentials block, Show more disclosure. Prefer `f:advanced` or a small custom disclosure wrapper that still submits the same fields.

2. **`AnkaMgmtCloud/anka-mgmt-cloud-config.css`** (and template adjunct CSS as needed)  
   - Replace multi-column `flex-basis: 25%` card layout with 2-column grid.  
   - Expanded chunk spans full width.  
   - Stop hiding drag affordance (`.repeated-chunk__header` / `.dd-handle`); restyle into the accordion header.  
   - Compact essentials grid; style Show more.

3. **JS adjunct** (new or extended under `AnkaCloudSlaveTemplate` / `AnkaMgmtCloud`)  
   - Toggle expand/collapse per chunk (multiple open allowed).  
   - Sync header chips from field values on input/change and after repeatable add.  
   - Ensure new repeatable chunks start expanded.  
   - Wire drag to the visible handle if Jenkins default handle placement is insufficient after CSS changes.

### Files likely touched

- `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/config.jelly` (Node Labels section only if needed)
- `src/main/resources/com/veertu/plugin/anka/AnkaMgmtCloud/anka-mgmt-cloud-config.css`
- `src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/config.jelly`
- `src/main/resources/com/veertu/plugin/anka/AnkaCloudSlaveTemplate/anka-cloud-slave-template-config.css`
- New or extended `.js` adjunct for accordion / header sync

### Risks

- Jenkins repeatable DOM varies by core version — test drag + expand on supported Jenkins baselines.
- Header chip sync must not break Stapler binding (read DOM values for display only).
- Expanded full-width + 2-col grid may need careful `align-items` so collapsed neighbors do not stretch oddly.

## Acceptance criteria

- [ ] With ≥2 labels, collapsed cards appear two-up on a wide viewport  
- [ ] Expanding a label spans full width; other labels can remain expanded  
- [ ] Essentials show the eight agreed fields; other fields are under Show more  
- [ ] Existing labels load collapsed; Add creates an expanded label  
- [ ] Drag handle reorders labels; order persists after Save  
- [ ] Collapsed header chips reflect label, template/tag, launch, capacity  
- [ ] No CasC / Labels API / field binding regressions  
- [ ] Create Dynamic Anka Node step still works if it shares template config adjuncts (verify)

## Reference mockups

- Current vs options: `docs/mockups/node-labels-ui-options.html`  
- Refined C: `docs/mockups/node-labels-option-c-refined.html`  
- C + drag: `docs/mockups/node-labels-option-c-v2-drag.html`  
