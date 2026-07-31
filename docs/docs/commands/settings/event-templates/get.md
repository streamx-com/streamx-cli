---
title: "streamx settings event-templates get"
sidebar_label: "get"
sidebar_position: 5
description: "Show the content of an event template"
---

# `streamx settings event-templates get`

Show the content of an event template

```bash
streamx settings event-templates get [options] [<templateId>]
```

Prints the template JSON as-is (text), or reformats it (json / yaml).

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
