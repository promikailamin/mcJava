# CoreServerMod

A Fabric server-side mod for Minecraft Java Edition 26.2 that accelerates various game mechanics to reduce waiting times and automate repetitive tasks.

## Features

### 1. Sugar Cane on Soul Sand
- Allows sugar cane placement on soul sand
- Grows by one block approximately every second when planted on soul sand
- Preserves vanilla growth behavior on other valid substrates

### 2. Accelerated Tree Growth
- All vanilla saplings and propagules grow into their correct trees within approximately 15 seconds
- Requires valid environmental and space conditions

### 3. Iron Block Villager
- The mod checks all villagers every tick for one standing on top of an iron block
- Matching villagers receive the `CreateVillager` tag and a floating two-line hologram showing the name and spawn countdown
- When a villager becomes a CreateVillager, conversion sounds and particles play and a message is broadcast in chat
- Tagged villagers attempt to spawn one iron golem every 5 seconds when a valid spawn position exists
- Spawn areas are searched up to 16 blocks horizontally and 4 vertically; chat messages announce immediately when an area is found or lost
- Iron golems only spawn on top of cobblestone, mud block, or hopper blocks
- A spawn position must be air or water with no buttons, trapdoors, slabs, or other non-air blocks in the mob's space; water above a valid base is allowed
- Does not require beds, village membership, gossip, panic, nearby mobs, professions, or other vanilla golem eligibility checks
- When a tagged villager is no longer standing on an iron block, its tag, hologram, and golem-spawning power are removed
- Other villagers retain vanilla behavior

### 4. Bad Villager
- The mod checks all villagers every tick for one standing on top of a redstone block
- Matching villagers receive the `BadVillager` tag and the visible name tag `BadVillager` in bold dark red text
- When a villager becomes a BadVillager, conversion sounds and particles play and a message is broadcast in chat
- A BadVillager is invincible and takes no damage from any source while it remains on the redstone block
- Hitting a BadVillager gives the attacker a random-level Bad Omen effect lasting 20 minutes, which can start a raid
- When a BadVillager is no longer standing on a redstone block, its tag, name tag, and invincibility are removed
- Other villagers retain vanilla behavior

### 5. Accelerated Crop Growth
- Applicable vanilla crops and plants grow to maturity within approximately 15 seconds
- Preserves necessary soil, light, water, and plant-specific requirements

### 6. Animal Breeding
- Removes breeding cooldown for supported breedable vanilla animals
- Allows repeated breeding whenever normal eligibility conditions are met
- Baby animals mature within approximately 15 seconds or instantly when fed once (configurable)

### 7. Sheep Wool Regrowth
- Sheep regrow wool and become shearable approximately every 5 seconds
- Preserves normal shearing drops and prevents duplicate drops

### 8. Villager Emerald Drops
- Villagers have a 40% chance to drop one emerald on successful trade
- Evaluates chance once per trade event and prevents duplicate rewards

### 9. Guaranteed Chicken from Eggs
- Every player-thrown chicken egg spawns exactly one chicken
- Bypasses vanilla random hatch chance without duplicate spawning

### 10. Chicken Egg Production
- Adult chickens produce one egg approximately every 20 seconds regardless of location or movement
- Baby chickens do not lay eggs

