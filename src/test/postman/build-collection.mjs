#!/usr/bin/env node
/*
 * Builds the Postman regression collection for the pasa-auto REST API.
 *
 *   node src/test/postman/build-collection.mjs            # writes next to this script
 *   node src/test/postman/build-collection.mjs <out-dir>  # writes into <out-dir>
 *
 * The collection is generated rather than hand-edited because most resources share the same CRUD /
 * pagination contract: one spec per resource below expands into ~15 requests with assertions. Edit this
 * file, re-run it, and commit both this file and the regenerated JSON.
 *
 * Conventions the assertions rely on (see GlobalExceptionHandler and the resources):
 *   - every JSON response is an ApiResponse envelope {success, message, data, error}
 *   - several resources answer "not found" with HTTP 200 + success=false instead of 404; U.notFound()
 *     accepts both so the suite pins the contract (success=false) without pinning that inconsistency
 *   - a non-numeric id is a NumberFormatException, mapped to 400 "Invalid value format"
 *   - an unsafe sortBy is refused with 400 "Unsupported sort field" (SortFieldValidator)
 */
import { writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
// Optional first argument: output directory (default: next to this script)
const OUT = join(process.argv[2] ? resolve(process.argv[2]) : here, 'pasa-auto-regression.postman_collection.json');

// A large id that no fixture will reach; still inside int range because supplier ids are Integer.
const MISSING_ID = 2147480000;

// ---------------------------------------------------------------------------------------------------
// Shared script helpers. Every script gets its own `pm`, so the helpers are a factory: the collection
// scripts define makeU globally (assigned without a declaration) and each request script starts with
// `var U = makeU(pm);`. A helper object built once would keep the pm of the script that built it.
// ---------------------------------------------------------------------------------------------------
const UTILS = String.raw`
makeU = function (pm) {
var U = {
  json: function () { try { return pm.response.json(); } catch (e) { return undefined; } },
  data: function () { var b = U.json(); return b ? b.data : undefined; },
  get: function (k) { var v = pm.collectionVariables.get(k); return (v === undefined || v === null || v === '') ? pm.environment.get(k) : v; },
  set: function (k, v) { pm.collectionVariables.set(k, v); },
  num: function (k) { return Number(U.get(k)); },
  status: function (code) { pm.test('status ' + code, function () { pm.response.to.have.status(code); }); },
  ok: function (code) {
    U.status(code || 200);
    pm.test('success=true', function () {
      var b = U.json();
      pm.expect(b, 'JSON envelope').to.be.an('object');
      pm.expect(b.success, 'success flag (error: ' + (b && b.error) + ')').to.eql(true);
    });
    return U.data();
  },
  fail: function (code, msg) {
    U.status(code);
    pm.test('success=false' + (msg ? ', error mentions "' + msg + '"' : ''), function () {
      var b = U.json();
      pm.expect(b, 'JSON envelope').to.be.an('object');
      pm.expect(b.success).to.eql(false);
      pm.expect(b.error, 'error message').to.be.a('string').and.not.empty;
      if (msg) { pm.expect(b.error).to.include(msg); }
    });
  },
  softFail: function (msg) { U.fail(200, msg); },
  notFound: function (msg) {
    pm.test('not found (404, or 200 with success=false)', function () {
      pm.expect([200, 404]).to.include(pm.response.code);
      var b = U.json();
      if (b === undefined) { pm.expect(pm.response.code).to.eql(404); return; }
      pm.expect(b.success).to.eql(false);
      if (msg) { pm.expect(String(b.error)).to.include(msg); }
    });
  },
  badRequest: function (msg) {
    U.status(400);
    if (msg) { pm.test('response mentions "' + msg + '"', function () { pm.expect(pm.response.text()).to.include(msg); }); }
  },
  unauthorized: function () { U.status(401); },
  forbidden: function () { U.status(403); },
  isArray: function (v, label) { pm.test((label || 'data') + ' is an array', function () { pm.expect(v).to.be.an('array'); }); return Array.isArray(v) ? v : []; },
  page: function (d, rowsPerPage, page) {
    pm.test('page envelope (rows, page, rowsPerPage, rowsNumber)', function () {
      pm.expect(d).to.be.an('object');
      pm.expect(d.rows).to.be.an('array');
      pm.expect(d.page).to.eql(page || 1);
      pm.expect(d.rowsPerPage).to.eql(rowsPerPage);
      pm.expect(d.rowsNumber).to.be.a('number').and.at.least(d.rows.length);
      pm.expect(d.rows.length).to.be.at.most(rowsPerPage);
    });
    return (d && Array.isArray(d.rows)) ? d.rows : [];
  },
  fields: function (obj, expected, label) {
    pm.test((label || 'fields') + ' match: ' + Object.keys(expected).join(', '), function () {
      pm.expect(obj, 'object').to.be.an('object');
      Object.keys(expected).forEach(function (k) { pm.expect(obj[k], k).to.eql(expected[k]); });
    });
  },
  expand: function (o) {
    var out = {};
    Object.keys(o).forEach(function (k) { out[k] = typeof o[k] === 'string' ? pm.variables.replaceIn(o[k]) : o[k]; });
    return out;
  },
  absent: function (obj, keys, label) {
    pm.test((label || 'response') + ' does not expose: ' + keys.join(', '), function () {
      keys.forEach(function (k) { pm.expect(obj == null || obj[k] === undefined || obj[k] === null, k + ' is exposed').to.eql(true); });
    });
  },
  contains: function (list, pred, label) {
    pm.test(label, function () { pm.expect((list || []).some(pred), label).to.eql(true); });
  },
  cookie: function (name) {
    return pm.response.headers.all()
      .filter(function (h) { return h.key.toLowerCase() === 'set-cookie'; })
      .map(function (h) { return h.value; })
      .filter(function (v) { return v.indexOf(name + '=') === 0; })[0];
  },
  cookieValue: function (setCookie) { return setCookie ? setCookie.split(';')[0].split('=').slice(1).join('=') : undefined; },
  claims: function (jwt) {
    try {
      var p = String(jwt).split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      while (p.length % 4) { p += '='; }
      return JSON.parse(atob(p));
    } catch (e) { return {}; }
  },
  skipUnless: function (cond, why) {
    if (!cond) {
      console.log('SKIPPED "' + pm.info.requestName + '": ' + why);
      if (pm.execution && pm.execution.skipRequest) { pm.execution.skipRequest(); }
    }
  },
  forgetCookie: function (path, name) {
    try { pm.cookies.jar().unset(pm.variables.replaceIn('{{baseUrl}}') + path, name, function () {}); }
    catch (e) { console.log('cookie jar not available (' + e + '); in the Postman app allow-list the baseUrl domain under Cookies > Manage'); }
  }
};
return U;
};
`;
const PRELUDE = 'var U = makeU(pm);';

const GLOBAL_TESTS = String.raw`
var U = makeU(pm);
pm.test('[global] no 5xx server error', function () {
  pm.expect(pm.response.code, pm.response.text().slice(0, 300)).to.be.below(500);
});
pm.test('[global] not locked out by the login throttle (429: wait 15 min or raise LOGIN_MAX_FAILURES_PER_IP)', function () {
  pm.expect(pm.response.code).to.not.eql(429);
});
var maxMs = Number(U.get('maxResponseMs') || 5000);
pm.test('[global] responds within ' + maxMs + ' ms', function () { pm.expect(pm.response.responseTime).to.be.below(maxMs); });
var ct = pm.response.headers.get('Content-Type') || '';
if (ct.indexOf('json') >= 0 && pm.response.text().length > 0) {
  pm.test('[global] JSON envelope is well formed', function () {
    var b = U.json();
    pm.expect(b, 'parsable JSON').to.not.eql(undefined);
    if (b && typeof b === 'object' && !Array.isArray(b) && Object.prototype.hasOwnProperty.call(b, 'success')) {
      pm.expect(b.success).to.be.a('boolean');
      if (b.success === false) { pm.expect(b.error, 'error message on failure').to.be.a('string'); }
    }
  });
}
`;

// ---------------------------------------------------------------------------------------------------
// Builders
// ---------------------------------------------------------------------------------------------------
const TOKENS = {
  owner: '{{ownerToken}}',
  admin: '{{adminToken}}',
  karyawan: '{{karyawanToken}}',
  customer: '{{customerToken}}',
  customer2: '{{customer2Token}}',
  customer3: '{{customer3Token}}',
  tempUser: '{{tempUserToken}}',
  invalid: 'not.a.valid-token',
};

const lines = (s) => (Array.isArray(s) ? s : String(s).trim().split('\n'));

/** Request item. A "#{{var}}" string in a body becomes an unquoted {{var}} so numeric ids stay numbers. */
function r(name, method, path, opts = {}) {
  const { as = 'none', body, query, pre, test, noRedirect, desc, headers = [] } = opts;
  const header = [...headers];
  if (TOKENS[as]) header.push({ key: 'Authorization', value: `Bearer ${TOKENS[as]}` });
  let raw = `{{baseUrl}}${path}`;
  if (query) {
    raw += '?' + Object.entries(query).map(([k, v]) => `${k}=${v}`).join('&');
  }
  const request = { method, header, url: raw };
  if (desc) request.description = desc;
  if (body !== undefined) {
    const text = typeof body === 'string' ? body : JSON.stringify(body, null, 2).replace(/"#(\{\{[A-Za-z0-9_]+\}\})"/g, '$1');
    request.body = { mode: 'raw', raw: text, options: { raw: { language: 'json' } } };
    header.push({ key: 'Content-Type', value: 'application/json' });
  }
  const item = { name, request, event: [] };
  if (pre) item.event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: [PRELUDE, ...lines(pre)] } });
  if (test) item.event.push({ listen: 'test', script: { type: 'text/javascript', exec: [PRELUDE, ...lines(test)] } });
  if (noRedirect) item.protocolProfileBehavior = { followRedirects: false };
  return item;
}

const f = (name, item, description) => ({ name, description, item });

// ---------------------------------------------------------------------------------------------------
// 00 Setup
// ---------------------------------------------------------------------------------------------------
const setup = f('00 Setup', [
  r('Public auth config + initialise run', 'GET', '/api/auth/config', {
    pre: String.raw`
// Fresh, unique values for this run so fixtures never collide with real data or earlier runs.
var runId = Date.now().toString(36).slice(-6).toUpperCase();
var runNum = String(Date.now() % 100000).padStart(5, '0');
U.set('runId', runId);
U.set('runNum', runNum);
['A', 'B', 'C', 'D'].forEach(function (s) { U.set('nopol' + s, 'QA' + runNum + s); });
U.set('nopolUnknown', 'QZ' + runNum + 'X');
U.set('customerEmail', 'qa.' + runId.toLowerCase() + '.c1@example.test');
U.set('customer2Email', 'qa.' + runId.toLowerCase() + '.c2@example.test');
U.set('customerPhone', '0899' + runNum + '01');
U.set('customer3Phone', '0898' + runNum + '03');
U.set('customer2Phone', '0899' + runNum + '02');
U.set('customerPassword', U.get('customerPassword') || 'Regress-Pass-2026');
U.set('customerPasswordNew', U.get('customerPassword') + '-v2');
U.set('tempUsername', 'qa_' + runId.toLowerCase());
U.set('tempUserPassword', 'Temp-' + runId + '-pw1');
U.set('tempUserPasswordNew', 'Temp-' + runId + '-pw2');
`,
    test: String.raw`
var d = U.ok();
pm.test('feature flags are booleans', function () {
  pm.expect(d.googleLoginEnabled).to.be.a('boolean');
  pm.expect(d.customerPortalEnabled).to.be.a('boolean');
});
U.set('googleEnabled', String(d.googleLoginEnabled));
`,
  }),
  r('Health check (anonymous)', 'GET', '/health', {
    test: String.raw`
var d = U.ok();
pm.test('status is UP with a timestamp', function () { pm.expect(d.status).to.eql('UP'); pm.expect(d.timestamp).to.exist; });
U.absent(d, ['application', 'version'], 'anonymous health probe');
`,
  }),
  r('Refresh without any refresh token -> 401', 'POST', '/api/auth/refresh', {
    pre: "U.forgetCookie('/api/auth/refresh', 'refresh_token');",
    test: "U.fail(401, 'Invalid or expired refresh token');",
  }),
  r('Refresh with a garbage refresh token -> 401', 'POST', '/api/auth/refresh', {
    body: { refreshToken: 'garbage.token.value' },
    pre: "U.forgetCookie('/api/auth/refresh', 'refresh_token');",
    test: 'U.fail(401);',
  }),
  ...[
    ['owner', 'Owner'],
    ['admin', 'Admin'],
    ['karyawan', 'Karyawan'],
  ].map(([who, role]) =>
    r(`Login ${who}`, 'POST', '/api/auth/login', {
      body: { username: `{{${who}Username}}`, password: `{{${who}Password}}` },
      test: String.raw`
var d = U.ok();
pm.test('issues an access token', function () { pm.expect(d.token).to.be.a('string').and.not.empty; pm.expect(d.expiresIn).to.be.a('number').and.above(0); });
pm.test('account has a permanent password (change the seeded password once before running the suite)', function () { pm.expect(d.mustChangePassword).to.eql(false); });
U.absent(d, ['refreshToken'], 'login body (refresh token lives only in the HttpOnly cookie)');
var c = U.cookie('refresh_token');
pm.test('refresh cookie is HttpOnly, SameSite=Strict, scoped to /api/auth', function () {
  pm.expect(c, 'Set-Cookie refresh_token').to.be.a('string');
  pm.expect(c).to.match(/HttpOnly/i);
  pm.expect(c).to.match(/SameSite=Strict/i);
  pm.expect(c).to.match(/Path=\/api\/auth/i);
});
var claims = U.claims(d.token);
pm.test('token groups include ${role}', function () { pm.expect(claims.groups || []).to.include('${role}'); });
${who === 'karyawan' ? "pm.test('karyawan token carries no Admin/Owner group', function () { pm.expect(claims.groups || []).to.not.include('Admin'); pm.expect(claims.groups || []).to.not.include('Owner'); });\nU.set('karyawanClaimId', claims.karyawanId == null ? '' : String(claims.karyawanId));" : ''}
U.set('${who}Token', d.token);
`,
    }),
  ),
  r('Read customer-access switch (remember original)', 'GET', '/api/settings/customer-access', {
    as: 'owner',
    test: String.raw`
var d = U.ok();
pm.test('enabled is a boolean', function () { pm.expect(d.enabled).to.be.a('boolean'); });
U.set('customerAccessOriginal', String(d.enabled));
`,
  }),
  r('Enable customer portal for the run', 'PUT', '/api/settings/customer-access', {
    as: 'owner',
    body: { enabled: true },
    test: "var d = U.ok(); U.fields(d, { enabled: true });",
  }),
  r('Public customer-access status reflects the switch', 'GET', '/api/public/customer-access-status', {
    test: "var d = U.ok(); U.fields(d, { customerAccessEnabled: true });",
  }),
], 'Run first. Logs in the three staff roles, generates unique run data and switches the customer portal on (restored in 99 Teardown).');

