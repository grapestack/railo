package railo.runtime.exp;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import railo.commons.io.IOUtil;
import railo.commons.io.res.Resource;
import railo.commons.io.res.util.ResourceUtil;
import railo.commons.lang.StringUtil;
import railo.runtime.Info;
import railo.runtime.PageContext;
import railo.runtime.PageSource;
import railo.runtime.dump.DumpData;
import railo.runtime.dump.DumpProperties;
import railo.runtime.dump.DumpTable;
import railo.runtime.dump.SimpleDumpData;
import railo.runtime.engine.ThreadLocalPageContext;
import railo.runtime.err.ErrorPage;
import railo.runtime.op.Caster;
import railo.runtime.type.Array;
import railo.runtime.type.ArrayImpl;
import railo.runtime.type.Collection;
import railo.runtime.type.KeyImpl;
import railo.runtime.type.List;
import railo.runtime.type.Struct;
import railo.runtime.type.StructImpl;
import railo.runtime.type.dt.DateTimeImpl;

/**
 * Railo Runtime Page Exception, all runtime Exception are sub classes of this class
 */
public abstract class PageExceptionImpl extends PageException {

	private static final Collection.Key RAW_TRACE = KeyImpl.getInstance("raw_trace");
	private static final Collection.Key TEMPLATE = KeyImpl.getInstance("template");
	private static final Collection.Key ID = KeyImpl.getInstance("id");
	private static final Collection.Key LINE = KeyImpl.getInstance("line");
	private static final Collection.Key TYPE = KeyImpl.getInstance("type");
	private static final Collection.Key COLUMN = KeyImpl.getInstance("column");
	private static final Collection.Key CODE_PRINT_HTML = KeyImpl.getInstance("codePrintHTML");
	private static final Collection.Key CODE_PRINT_PLAIN = KeyImpl.getInstance("codePrintPlain");
	
	
	
	
	private ArrayImpl tagContext=new ArrayImpl();
	private Struct additional=new StructImpl(Struct.TYPE_LINKED);
	/**
	 * Field <code>detail</code>
	 */
	protected String detail="";
	//private Throwable rootCause;
	private int tracePointer;
	private String errorCode="0";
	private String extendedInfo=null;

	private String type;
	private String customType;
	private boolean isInitTagContext=false;
	private LinkedList sources=new LinkedList();


	/**
	 * Class Constructor
	 * @param message Exception Message
	 * @param type Type as String
	 */
	public PageExceptionImpl(String message,String type) {
		this(message,type,null);
	}
	/**
	 * Class Constructor
	 * @param message Exception Message
	 * @param type Type as String
	 * @param customType CUstom Type as String
	 */
	public PageExceptionImpl(String message,String type, String customType) {
		super(message==null?"":message);
		//rootCause=this;
		this.type=type.toLowerCase().trim();
		this.customType=customType;
        //setAdditional("customType",getCustomTypeAsString());
	}
	
	/**
	 * Class Constructor
	 * @param e exception
	 * @param type Type as String
	 */
	public PageExceptionImpl(Throwable e,String type) {
		this(e,type,null);
	}
	
	/**
	 * Class Constructor
	 * @param e exception
	 * @param type Type as String
	 * @param customType CUstom Type as String
	 */
	public PageExceptionImpl(Throwable e,String type, String customType) {
		super(e.getMessage()==null || e.getMessage().trim().length()==0?e.getClass().getName():e.getMessage());
		if(e instanceof InvocationTargetException)e=((InvocationTargetException)e).getTargetException();
        
        //this.i
        initCause(e);
        //this.setStackTrace(e.getStackTrace());
        
		if(e instanceof IPageException) {
            IPageException pe=(IPageException)e;
			this.additional=pe.getAddional();
			this.setDetail(pe.getDetail());
			this.setErrorCode(pe.getErrorCode());
			this.setExtendedInfo(pe.getExtendedInfo());
		}
		
		//else if(e.getCause()!=null)rootCause=e.getCause();
		//else rootCause=e;

		this.type=type.toLowerCase().trim();
		this.customType=(customType==null)?this.type:customType;
	}
    
    /**
     * @see railo.runtime.exp.PageException#getDetail()
     */
	public String getDetail() { 
		if(detail==null || detail.equals(getMessage()))return "";
		return detail; 
	}
	
