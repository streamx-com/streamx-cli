---
title: "streamx org invitations create"
sidebar_label: "create"
sidebar_position: 3
description: "Invite a user to an organization"
---

# `streamx org invitations create`

Invite a user to an organization

```bash
streamx org invitations create [options] <email>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<email>` | yes | Email address to invite |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-r`, `--role` | `<role>` | Role to grant: owner, edit, view |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
