# Contributing

- All commands should extend `AbstractCommand` from [this package](./src/main/java/com/streamx/cli/framework).
- There are helper classes which extend the `AbstractCommand` class:
  - Use `AbstractCommandGroup` for commands which only contain subcommands and don't do anything else, e.g. `streamx settings`.
  - Use `AbstractSilentCommand` for commands which don't print any user-faced output, e.g. `streamx settings set`.
- Commands should throw only the [`CliException`](./src/main/java/com/streamx/cli/framework/CliException.java).
- All user facing messages should be provided by [`MessageProvider`](./src/main/java/com/streamx/cli/i18n/MessageProvider.java).

## Build

- Ensure that you have [Mandrel 23](https://github.com/graalvm/mandrel/releases/tag/mandrel-23.1.11.0-Final) installed.
- Then you can run `./mvnw clean install -Dnative` to build the native executable.

## Development

**IMPORTANT:** mark test which run StreamX mesh with the `DisabledIfDockerUnavaliable` annotation.
Otherwise, CI will fail because at this moment Docker isn't supported on macOS arm64.

- Enter Quarkus development console.

`./mvnw quarkus:dev`

- Use `e` button to edit CLI arguments.

### Native build configuration
Native build requires additional configuration like registering classes for reflection or registering resources to be included
in native artifact. This project uses `quarkus.native.resources.includes` property in
[application.properties](src/main/resources/application.properties) for resources registration and
`com/streamx/cli/ReflectionConfiguration.java` for reflection registration. More details about configuring native build can
be found in:
* https://quarkus.io/guides/writing-native-applications-tips
* https://quarkus.io/guides/native-reference


## Running tests

`./mvnw verify -Dnative`

## Release process

Use `./release.sh <patch|minor|major>` script.