import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { AccountVault } from './accountVault.js';

test('vault encrypts sensitive account data and supports account switching', async () => {
  const dir = await mkdtemp(path.join(os.tmpdir(), 'mega-chatgpt-vault-'));
  const vaultPath = path.join(dir, 'accounts.vault.json');
  process.env.MEGA_ACCOUNT_VAULT_KEY_B64 = randomBytes(32).toString('base64');

  try {
    const vault = new AccountVault(vaultPath);
    const first = await vault.createAccount({
      displayName: 'MEGA personale',
      email: 'person@example.com',
      session: 'secret-session-one',
    });
    const second = await vault.createAccount({
      displayName: 'MEGA lavoro',
      email: 'work@example.com',
      session: 'secret-session-two',
    });

    assert.match(first, /^acc_/);
    assert.match(second, /^acc_/);
    assert.deepEqual(new Set(await vault.listAccountRefs()), new Set([first, second]));
    assert.equal(await vault.getCurrentAccountRef(), first);

    const rawVault = await readFile(vaultPath, 'utf8');
    assert.equal(rawVault.includes('person@example.com'), false);
    assert.equal(rawVault.includes('work@example.com'), false);
    assert.equal(rawVault.includes('secret-session-one'), false);
    assert.equal(rawVault.includes('secret-session-two'), false);

    await vault.setCurrentAccount(second);
    assert.equal(await vault.getCurrentAccountRef(), second);

    const decrypted = await vault.getAccount(second);
    assert.equal(decrypted?.displayName, 'MEGA lavoro');
    assert.equal(decrypted?.email, 'work@example.com');
    assert.equal(decrypted?.session, 'secret-session-two');
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});
