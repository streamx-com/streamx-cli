---
title: "streamx project clusters set"
sidebar_label: "set"
sidebar_position: 4
description: "Set the full list of clusters a project runs on"
---

# `streamx project clusters set`

Set the full list of clusters a project runs on

```bash
streamx project clusters set [options] <clusterId>
```

Replaces the project's enabled clusters with exactly the given IDs.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<clusterId>` | yes | Cluster IDs, as shown by 'streamx org clusters list' |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
