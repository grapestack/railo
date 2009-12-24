package railo.commons.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarEntry;
import org.apache.commons.compress.archivers.tar.TarInputStream;
import org.apache.commons.compress.archivers.tar.TarOutputStream;
import org.apache.commons.compress.bzip2.CBZip2OutputStream;

import railo.commons.io.res.Resource;
import railo.commons.io.res.ResourceProvider;
import railo.commons.io.res.ResourcesImpl;
import railo.commons.io.res.filter.ExtensionResourceFilter;
import railo.commons.io.res.filter.OrResourceFilter;
import railo.commons.io.res.filter.ResourceFilter;
import railo.commons.lang.StringUtil;
import railo.runtime.op.Caster;

/**
 * Util to manipulate zip files
 */
public final class CompressUtil {

    /**
     * Field <code>FORMAT_ZIP</code>
     */
    public static final int FORMAT_ZIP=0;
    /**
     * Field <code>FORMAT_TAR</code>
     */
    public static final int FORMAT_TAR=1;
    /**
     * Field <code>FORMAT_TGZ</code>
     */
    public static final int FORMAT_TGZ=2;
    /**
     * Field <code>FORMAT_GZIP</code>
     */
    public static final int FORMAT_GZIP=3;
    /**
     * Field <code>FORMAT_BZIP</code>
     */
    public static final int FORMAT_BZIP=4;
    /**
     * Field <code>FORMAT_BZIP</code>
     */
    public static final int FORMAT_BZIP2=4;

    /**
     * Field <code>FORMAT_TBZ</code>
     */
    public static final int FORMAT_TBZ=5;
    /**
     * Field <code>FORMAT_TBZ2</code>
     */
    public static final int FORMAT_TBZ2=5;
    
    /**
     * Constructor of the class
     */
    private CompressUtil(){}

    /**
     * extract a zip file to a directory
     * @param format 
     * @param source 
     * @param target 
     * @throws IOException
    */
    public static void extract(int format,Resource source, Resource target) throws IOException {
    	if(format==FORMAT_ZIP)    	extractZip(source,target);
    	else if(format==FORMAT_TAR)	extractTar(source,target);
    	else if(format==FORMAT_GZIP)extractGZip(source,target);
    	else if(format==FORMAT_TGZ)	extractTGZ(source,target);
        else throw new IOException("can't extract in given format");
    }
    

    private static void extractTGZ(Resource source, Resource target) throws IOException {
    	//File tmpTarget = File.createTempFile("_temp","tmp");
    	Resource tmp = SystemUtil.getTempDirectory().getRealResource(System.currentTimeMillis()+".tmp");
        try {
	        // read Gzip
	        extractGZip(source, tmp);
	        
	        // read Tar
        	extractTar(tmp, target);
        }
        finally {
        	tmp.delete();
        }
	}

	private static void extractGZip(Resource source, Resource target) throws IOException {
    	InputStream is=null;
    	OutputStream os=null;
		try {
			is = new GZIPInputStream(IOUtil.toBufferedInputStream(source.getInputStream()));
			os = IOUtil.toBufferedOutputStream(target.getOutputStream());
			IOUtil.copy(is,os,false,false);
		} 
		finally {
			IOUtil.closeEL(is, os);
		}
	}

	/**
     * extract a zip file to a directory
     * @param format 
     * @param sources
     * @param target 
     * @throws IOException
    */
    public static void extract(int format,Resource[] sources, Resource target) throws IOException {
        if(format==FORMAT_ZIP  || format==FORMAT_TAR) {
            for(int i=0;i<sources.length;i++) {
                extract(format,sources[i],target);
            }
        }
        else throw new IOException("can't extract in given format");
    }
    
