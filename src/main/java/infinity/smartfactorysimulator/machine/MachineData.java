package infinity.smartfactorysimulator.machine;

import infinity.smartfactorysimulator.Noise;
import infinity.smartfactorysimulator.DataType;
import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class MachineData {

    private String id;
    private DataType type;
    private String value;
    private Noise noise;
    private Quality qualityConstraint;

}
