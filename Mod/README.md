# Bazaar Alert

A Fabric **1.21.11** client mod for Hypixel SkyBlock that watches your Bazaar buy orders and sell offers and alerts you when someone overbids or undercuts you.

## Features

- Tracks your active Bazaar **buy orders** and **sell offers**
- Polls the public Hypixel Bazaar API every few seconds
- Sends a **chat message** when you are outbid or undercut
- Plays a **sound** when an alert triggers
- Syncs orders automatically when you open **Manage Orders** in the Bazaar
- Also picks up new orders from `[Bazaar] Buy Order Setup!` / `Sell Offer Setup!` chat messages

## How to use

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11
3. Build this mod with `./gradlew build` (or use the JAR from `build/libs/`)
4. Drop the JAR into your `.minecraft/mods` folder
5. Join Hypixel SkyBlock and open the Bazaar → **Manage Orders** once to sync your current orders

The mod only runs checks while you are on Hypixel SkyBlock.

## Alerts

When someone beats your price, you will see a message like:

```
[Bazaar Alert] Your buy order Wheat was overbid! (You: 5.0, Top: 5.1)
```

## Configuration

Settings are in `BazaarAlertConfig.java` for now:

| Setting | Default | Description |
|---------|---------|-------------|
| `chatAlerts` | `true` | Show chat notifications |
| `soundAlerts` | `true` | Play a pling sound |
| `pollIntervalSeconds` | `5` | How often to check the API |
| `onlyOnHypixel` | `true` | Only poll while on Hypixel SkyBlock |

## Building

```bash
./gradlew build
```

Output JAR: `build/libs/bazaar-alert-1.0.0.jar`

## Notes

- This mod only **reads** public Bazaar API data and your in-game order GUI. It does not automate clicks or place orders.
- Hypixel API data can lag slightly behind in-game prices.
- Open **Manage Orders** again after cancelling orders so the mod stays in sync.
