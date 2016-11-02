package be.ac.ulg.montefiore.run.jahmm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Pseudocount {
	private double[] probabilities;	
	
	private double[] q;
	private double[][] a_ji;
	private double[] sum_a_j;
	
	
	//private static String directory = "/home/users/hartmann/workspace/HMM/resources";
	private static String directory = "F:/RunMetaModel/HMM/resources";		// laptop
	//private static String directory = "/home/users/saeed/HMM/resources";		// office
	
//	private static String directory = "resources";
		
	public static void main(String[] argv){
		System.out.println(Math.exp(GammaFunction.lnGamma(4)));
		Pseudocount ps = new Pseudocount(new double[20]);
		ps.print();
	}
	private void print(){
		System.out.println("q:");
		for(double tmp : q){
			System.out.print(tmp + "\t");
		}
		System.out.println();
		System.out.println("a_ji:");
		for(double[] a_j : a_ji){
			for(double a : a_j){
				System.out.print(a + "    \t");
			}
			System.out.println();
		}
		System.out.println("sum_A_j:");
		for(double tmp : sum_a_j){
			System.out.print(tmp + "   \t");
		}
		System.out.println();		
	}
	
	public Pseudocount(double[] probabilities){
		this.probabilities = probabilities;
		q = new double[9];
		a_ji = new double[9][20];
		sum_a_j = new double[9];
		try{
			BufferedReader reader = new BufferedReader(new FileReader(directory + "/Blocks9.plib"));
						
			int j = -1;			
			String line;						
			while((line = reader.readLine()) != null){				
				if(line.startsWith("Number")){
					j = new Integer(line.split("= ")[1]);					
				}else if(line.startsWith("Mixture")){
					q[j] = new Double(line.split("= ")[1].trim());					
				}else if(line.startsWith("Alpha")){
					String[] values = line.split("= ")[1].split(" ");
					sum_a_j[j] = new Double(values[0].trim());
					for(int i = 1; i < values.length; i++){
						a_ji[j][i-1] = new Double(values[i].trim());
					}
				}	
			}			
			reader.close();
		}catch(Exception e){
			System.err.println(e.toString());			
		}			
	}
	
	public double[] getPosteriorEstimates(double nbSequences){
		nbSequences *= 0.8;
		int size = probabilities.length;
		double[] X = new double[size];		
		double[] n = new double[size];
//		double[] ajn = new double[size];
		double sumN = 0;
		
		for(int i=0; i < size; i++){
			n[i] = probabilities[i] * nbSequences; 
			sumN += n[i];			
		}		
		for(int i=0; i<size; i++){			
			for(int j=0; j<9; j++){				
				double quot = (a_ji[j][i] + n[i])/(sum_a_j[j] + sumN);				
				double quot2 = getB(addVectors(a_ji[j],n))/getB(a_ji[j]);
//				System.out.println(i + "," + j + ": " + quot2);
				X[i] += q[j] * quot * quot2;
			}
		}
//		for(double xi : X){
//			System.out.print(xi + "\t");
//		}
//		System.out.println();
		return X;
	}
	private double[] addVectors(double[] a, double[] b){
		double[] res = new double[a.length];
		for(int i = 0; i<a.length; i++){
			res[i]=a[i] + b[i];
		}
		return res;
	}
	

	private double getB(double[] a){
		double B = 0;
		double nominator = 0;
		double sumA=0;
		for(int i = 0; i < a.length; i++){			
			nominator += GammaFunction.lnGamma(a[i]);
			sumA+=a[i];
		}		
		B = Math.exp(nominator - GammaFunction.lnGamma(sumA));
		return B;
	}
}
