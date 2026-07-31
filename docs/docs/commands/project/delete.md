---
title: "streamx project delete"
sidebar_label: "delete"
sidebar_position: 3
description: "Delete a project"
---

# `streamx project delete`

Delete a project

```bash
streamx project delete [options] [<projectId>]
```

Asks to type the project ID back as confirmation; --force deletes without asking.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<projectId>` | no | Project ID (defaults to the current project) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-f`, `--force` |  | Skip the confirmation prompt (required in non-interactive environments) |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
