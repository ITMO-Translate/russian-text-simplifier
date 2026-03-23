package itmo.code.service.helper;

import java.util.Map;

public final class LevelRanges {

    private LevelRanges() {
    }

    public static final Map<String, LevelRange> TARGET_RANGES = Map.of(
        "B1", new LevelRange(1.80, 3.21),
        "B2", new LevelRange(3.22, 5.00)
    );

    public static LevelRange getTargetRange(String level) {
        return TARGET_RANGES.getOrDefault(level, new LevelRange(0, 0));
    }

    public static boolean isInsideTarget(String level, double value) {
        LevelRange range = getTargetRange(level);
        return value >= range.min() && value <= range.max();
    }

    public record LevelRange(double min, double max) {
    }
}