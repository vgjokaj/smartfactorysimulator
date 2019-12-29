package infinity.smartfactorysimulator;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author Valon Gjokaj
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnedItems {
    
    private String id;
    private List<ReturnedItemsConstraint> constraints;
    
    private String topic;
    private IMqttClient mqttClient;
    private Jsonb jsonb = JsonbBuilder.create();
    
    public void simulate(String itemId, Map<String, Double> machineQualityMap) {
        if(machineQualityMap != null && !machineQualityMap.entrySet().isEmpty() &&
                constraints != null && !constraints.isEmpty()) {
            
            boolean returnItem = false;
            
            for(ReturnedItemsConstraint constraint : constraints) {
                if(!returnItem) {
                    if(machineQualityMap.containsKey(constraint.getMachineId())) {
                        final double machineQuality = machineQualityMap.get(constraint.getMachineId()) * 100;
                        final double qualityThresholdMinValue = constraint.getQualityThresholdMinValue();
                        final double qualityThresholdMaxValue = constraint.getQualityThresholdMaxValue();

                        if(machineQuality < qualityThresholdMinValue) {
                            returnItem = true;
                        }else if(machineQuality <= qualityThresholdMaxValue) {
                            // Decide randomly if an item should be returned or not.
                            double thresholdRange = qualityThresholdMaxValue - qualityThresholdMinValue;        // example: 80% - 70% = 10%
                            double step = 100 / thresholdRange;                                                 // example: 100% / 10% = 10% steps

                            double probabilityToReturn = (qualityThresholdMaxValue - machineQuality) * step;    // example: 80% - 71% = 9% * 10% = 90%
                            returnItem = MathUtils.getRandomBoolean(probabilityToReturn);
                        }
                    }
                }
            }
            
            // Send an event that the item has to be returned.
            if(returnItem) {
                publishMessage(itemId);
            }
        }
    }
    
    private void publishMessage(String itemId) {
        MqttMessage msg;
        try {
            msg = generateMessage(itemId);
            msg.setQos(2);
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
        } catch (MqttException ex) {
            Logger.getLogger(ReturnedItems.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private MqttMessage generateMessage(String itemId) {
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", itemId);

        Message returnedItemMsg = new Message(Instant.now().toEpochMilli(), data);
        byte[] payload = jsonb.toJson(returnedItemMsg).getBytes();
        return new MqttMessage(payload);
    }
}
