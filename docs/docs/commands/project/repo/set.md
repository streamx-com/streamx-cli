---
title: "streamx project repo set"
sidebar_label: "set"
sidebar_position: 3
description: "Connect a repository to a project, or change its settings"
---

# `streamx project repo set`

Connect a repository to a project, or change its settings

```bash
streamx project repo set [options]
```

Connects the repository when the project has none yet, otherwise updates the connection. Use 'ssh-key set' first (or 'project create --ssh-private-key') for private repositories.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--branch` | `<branch>` | Git branch the platform deploys from |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `--uri` | `<uri>` | Git repository URI |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
