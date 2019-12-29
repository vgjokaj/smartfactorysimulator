package infinity.smartfactorysimulator;

import infinity.smartfactorysimulator.config.Config;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author Valon Gjokaj
 */
public class MqttUtils {

    private static Config config;
    private static MqttConnectOptions mqttOptions;

    public static void updateMqttConnectOptions(Config config) {
        MqttUtils.config = config;
        MqttUtils.mqttOptions = new MqttConnectOptions();
        MqttUtils.mqttOptions.setAutomaticReconnect(true);
        MqttUtils.mqttOptions.setCleanSession(true);
        MqttUtils.mqttOptions.setConnectionTimeout(10);
        MqttUtils.mqttOptions.setUserName(config.getMqttUsername());
        MqttUtils.mqttOptions.setPassword(config.getMqttPassword().toCharArray());
    }

    public static IMqttClient createMqttClient(String publisherId) {
        MqttClient client = null;
        try {
            client = new MqttClient(config.getMqttServerURI(), publisherId);
            client.connect(mqttOptions);
        } catch (MqttException ex) {
            Logger.getLogger(MqttUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return client;
    }
}
