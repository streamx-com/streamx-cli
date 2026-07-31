---
title: "streamx context org unset"
sidebar_label: "unset"
sidebar_position: 2
description: "Clear the current organization of the active context"
---

# `streamx context org unset`

Clear the current organization of the active context

```bash
streamx context org unset [options]
```

Also clears the current project (it cannot exist without an organization). Idempotent. A STREAMX_ORG environment variable is not affected.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
