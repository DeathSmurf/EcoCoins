# EcoCoins — integración con ítems de otro mod.zip

Para que EcoCoins convierta una moneda física a dinero virtual, el ítem debe cumplir estas reglas:

1. Estar registrado en tu `mod.zip`.
2. Tener interacción `Secondary` activa.
3. Tener un `itemId` que coincida con `name_item` en `EcoCoins/Coins/*.json`.

## Flujo de canje desde la mano

1. Si el `itemId` en mano coincide con `name_item` (JSON), EcoCoins toma el `pay` de esa moneda.
2. EcoCoins consume `x1` del ítem físico en inventario.
3. EcoCoins deposita el balance virtual (`pay`).
4. Si el depósito falla, EcoCoins hace rollback y devuelve `x1` ítem.

## Trigger requerido

EcoCoins procesa monedas físicas solo con:

- `InteractionType.Secondary`

> Nota: actualmente el código también acepta `InteractionType.Use` como compatibilidad adicional.

## Tipos que EcoCoins ignora

EcoCoins ignora otros tipos como `Primary`, `Ability1`, `Ability2`, `Ability3`, `Pick`, etc.

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

## Diagnóstico de canje (logs)

Si no deposita balance al usar la moneda, revisa logs con prefijo `"[EcoCoins][Redeem]"`:

- `ignorado: itemId no mapeado ...` → `name_item` no coincide con el item real.
- `fallo: no se pudo remover x1 ...` → no pudo consumir la moneda del inventario.
- `fallo: depósito virtual falló ...` → TheEconomy rechazó el depósito.
- `ok: canjeado ...` → el canje terminó correctamente.


## Referencia directa de Secondary (BooksAndPapers)

Si quieres replicar el patrón de `Interaction Secondary: Type: Simple`, revisa:

- `docs/reference/BooksAndPapers/src/main/resources/Server/Item/Items/Books/Books_And_Papers_Book.json`
- guía de EcoCoins: `docs/reference/EcoCoins_SecondaryInteraction_Guide.md`
- plantilla lista para copiar: `docs/reference/templates/Template_EcoCoins_Coin_Item.json`

## Nodos de permisos (LuckPerms)

- `ecocoins.redeem.use`  
  Permite canjear monedas físicas desde la mano al balance digital.
- `ecocoins.command.change.use`  
  Permite usar `/change <moneda>` y `/change <moneda> <cantidad>`.
- `ecocoins.command.change.list`  
  Permite usar `/change` para ver monedas y `pay`.
- `ecocoins.command.changeall.use`  
  Permite usar `/changeall` para canjear todas las monedas físicas del inventario.
- `ecocoins.command.changeposition.use`
  Permite usar `/changeposition` para alternar la esquina del HUD.
- `ecocoins.command.changeoff.use`
  Permite usar `/changeoff` para ocultar el HUD.
- `ecocoins.command.changeon.use`
  Permite usar `/changeon` para volver a mostrar el HUD.

## Comandos disponibles

- `/change` → muestra la lista de monedas y su valor (`pay`).
- `/change <moneda>` → compra 1 moneda física.
- `/change <moneda> <cantidad>` → compra una cantidad específica.
- `/changeall` → canjea todas las monedas físicas del inventario al balance digital.

## Sonido de canje (redeem)

EcoCoins reproduce `SFX_EcoCoins_Redeem` al canjear balance.

Rutas de configuración (sí van en el repo):
- `src/main/assetpack/Server/Audio/SoundEvents/SFX_EcoCoins_Redeem.json`

> Importante: no dupliques el mismo `SFX_EcoCoins_Redeem` en otra carpeta, porque puede fallar la validación por ID de asset duplicado.

Ruta del audio (no se versiona en este entorno):
- `src/main/assetpack/Common/Sounds/EcoCoins/Redeem.ogg`

Pasos para dejarlo funcionando en tu servidor:
1. Copia tu `Redeem.ogg` en `ExportedAssetPack/Common/Sounds/EcoCoins/Redeem.ogg`.
2. Verifica que exista `ExportedAssetPack/Server/Audio/SoundEvents/SFX_EcoCoins_Redeem.json`.
3. Reinicia el servidor.


## UI de balance (HUD)

EcoCoins ahora muestra un HUD de balance en la esquina inferior, siguiendo la referencia visual de Ecotale.

### Comandos de HUD

- `/changeposition` → alterna entre esquina inferior derecha e inferior izquierda.
- `/changeoff` → oculta el HUD de balance.
- `/changeon` → vuelve a mostrar el HUD de balance.

### Compatibilidad con MultipleHUD

EcoCoins usa el mod **MultipleHUD** cuando está instalado para permitir múltiples HUD en pantalla al mismo tiempo.
Si no está presente, EcoCoins usa el HUD vanilla (un solo HUD custom).

Referencia de uso publicada por MultipleHUD:

`MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud1", new TestUIHUD());`
`MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud2", new TestUIHUD());`

Maven: https://maven.hytale-mods.dev/
