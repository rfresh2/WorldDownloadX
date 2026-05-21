package wdlx.events;

public record DisconnectEvent() {
    public static final DisconnectEvent INSTANCE = new DisconnectEvent();
}
