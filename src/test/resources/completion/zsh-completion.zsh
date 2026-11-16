# Run actual Zsh Tab completion in a pseudo-terminal. Only the CLI's data helpers
# are stubbed; Zsh's _arguments, matching, and command dispatch run normally.
if [[ "$1" == --child ]]; then
  autoload -Uz compinit
  compinit -D -u
  source "$2"
  CALL_LOG="$3"
  MATCH_LOG="$4"
  FAIL_HELPER="$5"
  PROMPT=''
  RPROMPT=''

  streamx() {
    printf '<%s>' "$@" >> "$CALL_LOG"
    printf '\n' >> "$CALL_LOG"
    if [[ "$FAIL_HELPER" == true ]]; then
      print -u2 -- 'helper unavailable'
      return 1
    fi
    case "$*" in
      *__complete-domains*) print -l shop shop.example.com other.example.com ;;
      *__complete-org-ids*) print -l so-alpha so-beta other-org ;;
      *__complete-project-ids*) print -l project-one project-two other-project ;;
      *__complete-template-ids*) print -l custom.page custom.asset other.page ;;
      *__complete-non-default-template-ids*) print -l editable.page ;;
      *__complete-registered-template-ids*) print -l registered.page ;;
      *__complete-settings-set-keys*) print -l custom.new custom.saved other.key ;;
      *__complete-settings-keys*) print -l custom.saved other.key ;;
      *) return 1 ;;
    esac
  }

  compadd() {
    local -a matches
    builtin compadd -A matches "$@"
    if (( ${#matches} )); then
      print -rl -- "${matches[@]}" >> "$MATCH_LOG"
    fi
    builtin compadd "$@"
  }
  capture_completion() {
    _main_complete
    print -r -- DONE >> "$MATCH_LOG"
  }
  zle -C capture-completion complete-word capture_completion
  bindkey '^I' capture-completion
  print -r -- READY >> "$MATCH_LOG"
  return
fi

zmodload zsh/zpty || exit 1
export TERM=xterm
zpty -b completion zsh -f -i || exit 1
trap 'zpty -d completion' EXIT

wait_for() {
  local marker="$1" attempt reply
  for attempt in {1..200}; do
    # Drain terminal output so the child cannot block on a full buffer.
    while zpty -r completion reply; do
      print -r -- "$reply"
    done
    [[ "$(<$MATCH_LOG)" == *"$marker"* ]] && return 0
    sleep 0.025
  done
  print -u2 -- "Timed out waiting for $marker"
  return 1
}

MATCH_LOG="$3"
zpty -w completion "source ${(q)0} --child ${(q)1} ${(q)2} ${(q)3} ${(q)5}"
wait_for READY || exit 1
zpty -w -n completion "$4"$'\t'
wait_for DONE || exit 1
