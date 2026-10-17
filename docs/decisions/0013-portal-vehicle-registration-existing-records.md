# ADR-0013: Registering a Plate That Is Already on Record (Customer Portal)

## Status
**Accepted** - 2026-10-17

## Context
Most vehicles a customer adds in the portal already exist in the workshop data: a staff-entered customer
record (`tb_pelanggan`) owns the plate through `tb_pelanggan_kendaraan`. Until now `POST
/api/customer/vehicles` silently adopted such a record for a customer without their own vehicles: the account
was linked to it, giving the customer the record's name, address and full service history, while the form
showed nothing about it. Two problems follow:

- the customer could not confirm the record is theirs, or reuse its brand/type;
- anyone who knows a plate could take over that record without any check.

## Decision
The check endpoint (`GET /api/customer/vehicles/check-nopol`) returns `ON_RECORD` when the plate's current
owner is a record no portal account has claimed and the caller has no provisioned record of their own. The
response carries the vehicle's `merk`/`jenis` and the owner's name masked (`T*****3`). The portal shows "this
plate is already registered: {vehicle}, registered to {owner}. Is this your car?", with Yes/No buttons and a
button beside the plate field that prefills brand and type. The customer can still change both.

`POST /api/customer/vehicles` takes `ownRecord` (`CustomerVehicleRegisterRequest`):

| Answer | Outcome |
|---|---|
| Yes, and the record has no phone/email, or one matches the account | Record linked to the account; the caller's own empty portal record is merged into it. Status `REGISTERED`. |
| Yes, but the record's phone/email contradict the account's | A link request (`match_type = NOPOL`) goes to the staff approval queue; nothing changes yet. Status `PENDING_APPROVAL`. On approval, the caller's empty portal record is merged into the approved one. |
| No, or no answer | The record stays with its owner; only the vehicle's current ownership is transferred to the caller's own record (created if needed). Status `REGISTERED`. |

Brand/type the customer chose are applied to the vehicle in the two `REGISTERED` outcomes. Unchanged
paths: a plate owned by another account, or a caller who already has their own provisioned record, still
goes through the vehicle claim flow (staff approval); a plate on nobody's record is attached to the caller's record.

## Consequences
- **Positive**: The customer sees and confirms what they adopt, and can reuse the recorded brand/type.
- **Positive**: A record whose contact details point to someone else can no longer be taken over without staff.
- **Negative**: A signed-in customer can probe plates and learn brand/type plus a masked owner name. This is
  bounded by `NopolLookupRateLimiter`: 20 distinct plates per account and 60 per client IP per 15 minutes
  (configurable, see `docs/deployment.md`); re-checking a plate is free, and going over the limit returns
  429 with `Retry-After`. The counters are in memory, per instance. The register and claim endpoints, which
  reveal only "registered to another customer", are not throttled.
- **Negative**: Answering "No" transfers the vehicle immediately even when "Yes" would have needed approval,
  so a customer can take a plate (not the record or its history) from a walk-in without review. Staff can
  see it in the ownership history (`keterangan`) and transfer it back.
- **Trade-off**: Linking is decided by contact data, which most walk-in records don't have (37 of 2,438 in
  development), so the majority of links remain automatic.