// ---------------------------------------------------------------------------------------------------
// 01 Auth (staff)
// ---------------------------------------------------------------------------------------------------
const authStaff = f('01 Auth - staff', [
  r('Login: blank password -> 400', 'POST', '/api/auth/login', {
    body: { username: '{{ownerUsername}}', password: '' },
    test: "U.badRequest('Password harus diisi');",
  }),
  r('Login: empty body -> 400', 'POST', '/api/auth/login', {
    body: {},
    test: "U.badRequest('Username harus diisi');",
  }),
  r('Login: wrong password -> 401', 'POST', '/api/auth/login', {
    body: { username: '{{ownerUsername}}', password: 'definitely-wrong-{{runId}}' },
    test: "U.fail(401); U.set('wrongPasswordError', U.json().error);",
  }),
  r('Login: unknown user -> 401 with the same message (no user enumeration)', 'POST', '/api/auth/login', {
    body: { username: 'qa-nobody-{{runId}}', password: 'whatever-123' },
    test: String.raw`
U.fail(401);
pm.test('same error as a wrong password', function () { pm.expect(U.json().error).to.eql(U.get('wrongPasswordError')); });
`,
  }),
  r('Login owner again (clears the failure counter)', 'POST', '/api/auth/login', {
    body: { username: '{{ownerUsername}}', password: '{{ownerPassword}}' },
    test: String.raw`
var d = U.ok();
U.set('ownerToken', d.token);
U.set('ownerRefreshCookie', U.cookieValue(U.cookie('refresh_token')));
`,
  }),
  r('Refresh (cookie) rotates the refresh token', 'POST', '/api/auth/refresh', {
    test: String.raw`
var d = U.ok();
pm.test('new access token', function () { pm.expect(d.token).to.be.a('string').and.not.empty; });
U.absent(d, ['refreshToken'], 'refresh body');
var rotated = U.cookieValue(U.cookie('refresh_token'));
pm.test('a new refresh cookie is issued', function () { pm.expect(rotated).to.be.a('string').and.not.empty; pm.expect(rotated).to.not.eql(U.get('ownerRefreshCookie')); });
U.set('ownerToken', d.token);
`,
  }),
  r('GET /me without token -> 401', 'GET', '/api/auth/me', { test: 'U.unauthorized();' }),
  r('GET /me with an invalid token -> 401', 'GET', '/api/auth/me', { as: 'invalid', test: 'U.unauthorized();' }),
  r('GET /me (owner)', 'GET', '/api/auth/me', {
    as: 'owner',
    test: String.raw`
var d = U.ok();
pm.test('identifies the caller', function () { pm.expect(d.username).to.eql(U.get('ownerUsername')); });
pm.test('roles include Owner', function () { pm.expect(d.roles).to.include('Owner'); });
U.set('ownerEmail', d.email || '');
`,
  }),
  r('PUT /me: invalid email -> 400', 'PUT', '/api/auth/me', {
    as: 'owner', body: { email: 'not-an-email' }, test: "U.badRequest('Email tidak valid');",
  }),
  r('PUT /me: blank email -> 400', 'PUT', '/api/auth/me', {
    as: 'owner', body: { email: '' }, test: "U.badRequest('Email harus diisi');",
  }),
  r('PUT /me: same email (idempotent update)', 'PUT', '/api/auth/me', {
    as: 'owner',
    body: { email: '{{ownerEmail}}' },
    pre: "U.skipUnless(U.get('ownerEmail'), 'owner has no email to re-save');",
    test: "var d = U.ok(); U.fields(d, { email: U.get('ownerEmail'), username: U.get('ownerUsername') });",
  }),
  r('Change password without token -> 401', 'POST', '/api/auth/change-password', {
    body: { currentPassword: 'x', newPassword: 'y' }, test: 'U.unauthorized();',
  }),
  r('Change password: missing fields -> 400', 'POST', '/api/auth/change-password', {
    as: 'owner', body: {}, test: "U.badRequest('harus diisi');",
  }),
  r('Change password: wrong current password -> 401', 'POST', '/api/auth/change-password', {
    as: 'owner',
    body: { currentPassword: 'wrong-{{runId}}', newPassword: 'Another-Strong-Pass-1' },
    test: "U.fail(401, 'Current password is incorrect');",
  }),
  r('Change password: new password too short -> 400', 'POST', '/api/auth/change-password', {
    as: 'owner',
    body: { currentPassword: '{{ownerPassword}}', newPassword: 'short' },
    test: "U.fail(400, 'at least 8 characters');",
  }),
  r('Google sign-in start', 'GET', '/api/auth/google/start', {
    noRedirect: true,
    test: String.raw`
if (U.get('googleEnabled') !== 'true') {
  U.status(404);
} else {
  U.status(303);
  pm.test('redirects to Google with PKCE', function () {
    var loc = pm.response.headers.get('Location');
    pm.expect(loc).to.include('https://accounts.google.com/');
    pm.expect(loc).to.include('code_challenge_method=S256');
  });
  pm.test('state cookie is HttpOnly + SameSite=Lax', function () {
    var c = U.cookie('google_oauth_state');
    pm.expect(c).to.match(/HttpOnly/i);
    pm.expect(c).to.match(/SameSite=Lax/i);
  });
}
`,
  }),
  r('Google callback without code/state', 'GET', '/api/auth/google/callback', {
    noRedirect: true,
    test: String.raw`
if (U.get('googleEnabled') !== 'true') { U.status(404); }
else { U.status(303); pm.test('back to login with google=error', function () { pm.expect(pm.response.headers.get('Location')).to.include('google=error'); }); }
`,
  }),
  r('Google callback with error=access_denied', 'GET', '/api/auth/google/callback', {
    noRedirect: true,
    query: { error: 'access_denied' },
    test: String.raw`
if (U.get('googleEnabled') !== 'true') { U.status(404); }
else { U.status(303); pm.test('back to login with google=denied', function () { pm.expect(pm.response.headers.get('Location')).to.include('google=denied'); }); }
`,
  }),
  r('Google callback with a forged state', 'GET', '/api/auth/google/callback', {
    noRedirect: true,
    query: { code: 'fake-code', state: 'forged-state' },
    pre: "U.forgetCookie('/api/auth/google', 'google_oauth_state');",
    test: String.raw`
if (U.get('googleEnabled') !== 'true') { U.status(404); }
else { U.status(303); pm.test('state mismatch is refused', function () { pm.expect(pm.response.headers.get('Location')).to.include('google=error'); }); }
`,
  }),
], 'Password login, token refresh rotation, profile and password policy, Google OAuth surface.');

// ---------------------------------------------------------------------------------------------------
// 02 Access control matrix
// ---------------------------------------------------------------------------------------------------
const deny = (as, method, path, code, body) =>
  r(`${as === 'none' ? 'anonymous' : as} ${method} ${path} -> ${code}`, method, path, {
    as, body, test: code === 401 ? 'U.unauthorized();' : 'U.forbidden();',
  });

const accessControl = f('02 Access control', [
  ...[
    ['GET', '/api/pazaauto/barang'],
    ['GET', '/api/pazaauto/pelanggan/paginated'],
    ['GET', '/api/users'],
    ['GET', '/api/pazaauto/summary'],
    ['GET', '/api/audit-trail'],
    ['GET', '/api/settings/customer-access'],
    ['GET', '/api/pazaauto/absensi/history'],
    ['POST', '/api/admin/caffeine-cache/clear'],
    ['GET', '/api/rbac/roles'],
  ].map(([m, p]) => deny('none', m, p, 401)),
  deny('invalid', 'GET', '/api/pazaauto/barang', 401),
  ...[
    ['GET', '/api/pazaauto/barang'],
    ['GET', '/api/pazaauto/pelanggan'],
    ['GET', '/api/pazaauto/spk'],
    ['GET', '/api/pazaauto/penjualan'],
    ['GET', '/api/pazaauto/pembelian'],
    ['GET', '/api/users'],
    ['GET', '/api/roles'],
    ['GET', '/api/roles/lookup'],
    ['GET', '/api/permissions'],
    ['GET', '/api/system-parameters'],
    ['GET', '/api/rbac/roles'],
    ['GET', '/api/pazaauto/summary'],
    ['GET', '/api/audit-trail'],
    ['GET', '/api/settings/customer-access'],
    ['POST', '/api/admin/caffeine-cache/clear'],
    ['GET', '/api/pazaauto/absensi'],
    ['GET', '/api/pazaauto/absensi/paginated'],
    ['POST', '/api/pazaauto/absensi/mark-absence', {}],
    ['GET', '/api/pazaauto/customer-bookings'],
    ['GET', '/api/pazaauto/customer-link-requests'],
    ['GET', '/api/pazaauto/vehicle-claim-requests'],
    ['GET', '/api/pazaauto/karyawan/unregistered'],
  ].map(([m, p, b]) => deny('karyawan', m, p, 403, b)),
  ...[
    ['GET', '/api/users'],
    ['GET', '/api/roles'],
    ['GET', '/api/permissions'],
    ['GET', '/api/system-parameters'],
    ['GET', '/api/rbac/roles'],
    ['GET', '/api/pazaauto/summary'],
    ['PUT', '/api/settings/customer-access', { enabled: true }],
  ].map(([m, p, b]) => deny('admin', m, p, 403, b)),
  r('admin GET /api/roles/lookup -> 200 (read-only role list)', 'GET', '/api/roles/lookup', {
    as: 'admin', test: "U.isArray(U.ok());",
  }),
  r('admin GET /api/settings/customer-access -> 200', 'GET', '/api/settings/customer-access', {
    as: 'admin', test: 'U.ok();',
  }),
  r('karyawan GET /api/pazaauto/karyawan -> id + name only (no colleague PII)', 'GET', '/api/pazaauto/karyawan', {
    as: 'karyawan',
    test: String.raw`
var rows = U.isArray(U.ok());
pm.test('each row only has id and namaKaryawan', function () {
  rows.forEach(function (row) {
    Object.keys(row).forEach(function (k) { pm.expect(['id', 'namaKaryawan'], 'unexpected field ' + k).to.include(k); });
  });
});
`,
  }),
  r("karyawan reads another employee's attendance -> 403", 'GET', `/api/pazaauto/absensi/today/${MISSING_ID}`, {
    as: 'karyawan', test: "U.forbidden();",
  }),
  r('karyawan reads own attendance history', 'GET', '/api/pazaauto/absensi/history', {
    as: 'karyawan',
    test: String.raw`
// Allowed only when the karyawan login is linked to an employee record (karyawanId claim)
if (U.get('karyawanClaimId')) { U.page(U.ok(), 10); } else { U.forbidden(); }
`,
  }),
  r('staff token on the customer portal -> 403', 'GET', '/api/customer/auth/me', {
    as: 'owner', test: 'U.forbidden();',
  }),
], 'Role matrix: anonymous -> 401, wrong role -> 403, and the deliberate read-only exceptions.');

// ---------------------------------------------------------------------------------------------------
// 03 Master data CRUD (generated)
// ---------------------------------------------------------------------------------------------------
/**
 * spec: key (variable prefix), base path, label (entity name in messages), create/update bodies,
 * check (fields to compare after create), checkUpdate, searchTerm (matches the created row),
 * searchEndpoint (GET {base}/search?q=), paginatedQuery (extra query for the paginated call), extra.
 */
function crudFolder(spec) {
  const { key, base, label, create, update, check, checkUpdate, searchTerm, searchEndpoint, extraQuery = {}, extra = [], invalidCreate } = spec;
  const idVar = `${key}Id`;
  const items = [
    r(`List all ${label}`, 'GET', base, { as: 'admin', test: 'U.isArray(U.ok());' }),
  ];
  if (invalidCreate) {
    invalidCreate.forEach(([what, body, msg]) =>
      items.push(r(`Create ${label}: ${what} -> 400`, 'POST', base, { as: 'admin', body, test: `U.badRequest(${JSON.stringify(msg)});` })),
    );
  }
  items.push(
    r(`Create ${label}`, 'POST', base, {
      as: 'admin',
      body: create,
      test: String.raw`
var d = U.ok();
pm.test('message says created', function () { pm.expect(U.json().message).to.match(/created/i); });
pm.test('assigned an id', function () { pm.expect(d.id).to.be.a('number'); });
U.fields(d, U.expand(${JSON.stringify(check)}), 'echoed');
U.set('${idVar}', String(d.id));
`,
    }),
    r(`Get ${label} by id`, 'GET', `${base}/{{${idVar}}}`, {
      as: 'admin',
      test: `var d = U.ok(); U.fields(d, Object.assign({ id: U.num('${idVar}') }, U.expand(${JSON.stringify(check)})), 'stored');`,
    }),
    r(`Update ${label}`, 'PUT', `${base}/{{${idVar}}}`, {
      as: 'admin',
      body: update,
      test: String.raw`
var d = U.ok();
pm.test('message says updated', function () { pm.expect(U.json().message).to.match(/updated/i); });
U.fields(d, U.expand(${JSON.stringify(checkUpdate)}), 'updated');
`,
    }),
    r(`Get ${label} after update (persisted)`, 'GET', `${base}/{{${idVar}}}`, {
      as: 'admin',
      test: `var d = U.ok(); U.fields(d, U.expand(${JSON.stringify(checkUpdate)}), 'persisted');`,
    }),
    r(`Paginated ${label} (page 1, 5 rows)`, 'GET', `${base}/paginated`, {
      as: 'admin', query: { page: 1, rowsPerPage: 5, ...extraQuery }, test: 'U.page(U.ok(), 5);',
    }),
    r(`Paginated ${label} page 2`, 'GET', `${base}/paginated`, {
      as: 'admin', query: { page: 2, rowsPerPage: 1, ...extraQuery }, test: 'U.page(U.ok(), 1, 2);',
    }),
    r(`Paginated ${label} sorted by id desc`, 'GET', `${base}/paginated`, {
      as: 'admin',
      query: { page: 1, rowsPerPage: 10, sortBy: 'id', descending: true, ...extraQuery },
      test: String.raw`
var rows = U.page(U.ok(), 10);
pm.test('ids are in descending order', function () {
  for (var i = 1; i < rows.length; i++) { pm.expect(rows[i - 1].id).to.be.at.least(rows[i].id); }
});
`,
    }),
    r(`Paginated ${label}: SQL-injection sortBy -> 400`, 'GET', `${base}/paginated`, {
      as: 'admin', query: { sortBy: 'id%3Bdrop%20table%20users' }, test: "U.fail(400, 'Unsupported sort field');",
    }),
  );
  if (searchTerm) {
    items.push(
      r(`Paginated ${label}: search finds the new record`, 'GET', `${base}/paginated`, {
        as: 'admin',
        query: { page: 1, rowsPerPage: 50, search: searchTerm },
        test: `var rows = U.page(U.ok(), 50); U.contains(rows, function (x) { return x.id === U.num('${idVar}'); }, 'created ${label} is in the results');`,
      }),
      r(`Paginated ${label}: search with no match`, 'GET', `${base}/paginated`, {
        as: 'admin',
        query: { search: 'zz-no-such-{{runId}}' },
        test: "var rows = U.page(U.ok(), 10); pm.test('no rows', function () { pm.expect(rows.length).to.eql(0); });",
      }),
    );
  }
  if (searchEndpoint) {
    items.push(r(`Search ${label} (q)`, 'GET', `${base}/search`, {
      as: 'admin',
      query: { q: searchTerm },
      test: `var rows = U.isArray(U.ok()); U.contains(rows, function (x) { return x.id === U.num('${idVar}'); }, 'created ${label} is found');`,
    }));
  }
  items.push(...extra);
  items.push(
    r(`Get ${label}: non-numeric id -> 400`, 'GET', `${base}/abc`, { as: 'admin', test: "U.fail(400, 'Invalid value format');" }),
    r(`Get ${label}: unknown id -> not found`, 'GET', `${base}/${MISSING_ID}`, { as: 'admin', test: 'U.notFound();' }),
    r(`Update ${label}: unknown id -> not found`, 'PUT', `${base}/${MISSING_ID}`, { as: 'admin', body: update, test: 'U.notFound();' }),
    r(`Delete ${label}`, 'DELETE', `${base}/{{${idVar}}}`, {
      as: 'admin',
      test: "U.ok(); pm.test('message says deleted', function () { pm.expect(U.json().message).to.match(/deleted/i); });",
    }),
    r(`Get ${label} after delete -> not found`, 'GET', `${base}/{{${idVar}}}`, { as: 'admin', test: 'U.notFound();' }),
  );
  return f(`${label}`, items, `CRUD + pagination contract for ${base}`);
}

