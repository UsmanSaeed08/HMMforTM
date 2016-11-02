package util.hmm;

import java.io.Serializable;
import java.util.Map;

/**
 * Class used for storing HMM model parameters on disk in packed (serialized) form. Each value of startTransitions
 * array is probability to start from corresponding state. Each row of stateEmissions array describes emissions of
 * corresponding state. Each value of stateTransitions is map of transitions from corresponding state. Each map of
 * transitions describes target states (map keys) and probabilities ot transit there (map values).
 */
public class HMMGraphData implements Serializable {
    public double[] startTransitions = null;
    public double[][] stateEmissions = null;
    public Map<Integer, Double>[] stateTransitions = null;

    /**
     * Empty constructor is used by standard java serialization mechanism.
     */
    public HMMGraphData() {}

    public HMMGraphData(double[] startTransitions, double[][] stateEmissions, Map<Integer, Double>[] stateTransitions) {
        this.startTransitions = startTransitions;
        this.stateEmissions = stateEmissions;
        this.stateTransitions = stateTransitions;
    }
}
