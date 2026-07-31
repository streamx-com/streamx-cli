---
title: "streamx settings event-templates edit"
sidebar_label: "edit"
sidebar_position: 4
description: "Open an event template in $EDITOR"
---

# `streamx settings event-templates edit`

Open an event template in $EDITOR

```bash
streamx settings event-templates edit [options] [<templateId>]
```

Default templates are copied into the context's event-templates/ folder before editing.
On save, the file is re-validated as JSON; invalid JSON re-opens the editor.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | no | Template ID (prompts if omitted) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