const masterData = f('03 Master data CRUD', [
  crudFolder({
    key: 'barang', base: '/api/pazaauto/barang', label: 'Barang',
    create: { namaBarang: 'QA Barang {{runId}}', kodeBarang: 'QB{{runNum}}', hargaJual: 15000, hargaBeli: 10000, satuan: 'PCS', stok: 5, stokMinimal: 1, active: true },
    check: { namaBarang: 'QA Barang {{runId}}', kodeBarang: 'QB{{runNum}}', satuan: 'PCS', stok: 5 },
    update: { namaBarang: 'QA Barang {{runId}} v2', kodeBarang: 'QB{{runNum}}', hargaJual: 17500, hargaBeli: 10000, satuan: 'PCS', stok: 7, stokMinimal: 1, active: true },
    checkUpdate: { namaBarang: 'QA Barang {{runId}} v2', stok: 7 },
    searchTerm: 'QB{{runNum}}', searchEndpoint: true,
  }),
  crudFolder({
    key: 'jasa', base: '/api/pazaauto/jasa', label: 'Jasa',
    create: { namaJasa: 'QA Jasa {{runId}}', hargaJasa: 50000, deskripsi: 'regression', estimasiWaktu: 30 },
    check: { namaJasa: 'QA Jasa {{runId}}', hargaJasa: 50000, estimasiWaktu: 30 },
    update: { namaJasa: 'QA Jasa {{runId}} v2', hargaJasa: 65000, deskripsi: 'regression v2', estimasiWaktu: 45 },
    checkUpdate: { namaJasa: 'QA Jasa {{runId}} v2', hargaJasa: 65000 },
    searchTerm: 'qa jasa {{runId}}',
  }),
  crudFolder({
    key: 'posisi', base: '/api/pazaauto/karyawan-posisi', label: 'Karyawan Posisi',
    create: { posisi: 'QA{{runId}}', keterangan: 'regression' },
    check: { posisi: 'QA{{runId}}' },
    update: { posisi: 'QA{{runId}}v2', keterangan: 'regression v2' },
    checkUpdate: { posisi: 'QA{{runId}}v2', keterangan: 'regression v2' },
    searchTerm: 'qa{{runId}}',
  }),
  crudFolder({
    key: 'supplier', base: '/api/pazaauto/supplier', label: 'Supplier',
    create: { namaSupplier: 'QA Supplier {{runId}}', alamat: 'Jl. Regresi 1', telepon: '0211234567', email: 'supplier.{{runNum}}@example.test', kota: 'Jakarta' },
    check: { namaSupplier: 'QA Supplier {{runId}}', kota: 'Jakarta' },
    update: { namaSupplier: 'QA Supplier {{runId}} v2', alamat: 'Jl. Regresi 2', telepon: '0211234567', email: 'supplier.{{runNum}}@example.test', kota: 'Bandung' },
    checkUpdate: { namaSupplier: 'QA Supplier {{runId}} v2', kota: 'Bandung' },
    searchTerm: 'qa supplier {{runId}}', searchEndpoint: true,
  }),
  crudFolder({
    key: 'sparepart', base: '/api/pazaauto/sparepart', label: 'Sparepart',
    create: { kodeSparepart: 'QS{{runNum}}', kodeBarang: 'QS{{runNum}}', namaSparepart: 'QA Sparepart {{runId}}', namaBarang: 'QA Sparepart {{runId}}', hargaJual: 25000, hargaBeli: 20000, satuan: 'PCS', stok: 3, stokMinimal: 1, merek: 'QA', active: true },
    check: { kodeSparepart: 'QS{{runNum}}', namaSparepart: 'QA Sparepart {{runId}}', stok: 3 },
    update: { kodeSparepart: 'QS{{runNum}}', kodeBarang: 'QS{{runNum}}', namaSparepart: 'QA Sparepart {{runId}} v2', namaBarang: 'QA Sparepart {{runId}}', hargaJual: 27000, hargaBeli: 20000, satuan: 'PCS', stok: 9, stokMinimal: 1, merek: 'QA', active: true },
    checkUpdate: { namaSparepart: 'QA Sparepart {{runId}} v2', stok: 9 },
    searchTerm: 'qs{{runNum}}',
  }),
  crudFolder({
    key: 'merk', base: '/api/pazaauto/merk-kendaraan', label: 'Merk Kendaraan',
    create: { nama: 'QAMERK{{runId}}', keterangan: 'regression' },
    check: { nama: 'QAMERK{{runId}}' },
    update: { nama: 'QAMERK{{runId}}V2', keterangan: 'regression v2' },
    checkUpdate: { nama: 'QAMERK{{runId}}V2' },
    searchTerm: 'qamerk{{runId}}',
  }),
  crudFolder({
    key: 'kendaraan', base: '/api/pazaauto/kendaraan', label: 'Kendaraan',
    create: { jenis: 'QAJENIS{{runId}}', merk: 'QAMERK{{runId}}', model: 'QA', keterangan: 'regression' },
    check: { jenis: 'QAJENIS{{runId}}', merk: 'QAMERK{{runId}}' },
    update: { jenis: 'QAJENIS{{runId}}V2', merk: 'QAMERK{{runId}}', model: 'QA2', keterangan: 'regression v2' },
    checkUpdate: { jenis: 'QAJENIS{{runId}}V2', model: 'QA2' },
    searchTerm: 'qajenis{{runId}}',
    extra: [
      r('Distinct merk list', 'GET', '/api/pazaauto/kendaraan/merk/distinct', {
        as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (m) { return m === 'QAMERK' + U.get('runId'); }, 'contains the new merk');",
      }),
      r('Distinct jenis list', 'GET', '/api/pazaauto/kendaraan/jenis/distinct', {
        as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (j) { return j === 'QAJENIS' + U.get('runId') + 'V2'; }, 'contains the updated jenis');",
      }),
      r('Distinct jenis by merk', 'GET', '/api/pazaauto/kendaraan/jenis/by-merk', {
        as: 'admin', query: { merk: 'QAMERK{{runId}}' },
        test: "var d = U.isArray(U.ok()); U.contains(d, function (j) { return j === 'QAJENIS' + U.get('runId') + 'V2'; }, 'jenis of that merk');",
      }),
      r('Distinct jenis by blank merk falls back to all jenis', 'GET', '/api/pazaauto/kendaraan/jenis/by-merk', {
        as: 'admin', test: 'U.isArray(U.ok());',
      }),
    ],
  }),
  crudFolder({
    key: 'karyawanCrud', base: '/api/pazaauto/karyawan', label: 'Karyawan',
    create: { namaKaryawan: 'QA Kary {{runId}}', alamat: 'Jl. Regresi', noTlpn: '0812000000', email: 'kary.{{runNum}}@example.test', jenisKelamin: 'L' },
    check: { namaKaryawan: 'QA Kary {{runId}}', jenisKelamin: 'L' },
    update: { namaKaryawan: 'QA Kary {{runId}} v2', alamat: 'Jl. Regresi 2', noTlpn: '0812000000', email: 'kary.{{runNum}}@example.test', jenisKelamin: 'L' },
    checkUpdate: { namaKaryawan: 'QA Kary {{runId}} v2', alamat: 'Jl. Regresi 2' },
    searchTerm: 'qa kary {{runId}}',
    extra: [
      r('Karyawan without a login account', 'GET', '/api/pazaauto/karyawan/unregistered', {
        as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (k) { return k.id === U.num('karyawanCrudId'); }, 'new karyawan has no login yet');",
      }),
    ],
  }),
  crudFolder({
    key: 'pelangganCrud', base: '/api/pazaauto/pelanggan', label: 'Pelanggan',
    create: { namaPelanggan: 'QA Pelanggan {{runId}}', alamat: 'Jl. Regresi', noHp: '0813{{runNum}}9', email: 'pel.{{runNum}}@example.test', kota: 'Depok' },
    check: { namaPelanggan: 'QA Pelanggan {{runId}}', kota: 'Depok' },
    update: { namaPelanggan: 'QA Pelanggan {{runId}} v2', alamat: 'Jl. Regresi 2', noHp: '0813{{runNum}}9', email: 'pel.{{runNum}}@example.test', kota: 'Bogor' },
    checkUpdate: { namaPelanggan: 'QA Pelanggan {{runId}} v2', kota: 'Bogor' },
    searchTerm: 'qa pelanggan {{runId}}',
    invalidCreate: [
      ['missing namaPelanggan', { alamat: 'x' }, 'Nama Pelanggan is required'],
      ['invalid email', { namaPelanggan: 'QA', email: 'nope' }, 'Invalid email format'],
      ['nopol longer than 10', { namaPelanggan: 'QA', nopol: 'ABCDEFGHIJK' }, 'No Polisi must not exceed 10 characters'],
    ],
  }),
], 'One sub-folder per master-data resource: create, read, update, paginate, search, sort, edge ids, delete.');

// ---------------------------------------------------------------------------------------------------
// 04 Customers & vehicle ownership (staff side)
// ---------------------------------------------------------------------------------------------------
const ownership = f('04 Pelanggan & vehicle ownership', [
  r('Create pelanggan A with vehicle nopolA', 'POST', '/api/pazaauto/pelanggan', {
    as: 'admin',
    body: { namaPelanggan: 'QA Owner A {{runId}}', nopol: '{{nopolA}}', merk: 'QAMERK{{runId}}', jenis: 'QAJENIS{{runId}}', noHp: '0814{{runNum}}1', alamat: 'Jl. A' },
    test: "var d = U.ok(); U.set('pelangganAId', String(d.id)); U.fields(d, { namaPelanggan: 'QA Owner A ' + U.get('runId') });",
  }),
  r('Create pelanggan B (no vehicle)', 'POST', '/api/pazaauto/pelanggan', {
    as: 'admin',
    body: { namaPelanggan: 'QA Owner B {{runId}}', noHp: '0814{{runNum}}2', alamat: 'Jl. B' },
    test: "var d = U.ok(); U.set('pelangganBId', String(d.id));",
  }),
  r('Find pelanggan A by nopol', 'GET', '/api/pazaauto/pelanggan/by-nopol/{{nopolA}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('pelangganAId') });",
  }),
  r('Find pelanggan by unknown nopol -> success=false', 'GET', '/api/pazaauto/pelanggan/by-nopol/{{nopolUnknown}}', {
    as: 'admin', test: "U.softFail('Pelanggan not found for nopol');",
  }),
  r('Update pelanggan A by nopol', 'PUT', '/api/pazaauto/pelanggan/by-nopol/{{nopolA}}', {
    as: 'admin',
    body: { namaPelanggan: 'QA Owner A {{runId}} upd', alamat: 'Jl. A 2', noHp: '0814{{runNum}}1' },
    test: "var d = U.ok(); U.fields(d, { id: U.num('pelangganAId'), namaPelanggan: 'QA Owner A ' + U.get('runId') + ' upd' });",
  }),
  r('Update by nopol: blank name -> 400', 'PUT', '/api/pazaauto/pelanggan/by-nopol/{{nopolA}}', {
    as: 'admin', body: { namaPelanggan: '' }, test: "U.badRequest('Nama Pelanggan is required');",
  }),
  r('Attach second vehicle nopolB to A', 'POST', '/api/pazaauto/pelanggan/{{pelangganAId}}/kendaraan', {
    as: 'admin',
    body: { nopol: '{{nopolB}}', merk: 'QAMERK{{runId}}', jenis: 'QAJENIS{{runId}}', tanggalMulai: '2024-01-01', keterangan: 'regression' },
    test: String.raw`
var d = U.ok();
U.fields(d, { nopol: U.get('nopolB'), idPelanggan: U.num('pelangganAId'), current: true });
U.set('kendaraanBId', String(d.idKendaraan));
`,
  }),
  r('Attach: nopol already owned -> success=false', 'POST', '/api/pazaauto/pelanggan/{{pelangganBId}}/kendaraan', {
    as: 'admin', body: { nopol: '{{nopolB}}' }, test: "U.softFail('already owned by another customer');",
  }),
  r('Attach: missing nopol -> 400', 'POST', '/api/pazaauto/pelanggan/{{pelangganAId}}/kendaraan', {
    as: 'admin', body: { merk: 'X' }, test: "U.badRequest('nopol is required');",
  }),
  r('Attach: nopol longer than 10 -> 400', 'POST', '/api/pazaauto/pelanggan/{{pelangganAId}}/kendaraan', {
    as: 'admin', body: { nopol: 'ABCDEFGHIJK' }, test: "U.badRequest('nopol must not exceed 10 characters');",
  }),
  r('Attach: unknown pelanggan -> success=false', 'POST', `/api/pazaauto/pelanggan/${MISSING_ID}/kendaraan`, {
    as: 'admin', body: { nopol: '{{nopolD}}' }, test: "U.softFail('Pelanggan not found');",
  }),
  r('Vehicles of pelanggan A', 'GET', '/api/pazaauto/pelanggan/{{pelangganAId}}/vehicles', {
    as: 'admin',
    test: String.raw`
var d = U.isArray(U.ok());
['nopolA', 'nopolB'].forEach(function (k) { U.contains(d, function (v) { return v.nopol === U.get(k); }, k + ' is listed'); });
`,
  }),
  r('History of pelanggan A', 'GET', '/api/pazaauto/pelanggan/{{pelangganAId}}/history', {
    as: 'admin',
    test: "var d = U.ok(); U.fields(d, { idPelanggan: U.num('pelangganAId') }); U.isArray(d.vehicles, 'vehicles'); U.isArray(d.transactions, 'transactions');",
  }),
  r('History of unknown pelanggan -> not found', 'GET', `/api/pazaauto/pelanggan/${MISSING_ID}/history`, {
    as: 'admin', test: 'U.notFound();',
  }),
  r('Vehicle options', 'GET', '/api/pazaauto/vehicles/options', {
    as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (v) { return v.nopol === U.get('nopolB'); }, 'nopolB is an option');",
  }),
  r('Vehicle history by nopol', 'GET', '/api/pazaauto/vehicles/by-nopol/{{nopolB}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { nopol: U.get('nopolB') }); U.isArray(d.owners, 'owners');",
  }),
  r('Vehicle history by unknown nopol -> success=false', 'GET', '/api/pazaauto/vehicles/by-nopol/{{nopolUnknown}}', {
    as: 'admin', test: "U.softFail('Vehicle not found');",
  }),
  r('Vehicle transactions by nopol', 'GET', '/api/pazaauto/vehicles/{{nopolB}}/transactions', {
    as: 'admin', test: 'U.isArray(U.ok());',
  }),
  r('Transfer nopolB from A to B', 'POST', '/api/pazaauto/kendaraan/transfer', {
    as: 'admin',
    body: { nopol: '{{nopolB}}', idPelangganBaru: '#{{pelangganBId}}', keterangan: 'regression transfer' },
    test: "var d = U.ok(); U.fields(d, { nopol: U.get('nopolB'), idPelanggan: U.num('pelangganBId'), current: true });",
  }),
  r('Transfer to the current owner -> success=false', 'POST', '/api/pazaauto/kendaraan/transfer', {
    as: 'admin', body: { nopol: '{{nopolB}}', idPelangganBaru: '#{{pelangganBId}}' }, test: "U.softFail('already owned by pelanggan');",
  }),
  r('Transfer dated before current ownership -> success=false', 'POST', '/api/pazaauto/kendaraan/transfer', {
    as: 'admin', body: { nopol: '{{nopolB}}', idPelangganBaru: '#{{pelangganAId}}', tanggalAwal: '2000-01-01' }, test: "U.softFail('cannot precede');",
  }),
  r('Transfer unknown nopol -> success=false', 'POST', '/api/pazaauto/kendaraan/transfer', {
    as: 'admin', body: { nopol: '{{nopolUnknown}}', idPelangganBaru: '#{{pelangganAId}}' }, test: "U.softFail('No active ownership');",
  }),
  r('Transfer: missing idPelangganBaru -> 400', 'POST', '/api/pazaauto/kendaraan/transfer', {
    as: 'admin', body: { nopol: '{{nopolB}}' }, test: "U.badRequest('idPelangganBaru is required');",
  }),
  r('Owner history of the vehicle master', 'GET', '/api/pazaauto/kendaraan/{{kendaraanBId}}/owners', {
    as: 'admin',
    test: String.raw`
var d = U.isArray(U.ok());
U.contains(d, function (o) { return o.nopol === U.get('nopolB') && o.idPelanggan === U.num('pelangganAId') && o.current === false; }, 'A is a closed (past) owner');
U.contains(d, function (o) { return o.nopol === U.get('nopolB') && o.idPelanggan === U.num('pelangganBId') && o.current === true; }, 'B is the current owner');
`,
  }),
  r('Vehicles of B after transfer', 'GET', '/api/pazaauto/pelanggan/{{pelangganBId}}/vehicles', {
    as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (v) { return v.nopol === U.get('nopolB'); }, 'B now has nopolB');",
  }),
], 'Junction-table ownership: attach, look up, transfer, and the refusal paths. Fixtures A/B are reused by later folders.');

