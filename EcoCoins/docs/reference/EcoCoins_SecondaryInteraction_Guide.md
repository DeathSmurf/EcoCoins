# EcoCoins + Interaction Secondary (Type: Simple)

Esta guía documenta el patrón que usa **BooksAndPapers** y cómo aplicarlo en **EcoCoins** para canjear monedas desde la mano del jugador.

## 1) Qué hace BooksAndPapers

En `Books_And_Papers_Book.json` el item define una interacción en `Interactions -> Secondary` con una lista de acciones:

1. una interacción `Type: "Simple"` (efecto local)
2. una interacción `Type: "OpenCustomUI"` (abre UI)

Ejemplo real (resumido):

```json
"Interactions": {
  "Secondary": {
    "Interactions": [
      {
        "Type": "Simple",
        "Effects": {
          "LocalSoundEventId": "SFX_Books_And_Papers_Open"
        }
      },
      {
        "Type": "OpenCustomUI",
        "Page": {
          "Id": "Books_And_Papers_Book"
        }
      }
    ]
  }
}
```

## 2) Qué necesita EcoCoins para canjear

EcoCoins escucha `PlayerInteractEvent` y procesa el canje cuando:

- `event.getActionType() == InteractionType.Secondary`
- el `itemId` del item en mano coincide con `name_item` en `EcoCoins/Coins/*.json`

Si coincide:

1. consume `x1` moneda física,
2. deposita `pay` al balance digital,
3. si falla el depósito, intenta rollback devolviendo la moneda.

## 3) Plantilla mínima para tus monedas físicas

En el JSON del item físico (el que vive en tu asset pack de items), usa:

```json
"Interactions": {
  "Secondary": {
    "Interactions": [
      {
        "Type": "Simple",
        "Effects": {
          "LocalSoundEventId": "SFX_UI_Craft"
        }
      }
    ]
  }
}
```

También puedes usar el alias de EcoCoins:

```json
"Type": "EcoCoins_CoinRedeem"
```

Ese alias se registra en `setup()` como `SimpleInteraction`.

## 4) Checklist rápido

- El item físico existe en el mod.zip (registrado correctamente).
- Tiene `Interactions.Secondary.Interactions[0].Type = "Simple"` (o `EcoCoins_CoinRedeem`).
- El `itemId` exacto del item coincide con `name_item` en `EcoCoins/Coins/*.json`.
- TheEconomy está disponible en runtime.

## 5) Caso típico de error

Si hay `Secondary` pero no deposita:

- normalmente el `itemId` real del item no coincide con `name_item`.
- revisa logs con prefijo `[EcoCoins][Redeem]` para ver si fue ignorado o falló el depósito.