### 11. Create Fly Integration (Optional)
- When [Create Fly](https://modrinth.com/mod/create-fly) is installed, the mod automatically enables Create recipe and washing integration
- Adds two Mechanical Press (Compacting) recipes and two Mechanical Mixer (Mixing) recipes:
  - 1 copper ingot + 1 iron ingot + 1 wheat seeds -> 1 emerald
  - 1 sugar cane + 1 snowball -> 1 slime ball
- Dropped items in flowing water are washed using Create's existing Splashing recipes
- This includes gravel -> flint, ice -> packed ice, and every other item that Create can wash with a fan and water
- Flowing-water washing is faster than fan washing and does not require a fan, leaves, or a water source block
- Only flowing water works; still source blocks are ignored
- The integration is optional - CoreServerMod still loads and works normally without Create Fly

## Installation

1. Install Fabric Loader 0.19.5+ for Minecraft 26.2 (requires Java 25)
2. Install Fabric API 0.161.0+26.2
3. Optionally install Create Fly 6.0.9+ for 26.2 to enable the Create recipes and flowing-water washing
4. Place `CoreServerMod-1.0.0.jar` in your server's `mods` folder
5. Start the server - a default configuration file will be generated at `config/coreservermod.json`

## Configuration

The mod generates a configuration file at `config/coreservermod.json` on first launch. All features can be toggled and intervals adjusted:

```json
{
  "sugar_cane_soul_sand_enabled": true,
  "sugar_cane_growth_interval_seconds": 1,
  "sapling_growth_time_seconds": 15,
  "crop_growth_time_seconds": 15,
  "iron_golem_spawn_enabled": true,
  "iron_golem_spawn_interval_seconds": 5,
  "max_automatic_golems_per_area": 10,
  "animal_breeding_cooldown_disabled": true,
  "baby_growth_time_seconds": 15,
  "instant_growth_when_fed": true,
  "sheep_wool_regrowth_seconds": 5,
  "villager_emerald_drop_enabled": true,
  "villager_emerald_drop_chance": 0.4,
  "guaranteed_chicken_from_egg": true,
  "chicken_egg_interval_seconds": 20,
  "create_flowing_water_washing_enabled": true,
  "create_flowing_water_washing_interval_ticks": 10
}
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `sugar_cane_soul_sand_enabled` | `true` | Enable sugar cane placement/growth on soul sand |
| `sugar_cane_growth_interval_seconds` | `1` | Growth interval for sugar cane on soul sand (seconds) |
| `sapling_growth_time_seconds` | `15` | Time for saplings to grow into trees (seconds) |
| `crop_growth_time_seconds` | `15` | Time for crops to reach maturity (seconds) |
| `iron_golem_spawn_enabled` | `true` | Enable iron golem spawning from iron block villagers |
| `iron_golem_spawn_interval_seconds` | `5` | Interval between golem spawn attempts (seconds) |
| `max_automatic_golems_per_area` | `10` | Maximum automatic golems per village area |
| `animal_breeding_cooldown_disabled` | `true` | Remove animal breeding cooldown |
| `baby_growth_time_seconds` | `15` | Time for baby animals to mature (seconds) |
| `instant_growth_when_fed` | `true` | Instantly grow baby animals when fed breeding item |
| `sheep_wool_regrowth_seconds` | `5` | Time for sheep to regrow wool (seconds) |
| `villager_emerald_drop_enabled` | `true` | Enable emerald drops from villager trades |
| `villager_emerald_drop_chance` | `0.4` | Chance (0.0-1.0) for emerald drop per trade |
| `guaranteed_chicken_from_egg` | `true` | Guarantee chicken spawn from thrown eggs |
| `chicken_egg_interval_seconds` | `20` | Interval for chicken egg laying (seconds) |
| `create_flowing_water_washing_enabled` | `true` | Enable Create Splashing recipes in flowing water when Create Fly is installed |
| `create_flowing_water_washing_interval_ticks` | `10` | Ticks an item must stay in flowing water before it is washed (lower is faster) |

## Building from Source

Requirements:
- Java 25+
- Gradle 9.8.1+ (the included wrapper downloads the correct version)

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/` and `dist/`.

## Continuous Integration

The GitHub Actions workflows live in `.github/workflows/`:

- **Build** (`build.yml`) runs on pushes to `main`, pull requests, and manual dispatch. It builds the mod, uploads the JAR as the `coreservermod-build` artifact, and publishes the JAR to `dist/` and `test-server/mods/` on `main`.
- **Test** (`test.yml`) is started manually with workflow dispatch. It downloads the artifact from the latest successful Build run and boots the test server with it. If no build artifact is available, the Test run cancels itself.

## Test Server

A test server setup is included in the `test-server/` directory with scripts:

- `build.sh` - Build the mod and copy to test server
- `start-test-server.sh` - Start the test server
- `stop-test-server.sh` - Stop the test server

## Compatibility

- Minecraft: 26.2
- Fabric Loader: 0.19.5+
- Fabric API: 0.161.0+26.2
- Java: 25+
- Create Fly: 6.0.9+ for 26.2 (optional, enables recipes and flowing-water washing)

## License

MIT License

## Issues

Report issues at: https://github.com/promikailamin/mcJava/issues