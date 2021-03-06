package dev.ttetris;

public enum ActivityState {
    LOADING("LOADING", 0),
    MENU("MENU", 1),
    GAME("GAME", 2),
    TOP_SCORES("TOP_SCORES", 3);

    private String name;
    private int val;
    private ActivityState(String str, int val) {
        this.name = str;
        this.val = val;
    }
}
