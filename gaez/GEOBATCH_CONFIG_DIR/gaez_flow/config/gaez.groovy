package it.geosolutions.geobatch.action.scripting

import org.apache.commons.io.FileUtils;

import com.sun.org.apache.xpath.internal.FoundIndex;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.FileUtils;

import it.geosolutions.geobatch.action.scripting.ScriptingConfiguration;
import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;
import it.geosolutions.geobatch.tools.file.Extract;

import it.geosolutions.geobatch.tools.file.Collector;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import it.geosolutions.geobatch.catalog.file.FileBasedCatalogImpl;
import it.geosolutions.geobatch.configuration.CatalogConfiguration;
import it.geosolutions.geobatch.global.CatalogHolder;
import it.geosolutions.geobatch.tools.file.Path;

import java.io.File;
import java.util.Queue;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;

import it.geosolutions.geobatch.geotiff.retile.*;
import it.geosolutions.geobatch.geotiff.overview.*;
import it.geosolutions.geobatch.imagemosaic.*;

import com.thoughtworks.xstream.XStream;

// gt time parsing
import it.geosolutions.geobatch.tools.time.TimeParser;
import java.text.SimpleDateFormat;
import java.util.Date;

// FreeMarker
import it.geosolutions.geobatch.actions.freemarker.*;

// TaskExecutor
import it.geosolutions.geobatch.task.*;
import it.geosolutions.geobatch.task.TaskExecutorConfiguration;

// utils
import it.geosolutions.geobatch.gaez.utils.*;

// Classification
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsConfiguration;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import java.util.Properties;

