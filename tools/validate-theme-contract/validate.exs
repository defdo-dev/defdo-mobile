#!/usr/bin/env elixir

root = Path.expand("../..", __DIR__)
dir = Path.join([root, "shared-contracts", "theme"])

required = [
  "theme_token_schema.json",
  "fallback_theme.light.fixture.json",
  "fallback_theme.dark.fixture.json",
  "theme.success.fixture.json",
  "theme.missing_required_token.fixture.json",
  "theme.invalid_color.fixture.json",
  "theme.low_contrast.fixture.json",
  "theme_redaction_rules.json"
]

abort = fn msg ->
  IO.puts(:stderr, msg)
  System.halt(1)
end

read_json = fn path ->
  case File.read(path) do
    {:ok, contents} ->
      case JSON.decode(contents) do
        {:ok, data} -> data
        {:error, error} -> abort.("#{path}: invalid JSON: #{inspect(error)}")
      end

    {:error, reason} ->
      abort.("#{path}: cannot read: #{reason}")
  end
end

missing =
  required
  |> Enum.reject(fn file -> File.regular?(Path.join(dir, file)) end)

unless missing == [] do
  abort.("missing theme contract files: #{Enum.join(missing, ", ")}")
end

schema = read_json.(Path.join(dir, "theme_token_schema.json"))
required_tokens = Map.fetch!(schema, "required_tokens")

if required_tokens == [] do
  abort.("theme schema must declare required tokens")
end

hex_color_re = ~r/\A#[0-9A-Fa-f]{6}\z/

["fallback_theme.light.fixture.json", "fallback_theme.dark.fixture.json", "theme.success.fixture.json"]
|> Enum.each(fn file ->
  fixture = read_json.(Path.join(dir, file))
  tokens = Map.get(fixture, "tokens", %{})
  missing_tokens = required_tokens -- Map.keys(tokens)

  unless missing_tokens == [] do
    abort.("#{file} missing tokens: #{Enum.join(missing_tokens, ", ")}")
  end

  Enum.each(tokens, fn {name, value} ->
    unless String.match?(value, hex_color_re) do
      abort.("#{file} token #{name} has invalid color #{value}")
    end
  end)
end)

missing_fixture = read_json.(Path.join(dir, "theme.missing_required_token.fixture.json"))
missing_fixture_tokens = Map.get(missing_fixture, "tokens", %{})

if (required_tokens -- Map.keys(missing_fixture_tokens)) == [] do
  abort.("missing-token fixture should be invalid")
end

invalid_color = read_json.(Path.join(dir, "theme.invalid_color.fixture.json"))
invalid_color_values = Map.get(invalid_color, "tokens", %{}) |> Map.values()

if Enum.all?(invalid_color_values, fn v -> String.match?(v, hex_color_re) end) do
  abort.("invalid-color fixture should include bad color")
end

IO.puts("theme contract valid")
