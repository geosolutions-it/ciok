package it.geosolutions.geobatch.action.scripting

import it.geosolutions.geobatch.action.scripting.ScriptingConfiguration;

import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;

import it.geosolutions.geobatch.flow.event.action.ActionException;

import it.geosolutions.geobatch.catalog.file.FileBasedCatalogImpl;
import it.geosolutions.geobatch.configuration.CatalogConfiguration;
import it.geosolutions.geobatch.global.CatalogHolder;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;

import java.io.File;
import java.util.Queue;

import com.sun.org.apache.xpath.internal.FoundIndex;
import com.thoughtworks.xstream.XStream;

import it.geosolutions.geobatch.geotiff.retile.*;
import it.geosolutions.geobatch.geotiff.overview.*;
import it.geosolutions.geobatch.imagemosaic.*;

// FreeMarker
import it.geosolutions.geobatch.actions.freemarker.*;

// TaskExecutor
import it.geosolutions.geobatch.task.*;
import it.geosolutions.geobatch.task.TaskExecutorConfiguration;

// utils
import it.geosolutions.geobatch.gaez.utils.*;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.filefilter.EmptyFileFilter;

import it.geosolutions.tools.commons.file.Path;
import it.geosolutions.tools.io.file.Collector;
import it.geosolutions.tools.io.file.Copy;
import it.geosolutions.tools.io.file.writer.*;
import it.geosolutions.tools.compress.file.Extract;

import java.util.Properties;

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


public Map execute(Map argsMap) throws Exception {

	final ScriptingConfiguration configuration=argsMap.get(ScriptingAction.CONFIG_KEY);
	
	final File tempDir=argsMap.get(ScriptingAction.TEMPDIR_KEY);
	final File configDir=argsMap.get(ScriptingAction.CONFIGDIR_KEY);
//	final List events=argsMap.get(ScriptingAction.EVENTS_KEY);
	final ProgressListenerForwarder listenerForwarder=argsMap.get(ScriptingAction.LISTENER_KEY);

	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ScriptingAction.class");	

        listenerForwarder.started();

	if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("OutputDataDir FILE: "+outputDataDirName);
		LOGGER.debug("OutputFinalDataDir FILE: "+outputFinalDataDirName);
		LOGGER.debug("InputDataDir FILE: "+inputDataDirName);
	}

// output directory
	final File outputDataDir=new File(outputDataDirName);
	outputDataDir.mkdirs();
	if (!outputDataDir.exists() || !outputDataDir.canWrite()){
	    String message="problem the output data dir "+outputDataDirName+" do not exists or is not writeable";
	    Exception e=new Exception(message);
	    listenerForwarder.failed(e);
	    throw e;
	}
// output directory
	final File outputFinalDataDir=new File(outputFinalDataDirName);
	outputFinalDataDir.mkdirs();
	if (!outputFinalDataDir.exists() || !outputFinalDataDir.canWrite()){
	    String message="problem the final output data dir "+outputFinalDataDirName+" do not exists or is not writeable";
	    Exception e=new Exception(message);
	    listenerForwarder.failed(e);
	    throw e;
	}
// input directory
	final File inputDataDir=new File(inputDataDirName);
	if (!inputDataDir.exists() || !inputDataDir.canRead()){
	    String message="problem the input data dir "+inputDataDirName+" do not exists or is not readable";
	    Exception e=new Exception(message);
	    listenerForwarder.failed(e);
	    throw e;
	}

