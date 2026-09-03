# ChatGPT plugin / MCP multi-account security

## Goal

Expose MEGA to ChatGPT through an MCP app/plugin without exposing MEGA passwords or raw session tokens to the model.

## Account identity model

ChatGPT sees only an opaque selector such as:

`acc_7a9a6a15-...`

The opaque `account_ref` is random and contains no email, username, password, session key, or MEGA node key. The mapping from `account_ref` to the real MEGA account/session is stored only inside an encrypted vault.

This is preferable to sending an encrypted email/account identifier to ChatGPT: the model does not need the real identifier at all.

## Encrypted storage

### Android

`SecureAccountStore` uses Android Keystore with AES/GCM/NoPadding. SharedPreferences contains only encrypted account payloads plus opaque account references.

Encrypted payload fields:

- display name
- email, when available
- MEGA session token returned by supported MEGA SDK authentication
- account_ref for integrity checking

The encryption key is generated inside Android Keystore and is not committed to GitHub.

### MCP backend

`AccountVault` uses AES-256-GCM. The key comes from `MEGA_ACCOUNT_VAULT_KEY_B64`, which must be stored in deployment secrets/KMS and never in source control.

Runtime vault files are ignored by git.

## Account switching

1. ChatGPT calls `mega_list_accounts`.
2. The connector returns only safe metadata and opaque `account_ref` values.
3. ChatGPT/user selects one `account_ref`.
4. `mega_switch_account` verifies and decrypts the selected vault record inside the connector.
5. The MEGA adapter resumes that account using the stored MEGA session via the official SDK flow.
6. Raw session data is never returned to ChatGPT.

## MCP tools in V0.1

- `mega_plugin_info`
- `mega_connection_status`
- `mega_list_accounts`
- `mega_switch_account`
- `mega_search`

Example safe response:

```json
{
  "status_string": "Connessione ChatGPT ↔ MEGA riuscita",
  "connected": true,
  "account_ref": "acc_...",
  "account_name": "MEGA personale",
  "account_email_masked": "c***@example.com"
}
```

The connector must never return the MEGA password, raw session token, recovery key, master key, or decrypted file/node keys.

## Device pairing rule

The Android app may securely provision/refresh an account session into the backend vault after an explicit user authentication or pairing flow. This provisioning endpoint is not an MCP tool and is not callable by the model.

## Current limitation

The MCP layer is implemented and builds successfully, but the MEGA gateway is currently a safe placeholder until the official MEGA SDK adapter is connected. It intentionally cannot report a false successful MEGA connection.

## ChatGPT distribution

The MCP server is the backend for the ChatGPT app. OpenAI Apps SDK is the recommended packaging path for ChatGPT apps backed by MCP. Publishing an MCP app does not automatically create a separate plugin listing; plugin distribution requires the corresponding plugin/app submission or workspace availability step.
