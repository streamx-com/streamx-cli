---
title: "streamx project pending-changes"
sidebar_label: "pending-changes"
sidebar_position: 6
description: "List changes waiting to be applied to a project"
---

# `streamx project pending-changes`

List changes waiting to be applied to a project

```bash
streamx project pending-changes [options] [<projectId>]
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<projectId>` | no | Project ID (defaults to the current project) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
