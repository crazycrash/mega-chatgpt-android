import { createCipheriv, createDecipheriv, randomBytes, randomUUID } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

export interface MegaAccountSecret {
  accountRef: string;
  displayName: string;
  email?: string;
  session: string;
}

interface EncryptedRecord {
  iv: string;
  tag: string;
  ciphertext: string;
}

interface VaultDocument {
  version: 1;
  currentAccountRef?: string;
  accounts: Record<string, EncryptedRecord>;
}

const EMPTY_VAULT: VaultDocument = {
  version: 1,
  accounts: {},
};

export class AccountVault {
  private readonly filePath: string;

  constructor(filePath = process.env.MEGA_ACCOUNT_VAULT_FILE ?? './data/accounts.vault.json') {
    this.filePath = filePath;
  }

  async createAccount(input: Omit<MegaAccountSecret, 'accountRef'>): Promise<string> {
    const accountRef = `acc_${randomUUID()}`;
    const doc = await this.loadDocument();
    doc.accounts[accountRef] = this.encrypt({ ...input, accountRef });
    if (!doc.currentAccountRef) doc.currentAccountRef = accountRef;
    await this.saveDocument(doc);
    return accountRef;
  }

  async upsertAccount(accountRef: string, input: Omit<MegaAccountSecret, 'accountRef'>): Promise<void> {
    if (!accountRef.startsWith('acc_')) throw new Error('Invalid account reference');
    const doc = await this.loadDocument();
    doc.accounts[accountRef] = this.encrypt({ ...input, accountRef });
    if (!doc.currentAccountRef) doc.currentAccountRef = accountRef;
    await this.saveDocument(doc);
  }

  async listAccountRefs(): Promise<string[]> {
    const doc = await this.loadDocument();
    return Object.keys(doc.accounts);
  }

  async getCurrentAccountRef(): Promise<string | undefined> {
    return (await this.loadDocument()).currentAccountRef;
  }

  async setCurrentAccount(accountRef: string): Promise<void> {
    const doc = await this.loadDocument();
    if (!doc.accounts[accountRef]) throw new Error('Unknown account reference');
    // Decrypt once before switching. This verifies the record and authentication tag.
    this.decrypt(doc.accounts[accountRef]);
    doc.currentAccountRef = accountRef;
    await this.saveDocument(doc);
  }

  async getAccount(accountRef: string): Promise<MegaAccountSecret | undefined> {
    const doc = await this.loadDocument();
    const record = doc.accounts[accountRef];
    return record ? this.decrypt(record) : undefined;
  }

  async deleteAccount(accountRef: string): Promise<void> {
    const doc = await this.loadDocument();
    delete doc.accounts[accountRef];
    if (doc.currentAccountRef === accountRef) {
      doc.currentAccountRef = Object.keys(doc.accounts)[0];
    }
    await this.saveDocument(doc);
  }

  private getKey(): Buffer {
    const encoded = process.env.MEGA_ACCOUNT_VAULT_KEY_B64;
    if (!encoded) {
      throw new Error('MEGA_ACCOUNT_VAULT_KEY_B64 is required. Generate a random 32-byte key and store it only in deployment secrets.');
    }
    const key = Buffer.from(encoded, 'base64');
    if (key.length !== 32) throw new Error('MEGA_ACCOUNT_VAULT_KEY_B64 must decode to exactly 32 bytes');
    return key;
  }

  private encrypt(value: MegaAccountSecret): EncryptedRecord {
    const iv = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', this.getKey(), iv);
    const plaintext = Buffer.from(JSON.stringify(value), 'utf8');
    const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const tag = cipher.getAuthTag();
    return {
      iv: iv.toString('base64'),
      tag: tag.toString('base64'),
      ciphertext: ciphertext.toString('base64'),
    };
  }

  private decrypt(record: EncryptedRecord): MegaAccountSecret {
    const decipher = createDecipheriv('aes-256-gcm', this.getKey(), Buffer.from(record.iv, 'base64'));
    decipher.setAuthTag(Buffer.from(record.tag, 'base64'));
    const plaintext = Buffer.concat([
      decipher.update(Buffer.from(record.ciphertext, 'base64')),
      decipher.final(),
    ]);
    return JSON.parse(plaintext.toString('utf8')) as MegaAccountSecret;
  }

  private async loadDocument(): Promise<VaultDocument> {
    try {
      const raw = await readFile(this.filePath, 'utf8');
      const parsed = JSON.parse(raw) as VaultDocument;
      if (parsed.version !== 1 || typeof parsed.accounts !== 'object') throw new Error('Unsupported vault format');
      return parsed;
    } catch (error) {
      const code = (error as NodeJS.ErrnoException).code;
      if (code === 'ENOENT') return structuredClone(EMPTY_VAULT);
      throw error;
    }
  }

  private async saveDocument(doc: VaultDocument): Promise<void> {
    await mkdir(path.dirname(this.filePath), { recursive: true });
    await writeFile(this.filePath, `${JSON.stringify(doc, null, 2)}\n`, { encoding: 'utf8', mode: 0o600 });
  }
}

export function maskEmail(email?: string): string | undefined {
  if (!email) return undefined;
  const [local, domain] = email.split('@');
  if (!domain) return '***';
  return `${local.slice(0, 1)}***@${domain}`;
}