    private static void extractTar(Resource tarFile, Resource targetDir) throws IOException {
        if(!targetDir.exists() || !targetDir.isDirectory())
            throw new IOException(targetDir+" is not a existing directory");
        
        if(!tarFile.exists())
            throw new IOException(tarFile+" is not a existing file");
        
        if(tarFile.isDirectory()) {
        	Resource[] files = tarFile.listResources(new ExtensionResourceFilter("tar"));
            if(files==null) 
                throw new IOException("directory "+tarFile+" is empty");
            extract(FORMAT_TAR,files,targetDir);
            return;
        }
        
//      read the zip file and build a query from its contents
        TarInputStream tis=null;
        try {
	        tis = new TarInputStream( IOUtil.toBufferedInputStream(tarFile.getInputStream()) ) ;     
	        TarEntry entry;
	        int mode;
	        while ( ( entry = tis.getNextEntry()) != null ) {
	        	//print.ln(entry);
	        	Resource target=targetDir.getRealResource(entry.getName());
	            if(entry.isDirectory()) {
	                target.mkdirs();
	            }
	            else {
	            	Resource parent=target.getParentResource();
	                if(!parent.exists())parent.mkdirs();
	                
	                IOUtil.copy(tis,target,false);
	            }
	            target.setLastModified(entry.getModTime().getTime());
	            mode=entry.getMode();
	            if(mode>0)target.setMode(mode);
	            //tis.closeEntry() ;
	        }
        }
        finally {
        	IOUtil.closeEL(tis);
        }
    }
    
    private static void extractZipOld(Resource zipFile, Resource targetDir) throws IOException {
        if(!targetDir.exists() || !targetDir.isDirectory())
            throw new IOException(targetDir+" is not a existing directory");
        
        if(!zipFile.exists())
            throw new IOException(zipFile+" is not a existing file");
        
        if(zipFile.isDirectory()) {
        	Resource[] files = zipFile.listResources(new ExtensionResourceFilter("tar"));
            if(files==null) 
                throw new IOException("directory "+zipFile+" is empty");
            extract(FORMAT_ZIP,files,targetDir);
            return;
        }
        
//      read the zip file and build a query from its contents
        TarInputStream tis=null;
        try {
	        tis = new TarInputStream( IOUtil.toBufferedInputStream(zipFile.getInputStream()) ) ;     
	        TarEntry entry;
	        int mode;
	        while ( ( entry = tis.getNextEntry()) != null ) {
	        	//print.ln(entry);
	        	Resource target=targetDir.getRealResource(entry.getName());
	            if(entry.isDirectory()) {
	                target.mkdirs();
	            }
	            else {
	            	Resource parent=target.getParentResource();
	                if(!parent.exists())parent.mkdirs();
	                
	                IOUtil.copy(tis,target,false);
	            }
	            target.setLastModified(entry.getModTime().getTime());
	            mode=entry.getMode();
	            if(mode>0)target.setMode(mode);
	            //tis.closeEntry() ;
	        }
        }
        finally {
        	IOUtil.closeEL(tis);
        }
    }
    
    private static void extractZip(Resource zipFile, Resource targetDir) throws IOException {
        if(!targetDir.exists() || !targetDir.isDirectory())
            throw new IOException(targetDir+" is not a existing directory");
        
        if(!zipFile.exists())
            throw new IOException(zipFile+" is not a existing file");
        
        if(zipFile.isDirectory()) {
        	Resource[] files = zipFile.listResources(
                    new OrResourceFilter(new ResourceFilter[]{
                            new ExtensionResourceFilter("zip"),
                            new ExtensionResourceFilter("jar"),
                            new ExtensionResourceFilter("war"),
                            new ExtensionResourceFilter("tar"),
                            new ExtensionResourceFilter("ear")
                    })
            );
            if(files==null) 
                throw new IOException("directory "+zipFile+" is empty");
            extract(FORMAT_ZIP,files,targetDir);
            return;
        }
        
//      read the zip file and build a query from its contents
        unzip(zipFile,targetDir);
        /*ZipInputStream zis=null;
        try {
	        zis = new ZipInputStream( IOUtil.toBufferedInputStream(zipFile.getInputStream()) ) ;     
	        ZipEntry entry;
	        while ( ( entry = zis.getNextEntry()) != null ) {
	        	Resource target=targetDir.getRealResource(entry.getName());
	            if(entry.isDirectory()) {
	                target.mkdirs();
	            }
	            else {
	            	Resource parent=target.getParentResource();
	                if(!parent.exists())parent.mkdirs();
	                
	                IOUtil.copy(zis,target,false);
	            }
	            target.setLastModified(entry.getTime());
	           zis.closeEntry() ;
	        }
        }
        finally {
        	IOUtil.closeEL(zis);
        }*/
    }
    

