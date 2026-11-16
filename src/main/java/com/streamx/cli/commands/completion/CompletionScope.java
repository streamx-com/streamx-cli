package com.streamx.cli.commands.completion;

/** Shell helpers for passing the command's selected scope to completion requests. */
final class CompletionScope {
  private CompletionScope() {
  }

  static String value(String option) {
    return "\"$(_streamx_completion_value " + option + ")\"";
  }

  static String functions(String wordsExpression, String programName) {
    return """
        _streamx_completion_value() {
          local option="$1" alias="${2-}" word value="" expecting=false
          for word in "%s"; do
            if [ "$expecting" = true ]; then
              [ "$word" = "=" ] && continue
              value="$word"
              expecting=false
            elif [ "$word" = "$option" ] || { [ -n "$alias" ] && [ "$word" = "$alias" ]; }; then
              expecting=true
            else
              case "$word" in
                "$option"=*) value="${word#*=}" ;;
              esac
            fi
          done
          printf '%%s' "$value"
        }

        _streamx_complete_scoped() {
          local context home
          local -a scope_args
          scope_args=()
          context=$(_streamx_completion_value --context -C)
          home=$(_streamx_completion_value --streamx-home -H)
          [ -n "$context" ] && scope_args+=(--context "$context")
          [ -n "$home" ] && scope_args+=(--streamx-home "$home")
          %s "${scope_args[@]}" "$@"
        }

        """.formatted(wordsExpression, programName);
  }
}
