---
title: "streamx auth token"
sidebar_label: "token"
sidebar_position: 0
description: "Manage personal access tokens"
---

# `streamx auth token`

Manage personal access tokens

```bash
streamx auth token <command>
```

Personal access tokens authenticate the CLI in CI and other non-interactive environments.
Set STREAMX_PLATFORM_TOKEN=&lt;token&gt; to use one for platform calls, no login needed.
A token acts as you, with your permissions, and does not expire until revoked.
These subcommands need a login session: a token cannot manage tokens, so unset STREAMX_PLATFORM_TOKEN to use them.

## Subcommands

| Command | Description |
| --- | --- |
| [`create`](./create) | Create a personal access token |
| [`list`](./list) | List your personal access tokens |
| [`revoke`](./revoke) | Revoke a personal access token |

---

Every command also accepts the [global options](../../global-options.md).
