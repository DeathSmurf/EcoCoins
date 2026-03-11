# Guía funcional: programar HUD con MultipleHUD en EcoCoins

Esta nota describe **solo** el flujo que funcionó en EcoCoins.

## 1) Objetivo
Mostrar un HUD de balance estable usando el patrón funcional probado:
- detección opcional de MultipleHUD,
- registro de HUD al entrar,
- actualización del HUD solo cuando cambia el estado visible.

---

## 2) Flujo funcional (paso a paso)

### Paso A: Inicializar compatibilidad en setup
En `EcoCoins.setup()` se debe llamar una vez:
- `HudHelper.init();`

`HudHelper.init()`:
1. intenta cargar `com.buuz135.mhud.MultipleHUD`,
2. obtiene `getInstance()`,
3. cachea métodos reflectivos:
   - `setCustomHud(Player, PlayerRef, String, CustomUIHud)`
   - `hideCustomHud(Player, PlayerRef, String)`
4. si falla, se usa fallback vanilla automáticamente.

---

### Paso B: Registrar HUD en join
En `BalanceHudService.showOnJoin(...)`:
1. obtener/crear instancia por jugador (`computeIfAbsent`),
2. aplicar estado inicial (`applyBalanceState(...)`),
3. registrar HUD con `HudHelper.setCustomHud(player, playerRef, hud)`.

Este registro se hace una vez por ciclo de conexión del jugador.

---

### Paso C: Actualizar HUD en cambios de balance
En `BalanceHudService.updateBalance(...)`:
1. recalcular estado (`applyBalanceState(...)`),
2. si cambió visualmente (`changed == true`), hacer `hud.render()`.

La actualización es incremental por estado; no re-registra HUD en cada cambio.

---

## 3) Contrato de `HudHelper`

## `setCustomHud(...)`
- si MultipleHUD está disponible: invoca `MultipleHUD.setCustomHud(...)` por reflexión;
- si falla o no está disponible: usa `player.getHudManager().setCustomHud(...)`.

## `hideCustomHud(...)`
- si MultipleHUD está disponible: invoca `hideCustomHud(...)` por reflexión;
- si no, se considera best-effort (sin romper flujo).

---

## 4) Estructura UI usada

EcoCoins quedó funcionando con:
- una sola página HUD: `Pages/EcoCoins_BalanceHud.ui`
- root group compatible: `Group #BalancePanel { ... }`
- actualización por ids de texto:
  - `CurrencyName`
  - `BalanceSymbol`
  - `BalanceAmount`

---

## 5) Referencia mínima de implementación

- Inicialización: `EcoCoins.setup() -> HudHelper.init()`
- Registro en join: `BalanceHudService.showOnJoin() -> HudHelper.setCustomHud(...)`
- Update por cambio: `BalanceHudService.updateBalance() -> hud.render()` cuando `applyBalanceState(...)` retorna `true`.

Con este flujo, el HUD queda desacoplado y estable para runtime con/sin MultipleHUD.