    private static void unzip(Resource zipFile,Resource targetDir) throws IOException {
    	/*if(zipFile instanceof File){
    		unzip((File)zipFile, targetDir);
    		return;
    	}*/
    
    	ZipInputStream zis=null;
        try {
	        zis = new ZipInputStream( IOUtil.toBufferedInputStream(zipFile.getInputStream()) ) ;     
	        ZipEntry entry;
	        while ( ( entry = zis.getNextEntry()) != null ) {
	        	Resource target=targetDir.getRealResource(entry.getName());
	            if(entry.isDirectory()) {
	                target.mkdirs();
	            }
	            else {
	            	Resource parent=target.getParentResource();
	                if(!parent.exists())parent.mkdirs();
	                IOUtil.copy(zis,target,false);
	            }
	            target.setLastModified(entry.getTime());
	            zis.closeEntry() ;
	        }
        }
        finally {
        	IOUtil.closeEL(zis);
        }
	}
    
    private static void unzip2(File zipFile,Resource targetDir) throws IOException {
    	ZipFile zf=null;
        try {
	        zf = new ZipFile(zipFile);
	        
	        ZipEntry entry;
	        Enumeration en = zf.entries();
	        while(en.hasMoreElements()){
	        	entry = (ZipEntry) en.nextElement();
	        	Resource target=targetDir.getRealResource(entry.getName());
	            if(entry.isDirectory()) {
	                target.mkdirs();
	            }
	            else {
	            	Resource parent=target.getParentResource();
	                if(!parent.exists())parent.mkdirs();
	                InputStream is = zf.getInputStream(entry);
	                IOUtil.copy(is,target,true);
	            }
	            target.setLastModified(entry.getTime());
	        }
        }
        finally {
        	IOUtil.closeEL(zf);
        }
	}

	/**
     * compress data to a zip file
     * @param format format it that should by compressed usally is CompressUtil.FORMAT_XYZ
     * @param source
     * @param target
     * @param includeBaseFolder 
     * @param mode 
     * @throws IOException
     */
    public static void compress(int format, Resource source, Resource target, boolean includeBaseFolder, int mode) throws IOException {
        if(     format==FORMAT_GZIP)    compressGZip(source,target);
        else if(format==FORMAT_BZIP2)   compressBZip2(source,target);
        else {
        	Resource[] sources=(!includeBaseFolder && source.isDirectory())?source.listResources():new Resource[]{source};
            compress(format, sources, target,mode);
        }
    }

    /**
     * compress data to a zip file
     * @param format format it that should by compressed usally is CompressUtil.FORMAT_XYZ
     * @param sources
     * @param target
     * @param mode 
     * @throws IOException
     */
    public static void compress(int format, Resource[] sources, Resource target, int mode) throws IOException {
                
        if(     format==FORMAT_ZIP)     compressZip(sources,target,null);
        else if(format==FORMAT_TAR)     compressTar(sources,target,mode);
        else if(format==FORMAT_TGZ)     compressTGZ(sources,target,mode);
        else if(format==FORMAT_TBZ2)    compressTBZ2(sources,target,mode);

        else throw new IOException("can't compress in given format");
    }

    /**
     * compress a source file/directory to a tar/gzip file
     * @param sources
     * @param target
     * @param mode 
     * @throws IOException
     */
    public static void compressTGZ(Resource[] sources, Resource target,int mode) throws IOException {
        File tmpTarget = File.createTempFile("_temp","tmp");
        try {
	        // write Tar
        	OutputStream tmpOs=new FileOutputStream(tmpTarget);
	        try {
	        	compressTar(sources,tmpOs,mode);
	        }
	        finally {
	        	IOUtil.closeEL(tmpOs);
	        }
	    	
	        // write Gzip
	        InputStream is = null;
	        OutputStream os = null;
	        try {
		        is = new FileInputStream(tmpTarget);
		        os = target.getOutputStream();
	        	compressGZip(is, os);
	        }
	        finally {
	        	IOUtil.closeEL(is,os);
	        }
        }
        finally {
        	tmpTarget.delete();
        }
    }
    
