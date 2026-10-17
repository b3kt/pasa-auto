# ADR-0012: Customer Contact Details Come from the Portal Account

## Status
**Accepted** - 2026-10-17

## Context
ADR-0011 made `tb_pelanggan` the owner of a customer's contacts (`no_hp`, `no_telepon`, `email`, plus the
legacy `tlpn`), with `tb_customer_accounts` holding only credentials. In practice a customer who uses the
portal already keeps a phone and an email on their account, and keeping a second copy on `tb_pelanggan`
means two values that drift: profile edits, staff edits and account registration each wrote a different one.

The data does not allow moving contacts to the account for everyone. In the development database, 33 of
the 37 customers with a phone number are walk-ins without a portal account, and an account can't represent
them: `email` and `password_hash` are `NOT NULL`, and `phone` is unique, so customers sharing a number would
collide. The account also has no place for a landline.

### Options Considered
- **Account wins when linked (chosen)**: a linked customer's phone/email come from the account; walk-ins keep
  theirs on `tb_pelanggan`.
- **Every customer gets an account row**: make login fields nullable and drop the unique phone. Rejected:
  it erases the credentials/identity boundary from ADR-0009.
- **Drop the columns for everyone**: rejected, because it loses the walk-in phone numbers.

## Decision
1. **Linked customer**: `tb_customer_accounts.phone` and `email` are authoritative. `tb_pelanggan.no_hp`/`email`
   are ignored for reads and not written by the application. Staff cannot edit them (the staff form shows
   them read-only, and the API ignores them); the customer edits the phone in the portal profile.
2. **Walk-in customer** (no live account): `tb_pelanggan.no_hp`/`email` remain their contact, editable by staff.
3. **Landline** (`no_telepon`, `tlpn`): deprecated for everyone. It is no longer written, returned or matched,
   and is dropped by the ADR-0011 contract migration.
4. **Login email is not edited through the profile.** It is a credential; changing it needs its own
   verified flow. The profile's mobile number is written to `tb_customer_accounts.phone`, and since that is
   also a login identifier, it stays unique (the save is rejected if another account uses the number).
5. **One resolver.** `PelangganContactResolver` decides the effective contact everywhere: staff customer
   pages, ownership/history DTOs, receipts, the portal profile. Staff DTOs carry `contactFromAccount` so the
   UI can lock the fields. `v_pelanggan_legacy` applies the same rule in SQL.

### Login identifiers
A customer registers with an email, a mobile number, or both (at least one), and signs in with either.
- `tb_customer_accounts.email` is nullable; `chk_tb_customer_accounts_login_identifier` requires email or
  phone (`V37`). Both stay unique: email case-insensitively, phone in canonical form (`PhoneNormalizer`),
  and a phone must normalize to a plausible national number (`PhoneNormalizer.isValid`).
- `POST /api/customer/auth/login` takes `identifier`: anything containing `@` is looked up as an email,
  anything else as a phone in any format. The old `email` key is still accepted, so a portal bundle cached by
  the PWA keeps working. Unknown identifiers cost the same bcrypt check as a wrong password.
- Brute-force counters are keyed by the canonical identifier in a `customer-login:` namespace, so different
  spellings of one number share a counter and never collide with a staff username.
- A phone-only account's token carries no `email` claim, and its phone can't be cleared from the profile
  (it would leave no way to sign in).

### Migration
`V36__customer_contacts_from_account.sql` (rollback-safe) copies a linked customer's `no_hp` onto an account
that has no phone yet, unless the number is taken; redefines `v_pelanggan_legacy` with the resolved contact
and a NULL landline; marks the columns deprecated with `COMMENT ON`. The previous build keeps working: it
still reads and writes the `tb_pelanggan` columns, which are left in place.

## Consequences
- **Positive**: One phone/email per linked customer; what the customer edits in the portal is what staff see.
- **Positive**: No contact data is lost; walk-ins are unaffected.
- **Negative**: Two places hold contacts (account for linked, record for walk-ins), so every reader must go
  through `PelangganContactResolver` or the view.
- **Negative**: When a walk-in links an account, the record's own phone/email stop being shown. V36 carries
  the phone over once; numbers that collide with another account are left on the record for manual review.
- **Trade-off**: `tb_pelanggan.no_hp`/`email` can't be dropped while walk-ins exist; they are deprecated only
  for linked customers.
