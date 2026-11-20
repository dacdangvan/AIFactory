# Content Item Workflow Runtime

## Authoring actions

The Content Item editor exposes workflow actions returned by the backend for the item's current state and the signed-in user's Product roles.

- **Save Draft**: writes a new immutable content version, then records the SAVE workflow event without changing state.
- **Submit for Review**: writes the latest content version, then executes the configured SUBMIT transition.
- **Recall**: executes the configured RECALL transition, normally from In Review to Draft.
- **Approve / Reject**: execute the configured manager transitions. Reject requires a reason, stored in workflow history.
- **Resubmit**: executes the configured transition from Rejected back to In Review.
- **Publish**: executes the configured publish transition.
- **Delete**: soft-deletes an item when its owner is in the initial state or Rejected. If an explicit DELETE permission contains DENY, that denial takes precedence.

The action toolbar is not hard-coded to a fixed state machine. It is populated from `cms_workflow_action_permissions` and `cms_workflow_transitions`; only allowed actions with a valid transition (plus SAVE) are returned.

## Runtime APIs

- `GET /api/v1/content-items/{id}/workflow-actions` — current state, lock version, canEdit and available actions.
- `POST /api/v1/content-items/{id}/workflow-actions` — execute an action. Body: `{ "action": "APPROVE", "comment": "optional", "expectedLockVersion": 0 }`.
- `GET /api/v1/content-items/{id}/workflow-history` — immutable action history.
- `DELETE /api/v1/content-items/{id}` — owner delete policy, implemented as a DELETE workflow event.

Workflow execution checks the user's Product role assignments against the role codes in each workflow user rule. DENY takes precedence over ALLOW. The backend re-checks permissions for every action and uses `cms_workflow_instances.lock_version` to reject stale concurrent workflow operations.

## Default VIB role aliases

The starter workflow UI still uses readable rule names. The runtime maps them to the current VIB RBAC roles:
- EDITOR → BU
- MANAGER → PO_ADMIN
- PUBLISHER → DEV_ADMIN or PO_ADMIN
- SYSTEM_ADMIN bypasses workflow role matching.

These aliases are defaults for the current role catalogue. If the enterprise role model changes, update the mapping in `ContentItemService.matchesWorkflowRule`.

## Visibility rules

The Content Item list and detail endpoints evaluate `cms_workflow_visibility_rules`:
- ALL grants visibility to matching user rules.
- OWNED / CREATED_BY_ME grants visibility to the owner.
- NONE denies visibility for the matching rule.
- If a state has no visibility rules, the owner and SYSTEM_ADMIN can view it.
- ASSIGNED_TO_ME is not active yet because the current workflow schema does not yet include task-assignment records.

For the starter workflow, Editors can see their own Draft/In Review/Rejected items; Managers can see all In Review and Approved items but not Draft items. SYSTEM_ADMIN can view all states.

## Rejected content

The starter workflow now grants EDITOR the SAVE action in REJECTED as well as RESUBMIT, allowing the Editor to revise the rejected content before resubmission.

## Important versioning note

Workflow definitions are versioned and Content Items pin a workflow version. The existing Workflow Configuration UI creates a workflow version but does not yet edit an existing workflow's immutable state/action/permission graph. If a workflow version already exists without the required permission rows, create a new configured workflow version and rebind the Product or Content Type; do not modify the historical version in place.

## Current limitations

- Notifications/email/Teams on submit, approve, reject and publish are not yet wired.
- Task assignment/delegation and SLA timers are not yet implemented.
- Publish currently changes workflow state only; immutable release manifests, route activation, cache invalidation and Public Delivery integration remain a later publishing phase.
- The role aliases above are a transitional mapping. Production must use the agreed Entra ID claims and authoritative RBAC assignments.


## Unpublish

- Content Items in workflow state `PUBLISHED` expose a publisher-only `UNPUBLISH` action to `DEV_ADMIN`, `PO_ADMIN`, and `SYSTEM_ADMIN`.
- The action moves the workflow instance back to the `APPROVED` state and records the actor/action in workflow history. The workflow version must contain an `APPROVED` state.
- This is currently a workflow-state unpublish only. It does not yet deactivate a public route, update a release manifest, invalidate CDN/cache, or remove content from the Next.js Public Delivery API; those require the Publishing/Delivery integration phase.
