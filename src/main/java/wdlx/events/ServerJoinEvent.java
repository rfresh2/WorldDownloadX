package wdlx.events;

public record ServerJoinEvent() {
    public static final ServerJoinEvent INSTANCE = new ServerJoinEvent();
}
