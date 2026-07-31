---
title: "streamx context org use"
sidebar_label: "use"
sidebar_position: 3
description: "Set the current organization for the active context"
---

# `streamx context org use`

Set the current organization for the active context

```bash
streamx context org use [options] <orgId>
```

Commands taking an organization fall back to it when the argument is omitted. Stored per context; STREAMX_ORG overrides it for a single invocation.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<orgId>` | yes | Organization ID |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
