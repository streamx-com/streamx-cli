---
title: "streamx settings event-templates which"
sidebar_label: "which"
sidebar_position: 13
description: "Print the resolved file path for a template ID"
---

# `streamx settings event-templates which`

Print the resolved file path for a template ID

```bash
streamx settings event-templates which [options] [<templateId>]
```

Prints only the absolute path that `publish event &lt;templateId&gt;` would resolve to. Useful for scripting: `$(streamx settings event-templates which page.published)`.

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
