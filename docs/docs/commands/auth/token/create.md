---
title: "streamx auth token create"
sidebar_label: "create"
sidebar_position: 1
description: "Create a personal access token"
---

# `streamx auth token create`

Create a personal access token

```bash
streamx auth token create [options] <name>
```

Prints the token once to standard output - copy it now, it cannot be retrieved again.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<name>` | yes | A label to recognize the token later (e.g. ci-github-actions) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
