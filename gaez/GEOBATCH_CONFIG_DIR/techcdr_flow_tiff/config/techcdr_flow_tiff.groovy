package it.geosolutions.geobatch.action.scripting;

import it.geosolutions.geobatch.flow.event.action.ActionException;

import it.geosolutions.geobatch.action.scripting.ScriptingConfiguration;
import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;

import it.geosolutions.geobatch.catalog.file.FileBasedCatalogImpl;
import it.geosolutions.geobatch.configuration.CatalogConfiguration;
import it.geosolutions.geobatch.global.CatalogHolder;

import com.sun.org.apache.xpath.internal.FoundIndex;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.filefilter.EmptyFileFilter;

import it.geosolutions.tools.commons.file.Path;
import it.geosolutions.tools.io.file.Collector;
import it.geosolutions.tools.io.file.writer.*;
import it.geosolutions.tools.compress.file.Extract;

import java.io.File;
import java.util.Queue;

import com.thoughtworks.xstream.XStream;


// FreeMarker
import it.geosolutions.geobatch.actions.freemarker.*;

// TaskExecutor
import it.geosolutions.geobatch.task.*;
import it.geosolutions.geobatch.task.TaskExecutorConfiguration;

// utils
import it.geosolutions.geobatch.gaez.utils.*;

// COPY FILES
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Callable;

// LOG
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


    /**
     * Script Main "execute" function
     **/
public Map execute(Map argsMap) throws Exception {
	
	final ScriptingConfiguration configuration=argsMap.get(ScriptingAction.CONFIG_KEY);
	final File temporaryDir=argsMap.get(ScriptingAction.TEMPDIR_KEY);

	final File configDir=argsMap.get(ScriptingAction.CONFIGDIR_KEY);
	final List events=argsMap.get(ScriptingAction.EVENTS_KEY);
	final ProgressListenerForwarder listenerForwarder=argsMap.get(ScriptingAction.LISTENER_KEY);

	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ScriptingAction.class");
	
	LOGGER.info("debug is: "+LOGGER.isDebugEnabled());

// output directory
	if (outputDirName==null){
	    String message="::techCDR:: check flow configuration: property \'outputDirName\' is null!";
	    ActionException e=new ActionException(message);
	    listenerForwarder.failed(e);
	    throw e;
	}
	final File outputDir=new File(outputDirName);
	outputDir.mkdirs();
	if (!outputDir.exists()){
	    String message="::techCDR:: check flow configuration "+outputDirName+" is not writeable";
	    ActionException e=new ActionException(message);
	    listenerForwarder.failed(e);
	    throw e;
	}

// backup directory
	if (backupDirName==null){
	    String message="::techCDR:: check flow configuration: property \'backupDirName\' is null!";
	    ActionException e=new ActionException(message);
	    listenerForwarder.failed(e);
	    throw e;
	}
	final File backupDir=new File(backupDirName);
	backupDir.mkdirs();
	if (!backupDir.exists()){
	    String message="::techCDR:: check flow configuration "+backupDirName+" is not writeable";
	    ActionException e=new ActionException(message);
	    listenerForwarder.failed(e);
	    throw e;
	}

	//final ThreadFactory threadFactory = Executors.defaultThreadFactory();
	//final ExecutorService es = Executors.newCachedThreadPool(threadFactory);
	//final int nThreads=5;
	//final ExecutorService es = Executors.newFixedThreadPool(nThreads);
	
	if (LOGGER.isDebugEnabled()) {		
//		LOGGER.debug("::techCDR:: workingdir : "+workingDirName);
		LOGGER.debug("::techCDR:: tempDir: "+temporaryDir);
		LOGGER.debug("::techCDR:: outputDir: "+outputDirName);
		LOGGER.debug("::techCDR:: backupDir: "+backupDirName);
	}

// starting script
        listenerForwarder.started();

// get the input from the method argument call
	FileSystemEvent ev=(FileSystemEvent) events.get(0);

	final File inputFile=ev.getSource();
	final String inputFileName=inputFile.getAbsolutePath();

	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("::techCDR:: Starting conversion of the file named: "+inputFileName);
	}

	final String inputFileBaseName=FilenameUtils.getBaseName(inputFileName);
	
	
