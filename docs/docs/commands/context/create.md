---
title: "streamx context create"
sidebar_label: "create"
sidebar_position: 2
description: "Create a context and switch to it"
---

# `streamx context create`

Create a context and switch to it

```bash
streamx context create [options] <name>
```

The new context starts with empty settings and becomes the current context.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<name>` | yes | Context name (lowercase, digits, dashes) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--from` | `<from>` | Copy settings and event templates (not the login) from this context |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
