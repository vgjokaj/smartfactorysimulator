package infinity.smartfactorysimulator.machine;

import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class Quality {

    public enum QualityType {
        MIN, MAX, RANGE
    }

    private QualityType type;
    private Double value1;
    private Double value2;

}
