---
title: "streamx org members set-role"
sidebar_label: "set-role"
sidebar_position: 4
description: "Change the role of an organization member"
---

# `streamx org members set-role`

Change the role of an organization member

```bash
streamx org members set-role [options] <userId>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<userId>` | yes | ID of an ACTIVE member, as shown by 'streamx org members list' |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-r`, `--role` | `<role>` | New role: owner, edit, view |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
