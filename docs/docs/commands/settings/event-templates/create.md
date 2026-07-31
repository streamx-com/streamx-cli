---
title: "streamx settings event-templates create"
sidebar_label: "create"
sidebar_position: 2
description: "Create a new event template (interactive wizard)"
---

# `streamx settings event-templates create`

Create a new event template (interactive wizard)

```bash
streamx settings event-templates create [options]
```

Prompts for a template ID and a CloudEvent type, then writes a starter template to the context's event-templates/&lt;id&gt;.json. Run `edit` afterwards to customize the content.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
