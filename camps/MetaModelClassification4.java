/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camps;

import util.hmm.*;

import javax.ws.rs.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST Web Service
 *
 * @author sneumann
 *
 *
 * MetaModelClassification4 has the same steps as MetaModelClassification3, but with improvements in some of them:
 * 1. List of candidate models with right count of tms are constructed
 * 2. For each of selected candidate model we do:
 * 2a. construct hmm graph (it was optimized by loading already prepared hmm from file, see MetaModelClassification4Prepare)
 * 2b. calculate score (log probability) of classifying sequence against this hmm model (it was optimized by using fast hmm-forward algorithm, see HMMGraph)
 * 2c. calculate z-score based on given score (it was optimized by keeping in memory all means and sigmas for all models)
 * 3. Sorting of list of models by z-score
 * 4. For each model of the top of this sorted list we do:
 * 4a. Calculate p-value of a score from item [2b.] (it was optimized by preparing distribution of scores for each cluster, see MetaModelClassification4Prepare)
 * 4b. Calculate the threshold for score from item [2b.] (loading of a threshold was optimized in same way as in item [2c.])
 * At the end only those matches are reported, where the score is above the meta model specific threshold.
 * Furthermore, a p-value and a Z-score is provided.
 *
 */

@Path("metaModel4")
public class MetaModelClassification4 {

    private static String WORK_DIR = "/ssd/camps3/";
    private static final String HMM_DIR = WORK_DIR + "HMMs/";
    private SystemCall server = new SystemCall();
    private String TMPFILES = "/usr/share/glassfishv3/glassfish/domains/domain1/tmp/tmhmm-";
    private String TMHMM = "/opt/tmhmm-2.0c/bin/tmhmm";
    private static String SCFILE = WORK_DIR + "scClustersAndTMSRange.txt";
    private static String THRESHOLDSFILE = WORK_DIR + "metaModels_thresholds_minSpec90.txt";
    private static String SCORESFILES = WORK_DIR + "metaModelsScores/";

    private static Map<String, HMMGraph> preloadedHmms = null;
    private static Map<String, Double[]> preloadedThresholdsAndZScores = null;

    public static void main(String[] args) throws Exception {
        String seq = "MSAMMVKLGLNKSALLLKPsafsraaalsssrrllfNTARTNFLSTSPLKNVASEMNTKA" +
                "AIAEEQILNKQRAKRPISPHLTIYQPQLTWYLSSLHRISLVLMGLGFYLFTILFGVSgll" +
                "glglTTEKVSNWYHQKFSKITEWSIKGSFAYLFAIHYGGAIRHLIWDTAKELTLKGVYRT" +
                "GYALIGFTAVLGTYLLTL".toUpperCase();
        long time = System.currentTimeMillis();
        String result = doItForTm(seq, 0.05, 10, 3);
        System.out.println(result);
        System.out.println("Time: " + (System.currentTimeMillis() - time) + " ms.");
    }

    /**
     * Retrieves representation of an instance of binfo.rws.camps.MetaModelClassification
     * @return an instance of java.lang.String
     */
    //GET
    //Produces("text/plain")
    //Path("/doIt/sequence/{sequence}")
    public String doIt(
            @PathParam("sequence") String sequence,
            @QueryParam("p") @DefaultValue("0.05") double p,
            @QueryParam("n") @DefaultValue("25") int n
           ) {

        String result = "";

        try {

            sequence = sequence.toUpperCase();

            int numTMS = predictTMS(sequence);


            if(numTMS > 0) {

                result = doItForTm(sequence, p, n, numTMS);
            }

        }catch(Exception e) {
             e.printStackTrace(System.err);
        }

        return result;
    }