// ---------------------------------------------------------------------------------------------------
// 05 Work orders (SPK) & sales (penjualan)
// ---------------------------------------------------------------------------------------------------
const spkSales = f('05 SPK & penjualan flow', [
  r('Fixture: jasa for the work order', 'POST', '/api/pazaauto/jasa', {
    as: 'admin', body: { namaJasa: 'QA Servis {{runId}}', hargaJasa: 80000 },
    test: "var d = U.ok(); U.set('fxJasaId', String(d.id)); U.set('fxJasaNama', d.namaJasa);",
  }),
  r('Next SPK number', 'GET', '/api/pazaauto/spk/get-next-spk-number', {
    as: 'admin',
    test: String.raw`
var d = U.ok();
var today = new Date(); var prefix = String(today.getFullYear()) + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');
pm.test('format yyyyMMddNN', function () { pm.expect(d).to.match(/^\d{10}$/); });
pm.test('prefixed with today (server and runner share a timezone)', function () { pm.expect(d.slice(0, 8)).to.eql(prefix); });
U.set('noSpk', d);
`,
  }),
  r('Same number from rekap-penjualan', 'GET', '/api/pazaauto/rekap-penjualan/get-next-spk-number', {
    as: 'admin', test: "var d = U.ok(); pm.test('matches /spk', function () { pm.expect(d).to.eql(U.get('noSpk')); });",
  }),
  r('Create SPK: missing noSpk -> 400', 'POST', '/api/pazaauto/spk', {
    as: 'admin', body: { nopol: '{{nopolA}}', mekanikList: [] }, test: "U.badRequest('No SPK is required');",
  }),
  r('Create SPK with one jasa line', 'POST', '/api/pazaauto/spk', {
    as: 'admin',
    body: {
      noSpk: '{{noSpk}}', tanggalJamSpk: '2026-01-01 08:00:00', nopol: '{{nopolA}}', pelangganId: '#{{pelangganAId}}',
      km: 12000, kmSaatIni: 12000, statusSpk: 'OPEN', status: 'AKTIF', keterangan: 'regression', mekanikList: [],
      details: [{ namaItem: '{{fxJasaNama}}', jasaId: '#{{fxJasaId}}', harga: 80000, jumlah: 1 }],
    },
    test: String.raw`
var d = U.ok();
U.fields(d, { noSpk: U.get('noSpk'), statusSpk: 'OPEN', nopol: U.get('nopolA') });
pm.test('queue number taken from the SPK number', function () { pm.expect(d.noAntrian).to.eql(Number(U.get('noSpk').slice(-2))); });
U.set('spkId', String(d.id));
`,
  }),
  r('Get SPK by id (with details)', 'GET', '/api/pazaauto/spk/{{spkId}}', {
    as: 'admin',
    test: String.raw`
var d = U.ok();
U.fields(d, { id: U.num('spkId'), noSpk: U.get('noSpk') });
var details = U.isArray(d.details, 'details');
pm.test('one jasa line', function () { pm.expect(details.length).to.eql(1); pm.expect(details[0].namaItem).to.eql(U.get('fxJasaNama')); });
`,
  }),
  r('Get SPK by noSpk', 'GET', '/api/pazaauto/spk/by-no-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('spkId') });",
  }),
  r('Get SPK by unknown noSpk -> success=false', 'GET', '/api/pazaauto/spk/by-no-spk/0000000000', {
    as: 'admin', test: "U.softFail('SPK not found');",
  }),
  r('Get SPK by unknown id -> not found', 'GET', `/api/pazaauto/spk/${MISSING_ID}`, { as: 'admin', test: 'U.notFound();' }),
  r('List SPK', 'GET', '/api/pazaauto/spk', {
    as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (s) { return s.id === U.num('spkId'); }, 'new SPK listed');",
  }),
  r('Paginated SPK search by noSpk', 'GET', '/api/pazaauto/spk/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 10, search: '{{noSpk}}' },
    test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (s) { return s.noSpk === U.get('noSpk'); }, 'found by search');",
  }),
  r('Paginated SPK: SQL-injection sortBy -> 400', 'GET', '/api/pazaauto/spk/paginated', {
    as: 'admin', query: { sortBy: 'id%20desc%2C1' }, test: "U.fail(400, 'Unsupported sort field');",
  }),
  r('Unprocessed list excludes an OPEN SPK', 'GET', '/api/pazaauto/spk/unprocessed', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('OPEN SPK is not unprocessed yet', function () { pm.expect(d.some(function (s) { return s.noSpk === U.get('noSpk'); })).to.eql(false); });",
  }),
  r('SPK details by noSpk', 'GET', '/api/pazaauto/spk-detail/by-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('one line', function () { pm.expect(d.length).to.eql(1); });",
  }),
  r('SPK detail by composite id noSpk:namaItem', 'GET', '/api/pazaauto/spk-detail/{{noSpk}}:{{fxJasaNama}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { noSpk: U.get('noSpk'), namaItem: U.get('fxJasaNama'), jumlah: 1 });",
  }),
  r('SPK detail id without separator -> 400', 'GET', '/api/pazaauto/spk-detail/no-separator', {
    as: 'admin', test: "U.badRequest('Invalid identifier format');",
  }),
  r('SPK detail list', 'GET', '/api/pazaauto/spk-detail', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('SPK detail paginated', 'GET', '/api/pazaauto/spk-detail/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);',
  }),
  r('Update SPK: start processing', 'PUT', '/api/pazaauto/spk/{{spkId}}', {
    as: 'admin',
    body: {
      id: '#{{spkId}}', noSpk: '{{noSpk}}', tanggalJamSpk: '2026-01-01 08:00:00', nopol: '{{nopolA}}', pelangganId: '#{{pelangganAId}}',
      km: 12000, kmSaatIni: 12050, statusSpk: 'OPEN', status: 'AKTIF', keterangan: 'regression started', mekanikList: [], startProcess: true,
      details: [{ namaItem: '{{fxJasaNama}}', jasaId: '#{{fxJasaId}}', harga: 90000, jumlah: 2 }],
    },
    test: "var d = U.ok(); U.fields(d, { statusSpk: 'PROSES' }); pm.test('startedAt stamped', function () { pm.expect(d.startedAt).to.exist; });",
  }),
  r('SPK detail replaced by the update', 'GET', '/api/pazaauto/spk-detail/by-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('line updated to qty 2', function () { pm.expect(d.length).to.eql(1); pm.expect(d[0].jumlah).to.eql(2); });",
  }),
  r('Add an SPK detail line directly', 'POST', '/api/pazaauto/spk-detail', {
    as: 'admin', body: { noSpk: '{{noSpk}}', namaItem: 'QA Extra {{runId}}', harga: 15000, jumlah: 1, keterangan: 'regression' },
    test: "var d = U.ok(); U.fields(d, { noSpk: U.get('noSpk'), namaItem: 'QA Extra ' + U.get('runId'), jumlah: 1 });",
  }),
  r('Update the SPK detail line', 'PUT', '/api/pazaauto/spk-detail/{{noSpk}}:QA Extra {{runId}}', {
    as: 'admin', body: { noSpk: '{{noSpk}}', namaItem: 'QA Extra {{runId}}', harga: 15000, jumlah: 3, keterangan: 'regression v2' },
    test: "var d = U.ok(); U.fields(d, { jumlah: 3, keterangan: 'regression v2' });",
  }),
  r('Update SPK detail: id without separator -> 400', 'PUT', '/api/pazaauto/spk-detail/no-separator', {
    as: 'admin', body: { harga: 1, jumlah: 1 }, test: "U.badRequest('Invalid identifier format');",
  }),
  r('SPK now has two lines', 'GET', '/api/pazaauto/spk-detail/by-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('two lines', function () { pm.expect(d.length).to.eql(2); });",
  }),
  r('Delete the SPK detail line', 'DELETE', '/api/pazaauto/spk-detail/{{noSpk}}:QA Extra {{runId}}', { as: 'admin', test: 'U.ok();' }),
  r('SPK detail line gone', 'GET', '/api/pazaauto/spk-detail/{{noSpk}}:QA Extra {{runId}}', { as: 'admin', test: 'U.notFound();' }),
  r('Unprocessed list includes the SPK in PROSES without a sale', 'GET', '/api/pazaauto/spk/unprocessed', {
    as: 'admin', test: "var d = U.isArray(U.ok()); U.contains(d, function (s) { return s.noSpk === U.get('noSpk'); }, 'PROSES SPK awaiting payment');",
  }),
  r('Update SPK: unknown id -> not found', 'PUT', `/api/pazaauto/spk/${MISSING_ID}`, {
    as: 'admin', body: { noSpk: '0000000000', mekanikList: [] }, test: 'U.notFound();',
  }),
  r('Create penjualan for the SPK', 'POST', '/api/pazaauto/penjualan', {
    as: 'admin',
    body: {
      noPenjualan: '{{noSpk}}', noSpk: '{{noSpk}}', tanggalJamPenjualan: '2026-01-01T10:00:00.000Z', grandTotal: 180000,
      uangDibayar: 200000, kembalian: 20000, metodePembayaran: 'CASH', statusPembayaran: 'LUNAS',
      keterangan: 'Payment for SPK {{noSpk}}', pelangganId: '#{{pelangganAId}}',
    },
    test: "var d = U.ok(); U.fields(d, { noPenjualan: U.get('noSpk'), noSpk: U.get('noSpk'), statusPembayaran: 'LUNAS' });",
  }),
  r('Create penjualan: duplicate noSpk -> 400', 'POST', '/api/pazaauto/penjualan', {
    as: 'admin',
    body: { noPenjualan: 'D{{runNum}}', noSpk: '{{noSpk}}', grandTotal: 1 },
    test: "U.fail(400, 'already exists in penjualan records');",
  }),
  r('Penjualan line (jasa)', 'POST', '/api/pazaauto/penjualan-detail', {
    as: 'admin',
    body: { noPenjualan: '{{noSpk}}', namaJasaBarang: '{{fxJasaNama}}', kategori: 'JASA', jasaId: '#{{fxJasaId}}', hargaJual: 90000, kuantiti: 2, total: 180000, keterangan: 'regression' },
    test: "var d = U.ok(); U.fields(d, { noPenjualan: U.get('noSpk'), kuantiti: 2, hargaJual: 90000 }); U.set('penjualanDetailId', String(d.id));",
  }),
  r('Get penjualan line', 'GET', '/api/pazaauto/penjualan-detail/{{penjualanDetailId}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('penjualanDetailId'), kategori: 'JASA' });",
  }),
  r('Update penjualan line', 'PUT', '/api/pazaauto/penjualan-detail/{{penjualanDetailId}}', {
    as: 'admin',
    body: { noPenjualan: '{{noSpk}}', namaJasaBarang: '{{fxJasaNama}}', kategori: 'JASA', jasaId: '#{{fxJasaId}}', hargaJual: 90000, kuantiti: 1, total: 90000, keterangan: 'regression v2' },
    test: "var d = U.ok(); U.fields(d, { kuantiti: 1, keterangan: 'regression v2' });",
  }),
  r('Penjualan lines list', 'GET', '/api/pazaauto/penjualan-detail', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Penjualan lines paginated', 'GET', '/api/pazaauto/penjualan-detail/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);',
  }),
  r('Penjualan line: unknown id -> not found', 'GET', `/api/pazaauto/penjualan-detail/${MISSING_ID}`, { as: 'admin', test: 'U.notFound();' }),
  r('Penjualan line: non-numeric id -> 400', 'GET', '/api/pazaauto/penjualan-detail/abc', { as: 'admin', test: "U.fail(400, 'Invalid value format');" }),
  r('Get penjualan by number', 'GET', '/api/pazaauto/penjualan/{{noSpk}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { noPenjualan: U.get('noSpk'), metodePembayaran: 'CASH' });",
  }),
  r('Get penjualan: unknown number -> 404', 'GET', '/api/pazaauto/penjualan/NOPE{{runNum}}', {
    as: 'admin', test: "U.fail(404, 'Penjualan not found');",
  }),
  r('Print penjualan (invoice view)', 'GET', '/api/pazaauto/penjualan/{{noSpk}}/print', {
    as: 'admin',
    test: "var d = U.ok(); U.fields(d, { noPenjualan: U.get('noSpk'), noSpk: U.get('noSpk') }); U.isArray(d.items, 'items');",
  }),
  r('Print penjualan: unknown number -> 404', 'GET', '/api/pazaauto/penjualan/NOPE{{runNum}}/print', {
    as: 'admin', test: 'U.status(404);',
  }),
  r('List penjualan', 'GET', '/api/pazaauto/penjualan', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Paginated penjualan by noSpk + status filter', 'GET', '/api/pazaauto/penjualan/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 10, search: '{{noSpk}}', statusFilter: 'LUNAS' },
    test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (p) { return p.noSpk === U.get('noSpk'); }, 'found');",
  }),
  r('Paginated penjualan: SQL-injection sortBy -> 400', 'GET', '/api/pazaauto/penjualan/paginated', {
    as: 'admin', query: { sortBy: "noSpk'--" }, test: "U.fail(400, 'Unsupported sort field');",
  }),
  r('Update penjualan', 'PUT', '/api/pazaauto/penjualan/{{noSpk}}', {
    as: 'admin',
    body: {
      noPenjualan: '{{noSpk}}', noSpk: '{{noSpk}}', tanggalJamPenjualan: '2026-01-01T10:00:00.000Z', grandTotal: 90000,
      uangDibayar: 100000, kembalian: 10000, metodePembayaran: 'TRANSFER', statusPembayaran: 'LUNAS', pelangganId: '#{{pelangganAId}}',
    },
    test: "var d = U.ok(); U.fields(d, { metodePembayaran: 'TRANSFER' });",
  }),
  r('Rekap: list', 'GET', '/api/pazaauto/rekap-penjualan', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Rekap: by noSpk', 'GET', '/api/pazaauto/rekap-penjualan/by-no-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('spkId') });",
  }),
  r('Rekap: by unknown noSpk -> success=false', 'GET', '/api/pazaauto/rekap-penjualan/by-no-spk/0000000000', {
    as: 'admin', test: "U.softFail('SPK not found');",
  }),
  r('Rekap: by id joins the penjualan', 'GET', '/api/pazaauto/rekap-penjualan/{{spkId}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { noSpk: U.get('noSpk'), noPenjualan: U.get('noSpk'), statusPembayaran: 'LUNAS' });",
  }),
  r('Rekap: paginated with payment filter', 'GET', '/api/pazaauto/rekap-penjualan/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 10, search: '{{noSpk}}', statusPembayaran: 'LUNAS' },
    test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (x) { return x.noSpk === U.get('noSpk'); }, 'found');",
  }),
  r('Rekap: summary', 'GET', '/api/pazaauto/rekap-penjualan/summary', {
    as: 'admin',
    test: String.raw`
var d = U.ok();
pm.test('summary counters are numbers', function () { ['totalSpk', 'totalPelanggan', 'totalKendaraan'].forEach(function (k) { pm.expect(d[k], k).to.be.a('number'); }); });
`,
  }),
  r('Rekap: unprocessed no longer lists the paid SPK', 'GET', '/api/pazaauto/rekap-penjualan/unprocessed', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('paid SPK is processed', function () { pm.expect(d.some(function (s) { return s.noSpk === U.get('noSpk'); })).to.eql(false); });",
  }),
  r('Rekap: POST is not supported -> 304', 'POST', '/api/pazaauto/rekap-penjualan', { as: 'admin', test: 'U.status(304);' }),
  r('Rekap: PUT is not supported -> 304', 'PUT', '/api/pazaauto/rekap-penjualan/{{spkId}}', { as: 'admin', test: 'U.status(304);' }),
  r('Cancel penjualan by noSpk reverts SPK to OPEN', 'DELETE', '/api/pazaauto/penjualan/cancel-by-no-spk/{{noSpk}}', {
    as: 'admin', test: "U.ok(); pm.test('message mentions OPEN', function () { pm.expect(U.json().message).to.include('OPEN'); });",
  }),
  r('SPK is OPEN again', 'GET', '/api/pazaauto/spk/by-no-spk/{{noSpk}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { statusSpk: 'OPEN' });",
  }),
  r('Penjualan is gone', 'GET', '/api/pazaauto/penjualan/{{noSpk}}', { as: 'admin', test: 'U.status(404);' }),
  r('Cancel penjualan again -> 400', 'DELETE', '/api/pazaauto/penjualan/cancel-by-no-spk/{{noSpk}}', {
    as: 'admin', test: "U.fail(400, 'Penjualan not found for SPK');",
  }),
  r('Cancel SPK (soft, status BATAL)', 'DELETE', '/api/pazaauto/spk/{{spkId}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { statusSpk: 'BATAL' }); pm.test('keeps the previous status in keterangan', function () { pm.expect(d.keterangan).to.include('lastStatus'); });",
  }),
  r('Cancel SPK: unknown id -> success=false', 'DELETE', `/api/pazaauto/spk/${MISSING_ID}`, { as: 'admin', test: "U.softFail('SPK not found');" }),
  r('Rekap cancel: unknown id -> success=false', 'DELETE', `/api/pazaauto/rekap-penjualan/${MISSING_ID}`, { as: 'admin', test: "U.softFail('SPK not found');" }),
  r('Delete SPK permanently', 'DELETE', '/api/pazaauto/spk/delete-by-no-spk/{{noSpk}}', {
    as: 'admin', test: "U.ok(); pm.test('message says permanently', function () { pm.expect(U.json().message).to.include('permanently'); });",
  }),
  r('SPK no longer exists', 'GET', '/api/pazaauto/spk/by-no-spk/{{noSpk}}', { as: 'admin', test: "U.softFail('SPK not found');" }),
  r('Delete penjualan line already removed by the cancel (no-op)', 'DELETE', '/api/pazaauto/penjualan-detail/{{penjualanDetailId}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete penjualan: unknown number', 'DELETE', '/api/pazaauto/penjualan/NOPE{{runNum}}', {
    as: 'admin', test: "pm.test('does not succeed silently or crash', function () { pm.expect([200, 404]).to.include(pm.response.code); });",
  }),
], 'End-to-end work order: number, create with lines, process, sell, print, report, cancel, delete. Some SPK endpoints are rate-limited (10/min for writes) - do not run this folder more than a few times a minute.');

