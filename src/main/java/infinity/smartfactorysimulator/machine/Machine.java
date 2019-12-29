package infinity.smartfactorysimulator.machine;

import infinity.smartfactorysimulator.Message;
import infinity.smartfactorysimulator.Noise;
import infinity.smartfactorysimulator.DataType;
import infinity.smartfactorysimulator.Constants;
import infinity.smartfactorysimulator.MathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
public class Machine {

    private String id;
    private List<MachineData> data = new ArrayList<>();
    private Map<String, List<Object>> generatedDataForCycle = new LinkedHashMap<>();
    private int dataFrequency;
    private int amountOfDataPerFrequency;
    private int totalDurationPerCycle;
    private int itemsProduced;
    private long totalTimeSpentInMillis;

    private String topic;
    private IMqttClient mqttClient;
    private int msgId;

    private String fixedCurvesFolderPath = Constants.CONFIG_FIXED_CURVES_DEFAULT_FOLDER_PATH;
    private Map<String, FixedCurve> fixedCurvesMap = new HashMap<>();

    private Jsonb jsonb = JsonbBuilder.create();

    public void simulate() throws MqttException {
        long start = System.currentTimeMillis();

        if (!mqttClient.isConnected()) {
            throw new RuntimeException("MqttClient is not connected! Please check your configuration!");
        }

        createFixedCurves();

        // Check if the total duration is reached
        long durationStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - durationStart < totalDurationPerCycle) {

            try {
                for (int i = 0; i < amountOfDataPerFrequency; i++) {
                    publishMessage();
                }
                Thread.sleep(dataFrequency);
            } catch (InterruptedException ex) {
                Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        totalTimeSpentInMillis += System.currentTimeMillis() - start;
    }

    private void createFixedCurves() {
        data.stream().
                filter(e -> e.getType() == DataType.FIXED_CURVE).
                forEach(i -> {
                    fixedCurvesMap.put(i.getId(), getFixedCurveFromJson(i.getValue()));
                });
    }

    private FixedCurve getFixedCurveFromJson(String fileName) {
        String filePath = fixedCurvesFolderPath + "/" + fileName;

        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            return jsonb.fromJson(jsonString, FixedCurve.class);
        } catch (IOException ex) {
            Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private void publishMessage() throws MqttException {
        MqttMessage msg = getMessage();
        msg.setId(msgId);
        msg.setQos(2);
        msg.setRetained(false);
        mqttClient.publish(topic, msg);
        msgId++;
    }

    private MqttMessage getMessage() {
        Message machineMessage = new Message(
                Instant.now().toEpochMilli(),
                generateData());

        byte[] payload = jsonb.toJson(machineMessage).getBytes();
        return new MqttMessage(payload);
    }

    private Map<String, Object> generateData() {
        Map<String, Object> generatedData = new LinkedHashMap<>();

        data.stream().forEach(e -> {
            Object value;

            switch (e.getType()) {
                case RND_INT:
                    value = MathUtils.generateUniformRandomInt(e.getValue());
                    break;
                case RND_FLOAT:
                    value = MathUtils.generateUniformRandomFloat(e.getValue());
                    break;
                case NORMAL_DST:
                    value = MathUtils.generateNormalDistribution(e.getValue());
                    break;
                case CAUCHY_DST:
                    value = MathUtils.generateCauchyDistribution(e.getValue());
                    break;
                case FIXED_CURVE:
                    value = fixedCurvesMap.get(e.getId()).getNextDataPoint();
                    if (e.getNoise() != null) {
                        value = (Double) value + getNoise(e.getNoise());
                    }
                    break;
                default:
                    value = e.getValue();
                    break;
            }

            generatedData.put(e.getId(), value);

            if (generatedDataForCycle.containsKey(e.getId())) {
                generatedDataForCycle.get(e.getId()).add(value);
            } else {
                List<Object> listOfValues = new ArrayList<>();
                listOfValues.add(value);
                generatedDataForCycle.put(e.getId(), listOfValues);
            }
        });

        return generatedData;
    }

    private Double getNoise(Noise noise) {
        Double result = 0.0;

        switch (noise.getType()) {
            case RND_INT:
                result = MathUtils.generateUniformRandomInt(noise.getValue()).doubleValue();
                break;
            case RND_FLOAT:
                result = MathUtils.generateUniformRandomFloat(noise.getValue()).doubleValue();
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

    public void increaseNumberOfItemsProduced() {
        itemsProduced++;
    }

    public void resetNumberOfItemsProduced() {
        itemsProduced = 0;
    }

    public void resetTotalTimeSpentInMillis() {
        totalTimeSpentInMillis = 0;
    }

    public void resetGeneratedDataForCycle() {
        generatedDataForCycle.clear();
    }
}
