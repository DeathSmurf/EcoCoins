# EcoCoins + Interaction Secondary (Type: EcoCoins_CoinRedeem)

Esta guía define la configuración válida para canjear monedas físicas en EcoCoins.

## Tipo obligatorio

En el item, el `Type` debe ser exactamente:

```json
"Type": "EcoCoins_CoinRedeem"
```

Ruta completa:

`BlockType > Interactions > Secondary > Interactions > 0 > Type > EcoCoins_CoinRedeem`

## Plantilla mínima

```json
"Interactions": {
  "Secondary": {
    "Interactions": [
      {
        "Type": "EcoCoins_CoinRedeem",
        "Effects": {
          "LocalSoundEventId": "SFX_UI_Craft"
        }
      }
    ]
  }
}
```

## Requisitos de canje

1. El item físico debe existir y estar registrado en tu asset pack.
2. El `itemId` del item debe coincidir exactamente con `name_item` en `EcoCoins/Coins/*.json`.
3. EcoCoins debe registrar correctamente `EcoCoins_CoinRedeem` en `setup()`.
4. TheEconomy debe estar disponible en runtime.

## Verificación rápida

- En startup debes ver: `interaction type registrado: EcoCoins_CoinRedeem`.
- Si ese log no aparece, el canje no funcionará hasta corregir el registro.