	/**
     * @see railo.runtime.exp.PageException#getErrorCode()
     */
	public String getErrorCode() { return errorCode==null?"":errorCode; }
	
	/**
     * @see railo.runtime.exp.PageException#getExtendedInfo()
     */
	public String getExtendedInfo() { return extendedInfo==null?"":extendedInfo; }
	
	/**
	 * @see railo.runtime.exp.IPageException#getTypeAsString()
	 *
	public abstract String getTypeAsString();*/
	

	/**
     * @see railo.runtime.exp.PageException#setDetail(java.lang.String)
     */
	public void setDetail(String detail) {
		this.detail=detail;
	}
	/**
     * @see railo.runtime.exp.PageException#setErrorCode(java.lang.String)
     */
	public void setErrorCode(String errorCode) {
		this.errorCode=errorCode;
	}
	/**
     * @see railo.runtime.exp.PageException#setExtendedInfo(java.lang.String)
     */
	public void setExtendedInfo(String extendedInfo) {
		this.extendedInfo=extendedInfo;
	}
	
	/**
	 * @see railo.runtime.exp.IPageException#getCatchBlock()
	 */
	public Struct getCatchBlock() {
		// TLPC
		return getCatchBlock(ThreadLocalPageContext.get());
	}
	
	/**
	 *
	 * @see railo.runtime.exp.IPageException#getCatchBlock(railo.runtime.PageContext)
	 */
	public Struct getCatchBlock(PageContext pc) {
		return new CatchBlock(pc,this);
	}
	
	public Array getTagContext(PageContext pc) {
		if(isInitTagContext) return tagContext;
		_getTagContext(pc,tagContext,this,sources);
		isInitTagContext=true;
		return tagContext;
	}
	

	public static Array getTagContext(PageContext pc,StackTraceElement[] traces) {
		ArrayImpl tagContext=new ArrayImpl();
		_getTagContext(pc,tagContext,traces,new LinkedList());
		return tagContext;
	}
	

	private static void _getTagContext(PageContext pc, ArrayImpl tagContext, Throwable t, LinkedList sources) {
		_getTagContext(pc, tagContext, getStackTraceElements(t), sources);
	}
	
