---
title: "streamx org members remove"
sidebar_label: "remove"
sidebar_position: 3
description: "Remove a member from an organization"
---

# `streamx org members remove`

Remove a member from an organization

```bash
streamx org members remove [options] <userId>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<userId>` | yes | ID of an ACTIVE member, as shown by 'streamx org members list' |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
