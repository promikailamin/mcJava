# CoreServerMod

A Fabric server-side mod for Minecraft Java Edition 1.21.4 that accelerates various game mechanics to reduce waiting times and automate repetitive tasks.

## Features

### 1. Sugar Cane on Soul Sand
- Allows sugar cane placement on soul sand
- Grows by one block approximately every second when planted on soul sand
- Preserves vanilla growth behavior on other valid substrates

### 2. Accelerated Tree Growth
- All vanilla saplings and propagules grow into their correct trees within approximately 15 seconds
- Requires valid environmental and space conditions

### 3. Iron Block Villager
- Villagers standing on iron blocks become special golem-spawning villagers
- Attempts to spawn exactly one iron golem every 100 server ticks (5 seconds) when a valid spawn position exists
- Does not require beds, village membership, gossip, panic, nearby mobs, professions, or other vanilla golem eligibility checks
- Moving off the iron block disables special behavior; standing on one again reactivates it with a fresh 5-second interval
- Other villagers retain vanilla behavior

### 4. Accelerated Crop Growth
- Applicable vanilla crops and plants grow to maturity within approximately 15 seconds
- Preserves necessary soil, light, water, and plant-specific requirements

### 5. Animal Breeding
- Removes breeding cooldown for supported breedable vanilla animals
- Allows repeated breeding whenever normal eligibility conditions are met
- Baby animals mature within approximately 15 seconds or instantly when fed once (configurable)

### 6. Sheep Wool Regrowth
- Sheep regrow wool and become shearable approximately every 5 seconds
- Preserves normal shearing drops and prevents duplicate drops

### 7. Villager Emerald Drops
- Villagers have a 40% chance to drop one emerald on successful trade
- Evaluates chance once per trade event and prevents duplicate rewards

### 8. Guaranteed Chicken from Eggs
- Every player-thrown chicken egg spawns exactly one chicken
- Bypasses vanilla random hatch chance without duplicate spawning

### 9. Chicken Egg Production
- Adult chickens produce one egg approximately every 20 seconds regardless of location or movement
- Baby chickens do not lay eggs

## Installation

1. Install Fabric Loader 0.16.9+ for Minecraft 1.21.4
2. Install Fabric API 0.118.0+1.21.4
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
  "chicken_egg_interval_seconds": 20
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

## Building from Source

Requirements:
- Java 21+
- Gradle 8.11+

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/` and `dist/`.

## Test Server

A test server setup is included in the `test-server/` directory with scripts:

- `build.sh` - Build the mod and copy to test server
- `start-test-server.sh` - Start the test server
- `stop-test-server.sh` - Stop the test server

## Compatibility

- Minecraft: 1.21.4
- Fabric Loader: 0.16.9+
- Fabric API: 0.118.0+1.21.4
- Java: 21+

## License

MIT License

## Issues

Report issues at: https://github.com/promikailamin/mcJava/issues