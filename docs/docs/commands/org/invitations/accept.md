---
title: "streamx org invitations accept"
sidebar_label: "accept"
sidebar_position: 1
description: "Accept an invitation to an organization"
---

# `streamx org invitations accept`

Accept an invitation to an organization

```bash
streamx org invitations accept [options]
```

The invitation token is read from standard input, or from --token-file.
It is not taken as an argument: that would leave a credential in the shell
history and expose it to anyone listing processes.

  streamx org invitations accept --org &lt;orgId&gt; --token-file ./token.txt
  pbpaste | streamx org invitations accept --org &lt;orgId&gt;

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--token-file` | `<tokenFile>` | File holding the invitation token; defaults to reading standard input |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
