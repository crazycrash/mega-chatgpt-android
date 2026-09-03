import type { MegaAccountSecret } from './accountVault.js';

export interface MegaConnectionStatus {
  connected: boolean;
  accountName?: string;
  email?: string;
  rootItems?: number;
  exportedLinks?: number;
  reason?: string;
}

export interface MegaSearchHit {
  nodeRef: string;
  name: string;
  type: 'file' | 'folder' | 'link';
  path?: string;
}

export interface MegaGateway {
  connectionStatus(account: MegaAccountSecret): Promise<MegaConnectionStatus>;
  search(account: MegaAccountSecret, query: string): Promise<MegaSearchHit[]>;
}

/**
 * Temporary adapter used until the official MEGA SDK service is wired in.
 * It intentionally never reports a successful MEGA connection.
 */
export class PendingMegaSdkGateway implements MegaGateway {
  async connectionStatus(_account: MegaAccountSecret): Promise<MegaConnectionStatus> {
    return {
      connected: false,
      reason: 'Encrypted account session is available, but the MEGA SDK adapter is not connected yet.',
    };
  }

  async search(_account: MegaAccountSecret, _query: string): Promise<MegaSearchHit[]> {
    throw new Error('MEGA SDK adapter is not connected yet');
  }
}
