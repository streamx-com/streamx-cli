---
title: "streamx settings event-templates register"
sidebar_label: "register"
sidebar_position: 8
description: "Register an event template file under a template ID (writes to settings)"
---

# `streamx settings event-templates register`

Register an event template file under a template ID (writes to settings)

```bash
streamx settings event-templates register [options] <templateId> <path>
```

Adds an `eventtemplate.&lt;id&gt;=&lt;path&gt;` entry to the active context's application.properties. The path can be absolute or relative to the context directory. Registered templates take precedence over the context's custom templates and the shared default templates.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | yes | Template ID (the value passed to `publish event`) |
| `<path>` | yes | Path to the template JSON file (relative to the context directory or absolute) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
