# MEGA ChatGPT MCP Server

This directory contains the ChatGPT-facing MCP backend for the MEGA connector.

## Security model

- ChatGPT receives only opaque `account_ref` values.
- MEGA passwords are never accepted by MCP tools.
- Raw MEGA session tokens are never returned by MCP tools.
- Account/session records are encrypted with AES-256-GCM.
- `MEGA_ACCOUNT_VAULT_KEY_B64` must be a random 32-byte key stored only in deployment secrets.

Example key generation for a deployment secret:

```bash
openssl rand -base64 32
```

Do not paste the generated key into ChatGPT, commit it to GitHub, or bundle it in the Android APK.

## Local development

```bash
npm install
npm run typecheck
npm run build
MEGA_ACCOUNT_VAULT_KEY_B64="<deployment-secret>" npm start
```

The development bootstrap intentionally binds to `127.0.0.1`. ChatGPT requires a remote MCP endpoint, so production deployment will use TLS + authentication in front of the MCP handler.

## Tools

- `mega_plugin_info`
- `mega_connection_status`
- `mega_list_accounts`
- `mega_switch_account`
- `mega_search`

## Next integration

Replace `PendingMegaSdkGateway` with the official MEGA SDK adapter. The adapter will use supported MEGA session-resume/authentication methods and return account identity, node tree statistics, exported/public links, and search results without exposing secret key material to ChatGPT.