// errorFile
	final File errorFile=new File(temporaryDir,inputFileBaseName+".err");
	errorFile.createNewFile();

// backupFile
	final File backupFile=new File(inputFileName);

	try {	
		// extract incoming file
		final String extractedFileName=Extract.extract(inputFileName,false);
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("::techCDR:: extractedFileName: "+extractedFileName);
		}
		if (extractedFileName==null){
		    String message="::techCDR:: unable to extract incoming file: "+inputFileName;
		    Exception e=new Exception(message);
		    throw e;
		}
		final File extractedFile=new File(extractedFileName);
		

// tilize file
		File tiledRasterFile=null;
		
// MAIN FILE CHECK and GDAL_TRANSLATE

		// check if extracted file is a directory
		if (!extractedFile.isDirectory()){
		    // incoming file is a single uncompressed file
// check and valorize tiledRasterFile
		    tiledRasterFile=gdal_translate(configDir,extractedFile,null,errorFile,temporaryDir,listenerForwarder)
		    if (tiledRasterFile==null){
			String message="::techCDR:: Incoming uncompressed file named \'"+extractedFile.getName()+"\' is not in a supported format";
			Exception e=new Exception(message);
			throw e;
		    }
		} else {
		    // collect all the files in that dir
		    // collect recursively all the file but the 'prj'
		    final Collector c=new Collector(FileFilterUtils.and(FileFilterUtils.fileFileFilter(),FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("*.prj",IOCase.INSENSITIVE))));
		    final List<File> fileList=c.collect(extractedFile);
		    if (fileList.size()<1){
			String message="::techCDR:: No input file found into: "+inputFileName+" aborting translation.";
			Exception e=new Exception(message);
			throw e;
		    }
// search for prj file
		    c.setFilter(FileFilterUtils.nameFileFilter("*.prj",IOCase.INSENSITIVE));
		    final List<File> prjFiles=c.collect(extractedFile);
		    final File prjFile;
		    if (prjFiles.size()>1){
			String message="::techCDR:: more than one prj files found into: "+inputFileName+" aborting translation.";
			Exception e=new Exception(message);
			throw e;
		    } else if (prjFiles.size()==1){
			prjFile=prjFiles.get(0);
		    } else {
			prjFile=null;
		    }

		    boolean success=false;
//		    File newErrorFile=File.createTempFile("temp_", errorFile.getName(), temporaryDir);
		    for (File file : fileList){
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("::techCDR:: trying to translate: "+file.getName());
			}
// check and valorize tiledRasterFile
			try {
				tiledRasterFile=gdal_translate(configDir,file,prjFile,errorFile,temporaryDir,listenerForwarder);
			} catch (Exception e){
// append message to errorFile
				ExceptionWriter.appendStack(errorFile, e);
				//append(errorFile,e.getLocalizedMessage());
			}
			if (tiledRasterFile!=null){
				// success!
				success=true;
				break;
			}
		    }
		    if (!success){
			String message="::techCDR:: No supported file found into: "+inputFileName;
			Exception e=new Exception(message);
			throw e;
		    }
		}

// ADDING OVERVIEW AND CHECK RESULTS
		final File overviewRasterFile;
// check and valorize overviewRasterFile
		File newErrorFile=File.createTempFile("overview_", errorFile.getName(), temporaryDir);
		try {
			overviewRasterFile=gdaladdo(configDir,tiledRasterFile,newErrorFile,temporaryDir,listenerForwarder)
		} catch (Exception e){
// append message to errorFile
			//append(errorFile,e.getLocalizedMessage());
			ExceptionWriter.appendStack(errorFile, e);
		}
		if (overviewRasterFile==null){
		    // append errors to errorfile
		    Writer.appendFile(newErrorFile, errorFile);
		    String message="::techCDR:: Incoming uncompressed file named \'"+extractedFile.getName()+"\' is not in a supported format";
		    Exception e=new Exception(message);
		    throw e;
		}
		
// RENAME and MOVE FILES TO Final dir
		File outputFile=new File(outputDir,inputFileBaseName+".tif");
		FileUtils.copyFile(overviewRasterFile, outputFile, true);
		
