package camps;

/**
 * This class is used to map an amino acid to an integer value and back
 * 
 * @author hartmann
 *
 */

public class ASMapping {	
	
	/**
	 * Maps an amino acid to an integer value
	 * 
	 * @param as amino acid
	 * @return	corresponding integer value	 * 
	 */
	public static int asToInt(char as)throws Exception{
		as = Character.toUpperCase(as);
		switch(as){
			case 'A':return 0;
			case 'C':return 1;
			case 'D':return 2;
			case 'E':return 3;
			case 'F':return 4;
			case 'G':return 5;
			case 'H':return 6;
			case 'I':return 7;
			case 'K':return 8;
			case 'L':return 9;
			case 'M':return 10;
			case 'N':return 11;
			case 'P':return 12;
			case 'Q':return 13;
			case 'R':return 14;
			case 'S':return 15;
			case 'T':return 16;
			case 'V':return 17;
			case 'W':return 18;
			case 'Y':return 19;			
			default :throw new Exception(as + " not a valid as");
		}		
	}
	
	/**
	 * Maps an integer value to an amino acid
	 * 
	 * @param number integer value corresponding to an amino acid
	 * @return corresponding amino acid	 *  
	 */
	public static char intToAS(int number)throws Exception{
		switch(number){
			case 0: return 'A';
			case 1: return 'C';
			case 2: return 'D';
			case 3: return 'E';
			case 4: return 'F';
			case 5: return 'G';
			case 6: return 'H';
			case 7: return 'I';
			case 8: return 'J';
			case 9: return 'L';
			case 10:return 'M';
			case 11:return 'N';
			case 12:return 'P';
			case 13:return 'Q';
			case 14:return 'R';
			case 15:return 'S';
			case 16:return 'T';
			case 17:return 'V';
			case 18:return 'W';
			case 19:return 'Y';
			default:throw new Exception(number + " not a valid as identifier");
		}		
	}
}
