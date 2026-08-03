package lunatech.jetrtp.model;

public record CachedLocation(
    String profileName,
    String worldName,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {}
