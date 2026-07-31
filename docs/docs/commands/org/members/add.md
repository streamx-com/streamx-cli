---
title: "streamx org members add"
sidebar_label: "add"
sidebar_position: 1
description: "Add an existing user to an organization"
---

# `streamx org members add`

Add an existing user to an organization

```bash
streamx org members add [options] <email>
```

The account must already exist in the identity provider; it is looked up by email. To bring in a new user, send an invitation instead: streamx org invitations create

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<email>` | yes | Email of an existing account |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-r`, `--role` | `<role>` | Role to grant: owner, edit, view |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
