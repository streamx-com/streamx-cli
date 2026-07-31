---
title: "streamx settings event-templates rename"
sidebar_label: "rename"
sidebar_position: 9
description: "Rename an event template"
---

# `streamx settings event-templates rename`

Rename an event template

```bash
streamx settings event-templates rename [options] [<oldId>] [<newId>]
```

For user-created templates: renames the file in the context's event-templates/ folder.
For registered templates: rewrites the settings entry under the new ID (the underlying file is not moved).
Default templates cannot be renamed - use `copy` to create a clone under a new ID.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<oldId>` | no | Current template ID (prompts if omitted) |
| `<newId>` | no | New template ID (prompts if omitted) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
