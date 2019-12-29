package infinity.smartfactorysimulator.machine;

import infinity.smartfactorysimulator.Message;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import lombok.Data;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author Valon Gjokaj
 */
@Data
public class QualityGate {

    private String id;
    private List<String> machineIds = new ArrayList<>();
    private List<Machine> machines = new ArrayList<>();

    private String topic;
    private IMqttClient mqttClient;
    private Jsonb jsonb = JsonbBuilder.create();

    public Map<String, Double> simulate(String itemId, List<String> machinesUsed) {
        if (!mqttClient.isConnected()) {
            throw new RuntimeException("MqttClient is not connected! Please check your configuration!");
        }

        Map<String, Double> machineQualityMap = new LinkedHashMap<>();

        machines.stream().filter(m -> machinesUsed.contains(m.getId())).forEach(machine -> {
            final double quality = calculateQualityInformation(machine);
            machineQualityMap.put(machine.getId(), quality);
        });

        publishMessage(itemId, machineQualityMap);
        return machineQualityMap;
    }

    public double calculateQualityInformation(Machine machine) {
        double aggregatedQuality = 0.0;
        int numberOfQualityDataSets = 0;

        for (MachineData machineData : machine.getData()) {
            if (machineData.getQualityConstraint() != null) {

                final List<Object> generatedValues = machine.getGeneratedDataForCycle().get(machineData.getId());
                int numberOfViolations = 0;

                if (generatedValues != null) {
                    if (machineData.getQualityConstraint().getType() == Quality.QualityType.MIN) {
                        final Double minValue = machineData.getQualityConstraint().getValue1();

                        for (Object value : generatedValues) {
                            if ((Double) value < minValue) {
                                numberOfViolations++;
                            }
                        }

                    } else if (machineData.getQualityConstraint().getType() == Quality.QualityType.MAX) {
                        final Double maxValue = machineData.getQualityConstraint().getValue1();

                        for (Object value : generatedValues) {
                            if ((Double) value > maxValue) {
                                numberOfViolations++;
                            }
                        }

                    } else if (machineData.getQualityConstraint().getType() == Quality.QualityType.RANGE) {
                        final Double minValue = machineData.getQualityConstraint().getValue1();
                        final Double maxValue = machineData.getQualityConstraint().getValue2();

                        for (Object value : generatedValues) {
                            if ((Double) value < minValue || (Double) value > maxValue) {
                                numberOfViolations++;
                            }
                        }

                    }

                    aggregatedQuality += (generatedValues.size() - numberOfViolations) / (double) generatedValues.size();
                    numberOfQualityDataSets++;
                }
            }
        }

        if (numberOfQualityDataSets == 0) {
            return 1.0;
        } else {
            return aggregatedQuality / numberOfQualityDataSets;
        }
    }

    private void publishMessage(String itemId, Map<String, Double> machineQualityMap) {
        MqttMessage msg;
        try {
            msg = generateQualityGateMessage(itemId, machineQualityMap);
            msg.setQos(2);
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
        } catch (MqttException ex) {
            Logger.getLogger(QualityGate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private MqttMessage generateQualityGateMessage(String itemId, Map<String, Double> machineQualityMap) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("qualityGateId", this.id);
        data.put("itemId", itemId);
        data.putAll(machineQualityMap);
        
        Message qualityGateMessage = new Message(Instant.now().toEpochMilli(), data);
        byte[] payload = jsonb.toJson(qualityGateMessage).getBytes();
        return new MqttMessage(payload);
    }
}
