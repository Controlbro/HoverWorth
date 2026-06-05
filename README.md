# HoverWorth

HoverWorth adds configurable worth and description lore to items shown in vanilla inventories without modifying the server-side item.

## Requirements

- **Java 25**
- **Paper 26.1.2**
- **PacketEvents 2.12.1** installed as a server plugin

The plugin is compiled with Java 25 bytecode and declares Paper API `26.1.2`, so older Java runtimes and older Paper versions are intentionally unsupported.

## Build

Build and validate the distributable plugin jar with Java 25:

```bash
./scripts/verify-compatibility.sh
```

The compatibility check builds the plugin and verifies that the resulting jar:

- contains Java 25 class files;
- declares Paper API `26.1.2` and the PacketEvents runtime dependency; and
- does not accidentally shade PacketEvents into HoverWorth.

The distributable jar is written to `target/HoverWorth-1.5.jar`.
