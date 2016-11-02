package util.hmm;

import java.io.Serializable;

/**
 * This class defines node of hidden markov model graph. It stores state number, emission probabilities,
 * list of transition target states and transition probabilities for it.
 */
public class HMMState implements Serializable {
    public String name;
    public int num;
    public double[] emiss;
    public double[] trans;
    public HMMState[] transStates;
    public boolean isEnd = false;
    //
    public HMMState(String name,int num) {
        this.name = name;
        this.num = num;
        this.transStates = new HMMState[0];
    }

    public void addNextState(HMMState st) {
        HMMState[] temp_arr = this.transStates;
        this.transStates = new HMMState[this.transStates.length+1];
        System.arraycopy(temp_arr,0,this.transStates,0,temp_arr.length);
        this.transStates[this.transStates.length-1] = st;
    }
}
