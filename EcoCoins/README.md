# EcoCoins — integración con items definidos en otro mod.zip

Si quieres que este `mod.jar` ejecute la lógica al **usar la moneda en la mano**, el item en tu `mod.zip` debe disparar interacción de moneda.

## Qué necesita EcoCoins para funcionar con tus monedas (`Coin_Copper`, `Coin_Bronze`, etc.)

1. El item debe existir en tu segundo `mod.zip`.
2. El `itemId` de ese item debe coincidir con `name_item` en `EcoCoins/Coins/*.json`.
3. En `Interactions`, debe existir el trigger que usa tu item.

## Importante: trigger de interacción

Este plugin procesa monedas físicas solo con:
- `InteractionType.Secondary`

Esto está alineado con items registrados en `Secondary` en tu mod.zip.

## ¿Cómo referenciar de manera correcta?

La referencia correcta es: **el ID real del item que registra tu mod.zip**.

### Regla práctica

- Si tu item se registra como `Coin_Copper`, usa:
  - `"name_item": "Coin_Copper"`
- Si se registra como `mycoins:Coin_Copper`, usa preferentemente:
  - `"name_item": "mycoins:Coin_Copper"`

> EcoCoins tolera comparaciones con/sin namespace, pero para evitar confusiones lo mejor es copiar exactamente el ID que ves en el registro del item.

### Ejemplo completo (recomendado)

```json
{
  "name_item": "mycoins:Coin_Copper",
  "money_name": {
    "primary": "copper",
    "aliases": ["cobre", "c"]
  },
  "pay": 1,
  "stack_item": 99
}
```

## `Interactions`: qué elegir

- **Reference Existing**: recomendado en la mayoría de casos.
  - Reutilizas comportamiento base y aseguras que el input se dispare.
- **Create Embedded**: solo si necesitas comportamiento custom del asset.

> Lo importante para este plugin es que se dispare `Secondary` y que el `itemId` coincida.

## Checklist rápido

- [ ] `name_item` coincide con el item real registrado en tu otro mod.
- [ ] El item tiene activa interacción `Secondary`.
- [ ] `pay > 0` y `money_name.primary` definido en cada moneda.
