# EcoCoins HUD Logic (flujo que sí funciona)

Este documento deja únicamente la lógica mínima que debe respetarse para que el HUD de EcoCoins se imprima sin errores.

## 1) Setup obligatorio
1. En `EcoCoins.setup()` se ejecuta `HudHelper.init()` una sola vez.
2. `HudHelper.init()` intenta enlazar MultipleHUD por reflexión.
3. Si MultipleHUD no está disponible, el sistema mantiene fallback a HUD vanilla.

## 2) Flujo de render en join
1. `BalanceHudService.showOnJoin(player, playerRef)` valida `player` y `playerRef`.
2. Obtiene/crea la instancia HUD por jugador (`computeIfAbsent`).
3. Aplica estado inicial con `applyBalanceState(balance)`.
4. Registra el HUD con `HudHelper.setCustomHud(player, playerRef, hud)`.

## 3) Flujo de actualización de balance
1. `BalanceHudService.updateBalance(player, playerRef)` recalcula estado con `applyBalanceState(balance)`.
2. Solo si hay cambio visible (`changed == true`) ejecuta `hud.render()`.
3. No se re-registra el HUD en cada cambio.

## 4) Contrato de integración de `HudHelper`
- `setCustomHud(...)`
  - Con MultipleHUD disponible: usa `MultipleHUD.setCustomHud(...)`.
  - Si falla o no existe MultipleHUD: usa `player.getHudManager().setCustomHud(...)`.
- `hideCustomHud(...)`
  - Con MultipleHUD disponible: usa `MultipleHUD.hideCustomHud(...)`.
  - Sin MultipleHUD: salida segura sin romper flujo.

## 5) Estructura UI mínima que debe conservarse
- Documento HUD: `Common/UI/Custom/Pages/EcoCoins_BalanceHud.ui`
- Root group esperado: `#BalancePanel`
- IDs que el HUD actualiza:
  - `CurrencyName`
  - `BalanceSymbol`
  - `BalanceAmount`

Si se respeta este orden lógico (init -> join/register -> update/render) y esta estructura UI, el HUD se mantiene estable en EcoCoins.
