package gcewing.sg;

public enum SGState {
    Idle, Dialling, Transient, Connected, Disconnecting, InterDialling;

    static final SGState[] VALUES = values();

    public static SGState valueOf(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : Idle;
    }
}
