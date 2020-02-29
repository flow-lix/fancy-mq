package learn.architecture.remoting.common;

/**
 * 支持3种SSL模式
 */
public enum TlsMode {

    /**
     * 不支持SSL连接
     */
    DISABLE("disable"),

    /**
     * 允许任何连接
     */
    PERMISSIVE("permissive"),

    /**
     * 必须使用SSL连接
     */
    ENFORCE("enforce");

    private final String name;

    TlsMode(String name) {
        this.name = name;
    }

    public static TlsMode parse(String name) {
        for (TlsMode mode : TlsMode.values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return PERMISSIVE;
    }
    public String getName() {
        return name;
    }
}