	private static void _getTagContext(PageContext pc, ArrayImpl tagContext, StackTraceElement[] traces, LinkedList sources) {
		//StackTraceElement[] traces = getStackTraceElements(t);
		
		int line=0;
		String template="",tlast;
		Struct item;
		StackTraceElement trace=null;
		int index=-1;
		PageSource ps;
		for(int i=0;i<traces.length;i++) {
			trace=traces[i];
			tlast=template;
			template=trace.getFileName();
			if(trace.getLineNumber()<=0 || template==null || ResourceUtil.getExtension(template).equals("java")) continue;
			// content
			if(!StringUtil.toStringEmptyIfNull(tlast).equals(template))index++;
			
			String[] content=null;
			try {
				Resource res = ResourceUtil.toResourceNotExisting(pc, template);
				if(res.exists())	
					content=IOUtil.toStringArray(IOUtil.getReader(res, pc.getConfig().getTemplateCharset()));
				else {
					ps=(PageSource) sources.get(index);
					if(ps!=null && trace.getClassName().equals(ps.getFullClassName())) {
						if(ps.physcalExists())
							content=IOUtil.toStringArray(IOUtil.getReader(ps.getPhyscalFile(), pc.getConfig().getTemplateCharset()));
						template=ps.getDisplayPath();
					}
				}	
			} 
			catch (Throwable th) {
				th.printStackTrace();
			}
			
			// check last
			if(tagContext.size()>0){
				try {
					Struct last=(Struct) tagContext.getE(tagContext.size());
					if(last.get(RAW_TRACE).equals(trace.toString()))continue;
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			
			item=new StructImpl();
			line=trace.getLineNumber();
			item.setEL(TEMPLATE,template);
			item.setEL(LINE,new Double(line));
			item.setEL(ID,"??");
			item.setEL(RAW_TRACE,trace.toString());
			item.setEL(TYPE,"cfml");
			item.setEL(COLUMN,new Double(0));
			if(content!=null) {
				item.setEL(CODE_PRINT_HTML,getCodePrint(content,line,true));
				item.setEL(CODE_PRINT_PLAIN,getCodePrint(content,line,false));
			}
			else {
				item.setEL(CODE_PRINT_HTML,"");
				item.setEL(CODE_PRINT_PLAIN,"");
			}
			// FUTURE id 
			tagContext.appendEL(item);
		}
	}
	
	
	
	public int getPageDeep() {
		StackTraceElement[] traces = getStackTraceElements(this);
		
		String template="",tlast;
		StackTraceElement trace=null;
		int index=0;
		for(int i=0;i<traces.length;i++) {
			trace=traces[i];
			tlast=template;
			template=trace.getFileName();
			if(trace.getLineNumber()<=0 || template==null || ResourceUtil.getExtension(template).equals("java")) continue;
			if(!StringUtil.toStringEmptyIfNull(tlast).equals(template))index++;
			
		}
		return index;
	}
	
	
	/**
     * @see railo.runtime.exp.PageException#getErrorBlock(railo.runtime.PageContext, railo.runtime.err.ErrorPage)
     */
	public Struct getErrorBlock(PageContext pc,ErrorPage ep) {
		Struct struct=new StructImpl();

		struct.setEL("browser",pc.cgiScope().get("HTTP_USER_AGENT",""));
		struct.setEL("datetime",new DateTimeImpl(pc));
		struct.setEL("diagnostics",getMessage()+' '+getDetail()+"<br>The error occurred on line "+getLine(pc)+" in file "+getFile(pc)+".");
		struct.setEL("GeneratedContent","");// TODO content of a custom-tag thistag.GeneratedContent
		struct.setEL("HTTPReferer",pc.cgiScope().get("HTTP_REFERER",""));
		struct.setEL("mailto",ep.getMailto());
		struct.setEL("message",getMessage());
		struct.setEL("QueryString",StringUtil.toStringEmptyIfNull(pc. getHttpServletRequest().getQueryString()));
		struct.setEL("RemoteAddress",pc.cgiScope().get("REMOTE_ADDR",""));
		struct.setEL("RootCause",getCatchBlock(pc));
		struct.setEL("StackTrace",getStackTraceAsString());
		struct.setEL("template",pc. getHttpServletRequest().getServletPath());
		
			struct.setEL("Detail",getDetail());
			struct.setEL("ErrorCode",getErrorCode());
			struct.setEL("ExtendedInfo",getExtendedInfo());
			struct.setEL("type",getTypeAsString());
			struct.setEL("TagContext",getTagContext(pc));
			struct.setEL("additional",additional);
			// TODO RootCause,StackTrace
		
		return struct;
	}
	
	
	/**
	 * @return return the file where the failure occurred
	 */
	private String getFile(PageContext pc) {
        if(getTagContext(pc).size()==0) return "";
        
        Struct sct=(Struct) getTagContext(pc).get(1,null);
        return Caster.toString(sct.get("template",""),"");
    }
    /**
     * @see railo.runtime.exp.PageException#getLine()
     */
	public String getLine(PageContext pc) {
        if(getTagContext(pc).size()==0) return "";
        
        Struct sct=(Struct) getTagContext(pc).get(1,null);
        return Caster.toString(sct.get("line",""),"");
    }
    /**
     * @see railo.runtime.exp.PageException#addContext(railo.runtime.PageSource, int, int)
     */
	public void addContext(PageSource pr, int line, int column, StackTraceElement element) {
		if(line==-187) {
			sources.add(pr);
			return;
		}
        
		Struct struct=new StructImpl();
        //print.out(pr.getDisplayPath());
		try {
			String[] content=pr.getSource();
			struct.set("template",pr.getDisplayPath());
			struct.set("line",new Double(line));
			struct.set("id","??");
			struct.set("Raw_Trace",(element!=null)?element.toString():"");
			struct.set("Type","cfml");
			struct.set("column",new Double(column));
			if(content!=null){
				struct.set("codePrintHTML",getCodePrint(content,line,true));
				struct.set("codePrintPlain",getCodePrint(content,line,false));
			}
			tagContext.append(struct);
		} 
		catch (Exception e) {}
	}
	
	private static String getCodePrint(String[] content,int line, boolean asHTML ) {
		StringBuffer sb=new StringBuffer();
		// bad Line
		for(int i=line-2;i<line+3;i++) {
			if(i>0 && i<=content.length) {
				if(asHTML && i==line)sb.append("<b>");
				if(asHTML)sb.append(i+": "+StringUtil.escapeHTML(content[i-1]));
				else sb.append(i+": "+(content[i-1]));
				if(asHTML && i==line)sb.append("</b>");
				if(asHTML)sb.append("<br>");
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	
	/**
	 * @see railo.runtime.dump.Dumpable#toDumpData(railo.runtime.PageContext, int)
	 */
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		
		//FFFFCF
    	DumpTable htmlBox = new DumpTable("#FFB200","#FFCC00","#350606");
		htmlBox.setTitle("Railo ["+Info.getVersionAsString()+"] - Error ("+StringUtil.ucFirst(getTypeAsString())+")");
		
		
		// Message
		htmlBox.appendRow(1,new SimpleDumpData("Message"),new SimpleDumpData(getMessage()));
		
		// Detail
		String detail=getDetail();
		if(!StringUtil.isEmpty(detail,true))
			htmlBox.appendRow(1,new SimpleDumpData("Detail"),new SimpleDumpData(detail));
		
		// additional
		Iterator it=additional.keyIterator();
		while(it.hasNext()) {
			String key=it.next().toString();
			htmlBox.appendRow(1,new SimpleDumpData(key),new SimpleDumpData(additional.get(key,"").toString()));
		}
		
		Array tagContext = getTagContext(pageContext);
		// Context MUSTMUST
		if(tagContext.size()>0) {
			Collection.Key[] keys=tagContext.keys();
			DumpTable context=new DumpTable("#FFB200","#FFCC00","#350606");
			//context.setTitle("The Error Occurred in");
			//context.appendRow(0,new SimpleDumpData("The Error Occurred in"));
			context.appendRow(7,
					new SimpleDumpData(""),
					new SimpleDumpData("template"),
					new SimpleDumpData("line"));
			try {
				for(int i=0;i<keys.length;i++) {
					Struct struct=(Struct)tagContext.get(keys[i],null);
					context.appendRow(1,
							new SimpleDumpData(i>0?"called from ":"occurred in"),
							new SimpleDumpData(struct.get("template","")+""),
							new SimpleDumpData(Caster.toString(struct.get("line",null))));
					
					
				}
				htmlBox.appendRow(1,new SimpleDumpData("Context"),context);
				
				
				//((StructImpl)tagContext.get(1,null)).get("codePrintHTML","").toString();
				// Code
				String strCode=((Struct)tagContext.get(1,null)).get("codePrintPlain","").toString();
				String[] arrCode = List.listToStringArray(strCode, '\n');
				arrCode=List.trim(arrCode);
				DumpTable code=new DumpTable("#FFB200","#FFCC00","#350606");
				
				for(int i=0;i<arrCode.length;i++) {
					code.appendRow(i==2?1:0,new SimpleDumpData(arrCode[i]));
				}
				htmlBox.appendRow(1,new SimpleDumpData("Code"),code);

			} 
            catch (PageException e) {}
		}
		
		
		// Java Stacktrace
		String strST=getStackTraceAsString();
		String[] arrST = List.listToStringArray(strST, '\n');
		arrST=List.trim(arrST);
		DumpTable st=new DumpTable("#FFB200","#FFCC00","#350606");
		
		for(int i=0;i<arrST.length;i++) {
			st.appendRow(i==0?1:0,new SimpleDumpData(arrST[i]));
		}
		htmlBox.appendRow(1,new SimpleDumpData("Java Stacktrace"),st);

		return htmlBox; 
	}	
	
	/**
     * @see railo.runtime.exp.PageException#getStackTraceAsString()
     */
	public String getStackTraceAsString() {
		
        StringWriter sw=new StringWriter();
	    PrintWriter pw=new PrintWriter(sw);
        printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    
    
    
    /**
     * @see railo.runtime.exp.PageException#printStackTrace()
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }
    
    /**
     * @see railo.runtime.exp.PageException#printStackTrace(java.io.PrintStream)
     */
    public void printStackTrace(PrintStream s) {
        StackTraceElement[] traces = getStackTraceElements(this);
        StackTraceElement trace;
        
        s.println(getMessage());
        for(int i=0;i<traces.length;i++){
            trace=traces[i];
            s.println("\tat "+trace);
        }
    }
    
    /**
     * @see railo.runtime.exp.PageException#printStackTrace(java.io.PrintWriter)
     */
    public void printStackTrace(PrintWriter s) {
    	StackTraceElement[] traces = getStackTraceElements(this);
        StackTraceElement trace;
        
        s.println(getMessage());
        for(int i=0;i<traces.length;i++){
            trace=traces[i];
            s.println("\tat "+trace+":"+trace.getLineNumber());
        }
    }
    
    private static StackTraceElement[] getStackTraceElements(Throwable t) {
    	Throwable cause=t.getCause();
    	if(cause==null) return t.getStackTrace();

    	ArrayList causes=new ArrayList();
    	fillStackTraceElements(causes,t);
    	
		return (StackTraceElement[]) causes.toArray(new StackTraceElement[causes.size()]);
	}
    

    private static void fillStackTraceElements(ArrayList causes, Throwable t) {
		if(t==null) return;
		fillStackTraceElements(causes, t.getCause());
		StackTraceElement[] traces = t.getStackTrace();
		for(int i=0;i<traces.length;i++) {
			//if(causes.contains(traces[i]))
			causes.add(traces[i]);
		}
	}
    
    
    
	/*public static void printStackTraceX(PrintWriter s,Throwable t) {
        
        StackTraceElement[] traces = getStackTraceElements(t);
        StackTraceElement trace;
        
        for(int i=0;i<traces.length;i++){
            trace=traces[i];
            s.println("\tat "+trace+":"+trace.getLineNumber());
        }
        t=t.getCause();
        if(t!=null) {
            s.println();
            printStackTraceX(s,t);
        }
    }*/
    
    
	/**
	 * set a additional key value
	 * @param key
	 * @param value
	 */
	public void setAdditional(String key, Object value) {
		additional.setEL(KeyImpl.init(key),value);
	}
	
	
	/**
     * @see railo.runtime.exp.PageException#getRootCause()
     */
	public Throwable getRootCause() {
        Throwable cause=this; 
        Throwable temp; 
        
        while((temp=cause.getCause())!=null)cause=temp;
        return cause;
	}

	/**
     * @see railo.runtime.exp.PageException#getTracePointer()
     */
	public int getTracePointer() {
		return tracePointer;
	}
	/**
     * @see railo.runtime.exp.PageException#setTracePointer(int)
     */
	public void setTracePointer(int tracePointer) {
		this.tracePointer = tracePointer;
	}
	
	/* *
     * @see railo.runtime.exp.PageException#typeEqual(java.lang.String)
     *  /
	public final boolean typeEqual(String type) {
	    type=type.toLowerCase().trim();
	    print.ln(getTypeAsString()+"-"+type);
		// ANY
        if(type.equals("any")) return true;
				
		// translate customtype ->custom_type
		else if(type.equals("customtype")) return typeEqual("custom_type");
		
        // Tye Compare
		else if(getTypeAsString().equalsIgnoreCase(type)) return true;
		
        // Custom Type
		else if(getTypeAsString().equals("custom_type")) {
		    return compareCustomType(type,getCustomTypeAsString().toLowerCase().trim());
		}
        // Native Compare
        
		return false;		
	}*/
    
    /**
     * @see railo.runtime.exp.IPageException#typeEqual(java.lang.String)
     */
    public boolean typeEqual(String type) {
    	if(type==null) return true;
        type=StringUtil.toUpperCase(type);
    	// ANY
        if(type.equals("ANY")) return true;// MUST check
        // Type Compare
        if(getTypeAsString().equalsIgnoreCase(type)) return true;
        return getClass().getName().equalsIgnoreCase(type);
    }
    
    /**
     * @see railo.runtime.exp.PageException#getTypeAsString()
     */
	public String getTypeAsString() {
		return type;
	}
	
	/**
     * @see railo.runtime.exp.PageException#getCustomTypeAsString()
     */
	public String getCustomTypeAsString() {
		return customType==null?type:customType;
	}
    
    /**
     * @see railo.runtime.exp.PageException#getAdditional()
     */
	public Struct getAdditional() {
        return additional;
    }public Struct getAddional() {
        return additional;
    }
    
    /**
     * @see java.lang.Throwable#getStackTrace()
     */
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }
    
    /*public static void printStackTrace(PrintWriter s,Throwable t) {
        
        //while((temp=cause.getCause())!=null)cause=temp;
        
        StackTraceElement[] traces = t.getStackTrace();
        StackTraceElement trace;
        
        s.println(t.getMessage());
        for(int i=0;i<traces.length;i++){
            trace=traces[i];
            s.println("\tat "+trace+":"+trace.getLineNumber());
        }
        t=t.getCause();
        if(t!=null)printStackTrace(s,t);
    }*/
    
    
    
}