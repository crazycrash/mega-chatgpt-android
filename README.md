# MEGA ChatGPT Android

Android client + MEGA connector project.

## Architecture

This repository contains:

- `android/` — Android client for authentication, configuration, diagnostics, account status and connector controls.
- `mcp-server/` — Model Context Protocol backend exposed to ChatGPT / Apps SDK.
- `docs/` — architecture, security and deployment documentation.

## Integration target

The connector is designed around MEGA's official SDK/API capabilities and OpenAI's Apps SDK / MCP model. ChatGPT talks to a remote MCP server; the MCP server authenticates the selected account and performs approved operations against MEGA.

Current MCP tools:

- `mega_plugin_info`
- `mega_connection_status`
- `mega_list_accounts`
- `mega_switch_account`
- `mega_search`

Planned MEGA tool surface:

- account / storage information
- list folders and files
- search files
- file metadata
- download/read supported content
- upload files
- create folders
- move / rename / delete operations
- exported/shared links where supported safely

## Multi-account privacy model

ChatGPT receives only opaque account selectors such as `acc_<random-uuid>`. The real MEGA account identity and resumable session are stored encrypted and are decrypted only inside the Android app or connector when that account is selected.

Android uses Android Keystore + AES-GCM. The MCP backend uses AES-256-GCM with a deployment key supplied through secret management. MEGA passwords, raw session tokens, recovery keys, and decryption keys must never be returned to ChatGPT, committed to GitHub, or embedded in the APK.

## ChatGPT app / plugin

The MCP server is the ChatGPT-facing backend. OpenAI Apps SDK is the intended packaging path. A custom MCP app can be tested in ChatGPT developer mode; a separate plugin listing/distribution step is still required when publishing it as a plugin.

## Status

- Android diagnostic APK builds successfully.
- MCP server typechecks and builds in GitHub Actions.
- Encrypted multi-account storage has been added on Android and backend.
- Next step: replace the placeholder MEGA gateway with the official MEGA SDK integration and provision a real encrypted MEGA session through the Android app.
