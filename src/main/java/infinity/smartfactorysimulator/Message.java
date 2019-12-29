package infinity.smartfactorysimulator;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author Valon Gjokaj
 */
@Data
@AllArgsConstructor
public class Message {

    private long timestamp;
    private Map<String, Object> data = new LinkedHashMap<>();
    
}