// ---------------------------------------------------------------------------------------------------
// 06 Purchasing (pembelian)
// ---------------------------------------------------------------------------------------------------
const purchasing = f('06 Pembelian flow', [
  r('Fixture: supplier', 'POST', '/api/pazaauto/supplier', {
    as: 'admin', body: { namaSupplier: 'QA Pemasok {{runId}}' }, test: "U.set('fxSupplierId', String(U.ok().id));",
  }),
  r('Next number for SPAREPART (default)', 'GET', '/api/pazaauto/pembelian/get-next-number', {
    as: 'admin', test: "var d = U.ok(); pm.test('FS + date + seq', function () { pm.expect(d).to.match(/^FS\\d{8}\\d+$/); });",
  }),
  r('Generate number for BARANG', 'GET', '/api/pazaauto/pembelian/generate-no', {
    as: 'admin', query: { jenisPembelian: 'BARANG' }, test: "var d = U.ok(); pm.test('FB prefix', function () { pm.expect(d).to.match(/^FB\\d{9,}$/); }); U.set('noPembelianBarang', d);",
  }),
  r('Create pembelian header only', 'POST', '/api/pazaauto/pembelian', {
    as: 'admin',
    body: { noPembelian: '{{noPembelianBarang}}', tanggalPembelian: '2026-01-02T08:00:00', jenisPembelian: 'BARANG', supplierId: '#{{fxSupplierId}}', grandTotal: 0, statusPembayaran: 'BELUM_LUNAS' },
    test: "var d = U.ok(); U.fields(d, { noPembelian: U.get('noPembelianBarang'), jenisPembelian: 'BARANG' }); U.set('pembelianBarangId', String(d.id));",
  }),
  r('Create pembelian: duplicate number -> rejected', 'POST', '/api/pazaauto/pembelian', {
    as: 'admin', body: { noPembelian: '{{noPembelianBarang}}', jenisPembelian: 'BARANG' },
    test: "U.fail(400, 'already exists');",
  }),
  r('Delete the header-only pembelian', 'DELETE', '/api/pazaauto/pembelian/{{pembelianBarangId}}', { as: 'admin', test: 'U.ok();' }),
  r('Generate number for OPERASIONAL', 'GET', '/api/pazaauto/pembelian/generate-no', {
    as: 'admin', query: { jenisPembelian: 'OPERASIONAL' },
    test: "var d = U.ok(); pm.test('FO prefix', function () { pm.expect(d).to.match(/^FO\\d{9,}$/); }); U.set('noPembelian', d);",
  }),
  r('Create pembelian with details', 'POST', '/api/pazaauto/pembelian/with-details', {
    as: 'admin',
    body: {
      pembelian: {
        noPembelian: '{{noPembelian}}', tanggalPembelian: '2026-01-02T09:00:00', jenisPembelian: 'OPERASIONAL',
        kategoriOperasional: 'LAINNYA', supplierId: '#{{fxSupplierId}}', grandTotal: 30000, statusPembayaran: 'LUNAS',
        metodePembayaran: 'CASH', keterangan: 'regression',
      },
      details: [
        { namaItem: 'QA Item 1', kategoriItem: 'LAIN', harga: 10000, kuantiti: 1, total: 10000 },
        { namaItem: 'QA Item 2', kategoriItem: 'LAIN', harga: 10000, kuantiti: 2, total: 20000 },
      ],
    },
    test: "var d = U.ok(); U.fields(d, { noPembelian: U.get('noPembelian'), jenisPembelian: 'OPERASIONAL' }); U.set('pembelianId', String(d.id));",
  }),
  r('Get pembelian by id', 'GET', '/api/pazaauto/pembelian/{{pembelianId}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('pembelianId'), noPembelian: U.get('noPembelian') });",
  }),
  r('Get pembelian by number through /{id} (non-numeric fallback)', 'GET', '/api/pazaauto/pembelian/{{noPembelian}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('pembelianId') }); pm.test('details loaded', function () { pm.expect(d.details).to.be.an('array').with.lengthOf(2); });",
  }),
  r('Get pembelian by number', 'GET', '/api/pazaauto/pembelian/by-no/{{noPembelian}}', {
    as: 'admin', test: "var d = U.ok(); pm.test('details loaded', function () { pm.expect(d.details).to.be.an('array').with.lengthOf(2); });",
  }),
  r('Get pembelian: unknown number -> 404', 'GET', '/api/pazaauto/pembelian/by-no/NOPE{{runNum}}', {
    as: 'admin', test: "U.fail(404, 'Pembelian not found');",
  }),
  r('Get pembelian: unknown id -> not found', 'GET', `/api/pazaauto/pembelian/${MISSING_ID}`, { as: 'admin', test: 'U.notFound();' }),
  r('Details by pembelian', 'GET', '/api/pazaauto/pembelian-detail/by-pembelian/{{pembelianId}}', {
    as: 'admin',
    test: "var d = U.isArray(U.ok()); pm.test('two lines', function () { pm.expect(d.length).to.eql(2); }); U.set('pembelianDetailId', String(d[0] && d[0].id));",
  }),
  r('Details by pembelian: non-numeric id -> 400', 'GET', '/api/pazaauto/pembelian-detail/by-pembelian/abc', {
    as: 'admin', test: "U.fail(400, 'Invalid value format');",
  }),
  r('Add a detail line', 'POST', '/api/pazaauto/pembelian-detail', {
    as: 'admin',
    body: { pembelianId: '#{{pembelianId}}', namaItem: 'QA Item 3', kategoriItem: 'LAIN', harga: 5000, kuantiti: 1, total: 5000 },
    test: "var d = U.ok(); U.fields(d, { pembelianId: U.num('pembelianId'), namaItem: 'QA Item 3' }); U.set('pembelianDetail3Id', String(d.id));",
  }),
  r('Get detail line', 'GET', '/api/pazaauto/pembelian-detail/{{pembelianDetail3Id}}', {
    as: 'admin', test: "var d = U.ok(); U.fields(d, { id: U.num('pembelianDetail3Id') });",
  }),
  r('Update detail line', 'PUT', '/api/pazaauto/pembelian-detail/{{pembelianDetail3Id}}', {
    as: 'admin',
    body: { pembelianId: '#{{pembelianId}}', namaItem: 'QA Item 3b', kategoriItem: 'LAIN', harga: 6000, kuantiti: 2, total: 12000 },
    test: "var d = U.ok(); U.fields(d, { namaItem: 'QA Item 3b', kuantiti: 2 });",
  }),
  r('Detail lines list', 'GET', '/api/pazaauto/pembelian-detail', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Detail lines paginated', 'GET', '/api/pazaauto/pembelian-detail/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);',
  }),
  r('Delete detail line', 'DELETE', '/api/pazaauto/pembelian-detail/{{pembelianDetail3Id}}', { as: 'admin', test: 'U.ok();' }),
  r('Detail line gone', 'GET', '/api/pazaauto/pembelian-detail/{{pembelianDetail3Id}}', { as: 'admin', test: 'U.notFound();' }),
  r('Update pembelian header', 'PUT', '/api/pazaauto/pembelian/{{pembelianId}}', {
    as: 'admin',
    body: { noPembelian: '{{noPembelian}}', jenisPembelian: 'OPERASIONAL', kategoriOperasional: 'LAINNYA', supplierId: '#{{fxSupplierId}}', grandTotal: 30000, statusPembayaran: 'BELUM_LUNAS', keterangan: 'regression v2' },
    test: "var d = U.ok(); U.fields(d, { statusPembayaran: 'BELUM_LUNAS', keterangan: 'regression v2' });",
  }),
  r('Update pembelian with details (replace lines)', 'PUT', '/api/pazaauto/pembelian/{{pembelianId}}/with-details', {
    as: 'admin',
    body: {
      pembelian: { noPembelian: '{{noPembelian}}', tanggalPembelian: '2026-01-02T09:00:00', jenisPembelian: 'OPERASIONAL', kategoriOperasional: 'LAINNYA', supplierId: '#{{fxSupplierId}}', grandTotal: 15000, statusPembayaran: 'LUNAS' },
      details: [{ namaItem: 'QA Item only', kategoriItem: 'LAIN', harga: 15000, kuantiti: 1, total: 15000 }],
    },
    test: "var d = U.ok(); U.fields(d, { id: U.num('pembelianId'), statusPembayaran: 'LUNAS' });",
  }),
  r('Lines were replaced', 'GET', '/api/pazaauto/pembelian-detail/by-pembelian/{{pembelianId}}', {
    as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('one line', function () { pm.expect(d.length).to.eql(1); pm.expect(d[0].namaItem).to.eql('QA Item only'); });",
  }),
  r('List pembelian', 'GET', '/api/pazaauto/pembelian', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Paginated pembelian filtered by jenis + search', 'GET', '/api/pazaauto/pembelian/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 10, jenisPembelian: 'OPERASIONAL', search: '{{noPembelian}}' },
    test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (p) { return p.id === U.num('pembelianId'); }, 'found');",
  }),
  r('Paginated pembelian filtered by another jenis excludes it', 'GET', '/api/pazaauto/pembelian/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 10, jenisPembelian: 'SPAREPART', search: '{{noPembelian}}' },
    test: "var rows = U.page(U.ok(), 10); pm.test('not in SPAREPART results', function () { pm.expect(rows.some(function (p) { return p.id === U.num('pembelianId'); })).to.eql(false); });",
  }),
  r('Paginated pembelian: SQL-injection sortBy -> 400', 'GET', '/api/pazaauto/pembelian/paginated', {
    as: 'admin', query: { sortBy: '(select%201)' }, test: "U.fail(400, 'Unsupported sort field');",
  }),
  r('Update pembelian: unknown id -> not found', 'PUT', `/api/pazaauto/pembelian/${MISSING_ID}`, {
    as: 'admin', body: { noPembelian: 'X', jenisPembelian: 'BARANG' }, test: 'U.notFound();',
  }),
  r('Delete pembelian', 'DELETE', '/api/pazaauto/pembelian/{{pembelianId}}', { as: 'admin', test: 'U.ok();' }),
  r('Pembelian gone', 'GET', '/api/pazaauto/pembelian/by-no/{{noPembelian}}', { as: 'admin', test: 'U.status(404);' }),
  r('Delete fixture supplier', 'DELETE', '/api/pazaauto/supplier/{{fxSupplierId}}', { as: 'admin', test: 'U.ok();' }),
], 'Purchase document numbering, header + lines, partial updates, filters and cleanup.');

// ---------------------------------------------------------------------------------------------------
// 07 Attendance (absensi)
// ---------------------------------------------------------------------------------------------------
const absensi = f('07 Absensi (attendance)', [
  r('Fixture: employee who will clock in', 'POST', '/api/pazaauto/karyawan', {
    as: 'admin', body: { namaKaryawan: 'QA Absen {{runId}}' }, test: "U.set('fxKaryawanId', String(U.ok().id));",
  }),
  r('Fixture: employee who never clocks in', 'POST', '/api/pazaauto/karyawan', {
    as: 'admin', body: { namaKaryawan: 'QA Bolos {{runId}}' }, test: "U.set('fxKaryawan2Id', String(U.ok().id));",
  }),
  r('Clock-in (admin on behalf of the employee)', 'POST', '/api/pazaauto/absensi/clock-in', {
    as: 'admin',
    body: { karyawanId: '#{{fxKaryawanId}}', deviceInfo: 'postman-regression', lokasiMasuk: 'Workshop' },
    test: String.raw`
// The clock-in IP allowlist (tb_absensi_config) may refuse the runner's address; then the flow is skipped
var b = U.json();
if (pm.response.code === 400 && b && /not allowed from this IP/.test(b.error)) {
  U.set('absensiBlocked', 'true');
  pm.test('clock-in refused by the IP allowlist (flow skipped)', function () { pm.expect(b.success).to.eql(false); });
} else {
  U.set('absensiBlocked', '');
  var d = U.ok();
  U.fields(d, { karyawanId: U.num('fxKaryawanId') });
  pm.test('jamMasuk stamped', function () { pm.expect(d.jamMasuk).to.exist; });
  U.set('absensiId', String(d.id));
}
`,
  }),
  ...[
    ['Clock-in twice -> 400', 'POST', '/api/pazaauto/absensi/clock-in', { karyawanId: '#{{fxKaryawanId}}' }, "U.fail(400, 'Already clocked in today');"],
    ["Today's attendance", 'GET', '/api/pazaauto/absensi/today/{{fxKaryawanId}}', undefined, "var d = U.ok(); U.fields(d, { id: U.num('absensiId'), karyawanId: U.num('fxKaryawanId') });"],
    ['Get attendance by id', 'GET', '/api/pazaauto/absensi/{{absensiId}}', undefined, "var d = U.ok(); U.fields(d, { id: U.num('absensiId') });"],
    ['Clock-out', 'POST', '/api/pazaauto/absensi/clock-out', { karyawanId: '#{{fxKaryawanId}}', lokasiKeluar: 'Workshop', keterangan: 'regression' }, "var d = U.ok(); pm.test('jamKeluar stamped', function () { pm.expect(d.jamKeluar).to.exist; });"],
    ['Clock-out twice -> 400', 'POST', '/api/pazaauto/absensi/clock-out', { karyawanId: '#{{fxKaryawanId}}' }, "U.fail(400, 'Already clocked out today');"],
    ['Update attendance', 'PUT', '/api/pazaauto/absensi/{{absensiId}}', { karyawanId: '#{{fxKaryawanId}}', tanggal: '{{today}}', status: 'HADIR', keterangan: 'edited by regression' }, "var d = U.ok(); U.fields(d, { keterangan: 'edited by regression' });"],
  ].map(([name, m, p, body, test]) => r(name, m, p, {
    as: 'admin', body, test,
    pre: "U.set('today', new Date().toISOString().slice(0, 10)); U.skipUnless(!U.get('absensiBlocked'), 'clock-in blocked by IP allowlist');",
  })),
  r('Clock-out without clock-in -> 400', 'POST', '/api/pazaauto/absensi/clock-out', {
    as: 'admin', body: { karyawanId: '#{{fxKaryawan2Id}}' },
    test: String.raw`
var b = U.json();
if (b && /not allowed from this IP/.test(b.error)) { U.fail(400); } else { U.fail(400, 'Must clock in before clocking out'); }
`,
  }),
  r("Today's attendance when absent -> 404", 'GET', '/api/pazaauto/absensi/today/{{fxKaryawan2Id}}', {
    as: 'admin', test: "U.fail(404, 'No attendance found for today');",
  }),
  r('Mark absence (SAKIT) for the second employee', 'POST', '/api/pazaauto/absensi/mark-absence', {
    as: 'admin',
    body: { karyawanId: '#{{fxKaryawan2Id}}', tanggal: '2026-01-05', status: 'SAKIT', keterangan: 'regression' },
    test: "var d = U.ok(); U.fields(d, { karyawanId: U.num('fxKaryawan2Id'), status: 'SAKIT' }); U.set('absensi2Id', String(d.id));",
  }),
  r('Create attendance record directly', 'POST', '/api/pazaauto/absensi', {
    as: 'admin',
    body: { karyawanId: '#{{fxKaryawan2Id}}', tanggal: '2026-01-06', status: 'IZIN', keterangan: 'regression' },
    test: "var d = U.ok(); U.fields(d, { status: 'IZIN' }); U.set('absensi3Id', String(d.id));",
  }),
  r('History of the second employee (date range + status)', 'GET', '/api/pazaauto/absensi/history', {
    as: 'admin', query: { karyawanId: '{{fxKaryawan2Id}}', startDate: '2026-01-01', endDate: '2026-01-31', status: 'SAKIT' },
    test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (a) { return a.id === U.num('absensi2Id'); }, 'SAKIT record in range');",
  }),
  r('History of everyone (admin may omit karyawanId)', 'GET', '/api/pazaauto/absensi/history', {
    as: 'admin', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);',
  }),
  r('History with a malformed date -> 400', 'GET', '/api/pazaauto/absensi/history', {
    as: 'admin', query: { startDate: 'not-a-date' },
    test: "U.fail(400, 'yyyy-MM-dd');",
  }),
  r('Monthly summary', 'GET', '/api/pazaauto/absensi/summary/{{fxKaryawan2Id}}', {
    as: 'admin', query: { month: 1, year: 2026 }, test: "var d = U.ok(); pm.test('summary is an object', function () { pm.expect(d).to.be.an('object'); });",
  }),
  r('List attendance', 'GET', '/api/pazaauto/absensi', { as: 'admin', test: 'U.isArray(U.ok());' }),
  r('Paginated attendance', 'GET', '/api/pazaauto/absensi/paginated', {
    as: 'admin', query: { page: 1, rowsPerPage: 5, sortBy: 'tanggal', descending: true }, test: 'U.page(U.ok(), 5);',
  }),
  r('Paginated attendance: SQL-injection sortBy -> 400', 'GET', '/api/pazaauto/absensi/paginated', {
    as: 'admin', query: { sortBy: 'tanggal%20desc' }, test: "U.fail(400, 'Unsupported sort field');",
  }),
  r('Get attendance: unknown id -> not found', 'GET', `/api/pazaauto/absensi/${MISSING_ID}`, { as: 'admin', test: 'U.notFound();' }),
  r('Delete attendance records (cleanup)', 'DELETE', '/api/pazaauto/absensi/{{absensi2Id}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete attendance record 3', 'DELETE', '/api/pazaauto/absensi/{{absensi3Id}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete clock-in record', 'DELETE', '/api/pazaauto/absensi/{{absensiId}}', {
    as: 'admin', pre: "U.skipUnless(!U.get('absensiBlocked'), 'no clock-in record');", test: 'U.ok();',
  }),
  r('Delete fixture employee 1', 'DELETE', '/api/pazaauto/karyawan/{{fxKaryawanId}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete fixture employee 2', 'DELETE', '/api/pazaauto/karyawan/{{fxKaryawan2Id}}', { as: 'admin', test: 'U.ok();' }),
], 'Clock-in/out state machine, absence marking, history filters. Self-service checks for the Karyawan role live in 02 Access control.');

