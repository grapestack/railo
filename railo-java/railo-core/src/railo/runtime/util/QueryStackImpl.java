package railo.runtime.util;

import railo.runtime.PageContext;
import railo.runtime.engine.ThreadLocalPageContext;
import railo.runtime.type.Objects;
import railo.runtime.type.Query;
import railo.runtime.type.QueryColumn;
import railo.runtime.type.QueryImpl;
import railo.runtime.type.Collection.Key;

/**
 * Stack for Query Objects
 */
public final class QueryStackImpl implements QueryStack {
	Query[] queries=new QueryImpl[20];
	int start=queries.length;
	
	/**
     * @see railo.runtime.util.QueryStack#addQuery(railo.runtime.type.Query)
     */
	public void addQuery(Query query) {
        if(start<1)grow();
        queries[--start]= query;
	}
	public void addQuery(QueryImpl query) {
        if(start<1)grow();
        queries[--start]=query;
	}

    /**
     * @see railo.runtime.util.QueryStack#removeQuery()
     */
	public void removeQuery() {
        //print.ln("queries["+start+"]=null;");
        queries[start++]=null;
	}
	
	/**
     * @see railo.runtime.util.QueryStack#isEmpty()
     */
	public boolean isEmpty() {
		return start==queries.length;
	}

	/**
     * @see railo.runtime.util.QueryStack#getDataFromACollection(java.lang.String)
     */
	public Object getDataFromACollection(String key) {
		return getDataFromACollection(ThreadLocalPageContext.get(),key);
	}
	
	/**
	 * @see railo.runtime.util.QueryStack#getDataFromACollection(railo.runtime.PageContext, java.lang.String)
	 */
	public Object getDataFromACollection(PageContext pc,String key) {
		Object rtn=null;
		
		// get data from queries
		for(int i=start;i<queries.length;i++) {
			rtn=((Objects)queries[i]).get(pc,key,"");
			if(rtn!=null) {
				return rtn;
			}
		}
		return null;
	}

	/**
	 * @see railo.runtime.util.QueryStack#getDataFromACollection(railo.runtime.type.Collection.Key)
	 */
	public Object getDataFromACollection(Key key) {
		return getDataFromACollection(ThreadLocalPageContext.get(),key); 
	}
	
	/**
	 * @see railo.runtime.util.QueryStack#getDataFromACollection(railo.runtime.PageContext, railo.runtime.type.Collection.Key)
	 */
	public Object getDataFromACollection(PageContext pc,Key key) {
		Object rtn=null;
		
		// get data from queries
		for(int i=start;i<queries.length;i++) {
			rtn=((QueryImpl)queries[i]).get(pc,key,"");
			if(rtn!=null) {
				return rtn;
			}
		}
		return null;
	}
	
	/**
     * @see railo.runtime.util.QueryStack#getColumnFromACollection(java.lang.String)
     */
	public QueryColumn getColumnFromACollection(String key) {
		QueryColumn rtn=null;
		
		// get data from queries
		for(int i=start;i<queries.length;i++) {
			rtn=queries[i].getColumn(key,null);
			if(rtn!=null) {
				return rtn;
			}
		}
		return null;
	}

	/**
	 *
	 * @see railo.runtime.util.QueryStack#getColumnFromACollection(railo.runtime.type.Collection.Key)
	 */
	public QueryColumn getColumnFromACollection(Key key) {
		QueryColumn rtn=null;
		
		// get data from queries
		for(int i=start;i<queries.length;i++) {
			rtn=queries[i].getColumn(key,null);
			if(rtn!=null) {
				return rtn;
			}
		}
		return null;
	}
	
	/**
     * @see railo.runtime.util.QueryStack#clear()
     */
	public void clear() {
		for(int i=start;i<queries.length;i++) {
			queries[i]=null;
		}
		start=queries.length;
	}
    
    private void grow() {
        Query[] tmp=new QueryImpl[queries.length+20];
        for(int i=0;i<queries.length;i++) {
            tmp[i+20]=queries[i];
        }
        queries=tmp;
        start+=20;
    }
    
	/**
	 * @see railo.runtime.util.QueryStack#getQueries()
	 */
	public Query[] getQueries() {
		Query[] tmp=new QueryImpl[queries.length-start];
		int count=0;
		for(int i=start;i<queries.length;i++) {
			tmp[count++]=queries[i];
		}
		return tmp;
	}
}