// COPY FILES
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
//^^^manual^^^


    /**
     * Script Main "execute" function
     * @eventFileName
     **/
    public List execute(ScriptingConfiguration configuration, String eventFileName, ProgressListenerForwarder listenerForwarder) throws Exception {
//	DataStoreHandler.jdbcTest("/home/carlo/work/project/ciok/trunk/gaez/GEOBATCH_DATA_DIR/gaez_flow/config/jdbc.properties");

        listenerForwarder.started();

	final Map props = configuration.getProperties();

// working dir
	final FileBasedCatalogImpl catalog=(FileBasedCatalogImpl) CatalogHolder.getCatalog();
	final File workingDir=Path.findLocation(configuration.getWorkingDirectory(),catalog.getBaseDirectory());
        final String workingDirName = workingDir.getAbsolutePath();

// output directory
	final String outputDataDirName = props.get("OutputDataDir");
	final File outputDataDir=new File(outputDataDirName);
	outputDataDir.mkdirs();
	if (!outputDataDir.exists() || !outputDataDir.canWrite()){
	    String message="::GAEZ:: problem the output data dir "+outputDataDirName+" do not exists or is not writeable";
	    Exception e=new Exception(message);
	    listenerForwarder.failed(e);
	    throw e;
	}

System.out.println("OutputDataDir FILE: "+outputDataDirName);
System.out.println("workingdir FILE: "+workingDirName);

// START.GO file
	final FileSystemEvent event=eventList.get(0);
	final Long eventTime=event.getTimestamp();
    	final File eventFile=new File(workingDirName+File.separator+eventTime);
	if (!eventFile.mkdirs()){
	    String message="::GAEZ:: problem generating the working data dir "+eventFile+" do not exists or is not writeable";
	    Exception e=new Exception(message);
	    listenerForwarder.failed(e);
	    throw e;
	}
	final File eventParentFile=eventFile;
	//final File eventFile=event.getSource();
	//final File eventParentFile=eventFile.getParentFile();
System.out.println("event FILE: "+eventFileName);
System.out.println("event FILE generated: "+eventFile);

// READ FROM DB:
	// PARAMS
	final int limitSize=props.get("LimitSize");
	final File dataStorePropFile=new File(workingDirName,props.get("DataStorePropFile"));
	final Properties dataStoreProp=new Properties();
//	final String table=props.get("Table"); // TODO REMOVEME
//	final String store=props.get("Store"); // TODO REMOVEME
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

	FileInputStream fis=null;
	List listOfMap=null;
	try {
		fis=new FileInputStream(dataStorePropFile);
		dataStoreProp.load(fis);
//		listOfMap=DataStoreHandler.readSet(dataStoreProp,table,store,limitSize);
		listOfMap=DataStoreHandler.select4UpdatePrepStat(dataStoreProp,limitSize);

		if (listOfMap!=null){
System.out.println(listOfMap.toString());

			if (listOfMap.size()==0){
			    // ////
			    // forwarding event to the next action
			    // dummy results
			    final List results = new ArrayList();
			    return results;
			}
		}
		else {
			final NullPointerException e=new NullPointerException("Unable to get results from the database");
			listenerForwarder.failed(e);
			throw e;
		}
		
	}catch (FileNotFoundException e) {
e.printStackTrace();
	    listenerForwarder.failed(e);
	    throw e;
	}catch (Throwable e) {
e.printStackTrace();
	    listenerForwarder.failed(e);
	    throw e;
	}
	finally {
		if (fis!=null)
			IOUtils.closeQuietly(fis);
	}
	
	//final ThreadFactory threadFactory = Executors.defaultThreadFactory();
	//final ExecutorService es = Executors.newCachedThreadPool(threadFactory);
	final int nThreads=5;
	final ExecutorService es = Executors.newFixedThreadPool(nThreads);
    try {
// copy file started
	boolean copyFileStarted=false;

// MOVE FILES LOCALLY
	File imageFile;
// base name never change so store now (to rename rdc files)
	String imageBaseName;

	final List<FutureTask<File>> futureImageFileTaskList=null;
	final List<FutureTask<File>> futureRdcFileTaskList=new ArrayList();
	if (copyFileStarted==false){
		
		final List<File> imageFileList=new ArrayList();
		for (Map map : listOfMap){
			
			imageFile=new File(map.get("file_path_rst"), map.get("file_name_rst"));
			imageFileList.add(imageFile);
			imageBaseName=FilenameUtils.getBaseName(imageFile.getName());
			File rdcFile = new File(map.get("file_path_rst"), imageBaseName+".rdc");
			if (!rdcFile.exists()){
				rdcFile = new File(map.get("file_path_rdc"), map.get("file_name_rdc"));
				final String rdcFileName=rdcFile.getAbsolutePath();
				
//				if (!imageParentFile.equals(rdcFile.getParentFile())){
//		System.out.println("Copying the rdc file into the rst parent folder");
//				// copy copy .rdc
//				    final FutureTask<File> futureRdcFileTask = Path.asynchFileCopyToNFS(es,rdcFile, new File(eventFile.getParentFile(),imageBaseName+".rdc"), 180);
//				    futureRdcFileTaskList.add(futureRdcFileTask);
//				}
				
			}
			// copy copy .rdc
			final FutureTask<File> futureRdcFileTask = Path.asynchFileCopyToNFS(es,rdcFile, new File(eventParentFile,imageBaseName+".rdc"), 180);
			futureRdcFileTaskList.add(futureRdcFileTask);
		}
		futureImageFileTaskList=Path.asynchCopyListFileToNFS(es, imageFileList, eventParentFile, 180);
		if (futureImageFileTaskList==null){
			final String message="::GAEZ:: problem copying file images";
			for (Map map : listOfMap){
				map.put("status_msg",message);
				map.put("status_code","KO"); // throw e;
			}
			//continue;
			final Exception e=new Exception(message);
			listenerForwarder.failed(e);
			throw e;
		}
		copyFileStarted=true;
	}
// for each element of the listOfMap list (limitSize)
	final int nImageSize=listOfMap.size();
	int nImage=-1;
	while (++nImage<nImageSize){
		final Map map = listOfMap.get(nImage);
		try {
            /* ----------------------------------------------------------------- */

		if (copyFileStarted==true){
			final FutureTask<File> future=futureImageFileTaskList.get(nImage);
			// wait for the i-th copy image file task
			imageFile=future.get();
			// wait for the i-th copy rdc file task
			final FutureTask<File> futureRdcFileTask = futureRdcFileTaskList.get(nImage);
			rdcFile=futureRdcFileTask.get();
		}
		else {
			continue;
		}

// CHECKS for file

		if (imageFile!=null && !imageFile.exists()){
		    final String message="::GAEZ:: problem the file "+imageFile.getAbsolutePath()+" do not exists";
		    //Exception e=new Exception(message);
		    //listenerForwarder.failed(e);
		    map.put("status_msg",message);
		    map.put("status_code","KO"); // throw e;
		    continue;
		}

// once moved images has new parents
	// tiff file
		imageBaseName=FilenameUtils.getBaseName(imageFile.getName());
		final File imageParentFile=imageFile.getParentFile();
		final String imageFileName=imageFile.getAbsolutePath();
		final String imageExtension=FilenameUtils.getExtension(imageFileName);

		if (rdcFile!=null && !rdcFile.exists()){
		    final String message="::GAEZ:: problem the file "+rdcFile.getAbsolutePath()+" do not exists";
		    //Exception e=new Exception(message);
		    //listenerForwarder.failed(e);
		    map.put("status_msg",message);
		    map.put("status_code","KO"); // throw e;
		    continue;
		}

// PREPARE OUTPUT FOLDER
// tiffOutputDataDir = outputDataDir / class_type

		final File tiffOutputDataDir=new File(outputDataDirName,map.get("class_type")+File.separator+"TIFF");
		final String tiffOutputDataDirName=tiffOutputDataDir.getAbsolutePath();
		tiffOutputDataDir.mkdirs();
		if (!tiffOutputDataDir.exists() || !tiffOutputDataDir.canWrite()){
		    final String message="::GAEZ:: problem the output data dir "+tiffOutputDataDirName+" do not exists or is not writeable";
		    map.put("status_msg",message);
		    map.put("status_code","KO"); // throw e;
		    continue;
		}
System.out.println("tiffOutputDataDirName FILE: "+tiffOutputDataDirName);

		final File continuousOutputDataDir=new File(outputDataDirName,map.get("class_type")+File.separator+"CONTINUOUS");
		final String continuousOutputDataDirName=continuousOutputDataDir.getAbsolutePath();
		continuousOutputDataDir.mkdirs();
		if (!continuousOutputDataDir.exists() || !continuousOutputDataDir.canWrite()){
		    final String message="::GAEZ:: problem the output data dir "+continuousOutputDataDirName+" do not exists or is not writeable";
		    map.put("status_msg",message);
		    map.put("status_code","KO"); // throw e;
		    continue;
		}
System.out.println("continuousOutputDataDir FILE: "+continuousOutputDataDirName);

		final File discreteOutputDataDir=new File(outputDataDirName,map.get("class_type")+File.separator+"DISCRETE");
		final String discreteOutputDataDirName=discreteOutputDataDir.getAbsolutePath();
		discreteOutputDataDir.mkdirs();
		if (!discreteOutputDataDir.exists() || !discreteOutputDataDir.canWrite()){
		    final String message="::GAEZ:: problem the output data dir "+discreteOutputDataDirName+" do not exists or is not writeable";
		    map.put("status_msg",message);
		    map.put("status_code","KO"); // throw e;
		    continue;
		}
System.out.println("discreteOutputDataDir FILE: "+discreteOutputDataDirName);

// CHECK RDC to understand file type
		final Map<String,String> rdcMap=RdcReader.readRdc(rdcFile);
		final String cats=rdcMap.get("legend_cats");
		final int catsVal=Integer.parseInt(cats);
		final boolean discrete;
		if (catsVal>0){
		    discrete=true;
		    map.put("file_type","D");
		}
		else {
		    discrete=false;
		    map.put("file_type","C");
		}

// GDALINFO 2 CSV
		Element GdalInfoDocElement=null; // used into the class stats...
		if (discrete){
			
			final File template=new File(workingDir,props.get("Template"));
			final File csvOut=new File(discreteOutputDataDirName, map.get("gaez_id")+"_legend.csv");
			final Document GdalInfoDoc=GdalInfoExecutor.gdalinfoToDocument(imageFile);
			GdalInfoDocElement=GdalInfoDoc.getRootElement();
			final File gdalInfoResFile=GdalInfoExecutor.process(GdalInfoExecutor.convertToDOM(GdalInfoDoc), template, csvOut);
			
			
			if (gdalInfoResFile!=null){
//TODO LOG
System.out.println("OK");
			}
			else {
			    map.put("status_msg","Unable to get gdalinfoToCSV output");
			    map.put("status_code","KO"); // throw e;
System.out.println("NOT OK: Unable to get gdalinfoToCSV output");
			    continue;
			}
		}
// FREEMARKER -> GDALTRANSLATE

     
// search for needed files
		Queue queue=new LinkedList();

// ----------------------- FreeMarker ----------------
System.out.println("----------------------- FreeMarker ----------------");

		FreeMarkerConfiguration fmc=new FreeMarkerConfiguration(configuration.getId(),"GAEZ_freemarker",configuration.getDescription());
		// SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
		fmc.setWorkingDirectory(workingDirName);

	// relative to the working dir
		fmc.setInput(props.get("TranslateTemplateInput"));

	// params to inject into the ROOT datadir for FreeMarker
		final Map<String,String> fmRoot=new HashMap<String,String>();

	// inject statically (into configuration) free Marker vars...
// GEOTIFF IMAGE OUTPUT
		final File imageOutputFile=new File(tiffOutputDataDirName,map.get("gaez_id")+".tif");
		fmRoot.put("DESTDIR",tiffOutputDataDirName);
		fmRoot.put("FILENAME",imageOutputFile.getName());
		fmc.setRoot(fmRoot);

	// output data dir for xml task executor command (temporary files)
		fmc.setOutput(eventParentFile.getName());
		
		// SIMULATE THE EventObject on the queue 
	        FileSystemEvent imageEventFile=new FileSystemEvent(imageFile,FileSystemEventType.FILE_ADDED);
		queue.add(imageEventFile);
		
		FreeMarkerAction fma=new FreeMarkerAction(fmc);
		
		queue=fma.execute(queue);

// ----------------------- TaskExecutor ----------------
System.out.println("----------------------- TaskExecutor ----------------");

// TRANSLATE
		final TaskExecutorConfiguration teConfig=new TaskExecutorConfiguration(configuration.getId(),configuration.getName(),configuration.getDescription());
		
		teConfig.setDefaultScript(props.get("DefaultScript"));
		teConfig.setErrorFile(props.get("TranslateErrorFile"));
		teConfig.setExecutable(props.get("TranslateExecutable"));
		teConfig.setFailIgnored(false);
	//        teConfig.setOutput(getOutput());
	//        teConfig.setOutputName(getOutputName());
	//        teConfig.setServiceID(getServiceID());
		teConfig.setTimeOut(120000);
	//        teConfig.setVariables(getVariables());
		teConfig.setWorkingDirectory(workingDirName);
		teConfig.setXsl(props.get("TranslateXsl"));

		TaskExecutor tea=new TaskExecutor(teConfig);
		
		queue=tea.execute(queue);

// FREEMARKER -> GDALOVERVIEW

// ----------------------- FreeMarker ----------------
System.out.println("----------------------- FreeMarker ----------------");

		queue.clear();

	// relative to the working dir
		fmc=new FreeMarkerConfiguration(configuration.getId(),"GAEZ_freemarker",configuration.getDescription());
		// SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
		fmc.setWorkingDirectory(workingDirName);

	// relative to the working dir
		fmc.setInput(props.get("OverviewTemplateInput"));
		fmc.setRoot(fmRoot);
		fmc.setOutput(eventParentFile.getName());
		
		// SIMULATE THE EventObject on the queue 

		queue.add(imageEventFile);
		
		fma=new FreeMarkerAction(fmc);
		
		queue=fma.execute(queue);

// ----------------------- TaskExecutor ----------------
System.out.println("----------------------- TaskExecutor ----------------");

// OVERVIEW

		teConfig.setErrorFile(props.get("OverviewErrorFile"));
		teConfig.setExecutable(props.get("OverviewExecutable"));
		teConfig.setXsl(props.get("OverviewXsl"));

		tea=new TaskExecutor(teConfig);
		
		queue=tea.execute(queue);

		if (queue.size()>0){	
			queue.clear();
// IT'S ALL OK		// results
			//List results = new ArrayList();	
//			results.add(imageOutputFile.getAbsolutePath());

		}
		else {
			String message="::GAEZ:: ouptup queue events from freemarker do not contain events...";
			//Exception e=new Exception(message);
			//listenerForwarder.failed(e);
			map.put("status_msg",message);
			map.put("status_code","KO"); // throw e;
			continue;
		}


System.out.println("DONE: overview");

// END -------------------- FreeMarker -----------------------

//TODO what to do with percentage?

// GENERATE CLASSIFIED STATS COMMAND

		final File inputFile = File.createTempFile("clstats_in", ".xml",eventParentFile);
	//        FileUtils.forceDeleteOnExit(inputFile);
		
		final File areaRasterFile = new File(workingDirName, props.get("AreaClassFile"));
		final File gaulFile = new File(workingDirName, props.get("GAULFile"));

		final File outputFile;
		final File summaryOutputFile = File.createTempFile("clSummaryStats_in", ".properties",eventParentFile);

// CLASSIFIED STATS ACTION

		final ClassifiedStatsConfiguration actionCfg = new ClassifiedStatsConfiguration("csc", "csc", "csc");

		
		final Element reqRoot;
		if (discrete){
			// notice that '0' is the gaul level
			outputFile = new File(discreteOutputDataDirName,"I0_"+map.get("gaez_id")+".csv");
			reqRoot= new Element("classifiedStats")
				.addContent(new Element("data").setText(areaRasterFile.getAbsolutePath()))
				.addContent(new Element("classification").setText(gaulFile.getAbsolutePath()))
				.addContent(new Element("classification").setText(imageOutputFile.getAbsolutePath()));

			final Element output = new Element("output")
				.addContent(new Element("file").setText(outputFile.getAbsolutePath()))
				.addContent(new Element("summary").setText(summaryOutputFile.getAbsolutePath()))
				.addContent(new Element("gaez_id").setText(map.get("gaez_id")));
				// if it is a discrete file type categories node should never be null!
			if (GdalInfoDocElement!=null){
				/*
				<categories>
				  <class><value>VVV</value><descrtiprion>DDD</description></class>
				*/
				final Element categories=new Element("categories");
				for (Element row : (List)GdalInfoDocElement.getChildren("Row")) {
					final List<Element> fValues = row.getChildren("F");
					String code = fValues.get(0).getText();
					String desc = fValues.get(fValues.size()-1).getText();
					Element category = new Element("category");
					category.addContent(new Element("code").setText(code));
					category.addContent(new Element("description").setText(desc));
					categories.addContent(category);
				}
				output.addContent(categories);
			}
			else {
				final String message="ERROR ON STATS: here 'GdalInfoDocElement' may NOT be null";
System.out.println(message);
				map.put("status_msg",message);
				map.put("status_code","KO"); // throw e;
				continue;
			}
			reqRoot.addContent(output);
		}
		else { // continue file
// CHECK FOR PERCENTAGE
System.out.println("PERCENTAGE VALUE:"+map.get("percentage"));
			if (map.get("percentage").equals(true)){
				// notice that '0' is the gaul level
				outputFile = new File(continuousOutputDataDirName,"I0_"+map.get("gaez_id")+".csv");
				reqRoot= new Element("classifiedStats")
					.addContent(new Element("data").setText(imageOutputFile.getAbsolutePath()))
					// this will be merged to the source data and placed into the data element so
					// here is accepted a float raster
					.addContent(new Element("data_multiplier").setText(areaRasterFile.getAbsolutePath()))
					.addContent(new Element("classification").setText(gaulFile.getAbsolutePath()));
					

				final Element output = new Element("output")
					.addContent(new Element("file").setText(outputFile.getAbsolutePath()))
					.addContent(new Element("summary").setText(summaryOutputFile.getAbsolutePath()))
					.addContent(new Element("gaez_id").setText(map.get("gaez_id")));
				// if it is a discrete file type categories node should never be null!
				if (GdalInfoDocElement!=null){
					final String message="here 'GdalInfoDocElement' should be null";
System.out.println(message);
					map.put("status_msg",message);
					map.put("status_code","KO"); // throw e;
					continue;
				}
				else {
					/*
					<categories>
					  <class><value>VVV</value><descrtiprion>DDD</description></class>
					*/
					final Element categories=new Element("categories");
					final Element category = new Element("category");
					category.addContent(new Element("code").setText(map.get("gaez_id")));
					category.addContent(new Element("description").setText(rdcMap.get("file_title")));
					categories.addContent(category);
					output.addContent(categories);
				}
				reqRoot.addContent(output);
			}
			else {
				// notice that '0' is the gaul level
				outputFile = new File(continuousOutputDataDirName,"F0_"+map.get("gaez_id")+".csv");
				reqRoot= new Element("classifiedStats")
					.addContent(new Element("data").setText(imageOutputFile.getAbsolutePath()))
					.addContent(new Element("classification").setText(gaulFile.getAbsolutePath()));

				final Element output = new Element("output")
					.addContent(new Element("file").setText(outputFile.getAbsolutePath()))
					.addContent(new Element("summary").setText(summaryOutputFile.getAbsolutePath()))
					.addContent(new Element("gaez_id").setText(map.get("gaez_id")));
				reqRoot.addContent(output);
			}
		}
		final XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
		FileUtils.writeStringToFile(inputFile, outputter.outputString(reqRoot));
		queue.add(new FileSystemEvent(inputFile, FileSystemEventType.FILE_ADDED));
		final ClassifiedStatsAction action = new ClassifiedStatsAction(actionCfg);
		queue = action.execute(queue);

// GET 
		fis=null;
		final Properties summaryOutputProp=new Properties();
		try {
			fis=new FileInputStream(summaryOutputFile);
			summaryOutputProp.load(fis);
// TODO map.put("no_data_value",summaryOutputProp.get(""));
			map.put("min_value",Double.parseDouble(summaryOutputProp.get("min")));
			map.put("max_value",Double.parseDouble(summaryOutputProp.get("max")));
		}
		finally {
			if (fis!=null)
				IOUtils.closeQuietly(fis);
		}

//TODO        LOGGER.info("Output file is " + outputFile);
//TODO        LOGGER.info(IOUtils.toString(new FileReader(outputFile)));

// MOVE FILES TO NFS
// TODO??? (NEEDED)


// SET STATUS OK
		map.put("status_code","OK");
		map.put("status_msg",null);
//final Set<String> set=map.keySet();
//final Iterator<String> it=set.iterator();
//while (it.hasNext()){
//	String s=it.next();
//	System.out.println(s+" "+map.get(s));
//}

	    } catch (IOException ioe) {
		final String message="::GAEZ:: problem " +ioe.getLocalizedMessage();
		//Exception e=new Exception(message);
		//listenerForwarder.failed(e);
ioe.printStackTrace();
		map.put("status_msg",message);
		map.put("status_code","KO"); // throw e;
		continue;
		
	    } catch (Throwable t){
		final String message="::GAEZ:: problem " +t.getLocalizedMessage();
		//Exception e=new Exception(message);
		//listenerForwarder.failed(e);
t.printStackTrace();
		map.put("status_msg",message);
		map.put("status_code","KO"); // throw e;
		continue;
	    }

	} //for

    } catch (Throwable t){
	final String message="::GAEZ:: problem " +t.getLocalizedMessage();
t.printStackTrace();
//	map.put("status_msg",message);
//	map.put("status_code","KO"); // throw e;
	listenerForwarder.failed(t);
    } finally {
// WRITE INTO DB all the modified properties found into the listOfMap (STATUS_FLAG, ...)
	//DataStoreHandler.updateStatus(dataStoreProp, table, store, listOfMap);
	DataStoreHandler.jdbcUpdateStatus(dataStoreProp, listOfMap);

// close Executor Service
	if (es!=null){
		es.shutdownNow();
	}


    }
	
	listenerForwarder.progressing(100,"completed");
	listenerForwarder.completed();

        // ////
        // forwarding event to the next action
	// dummy results
	final List results = new ArrayList();
        return results;
    }