// ---------------------------------------------------------------------------------------------------
// 08 Administration (Owner)
// ---------------------------------------------------------------------------------------------------
const admin = f('08 Administration', [
  f('Users & temporary passwords', [
    r('List users (no password hashes)', 'GET', '/api/users', {
      as: 'owner',
      test: "var d = U.isArray(U.ok()); pm.test('no password material in any row', function () { d.forEach(function (u) { pm.expect(u.passwordHash).to.eql(undefined); pm.expect(u.password).to.eql(undefined); }); });",
    }),
    r('Paginated users', 'GET', '/api/users/paginated', { as: 'owner', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);' }),
    r('Paginated users: SQL-injection sortBy -> 400', 'GET', '/api/users/paginated', { as: 'owner', query: { sortBy: 'username%3B' }, test: "U.fail(400, 'Unsupported sort field');" }),
    r('Pending Google sign-ups', 'GET', '/api/users/pending', { as: 'owner', test: 'U.isArray(U.ok());' }),
    r('Create user: weak password -> 400', 'POST', '/api/users', {
      as: 'owner', body: { username: '{{tempUsername}}x', email: '{{tempUsername}}x@example.test', password: 'short', active: true },
      test: "U.fail(400, 'at least 8 characters');",
    }),
    r('Create user: password equal to username -> 400', 'POST', '/api/users', {
      as: 'owner', body: { username: '{{tempUsername}}', email: '{{tempUsername}}@example.test', password: '{{tempUsername}}', active: true },
      test: "U.fail(400, 'same as the username');",
    }),
    r('Create user with a temporary password', 'POST', '/api/users', {
      as: 'owner', body: { username: '{{tempUsername}}', email: '{{tempUsername}}@example.test', password: '{{tempUserPassword}}', active: true },
      test: String.raw`
var d = U.ok();
U.fields(d, { username: U.get('tempUsername'), mustChangePassword: true });
U.absent(d, ['password', 'passwordHash'], 'created user');
U.set('tempUserId', String(d.id));
`,
    }),
    r('Get user', 'GET', '/api/users/{{tempUserId}}', {
      as: 'owner', test: "var d = U.ok(); U.fields(d, { id: U.num('tempUserId') }); U.absent(d, ['password', 'passwordHash'], 'user');",
    }),
    r('Login with the temporary password', 'POST', '/api/auth/login', {
      body: { username: '{{tempUsername}}', password: '{{tempUserPassword}}' },
      test: "var d = U.ok(); U.fields(d, { mustChangePassword: true }); U.set('tempUserToken', d.token);",
    }),
    r('Temporary session: role endpoints -> 403', 'GET', '/api/pazaauto/barang', { as: 'tempUser', test: 'U.forbidden();' }),
    r('Temporary session: /me still works', 'GET', '/api/auth/me', {
      as: 'tempUser', test: "var d = U.ok(); U.fields(d, { username: U.get('tempUsername') });",
    }),
    r('Temporary session: new password equal to current -> 400', 'POST', '/api/auth/change-password', {
      as: 'tempUser', body: { currentPassword: '{{tempUserPassword}}', newPassword: '{{tempUserPassword}}' },
      test: "U.fail(400, 'must be different');",
    }),
    r('Change the temporary password', 'POST', '/api/auth/change-password', {
      as: 'tempUser', body: { currentPassword: '{{tempUserPassword}}', newPassword: '{{tempUserPasswordNew}}' },
      test: "var d = U.ok(); U.fields(d, { mustChangePassword: false }); U.set('tempUserToken', d.token);",
    }),
    r('Old password no longer works', 'POST', '/api/auth/login', {
      body: { username: '{{tempUsername}}', password: '{{tempUserPassword}}' }, test: 'U.fail(401);',
    }),
    r('New password works', 'POST', '/api/auth/login', {
      body: { username: '{{tempUsername}}', password: '{{tempUserPasswordNew}}' },
      test: "var d = U.ok(); U.fields(d, { mustChangePassword: false }); U.set('tempUserToken', d.token);",
    }),
    r('Update user: weak password -> 400', 'PUT', '/api/users/{{tempUserId}}', {
      as: 'owner', body: { username: '{{tempUsername}}', email: '{{tempUsername}}@example.test', password: 'short', active: true },
      test: "U.fail(400, 'at least 8 characters');",
    }),
    r('Update user email', 'PUT', '/api/users/{{tempUserId}}', {
      as: 'owner', body: { username: '{{tempUsername}}', email: 'changed.{{tempUsername}}@example.test', active: true },
      test: String.raw`
var d = U.ok();
U.fields(d, { email: 'changed.' + U.get('tempUsername') + '@example.test', mustChangePassword: false });
U.absent(d, ['password', 'passwordHash'], 'updated user');
`,
    }),
    r('Update user: unknown id -> 404', 'PUT', `/api/users/${MISSING_ID}`, {
      as: 'owner', body: { username: 'x', email: 'x@example.test', active: true }, test: "U.fail(404, 'Data not found');",
    }),
    r('Logout the temporary user (all sessions)', 'POST', '/api/auth/logout', {
      as: 'tempUser', test: "U.ok(); pm.test('refresh cookie cleared', function () { pm.expect(U.cookie('refresh_token')).to.match(/Max-Age=0/i); });",
    }),
    r('Refresh after logout -> 401', 'POST', '/api/auth/refresh', {
      pre: "U.forgetCookie('/api/auth/refresh', 'refresh_token');", test: 'U.unauthorized();',
    }),
    r('Approve unknown user -> error', 'POST', `/api/users/${MISSING_ID}/approve`, {
      as: 'owner', test: "pm.test('4xx with success=false', function () { pm.expect(pm.response.code).to.be.within(400, 499); pm.expect(U.json().success).to.eql(false); });",
    }),
    r('Reject unknown user -> error', 'POST', `/api/users/${MISSING_ID}/reject`, {
      as: 'owner', test: "pm.test('4xx with success=false', function () { pm.expect(pm.response.code).to.be.within(400, 499); pm.expect(U.json().success).to.eql(false); });",
    }),
    r('Get user: unknown id -> 404', 'GET', `/api/users/${MISSING_ID}`, { as: 'owner', test: "U.fail(404, 'Data not found');" }),
    r('Get user: non-numeric id -> 400', 'GET', '/api/users/abc', { as: 'owner', test: "U.fail(400, 'Invalid value format');" }),
  ]),
  f('Roles, permissions & RBAC', [
    r('Create role (CRUD API)', 'POST', '/api/roles', {
      as: 'owner', body: { name: 'QA_ROLE_{{runId}}', description: 'regression', active: true },
      test: "var d = U.ok(); U.fields(d, { name: 'QA_ROLE_' + U.get('runId') }); U.set('roleId', String(d.id));",
    }),
    r('Get role', 'GET', '/api/roles/{{roleId}}', { as: 'owner', test: "var d = U.ok(); U.fields(d, { id: U.num('roleId') });" }),
    r('Update role', 'PUT', '/api/roles/{{roleId}}', {
      as: 'owner', body: { name: 'QA_ROLE_{{runId}}', description: 'regression v2', active: true },
      test: "var d = U.ok(); U.fields(d, { description: 'regression v2' });",
    }),
    r('Paginated roles', 'GET', '/api/roles/paginated', { as: 'owner', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);' }),
    r('Role lookup', 'GET', '/api/roles/lookup', {
      as: 'owner', test: "var d = U.isArray(U.ok()); U.contains(d, function (x) { return x.id === U.num('roleId'); }, 'new role in lookup');",
    }),
    r('Assign the temp user to the role', 'PUT', '/api/roles/{{roleId}}/users', {
      as: 'owner', body: { userIds: ['#{{tempUserId}}'] }, test: 'U.ok();',
    }),
    r('Users of the role', 'GET', '/api/roles/{{roleId}}/users', {
      as: 'owner', test: "var d = U.isArray(U.ok()); U.contains(d, function (u) { return u.id === U.num('tempUserId'); }, 'temp user assigned');",
    }),
    r('Clear role users', 'PUT', '/api/roles/{{roleId}}/users', { as: 'owner', body: { userIds: [] }, test: 'U.ok();' }),
    r('Create permission (CRUD API)', 'POST', '/api/permissions', {
      as: 'owner', body: { name: 'qa:{{runId}}:read', description: 'regression', resource: 'qa{{runId}}', action: 'read', active: true },
      test: "var d = U.ok(); U.set('permissionId', String(d.id));",
    }),
    r('Get permission', 'GET', '/api/permissions/{{permissionId}}', { as: 'owner', test: "var d = U.ok(); U.fields(d, { action: 'read' });" }),
    r('Update permission', 'PUT', '/api/permissions/{{permissionId}}', {
      as: 'owner', body: { name: 'qa:{{runId}}:read', description: 'regression v2', resource: 'qa{{runId}}', action: 'read', active: true },
      test: "var d = U.ok(); U.fields(d, { description: 'regression v2' });",
    }),
    r('List permissions', 'GET', '/api/permissions', { as: 'owner', test: 'U.isArray(U.ok());' }),
    r('Paginated permissions', 'GET', '/api/permissions/paginated', { as: 'owner', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);' }),
    r('Paginated permissions: SQL-injection sortBy -> 400', 'GET', '/api/permissions/paginated', { as: 'owner', query: { sortBy: 'name%3B' }, test: "U.fail(400, 'Unsupported sort field');" }),
    r('RBAC: create role with blank name -> 400', 'POST', '/api/rbac/roles', {
      as: 'owner', body: { name: '' }, test: "U.badRequest('Role name harus diisi');",
    }),
    r('RBAC: create role', 'POST', '/api/rbac/roles', {
      as: 'owner', body: { name: 'QA_RBAC_{{runId}}', description: 'regression' },
      test: String.raw`
// 403 here means app.rbac.enabled=false; every other RBAC request is then skipped
if (pm.response.code === 403) { U.fail(403, 'RBAC module is not enabled'); U.set('rbacOff', 'true'); }
else { U.set('rbacOff', ''); var d = U.ok(); U.fields(d, { name: 'QA_RBAC_' + U.get('runId') }); U.set('rbacRoleId', String(d.id)); }
`,
    }),
    r('RBAC: duplicate role name -> 400', 'POST', '/api/rbac/roles', {
      as: 'owner', body: { name: 'QA_RBAC_{{runId}}' }, pre: "U.skipUnless(!U.get('rbacOff'), 'RBAC disabled');", test: 'U.fail(400);',
    }),
    ...[
      ['RBAC: list roles', 'GET', '/api/rbac/roles', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (x) { return x.id === U.num('rbacRoleId'); }, 'role listed');"],
      ['RBAC: get role', 'GET', '/api/rbac/roles/{{rbacRoleId}}', undefined, "var d = U.ok(); U.fields(d, { id: U.num('rbacRoleId') });"],
      ['RBAC: get unknown role -> 404', 'GET', `/api/rbac/roles/${MISSING_ID}`, undefined, 'U.fail(404);'],
      ['RBAC: update role', 'PUT', '/api/rbac/roles/{{rbacRoleId}}', { name: 'QA_RBAC_{{runId}}', description: 'regression v2' }, "var d = U.ok(); U.fields(d, { description: 'regression v2' });"],
      ['RBAC: deactivate role', 'POST', '/api/rbac/roles/{{rbacRoleId}}/deactivate', undefined, 'U.ok();'],
      ['RBAC: role is inactive', 'GET', '/api/rbac/roles/{{rbacRoleId}}', undefined, "var d = U.ok(); U.fields(d, { active: false });"],
      ['RBAC: activate role', 'POST', '/api/rbac/roles/{{rbacRoleId}}/activate', undefined, 'U.ok();'],
      ['RBAC: activate unknown role -> 404', 'POST', `/api/rbac/roles/${MISSING_ID}/activate`, undefined, 'U.fail(404);'],
      ['RBAC: create permission: missing resource -> 400', 'POST', '/api/rbac/permissions', { name: 'x', action: 'read' }, "U.badRequest('Resource harus diisi');"],
      ['RBAC: create permission', 'POST', '/api/rbac/permissions', { name: 'qa:{{runId}}:write', resource: 'qa{{runId}}', action: 'write' }, "var d = U.ok(); U.set('rbacPermissionId', String(d.id));"],
      ['RBAC: list permissions', 'GET', '/api/rbac/permissions', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (p) { return p.id === U.num('rbacPermissionId'); }, 'permission listed');"],
      ['RBAC: get permission', 'GET', '/api/rbac/permissions/{{rbacPermissionId}}', undefined, "var d = U.ok(); U.fields(d, { action: 'write' });"],
      ['RBAC: get unknown permission -> 404', 'GET', `/api/rbac/permissions/${MISSING_ID}`, undefined, 'U.fail(404);'],
      ['RBAC: assign permission to role', 'POST', '/api/rbac/roles/{{rbacRoleId}}/permissions/{{rbacPermissionId}}', undefined, 'U.ok();'],
      ['RBAC: role permissions', 'GET', '/api/rbac/roles/{{rbacRoleId}}/permissions', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (p) { return p.id === U.num('rbacPermissionId'); }, 'assigned');"],
      ['RBAC: assign unknown permission -> 400', 'POST', `/api/rbac/roles/{{rbacRoleId}}/permissions/${MISSING_ID}`, undefined, 'U.fail(400);'],
      ['RBAC: assign role to temp user', 'POST', '/api/rbac/users/{{tempUsername}}/roles/{{rbacRoleId}}', undefined, 'U.ok();'],
      ['RBAC: user roles', 'GET', '/api/rbac/users/{{tempUsername}}/roles', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (x) { return x.id === U.num('rbacRoleId'); }, 'role on user');"],
      ['RBAC: user permissions (through the role)', 'GET', '/api/rbac/users/{{tempUsername}}/permissions', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (p) { return p.id === U.num('rbacPermissionId'); }, 'inherited permission');"],
      ['RBAC: roles of unknown user -> 404', 'GET', '/api/rbac/users/qa-nobody-{{runId}}/roles', undefined, 'U.fail(404);'],
      ['RBAC: permissions of unknown user -> 404', 'GET', '/api/rbac/users/qa-nobody-{{runId}}/permissions', undefined, 'U.fail(404);'],
      ['RBAC: assign role to unknown user -> 400', 'POST', '/api/rbac/users/qa-nobody-{{runId}}/roles/{{rbacRoleId}}', undefined, 'U.fail(400);'],
      ['RBAC: remove role from temp user', 'DELETE', '/api/rbac/users/{{tempUsername}}/roles/{{rbacRoleId}}', undefined, 'U.ok();'],
      ['RBAC: remove permission from role', 'DELETE', '/api/rbac/roles/{{rbacRoleId}}/permissions/{{rbacPermissionId}}', undefined, 'U.ok();'],
      ['RBAC: role permissions now empty', 'GET', '/api/rbac/roles/{{rbacRoleId}}/permissions', undefined, "var d = U.isArray(U.ok()); pm.test('empty', function () { pm.expect(d.length).to.eql(0); });"],
      ['RBAC: delete role', 'DELETE', '/api/rbac/roles/{{rbacRoleId}}', undefined, 'U.ok();'],
      ['RBAC: delete role again -> 404', 'DELETE', '/api/rbac/roles/{{rbacRoleId}}', undefined, 'U.fail(404);'],
      ['Delete RBAC permission (CRUD API)', 'DELETE', '/api/permissions/{{rbacPermissionId}}', undefined, 'U.ok();'],
    ].map(([name, m, p, body, test]) => r(name, m, p, { as: 'owner', body, test, pre: "U.skipUnless(!U.get('rbacOff'), 'RBAC disabled');" })),
    r('Delete permission (CRUD API)', 'DELETE', '/api/permissions/{{permissionId}}', { as: 'owner', test: 'U.ok();' }),
    r('Delete role (CRUD API)', 'DELETE', '/api/roles/{{roleId}}', { as: 'owner', test: 'U.ok();' }),
    r('Role gone', 'GET', '/api/roles/{{roleId}}', { as: 'owner', test: 'U.notFound();' }),
    r('Deactivate the temp user', 'PUT', '/api/users/{{tempUserId}}', {
      as: 'owner', body: { username: '{{tempUsername}}', email: 'changed.{{tempUsername}}@example.test', active: false },
      test: "U.fields(U.ok(), { active: false });",
    }),
    r('Deactivated user cannot log in', 'POST', '/api/auth/login', {
      body: { username: '{{tempUsername}}', password: '{{tempUserPasswordNew}}' }, test: 'U.fail(401);',
    }),
    r('Delete the temp user', 'DELETE', '/api/users/{{tempUserId}}', { as: 'owner', test: 'U.ok();' }),
    r('Temp user gone', 'GET', '/api/users/{{tempUserId}}', { as: 'owner', test: 'U.notFound();' }),
  ]),
  f('System parameters', [
    r('Create parameter', 'POST', '/api/system-parameters', {
      as: 'owner', body: { name: 'qa.param.{{runId}}', value: 'v1' },
      test: "var d = U.ok(); U.fields(d, { name: 'qa.param.' + U.get('runId'), value: 'v1' }); U.set('paramId', String(d.id));",
    }),
    r('Get parameter', 'GET', '/api/system-parameters/{{paramId}}', { as: 'owner', test: "var d = U.ok(); U.fields(d, { value: 'v1' });" }),
    r('Update parameter', 'PUT', '/api/system-parameters/{{paramId}}', {
      as: 'owner', body: { name: 'qa.param.{{runId}}', value: 'v2' }, test: "var d = U.ok(); U.fields(d, { value: 'v2' });",
    }),
    r('List parameters', 'GET', '/api/system-parameters', { as: 'owner', test: 'U.isArray(U.ok());' }),
    r('Paginated parameters: search', 'GET', '/api/system-parameters/paginated', {
      as: 'owner', query: { page: 1, rowsPerPage: 10, search: 'qa.param.{{runId}}' },
      test: "var rows = U.page(U.ok(), 10); U.contains(rows, function (p) { return p.id === U.num('paramId'); }, 'found');",
    }),
    r('Paginated parameters: SQL-injection sortBy -> 400', 'GET', '/api/system-parameters/paginated', {
      as: 'owner', query: { sortBy: 'name%20asc' }, test: "U.fail(400, 'Unsupported sort field');",
    }),
    r('Get parameter: unknown id -> 404', 'GET', `/api/system-parameters/${MISSING_ID}`, { as: 'owner', test: "U.fail(404, 'Data not found');" }),
    r('Delete parameter', 'DELETE', '/api/system-parameters/{{paramId}}', { as: 'owner', test: 'U.ok();' }),
  ]),
  f('Audit trail, cache, summary', [
    r('Recent audit rows honour the limit', 'GET', '/api/audit-trail', {
      as: 'admin', query: { limit: 3 }, test: "var d = U.isArray(U.ok()); pm.test('at most 3 rows', function () { pm.expect(d.length).to.be.at.most(3); });",
    }),
    r('Audit limit is capped at 1000', 'GET', '/api/audit-trail', {
      as: 'admin', query: { limit: 5000 }, test: "var d = U.isArray(U.ok()); pm.test('at most 1000 rows', function () { pm.expect(d.length).to.be.at.most(1000); });",
    }),
    r('Audit usernames', 'GET', '/api/audit-trail/usernames', { as: 'admin', test: 'U.isArray(U.ok());' }),
    r('Audit paginated (newest first by default)', 'GET', '/api/audit-trail/paginated', {
      as: 'admin', query: { page: 1, rowsPerPage: 5 }, test: 'U.page(U.ok(), 5);',
    }),
    r('Audit paginated: SQL-injection sortBy -> 400', 'GET', '/api/audit-trail/paginated', {
      as: 'admin', query: { sortBy: 'id%3Bselect' }, test: "U.fail(400, 'Unsupported sort field');",
    }),
    r('Audit rows for tb_pelanggan', 'GET', '/api/audit-trail/table/tb_pelanggan', {
      as: 'admin', query: { limit: 20 }, test: 'U.isArray(U.ok());',
    }),
    r('Audit rows for pelanggan A', 'GET', '/api/audit-trail/table/tb_pelanggan/record/{{pelangganAId}}', {
      as: 'admin', test: "var d = U.isArray(U.ok()); pm.test('triggers recorded the fixture insert', function () { pm.expect(d.length).to.be.above(0); });",
    }),
    r('Audit rows by record id', 'GET', '/api/audit-trail/record/{{pelangganAId}}', { as: 'admin', query: { limit: 5 }, test: 'U.isArray(U.ok());' }),
    r('Audit rows by user id', 'GET', '/api/audit-trail/user/1', { as: 'admin', query: { limit: 5 }, test: 'U.isArray(U.ok());' }),
    r('Audit rows by record: non-numeric id -> 404', 'GET', '/api/audit-trail/record/abc', {
      as: 'admin', test: "pm.test('rejected', function () { pm.expect([400, 404]).to.include(pm.response.code); });",
    }),
    r('Clear server caches', 'POST', '/api/admin/caffeine-cache/clear', {
      as: 'admin', test: "var d = U.ok(); pm.test('returns the number of caches cleared', function () { pm.expect(d).to.be.a('number').and.at.least(0); });",
    }),
    r('Business summary for a date range', 'GET', '/api/pazaauto/summary', {
      as: 'owner', query: { startDate: '2026-01-01', endDate: '2026-12-31' },
      test: "var d = U.ok(); pm.test('summary object', function () { pm.expect(d).to.be.an('object'); });",
    }),
    r('Business summary defaults to this month', 'GET', '/api/pazaauto/summary', {
      as: 'owner', test: "var d = U.ok(); pm.test('summary object', function () { pm.expect(d).to.be.an('object'); });",
    }),
  ]),
], 'Owner/Admin tooling: users (incl. temporary-password lockdown), roles, permissions, RBAC, parameters, audit, caches, summary.');

// ---------------------------------------------------------------------------------------------------
// 09 Customer portal
// ---------------------------------------------------------------------------------------------------
const customer = f('09 Customer portal', [
  f('Gate', [
    r('Disable the portal', 'PUT', '/api/settings/customer-access', { as: 'owner', body: { enabled: false }, test: "U.fields(U.ok(), { enabled: false });" }),
    r('Portal answers 503 while disabled (before auth)', 'GET', '/api/customer/auth/me', {
      test: "U.fail(503, 'temporarily unavailable');",
    }),
    r('Public status says disabled', 'GET', '/api/public/customer-access-status', { test: "U.fields(U.ok(), { customerAccessEnabled: false });" }),
    r('Re-enable the portal', 'PUT', '/api/settings/customer-access', { as: 'owner', body: { enabled: true }, test: "U.fields(U.ok(), { enabled: true });" }),
    r('Admin may not flip the switch -> 403', 'PUT', '/api/settings/customer-access', { as: 'admin', body: { enabled: false }, test: 'U.forbidden();' }),
  ]),
  f('Registration & login', [
    r('Register: invalid email -> 400', 'POST', '/api/customer/auth/register', {
      body: { email: 'nope', password: '{{customerPassword}}', fullName: 'QA' }, test: "U.badRequest('Email tidak valid');",
    }),
    r('Register: missing name -> 400', 'POST', '/api/customer/auth/register', {
      body: { email: '{{customerEmail}}', password: '{{customerPassword}}' }, test: "U.badRequest('Nama harus diisi');",
    }),
    r('Register: weak password -> 400', 'POST', '/api/customer/auth/register', {
      body: { email: '{{customerEmail}}', password: 'short', fullName: 'QA Customer' }, test: "U.fail(400, 'at least 8 characters');",
    }),
    r('Register customer 1', 'POST', '/api/customer/auth/register', {
      body: { email: '{{customerEmail}}', phone: '{{customerPhone}}', password: '{{customerPassword}}', fullName: 'QA Customer {{runId}}' },
      test: String.raw`
var d = U.ok();
U.fields(d, { email: U.get('customerEmail'), fullName: 'QA Customer ' + U.get('runId'), linked: false });
U.absent(d, ['refreshToken'], 'register body');
var c = U.cookie('customer_refresh_token');
pm.test('customer refresh cookie is HttpOnly, SameSite=Strict, scoped to /api/customer/auth', function () {
  pm.expect(c).to.be.a('string');
  pm.expect(c).to.match(/HttpOnly/i);
  pm.expect(c).to.match(/SameSite=Strict/i);
  pm.expect(c).to.match(/Path=\/api\/customer\/auth/i);
});
pm.test('token carries only the Customer group', function () { pm.expect(U.claims(d.token).groups).to.eql(['Customer']); });
U.set('customerToken', d.token);
`,
    }),
    r('Register: duplicate email -> 400', 'POST', '/api/customer/auth/register', {
      body: { email: '{{customerEmail}}', password: '{{customerPassword}}', fullName: 'QA dup' }, test: "U.fail(400, 'Email is already registered');",
    }),
    r('Register: duplicate phone -> 400', 'POST', '/api/customer/auth/register', {
      body: { email: 'other.{{customerEmail}}', phone: '{{customerPhone}}', password: '{{customerPassword}}', fullName: 'QA dup' },
      test: "U.fail(400, 'Phone number is already registered');",
    }),
    r('Register customer 2 (no phone)', 'POST', '/api/customer/auth/register', {
      body: { email: '{{customer2Email}}', password: '{{customerPassword}}', fullName: 'QA Customer2 {{runId}}' },
      test: "var d = U.ok(); U.set('customer2Token', d.token);",
    }),
    r('Register: neither email nor phone -> 400', 'POST', '/api/customer/auth/register', {
      body: { password: '{{customerPassword}}', fullName: 'QA none' }, test: "U.badRequest('Isi email atau nomor HP');",
    }),
    r('Register: invalid phone -> 400', 'POST', '/api/customer/auth/register', {
      body: { phone: '0812', password: '{{customerPassword}}', fullName: 'QA bad phone' },
      test: "U.fail(400, 'Phone number is not valid');",
    }),
    r('Register customer 3 (phone only)', 'POST', '/api/customer/auth/register', {
      body: { phone: '+62 {{customer3Phone}}', password: '{{customerPassword}}', fullName: 'QA Customer3 {{runId}}' },
      test: String.raw`
var d = U.ok();
pm.test('phone-only account has no email', function () { pm.expect(d.email == null).to.eql(true); });
pm.test('token omits the email claim', function () { pm.expect(U.claims(d.token)).to.not.have.property('email'); });
U.set('customer3Token', d.token);
`,
    }),
    r('Login: wrong password -> 401', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customerEmail}}', password: 'wrong-{{runId}}' }, test: "U.fail(401, 'Invalid email/phone or password');",
    }),
    r('Login: unknown email -> 401 (same message)', 'POST', '/api/customer/auth/login', {
      body: { identifier: 'nobody.{{customerEmail}}', password: 'whatever-123' }, test: "U.fail(401, 'Invalid email/phone or password');",
    }),
    r('Login: blank identifier -> 400', 'POST', '/api/customer/auth/login', {
      body: { identifier: '', password: 'x' }, test: "U.badRequest('Email atau nomor HP harus diisi');",
    }),
    r('Login: legacy "email" key still accepted', 'POST', '/api/customer/auth/login', {
      body: { email: '{{customerEmail}}', password: '{{customerPassword}}' }, test: "U.ok();",
    }),
    r('Login by phone (any format) - customer 1', 'POST', '/api/customer/auth/login', {
      body: { identifier: '+62 {{customerPhone}}', password: '{{customerPassword}}' },
      test: "var d = U.ok(); U.fields(d, { email: U.get('customerEmail') });",
    }),
    r('Login by phone - customer 3 (phone only)', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customer3Phone}}', password: '{{customerPassword}}' },
      test: "var d = U.ok(); U.set('customer3Token', d.token);",
    }),
    r('Login customer 1', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customerEmail}}', password: '{{customerPassword}}' },
      test: "var d = U.ok(); U.fields(d, { linked: false }); U.set('customerToken', d.token); U.set('customerRefreshToken', U.cookieValue(U.cookie('customer_refresh_token')));",
    }),
    r('Customer /me', 'GET', '/api/customer/auth/me', {
      as: 'customer', test: "var d = U.ok(); pm.test('accountId is a number', function () { pm.expect(d.accountId).to.be.a('number'); }); U.fields(d, { linked: false }); U.set('customerAccountId', String(d.accountId));",
    }),
    r('Customer /me without token -> 401', 'GET', '/api/customer/auth/me', { test: 'U.unauthorized();' }),
    r('Customer token on a staff endpoint -> 403', 'GET', '/api/pazaauto/pelanggan', { as: 'customer', test: 'U.forbidden();' }),
    r('Customer token on staff /api/auth/me', 'GET', '/api/auth/me', {
      as: 'customer', test: "pm.test('not treated as a staff session', function () { pm.expect([401, 403]).to.include(pm.response.code); });",
    }),
  ]),
  f('Unlinked account', [
    ...[
      ['/api/customer/vehicles', 'vehicles'],
      ['/api/customer/bookings', 'bookings'],
      ['/api/customer/spk', 'work orders'],
    ].map(([p, what]) => r(`Unlinked: ${what} -> 400 (link first)`, 'GET', p, {
      as: 'customer', test: "U.fail(400, 'Link your account to a customer record first');",
    })),
    r('Profile of an unlinked account', 'GET', '/api/customer/profile', {
      as: 'customer', test: "var d = U.ok(); U.fields(d, { email: U.get('customerEmail'), linked: false });",
    }),
    r('Vehicle merk list (public)', 'GET', '/api/customer/vehicles/merk', { test: 'U.isArray(U.ok());' }),
    r('Vehicle jenis list (public)', 'GET', '/api/customer/vehicles/jenis', { test: 'U.isArray(U.ok());' }),
    r('Vehicle jenis by merk (public)', 'GET', '/api/customer/vehicles/jenis', {
      query: { merk: 'QAMERK{{runId}}' }, test: "var d = U.isArray(U.ok()); U.contains(d, function (j) { return j === 'QAJENIS' + U.get('runId'); }, 'fixture jenis listed');",
    }),
    r('Check nopol: blank -> 400', 'GET', '/api/customer/vehicles/check-nopol', { as: 'customer', test: "U.fail(400, 'nopol is required');" }),
    r('Check nopol: new plate is available', 'GET', '/api/customer/vehicles/check-nopol', {
      as: 'customer', query: { nopol: '{{nopolC}}' }, test: "U.fields(U.ok(), { nopol: U.get('nopolC'), status: 'AVAILABLE', canRegister: true });",
    }),
    r('Check nopol: normalises spaces and case', 'GET', '/api/customer/vehicles/check-nopol', {
      as: 'customer', pre: "U.set('nopolCSpaced', U.get('nopolC').toLowerCase().replace(/(..)/, '$1 '));",
      query: { nopol: '{{nopolCSpaced}}' }, test: "U.fields(U.ok(), { nopol: U.get('nopolC') });",
    }),
    r('Claim before linking -> 400', 'POST', '/api/customer/vehicles/claim', {
      as: 'customer', body: { nopol: '{{nopolA}}' }, test: "U.fail(400, 'Link your account to a customer record first');",
    }),
    r('Link search by unknown nopol -> 404', 'GET', '/api/customer/link/search-by-nopol/{{nopolUnknown}}', {
      as: 'customer', test: "U.fail(404, 'No unlinked vehicle record found');",
    }),
    r('Link search by contact (masked results)', 'GET', '/api/customer/link/search-by-contact', {
      as: 'customer', query: { phone: '0814{{runNum}}1' },
      test: String.raw`
var d = U.isArray(U.ok());
pm.test('candidates are masked', function () { d.forEach(function (c) { if (c.maskedPhone) pm.expect(c.maskedPhone).to.include('*'); }); });
`,
    }),
    r('Customer 2 skips linking (placeholder record)', 'POST', '/api/customer/link/skip', { as: 'customer2', test: 'U.ok();' }),
    r('Customer 2 skips again -> 400 (already linked)', 'POST', '/api/customer/link/skip', {
      as: 'customer2', test: "U.fail(400, 'already linked');",
    }),
    r('Customer 2 logs in again (token now carries pelangganId)', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customer2Email}}', password: '{{customerPassword}}' },
      test: "var d = U.ok(); U.fields(d, { linked: true }); U.set('customer2Token', d.token);",
    }),
  ]),
  f('Vehicles & claims', [
    r('Register a new vehicle (provisions the customer record)', 'POST', '/api/customer/vehicles', {
      as: 'customer', body: { nopol: '{{nopolC}}', merk: 'QAMERK{{runId}}', jenis: 'QAJENIS{{runId}}' },
      test: String.raw`
var d = U.ok();
U.fields(d, { status: 'REGISTERED' });
pm.test('relinked: the token must be refreshed', function () { pm.expect(d.relinked).to.eql(true); });
U.contains(d.vehicles, function (v) { return v.nopol === U.get('nopolC'); }, 'vehicle is listed');
`,
    }),
    r('Refresh the customer token (picks up pelangganId)', 'POST', '/api/customer/auth/refresh', {
      // The jar now holds customer 2's cookie, so customer 1's token is sent in the body (the fallback path)
      body: { refreshToken: '{{customerRefreshToken}}' },
      pre: "U.forgetCookie('/api/customer/auth/refresh', 'customer_refresh_token');",
      test: String.raw`
var d = U.ok();
U.fields(d, { linked: true });
U.absent(d, ['refreshToken'], 'refresh body');
pm.test('rotated refresh cookie', function () { pm.expect(U.cookie('customer_refresh_token')).to.be.a('string'); });
U.set('customerToken', d.token);
`,
    }),
    r('Customer refresh without a token -> 401', 'POST', '/api/customer/auth/refresh', {
      body: {}, pre: "U.forgetCookie('/api/customer/auth/refresh', 'customer_refresh_token');",
      test: "U.fail(401, 'Invalid or expired refresh token');",
    }),
    r('Login customer 1 again (restores the refresh cookie)', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customerEmail}}', password: '{{customerPassword}}' },
      test: "var d = U.ok(); U.fields(d, { linked: true }); U.set('customerToken', d.token);",
    }),
    r('/me is linked now', 'GET', '/api/customer/auth/me', { as: 'customer', test: "U.fields(U.ok(), { linked: true });" }),
    r('My vehicles', 'GET', '/api/customer/vehicles', {
      as: 'customer', test: "var d = U.isArray(U.ok()); U.contains(d, function (v) { return v.nopol === U.get('nopolC'); }, 'own plate listed');",
    }),
    r('Check nopol: own plate', 'GET', '/api/customer/vehicles/check-nopol', {
      as: 'customer', query: { nopol: '{{nopolC}}' }, test: "U.fields(U.ok(), { status: 'OWNED_BY_SELF', canRegister: false });",
    }),
    r("Check nopol: someone else's plate", 'GET', '/api/customer/vehicles/check-nopol', {
      as: 'customer', query: { nopol: '{{nopolA}}' }, test: "U.fields(U.ok(), { status: 'OWNED_BY_OTHER', canRegister: false });",
    }),
    r('Check nopol: plate on an unclaimed record, caller without vehicles -> ON_RECORD', 'GET', '/api/customer/vehicles/check-nopol', {
      as: 'customer2', query: { nopol: '{{nopolA}}' },
      test: String.raw`
var d = U.ok();
U.fields(d, { status: 'ON_RECORD', canRegister: true, merk: 'QAMERK' + U.get('runId'), jenis: 'QAJENIS' + U.get('runId') });
pm.test('owner name is masked', function () {
  pm.expect(d.maskedOwnerName).to.match(/^Q\*+/);
  pm.expect(d.maskedOwnerName).to.not.contain('Owner');
});
`,
    }),
    r('Register own plate again -> 400', 'POST', '/api/customer/vehicles', {
      as: 'customer', body: { nopol: '{{nopolC}}' }, test: "U.fail(400, 'already registered to your account');",
    }),
    r("Register someone else's plate -> 400 (claim instead)", 'POST', '/api/customer/vehicles', {
      as: 'customer', body: { nopol: '{{nopolA}}' }, test: "U.fail(400, 'file a claim');",
    }),
    r('Register: missing nopol -> 400', 'POST', '/api/customer/vehicles', { as: 'customer', body: {}, test: "U.badRequest('nopol is required');" }),
    r('Update own vehicle', 'PUT', '/api/customer/vehicles/{{nopolC}}', {
      as: 'customer', body: { nopol: '{{nopolC}}', merk: 'QAMERK{{runId}}', jenis: 'QAJENIS{{runId}}X' },
      test: "var d = U.isArray(U.ok()); U.contains(d, function (v) { return v.nopol === U.get('nopolC') && v.jenis === 'QAJENIS' + U.get('runId') + 'X'; }, 'jenis updated');",
    }),
    r("Update someone else's vehicle -> 400", 'PUT', '/api/customer/vehicles/{{nopolA}}', {
      as: 'customer', body: { nopol: '{{nopolA}}', jenis: 'HACK' }, test: "U.fail(400, 'does not belong to this customer');",
    }),
    r("Delete someone else's vehicle -> 400", 'DELETE', '/api/customer/vehicles/{{nopolA}}', {
      as: 'customer', test: "U.fail(400, 'does not belong to this customer');",
    }),
    r('Claim: blank nopol -> 400', 'POST', '/api/customer/vehicles/claim', { as: 'customer', body: { nopol: '' }, test: "U.badRequest('nopol is required');" }),
    r('Claim: unregistered plate -> 400', 'POST', '/api/customer/vehicles/claim', {
      as: 'customer', body: { nopol: '{{nopolUnknown}}' }, test: "U.fail(400, 'is not registered to any customer');",
    }),
    r('Claim: own plate -> 400', 'POST', '/api/customer/vehicles/claim', {
      as: 'customer', body: { nopol: '{{nopolC}}' }, test: "U.fail(400, 'already registered to your account');",
    }),
    r("Claim pelanggan A's plate", 'POST', '/api/customer/vehicles/claim', {
      as: 'customer', body: { nopol: '{{nopolA}}' },
      test: String.raw`
var d = U.ok();
pm.test('pending approval or linked (app.customer.vehicle-claim.mode)', function () { pm.expect(['PENDING_APPROVAL', 'LINKED']).to.include(d.status); });
U.set('claimMode', d.status);
`,
    }),
    ...[
      ['Second pending claim -> 400', 'POST', '/api/customer/vehicles/claim', 'customer', { nopol: '{{nopolB}}' }, "U.fail(400, 'already have a pending vehicle claim');"],
      ['Another customer claims the same plate -> 400', 'POST', '/api/customer/vehicles/claim', 'customer2', { nopol: '{{nopolA}}' }, "U.fail(400, 'already awaiting approval');"],
      ['Staff: pending claims include ours', 'GET', '/api/pazaauto/vehicle-claim-requests', 'admin', undefined, String.raw`
var d = U.isArray(U.ok());
var mine = d.filter(function (c) { return c.nopol === U.get('nopolA') && c.customerAccountId === U.num('customerAccountId'); })[0];
pm.test('claim is queued with the current owner', function () { pm.expect(mine, 'claim').to.be.an('object'); pm.expect(mine.status).to.eql('PENDING'); pm.expect(mine.currentOwnerPelangganId).to.eql(U.num('pelangganAId')); });
if (mine) { U.set('claimId', String(mine.id)); }
`],
      ['Staff: approve the claim', 'POST', '/api/pazaauto/vehicle-claim-requests/{{claimId}}/approve', 'admin', undefined, 'U.ok();'],
      ['Staff: approve twice -> 400', 'POST', '/api/pazaauto/vehicle-claim-requests/{{claimId}}/approve', 'admin', undefined, "U.fail(400, 'already been reviewed');"],
      ['Staff: reject a reviewed claim -> 400', 'POST', '/api/pazaauto/vehicle-claim-requests/{{claimId}}/reject', 'admin', { note: 'late' }, "U.fail(400, 'already been reviewed');"],
      ['Staff: approved claims list', 'GET', '/api/pazaauto/vehicle-claim-requests', 'admin', undefined, "var d = U.isArray(U.ok()); U.contains(d, function (c) { return c.id === U.num('claimId'); }, 'claim is APPROVED');"],
    ].map(([name, m, p, as, body, test]) => r(name, m, p, {
      as, body, test, query: name === 'Staff: approved claims list' ? { status: 'APPROVED' } : undefined,
      pre: "U.skipUnless(U.get('claimMode') === 'PENDING_APPROVAL', 'claim mode is not APPROVAL');",
    })),
    r('Claimed plate now belongs to customer 1', 'GET', '/api/customer/vehicles', {
      as: 'customer', test: "var d = U.isArray(U.ok()); U.contains(d, function (v) { return v.nopol === U.get('nopolA'); }, 'nopolA transferred');",
    }),
    r('Staff: approve unknown claim -> 400', 'POST', `/api/pazaauto/vehicle-claim-requests/${MISSING_ID}/approve`, {
      as: 'admin', test: "U.fail(400, 'Vehicle claim request not found');",
    }),
    r('Staff: reject unknown claim -> 400', 'POST', `/api/pazaauto/vehicle-claim-requests/${MISSING_ID}/reject`, {
      as: 'admin', body: { note: 'x' }, test: "U.fail(400, 'Vehicle claim request not found');",
    }),
    r('Staff: link request queue', 'GET', '/api/pazaauto/customer-link-requests', { as: 'admin', test: 'U.isArray(U.ok());' }),
    r('Staff: approve unknown link request -> 400', 'POST', `/api/pazaauto/customer-link-requests/${MISSING_ID}/approve`, {
      as: 'admin', test: 'U.fail(400);',
    }),
    r('Staff: reject unknown link request -> 400', 'POST', `/api/pazaauto/customer-link-requests/${MISSING_ID}/reject`, {
      as: 'admin', body: { note: 'x' }, test: 'U.fail(400);',
    }),
  ]),
  f('Profile', [
    r('Linked profile', 'GET', '/api/customer/profile', {
      as: 'customer', test: "var d = U.ok(); U.fields(d, { linked: true }); pm.test('has a pelangganId', function () { pm.expect(d.pelangganId).to.be.a('number'); }); U.set('customerPelangganId', String(d.pelangganId));",
    }),
    r('Update profile: blank name -> 400', 'PUT', '/api/customer/profile', { as: 'customer', body: { namaPelanggan: '' }, test: "U.badRequest('Name is required');" }),
    r('Update profile', 'PUT', '/api/customer/profile', {
      as: 'customer', body: { namaPelanggan: 'QA Customer {{runId}} upd', alamat: 'Jl. Portal 1', kota: 'Depok', kodePos: '16411' },
      test: "var d = U.ok(); U.fields(d, { namaPelanggan: 'QA Customer ' + U.get('runId') + ' upd', kota: 'Depok' });",
    }),
    r('Staff sees the customer edit on the pelanggan record', 'GET', '/api/pazaauto/pelanggan/{{customerPelangganId}}', {
      as: 'admin', test: "U.fields(U.ok(), { namaPelanggan: 'QA Customer ' + U.get('runId') + ' upd' });",
    }),
  ]),
  f('Bookings & work orders', [
    r('Booking: missing km -> 400', 'POST', '/api/customer/bookings', {
      as: 'customer', body: { nopol: '{{nopolC}}', requestedDateTime: '2026-12-01T09:00:00' }, test: "U.badRequest('KM / Odometer harus diisi');",
    }),
    r('Booking: missing date -> 400', 'POST', '/api/customer/bookings', {
      as: 'customer', body: { nopol: '{{nopolC}}', km: 1000 }, test: "U.badRequest('Tanggal booking harus diisi');",
    }),
    ...['1', '2', '3'].map((n) => r(`Create booking ${n}`, 'POST', '/api/customer/bookings', {
      as: 'customer',
      body: { nopol: '{{nopolC}}', km: 1000 + Number(n), requestedDateTime: `2026-12-0${n}T09:00:00`, serviceType: 'SERVIS', description: `regression booking ${n}` },
      test: `var d = U.ok(); U.fields(d, { status: 'REQUESTED', nopol: U.get('nopolC'), km: ${1000 + Number(n)} }); U.set('booking${n}Id', String(d.id));`,
    })),
    r('My bookings', 'GET', '/api/customer/bookings', {
      as: 'customer', test: "var d = U.isArray(U.ok()); pm.test('three bookings', function () { pm.expect(d.filter(function (b) { return [U.num('booking1Id'), U.num('booking2Id'), U.num('booking3Id')].indexOf(b.id) >= 0; }).length).to.eql(3); });",
    }),
    r('Get my booking', 'GET', '/api/customer/bookings/{{booking1Id}}', { as: 'customer', test: "U.fields(U.ok(), { id: U.num('booking1Id') });" }),
    r("Another customer's booking -> 404", 'GET', '/api/customer/bookings/{{booking1Id}}', { as: 'customer2', test: "U.fail(404, 'Not found');" }),
    r('Unknown booking -> 404', 'GET', `/api/customer/bookings/${MISSING_ID}`, { as: 'customer', test: "U.fail(404, 'Not found');" }),
    r("Cancel another customer's booking -> 400", 'POST', '/api/customer/bookings/{{booking1Id}}/cancel', { as: 'customer2', test: "U.fail(400, 'Booking not found');" }),
    r('Cancel booking 1', 'POST', '/api/customer/bookings/{{booking1Id}}/cancel', { as: 'customer', test: 'U.ok();' }),
    r('Cancel booking 1 again -> 400', 'POST', '/api/customer/bookings/{{booking1Id}}/cancel', {
      as: 'customer', test: "U.fail(400, 'Only a requested booking can be cancelled');",
    }),
    r('Staff: requested bookings include booking 2', 'GET', '/api/pazaauto/customer-bookings', {
      as: 'admin', query: { status: 'REQUESTED' },
      test: "var d = U.isArray(U.ok()); U.contains(d, function (b) { return b.id === U.num('booking2Id') && b.customerEmail === U.get('customerEmail'); }, 'booking 2 queued with the customer email');",
    }),
    r('Staff: convert a non-confirmed booking -> 400', 'POST', '/api/pazaauto/customer-bookings/{{booking2Id}}/convert-to-spk', {
      as: 'admin', body: { km: 1002 }, test: "U.fail(400, 'Only a confirmed booking can be converted');",
    }),
    r('Staff: confirm booking 2', 'POST', '/api/pazaauto/customer-bookings/{{booking2Id}}/confirm', { as: 'admin', body: { note: 'see you' }, test: 'U.ok();' }),
    r('Staff: confirm twice -> 400', 'POST', '/api/pazaauto/customer-bookings/{{booking2Id}}/confirm', {
      as: 'admin', body: {}, test: "U.fail(400, 'not in the expected status');",
    }),
    r('Staff: convert booking 2 to a work order', 'POST', '/api/pazaauto/customer-bookings/{{booking2Id}}/convert-to-spk', {
      as: 'admin', body: { km: 1002, mekanikList: [] },
      test: "var d = U.ok(); pm.test('returns the spkId', function () { pm.expect(d.spkId).to.be.a('number'); }); U.set('bookingSpkId', String(d.spkId));",
    }),
    r('Staff: convert twice -> 400', 'POST', '/api/pazaauto/customer-bookings/{{booking2Id}}/convert-to-spk', {
      as: 'admin', body: { km: 1002 }, test: "U.fail(400, 'Only a confirmed booking can be converted');",
    }),
    r('Staff: reject booking 3 with a note', 'POST', '/api/pazaauto/customer-bookings/{{booking3Id}}/reject', { as: 'admin', body: { note: 'fully booked' }, test: 'U.ok();' }),
    r('Staff: confirm a cancelled booking -> 400', 'POST', '/api/pazaauto/customer-bookings/{{booking1Id}}/confirm', {
      as: 'admin', test: "U.fail(400, 'not in the expected status');",
    }),
    r('Staff: confirm unknown booking -> 400', 'POST', `/api/pazaauto/customer-bookings/${MISSING_ID}/confirm`, {
      as: 'admin', test: "U.fail(400, 'Booking not found');",
    }),
    r('Customer sees the outcomes', 'GET', '/api/customer/bookings', {
      as: 'customer',
      test: String.raw`
var d = U.isArray(U.ok());
var by = {}; d.forEach(function (b) { by[b.id] = b; });
pm.test('booking 1 CANCELLED', function () { pm.expect(by[U.num('booking1Id')].status).to.eql('CANCELLED'); });
pm.test('booking 2 CONVERTED with its SPK', function () { pm.expect(by[U.num('booking2Id')].status).to.eql('CONVERTED'); pm.expect(by[U.num('booking2Id')].resultingSpkId).to.eql(U.num('bookingSpkId')); pm.expect(by[U.num('booking2Id')].staffNote).to.eql('see you'); });
pm.test('booking 3 REJECTED with the note', function () { pm.expect(by[U.num('booking3Id')].status).to.eql('REJECTED'); pm.expect(by[U.num('booking3Id')].staffNote).to.eql('fully booked'); });
`,
    }),
    r('Customer work orders include the converted SPK', 'GET', '/api/customer/spk', {
      as: 'customer', test: "var d = U.isArray(U.ok()); U.contains(d, function (s) { return s.id === U.num('bookingSpkId'); }, 'converted SPK listed');",
    }),
    r('Customer work order: status only, no pricing', 'GET', '/api/customer/spk/{{bookingSpkId}}', {
      as: 'customer',
      test: String.raw`
var d = U.ok();
U.fields(d, { id: U.num('bookingSpkId'), nopol: U.get('nopolC'), statusSpk: 'OPEN' });
U.absent(d, ['grandTotal', 'details', 'mekanikList', 'pelangganId', 'namaKaryawan'], 'customer SPK view');
U.set('bookingNoSpk', d.noSpk);
`,
    }),
    r("Another customer's work order -> 404", 'GET', '/api/customer/spk/{{bookingSpkId}}', { as: 'customer2', test: "U.fail(404, 'Not found');" }),
    r('Invoice before any sale -> 404', 'GET', '/api/customer/spk/{{bookingSpkId}}/invoice', { as: 'customer', test: "U.fail(404, 'Not found');" }),
    r('Unknown work order -> 404', 'GET', `/api/customer/spk/${MISSING_ID}`, { as: 'customer', test: "U.fail(404, 'Not found');" }),
    r('Staff: SPK created from the booking', 'GET', '/api/pazaauto/spk/{{bookingSpkId}}', {
      as: 'admin', test: "U.fields(U.ok(), { nopol: U.get('nopolC'), statusSpk: 'OPEN', km: 1002, keterangan: 'regression booking 2' });",
    }),
    r('Staff: delete the booking SPK (cleanup)', 'DELETE', '/api/pazaauto/spk/delete-by-no-spk/{{bookingNoSpk}}', { as: 'admin', test: 'U.ok();' }),
  ]),
  f('Password & session', [
    r('Change password: wrong current -> 401', 'POST', '/api/customer/auth/change-password', {
      as: 'customer', body: { currentPassword: 'wrong-{{runId}}', newPassword: '{{customerPasswordNew}}' }, test: "U.fail(401, 'Current password is incorrect');",
    }),
    r('Change password: same as current -> 400', 'POST', '/api/customer/auth/change-password', {
      as: 'customer', body: { currentPassword: '{{customerPassword}}', newPassword: '{{customerPassword}}' }, test: "U.fail(400, 'must be different');",
    }),
    r('Change password: weak -> 400', 'POST', '/api/customer/auth/change-password', {
      as: 'customer', body: { currentPassword: '{{customerPassword}}', newPassword: 'short' }, test: "U.fail(400, 'at least 8 characters');",
    }),
    r('Change password', 'POST', '/api/customer/auth/change-password', {
      as: 'customer', body: { currentPassword: '{{customerPassword}}', newPassword: '{{customerPasswordNew}}' },
      test: "var d = U.ok(); U.fields(d, { linked: true }); U.set('customerToken', d.token);",
    }),
    r('Old password rejected', 'POST', '/api/customer/auth/login', { body: { identifier: '{{customerEmail}}', password: '{{customerPassword}}' }, test: 'U.fail(401);' }),
    r('New password accepted', 'POST', '/api/customer/auth/login', {
      body: { identifier: '{{customerEmail}}', password: '{{customerPasswordNew}}' }, test: "U.set('customerToken', U.ok().token);",
    }),
    r('Delete own vehicle', 'DELETE', '/api/customer/vehicles/{{nopolC}}', {
      as: 'customer', test: "var d = U.isArray(U.ok()); pm.test('nopolC removed', function () { pm.expect(d.some(function (v) { return v.nopol === U.get('nopolC'); })).to.eql(false); });",
    }),
    r('Logout customer 1', 'POST', '/api/customer/auth/logout', {
      as: 'customer', test: "U.ok(); pm.test('refresh cookie cleared', function () { pm.expect(U.cookie('customer_refresh_token')).to.match(/Max-Age=0/i); });",
    }),
    r('Refresh after logout -> 401', 'POST', '/api/customer/auth/refresh', {
      pre: "U.forgetCookie('/api/customer/auth/refresh', 'customer_refresh_token');", test: 'U.unauthorized();',
    }),
    r('Logout without a customer token -> 401', 'POST', '/api/customer/auth/logout', { test: 'U.unauthorized();' }),
    r('Logout customer 2', 'POST', '/api/customer/auth/logout', { as: 'customer2', test: 'U.ok();' }),
    r('Logout customer 3', 'POST', '/api/customer/auth/logout', { as: 'customer3', test: 'U.ok();' }),
  ]),
], 'Self-service portal end to end: gate, registration, linking, vehicles, claims (+ staff approval), profile, bookings (+ staff review), work orders, password and session.');

