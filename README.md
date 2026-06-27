# Kingdom Come Combat

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

## Vanilla entity attack allowlist

Entity types in the data-pack tag below always use Minecraft's vanilla attack:

`data/kingdom_come_combat/tags/entity_type/always_vanilla_attackable.json`

The built-in list contains `#minecraft:boat`, `minecraft:item_frame`,
`minecraft:glow_item_frame`, and `minecraft:painting`. A data pack can append
entity IDs or entity-type tags with `"replace": false`, or replace the full
list with `"replace": true`.
