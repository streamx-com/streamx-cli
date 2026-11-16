package com.streamx.cli.commands.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Executes generated shell functions so later assignments cannot silently discard candidates. */
@QuarkusTest
class BashCompletionIT extends CliBaseIT {

  @Test
  void shouldResolveEachExistingTemplateProviderAtCompletionTime() throws Exception {
    runBash("""
        check _picocli_streamx_publish_event 'custom.page custom.asset' \
          streamx publish event custom.
        [[ "$(cat "$CALL_LOG")" == '<__complete-template-ids>' ]] || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_eventtemplates_copy 'custom.page custom.asset' \
          streamx settings event-templates copy custom.
        [[ "$(cat "$CALL_LOG")" == '<__complete-template-ids>' ]] || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_eventtemplates_rename 'editable.page' \
          streamx settings event-templates rename editable.
        [[ "$(cat "$CALL_LOG")" == '<__complete-non-default-template-ids>' ]] || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_eventtemplates_unregister 'registered.page' \
          streamx settings event-templates unregister registered.
        [[ "$(cat "$CALL_LOG")" == '<__complete-registered-template-ids>' ]] || exit 1
        """);
  }

  @Test
  void shouldResolveSettingsKeysAndFilterByTypedPrefix() throws Exception {
    runBash("""
        check _picocli_streamx_settings_set 'custom.new custom.saved' \
          streamx settings set custom.
        [[ "$(cat "$CALL_LOG")" == '<__complete-settings-set-keys>' ]] || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_get 'custom.saved' streamx settings get custom.
        [[ "$(cat "$CALL_LOG")" == '<__complete-settings-keys>' ]] || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_unset 'custom.saved' streamx settings unset custom.
        [[ "$(cat "$CALL_LOG")" == '<__complete-settings-keys>' ]] || exit 1
        """);
  }

  @Test
  void shouldNotSuggestSourceIdsOrKeysForOtherArguments() throws Exception {
    runBash("""
        check _picocli_streamx_settings_set '' streamx settings set custom.saved ''
        check _picocli_streamx_settings_eventtemplates_copy '' \
          streamx settings event-templates copy custom.page ''
        check _picocli_streamx_settings_eventtemplates_rename '' \
          streamx settings event-templates rename editable.page ''
        check _picocli_streamx_publish_event '--help' streamx publish event --he
        [[ ! -s "$CALL_LOG" ]] || exit 1
        """);
  }

  @Test
  void shouldForwardScopeWithoutCountingOptionsAsPositionalArguments() throws Exception {
    runBash("""
        check _picocli_streamx_settings_set 'custom.new custom.saved' \
          streamx settings set --context team --streamx-home '/tmp/team home' custom.
        [[ "$(cat "$CALL_LOG")" == \
          '<--context><team><--streamx-home></tmp/team home><__complete-settings-set-keys>' ]] \
          || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_set 'custom.new custom.saved' \
          streamx settings set -C team -H '/tmp/team home' custom.
        [[ "$(cat "$CALL_LOG")" == \
          '<--context><team><--streamx-home></tmp/team home><__complete-settings-set-keys>' ]] \
          || exit 1
        : > "$CALL_LOG"
        check _picocli_streamx_settings_set 'custom.new custom.saved' \
          streamx settings set --context=team --streamx-home='/tmp/team home' custom.
        [[ "$(cat "$CALL_LOG")" == \
          '<--context><team><--streamx-home></tmp/team home><__complete-settings-set-keys>' ]] \
          || exit 1
        : > "$CALL_LOG"
        # Bash can split --option=value into three completion words.
        check _picocli_streamx_settings_set 'custom.new custom.saved' \
          streamx settings set --context = team --streamx-home = '/tmp/team home' custom.
        [[ "$(cat "$CALL_LOG")" == \
          '<--context><team><--streamx-home></tmp/team home><__complete-settings-set-keys>' ]] \
          || exit 1
        """);
  }

  private void runBash(String scenario) throws Exception {
    ProcessResult generated = exec("completion", "bash");
    generated.assertSuccess();
    Path script = Files.createTempFile("bash-completion-", ".bash");
    Path calls = Files.createTempFile("completion-calls-", ".log");
    Path output = Files.createTempFile("completion-output-", ".log");
    Process process = null;
    try {
      Files.writeString(script, generated.stdout() + """

          CALL_LOG="$1"
          compopt() { :; }
          streamx() {
            printf '<%s>' "$@" >> "$CALL_LOG"
            case "${!#}" in
              __complete-template-ids) printf '%s\\n' custom.page custom.asset other.page ;;
              __complete-non-default-template-ids) printf '%s\\n' editable.page ;;
              __complete-registered-template-ids) printf '%s\\n' registered.page ;;
              __complete-settings-set-keys) printf '%s\\n' custom.new custom.saved other.key ;;
              __complete-settings-keys) printf '%s\\n' custom.saved other.key ;;
              *) return 1 ;;
            esac
          }
          check() {
            local function_name="$1" expected="$2"
            shift 2
            COMP_WORDS=("$@")
            COMP_CWORD=$((${#COMP_WORDS[@]} - 1))
            COMPREPLY=()
            "$function_name"
            if [[ "${COMPREPLY[*]}" != "$expected" ]]; then
              printf 'Completion for %s: expected [%s], got [%s]\\n' \
                "$*" "$expected" "${COMPREPLY[*]}"
              exit 1
            fi
          }
          """ + scenario);
      process = new ProcessBuilder("bash", script.toString(), calls.toString())
          .redirectErrorStream(true).redirectOutput(output.toFile()).start();
      assertThat(process.waitFor(10, TimeUnit.SECONDS)).as("completion finished").isTrue();
      assertThat(process.exitValue()).as(Files.readString(output)).isZero();
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly().waitFor();
      }
      Files.deleteIfExists(script);
      Files.deleteIfExists(calls);
      Files.deleteIfExists(output);
    }
  }
}
