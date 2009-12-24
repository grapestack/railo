package railo.runtime.tag;

import railo.commons.lang.StringUtil;
import railo.runtime.exp.ApplicationException;
import railo.runtime.exp.CatchBlock;
import railo.runtime.exp.CustomTypeException;
import railo.runtime.exp.PageException;
import railo.runtime.ext.tag.TagImpl;
import railo.runtime.op.Caster;
import railo.runtime.type.ObjectWrap;

/**
* The cfthrow tag raises a developer-specified exception that can be caught with cfcatch tag 
*   having any of the following type specifications - cfcatch type = 'custom_type', cfcatch type = 'Application'
*   'cfcatch' type = 'Any'
*
*
*
**/
public final class Throw extends TagImpl {

	/** A custom error code that you supply. */
	private String extendedinfo=null;

	/** A custom type or the predefined type Application. Do not enter any other predefined types because 
	** 		they are not generated by ColdFusion applications. If you specify the exception type Application, you 
	** 		need not specify a type for cfcatch, because the Application type is the default cfcatch type */
	private String type="application";

	/** A detailed description of the event. The ColdFusion server appends the position of the error to 
	** 		this description; the server uses this parameter if an error is not caught by your code. */
	private String detail="";

	/** A message that describes the exceptional event. */
	private String message="";

	/** A custom error code that you supply. */
	private String errorcode="";

	/** a native java exception Object, if this attribute is defined all other will be ignored. */
	private Object object;

	/**
	* @see javax.servlet.jsp.tagext.Tag#release()
	*/
	public void release()	{
		super.release();
		extendedinfo=null;
		type="application";
		detail="";
		message="";
		errorcode="";
		object=null;
	}



	/** set the value extendedinfo
	*  A custom error code that you supply.
	* @param extendedinfo value to set
	**/
	public void setExtendedinfo(String extendedinfo)	{
		this.extendedinfo=extendedinfo;
	}

	/** set the value type
	*  A custom type or the predefined type Application. Do not enter any other predefined types because 
	* 		they are not generated by ColdFusion applications. If you specify the exception type Application, you 
	* 		need not specify a type for cfcatch, because the Application type is the default cfcatch type
	* @param type value to set
	**/
	public void setType(String type)	{
		this.type=type;
	}

	/** set the value detail
	*  A detailed description of the event. The ColdFusion server appends the position of the error to 
	* 		this description; the server uses this parameter if an error is not caught by your code.
	* @param detail value to set
	**/
	public void setDetail(String detail)	{
		this.detail=detail;
	}

	/** set the value message
	*  A message that describes the exceptional event.
	* @param message value to set
	**/
	public void setMessage(String message)	{
		this.message=message;
	}

	/** set the value errorcode
	*  A custom error code that you supply.
	* @param errorcode value to set
	**/
	public void setErrorcode(String errorcode)	{
		this.errorcode=errorcode;
	}

	/** set the value object
	*  a native java exception Object, if this attribute is defined all other will be ignored.
	* @param object object to set
	 * @throws PageException
	**/
	public void setObject(Object object) throws PageException	{
		if((object instanceof ObjectWrap))
			setObject(((ObjectWrap)object).getEmbededObject());
		else this.object=object;
	}


	/**
	* @see javax.servlet.jsp.tagext.Tag#doStartTag()
	*/
	public int doStartTag() throws PageException	{
		if(object instanceof String && StringUtil.isEmpty(message)) {
			message=object.toString();
		}
		else if(object!=null) {
			if(object instanceof CatchBlock) {
				CatchBlock cb = (CatchBlock)object;
				throw cb.getPageException();
			}
			if(object instanceof PageException) throw (PageException)object;
			if(object instanceof Throwable) {
				Throwable t=(Throwable)object;
				throw new CustomTypeException(t.getMessage(),"","",t.getClass().getName());
			}
			throw new ApplicationException("attribute object of type cfthrow must define a Exception Object, now ("+Caster.toClassName(object)+")");
			
			
			
		}
		
		CustomTypeException exception = new CustomTypeException( message,detail,errorcode,type);
		if(extendedinfo!=null)exception.setExtendedInfo(extendedinfo);
		throw exception;
		//return SKIP_BODY;
	}

	/**
	* @see javax.servlet.jsp.tagext.Tag#doEndTag()
	*/
	public int doEndTag()	{
		return EVAL_PAGE;
	}
}