    /**
     * compress a source file/directory to a tar/bzip2 file
     * @param sources
     * @param target
     * @param mode 
     * @throws IOException
     */
    private static void compressTBZ2(Resource[] sources, Resource target, int mode) throws IOException {
        //File tmpTarget = File.createTempFile("_temp","tmp");
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        compressTar(sources,baos,mode);
        _compressBZip2(new ByteArrayInputStream(baos.toByteArray()), target.getOutputStream());
        //tmpTarget.delete();
    }

    /**
     * compress a source file to a gzip file
     * @param source
     * @param target
     * @throws IOException 
     * @throws IOException
     */
    private static void compressGZip(Resource source, Resource target) throws IOException {
        if(source.isDirectory()) {
            throw new IOException("you can only create a GZIP File from a single source file, use TGZ (TAR-GZIP) to first TAR multiple files");
        }
        InputStream is=null;
        OutputStream os=null;
		try {
			is = source.getInputStream();
			os = target.getOutputStream();
		} catch(IOException ioe) {
			IOUtil.closeEL(is, os);
			throw ioe;
		}
        compressGZip(is,os);
        
    }

    public static void compressGZip(InputStream source, OutputStream target) throws IOException {
        InputStream is = IOUtil.toBufferedInputStream(source);
        if(!(target instanceof GZIPOutputStream)) target = new GZIPOutputStream(IOUtil.toBufferedOutputStream(target));
        IOUtil.copy(is,target,true,true);       
    }

    /**
     * compress a source file to a bzip2 file
     * @param source
     * @param target
     * @throws IOException
     */
    private static void compressBZip2(Resource source, Resource target) throws IOException {
        if(source.isDirectory()) {
            throw new IOException("you can only create a BZIP File from a single source file, use TBZ (TAR-BZIP2) to first TAR multiple files");
        }
        InputStream is=null;
        OutputStream os=null;
        try {
        	is=source.getInputStream();
        	os=target.getOutputStream();
        }
        catch(IOException ioe) {
        	IOUtil.closeEL(is, os);
        	throw ioe;
        }
        
        _compressBZip2(is, os);
    }

    /**
     * compress a source file to a bzip2 file
     * @param source
     * @param target
     * @throws IOException
     */
    private static void _compressBZip2(InputStream source, OutputStream target) throws IOException {
        
        InputStream is = IOUtil.toBufferedInputStream(source);
        OutputStream os = new CBZip2OutputStream(IOUtil.toBufferedOutputStream(target));
        IOUtil.copy(is,os,true,true);
    }
    
    /**
     * compress a source file/directory to a zip file
     * @param sources
     * @param target
     * @param filter 
     * @throws IOException
     */
    public static void compressZip(Resource[] sources, Resource target, ResourceFilter filter) throws IOException {
        ZipOutputStream zos = null;
        try {
        	zos = new ZipOutputStream(IOUtil.toBufferedOutputStream(target.getOutputStream()));
            compressZip("",sources, zos, filter);
        }
        finally {
            IOUtil.closeEL(zos);
        }
    }
    
    public static void compressZip( Resource[] sources, ZipOutputStream zos, ResourceFilter filter) throws IOException {
        compressZip("",sources, zos, filter);
    }
    

    private static void compressZip(String parent, Resource[] sources, ZipOutputStream zos, ResourceFilter filter) throws IOException {
        if(parent.length()>0)parent+="/";
        for(int i=0;i<sources.length;i++) { 
            compressZip(parent+sources[i].getName(),sources[i],zos,filter);
        }
    }
    
