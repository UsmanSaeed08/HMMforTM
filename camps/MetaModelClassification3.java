/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camps;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfIntegerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

/**
 * REST Web Service
 *
 * @author sneumann
 *
 *
 * Same as MetaModelClassification2, but with thresholds.
 *
 * Only those matches are returned, where the score is above the meta model
 * specific threshold. Furthermore, a p-value and a Z-score is provided.
 *
 */

@Path("metaModel3")
public class MetaModelClassification3 {

    //private static final String HMM_DIR = "/home/proj/Camps3/data4website/HMMs/";
    private static final String HMM_DIR = "/ssd/camps3/HMMs/";

    private SystemCall server = new SystemCall();
    private String TMPFILES = "/usr/share/glassfishv3/glassfish/domains/domain1/tmp/tmhmm-";
    //private String TMPFILES = "/tmp/tmhmm-";
    private String TMHMM = "/opt/tmhmm-2.0c/bin/tmhmm";
    //private String TMHMM = "/usr/apps/bin/tmhmm";
    private String SCFILE = "/ssd/camps3/scClustersAndTMSRange.txt";
    //private String SCFILE = "/home/proj/Camps3/data4website/scClustersAndTMSRange.txt";
    private String THRESHOLDSFILE = "/ssd/camps3/metaModels_thresholds_minSpec90.txt";
    private String SCORESFILES = "/ssd/camps3/metaModelsScores/";


