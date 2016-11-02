package camps;

public class MaxList implements Comparable<Object>{
	private double max;
	private String clusterCode;

        public MaxList(double max, String clusterCode){
		this.max= max;
		this.clusterCode = clusterCode;
	}

        public String getClusterCode() {
            return this.clusterCode;
        }

        public double getMax() {
            return this.max;
        }
	
	public int compareTo(Object b){
		MaxList tmp = (MaxList)b;
		if(tmp.max > this.max){
			return 1;
		}
		else if(tmp.max < this.max){
			return -1;
		}
		else return 0;
	}
}
