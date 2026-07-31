---
title: "streamx project update"
sidebar_label: "update"
sidebar_position: 9
description: "Update a project's name or description"
---

# `streamx project update`

Update a project's name or description

```bash
streamx project update [options] [<projectId>]
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<projectId>` | no | Project ID (defaults to the current project) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-d`, `--description` | `<description>` | New project description |
| `-n`, `--name` | `<name>` | New project name |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
