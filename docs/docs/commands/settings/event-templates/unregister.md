---
title: "streamx settings event-templates unregister"
sidebar_label: "unregister"
sidebar_position: 11
description: "Unregister a settings-registered event template (default templates are not affected)"
---

# `streamx settings event-templates unregister`

Unregister a settings-registered event template (default templates are not affected)

```bash
streamx settings event-templates unregister [options] [<templateId>]
```

Removes an `eventtemplate.&lt;id&gt;` entry from &lt;streamxHome&gt;/config/application.properties. The underlying file on disk is not touched. This command only operates on settings-registered templates - it will not delete defaults or user-created templates.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | no | Template ID to unregister (prompts if omitted) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
