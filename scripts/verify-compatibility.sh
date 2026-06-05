#!/usr/bin/env bash
set -euo pipefail

readonly EXPECTED_JAVA_MAJOR=69
readonly EXPECTED_API_VERSION=26.1.2
readonly MAIN_CLASS=com.jolly.hoverworth.HoverWorth

if [[ "$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p')" != "25" ]]; then
  echo "Compatibility build requires Java 25." >&2
  exit 1
fi

mvn -B clean package "$@"

mapfile -t plugin_jars < <(find target -maxdepth 1 -type f -name 'HoverWorth-*.jar' ! -name 'original-*' -print)
if [[ ${#plugin_jars[@]} -ne 1 ]]; then
  echo "Expected exactly one packaged HoverWorth jar, found ${#plugin_jars[@]}." >&2
  exit 1
fi
readonly plugin_jar="${plugin_jars[0]}"

major_version="$(javap -verbose -classpath "$plugin_jar" "$MAIN_CLASS" | sed -n 's/^[[:space:]]*major version: //p')"
if [[ "$major_version" != "$EXPECTED_JAVA_MAJOR" ]]; then
  echo "Expected Java 25 class major version $EXPECTED_JAVA_MAJOR, found $major_version." >&2
  exit 1
fi

plugin_yml="$(unzip -p "$plugin_jar" plugin.yml)"
if ! grep -Fq "api-version: '$EXPECTED_API_VERSION'" <<<"$plugin_yml"; then
  echo "Packaged plugin.yml does not target Paper API $EXPECTED_API_VERSION." >&2
  exit 1
fi
if ! grep -Fq 'depend: [packetevents]' <<<"$plugin_yml"; then
  echo "Packaged plugin.yml does not require PacketEvents." >&2
  exit 1
fi
if jar tf "$plugin_jar" | grep '^com/github/retrooper/packetevents/' >/dev/null; then
  echo "PacketEvents classes must not be shaded into the plugin jar." >&2
  exit 1
fi

echo "Verified $plugin_jar for Java 25 and Paper $EXPECTED_API_VERSION."
