# EcoCoins — integración con ítems de otro `mod.zip`

Esta guía explica cómo configurar monedas físicas para que EcoCoins las convierta en balance virtual sin confusión.

## Requisitos del ítem físico

Para que una moneda se pueda canjear, el ítem debe cumplir **todo** esto:

1. Estar registrado en tu `mod.zip`.
2. Tener interacción `Secondary` activa.
3. Usar `Type: "EcoCoins_CoinRedeem"` en la ruta de interacción.
4. Tener un `itemId` que coincida con `name_item` en `EcoCoins/Coins/*.json`.

## Trigger de canje

EcoCoins procesa monedas físicas solo con:

- `InteractionType.Secondary`

## Type que debes usar en el ítem

EcoCoins registra el type custom `EcoCoins_CoinRedeem` en `setup()` usando `getCodecRegistry(Interaction.CODEC).register(...)`.

Configura el ítem así:

- `BlockType > Interactions > Secondary > Interactions > 0 > Type`
  - `EcoCoins_CoinRedeem`

## Flujo de canje (runtime)

1. Si el `itemId` en mano coincide con `name_item` (JSON), EcoCoins toma el `pay` de esa moneda.
2. EcoCoins consume `x1` del ítem físico en inventario.
3. EcoCoins deposita el balance virtual (`pay`).
4. Si el depósito falla, EcoCoins hace rollback y devuelve `x1` ítem.

## Referencia de `name_item`

Usa el ID real exacto del ítem registrado por tu asset pack.

- Si el ítem es `Coin_Copper` → `"name_item": "Coin_Copper"`
- Si el ítem es `mycoins:Coin_Copper` → `"name_item": "mycoins:Coin_Copper"`

## Verificación rápida en consola

Al iniciar el plugin deberías ver:

- `interactionTrigger=Secondary`
- `interaction type registrado: EcoCoins_CoinRedeem`

Si no aparecen, probablemente estás ejecutando un `.jar` viejo o falló el registro del interaction type.

## Diagnóstico de canje (logs)

Si no deposita balance al usar la moneda, revisa logs con prefijo `"[EcoCoins][Redeem]"`:

- `ignorado: itemId no mapeado ...` → `name_item` no coincide con el ítem real.
- `fallo: no se pudo remover x1 ...` → no pudo consumir la moneda del inventario.
- `fallo: depósito virtual falló ...` → TheEconomy rechazó el depósito.
- `ok: canjeado ...` → el canje terminó correctamente.

## Referencias útiles

- guía de EcoCoins: `docs/reference/EcoCoins_SecondaryInteraction_Guide.md`
- plantilla lista para copiar: `docs/reference/templates/Template_EcoCoins_Coin_Item.json`


## Timeout de comandos (/change, /changeall y /changehand)

EcoCoins aplica un sistema de espera antes de ejecutar:

- `/change <moneda>` y `/change <moneda> <cantidad>`
  - default: `5s`
  - VIP (`ecocoins.vip`): `3s`
- `/changeall`
  - default: `15s`
  - VIP (`ecocoins.vip`): `7s`
- `/changehand`
  - default: `15s`
  - VIP (`ecocoins.vip`): `7s`

Permisos con prioridad:

1. `ecocoins.timepass` → sin espera (ejecución inmediata)
2. `ecocoins.vip` → espera corta
3. sin ambos → espera por defecto

Durante la espera, el jugador debe permanecer en el mismo bloque donde ejecutó el comando.
Si cambia a otro bloque antes de terminar el tiempo, el comando se cancela.
Si el jugador se desconecta durante la espera, también se cancela.

## Nodos de permisos (LuckPerms)

- `ecocoins.redeem.use`  
  Permite canjear monedas físicas desde la mano al balance digital.
- `ecocoins.command.change.use`  
  Permite usar `/change <moneda>` y `/change <moneda> <cantidad>`.
- `ecocoins.command.change.list`  
  Permite usar `/change` para ver monedas y `pay`.
- `ecocoins.command.changeall.use`  
  Permite usar `/changeall` para canjear todas las monedas físicas del inventario.
- `ecocoins.command.changehand.use`
  Permite usar `/changehand` para canjear solo monedas de la hotbar (slots 1-9).
- `ecocoins.vip`
  Aplica timeout corto en `/change`, `/changeall` y `/changehand`.
- `ecocoins.timepass`
  Omite timeout en `/change`, `/changeall` y `/changehand` (ejecución inmediata).
- `ecocoins.command.reload.use`
  Permite usar `/ecocoins reload` para recargar Coins/Languages en caliente.
- `ecocoins.command.changeoff.use`
  Permite usar `/changeoff` para ocultar el HUD.
- `ecocoins.command.changeon.use`
  Permite usar `/changeon` para volver a mostrar el HUD.

## Comandos disponibles

- `/change` → muestra la lista de monedas y su valor (`pay`).
- `/change <moneda>` → compra 1 moneda física.
- `/change <moneda> <cantidad>` → compra una cantidad específica.
- `/changeall` → canjea todas las monedas físicas del inventario al balance digital.
- `/changehand` → canjea solo monedas físicas en hotbar (slots 1-9) al balance digital.
- `/changeoff` → oculta el HUD de balance.
- `/changeon` → vuelve a mostrar el HUD de balance.
- `/ecocoins reload` → recarga Coins/Languages sin reiniciar servidor (assets requieren reinicio).

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
