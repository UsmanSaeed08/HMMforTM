package camps;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;

public class SecOrderHMM {

		private Hmm<ObservationInteger> hmm;
		private String[] secOrder;
		private String clusterCode;
		
		
		public SecOrderHMM(Hmm<ObservationInteger> hmm, String[] secOrder, String clusterCode){
			this.hmm = hmm;
			this.secOrder = secOrder;
			this.clusterCode = clusterCode;
		}

                public void setClusterCode(String clusterCode) {
                    this.clusterCode = clusterCode;
                }
	
		public String getClusterCode(){
			return clusterCode;
		}
		public Hmm<ObservationInteger> getHmm(){
			return hmm;
		}
		public String[] getSecOrder(){
			return secOrder;
		}		
}
