import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { createCsrfToken, hashToken } from './csrf.util';

describe('csrf util', () => {
  it('creates unique tokens', () => {
    const a = createCsrfToken();
    const b = createCsrfToken();
    assert.notEqual(a, b);
    assert.equal(a.length, 64);
  });

  it('hashes deterministically', () => {
    assert.equal(hashToken('abc'), hashToken('abc'));
    assert.notEqual(hashToken('abc'), hashToken('abd'));
  });
});
