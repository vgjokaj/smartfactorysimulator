package infinity.smartfactorysimulator.config;

import infinity.smartfactorysimulator.ReturnedItems;
import infinity.smartfactorysimulator.event.EventSource;
import infinity.smartfactorysimulator.machine.Machine;
import infinity.smartfactorysimulator.machine.MachineGroup;
import infinity.smartfactorysimulator.machine.QualityGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class Config {

    private String mqttServerURI;
    private String mqttUsername;
    private String mqttPassword;
    private String rootTopic;
    private Map<String, Machine> machines = new HashMap<>();
    private Map<String, MachineGroup> machineGroups = new HashMap<>();
    private QualityGate qualityGate;
    private ReturnedItems returnedItems;
    private List<EventSource> eventSources = new ArrayList<>();
    private List<String> productionLine = new ArrayList<>();

}
