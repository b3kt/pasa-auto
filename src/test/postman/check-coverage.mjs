#!/usr/bin/env node
/*
 * Lists OpenAPI operations that the regression collection never calls, so a new endpoint can't slip
 * in untested. Exits 1 when something is uncovered.
 *
 *   node src/test/postman/check-coverage.mjs http://localhost:8080/openapi?format=json
 *   node src/test/postman/check-coverage.mjs path/to/openapi.json
 */
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const source = process.argv[2] || 'http://localhost:8080/openapi?format=json';
const spec = /^https?:/.test(source)
  ? await (await fetch(source)).json()
  : JSON.parse(readFileSync(source, 'utf8'));
const collection = JSON.parse(readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'pasa-auto-regression.postman_collection.json'), 'utf8'));

const METHODS = ['get', 'post', 'put', 'delete', 'patch'];
const ops = Object.entries(spec.paths).flatMap(([path, item]) => Object.keys(item)
  .filter((m) => METHODS.includes(m))
  .map((m) => ({ method: m.toUpperCase(), path, re: new RegExp('^' + path.replace(/\{[^}]+\}/g, '[^/]+') + '$') })));

const requests = [];
(function walk(items) {
  for (const it of items) {
    if (it.item) walk(it.item);
    else requests.push({ method: it.request.method, path: it.request.url.replace('{{baseUrl}}', '').split('?')[0] });
  }
})(collection.item);

const missing = ops.filter((o) => !requests.some((r) => r.method === o.method && o.re.test(r.path)));
console.log(`${ops.length - missing.length}/${ops.length} operations covered by ${requests.length} requests`);
missing.forEach((o) => console.log(`  NOT COVERED  ${o.method} ${o.path}`));
process.exit(missing.length ? 1 : 0);
