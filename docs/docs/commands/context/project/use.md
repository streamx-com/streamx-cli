---
title: "streamx context project use"
sidebar_label: "use"
sidebar_position: 3
description: "Set the current project for the active context"
---

# `streamx context project use`

Set the current project for the active context

```bash
streamx context project use [options] <projectId>
```

Commands taking a project fall back to it when the argument is omitted. Requires a current organization (the project is resolved inside it); STREAMX_PROJECT overrides the stored value for a single invocation.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<projectId>` | yes | Project ID |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
