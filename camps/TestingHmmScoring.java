package camps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import util.hmm.HMMGraph;
import util.hmm.HMMGraphData;
import util.hmm.HMMMath;

public class TestingHmmScoring {

	/**
	 * @param args
	 */
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
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		String sequence = "MLGLFFRQVWTLTVKNLLIVLVRPTFTTTLRALVLPVIFVAFISFAKNLFIPPAEYGIGSPTPVRSLGDALGAVSGSRDKVVFVHNGLTGGDIEQVINRVAEPVRTTQKQVQVLSSENELRELCRTSLRGVSSCVAAAVFYSSPTEGPGGHWNYSIRTDGALGVGIKVDQRDNDQETYLLPFQHSIDWAIAQANASSEQNALPNELLEYPFTSLTQAERKDQIRTRYMGaiidiiavaifievvGVTYQLTGLIAMERELGMSQLIDCMMPSHSHWQSQAARFISAHLALDIVYGPAWVIAGIILKYGVFRRTSAGILVIYNILAGLSLASFSVFGASFFRKAQLSGISIVIACLLLGVIAQLAPASSSGAVGILSLLFPPMNFVYFFVLMARWERQNLPTNLVHAAPNSSHSIPGIAFWILLIVQIIVYPVLAAVVERALYGTTSKSRKILNTDDPTALSLNGFTKTYRPSWFYRNVASRFGSTRQSVYAVNDLSMNVRKGEIVVLLGANGSGKSTTLDAisglttissgsidiNYGGSGGRFGLCPQKNVLWDTLTVKEHVKIFNKLKSTGEVdkddellkllddcdlSHKVNARSKTLSGGQKRKVQLAMMLTGGSSICAVDEVSSGIDPIARAKIWDILLAARGSRTILLTTHFLDEADLLADHITILSKGVLRAQGSSVELKDRLGSGYRIHVLNVPGSEKVTGSEFENIPKEVHFDDTVYTVKDSATASRLMSMLEQKGITEYRVSGPTIEDVFLKVAEELDSNSVHEEISVGQKGTIVSEKNATDGEHGGLQLLTGKRISMLLQSWYLFRKRATILRRNPVPYLAALLIPVIAAGLVTLFLKDATKPTCSGESTYRASSSESLASQNNFKFVIGPSDMITPSVLENFVSSLSGFTKPAQKESLDVESHFHLVNSLAEFDDYISTNYANVTPGGFYLGDTNSAPTFAWKGDNGEFPLSAFAQNALDNIITGTPIHFQFQYFDIPWQSGAGKTLQLIVYFGLAMAVYPALFSLYPTVEQLKNVRALHFSNGVRGVSLWLAYLTFDFCivvassvlaiiiFRAVSDVWYHAEYLFVVFFLYGLCSTILAYVVSLFSKSQLAAFAIAAGGQCVLFLIYFIAYMSVLTYAPTQKVDDYLQVTHFTIGIISPTGNLLRALFTALNTFSILCRGREIASYPGEITLYGGPILYLILQSLVLFGLLLWVDRGPTFSMLRRTKNKDEEEKNAVDGDVAAELARVADSTDGLRVLHLSkkfkkfVAVDDVTFGVPKSQVFALLGPNGAGKTTTITLIRGDMQPSDNGGDILVNEVSVLKNRAAARSHLGVCPQFDAMDQMTVIEHLEFYARIRGVPDVKHNVTEVIRAVGLTSFQDRMATKLSGGNKRKLSLGIALMGNPQVLLLDEPSSGMDAASKRVMWKTLASVVPGRSIVLTTHSMEEADALATRAGIMAKRMLALGTTDYLRKKYGNKYHVHLVHSRAPHTTDADMARIREWVQDSFPSAVIEQKTYHGQVRFSVPATAEIIPSNEKSIERDKASVTADKFGRDVSDEPDVQRQEPKATNNNIVSKLFSKLEQGRALLGIQYYSVSQTTLDQVFLTIVGQHRVEEENSG"; // seq to test
		//String HMM_DIR = "F:/SC_Clust_postHmm/HMMs_old/HMMs_old/";	//old camps
		String HMM_DIR = "F:/SC_Clust_postHmm/RunMetaModel_gef/RunMetaModel_gef/HMMs/CAMPS4_1/";	//new
		
		// run one time only... cuz files would be serialized after one run
		 for (File f : new File(HMM_DIR).listFiles()) {
			String fileName = f.getName();
			if (fileName.endsWith(".hmm")) {
				System.out.println(fileName);
				HMMGraphData hmm = loadHmmData(f);
				serializeObjectToFile(hmm, new File(HMM_DIR, fileName + ".serialized"));
			}
		}
		
		
		
		ArrayList <String> seq; // array list of test sequences
		double max = Double.NEGATIVE_INFINITY;
		
		for (File f : new File(HMM_DIR).listFiles()) {
			String fileName = f.getName();
			if (fileName.endsWith(".hmm")) {
				System.out.println(fileName);
				HMMGraph hmm = loadHMM(fileName, HMM_DIR);
				int sequenceLength = sequence.length(); 
				double [][] farr = null;
				farr = checkForStateCount(farr, hmm.getStates().length, sequenceLength);
				// test against all HMMs
				double score = score(sequence, hmm, farr);
				System.out.print("Score: "+score + "for HMM" + fileName + "\n");
				if (score > max){
					max = score;
				}
			}
		}
		System.out.print("MaxScore: "+ max + "\n");
		
	
	
		

	}

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
	private static HMMGraph loadHMM(String clusterName, String HMM_DIR) throws IOException, ClassNotFoundException {
		File hmmSerialized = new File(HMM_DIR, clusterName + ".serialized");
		InputStream bis = new BufferedInputStream(new FileInputStream(hmmSerialized));
		ObjectInputStream is = new ObjectInputStream(bis);
		Object obj = is.readObject();
		is.close();
		HMMGraphData data = (HMMGraphData)obj;
		HMMGraph hmm = new HMMGraph();
		hmm.init(data);
		return hmm;
	}
	private static void serializeObjectToFile(Serializable obj, File out) throws IOException {
		OutputStream bos = new BufferedOutputStream(new FileOutputStream(out));
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(obj);
		os.close();
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

}
