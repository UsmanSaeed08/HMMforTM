package camps;

import util.hmm.HMMGraphData;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is used for preparation of some data before using MetaModelClassification4. It should be started every
 * time when some data changed in files "camps_data/HMMs/*.hmm" or "camps_data/metaModelsScores/*_nonmembers.scores".
 * These file are not used in MetaModelClassification4 (in contrast to MetaModelClassification3). Instead of these
 * files this preparation procedure makes their analogs: "camps_data/HMMs/*.hmm.serialized" and
 * "camps_data/metaModelsScores/*_nonmembers.distfunc". These analogs should be put on application server that is
 * responsible for meta-model classification.
 */
public class MetaModelClassification4Prepare {
    private static final String HMM_DIR = "camps_data/HMMs/";
    private static final String SCORESFILES = "camps_data/metaModelsScores/";
    private static final boolean ONLY_HMMS = false;

    public static void main(String[] args) throws IOException {
        System.out.println("Preparing common serialization file for all hmms...");
        for (File f : new File(HMM_DIR).listFiles()) {
            String fileName = f.getName();
            if (fileName.endsWith(".hmm")) {
                System.out.println(fileName);
                HMMGraphData hmm = loadHmmData(f);
                serializeObjectToFile(hmm, new File(HMM_DIR, fileName + ".serialized"));
            }
        }
        if (ONLY_HMMS)
            return;
        System.out.println("Preparing distribution function files for nonmemeber scores...");
        for (File f : new File(SCORESFILES).listFiles()) {
            if (f.getName().endsWith("_nonmembers.scores")) {
                String fileName = f.getName();
                String outFileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".distfunc";
                System.out.println(fileName);
                processScoreFile(f, new File(SCORESFILES, outFileName));
            }
        }
    }

    private static void serializeObjectToFile(Serializable obj, File out) throws IOException {
        OutputStream bos = new BufferedOutputStream(new FileOutputStream(out));
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(obj);
        os.close();
    }

    private static HMMGraphData loadHmmData(File file) {
        int region=0; int i = 0; int nbStates = 0;int observables = 0;
        //Open an output stream
        String alphabet = getAlphabet();
        double[] startTransitions = null;
        double[][] stateEmissions = null;
        Map<Integer, Double>[] stateTransitions = null;
        try{
            BufferedReader reader = new BufferedReader(new FileReader((file)));
            String line;
            while((line = reader.readLine()) != null){
                if(line.startsWith("#states")){
                    region=0;
                }else if(line.startsWith("#observables")){
                    region=1;
                }else if(line.startsWith("#P_i")){
                    region=2;
                }else if(line.startsWith("#A_ij")){
                    region=3;
                }else if(line.startsWith("#IntegerDistribution")){
                    region=4;i=0;
                }else if(line.startsWith("#secOrder")){
                    region=5;
                }else{
                    switch(region){
                        case 0: nbStates = new Integer(line);
                            startTransitions = new double[nbStates];
                            stateEmissions = new double[nbStates][alphabet.length()];
                            //noinspection unchecked
                            stateTransitions = new Map[nbStates];
                            break;
                        case 1: observables = new Integer(line);
                            if (observables != alphabet.length())
                                throw new IllegalStateException();
                            break;
                        case 2: String[] pi = line.split(",");
                            for(int j=0; j<nbStates; j++){
                                startTransitions[j] = Double.parseDouble(pi[j]);
                            }
                            break;
                        case 3: String[] aj = line.split(",");
                            Map<Integer, Double> transRow = stateTransitions[i];
                            if (transRow == null) {
                                transRow = new TreeMap<Integer, Double>();
                                stateTransitions[i] = transRow;
                            }
                            for(int j=0; j<nbStates; j++){
                                double transProbab = Double.parseDouble(aj[j]);
                                if (transProbab > 0) {
                                    transRow.put(j, transProbab);
                                }
                            }
                            i++;
                            break;
                        case 4: String[] intDistS = line.split(",");
                            for(int j=0; j<observables; j++){
                                stateEmissions[i][j] = Double.parseDouble(intDistS[j]);
                            }
                            i++;
                            break;
                        case 5: //secOrder = line.split(",");
                            break;
                    }
                }

            }
            reader.close();
        }catch(Exception e){
            throw new IllegalStateException(e);
        }
        return new HMMGraphData(startTransitions, stateEmissions, stateTransitions);
    }

    private static String getAlphabet() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            try {
                ret.append(ASMapping.intToAS(i));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return ret.toString();
    }

    public static void processScoreFile(File scoresFile, File outputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(scoresFile));
        String line;
        TreeMap<Double, Integer> map = new TreeMap<Double, Integer>();
        int countScores = 0;
        while((line=br.readLine()) != null) {
            String[] content = line.split("\t");
            String scoreStr = content[0].trim();
            if(scoreStr.equals("NaN") || scoreStr.equals("-Infinity")) {
                continue;
            }
            countScores++;
            double currentScore = Double.parseDouble(scoreStr);
            currentScore = Math.round(currentScore * 1000) / 1000.0;
            Integer localCount = map.get(currentScore);
            if (localCount == null) {
                localCount = 1;
            } else {
                localCount++;
            }
            map.put(currentScore, localCount);
        }
        br.close();
        int countScores2 = 0;
        TreeMap<Double, Integer> map2 = new TreeMap<Double, Integer>();
        for (Double key : map.descendingKeySet()) {
            int localCount = map.get(key);
            countScores2 += localCount;
            map2.put(key, countScores2);
        }
        if (countScores != countScores2)
            throw new IllegalStateException("Not good: " + countScores + ", " + countScores2);
        PrintWriter pw = new PrintWriter(outputFile);
        for (Double key : map.keySet()) {
            pw.println(key + "\t" + map2.get(key) + "\t" + map.get(key));
        }
        pw.close();
    }
}
