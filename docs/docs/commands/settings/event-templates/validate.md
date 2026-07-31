---
title: "streamx settings event-templates validate"
sidebar_label: "validate"
sidebar_position: 12
description: "Validate one or all event templates"
---

# `streamx settings event-templates validate`

Validate one or all event templates

```bash
streamx settings event-templates validate [options] [<templateId>]
```

Checks that a template file is valid JSON and has the required CloudEvents fields (specversion, id, source, type). Use without arguments or with --all to validate every known template. Exit code is non-zero if any template is invalid.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | no | Template ID to validate (prompts if omitted; ignored with --all) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-a`, `--all` |  | Validate every known template |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
