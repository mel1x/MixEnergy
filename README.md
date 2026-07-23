# MixEnergy Mod

The **MixEnergy Mod** introduces a unique mechanic to Minecraft: **player energy**. This energy system adds a new layer of challenge and strategy by making energy a consumable resource for various actions. 

<p align="center">
  <img src="https://cdn.modrinth.com/data/fFAmfqIe/images/e0d938047412cab349c32a9f334333fe9fa3006d.gif" />
</p>

## Key Features

- **Energy Consumption**: Players will now spend energy on the following actions:
  - **Running**: Energy is drained while sprinting.
  - **Breaking Blocks**: Mining and harvesting require energy.
  - **Building Blocks**: Placing blocks consumes energy.
  - **Attacking**: Performing attacks depletes energy.

  If the player runs out of energy, these actions will be temporarily **disabled** until energy is restored.

- **Energy Commands**:
  - `/setEnergy <value> <player:optional>`: Set a specific player's energy level. If no player is specified, it defaults to the executing player.
  - `/setMaxEnergy <value> <player:optional>`: Adjust the maximum energy limit for a player. Similar to `/setEnergy`, the command defaults to the executing player if no target is provided.

- **Energy HUD**

- **_In creative mode energy doesn't working_**

## Configuration

MixEnergy creates two files in the Minecraft or dedicated-server `config` directory:

- `mixenergy-common.toml` contains authoritative gameplay rules. On a dedicated
  server, edit this file on the server.
- `mixenergy-client.toml` contains the local HUD position.

In single-player, every energy source can also be changed from the mod settings
screen. Changes are applied and saved immediately. During multiplayer, gameplay
toggles are shown as server-controlled while the local HUD position remains
editable.

### Example

```toml
[general]
defaultMaxEnergy = 45.0
# 20 server ticks = 1 second at normal TPS
energyRegenCooldownTicks = 30

[energy_sources]
sprinting = true
fastSwimming = true
breakingBlocks = true
placingBlocks = true
attacks = true
jumping = false

[energy_costs]
# Movement costs are charged every server tick
sprintingPerTick = 0.25
fastSwimmingPerTick = 0.25
breakingBlock = 2.0
placingBlock = 1.0
attack = 3.0
jump = 1.0

[regeneration]
# Restored per regeneration pulse; one pulse normally occurs every 3 ticks
baseRate = 1.0
maxRate = 1.8
speedMultiplier = 1.0 # 0 disables passive regeneration
```

The HUD position is stored separately:

```toml
[hud]
energyBarPosition = "ABOVE_HOTBAR"
```

Available positions are `ABOVE_HOTBAR`, `TOP_LEFT`, `TOP_RIGHT`, `TOP_CENTER`,
`BOTTOM_LEFT`, and `BOTTOM_RIGHT`.


## Gameplay Impact

This mod challenges players to manage their energy wisely, adding a survival aspect even in non-survival game modes. Whether you're mining deep underground, battling mobs, or building your dream base, energy management becomes crucial to success.

Enhance your Minecraft experience with **MixEnergy** and test your resourcefulness like never before!

<details>
<summary>Tags</summary>
Stamina, Stamina system, Energy, Player actions, Player stamina, Player energy, Running energy Minecraft, Building energy Minecraft, Attacking energy Minecraft
</details>
