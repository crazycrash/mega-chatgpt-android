import { McpServer } from '@modelcontextprotocol/server';
import * as z from 'zod/v4';
import { AccountVault, maskEmail } from './accountVault.js';
import { PendingMegaSdkGateway, type MegaGateway } from './megaGateway.js';

const vault = new AccountVault();
const gateway: MegaGateway = new PendingMegaSdkGateway();

function result(payload: Record<string, unknown>, isError = false) {
  return {
    content: [{ type: 'text' as const, text: JSON.stringify(payload) }],
    structuredContent: payload,
    ...(isError ? { isError: true } : {}),
  };
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

async function resolveAccount(accountRef?: string) {
  const ref = accountRef ?? (await vault.getCurrentAccountRef());
  if (!ref) return { error: 'No MEGA account is configured.' } as const;
  const account = await vault.getAccount(ref);
  if (!account) return { error: `Unknown account reference: ${ref}` } as const;
  return { ref, account } as const;
}

export function buildServer(): McpServer {
  const server = new McpServer(
    { name: 'mega-chatgpt', version: '0.1.0' },
    {
      instructions:
        'Use opaque account_ref values to select MEGA accounts. Never request or expose MEGA passwords or raw session tokens. Call mega_connection_status before reading account content when connection state is uncertain.',
    },
  );

  server.registerTool(
    'mega_plugin_info',
    {
      title: 'MEGA connector information',
      description: 'Return connector version and privacy/security behavior without exposing account secrets.',
    },
    async () =>
      result({
        connector: 'mega-chatgpt',
        version: '0.1.0',
        protocol: 'MCP',
        account_selector: 'opaque account_ref',
        sensitive_data_policy: 'MEGA passwords and raw session tokens are never returned to ChatGPT.',
      }),
  );

  server.registerTool(
    'mega_connection_status',
    {
      title: 'MEGA connection status',
      description:
        'Check whether a selected or current encrypted MEGA account is usable and return a human-readable status string plus safe account metadata.',
      inputSchema: z.object({
        account_ref: z.string().startsWith('acc_').optional(),
      }),
    },
    async ({ account_ref }) => {
      try {
        const resolved = await resolveAccount(account_ref);
        if ('error' in resolved) {
          return result(
            {
              status_string: 'Connessione MEGA non configurata',
              connected: false,
              reason: resolved.error,
            },
            true,
          );
        }

        const status = await gateway.connectionStatus(resolved.account);
        return result({
          status_string: status.connected
            ? 'Connessione ChatGPT ↔ MEGA riuscita'
            : 'Connessione ChatGPT ↔ MEGA non ancora riuscita',
          connected: status.connected,
          account_ref: resolved.ref,
          account_name: resolved.account.displayName,
          account_email_masked: maskEmail(resolved.account.email),
          root_items: status.rootItems,
          exported_links: status.exportedLinks,
          reason: status.reason,
        });
      } catch (error) {
        return result(
          {
            status_string: 'Errore durante il controllo della connessione MEGA',
            connected: false,
            reason: errorMessage(error),
          },
          true,
        );
      }
    },
  );

  server.registerTool(
    'mega_list_accounts',
    {
      title: 'List MEGA accounts',
      description:
        'List the MEGA accounts stored in the encrypted connector vault. Returns opaque account references and safe display metadata only.',
    },
    async () => {
      try {
        const refs = await vault.listAccountRefs();
        const current = await vault.getCurrentAccountRef();
        const accounts = [] as Array<Record<string, unknown>>;
        for (const ref of refs) {
          const account = await vault.getAccount(ref);
          if (!account) continue;
          accounts.push({
            account_ref: ref,
            account_name: account.displayName,
            account_email_masked: maskEmail(account.email),
            current: ref === current,
          });
        }
        return result({
          status_string: `${accounts.length} account MEGA configurati`,
          accounts,
        });
      } catch (error) {
        return result({ status_string: 'Impossibile leggere il vault account', reason: errorMessage(error) }, true);
      }
    },
  );

  server.registerTool(
    'mega_switch_account',
    {
      title: 'Switch MEGA account',
      description:
        'Select a previously stored MEGA account by opaque account_ref. The encrypted record is authenticated and decrypted only inside the connector before switching.',
      inputSchema: z.object({
        account_ref: z.string().startsWith('acc_'),
      }),
    },
    async ({ account_ref }) => {
      try {
        await vault.setCurrentAccount(account_ref);
        const account = await vault.getAccount(account_ref);
        return result({
          status_string: 'Account MEGA selezionato',
          account_ref,
          account_name: account?.displayName,
          account_email_masked: maskEmail(account?.email),
        });
      } catch (error) {
        return result({ status_string: 'Cambio account MEGA non riuscito', reason: errorMessage(error) }, true);
      }
    },
  );

  server.registerTool(
    'mega_search',
    {
      title: 'Search MEGA',
      description: 'Search file, folder, and supported link names in the selected MEGA account.',
      inputSchema: z.object({
        query: z.string().min(1).max(300),
        account_ref: z.string().startsWith('acc_').optional(),
      }),
    },
    async ({ query, account_ref }) => {
      try {
        const resolved = await resolveAccount(account_ref);
        if ('error' in resolved) return result({ status_string: 'Ricerca MEGA non disponibile', reason: resolved.error }, true);
        const hits = await gateway.search(resolved.account, query);
        return result({
          status_string: `${hits.length} risultati MEGA`,
          account_ref: resolved.ref,
          query,
          hits,
        });
      } catch (error) {
        return result({ status_string: 'Ricerca MEGA non riuscita', reason: errorMessage(error) }, true);
      }
    },
  );

  return server;
}
