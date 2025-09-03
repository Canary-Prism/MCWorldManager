package canaryprism.mcwm.saves;

public enum Gamemode {
    SURVIVAL("Survival Mode"),
    CREATIVE("Creative Mode"),
    ADVENTURE("Adventure Mode"),
    SPECTATOR("Spectator Mode"),
    HARDCORE("Hardcore Mode!");

    public final String name;

    Gamemode(String name) {
        this.name = name;
    }

    public static Gamemode fromIndex(int index) {
        return switch (index) {
            case 0 -> SURVIVAL;
            case 1 -> CREATIVE;
            case 2 -> ADVENTURE;
            case 3 -> SPECTATOR;
            default -> throw new IllegalArgumentException("Invalid gamemode index: " + index);
        };
    }
}