    private static String doItForTm(String sequence, double p, int n, int numTMS) throws Exception {
        StringBuilder result = new StringBuilder();
        int sequenceLength = sequence.length();

        ArrayList<String> candidateModels = candidateModels(numTMS);

        double[][] farr = null;
        Map<String, Double[]> thrMeanSdMap = loadMetaModelThresholdsAndZScores();

        Hashtable<String,Double> model2score = new Hashtable<String,Double>();

        ArrayList<MaxList> maxList = new ArrayList<MaxList>();

        for(String clusterCode : candidateModels){

            //System.out.println("Loading hmm: " + clusterCode);

            HMMGraph hmm = loadHMM(clusterCode);

            farr = checkForStateCount(farr, hmm.getStates().length, sequenceLength);
            // test against all HMMs
            double score = score(sequence, hmm, farr);
            //System.out.println("Cluster code: " +clusterCode+", score: "+score);

            if((score + "").equals("NaN")){
                //System.out.println("NaN " + k + " " + secOrderHMMs.get(k).getClusterId());
            }else{

                double zscore = getZScore(clusterCode, score, thrMeanSdMap);

                maxList.add(new MaxList(zscore,clusterCode));
                model2score.put(clusterCode, Double.valueOf(score));
            }
        }

        Collections.sort(maxList);

        int countResults = 0;


        for(MaxList ml: maxList) {

            String clusterCode = ml.getClusterCode();

            double zscore = ml.getMax();

            double score = model2score.get(clusterCode).doubleValue();

            double threshold = getMetaModelThreshold(thrMeanSdMap, clusterCode);

            String pvalueStr = getPValue(clusterCode, score);

            double pvalue;
            if(pvalueStr.startsWith("<")) {
                pvalue = Double.parseDouble(pvalueStr.substring(1));
            }
            else{
                pvalue = Double.parseDouble(pvalueStr);
            }

            if(pvalue >p) {
                continue;
            }


            //System.out.println("\tResult: "+clusterCode+"\t"+score+"\t"+threshold);

            if(score >= threshold) {

                countResults++;

                if(countResults>n) {
                    break;
                }

                BigDecimal bd = new BigDecimal(score);
                bd = bd.setScale(4, BigDecimal.ROUND_HALF_UP);
                double scoreRounded = bd.doubleValue();

                result.append(clusterCode +"\t" +scoreRounded+ "\t"+ pvalueStr +"\t"+zscore+"\n");
            }
        }
        return result.toString();
    }

    private int predictTMS(String sequence) {

        int numTMS = 0;

        Pattern p = Pattern.compile("(.*)\\s+len=\\d+\\s+ExpAA=\\d+\\.\\d+\\s+First60=\\d+\\.\\d+\\s+PredHel=(\\d+)\\s+Topology=(.*)",Pattern.DOTALL);		//complete row

        String filename = (TMPFILES + server.call("date +%F-%H-%M-%S-%N")).trim();
        try {
            BufferedWriter tempfile = new BufferedWriter(new FileWriter(new File(filename)));
            tempfile.write(sequence);
            tempfile.close();

            String cmd = TMHMM + " -short " + filename;
            String query = server.call(cmd);

            Matcher m = p.matcher(query);
            if(m.matches()) {
                numTMS = Integer.parseInt(m.group(2));
            }

            System.out.println("--Number of predicted TMS: " +numTMS);

        } catch (IOException ex) {
            //Logger.getLogger(MetaModelClassification2.class.getName()).log(Level.SEVERE, "Server Error IO", ex);
            throw new WebApplicationException(500);
        } catch(Exception e) {
             e.printStackTrace(System.err);
        }

        return numTMS;
    }

    private static ArrayList<String> candidateModels(int numTMS) {

        ArrayList<String> candidates = null;

        try {

            candidates = new ArrayList<String>();

            BufferedReader br = new BufferedReader(new FileReader(new File(SCFILE)));
            String line;
            while((line = br.readLine()) != null) {

                String[] content = line.split("\t");

                String scCode = content[0].trim();
                String tmsRange = content[1].trim();

                //int min = Integer.parseInt(tmsRange.split("-")[0]);
                //int max = Integer.parseInt(tmsRange.split("-")[1]);

                int min = Integer.parseInt(tmsRange.split("-")[0])-1;
                int max = Integer.parseInt(tmsRange.split("-")[1])+1;

                if(numTMS >= min && numTMS <= max) {
                    candidates.add(scCode);
                }
            }
            br.close();

            System.out.println("--Number of meta model candidates: " +candidates.size());

        }catch(Exception e) {
             e.printStackTrace(System.err);
        }

        return candidates;
    }

