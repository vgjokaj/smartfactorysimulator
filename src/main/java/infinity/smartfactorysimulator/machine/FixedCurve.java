package infinity.smartfactorysimulator.machine;

import java.util.Iterator;
import java.util.List;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Valon Gjokaj
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FixedCurve {

    private List<Double> dataPoints;
    private Iterator<Double> iterator;

    @JsonbCreator
    public FixedCurve(@JsonbProperty("dataPoints") List<Double> dataPoints) {
        this.dataPoints = dataPoints;
        this.iterator = dataPoints.iterator();
    }

    public Double getNextDataPoint() {
        if (iterator == null || !iterator.hasNext()) {
            iterator = dataPoints.iterator();
        }

        return iterator.next();
    }

}
