package canaryprism.mcwm.saves;

public enum Gamemode {
    survival("Survival Mode"), 
    creative("Creative Mode"), 
    adventure("Adventure Mode"), 
    spectator("Spectator Mode"),
    hardcore("Hardcore Mode!");

    public final String name;

    private Gamemode(String name) {
        this.name = name;
    }

    public static Gamemode fromIndex(int index) {
        return switch (index) {
            case 0 -> survival;
            case 1 -> creative;
            case 2 -> adventure;
            case 3 -> spectator;
            default -> throw new IllegalArgumentException("Invalid gamemode index: " + index);
        };
    }
}
