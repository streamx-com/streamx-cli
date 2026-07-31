---
title: "streamx context delete"
sidebar_label: "delete"
sidebar_position: 4
description: "Delete a context"
---

# `streamx context delete`

Delete a context

```bash
streamx context delete [options] <name>
```

Removes the context's settings, event templates and stored login from this machine. The login is not revoked; run 'streamx auth logout' in the context first.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<name>` | yes | Context name |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
