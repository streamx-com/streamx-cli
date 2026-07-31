---
title: "streamx settings event-templates reset-default-templates"
sidebar_label: "reset-default-templates"
sidebar_position: 10
description: "Delete and repopulate the <streamxHome>/default-event-templates folder"
---

# `streamx settings event-templates reset-default-templates`

Delete and repopulate the &lt;streamxHome&gt;/default-event-templates folder

```bash
streamx settings event-templates reset-default-templates [options]
```

Wipes the bundled default templates from &lt;streamxHome&gt;/default-event-templates (shared by all contexts) and restores them from the files embedded in the CLI jar. The contexts' own event templates and registered templates in settings are not touched.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-f`, `--force` |  | Skip the confirmation prompt (required in non-interactive environments) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
