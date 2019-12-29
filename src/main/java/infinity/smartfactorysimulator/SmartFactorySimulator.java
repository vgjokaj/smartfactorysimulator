package infinity.smartfactorysimulator;

import infinity.smartfactorysimulator.event.EventSource;
import infinity.smartfactorysimulator.machine.Machine;
import infinity.smartfactorysimulator.config.Config;
import infinity.smartfactorysimulator.machine.MachineGroup;
import infinity.smartfactorysimulator.machine.QualityGate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author Valon Gjokaj
 */
public class SmartFactorySimulator {

    public static void main(String[] args) throws MqttException, IOException {
        System.out.println("");
        System.out.println("--------------------------");
        System.out.println("--- Starting Simulator ---");
        System.out.println("--------------------------");
        System.out.println("");
        
        final Config config = getConfig(args);
        String curveTemplatesFolderPath = getFixedCurvesTemplatesFolderPath(args);
        MqttUtils.updateMqttConnectOptions(config);

        final Map<String, Machine> machines = getMachines(config, curveTemplatesFolderPath);
        final Map<String, MachineGroup> machineGroups = getMachineGroups(config, curveTemplatesFolderPath);
        final QualityGate qualityGate = getQualityGate(config, machines, machineGroups);
        final ReturnedItems returnedItems = getReturnedItems(config, machines);
        final List<String> productionLine = config.getProductionLine();
        final List<EventSource> eventSources = getEventSources(config, machines, machineGroups);

        startSimulation(machines, machineGroups, qualityGate, returnedItems, productionLine, eventSources);
    }

    private static Config getConfig(String[] args) throws IOException {
        Jsonb jsonb = JsonbBuilder.create();

        String configPath = Constants.CONFIG_DEFAULT_PATH;
        if (args.length > 0) {
            configPath = args[0];
        }

        String jsonString = new String(Files.readAllBytes(Paths.get(configPath)));
        return jsonb.fromJson(jsonString, Config.class);
    }

    private static String getFixedCurvesTemplatesFolderPath(String[] args) {
        String curveTemplatesFolderPath = Constants.CONFIG_FIXED_CURVES_DEFAULT_FOLDER_PATH;
        if (args.length > 1) {
            curveTemplatesFolderPath = args[1];
        }

        return curveTemplatesFolderPath;
    }

    private static Map<String, Machine> getMachines(Config config, String curveTemplatesFolderPath) {
        Map<String, Machine> machines = config.getMachines();

        machines.entrySet().forEach(entry -> {
            entry.getValue().setMqttClient(MqttUtils.createMqttClient(Constants.SIMULATOR_NAME + "-" + entry.getValue().getId()));
            entry.getValue().setTopic(config.getRootTopic() + "/machines/" + entry.getValue().getId());
            entry.getValue().setFixedCurvesFolderPath(curveTemplatesFolderPath);
        });

        return machines;
    }

    private static Map<String, MachineGroup> getMachineGroups(Config config, String curveTemplatesFolderPath) {
        Map<String, MachineGroup> machineGroups = config.getMachineGroups();

        machineGroups.entrySet().forEach(entry -> {
            entry.getValue().getMachines().forEach(machine -> {
                machine.setMqttClient(MqttUtils.createMqttClient(Constants.SIMULATOR_NAME + "-" + machine.getId()));
                machine.setTopic(config.getRootTopic() + "/machines/" + machine.getId());
                machine.setFixedCurvesFolderPath(curveTemplatesFolderPath);
            });
        });

        return machineGroups;
    }

    private static QualityGate getQualityGate(Config config, Map<String, Machine> machines, Map<String, MachineGroup> machineGroups) {
        final QualityGate qualityGate = config.getQualityGate();
        qualityGate.setTopic(config.getRootTopic() + "/qualityGate/" + qualityGate.getId());
        qualityGate.setMqttClient(MqttUtils.createMqttClient(Constants.SIMULATOR_NAME + "-" + qualityGate.getId()));

        qualityGate.getMachineIds().forEach(id -> {
            if (machines.containsKey(id)) {
                qualityGate.getMachines().add(machines.get(id));
            } else {
                final MachineGroup machineGroup = machineGroups.get(id);
                if (machineGroup != null) {
                    qualityGate.getMachines().addAll(machineGroup.getMachines());
                }
            }
        });

        return qualityGate;
    }
    
    private static ReturnedItems getReturnedItems(Config config, Map<String, Machine> machines) {
        final ReturnedItems returnedItems = config.getReturnedItems();
        returnedItems.setTopic(config.getRootTopic() + "/tickets/" + returnedItems.getId());
        returnedItems.setMqttClient(MqttUtils.createMqttClient(Constants.SIMULATOR_NAME + "-" + returnedItems.getId()));

        return returnedItems;
    }

