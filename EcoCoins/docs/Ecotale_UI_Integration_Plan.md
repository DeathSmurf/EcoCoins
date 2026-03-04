# Investigación: usar el sistema de UI de Ecotale dentro de EcoCoins

## Resumen ejecutivo
Sí, **es viable** reutilizar el sistema de UI de Ecotale en EcoCoins con cambios moderados, porque ambos mods ya están sobre la API de servidor de Hytale y Ecotale separa la UI en:
1. archivos `.ui` (layout/estilos), y
2. clases Java (`InteractiveCustomUIPage` y `SimpleHud`) que actualizan valores y eventos.

La forma más segura para EcoCoins es hacerlo en dos fases:
- **Fase 1 (rápida):** HUD de balance EcoCoin usando un wrapper tipo `SimpleHud` y un `.ui` propio.
- **Fase 2 (interactivo):** pantalla de conversión/canje inspirada en `PayGui`, pero conectada a `CoinManager + TheEconomyService` de EcoCoins.

---

## Qué usa exactamente Ecotale para UI

### 1) UI interactiva (pantallas completas)
Ecotale usa `InteractiveCustomUIPage` para una pantalla de pago con:
- carga de layout (`cmd.append("Pages/Ecotale_PayPage.ui")`),
- bindings de eventos (`ValueChanged`, `Activating`),
- refresco incremental (`sendUpdate`) al cambiar input o selección.

Referencia: `docs/reference/Ecotale/src/main/java/com/ecotale/gui/PayGui.java`.

### 2) HUD persistente (overlay)
Ecotale usa `BalanceHud extends SimpleHud` para mostrar balance en pantalla:
- UI base: `Pages/Ecotale_BalanceHud.ui`,
- actualización por `setText(...)` + `pushUpdates()`,
- animación del número con `HudScheduler`.

Referencias:
- `docs/reference/Ecotale/src/main/java/com/ecotale/hud/BalanceHud.java`
- `docs/reference/Ecotale/src/main/java/com/ecotale/lib/simplehud/SimpleHud.java`
- `docs/reference/Ecotale/src/main/java/com/ecotale/lib/simplehud/HudScheduler.java`

### 3) Estilos reutilizables
Ecotale centraliza estilos/colores en `Ecotale_Common.ui` y los importa desde otras páginas.

Referencia: `docs/reference/Ecotale/src/main/resources/Common/UI/Custom/Pages/Ecotale_Common.ui`.

---

## Compatibilidad con EcoCoins actual

EcoCoins hoy ya tiene el backend de economía y canje físico:
- `CoinManager` (definiciones de monedas y mapeo por itemId),
- `TheEconomyService` (puente a economía digital),
- comandos `/change` y `/changeall`.

Referencias:
- `src/main/java/com/ecocoins/core/CoinManager.java`
- `src/main/java/com/ecocoins/core/TheEconomyService.java`
- `src/main/java/com/ecocoins/commands/ChangeMoneyCommand.java`
- `src/main/java/com/ecocoins/commands/ChangeAllMoneyCommand.java`

Conclusión: **el backend necesario ya existe**. Falta la capa de UI + wiring de eventos.

---

## Estrategia recomendada para EcoCoins

## Fase 1: HUD EcoCoin (bajo riesgo, alto valor)

### Objetivo
Mostrar el balance EcoCoin del jugador en pantalla con estética Ecotale (o variante EcoCoins).

### Implementación sugerida
1. **Copiar/adaptar infraestructura HUD**
   - Crear `com.ecocoins.ui.simplehud.SimpleHud` y `HudScheduler` (basado en referencia Ecotale).
2. **Crear HUD específico**
   - `com.ecocoins.ui.hud.EcoCoinBalanceHud` con `setText("BalanceAmount", ...)`.
3. **Crear layout UI**
   - Nuevo archivo: `src/main/assetpack/Common/UI/Custom/Pages/EcoCoins_BalanceHud.ui`.
4. **Agregar estilos comunes**
   - Opcional: `EcoCoins_Common.ui` para no acoplar nombres `Ecotale_*`.
5. **Actualizar HUD cuando cambie balance**
   - Cada flujo que haga `economy.add/remove` debe disparar update de HUD.
   - Primera integración: `/change` y `/changeall`.

### Riesgos
- Múltiples HUD simultáneos por jugador si no se centraliza un registro (`Map<UUID, Hud>`).
- Rebuild excesivo de UI (impacto de rendimiento) si no se usan updates incrementales.

---

## Fase 2: Pantalla interactiva de canje/compra EcoCoin

### Objetivo
UI visual para:
- listar monedas (`CoinManager.getCoinsSnapshot()`),
- elegir moneda + cantidad,
- previsualizar costo,
- confirmar compra (equivalente a `/change`).

### Implementación sugerida
1. Crear `EcoCoinsChangeGui extends InteractiveCustomUIPage<Data>`.
2. Diseñar `EcoCoins_ChangePage.ui` (lista + input cantidad + botón confirmar).
3. Bindings de eventos:
   - `ValueChanged` para cantidad/filtros,
   - `Activating` para seleccionar moneda y confirmar.
4. Reusar lógica de negocio actual:
   - Validaciones de inventario,
   - validación de fondos,
   - retiro de saldo + entrega de ítems,
   - rollback en error.

**Importante:** no duplicar reglas. Extraer la lógica de `ChangeMoneyCommand` a un servicio compartido para que comando y GUI usen el mismo flujo.

---

## Cambios estructurales que conviene hacer antes

1. **Servicio transaccional único**
   - Crear algo como `CoinTradeService` con método:
     - `TradeResult buyCoin(UUID player, String moneyName, int amount)`.
   - Usado por `/change`, `/changeall` y futura GUI.

2. **Registro central de HUDs**
   - Similar a `BalanceHudSystem` de Ecotale.

3. **Configuración UI en JSON**
   - Feature flags:
     - `ui.enableHud`
     - `ui.enableChangeGui`
     - `ui.theme`.

---

## Checklist técnico para empaquetado de UI en EcoCoins

1. Asegurar que los `.ui` estén en recursos del plugin final.
2. Verificar rutas relativas usadas por `cmd.append("Pages/...")`.
3. Si se agregan texturas, incluirlas en assetpack y validar IDs.
4. Verificar que el servidor encuentre los assets al iniciar.

Nota: hoy EcoCoins usa `sourceSets.main.resources.srcDir "src/main/assetpack"`; por eso los nuevos `.ui` deben colocarse dentro de ese árbol para entrar al JAR.

Referencia: `build.gradle`.

---

## Propuesta concreta de primer milestone (2-4 horas)

1. Crear `EcoCoins_BalanceHud.ui` minimalista.
2. Implementar `EcoCoinBalanceHud` sin animación (actualización instantánea).
3. Mostrar HUD al entrar jugador.
4. Actualizar HUD en `/change` y `/changeall` tras operación exitosa.
5. Añadir toggle por config para apagar HUD.

Con esto validas pipeline completo de UI en EcoCoins sin tocar todavía UI interactiva compleja.

---

## Conclusión

Sí se puede integrar el UI de Ecotale en EcoCoins y es técnicamente consistente con la base actual del proyecto. La recomendación es comenzar por HUD de balance (simple, reusable) y luego migrar a una pantalla interactiva de canje basada en el patrón de `PayGui`, reutilizando la lógica económica ya existente para evitar duplicación y errores.