    private static void compressZip(String parent, Resource source, ZipOutputStream zos, ResourceFilter filter) throws IOException {
    	if(source.isFile()) {
    		//if(filter.accept(source)) {
	    		ZipEntry ze=new ZipEntry(parent);
	    		ze.setTime(source.lastModified());
	    		zos.putNextEntry(ze);
	            try {
	                IOUtil.copy(source,zos,false);
	            } 
	            finally {
	                zos.closeEntry();
	            }
    		//}
        }
        else if(source.isDirectory()) {
        	if(!StringUtil.isEmpty(parent)) {
        		ZipEntry ze=new ZipEntry(parent+"/");
        		ze.setTime(source.lastModified());
        		try {
        			zos.putNextEntry(ze);
        		}
        		catch(IOException ioe) {
        			if(Caster.toString(ioe.getMessage()).indexOf("duplicate")==-1)throw ioe;
        		}
        		zos.closeEntry();
        	}
            compressZip(parent, filter==null?source.listResources():source.listResources(filter),zos,filter);
        }
    }

    /**
     * compress a source file/directory to a tar file
     * @param sources
     * @param target
     * @param mode 
     * @throws IOException
     */
    public static void compressTar(Resource[] sources,Resource target, int mode) throws IOException {
        compressTar(sources, IOUtil.toBufferedOutputStream(target.getOutputStream()), mode);
    }
    
    public static void compressTar(Resource[] sources,OutputStream target, int mode) throws IOException {
        if(target instanceof TarOutputStream){
            compressTar("",sources,(TarOutputStream)target,mode);
            return;
        }
    	TarOutputStream tos=new TarOutputStream(target);
        tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        try {
            compressTar("",sources, tos,mode);
        }
        finally {
    		IOUtil.closeEL(tos);
        }
    }
    
    public static void compressTar(String parent, Resource[] sources, TarOutputStream tos, int mode) throws IOException {

        if(parent.length()>0)parent+="/";
        for(int i=0;i<sources.length;i++) { 
            compressTar(parent+sources[i].getName(),sources[i],tos,mode);
        }
    }
    
    private static void compressTar(String parent, Resource source,TarOutputStream tos, int mode) throws IOException {
    	if(source.isFile()) {
    		//TarEntry entry = (source instanceof FileResource)?new TarEntry((FileResource)source):new TarEntry(parent);
    		TarEntry entry = new TarEntry(parent);
            
            entry.setName(parent);
            
            // mode
            //100777 TODO ist das so ok?
            if(mode>0)	entry.setMode(mode);
            else if((mode=source.getMode())>0)	entry.setMode(mode);
            
            entry.setSize(source.length());
            entry.setModTime(source.lastModified());
            tos.putNextEntry(entry);
            try {
            	IOUtil.copy(source,tos,false);
            } 
            finally {
        		tos.closeEntry();
            }
        }
        else if(source.isDirectory()) {
            compressTar(parent, source.listResources(),tos,mode);
        }
    }

    public static void main(String[] args) throws IOException {
    	ResourceProvider frp = ResourcesImpl.getFileResourceProvider();
    	Resource src = frp.getResource("/Users/mic/temp/a");
    	
    	Resource tgz = frp.getResource("/Users/mic/temp/b/a.tgz");
    	tgz.getParentResource().mkdirs();
    	Resource tar = frp.getResource("/Users/mic/temp/b/a.tar");
    	tar.getParentResource().mkdirs();
    	Resource zip = frp.getResource("/Users/mic/temp/b/a.zip");
    	zip.getParentResource().mkdirs();
    	
    	Resource tgz1 = frp.getResource("/Users/mic/temp/b/tgz");
    	tgz1.mkdirs();
    	Resource tar1 = frp.getResource("/Users/mic/temp/b/tar");
    	tar1.mkdirs();
    	Resource zip1 = frp.getResource("/Users/mic/temp/b/zip");
    	zip1.mkdirs();
    	
    	compressTGZ(new Resource[]{src}, tgz, -1);
    	compressTar(new Resource[]{src}, tar, -1);
    	compressZip(new Resource[]{src}, zip, null);
    	
    	extractTGZ(tgz, tgz1);
    	extractTar(tar, tar1);
    	extractZip(src, zip1);
    	
	}
}