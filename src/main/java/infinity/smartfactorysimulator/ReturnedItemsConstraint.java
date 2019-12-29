package infinity.smartfactorysimulator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Valon Gjokaj
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnedItemsConstraint {
    
    private String machineId;
    private double qualityThresholdMinValue;
    private double qualityThresholdMaxValue;
}
