# EcoCoins — integración con ítems de otro mod.zip

Para que EcoCoins convierta una moneda física a dinero virtual, el ítem debe cumplir estas reglas:

1. Estar registrado en tu `mod.zip`.
2. Tener interacción `Secondary` activa.
3. Tener un `itemId` que coincida con `name_item` en `EcoCoins/Coins/*.json`.

## Trigger requerido

EcoCoins procesa monedas físicas únicamente con:

- `InteractionType.Secondary`

## Tipos que EcoCoins ignora

Aunque el engine emita otros tipos (`Use`, `Primary`, `Ability1`, etc.), este plugin no los procesa para redimir monedas.

## Type recomendado para `Secondary` en tu item

EcoCoins registra el type custom `EcoCoins_CoinRedeem` (alias de `SimpleInteraction`) en `setup()` usando `getCodecRegistry(Interaction.CODEC).register(...)`.

- En `BlockType > Interactions > Secondary > Interactions > 0 > Type` puedes usar:
  - `EcoCoins_CoinRedeem` (recomendado), o
  - `Simple` si no quieres depender del alias.

## Referencia de `name_item`

Usa el ID real exacto del ítem registrado por tu asset pack.

- Si el ítem es `Coin_Copper` → `"name_item": "Coin_Copper"`
- Si el ítem es `mycoins:Coin_Copper` → `"name_item": "mycoins:Coin_Copper"`

## Verificación rápida en consola

Al iniciar el plugin deberías ver `interactionTrigger=Secondary` en los logs de arranque.
Si no aparece, probablemente estás ejecutando un `.jar` viejo.

Si en startup no ves el log `interaction type registrado: EcoCoins_CoinRedeem`, usa `Simple` en el `Type` del item y revisa warnings de EcoCoins para saber por qué el alias no se registró.