// ---------------------------------------------------------------------------------------------------
// 99 Teardown
// ---------------------------------------------------------------------------------------------------
const teardown = f('99 Teardown', [
  r('Delete fixture jasa', 'DELETE', '/api/pazaauto/jasa/{{fxJasaId}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete pelanggan A', 'DELETE', '/api/pazaauto/pelanggan/{{pelangganAId}}', { as: 'admin', test: 'U.ok();' }),
  r('Delete pelanggan B', 'DELETE', '/api/pazaauto/pelanggan/{{pelangganBId}}', { as: 'admin', test: 'U.ok();' }),
  r('Restore the customer-access switch', 'PUT', '/api/settings/customer-access', {
    as: 'owner', body: { enabled: '#{{customerAccessOriginal}}' },
    pre: "U.skipUnless(U.get('customerAccessOriginal') === 'true' || U.get('customerAccessOriginal') === 'false', 'original value unknown');",
    test: "U.fields(U.ok(), { enabled: U.get('customerAccessOriginal') === 'true' });",
  }),
  ...['admin', 'karyawan', 'owner'].map((who) => r(`Logout ${who}`, 'POST', '/api/auth/logout', { as: who, test: 'U.ok();' })),
  r('Logged-out session cannot refresh', 'POST', '/api/auth/refresh', {
    pre: "U.forgetCookie('/api/auth/refresh', 'refresh_token');", test: 'U.unauthorized();',
  }),
], 'Removes fixtures, restores the portal switch, ends all staff sessions.');

// ---------------------------------------------------------------------------------------------------
const collection = {
  info: {
    name: 'pasa-auto API regression',
    description:
      'Generated by src/test/postman/build-collection.mjs - edit the generator, not this file.\n\n'
      + 'Run the whole collection in order (folders depend on fixtures created earlier). See src/test/postman/README.md.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  event: [
    { listen: 'prerequest', script: { type: 'text/javascript', exec: lines(UTILS) } },
    // makeU is defined in both phases so neither depends on the other having run
    { listen: 'test', script: { type: 'text/javascript', exec: lines(UTILS + GLOBAL_TESTS) } },
  ],
  variable: [],
  item: [setup, authStaff, accessControl, masterData, ownership, spkSales, purchasing, absensi, admin, customer, teardown],
};

writeFileSync(OUT, JSON.stringify(collection, null, 2) + '\n');

const count = (items) => items.reduce((n, it) => n + (it.item ? count(it.item) : 1), 0);
console.log(`Wrote ${OUT} (${count(collection.item)} requests)`);
