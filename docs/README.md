# StreamX CLI docs

Docusaurus site for the CLI. Theme follows streamx.com: background `#0a0a0b`, text `#ecf5ff`,
surfaces `#232323`, accent `#7714ff`, type "Be Vietnam Pro" - see `src/css/custom.css`.

## Layout

    docs/
      intro.md              hand-written
      commands/             GENERATED - do not edit

## Regenerating the command reference

The reference is produced from the live picocli command tree, so it can never drift from the
CLI's actual behaviour. It is written by a hidden command:

```bash
mvn -q package -DskipTests
java -jar target/streamx-cli-*-runner.jar __generate-docs docs/docs/commands
```

Regenerate whenever a command, option, argument or help text changes, and commit the result.

What the generator emits, per command:

- front matter (`title`, `sidebar_label`, `sidebar_position`, `description`)
- the synopsis
- subcommand table with links, for groups
- arguments table, with a required column
- options table, listing only the options specific to that command

Options accepted by *every* command are collected once into `commands/global-options.md`;
each page links to it rather than repeating them. That set is derived as the intersection of the
options across all leaf commands, so it stays correct as commands come and go. Options that most
- but not all - commands share (`--output`, `--verbose`) stay on the individual pages, because
claiming they work everywhere would be wrong.

Hidden commands (`__complete-*`, `__generate-docs`) are skipped.

## Running the site

```bash
npm install
npm start          # dev server with live reload
npm run build      # static build into build/
```
