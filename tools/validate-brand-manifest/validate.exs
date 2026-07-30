#!/usr/bin/env elixir

root = Path.expand("../..", __DIR__)
dir = Path.join([root, "shared-contracts", "brand"])
schema_path = Path.join(dir, "brand_manifest_schema.json")
sample_path = Path.join(dir, "sample.defdo-telecom.dev.manifest.json")

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

dig_path = fn map, path ->
  path
  |> String.split(".")
  |> Enum.reduce(map, fn
    key, node when is_map(node) -> Map.get(node, key)
    _key, _node -> nil
  end)
end

unless File.regular?(schema_path) do
  abort.("missing brand schema")
end

unless File.regular?(sample_path) do
  abort.("missing sample brand manifest")
end

schema = read_json.(schema_path)
manifest = read_json.(sample_path)

missing =
  Map.fetch!(schema, "required_fields")
  |> Enum.reject(fn path -> dig_path.(manifest, path) end)

unless missing == [] do
  abort.("brand manifest missing fields: #{Enum.join(missing, ", ")}")
end

android_app_id = manifest |> Map.get("android", %{}) |> Map.get("application_id", "")
if to_string(android_app_id) == "" do
  abort.("android application_id must be non-empty")
end

ios_bundle_id = manifest |> Map.get("ios", %{}) |> Map.get("bundle_id", "")
if to_string(ios_bundle_id) == "" do
  abort.("ios bundle_id must be non-empty")
end

auth_client_id = manifest |> Map.get("auth", %{}) |> Map.get("client_id", "")
if to_string(auth_client_id) == "" do
  abort.("auth client_id must be public metadata")
end

IO.puts("brand manifest valid")
