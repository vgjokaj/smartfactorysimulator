package infinity.smartfactorysimulator.event;

import infinity.smartfactorysimulator.DataType;
import infinity.smartfactorysimulator.Noise;
import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class EventData {

    public enum ConditionType {
        AFTER_ITEMS, AFTER_SECONDS
    }

    private String id;
    private DataType type;
    private ConditionType conditionType;
    private Integer conditionValue;
    private Noise noise;

}
