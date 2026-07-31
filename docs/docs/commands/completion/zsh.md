---
title: "streamx completion zsh"
sidebar_label: "zsh"
sidebar_position: 2
description: "Generate zsh completion script (use --help for setup instructions)"
---

# `streamx completion zsh`

Generate zsh completion script (use --help for setup instructions)

```bash
streamx completion zsh
```

If installed via Homebrew, zsh completions are set up automatically.
New terminal sessions will have completions available out of the box.

Manual setup (without Homebrew):
  mkdir -p ~/.zsh/completions
  streamx completion zsh &gt; ~/.zsh/completions/_streamx

Then add this to ~/.zshrc (before the line that runs compinit):
  fpath=(~/.zsh/completions $fpath)

To load completions in the current session only:
  source &lt;(streamx completion zsh)

---

Every command also accepts the [global options](../global-options.md).
