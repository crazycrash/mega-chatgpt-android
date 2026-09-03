# MEGA ChatGPT Android

Android client + MEGA connector project.

## Architecture

This repository will contain:

- `android/` — Android client for authentication, configuration, account status and connector controls.
- `mcp-server/` — remote Model Context Protocol server exposed to ChatGPT.
- `docs/` — architecture, security and deployment documentation.

## Integration target

The connector is designed around MEGA's official SDK/API capabilities and OpenAI's Apps SDK / MCP model. ChatGPT talks to the remote MCP server; the MCP server authenticates the user and performs approved operations against MEGA.

Initial tool surface planned:

- account / storage information
- list folders and files
- search files
- file metadata
- download/read supported content
- upload files
- create folders
- move / rename / delete operations
- shared links (where supported safely)

Credentials and MEGA session secrets must never be committed to this repository or embedded in the Android APK.

## Status

Initial project bootstrap.
