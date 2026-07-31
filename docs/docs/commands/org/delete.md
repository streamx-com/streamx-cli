---
title: "streamx org delete"
sidebar_label: "delete"
sidebar_position: 3
description: "Delete an organization"
---

# `streamx org delete`

Delete an organization

```bash
streamx org delete [options] [<orgId>]
```

Asks to type the organization ID back as confirmation; --force deletes without asking.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<orgId>` | no | Organization ID (defaults to the current organization) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-f`, `--force` |  | Skip the confirmation prompt (required in non-interactive environments) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
