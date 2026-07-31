---
title: "streamx settings event-templates delete"
sidebar_label: "delete"
sidebar_position: 3
description: "Delete a user-created event template"
---

# `streamx settings event-templates delete`

Delete a user-created event template

```bash
streamx settings event-templates delete [options] [<templateId>]
```

Deletes a template from the context's event-templates/ folder.
Default templates cannot be deleted this way - use `reset-default-templates` to restore them.
Registered templates cannot be deleted this way - use `unregister` instead.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | no | Template ID (prompts if omitted) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-f`, `--force` |  | Skip the confirmation prompt |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
