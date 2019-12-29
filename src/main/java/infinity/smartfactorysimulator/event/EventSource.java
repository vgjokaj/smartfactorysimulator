package infinity.smartfactorysimulator.event;

import infinity.smartfactorysimulator.DataType;
import infinity.smartfactorysimulator.Message;
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
public class EventSource {

    private String topic;
    private IMqttClient mqttClient;

    private String eventSourceId;
    private List<EventsForMachine> events = new ArrayList<>();

    private Jsonb jsonb = JsonbBuilder.create();

    public void simulate() {
        if (!mqttClient.isConnected()) {
            throw new RuntimeException("MqttClient is not connected! Please check your configuration!");
        }

        events.forEach(e -> e.sendEvents(mqttClient, topic, eventSourceId));
    }

    public void simulateCustomEvent(DataType eventType, String machineId, String itemId) {
        if (!mqttClient.isConnected()) {
            throw new RuntimeException("MqttClient is not connected! Please check your configuration!");
        }

        if (eventType == null || machineId == null || itemId == null) {
            throw new IllegalArgumentException("Please check your arguments! None of them can be null!");
        }

        publishMessage(eventType, machineId, itemId);
    }

    private void publishMessage(DataType eventType, String machineId, String itemId) {
        MqttMessage msg;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("eventSourceId", eventSourceId);
            data.put("eventType", eventType);
            data.put("machineId", machineId);
            data.put("itemId", itemId);  
            
            Message machineMessage = new Message(Instant.now().toEpochMilli(), data);
            byte[] payload = jsonb.toJson(machineMessage).getBytes();

            msg = new MqttMessage(payload);
            msg.setQos(2);
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
        } catch (MqttException ex) {
            Logger.getLogger(EventsForMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
