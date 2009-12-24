package railo.runtime.exp;

import java.util.ArrayList;

import railo.runtime.PageContext;
import railo.runtime.config.ConfigImpl;
import railo.transformer.library.function.FunctionLib;
import railo.transformer.library.function.FunctionLibFunction;
import railo.transformer.library.function.FunctionLibFunctionArg;

/**
 * specified exception for Build In Function
 */
public final class FunctionException extends ExpressionException {

	/* *
	 * constructor of the class
	 * @param pc current Page Context
	 * @param functionName Name of the function that thorw the Exception
	 * @param badArgumentPosition Position of the bad argument in the Argument List of the function
	 * @param badArgumentName Name of the bad Argument
	 * @param message additional Exception message
	 * /
    public FunctionException(PageContext pc,String functionName, String badArgumentPosition, String badArgumentName, String message) {
        this((PageContext)pc,functionName,badArgumentPosition,badArgumentName,message);
    }*/
    
    /**
     * constructor of the class
     * @param pc current Page Context
     * @param functionName Name of the function that thorw the Exception
     * @param badArgumentPosition Position of the bad argument in the Argument List of the function
     * @param badArgumentName Name of the bad Argument
     * @param message additional Exception message
     */
	public FunctionException(PageContext pc,String functionName, int badArgumentPosition, String badArgumentName, String message) {
        this(pc,functionName,toStringBadArgumentPosition(badArgumentPosition),badArgumentName,message);
    }
	
	private static String toStringBadArgumentPosition(int pos) {
		switch(pos) {
		case 1:return "first";
		case 2:return "second";
		case 3:return "third";
		case 4:return "forth";
		case 5:return "fifth";
		case 6:return "sixth";
		case 7:return "seventh";
		case 8:return "eighth";
		case 9:return "ninth";
		case 10:return "tenth";
		case 11:return "eleventh";
		case 12:return "twelfth";
		}
		// TODO Auto-generated method stub
		return pos+"th";
	}

	public FunctionException(PageContext pc,String functionName, String badArgumentPosition, String badArgumentName, String message) {
        super("invalid call of the function "+functionName+", "+(badArgumentPosition)+" Argument ("+badArgumentName+") is invalid, "+message);
        setAdditional("function info",getFunctionInfo(pc,functionName));
    }
    
	private static String getFunctionInfo(PageContext pc,String functionName) {
		FunctionLib[] flds;
		flds = ((ConfigImpl)pc.getConfig()).getFLDs();
				
		FunctionLibFunction function=null;
		for(int i=0;i<flds.length;i++) {
			function = flds[i].getFunction(functionName.toLowerCase());
			if(function!=null)break;
		}
		if(function == null) return "";
		
		StringBuffer rtn=new StringBuffer();
		rtn.append(function.getName()+"(");
		
		
		int optionals=0;
		ArrayList args = function.getArg();
		for(int i=0;i<args.size();i++) {
			FunctionLibFunctionArg arg=(FunctionLibFunctionArg) args.get(i);
			if(i!=0)rtn.append(", ");
			if(!arg.getRequired()) {
				rtn.append("[");
				optionals++;
			}
			rtn.append(arg.getTypeAsString());
		}
		for(int i=0;i<optionals;i++)
			rtn.append("]");
		rtn.append("):"+function.getReturnTypeAsString());
		
		return rtn.toString();
	}
	


}