    /**
     * Retrieves representation of an instance of binfo.rws.camps.MetaModelClassification
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    @Path("/doIt/sequence/{sequence}")
    public String doIt(
            @PathParam("sequence") String sequence,
            @QueryParam("p") @DefaultValue("0.05") double p,
            @QueryParam("n") @DefaultValue("25") int n
           ) {

        String result = "";

        try {

            int sequenceLength = sequence.length();

            sequence = sequence.toUpperCase();

            int numTMS = predictTMS(sequence);


            if(numTMS > 0) {

                ArrayList<String> candidateModels = candidateModels(numTMS);

                Hashtable<String,Double> model2score = new Hashtable<String,Double>();

                ArrayList<MaxList> maxList = new ArrayList<MaxList>();
               

                File[] files = new File(HMM_DIR).listFiles();
                for(File file: files){

                    if(!file.getName().endsWith(".hmm")) continue;

                    String clusterCode = file.getName().replace(".hmm", "");

                    if(!candidateModels.contains(clusterCode)) {
                        continue;
                    }

                    //System.out.println("Loading hmm: " +file.getName());

                    SecOrderHMM hmm = loadHmmFromFile(file);
                    hmm.setClusterCode(clusterCode);

                    // test against all HMMs
                    double score = score(sequence, hmm);
                    //System.out.println("Cluster code: " +clusterCode+", score: "+score);

                    if((score + "").equals("NaN")){
                        //System.out.println("NaN " + k + " " + secOrderHMMs.get(k).getClusterId());
                    }else{

                        double zscore = getZScore(clusterCode, score);
                       
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

                    double threshold = getMetaModelThreshold(clusterCode);

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


                    //System.out.println("\tResult: "+clusterCode+"\t"+score+"\t"+SCORE_THRESHOLD);

                    if(score >= threshold) {

                        countResults++;

                        if(countResults>n) {
                            break;
                        }   

                        BigDecimal bd = new BigDecimal(score);
                        bd = bd.setScale(4, BigDecimal.ROUND_HALF_UP);
                        double scoreRounded = bd.doubleValue();

                        result += clusterCode +"\t" +scoreRounded+ "\t"+ pvalueStr +"\t"+zscore+"\n";
                    }
                }
            }

        }catch(Exception e) {
             e.printStackTrace(System.err);
        }

        return result;
    }



    /**
     * creates a new HMM out of the paramters from a given file
     *
     * @param fileName parameter file for the HMM
     * @return the new generated HMM
     * @throws Exception file not found Exception
     */
    private static SecOrderHMM loadHmmFromFile(File file)throws Exception{

        Hmm<ObservationInteger> hmm = null;
	String[] secOrder = null;
	int region=0; int i = 0; int nbStates = 0;int observables = 0;
	//Open an output stream

	try{

            Logger.getLogger(MetaModelClassification2.class.getName()).log(Level.FINE, "Loading HMM: ", file.getName());

            BufferedReader reader = null;
            reader = new BufferedReader(new FileReader((file)));

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
				break;
			case 1: observables = new Integer(line);
				hmm = new Hmm<ObservationInteger>(nbStates, new OpdfIntegerFactory(observables));
				break;
			case 2: String[] pi = line.split(",");
				for(int j=0; j<nbStates; j++){
                                    hmm.setPi(j, new Double(pi[j]));
				}
				break;
			case 3: String[] aj = line.split(",");
				for(int j=0; j<nbStates; j++){
                                    hmm.setAij(i, j, new Double(aj[j]));
				}
				i++;
				break;
			case 4: String[] intDistS = line.split(",");
				double[] intDist = new double[observables];
				for(int j=0; j<observables; j++){
                                    intDist[j] = new Double(intDistS[j]);
				}
				hmm.setOpdf(i, new OpdfInteger(intDist));
				i++;
				break;
			case 5: secOrder = line.split(",");
				break;
		    }
		}

            }
            reader.close();
	}catch(Exception e){
            //System.err.println(e.toString());
            return null;
	}
	String clusterCode = "";
	try{
            String fileName = file.getName();
            clusterCode = fileName.replace(".hmm", "");
	}
	catch(Exception e){
			System.err.println(e.getMessage());
	}
        //System.out.println("Load hmm and create new hmm with cluster code: " +clusterCode);
	return new SecOrderHMM(hmm, secOrder, clusterCode);
    }


    private static double score(String s1, SecOrderHMM secOrderHMM) throws Exception{

        Hmm<ObservationInteger> hmm = secOrderHMM.getHmm();
	ArrayList<ObservationInteger> seq = new ArrayList<ObservationInteger>();
	for(int i = 0; i < s1.length(); i++){
            try{
                int number =  ASMapping.asToInt(s1.charAt(i));
		seq.add(new ObservationInteger(number));
            }catch(Exception e){
                e.printStackTrace(System.err);
            }
	}

	double score = hmm.lnProbability(seq);
	return score;
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
            Logger.getLogger(MetaModelClassification2.class.getName()).log(Level.SEVERE, "Server Error IO", ex);
            throw new WebApplicationException(500);
        } catch(Exception e) {
             e.printStackTrace(System.err);
        }

        return numTMS;
    }

    private ArrayList<String> candidateModels(int numTMS) {

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

    private double getMetaModelThreshold(String clusterCode) {

        double threshold = -3;

        try{

            BufferedReader br = new BufferedReader(new FileReader(new File(THRESHOLDSFILE)));
            String line;
            while((line=br.readLine()) != null) {

                if(line.startsWith("#SC-cluster")) {    //skip header
                    continue;
                }

                String[] content = line.split("\t");

                String currentClusterCode = content[0].trim();

                if(currentClusterCode.equals(clusterCode)) {

                    threshold = Double.parseDouble(content[1].trim());
                    break;
                }
            }
            br.close();

        }catch(Exception e) {
            e.printStackTrace(System.err);
        }

        return threshold;
    }

    private String getPValue(String clusterCode, double score) {

        String pvalue = "1";

        try {

            //
            //parse file containing scores for non-members
            //
            File scoresFile = new File(SCORESFILES+clusterCode+"_nonmembers.scores");

            if(scoresFile.exists()) {

                int countScores = 0;
                int countScoresBetterOrEqualGivenScore = 0;

                BufferedReader br = new BufferedReader(new FileReader(scoresFile));
                String line;
                while((line=br.readLine()) != null) {

                    String[] content = line.split("\t");

                    String scoreStr = content[0].trim();

                    if(scoreStr.equals("NaN") || scoreStr.equals("-Infinity")) {
                        continue;
                    }


                    countScores++;
                    double currentScore = Double.parseDouble(scoreStr);

                    if(currentScore >= score) {
                        countScoresBetterOrEqualGivenScore++;
                    }
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


    private double roundPValue(double pvalue) {

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


    private double getZScore(String clusterCode, double score) {

        double zscore = 0;

        try {

            BufferedReader br = new BufferedReader(new FileReader(new File(THRESHOLDSFILE)));
            String line;
            while((line=br.readLine()) != null) {

                if(line.startsWith("#SC-cluster")) {    //skip header
                    continue;
                }

                String[] content = line.split("\t");

                String currentClusterCode = content[0].trim();

                if(currentClusterCode.equals(clusterCode)) {

                    String meanStr = content[8].trim();
                    String sdStr = content[10].trim();

                    if(!meanStr.equals("-") && !sdStr.equals("-")) {

                        double mean = Double.parseDouble(meanStr);
                        double sd = Double.parseDouble(sdStr);
                        
                        zscore = (score - mean)/sd;

                        BigDecimal bd = new BigDecimal(zscore);
                        bd = bd.setScale(4, BigDecimal.ROUND_HALF_UP);
                        zscore = bd.doubleValue();
                    }
                    break;
                }
            }
            br.close();

        }catch(Exception e) {
            e.printStackTrace(System.err);
        }

        return zscore;
    }
}
