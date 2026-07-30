#!/usr/bin/env elixir

root = Path.expand("../..", __DIR__)
dir = Path.join([root, "shared-contracts", "auth"])

required = [
  "defdo_auth_mobile_contract.json",
  "pkce.test_vectors.json",
  "discovery.success.fixture.json",
  "discovery.invalid.fixture.json",
  "authorization_request.expected.json",
  "authorization_request.golden_url.json",
  "token.success.fixture.json",
  "token.error.fixture.json",
  "refresh.success.fixture.json",
  "refresh.invalid_grant.fixture.json",
  "revoke.success.fixture.json",
  "revoke.failure.fixture.json",
  "auth_error_normalization.json",
  "auth_redaction_rules.json"
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
  abort.("missing auth contract files: #{Enum.join(missing, ", ")}")
end

contract = read_json.(Path.join(dir, "defdo_auth_mobile_contract.json"))
required_params = ["response_type", "client_id", "redirect_uri", "scope", "state", "code_challenge", "code_challenge_method"]
missing_params = required_params -- Map.get(contract, "required_authorization_params", [])

unless missing_params == [] do
  abort.("auth contract missing params: #{Enum.join(missing_params, ", ")}")
end

unless Map.get(contract, "code_challenge_method") == "S256" do
  abort.("auth contract must use S256")
end

unless Map.get(contract, "oracle") == "defdo_auth_client" do
  abort.("defdo_auth_client must remain oracle")
end

unless Map.get(contract, "runtime_mobile_sdk") == false do
  abort.("auth contract must not be runtime SDK")
end

expected = read_json.(Path.join(dir, "authorization_request.expected.json"))
missing_expected = required_params -- Map.keys(expected)

unless missing_expected == [] do
  abort.("authorization fixture missing fields: #{Enum.join(missing_expected, ", ")}")
end

unless Map.get(expected, "response_type") == "code" do
  abort.("authorization fixture must use response_type=code")
end

unless Map.get(expected, "code_challenge_method") == "S256" do
  abort.("authorization fixture must use S256")
end

golden = read_json.(Path.join(dir, "authorization_request.golden_url.json"))

unless Map.has_key?(golden, "expected_url") do
  abort.("golden URL fixture missing expected_url")
end

if String.contains?(Map.get(golden, "expected_url"), "+") do
  abort.("golden URL must use %20 for spaces")
end

success = read_json.(Path.join(dir, "discovery.success.fixture.json"))
["issuer", "authorization_endpoint", "token_endpoint", "revocation_endpoint"]
|> Enum.each(fn field ->
  unless Map.has_key?(success, field) do
    abort.("discovery success missing #{field}")
  end
end)

invalid_grant = read_json.(Path.join(dir, "refresh.invalid_grant.fixture.json"))

unless Map.get(invalid_grant, "requires_login") == true do
  abort.("invalid_grant must require login")
end

unless Map.get(invalid_grant, "clear_local_session") == true do
  abort.("invalid_grant must clear local session")
end

revoke_failure = read_json.(Path.join(dir, "revoke.failure.fixture.json"))

unless Map.get(revoke_failure, "clear_local_session") == true do
  abort.("revoke failure must still clear local session")
end

redaction = read_json.(Path.join(dir, "auth_redaction_rules.json"))

["access_token", "refresh_token", "id_token", "code", "code_verifier", "claims"]
|> Enum.each(fn key ->
  redact_keys = Map.get(redaction, "redact_keys", [])

  unless key in redact_keys do
    abort.("redaction rules missing #{key}")
  end
end)

unless Map.has_key?(redaction, "redact_headers") do
  abort.("redaction rules missing redact_headers")
end

unless Map.has_key?(redaction, "redact_callback_url") do
  abort.("redaction rules missing redact_callback_url")
end

unless Map.has_key?(redaction, "redact_claims_values") do
  abort.("redaction rules missing redact_claims_values")
end

redact_headers = Map.get(redaction, "redact_headers", [])

unless "authorization" in redact_headers do
  abort.("redaction headers missing authorization")
end

unless "set-cookie" in redact_headers do
  abort.("redaction headers missing set-cookie")
end

IO.puts("auth contract valid")
