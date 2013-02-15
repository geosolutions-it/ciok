package it.geosolutions.geobatch.gaez.utils.rules;

public enum FileType {
	
	F,
	C,
	C4,
	P4,
	NOVALUE;
	
	public boolean isDiscrete(){
		switch (this){
		case F:
			return true;
		default:
			return false;
		}
	}
	public static FileType toFileType(String str){
	    try {
	        return valueOf(str);
	    	} 
	    catch (Exception ex) 
	    	{
	        return NOVALUE;
	    	}
	}
	
}



