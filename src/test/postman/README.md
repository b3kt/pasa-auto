# API regression suite (Postman / Newman)

`pasa-auto-regression.postman_collection.json` exercises every REST endpoint under `/api/**` plus
`/health`. It covers the happy path and the edge cases for each one: validation errors, wrong role,
missing token, unknown and malformed ids, state-machine violations, unsafe `sortBy`, data-isolation
checks between customers, and response-shape checks (no password hashes, no refresh token in a body,
no pricing in the customer work-order view).

| Folder | What it covers |
|---|---|
| 00 Setup | Logs in Owner / Admin / Karyawan, generates unique run data, switches the customer portal on |
| 01 Auth - staff | Login, refresh-token rotation and cookie flags, `/me`, password policy, Google OAuth surface |
| 02 Access control | Role matrix: anonymous -> 401, wrong role -> 403, and the deliberate exceptions |
| 03 Master data CRUD | Barang, Jasa, Karyawan Posisi, Supplier, Sparepart, Merk, Kendaraan, Karyawan, Pelanggan |
| 04 Pelanggan & vehicle ownership | Attach, look up, transfer, owner history, refusal paths |
| 05 SPK & penjualan flow | Work order -> process -> sale -> print -> report -> cancel -> delete |
| 06 Pembelian flow | Numbering, header + lines, partial updates, filters |
| 07 Absensi | Clock-in/out state machine, absences, history, monthly summary |
| 08 Administration | Users and the temporary-password lockdown, roles, permissions, RBAC, system parameters, audit, caches, summary |
| 09 Customer portal | Gate, registration, linking, vehicles, claims plus staff approval, profile, bookings plus staff review, work orders, session |
| 99 Teardown | Removes fixtures, restores the portal switch, logs every session out |

The folders depend on each other (fixtures, tokens, cookies), so **run the whole collection in
order**. Running one folder on its own only works after `00 Setup`.

## Prerequisites

1. The application is running (`./mvnw quarkus:dev`) with its database.
2. The three staff accounts have **permanent passwords**. Seeded accounts are flagged
   `must_change_password`, and their tokens carry no roles until the password is changed. Log in
   once through the UI and change the password, or the `Login ...` tests in 00 Setup fail with a
   clear message.
3. Fill in the passwords in a copy of the environment file. Keep the copy out of git, e.g.
   `local.postman_environment.local.json` (`*.local` and `.env` are already ignored):

   ```bash
   cp src/test/postman/local.postman_environment.json src/test/postman/pasa-auto.local
   ```

## Running

With Newman (no install needed beyond Node):

```bash
npx newman@6 run src/test/postman/pasa-auto-regression.postman_collection.json -e src/test/postman/pasa-auto.local --reporters cli,junit --reporter-junit-export target/newman/regression.xml
```

You can also pass credentials inline instead of using a file:

```bash
npx newman@6 run src/test/postman/pasa-auto-regression.postman_collection.json -e src/test/postman/local.postman_environment.json --env-var ownerPassword=... --env-var adminPassword=... --env-var karyawanPassword=...
```

In the Postman app: import the collection and the environment, select the environment, then use
**Run collection**. Under *Cookies > Manage cookies*, allow-list the `baseUrl` domain (`localhost`) so
scripts can clear the refresh cookie between steps.

## Things to know before running it repeatedly

- **Login throttle.** One run makes about 8 deliberate failed logins from your IP. The per-IP limit is
  20 failures per 15 minutes (`LOGIN_MAX_FAILURES_PER_IP`), so a third run inside 15 minutes gets
  locked out. Every request also has a `[global] not locked out` assertion that names this cause.
  On a dedicated test instance, start the app with `LOGIN_MAX_FAILURES_PER_IP=1000`. The counters are
  in memory, so restarting the app also clears them.
- **Rate limits.** The SPK endpoints are rate-limited (writes 10/min, `delete-by-no-spk` 5/min).
  A single run stays well under these limits.
- **Data left behind.** Fixtures are created with a unique run id (`QA<runId>`, plates `QA<nnnnn>A..D`)
  and deleted in the same run where the API allows it. Customer accounts can't be deleted through the
  API, so every run leaves two `qa.<runid>.c1/c2@example.test` accounts and their portal customer
  records. Soft-deleted rows and audit-trail entries also remain. Point the suite at a disposable
  database if that matters to you.
- **Environment-dependent branches** adapt automatically: Google sign-in on or off, RBAC module on or
  off, clock-in IP allowlist, and vehicle-claim mode `APPROVAL` or `IMMEDIATE`. A skipped request is
  logged as `SKIPPED "<name>": <reason>`.
- The SPK-number test assumes the runner and the server share a timezone.

## Changing the suite

The collection is **generated**. Edit `build-collection.mjs`, then regenerate, and commit both files:

```bash
node src/test/postman/build-collection.mjs
```

- `r(name, method, path, { as, body, query, pre, test, noRedirect })` builds one request. `as` picks
  the token (`owner`, `admin`, `karyawan`, `customer`, `customer2`, `tempUser`, `invalid`). A body
  string `"#{{var}}"` is emitted unquoted, so numeric ids stay numbers.
- `crudFolder({...})` expands one resource spec into the full CRUD, pagination and edge-case
  sequence. Adding a new master-data resource usually takes one spec.
- Test scripts use the helpers from `makeU(pm)`: `U.ok()`, `U.fail(code, msg)`, `U.softFail(msg)`
  (200 with `success=false`), `U.notFound()`, `U.badRequest(msg)`, `U.forbidden()`, `U.page()`,
  `U.fields()`, `U.absent()`, `U.contains()`, `U.get/set` (collection variables), `U.skipUnless()`.

After adding or changing endpoints, check that every operation in the OpenAPI spec is still called
somewhere. The script exits 1 and lists anything uncovered:

```bash
node src/test/postman/check-coverage.mjs http://localhost:8080/openapi?format=json
```

Some tests are expected to fail until their bugs are fixed. See
[docs/api-regression-findings.md](../../../docs/api-regression-findings.md).
