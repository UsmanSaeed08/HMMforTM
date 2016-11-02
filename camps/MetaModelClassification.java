/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package binfo.rws.camps;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfIntegerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * REST Web Service
 *
 * @author sneumann
 */

@Path("metaModel")
public class MetaModelClassification {

    private static final double SCORE_THRESHOLD = -3;           // worst score to be ok for classification
    private static final double CONFIDENCE_THRESHOLD = 0.03;	// minimum distance to second best meta-model
    //private static final String HMM_DIR = "/home/proj/Camps3/data4website/HMMs/";
    private static final String HMM_DIR = "/ssd/camps3/HMMs/";

    
    /**
     * Retrieves representation of an instance of binfo.rws.camps.MetaModelClassification
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    @Path("/doIt/sequence/{sequence}")
    public String doIt(
            @PathParam("sequence") String sequence,
            @QueryParam("m") @DefaultValue("0") int m
           ) {

        String result = "";

        try {

            int sequenceLength = sequence.length();

                               
            ArrayList<MaxList> maxList = new ArrayList<MaxList>();
            double max = Double.NEGATIVE_INFINITY;

            double confidence = 1;


            File[] files = new File(HMM_DIR).listFiles();
            for(File file: files){

                if(!file.getName().endsWith(".hmm")) continue;

                //System.out.println("Loading hmm: " +file.getName());

                SecOrderHMM hmm = loadHmmFromFile(file);
                String clusterCode = file.getName().replace(".hmm", "");               
                hmm.setClusterCode(clusterCode);

                // test against all HMMs
                double score = score(sequence, hmm);
                //System.out.println("Cluster code: " +clusterCode+", score: "+score);

                if((score + "").equals("NaN")){
                    //System.out.println("NaN " + k + " " + secOrderHMMs.get(k).getClusterId());
                }else{
                    maxList.add(new MaxList(score,clusterCode));
                }

                if( score > max){
                    max = score;
                }
            }

            Collections.sort(maxList);
            //confidence = max - maxList.get(1).max;
            confidence = max - maxList.get(1).getMax();

            //System.out.println(confidence);

            int countResults = 0;


            if(m == 0) {   //non-restrictive search

                if(confidence >= CONFIDENCE_THRESHOLD && max >= SCORE_THRESHOLD){

                    //sequence could be classified unambiguously

                    countResults = 1;

                    MaxList ml = maxList.get(0);

                    //String clusterCode = ml.clusterCode;
                    String clusterCode = ml.getClusterCode();
                    //double score = ml.max;
                    double score = ml.getMax();

                    result += clusterCode +"\t" +score+"\n";

                }
                else {
                    for(MaxList ml: maxList) {

                        //String clusterCode = ml.clusterCode;
                        String clusterCode = ml.getClusterCode();
                        //double score = ml.max;
                        double score = ml.getMax();

                        //System.out.println("\tResult: "+clusterCode+"\t"+score+"\t"+SCORE_THRESHOLD);

                        if(score >= SCORE_THRESHOLD) {

                        countResults++;

                        if(countResults>10) {
                            break;
                        }

                            result += clusterCode +"\t" +score+"\n";
                        }
                    }
                }


                
            }
            else if(m == 1){   //restrictive search

                if(confidence >= CONFIDENCE_THRESHOLD && max >= SCORE_THRESHOLD){

                    //sequence could be classified unambiguously

                    countResults = 1;

                    MaxList ml = maxList.get(0);

                    //String clusterCode = ml.clusterCode;
                    String clusterCode = ml.getClusterCode();
                    //double score = ml.max;
                    double score = ml.getMax();

                    result += clusterCode +"\t" +score+"\n";

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

            Logger.getLogger(MetaModelClassification.class.getName()).log(Level.FINE, "Loading HMM: ", file.getName());
            
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
}
