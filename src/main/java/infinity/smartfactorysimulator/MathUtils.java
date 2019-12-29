package infinity.smartfactorysimulator;

import java.util.Random;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 *
 * @author Valon Gjokaj
 */
public class MathUtils {

    private static final Random random = new Random();

    public static int getRandomInt(int max) {
        return random.nextInt(max);
    }
    
    public static boolean getRandomBoolean(double probabilityForTrueInPercentage) {
        return getRandomInt(100) >= probabilityForTrueInPercentage;
    }

    public static Integer generateUniformRandomInt(String value) {
        String[] range = value.split(",");
        int min = Integer.parseInt(range[0].trim());
        int max = Integer.parseInt(range[1].trim());

        return getRandomInt(max - min + 1) + min;
    }

    public static Float generateUniformRandomFloat(String value) {
        String[] range = value.split(",");
        float min = Float.parseFloat(range[0].trim());
        float max = Float.parseFloat(range[1].trim());

        return min + random.nextFloat() * (max - min);
    }

    public static Double generateNormalDistribution(String value) {
        final String[] distributionConfig = value.split(",");

        final double mean = Double.parseDouble(distributionConfig[0].trim());
        final double deviation = Double.parseDouble(distributionConfig[1].trim());

        NormalDistribution normalDistribution = new NormalDistribution(mean, deviation);
        return normalDistribution.sample();
    }

    public static Double generateCauchyDistribution(String value) {
        final String[] distributionConfig = value.split(",");

        final double location = Double.parseDouble(distributionConfig[0].trim());
        final double scale = Double.parseDouble(distributionConfig[1].trim());

        CauchyDistribution cauchyDistribution = new CauchyDistribution(location, scale);
        return cauchyDistribution.sample();
    }
}
