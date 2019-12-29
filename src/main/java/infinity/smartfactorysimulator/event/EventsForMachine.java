package infinity.smartfactorysimulator.event;

import infinity.smartfactorysimulator.machine.Machine;
import infinity.smartfactorysimulator.DataType;
import infinity.smartfactorysimulator.MathUtils;
import infinity.smartfactorysimulator.Message;
import infinity.smartfactorysimulator.Noise;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
public class EventsForMachine {

    private String machineId;
    private Machine machine;
    private List<EventData> eventsToGenerate = new ArrayList<>();
    private Jsonb jsonb = JsonbBuilder.create();

    public void sendEvents(IMqttClient mqttClient, String topic, String eventSourceId) {
        eventsToGenerate.forEach(e -> {

            if (e.getConditionType() == EventData.ConditionType.AFTER_ITEMS) {

                Integer afterItemsNumber = e.getConditionValue();
                if (e.getNoise() != null) {
                    afterItemsNumber += getNoise(e.getNoise()).intValue();
                }

                if (machine.getItemsProduced() > afterItemsNumber) {
                    publishMessage(mqttClient, topic, eventSourceId, e.getType());

                    if (e.getType() == DataType.EVENT_MAINTENANCE) {
                        machine.resetNumberOfItemsProduced();
                        machine.resetTotalTimeSpentInMillis();
                    }
                }

            } else if (e.getConditionType() == EventData.ConditionType.AFTER_SECONDS) {

                Integer everySecNumber = e.getConditionValue();
                if (e.getNoise() != null) {
                    everySecNumber += getNoise(e.getNoise()).intValue();
                }

                if (machine.getTotalTimeSpentInMillis() > everySecNumber * 1000) {
                    publishMessage(mqttClient, topic, eventSourceId, e.getType());

                    if (e.getType() == DataType.EVENT_MAINTENANCE) {
                        machine.resetNumberOfItemsProduced();
                        machine.resetTotalTimeSpentInMillis();
                    }

                }
            } else {
                throw new UnsupportedOperationException("ConditionType not supported: " + e.getConditionType());
            }
        });
    }

    private Number getNoise(Noise noise) {
        Number result = 0.0;

        switch (noise.getType()) {
            case RND_INT:
                result = MathUtils.generateUniformRandomInt(noise.getValue());
                break;
            case RND_FLOAT:
                result = MathUtils.generateUniformRandomFloat(noise.getValue());
                break;
            case NORMAL_DST:
                result = MathUtils.generateNormalDistribution(noise.getValue());
                break;
            case CAUCHY_DST:
                result = MathUtils.generateCauchyDistribution(noise.getValue());
                break;
            default:
                break;
        }

        return result;
    }

    private void publishMessage(IMqttClient mqttClient, String topic, String eventSourceId, DataType eventType) {
        MqttMessage msg;
        try {
            msg = generateEventMessage(eventSourceId, eventType);
            msg.setQos(2);
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
        } catch (MqttException ex) {
            Logger.getLogger(EventsForMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private MqttMessage generateEventMessage(String eventSourceId, DataType eventType) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventSourceId", eventSourceId);
        data.put("eventType", eventType);
        data.put("machineId", machine.getId());
        
        Message machineMessage = new Message(Instant.now().toEpochMilli(), data);
        byte[] payload = jsonb.toJson(machineMessage).getBytes();
        return new MqttMessage(payload);
    }
}
