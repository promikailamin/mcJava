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
- Matching villagers receive the `CreateVillager` tag and the visible name tag `CreateVillager` in bold gold text
- When a villager becomes a CreateVillager, conversion sounds and particles play and a message is broadcast in chat
- Tagged villagers attempt to spawn one iron golem every 5 seconds when a valid spawn position exists
- Each iron golem spawn plays its own sounds and particles without sending another chat message
- Iron golems only spawn on top of cobblestone, mud block, or hopper blocks; water or flowing water directly above a valid block is ignored, making that position valid for spawning
- Does not require beds, village membership, gossip, panic, nearby mobs, professions, or other vanilla golem eligibility checks
- When a tagged villager is no longer standing on an iron block, its tag, name tag, and golem-spawning power are removed
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

### 11. TheTrader Villager
- The mod checks all villagers every tick for one standing on top of an emerald block
- A villager named `TheTrader` with a name tag also becomes a TheTrader
- Matching villagers receive the `TheTrader` tag and, when created with an emerald block, the visible name tag `TheTrader` in bold green text
- When a villager becomes a TheTrader, conversion sounds and particles play and a message is broadcast in chat
- A TheTrader picks up nearby dropped items and instantly drops emeralds using these trades:
  - 8 sticks for 1 emerald
  - 4 planks of any wood type for 1 emerald
  - 1 log of any wood type for 1 emerald
  - 1 iron ingot for 1 emerald
  - 1 diamond for 9 emeralds
- Emeralds are never picked up or consumed by a TheTrader
- Leftover items that do not complete a trade remain on the ground
- When a TheTrader created with an emerald block is no longer standing on it, its tag and name tag are removed
- Other villagers retain vanilla behavior

### 12. TheDestroyer Villager
- The mod checks all villagers every tick for one standing on top of a TNT block
- Matching villagers receive the `TheDestroyer` tag and the visible name tag `TheDestroyer` in bold red text
- When a villager becomes a TheDestroyer, conversion sounds and particles play and a message is broadcast in chat
- A TheDestroyer attempts to spawn one creeper every 5 seconds when a valid spawn position exists
- Creepers only spawn on top of cobblestone, mud block, or hopper blocks
- Each creeper spawn plays its own sounds and particles
- When a TheDestroyer is no longer standing on a TNT block, its tag, name tag, and creeper-spawning power are removed
- Other villagers retain vanilla behavior

## Installation

1. Install Fabric Loader 0.19.5+ for Minecraft 26.2 (requires Java 25)
2. Install Fabric API 0.161.0+26.2
3. Place `CoreServerMod-1.0.0.jar` in your server's `mods` folder
4. Start the server - a default configuration file will be generated at `config/coreservermod.json`

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
  "destroyer_creeper_spawn_enabled": true,
  "destroyer_creeper_spawn_interval_seconds": 5,
  "max_automatic_creepers_per_area": 10
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
| `destroyer_creeper_spawn_enabled` | `true` | Enable creeper spawning from TNT block villagers |
| `destroyer_creeper_spawn_interval_seconds` | `5` | Interval between creeper spawn attempts (seconds) |
| `max_automatic_creepers_per_area` | `10` | Maximum automatic creepers per area |

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

## License

MIT License

## Issues

Report issues at: https://github.com/promikailamin/mcJava/issues