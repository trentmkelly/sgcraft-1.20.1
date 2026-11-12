package gcewing.sg;

public enum IrisState {
    Open, Closing, Closed, Opening;

    static final IrisState[] VALUES = values();

    public static IrisState valueOf(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : Open;
    }
}
