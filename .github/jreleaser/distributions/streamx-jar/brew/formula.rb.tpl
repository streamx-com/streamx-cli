# {{jreleaserCreationStamp}}
{{#brewRequireRelative}}
require_relative "{{.}}"
{{/brewRequireRelative}}

class {{brewFormulaName}} < Formula
  desc "{{projectDescription}}"
  homepage "{{projectLinkHomepage}}"
  url "{{distributionUrl}}"{{#brewDownloadStrategy}}, :using => {{.}}{{/brewDownloadStrategy}}
  version "{{projectVersion}}"
  sha256 "{{distributionChecksumSha256}}"
  license "{{projectLicense}}"

  {{#brewHasLivecheck}}
  livecheck do
    {{#brewLivecheck}}
    {{.}}
    {{/brewLivecheck}}
  end
  {{/brewHasLivecheck}}
  {{#brewDependencies}}
  depends_on {{.}}
  {{/brewDependencies}}

  def install
    libexec.install Dir["*"]
    (bin/"{{distributionExecutableName}}").write_env_script libexec/"bin/{{distributionExecutableUnix}}",
      Language::Java.overridable_java_home_env("21")

    # Install pre-generated shell completions
    bash_completion.install libexec/"share/completions/{{distributionExecutableName}}.bash" => "{{distributionExecutableName}}"
    zsh_completion.install libexec/"share/completions/_{{distributionExecutableName}}"
  end

  test do
    output = shell_output("#{bin}/{{distributionExecutableName}} --version")
    assert_match "{{projectVersion}}", output
  end
end
