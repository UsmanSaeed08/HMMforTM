package util.hmm;

import java.io.*;
import java.util.*;

/**
 * Class holds hidden markov model graph of states and provide function for
 * calculation probability of sequence (forward algorithm only).
 */
public class HMMGraph implements Serializable {
    protected HMMState[] states;
    protected HMMState startState;

    /**
     * Method used for initialization of emission and transition probabilities as well as graph structure (nodes and
     * edges) of HMM model.
     * @param data parameters in packed form
     */
    public void init(HMMGraphData data) {
        double[] startTransitions = data.startTransitions;
        double[][] stateEmissions = data.stateEmissions;
        Map<Integer, Double>[] stateTransitions = data.stateTransitions;
        List<HMMState> stateVec = new ArrayList<HMMState>();
        Hashtable<String, HMMState> stateHash = new Hashtable<String, HMMState>();
        {
            startState = new HMMState("start", 0);
            stateVec.add(startState);
            stateHash.put(startState.name, startState);
        }

        int stateCount = stateEmissions.length;
        for (int statePos = 0; statePos < stateCount; statePos++) {
            int stateNum = statePos + 1;
            String stateName = "state" + stateNum;
            HMMState new_state = new HMMState(stateName, stateNum);
            new_state.isEnd = true;
            stateVec.add(new_state);
            stateHash.put(stateName, new_state);
            new_state.emiss = toLogs(stateEmissions[statePos]);
        }
        for (int fromStatePos = 0; fromStatePos < stateCount; fromStatePos++) {
            String fromStateName = "state" + (fromStatePos + 1);
            HMMState fromState = stateHash.get(fromStateName);
            List<Double> probabs = new ArrayList<Double>();
            for (Map.Entry<Integer, Double> entry : stateTransitions[fromStatePos].entrySet()) {
                int toStatePos = entry.getKey();
                double transProbab = entry.getValue();
                String toStateName = "state" + (toStatePos + 1);
                HMMState toState = stateHash.get(toStateName);
                fromState.addNextState(toState);
                probabs.add(transProbab);
            }
            fromState.trans = toLogs(probabs);
        }
        for (int toStatePos = 0; toStatePos < stateCount; toStatePos++) {
            String toStateName = "state" + (toStatePos + 1);
            HMMState toState = stateHash.get(toStateName);
            startState.addNextState(toState);
        }
        startState.trans = toLogs(startTransitions);
        states = stateVec.toArray(new HMMState[stateVec.size()]);
    }

    private double[] toLogs(double[] probabs) {
        double[] temp = new double[probabs.length];
        for(int i=0; i<temp.length; i++)
            temp[i] = HMMMath.log(probabs[i]);
        return temp;
    }

    private double[] toLogs(List<Double> probabs) {
        double[] temp = new double[probabs.size()];
        for(int i=0; i<temp.length; i++)
            temp[i] = probabs.get(i);
        return toLogs(temp);
    }

    public HMMState[] getStates() {
        return this.states;
    }

    /**
     * Method computes probability of sequence according to hmm model by forward algorithm.
     * @param seq sequence
     * @param f helper array of size not less than [states.length][seq.length + 1]
     * @return returns log(probability(seq))
     */
    public double calcLogProbab(int[] seq, double[][] f) {
        int states_count = states.length;
        int seqLen = seq.length;
        for(int s=0;s<states_count;s++) {
            f[s][0] = (states[s] == startState)?0: HMMMath.LOG_NEG_INF;
        }
        for(int i=0;i<startState.transStates.length;i++) {
            int s_ = startState.transStates[i].num;
            double trans_log_probab = startState.trans[i];
            f[s_][0] = HMMMath.logSum(f[s_][0], f[startState.num][0] + trans_log_probab);
        }
        for(int pos=0;pos<seqLen;pos++) {
            for(int s = 0; s < states_count; s++)
                f[s][pos+1] = HMMMath.LOG_NEG_INF;
            for(int s = 1; s < states_count; s++) {
                HMMState st = states[s];
                if(st == startState)
                    continue;
                double old_val = f[s][pos];
                double emiss_w = st.emiss[seq[pos]];
                f[s][pos] = old_val+emiss_w;
                for(int i=0;i<st.transStates.length;i++) {
                    int s_ = st.transStates[i].num;
                    double trans_log_probab = st.trans[i];
                    f[s_][pos+1] = HMMMath.logSum(f[s_][pos+1],f[s][pos]+trans_log_probab);
                }
            }
        }
        double fwd_log_p = HMMMath.LOG_NEG_INF;
        for(int s=0;s<states_count;s++) {
            if(states[s].isEnd) fwd_log_p = HMMMath.logSum(fwd_log_p,f[s][seq.length]);
        }
        return fwd_log_p;
    }
}
