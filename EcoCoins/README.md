# EcoCoins — integración con items definidos en otro mod.zip

Sí: si quieres que este `mod.jar` ejecute la lógica al **usar la moneda en la mano**, tu item en el otro `mod.zip` debe disparar la interacción **`Use`**.

## Qué necesita EcoCoins para funcionar con tus monedas (`Coin_Copper`, `Coin_Bronze`, etc.)

1. El item debe existir en tu segundo `mod.zip`.
2. El `itemId` de ese item debe coincidir con `name_item` en `EcoCoins/Coins/*.json`.
3. En `Interactions`, debes tener la key **`Use`** configurada para que el evento llegue al plugin.

## `Interactions -> Use`: qué elegir

- **Reference Existing**: recomendado en la mayoría de casos.
  - Reutilizas un comportamiento existente y aseguras que el input `Use` se dispare.
- **Create Embedded**: solo si necesitas comportamiento custom del asset.
  - Sirve, pero no es obligatorio para EcoCoins.

> Lo importante para este plugin es que se dispare `InteractionType.Use` y que el `itemId` coincida.

## Checklist rápido

- [ ] `name_item` coincide con el item real registrado en tu otro mod.
- [ ] El item tiene `Interactions -> Use` activo.
- [ ] `pay > 0` y `money_name.primary` definido en cada moneda.