// also copy aux.xml file if present
		final File auxFile=new File(tiledRasterFile.getParent(),inputFileBaseName+".tif.aux.xml");
		if (auxFile.exists()){
		    FileUtils.copyFile(auxFile, new File(outputDir,inputFileBaseName+".tif.aux.xml"), true);
		}
		

		listenerForwarder.progressing(100,"completed");
		listenerForwarder.completed();
		// ////
		// forwarding event to the next action
		// dummy results
		final List results = new ArrayList();
		results.add(outputFile);
		final Map retMap=new HashMap();
		retMap.put(ScriptingAction.RETURN_KEY,results)
		return retMap;
		

	} catch (Throwable t){

		
// TODO append message to errorFile
		final String message="::techCDR:: problem " +t.getLocalizedMessage();

// perform error handling
		try {
		      FileUtils.moveFileToDirectory(errorFile, backupDir, true);
		} catch (Exception e){
		      listenerForwarder.failed(e);
		      if (LOGGER.isErrorEnabled()) {
			  LOGGER.error("Unable to write error to error file: "+e.getLocalizedMessage());
		      }
// append message to errorFile
		      // append(errorFile,e.getLocalizedMessage());
		      ExceptionWriter.appendStack(errorFile, e);
		}
		
		listenerForwarder.failed(t);
		throw t;
	} finally {

// perform backup
		try {
		      FileUtils.moveFileToDirectory(backupFile, backupDir, true);
		} catch (Exception e){
		      listenerForwarder.failed(e);
		      if (LOGGER.isErrorEnabled()) {
			  LOGGER.error(e.getLocalizedMessage());
		      }
// append message to errorFile
		      //append(errorFile,e.getLocalizedMessage());
		      ExceptionWriter.appendStack(errorFile, e);
		}

// close Executor Service
		//if (es!=null){
		//	es.shutdownNow();
		//}
	}
	
	
}


File gdal_translate(File configDir, File rasterFile, File prjFile, File errorFile, File temporaryDir, ProgressListenerForwarder listenerForwarder) throws Exception {
	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ScriptingAction.class");
// FREEMARKER -> GDALTRANSLATE
	listenerForwarder.setTask("FreeMarker - gdal_translate");
	// ----------------------- FreeMarker ----------------
	Queue queue=new LinkedList();
	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("-------------------------- FreeMarker - gdal_translate--------------------");
	}

	FreeMarkerConfiguration fmc=new FreeMarkerConfiguration("freemarker_gdal_translate_id","techCDR_freemarker","freemarker_gdal_translate");
	
	// relative to the configDir
	fmc.setInput(translateTemplateName);

// output file
	final File outputFile=new File(rasterFile.getParent(),"tiled_"+rasterFile.getName());

	// params to inject into the ROOT datadir for FreeMarker
	final Map<String,String> fmRoot=new HashMap<String,String>();
	fmRoot.put("DESTDIR",outputFile.getParent());
	fmRoot.put("FILENAME",outputFile.getName());
	fmc.setRoot(fmRoot);

	// output data dir for xml task executor command (temporary files)
	fmc.setOutput(temporaryDir.getAbsolutePath());

	// SIMULATE THE EventObject on the queue 
	FileSystemEvent imageEventFile=new FileSystemEvent(rasterFile,FileSystemEventType.FILE_ADDED);

	queue.add(imageEventFile);
// add prjFile
	if (prjFile!=null)
		queue.add(new FileSystemEvent(prjFile,FileSystemEventType.FILE_ADDED));

	FreeMarkerAction fma=new FreeMarkerAction(fmc);
	fma.setTempDir(temporaryDir);
	// SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
	fma.setConfigDir(configDir);

	queue=fma.execute(queue);

	if (queue.size()>0){
// IT'S ALL OK
		// leaving the image on the queue to pass it to the TaskExecutor
	}
	else {
		String message="::techCDR:: The output event queue from freemarker does not contains events...";
		ActionException e=new ActionException(FreeMarkerAction.class, message);
		listenerForwarder.failed(e);
		throw e;
	}

