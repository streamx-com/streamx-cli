---
title: "streamx completion bash"
sidebar_label: "bash"
sidebar_position: 1
description: "Generate bash completion script (use --help for setup instructions)"
---

# `streamx completion bash`

Generate bash completion script (use --help for setup instructions)

```bash
streamx completion bash
```

If installed via Homebrew, bash completions are set up automatically.
The bash-completion package is required:
  brew install bash-completion@2
Follow its caveats to configure your shell, then open a new terminal.

Manual setup (without Homebrew):
  streamx completion bash &gt; /etc/bash_completion.d/streamx

To load completions in the current session only:
  source &lt;(streamx completion bash)

---

Every command also accepts the [global options](../global-options.md).