// READ FROM DB:
	// PARAMS

	final File dataStorePropFile=new File(configDir,dataStorePropFileName);
	/*
	id serial NOT NULL,
	  gaez_id character varying(100) NOT NULL,
	  file_path_rst character varying(100) NOT NULL,
	  file_name_rst character varying(100) NOT NULL,
	  file_path_rdc character varying(100) DEFAULT NULL::character varying,
	  file_name_rdc character varying(100) DEFAULT NULL::character varying,
	  file_type character varying(1) DEFAULT NULL::character varying,
	  class_type character varying(64) NOT NULL,
	  status_code character varying(3) DEFAULT 'RDY'::character varying,
	  status_msg character varying(1024) DEFAULT NULL::character varying,
	  min_value double precision,
	  max_value double precision,
	  no_data_value double precision,
	*/
	final Properties dataStoreProp=new Properties();
	FileInputStream fis=null;
	List listOfMap=null;
	try {
		fis=new FileInputStream(dataStorePropFile);
		dataStoreProp.load(fis);
		listOfMap=DataStoreHandler.select4UpdatePrepStat(dataStoreProp,limitSize,"status_code='RDY' and ignore=1","status_code='LCK'");
		if (LOGGER.isDebugEnabled()){
				LOGGER.debug("Now the list of map about the gaezFlowTiff is updated with: (dataStoreProp, limitSize, where status_code='RDY', set status_code='LCK') ");
			}

		if (listOfMap==null){
			final String message="Unable to get results from the database";
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(message);
			}
			final NullPointerException e=new NullPointerException(message);
			listenerForwarder.failed(e);
			throw e;
		}
		if (LOGGER.isDebugEnabled()) {
		      LOGGER.debug("ListOfMap size: "+listOfMap.size());
		}
		if (listOfMap.size()==0){
		    // NO object to process exit with success

		    // forwarding event to the next action
		    // dummy results
		    return null;
		}
	} catch (FileNotFoundException e) {
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error("Problem reading "+e.getLocalizedMessage());
		}
		listenerForwarder.failed(e);
		throw e;
	} catch (IOException e) {
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error("Problem reading "+e.getLocalizedMessage());
		}
		listenerForwarder.failed(e);
		throw e;
	}
	finally {
		IOUtils.closeQuietly(fis);
	}
	
	final ExecutorService es = Executors.newFixedThreadPool(nExecutorThreads);
    try {

// MOVE FILES LOCALLY
		File imageFile;
		File imagePath;

// base name never change so store now (to rename rdc files)
		String imageBaseName;

	
		final List<FutureTask<File>> futureImageFileTaskList=new ArrayList();
		final List<FutureTask<File>> futureRdcFileTaskList=new ArrayList();
		
		for (Map map : listOfMap){
			
			imagePath=new File(inputDataDir,map.get("file_path_rst"));
			imageFile=new File(imagePath, map.get("file_name_rst"));
			imageBaseName=FilenameUtils.getBaseName(imageFile.getName());
			
			File rdcFile = new File(imagePath, imageBaseName+".rdc");
			if (!rdcFile.exists()){
				File rdcPath= new File(inputDataDir,map.get("file_path_rdc"));
				rdcFile = new File(rdcPath, map.get("file_name_rdc"));
				final String rdcFileName=rdcFile.getAbsolutePath();
			}
			// copy copy .rdc
			final FutureTask<File> futureRdcFileTask = Copy.asynchFileCopyToNFS(es,rdcFile, new File(tempDir,map.get("gaez_id")+".rdc"), 180);
			futureRdcFileTaskList.add(futureRdcFileTask);
			final FutureTask<File> futureImageFileTask = Copy.asynchFileCopyToNFS(es,imageFile, new File(tempDir,map.get("gaez_id")+".rst"), 180);
			futureImageFileTaskList.add(futureImageFileTask);
		}
		if (futureImageFileTaskList==null){
			final String message="fatal problem copying file images";
			for (Map map : listOfMap){
				map.put("status_msg",message);
				map.put("status_code","TKO"); // throw e;
			}
			final Exception e=new Exception(message);
			listenerForwarder.failed(e);
			throw e;
		}
	
// list of future moves
		final List<AsynchMove> futureMoveList=new ArrayList();

	// for each element of the listOfMap list (limitSize)
		final int nImageSize=listOfMap.size();
		int nImage=-1;
		while (++nImage<nImageSize){
			final Map map = listOfMap.get(nImage);
			try {
				final FutureTask<File> future=futureImageFileTaskList.get(nImage);
				// wait for the i-th copy image file task
				imageFile=future.get();
	// CHECKS for file
				if (imageFile==null){
					final String message="problem the RST file associated with GAEZ_ID: "+map.get("gaez_id")+" does not exists";
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				if (!imageFile.exists()){
					final String message="problem the file "+imageFile.getAbsolutePath()+" does not exists";
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				imageBaseName=FilenameUtils.getBaseName(imageFile.getName());
				final File imageParentFile=imageFile.getParentFile();
				final String imageFileName=imageFile.getAbsolutePath();
				final String imageExtension=FilenameUtils.getExtension(imageFileName);
	// once moved images has new parents
	// tiff file
				// wait for the i-th copy rdc file task
				final FutureTask<File> futureRdcFileTask = futureRdcFileTaskList.get(nImage);
				rdcFile=futureRdcFileTask.get();
				if (rdcFile==null){
					final String message="problem the RDC associated with GAEZ_ID: "+map.get("gaez_id")+" does not exists";
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				if (!rdcFile.exists()){
					final String message="problem the file "+rdcFile.getAbsolutePath()+" does not exists";
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}

	// PREPARE OUTPUT FOLDER

	// tiffOutputDataDir = outputDataDir / class_type

	//final File tiffOutputDataDir=new File(outputDataDirName,map.get("class_type")+File.separator+"TIFF");
				final File tiffOutputDataDir=new File(outputDataDirName,"tiff");
				final File tiffOutputFinalDataDir=new File(outputFinalDataDirName,"tiff"+File.separator+map.get("file_path_rst"));
				final String tiffOutputDataDirName=tiffOutputDataDir.getAbsolutePath();
				final String tiffOutputFinalDataDirName=tiffOutputFinalDataDir.getAbsolutePath();
				tiffOutputDataDir.mkdirs();
				if (!tiffOutputDataDir.exists() || !tiffOutputDataDir.canWrite()){
					final String message="problem the output data dir "+tiffOutputDataDirName+" does not exists or is not writeable";
					if (LOGGER.isErrorEnabled()) {
						  LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				tiffOutputFinalDataDir.mkdirs();
				if (!tiffOutputFinalDataDir.exists() || !tiffOutputFinalDataDir.canWrite()){
					final String message="problem the final output data dir "+tiffOutputFinalDataDirName+" does not exists or is not writeable";
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}


				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("tiffOutputDataDirName FILE: "+tiffOutputDataDirName);
					LOGGER.debug("tiffOutputFinalDataDirName FILE: "+tiffOutputFinalDataDirName);
				}
	
	// FREEMARKER -> GDALTRANSLATE
	// tilized file
				
				File tiledRasterFile=null;
				File tileErrorFile=File.createTempFile("tile_", imageFile.getName()+".err", tempDir);
				try {
					tiledRasterFile=gdal_translate(configDir,imageFile,null,tileErrorFile,tempDir,listenerForwarder);
				} catch (Exception e){
	// append message to errorFile
					ExceptionWriter.appendStack(tileErrorFile, e);
					
					if (LOGGER.isErrorEnabled()) {
						  LOGGER.error(e.getLocalizedMessage(),e);
					}
					map.put("status_msg",e.getLocalizedMessage());
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				if (tiledRasterFile==null){
					String message="No supported file found into: "+imageFile+" see: "+tileErrorFile;
					if (LOGGER.isErrorEnabled()) {
						 LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}

	// ADDING OVERVIEW AND CHECK RESULTS
				final File overviewRasterFile;
	// check and valorize overviewRasterFile
				File overviewErrorFile=File.createTempFile("overview_", imageFile.getName()+".err", tempDir);
				try {
					overviewRasterFile=gdaladdo(configDir,tiledRasterFile,overviewErrorFile,tempDir,listenerForwarder)
				} catch (Exception e){
	// append message to errorFile
					//append(errorFile,e.getLocalizedMessage());
					ExceptionWriter.appendStack(overviewErrorFile, e);
					if (LOGGER.isErrorEnabled()) {
						  LOGGER.error(e.getLocalizedMessage(),e);
					}
					map.put("status_msg",message);
					map.put("status_code","TKO"); // throw e;
					continue;
				}
				if (overviewRasterFile==null){
				    // append errors to errorfile
				    Writer.appendFile(overviewErrorFile, tileErrorFile);
				    String message="Incoming file named \'"+tiledRasterFile.getName()+"\' is not in a supported format. see: "+tileErrorFile;
				    if (LOGGER.isErrorEnabled()) {
					      LOGGER.error(message);
				    }
				    map.put("status_msg",message);
				    map.put("status_code","TKO"); // throw e;
				    continue;
				}

	// for discrete files only
				if (map.get("file_type").equals("F")){
	// get the aux file also
					final File auxFile=new File(overviewRasterFile.getParent(),FilenameUtils.getBaseName(imageFileName)+".tif.aux.xml");
					FileUtils.moveFile(new File(tiledRasterFile.getAbsolutePath()+".aux.xml"),auxFile);
					AsynchMove moveAux = new AsynchMove(map, auxFile, tiffOutputFinalDataDir,es);
					futureMoveList.add(moveAux);
				}

	// MOVE FILES TO Final dir
				FileUtils.deleteQuietly(imageFile)
				imageFile=new File(overviewRasterFile.getParent(),FilenameUtils.getBaseName(imageFileName)+".tif");
				FileUtils.moveFile(overviewRasterFile,imageFile);
	// if overwrite == true remove the existing file
				if (overwrite){
					FileUtils.deleteQuietly(new File(tiffOutputFinalDataDir,imageFile.getName()));
				}
				AsynchMove moveTiff =new AsynchMove(map, imageFile, tiffOutputFinalDataDir,es);
				map.put("file_name_tif", imageFile.getName());
				map.put("file_path_tif", map.get("file_path_rst"));
				futureMoveList.add(moveTiff);

	// SET STATUS OK
				
			} catch (Throwable t){
				final String message="problem " +t.getLocalizedMessage();
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(message);
				}
				map.put("status_msg",message);
				map.put("status_code","TKO"); // throw e;
				continue;
			}

		} //while rows
// check moves		
		final Iterator<AsynchMove> itmove=futureMoveList.iterator();
		while (itmove.hasNext()){
			final AsynchMove futureMove=itmove.next();
			final Map map= futureMove.getMap();
			if (futureMove.isMoved()==Boolean.TRUE){
				map.put("status_code","TOK");
				map.put("status_msg",null);
			}
			else {
				final String message="Error moving file: "+ futureMove.getSource() +" to destination: "+futureMove.getDest();
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(message);
				}
				map.put("status_msg",message);
				map.put("status_code","TKO"); // throw e;
			}
		}

		listenerForwarder.progressing(100,"completed");
		listenerForwarder.completed();

	} catch (Throwable t){
		final String message="problem " +t.getLocalizedMessage();
		if (LOGGER.isErrorEnabled()) {
		    LOGGER.error(message);
		}
		listenerForwarder.failed(t);
		throw t;
	} finally {

		// WRITE INTO DB all the modified properties found into the listOfMap (STATUS_FLAG, ...)
		try {
			  if (LOGGER.isDebugEnabled()){
				LOGGER.debug("-----------------------DEBUG----------------------------");
				for (Map mapToCheck : listOfMap){
					LOGGER.debug("-------------------- Map size: "+mapToCheck.size()+"-------------------");
					final Set rowSet = mapToCheck.keySet();
					final Iterator it = rowSet.iterator();
					while (it.hasNext()) {
						String rowKey = it.next();
						String rowVal = mapToCheck.get(rowKey);
						LOGGER.debug("Map key: "+rowKey+" Map value: "+rowVal);
					}
				}
				LOGGER.debug("-----------------------DEBUG----------------------------");
			  }
			  
			  // UPDATE DB
			  DataStoreHandler.jdbcUpdateStatus(dataStoreProp, listOfMap);

		} catch (Exception e){
			  final String message="DB UPDATE ERROR: " +e.getLocalizedMessage();
			  if (LOGGER.isErrorEnabled()){
				  LOGGER.error(message,e);
			  }
			  listenerForwarder.failed(e);
		}

		// close Executor Service
		if (es!=null){
			try {
			      es.shutdown();
			}  catch (Exception e){
			  if (LOGGER.isErrorEnabled()){
				  LOGGER.error(e);
			  }  
			}  
		}
	}
	
        // ////
        // forwarding event to the next action
	// dummy results
        return null;
}


File gdal_translate(File configDir, File rasterFile, File prjFile, File errorFile, File tempDir, ProgressListenerForwarder listenerForwarder) throws Exception {
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
	fmc.setOutput(tempDir.getAbsolutePath());

	// SIMULATE THE EventObject on the queue 
	FileSystemEvent imageEventFile=new FileSystemEvent(rasterFile,FileSystemEventType.FILE_ADDED);

	queue.add(imageEventFile);
// add prjFile
	if (prjFile!=null)
		queue.add(new FileSystemEvent(prjFile,FileSystemEventType.FILE_ADDED));

	FreeMarkerAction fma=new FreeMarkerAction(fmc);
	fma.setTempDir(tempDir);
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
	tea.setTempDir(tempDir);
	tea.setConfigDir(configDir);
	
	queue=tea.execute(queue);

//  TODO CHECKS
	if (!outputFile.exists()){
	    return null;
	}
	return outputFile;
}



File gdaladdo(File configDir, File rasterFile, File errorFile, File tempDir, ProgressListenerForwarder listenerForwarder) throws Exception {
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
	fmc.setOutput(tempDir.getAbsolutePath());
	
	// SIMULATE THE EventObject on the queue 
	FileSystemEvent imageEventFile=new FileSystemEvent(rasterFile,FileSystemEventType.FILE_ADDED);

	// SIMULATE THE EventObject on the queue 
	queue.add(imageEventFile);

	fma=new FreeMarkerAction(fmc);
	fma.setTempDir(tempDir);
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
	tea.setTempDir(tempDir);
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