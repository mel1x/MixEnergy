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

## Config
In the latest versions of the mod, a config has been added that allows you to set the initial energy values, regeneration speed, and, if desired, disable the impact of energy on block breaking or attacking entities.

### Example
```
[general]
  defaultMaxEnergy = 45.0
  energyCostForBreakingBlocks = true
  energyCostForAttacks = true
  energyRegenCooldown = 1500
```


## Gameplay Impact

This mod challenges players to manage their energy wisely, adding a survival aspect even in non-survival game modes. Whether you're mining deep underground, battling mobs, or building your dream base, energy management becomes crucial to success.

Enhance your Minecraft experience with **MixEnergy** and test your resourcefulness like never before!

<details>
<summary>Tags</summary>
Stamina, Stamina system, Energy, Player actions, Player stamina, Player energy, Running energy Minecraft, Building energy Minecraft, Attacking energy Minecraft
</details>