    private static double roundPValue(double pvalue) {

        double pvalueRounded = pvalue;

        try {

            String pvalueStr = String.valueOf(pvalue);
            if(pvalueStr.contains("E")) {

                double mantisse = (new Double(pvalueStr.substring(0, pvalueStr.indexOf("E")))).doubleValue();
		String exponent = pvalueStr.substring(pvalueStr.indexOf("E")+1);

		BigDecimal bd = new BigDecimal(mantisse);
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
		double mantisseRounded = bd.doubleValue();

		pvalueRounded = new Double(mantisseRounded+"E"+exponent);
            }
            else{

                BigDecimal bd = new BigDecimal(pvalue);
		bd = bd.setScale(4, BigDecimal.ROUND_HALF_UP);
		pvalueRounded = bd.doubleValue();
            }

        }catch(Exception e) {
            e.printStackTrace(System.err);
        }

        return pvalueRounded;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Method checks (and extends) size of array used in hmm-forward algorithm (see HMMGraph).
     * @param farr 2d double array with size stateCount * (sequenceLength+1)
     * @param stateCount states in hmm
     * @param sequenceLength letters in sequence
     * @return old or new (if it was extended) instance of farr
     */
    private static double[][] checkForStateCount(double[][] farr, int stateCount, int sequenceLength) {
        if (farr == null || farr.length < stateCount) {
            farr = new double[stateCount][sequenceLength + 1];
        }
        return farr;
    }

    /**
     * Method loads hmm graph parameters from *.hmm.serialized file (prepeared in MetaModelClassification4Prepare)
     * and construct hmm graph (HMMGraph).
     * @param clusterName cluster code
     * @return hmm graph
     * @throws IOException in case of file errors
     * @throws ClassNotFoundException in case of absense of class HMMGraphData in JVM
     */
    private static HMMGraph loadHMM(String clusterName) throws IOException, ClassNotFoundException {
        File hmmSerialized = new File(HMM_DIR, clusterName + ".hmm.serialized");
        InputStream bis = new BufferedInputStream(new FileInputStream(hmmSerialized));
        ObjectInputStream is = new ObjectInputStream(bis);
        Object obj = is.readObject();
        is.close();
        HMMGraphData data = (HMMGraphData)obj;
        HMMGraph hmm = new HMMGraph();
        hmm.init(data);
        return hmm;
    }

    /**
     * Method calculates sequence probability in hmm-model (using forward algorithm).
     * @param s1 amino acid text sequence
     * @param hmm hidden markov model
     * @param farr temporary array (prepared in checkForStateCount())
     * @return sequence probability
     */
    private static double score(String s1, HMMGraph hmm, double[][] farr) {
        int[] seq = new int[s1.length()];
        for(int i = 0; i < s1.length(); i++){
            try{
                seq[i] = ASMapping.asToInt(s1.charAt(i));
            }catch(Exception e){
                e.printStackTrace(System.err);
            }
        }
        double score;
        try {
            score = hmm.calcLogProbab(seq, farr) / seq.length;
        } catch (Exception ignore) {
            score = HMMMath.LOG_NEG_INF;
        }
        return score;
    }

    /**
     * Method loads cache data from "metaModels_thresholds_minSpec90.txt" file into memory.
     * @return mapping from cluster code to array Double[] {threshold, mean, standard deviation}.
     */
    private static Map<String, Double[]> loadMetaModelThresholdsAndZScores() {
        if (preloadedThresholdsAndZScores != null)
            return preloadedThresholdsAndZScores;

        Map<String, Double[]> ret = new TreeMap<String, Double[]>();

        try{

            BufferedReader br = new BufferedReader(new FileReader(new File(THRESHOLDSFILE)));
            String line;
            while((line=br.readLine()) != null) {

                if(line.startsWith("#SC-cluster")) {    //skip header
                    continue;
                }

                String[] content = line.split("\t");

                String clusterCode = content[0].trim();
                Double threshold = Double.parseDouble(content[1].trim());
                Double mean = parseDouble(content[8].trim());
                Double sd = parseDouble(content[10].trim());
                ret.put(clusterCode, new Double[] {threshold, mean, sd});
            }
            br.close();

        }catch(Exception e) {
            e.printStackTrace(System.err);
        }

        preloadedThresholdsAndZScores = ret;
        return ret;
    }

    private static Double parseDouble(String text) {
        if (text.equals("-"))
            return null;
        return Double.parseDouble(text);
    }

    /**
     * Method returns z-score calculated using cache loaded in loadMetaModelThresholdsAndZScores().
     * @param clusterCode cluster code
     * @param score score
     * @param meanSdMap cache loaded in loadMetaModelThresholdsAndZScores()
     * @return z-score
     */
    private static double getZScore(String clusterCode, double score, Map<String, Double[]> meanSdMap) {
        Double[] row = meanSdMap.get(clusterCode);
        Double mean = row[1];
        Double sd = row[2];
        if(mean != null && sd != null) {
            double zscore = (score - mean)/sd;
            BigDecimal bd = new BigDecimal(zscore);
            bd = bd.setScale(4, BigDecimal.ROUND_HALF_UP);
            return bd.doubleValue();
        } else {
            return 0;
        }
    }

    /**
     * Method returns threshold from cache loaded in loadMetaModelThresholdsAndZScores().
     * @param clusterCode cluster code
     * @param threshMap cache loaded in loadMetaModelThresholdsAndZScores()
     * @return threshold
     */
    private static double getMetaModelThreshold(Map<String, Double[]> threshMap, String clusterCode) {
        Double[] ret = threshMap.get(clusterCode);
        if (ret == null) {
            return -3.0;
        }
        return ret[0];
    }

    /**
     * This method was optimized by using files {cluster-code}_nonmembers.distfunc (prepared in
     * MetaModelClassification4Prepare) instead of files {cluster-code}_nonmembers.scores (see
     * MetaModelClassification3). These files contain not plain scores of fitting all non-members to certain cluster
     * but distribution function of count of evidences with certain score. To count non-members with scores higher
     * than threshold we just need to find in a file a cell with queried score.
     * @param clusterCode cluster code
     * @param score lower threshold
     * @return p-value of evidence of scores no less than score
     */
    private static String getPValue(String clusterCode, double score) {

        String pvalue = "1";

        try {

            //
            //parse file containing scores for non-members
            //
            File scoresFile = new File(SCORESFILES+clusterCode+"_nonmembers.distfunc");
            if(scoresFile.exists()) {

                int countScores = 0;
                int countScoresBetterOrEqualGivenScore = 0;
                BufferedReader br = new BufferedReader(new FileReader(scoresFile));
                String line;
                while((line=br.readLine()) != null) {
                    String[] content = line.split("\t");
                    double currentScore = Double.parseDouble(content[0].trim());
                    countScoresBetterOrEqualGivenScore = Integer.parseInt(content[1].trim());
                    if (countScores == 0)
                        countScores = countScoresBetterOrEqualGivenScore;
                    if(currentScore >= score)
                        break;
                    countScoresBetterOrEqualGivenScore = 0;
                }
                br.close();

                if(countScores == 0 && countScoresBetterOrEqualGivenScore == 0) {
                    pvalue = "1";
                }
                else if(countScoresBetterOrEqualGivenScore == 0) {

                    countScoresBetterOrEqualGivenScore = 1;

                    double pvalueDouble = countScoresBetterOrEqualGivenScore/((double) countScores);

                    double pvalueDoubleRounded = roundPValue(pvalueDouble);

                    pvalue = "<"+String.valueOf(pvalueDoubleRounded);
                }
                else {

                    double pvalueDouble = countScoresBetterOrEqualGivenScore/((double) countScores);

                    double pvalueDoubleRounded = roundPValue(pvalueDouble);

                    pvalue = String.valueOf(pvalueDoubleRounded);
                }
            }

        }catch(Exception e) {
            e.printStackTrace(System.err);
        }

        return pvalue;
    }
}
