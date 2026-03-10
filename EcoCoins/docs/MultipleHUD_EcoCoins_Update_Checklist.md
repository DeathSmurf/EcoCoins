# EcoCoins + MultipleHUD: checklist de estabilidad para actualizaciones

> Objetivo: evitar que nuevas actualizaciones rompan el login con errores de Custom UI,
> especialmente `Could not find document HUD/MultipleHUD.ui`.

## 1) Modelo lógico (cómo debe funcionar)

### 1.1 Flujo esperado en EcoCoins
1. `EcoCoins.setup()` llama `HudHelper.init()` una vez al arrancar plugin.
2. Si `MultipleHUD` está disponible, `HudHelper` cachea métodos reflectivos (`setCustomHud` / `hideCustomHud`).
3. En join, `BalanceHudService.showOnJoin()` crea/reutiliza `EcoCoinBalanceHud`, aplica estado y registra HUD vía `HudHelper.setCustomHud(...)`.
4. En cambios de balance, `BalanceHudService.updateBalance()` solo hace `hud.render()` cuando hay cambios visibles.

### 1.2 Flujo esperado en MultipleHUD (referencia)
- `MultipleCustomUIHud.build(...)` hace `uiCommandBuilder.append("HUD/MultipleHUD.ui")`.
- Luego encapsula HUDs individuales con prefijos y hace `update(...)`.

Implicación: si el runtime no resuelve el documento `HUD/MultipleHUD.ui`, el cliente se puede desconectar durante GameLoading.

---

## 2) Invariantes que NO se deben romper

### 2.1 Invariantes de EcoCoins
- Mantener **un único** HUD de balance activo: `Pages/EcoCoins_BalanceHud.ui`.
- No reintroducir variantes Left/Right ni lógica de toggle de posición.
- No re-registrar HUD en cada update de balance (registrar en join, actualizar estado luego).
- Mantener fallback vanilla en `HudHelper.setCustomHud(...)` si falla reflexión.

### 2.2 Invariantes de integración
- `HudHelper.init()` debe ejecutarse en `setup()` antes de uso del HUD.
- Si hay excepción reflectiva en MultipleHUD, nunca dejar al plugin en estado roto (fallback a vanilla).
- No meter cambios de UI root IDs innecesarios sin validar compatibilidad de selectors.

---

## 3) Checklist previo a merge (obligatorio)

## 3.1 Código
- [ ] `EcoCoins.setup()` sigue llamando `HudHelper.init()`.
- [ ] `BalanceHudService.showOnJoin()` registra vía `HudHelper.setCustomHud(...)`.
- [ ] `BalanceHudService.updateBalance()` solo renderiza si `applyBalanceState(...)` retorna `true`.
- [ ] No existe `ChangePositionCommand` ni referencias a `/changeposition`.
- [ ] No existen archivos `EcoCoins_BalanceHud_Left.ui` / `EcoCoins_BalanceHud_Right.ui`.

## 3.2 Assets
- [ ] `assets.json` contiene solo `Common/UI/Custom/Pages/EcoCoins_BalanceHud.ui` para el HUD de balance.
- [ ] El HUD principal compila/carga sin errores de selector o sintaxis.

## 3.3 Runtime (manual)
- [ ] Arranca server con EcoCoins + MultipleHUD + mods dependientes.
- [ ] Login completo hasta world load sin `Failed to apply CustomUI HUD commands`.
- [ ] No aparece `Could not find document HUD/MultipleHUD.ui` en cliente.
- [ ] HUD muestra balance correctamente tras pickup/cambio de dinero.

---

## 4) Checklist de diagnóstico rápido (si vuelve el error)

1. Confirmar versión/jar realmente desplegado (evitar jar viejo en server).
2. Confirmar que el stacktrace sale del camino MultipleHUD (`HUD/MultipleHUD.ui`).
3. Confirmar que EcoCoins no reintrodujo left/right o toggles de posición.
4. Probar login sin MultipleHUD para aislar si el problema está en integración o en HUD base.
5. Revisar orden/carga de mods y conflictos de assetpacks.

---

## 5) Política de cambios para futuros agentes (Codex/ChatGPT)

- Antes de tocar HUD, leer este checklist completo.
- No hacer cambios “creativos” de estructura UI sin justificar impacto runtime.
- Priorizar compatibilidad con patrón Ecotale ya validado en este repo:
  - registro único en join,
  - updates incrementales por cambio,
  - fallback vanilla en fallos de reflexión.
- Si se propone workaround temporal, documentarlo y dejar plan para revertirlo.

---

## 6) Comandos útiles de verificación rápida

```bash
# Referencias peligrosas (no deberían existir)
rg -n "changeposition|BalanceHud_Left|BalanceHud_Right|ChangePositionCommand" src/main

# Flujo principal de HUD
rg -n "HudHelper\.init\(|setCustomHud\(|updateBalance\(|applyBalanceState\(" src/main/java

# Assets HUD
cat src/main/assetpack/assets.json
```
