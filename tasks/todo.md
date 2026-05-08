# Todo

## Victim History List-Only UI

- [x] Remove detail interactions from history list UI
- [x] Remove detail CTA from history card layout
- [ ] Validate list-only behavior and document results

## Victim History Detail Fix

- [x] Inspect victim history list/detail data flow (Android + API)
- [x] Fix API history item id mapping for detail lookup
- [ ] Validate detail flow and document results

- [x] Review current outbound inventory flow + UI entry points
- [x] Design outbound adjustments (mission codeName lookup, relaxed quantity rules, log reference update)
- [x] Implement backend use cases + service wiring + repository updates
- [x] Update staff UI for outbound manual entry (no QR), strings/dimens
- [x] Add/update unit tests for outbound inventory service/usecase
- [ ] Review results and note follow-ups

## Staff Tasks - Upcoming List Fix

- [x] Review staff tasks UI, API, and data flow
- [x] Add staff tasks domain/repository/usecase/viewmodel
- [x] Refactor StaffTasksFragment + adapter bindings
- [ ] Verify list rendering and error states

## Review (Staff Tasks)

- Status: In progress
- Notes: Awaiting runtime verification in app.

## Review (Victim History Detail Fix)

- Status: Pending verification
- Notes: History list now returns request id for detail lookup; backend tests not run.

## Review (Victim History List-Only UI)

- Status: Pending verification
- Notes: Detail button/click removed; no history detail API triggered from UI.

## Review

- Status: Pending test run
- Notes: Outbound preview now includes aid request detail; export quantities are editable and validated against stock.
