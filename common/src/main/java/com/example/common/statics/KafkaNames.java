package com.example.common.statics;

public abstract class KafkaNames {
    public static final String ORDER_COMMANDS = "order-commands";
    public static final String ORDER_EVENTS = "order-events";

    public static final String INVENTORY_COMMANDS = "inventory-commands";
    public static final String INVENTORY_EVENTS = "inventory-events";

    public static final String PAYMENT_COMMANDS = "payment-commands";
    public static final String PAYMENT_EVENTS = "payment-events";

    public static final String SAGA_EVENTS = "orchestrator-events";//Currently only completed event

    //Groups
    public static final String ORCHESTRATOR_GROUP = "orchestrator-group";
    public static final String ORDER_GROUP = "order-service-group";
    public static final String INVENTORY_GROUP = "inventory-service-group";
    public static final String PAYMENT_GROUP = "payment-service-group";

    //Groups for SAGA_EVENTS
    public static final String INVENTORY_SAGA_GROUP = "inventory-orchestrator-group";
}