// ----------------------- TaskExecutor ----------------
// TRANSLATE
	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("----------------------- TaskExecutor: Translate ----------------");
	}
	
	final TaskExecutorConfiguration teConfig=new TaskExecutorConfiguration("gdal_translate_id","techCDR_translate","gdal_translate");
	
	teConfig.setDefaultScript(defaultScriptName);
	teConfig.setErrorFile(errorFile.getAbsolutePath());
	teConfig.setExecutable(translateExecutable);
	teConfig.setFailIgnored(false);
//        teConfig.setOutput(getOutput());
//        teConfig.setOutputName(getOutputName());
//        teConfig.setServiceID(getServiceID());
	teConfig.setTimeOut(120000);
//        teConfig.setVariables(getVariables());
	
	teConfig.setXsl(translateXslName);

	TaskExecutor tea=new TaskExecutor(teConfig);
	tea.setTempDir(temporaryDir);
	tea.setConfigDir(configDir);
	
	queue=tea.execute(queue);

//  TODO CHECKS
	if (!outputFile.exists()){
	    return null;
	}
	return outputFile;
}



File gdaladdo(File configDir, File rasterFile, File errorFile, File temporaryDir, ProgressListenerForwarder listenerForwarder) throws Exception {
	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ScriptingAction.class");
// FREEMARKER -> GDALOVERVIEW
// ----------------------- FreeMarker ----------------
	listenerForwarder.setTask("FreeMarker - overview");
	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("-------------------- FreeMarker - overview ----------------");
	}

	Queue queue=new LinkedList();

	// relative to the configDir
	fmc=new FreeMarkerConfiguration("techCDR_freemarker","techCDR_freemarker","techCDR_freemarker");

	// params to inject into the ROOT datadir for FreeMarker
	final Map<String,String> fmRoot=new HashMap<String,String>();
	fmRoot.put("DESTDIR",rasterFile.getParent());
	fmRoot.put("FILENAME",rasterFile.getName());
	fmc.setRoot(fmRoot);

	// relative to the configDir
	fmc.setInput(overviewTemplateName);
	fmc.setOutput(temporaryDir.getAbsolutePath());
	
	// SIMULATE THE EventObject on the queue 
	FileSystemEvent imageEventFile=new FileSystemEvent(rasterFile,FileSystemEventType.FILE_ADDED);

	// SIMULATE THE EventObject on the queue 
	queue.add(imageEventFile);

	fma=new FreeMarkerAction(fmc);
	fma.setTempDir(temporaryDir);
	// SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
	fma.setConfigDir(configDir);

	queue=fma.execute(queue);

	if (queue.size()>0){
// IT'S ALL OK
		// leaving the image on the queue to pass it to the TaskExecutor
	}
	else {
		String message="::techCDR:: The output event queue from freemarker does not contains events...";
		ActionException e=new ActionException(FreeMarkerAction.class, message);
		listenerForwarder.failed(e);
		throw e;
	}

// ----------------------- TaskExecutor ----------------
// OVERVIEW
	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("--------------------- TaskExecutor - overview ----------------");
	}

	final TaskExecutorConfiguration teConfig=new TaskExecutorConfiguration("gdal_translate_id","techCDR_translate","gdal_translate");
	
	teConfig.setDefaultScript(defaultScriptName);
	teConfig.setErrorFile(errorFile.getAbsolutePath());
	teConfig.setExecutable(overviewExecutable);
	teConfig.setFailIgnored(false);
//        teConfig.setOutput(getOutput());
//        teConfig.setOutputName(getOutputName());
//        teConfig.setServiceID(getServiceID());
	teConfig.setTimeOut(120000);
//        teConfig.setVariables(getVariables());
	teConfig.setXsl(overviewXslName);

	tea=new TaskExecutor(teConfig);
	tea.setTempDir(temporaryDir);
	tea.setConfigDir(configDir);

	queue=tea.execute(queue);

	if (LOGGER.isInfoEnabled()) {
		LOGGER.info("::techCDR:: Overview operation is complete");
	}

	if (!new EmptyFileFilter().accept(errorFile)){
		// error file is NOT empty
		return null
	}
	
	return rasterFile;
}


void append(File errorFile, String message){
	// append message to errorFile
	try {
	    FileUtils.writeStringToFile(errorFile, message,true);
	} catch (IOException e1) {
	    if (LOGGER.isErrorEnabled()) {
		LOGGER.error(e1.getLocalizedMessage());
	    }
	}
}