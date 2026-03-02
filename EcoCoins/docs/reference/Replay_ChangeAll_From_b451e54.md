# Replay de cambios (desde `b451e54` hasta la versión con `/changeall`)

Este documento resume los cambios que debes reaplicar en un entorno que quedó en `b451e54`.

## Archivos que se agregan/modifican

1. **Nuevo comando**
   - `src/main/java/com/ecocoins/commands/ChangeAllMoneyCommand.java`
   - Implementa `/changeall`:
     - Cuenta monedas EcoCoins del inventario.
     - Remueve ítems físicos.
     - Deposita total a TheEconomy.
     - Rollback de inventario si falla depósito.
     - Reproduce `SFX_EcoCoins_Redeem` en éxito.

2. **Registro del comando**
   - `src/main/java/com/ecocoins/EcoCoins.java`
   - Registrar `new ChangeAllMoneyCommand(languageManager, coinManager, economy)` en `setup()`.

3. **Idiomas**
   - `src/main/assetpack/EcoCoins/Languages/en_US.json`
   - `src/main/assetpack/EcoCoins/Languages/es_ES.json`
   - Agregar claves `command.changeall.*`.

4. **Resolución de idioma robusta**
   - `src/main/java/com/ecocoins/core/LanguageManager.java`
   - Compatibilidad entre formatos `en-US` y `en_US` sin exigir renombre físico de archivos.
   - `defaultLang` inicial toma `Locale.getDefault().toLanguageTag()`.

5. **Config idioma por defecto**
   - `src/main/assetpack/EcoCoins/Languages/languages.json`
   - `default_language` en formato configurable (`es-ES`).

6. **README**
   - `README.md`
   - Documentar permiso `ecocoins.command.changeall.use` y comando `/changeall`.
   - Nota de compatibilidad sobre `InteractionType.Use`.

## Nota importante para merge

- Puedes mantener físicamente los archivos como `en_US.json` / `es_ES.json` para evitar conflictos de renombre.
- El código ya queda tolerante a ambos formatos de locale (`-` y `_`).
