---
title: "streamx project repo remove"
sidebar_label: "remove"
sidebar_position: 2
description: "Disconnect the repository from a project"
---

# `streamx project repo remove`

Disconnect the repository from a project

```bash
streamx project repo remove [options]
```

Only removes the connection; the Git repository itself is not touched.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
