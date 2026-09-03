# Connector V1

Goal: allow ChatGPT to search and inspect the user's MEGA cloud content through an authenticated connector.

## First probes

1. Determine whether a MEGA account session is available.
2. Fetch the cloud node tree and report root child count.
3. Enumerate account public/exported links when exposed by the SDK.
4. Search node names for a requested phrase such as `Corsi ITA`.
5. Return metadata first; retrieve file bytes only on explicit request.

## Android bridge

The Android client owns user-facing account/session setup. It must use supported MEGA SDK mechanisms and must not read another app's private storage. If the official MEGA app's viewed-link history is not exposed by the MEGA SDK, that history cannot be assumed accessible from our app.

## Test contract

`mega_status` returns authentication state and root statistics.
`mega_search` returns matching nodes.
`mega_public_links` returns account-exported links supported by the SDK.

The connector must distinguish account-exported/public links from arbitrary links merely opened in the official MEGA Android app.