    private static List<EventSource> getEventSources(Config config, Map<String, Machine> machines, Map<String, MachineGroup> machineGroups) {
        final List<EventSource> eventSources = config.getEventSources();

        eventSources.forEach(es -> {
            es.setTopic(config.getRootTopic() + "/events/" + es.getEventSourceId());
            es.setMqttClient(MqttUtils.createMqttClient(Constants.SIMULATOR_NAME + "-" + es.getEventSourceId()));
            es.getEvents().forEach(e -> {
                if (machines.containsKey(e.getMachineId())) {
                    e.setMachine(machines.get(e.getMachineId()));
                } else {
                    e.setMachine(getMachineFromMachineGroups(e.getMachineId(), machineGroups));
                }
            });
        });

        return eventSources;
    }

    private static Machine getMachineFromMachineGroups(String machineId, Map<String, MachineGroup> machineGroups) {
        final Iterator<Map.Entry<String, MachineGroup>> groupsIter = machineGroups.entrySet().iterator();
        while (groupsIter.hasNext()) {
            Map.Entry<String, MachineGroup> machineGroupEntry = groupsIter.next();
            final List<Machine> machines = machineGroupEntry.getValue().getMachines();

            for (Machine machine : machines) {
                if (machine.getId().equals(machineId)) {
                    return machine;
                }
            }
        }

        return null;
    }

    private static void startSimulation(Map<String, Machine> machines, Map<String, MachineGroup> machineGroups, QualityGate qualityGate,
            ReturnedItems returnedItems, List<String> productionLine, List<EventSource> eventSources) throws MqttException {

        final Optional<EventSource> mesEventSource = eventSources.stream().
                filter(e -> e.getEventSourceId().equals(Constants.CONFIG_EVENT_MES)).
                findFirst();
        if (mesEventSource.isPresent()) {
            eventSources.remove(mesEventSource.get());
        }

        while (true) {
            final String itemId = UUID.randomUUID().toString();

            List<String> machinesUsed = new ArrayList<>();
            List<String> machineGroupsUsed = new ArrayList<>();

            for (String id : productionLine) {
                if (machines.containsKey(id)) {

                    if (mesEventSource.isPresent()) {
                        mesEventSource.get().simulateCustomEvent(DataType.EVENT_MES_START_PRODUCTION, id, itemId);
                    }

                    machines.get(id).simulate();

                    if (mesEventSource.isPresent()) {
                        mesEventSource.get().simulateCustomEvent(DataType.EVENT_MES_END_PRODUCTION, id, itemId);
                    }
                    machinesUsed.add(id);

                } else if (machineGroups.containsKey(id)) {
                    final MachineGroup group = machineGroups.get(id);
                    final int randomIndex = MathUtils.getRandomInt(group.getMachines().size());
                    final Machine selectedMachine = group.getMachines().get(randomIndex);
                    group.setSelectedMachine(selectedMachine);
                    
                    if (mesEventSource.isPresent()) {
                        mesEventSource.get().simulateCustomEvent(DataType.EVENT_MES_START_PRODUCTION, selectedMachine.getId(), itemId);
                    }
                    
                    selectedMachine.simulate();

                    machinesUsed.add(selectedMachine.getId());
                    machineGroupsUsed.add(group.getId());
                    
                    if (mesEventSource.isPresent()) {
                        mesEventSource.get().simulateCustomEvent(DataType.EVENT_MES_END_PRODUCTION, selectedMachine.getId(), itemId);
                    }
                }else {
                    throw new UnsupportedOperationException("Unknown id in a production line: " + id);
                }
            }

            machines.entrySet().stream().
                    filter(entry -> machinesUsed.contains(entry.getKey())).
                    forEach(e -> e.getValue().increaseNumberOfItemsProduced());
            machineGroups.entrySet().stream().
                    filter(entry -> machineGroupsUsed.contains(entry.getKey())).
                    forEach(mg -> mg.getValue().getSelectedMachine().increaseNumberOfItemsProduced());

            eventSources.forEach(EventSource::simulate);

            if (qualityGate != null) {
                final Map<String, Double> machineQualityMap = qualityGate.simulate(itemId, machinesUsed);
                
                if(returnedItems != null) {
                    returnedItems.simulate(itemId, machineQualityMap);
                }
            }

            machines.entrySet().stream().
                    filter(entry -> machinesUsed.contains(entry.getKey())).
                    forEach(e -> e.getValue().resetGeneratedDataForCycle());
            machineGroups.entrySet().stream().
                    filter(entry -> machineGroupsUsed.contains(entry.getKey())).
                    forEach(mg -> mg.getValue().getSelectedMachine().resetGeneratedDataForCycle());
        }
    }
}
