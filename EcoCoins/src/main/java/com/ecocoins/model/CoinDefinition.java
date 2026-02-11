package com.ecocoins.model;

import java.util.List;

public final class CoinDefinition {
    public String name_item;      // itemId del stack físico
    public int stack_item;        // tamaño de stack (informativo; el asset pack decide)
    public double pay;            // valor digital a agregar por moneda
    public MoneyName money_name;  // nombres para comando
    public String DrawType;       // "Model" / "Icon" (opcional)

    public static final class MoneyName {
        public String primary;
        public List<String> aliases;
    }
}
