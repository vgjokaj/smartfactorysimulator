package infinity.smartfactorysimulator.machine;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class MachineGroup {

    private String id;
    private Machine selectedMachine;
    private List<Machine> machines = new ArrayList<>();
    
}
