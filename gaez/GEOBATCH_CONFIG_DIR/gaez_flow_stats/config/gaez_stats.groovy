package it.geosolutions.geobatch.action.scripting;

import it.geosolutions.geobatch.catalog.file.FileBasedCatalogImpl;
import it.geosolutions.geobatch.configuration.CatalogConfiguration;
import it.geosolutions.geobatch.global.CatalogHolder;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;

import com.sun.org.apache.xpath.internal.FoundIndex;

import it.geosolutions.geobatch.geotiff.retile.*;
import it.geosolutions.geobatch.geotiff.overview.*;
import it.geosolutions.geobatch.imagemosaic.*;

import it.geosolutions.geobatch.action.scripting.ScriptingConfiguration;
import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.FileUtils;

import it.geosolutions.tools.io.file.Collector;
import it.geosolutions.tools.io.file.Copy;
import it.geosolutions.tools.commons.file.Path;
import it.geosolutions.tools.compress.file.Extract;

import java.io.File;
import java.util.Queue;

import com.thoughtworks.xstream.XStream;

// gt time parsing
import it.geosolutions.tools.commons.time.TimeParser;
import java.text.SimpleDateFormat;
import java.util.Date;

// FreeMarker
import it.geosolutions.geobatch.actions.freemarker.*;
// TaskExecutor
import it.geosolutions.geobatch.task.*;
import it.geosolutions.geobatch.task.TaskExecutorConfiguration;

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

// utils
import it.geosolutions.geobatch.gaez.utils.*;
import it.geosolutions.geobatch.gaez.utils.AggregatingRules;
import it.geosolutions.geobatch.gaez.utils.AreaRationalizer;
import it.geosolutions.geobatch.gaez.utils.GdalInfoExecutor;
import it.geosolutions.geobatch.gaez.utils.rules.FileType;

import it.geosolutions.geobatch.gaez.utils.rules.*;
import it.geosolutions.geobatch.gaez.utils.rules.ruleA.RuleA;
import it.geosolutions.geobatch.gaez.utils.rules.ruleB.RuleB;
import it.geosolutions.geobatch.gaez.utils.rules.ruleD.RuleD;


// LOG
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public Map execute(Map argsMap) throws Exception {
	
	final ScriptingConfiguration configuration=argsMap.get(ScriptingAction.CONFIG_KEY);
	final String runningContext=argsMap.get(ScriptingAction.CONTEXT_KEY);
	final File runningContextDir=new File(runningContext);
//	final List events=argsMap.get(ScriptingAction.EVENTS_KEY);
	final ProgressListenerForwarder listenerForwarder=argsMap.get(ScriptingAction.LISTENER_KEY);

	final Logger LOGGER = LoggerFactory.getLogger("it.geosolutions.geobatch.action.scripting.ActionScripting.class");

	listenerForwarder.started();

	// working dir
	final FileBasedCatalogImpl catalog=(FileBasedCatalogImpl) CatalogHolder.getCatalog();
	final File workingDir=Path.findLocation(configuration.getWorkingDirectory(),catalog.getBaseDirectory());
	final String workingDirName = workingDir.getAbsolutePath();

	final File outputFinalDataDir=new File(outputFinalDataDirName);
	if (!outputFinalDataDir.exists() || !outputFinalDataDir.canWrite()){
		String message="problem the final output data dir "+outputFinalDataDirName+" do not exists or is not writeable";
		Exception e=new Exception(message);
		listenerForwarder.failed(e);
		throw e;
	}

	final File inputDataDir=new File(inputDataDirName);
	if (!inputDataDir.exists() || !inputDataDir.canRead()){
		String message="problem the input data dir "+inputDataDirName+" do not exists or is not readable";
		Exception e=new Exception(message);
		listenerForwarder.failed(e);
		throw e;
	}

	if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("workingDir FILE : "  + workingDirName);
		LOGGER.debug("runningContext: " + runningContext);
		LOGGER.debug("InputDataDir FILE: " + inputDataDirName);
		LOGGER.debug("OutputFinalDataDir FILE: " + outputFinalDataDirName);
	}
	// READ FROM DB:
	// PARAMS
	final Properties dataStoreProp=new Properties();
	FileInputStream fis=null;

	List listOfMap=null;

	// TODO move into config
	int MAX_LAYERS=4;
	int MAX_GAUL_LEVEL=2;

	File[][] areasFiles=new File[MAX_GAUL_LEVEL][MAX_LAYERS];
	File[][] gaulsFiles=new File[MAX_GAUL_LEVEL][MAX_LAYERS];

	//for the computation on the rule D is nedeed the area_ratio
	final File[][] areasRatioFiles= new File[MAX_GAUL_LEVEL][MAX_LAYERS];

	try {

		// Loading gaul and area files first

		String keyArea="";
		String keyGauls="";	
		String keyAreaRatio="";
		Map props=configuration.getProperties();
		int iLevel=0;
		while(iLevel<MAX_GAUL_LEVEL)
		{
			
			String[] areas=props.get(keyArea.format("AreaClassFile_%d",iLevel)).split(";");
			String[] gauls=props.get(keyGauls.format("GAULFile_%d",iLevel)).split(";");
			// rule D ratio 
			String[] areasRatio=props.get(keyAreaRatio.format("AreaRatioFile_%d",iLevel)).split(";");

			if (areas.length>MAX_LAYERS|| gauls.length>MAX_LAYERS || areasRatio.length>MAX_LAYERS)
			{
				String message="problem the Layers specified are more then MAX_LAYER const.Please check the script";
				Exception e=new Exception(message);
				if (LOGGER.isErrorEnabled()){
					LOGGER.error(e.getLocalizedMessage(),e);
				}
				listenerForwarder.failed(e);
				throw e;
			}

			int iLayers=0;
			while(iLayers < areas.length)
			{
				areasFiles[iLevel][iLayers]=new File(workingDirName,areas[iLayers]);
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("areaFiles : "+iLevel+": "+areasFiles[iLevel][iLayers].getAbsolutePath());
				}
				iLayers++;
			}
			iLayers=0;
			while(iLayers < gauls.length)
			{
				gaulsFiles[iLevel][iLayers]=new File(workingDirName,gauls[iLayers]);
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("gaul files : "+iLevel+": "+gaulsFiles[iLevel][iLayers].getAbsolutePath());
				}
				iLayers++;
			}
			iLayers=0;
			while(iLayers < areasRatio.length)
			{
				areasRatioFiles[iLevel][iLayers]=new File(workingDirName,areasRatio[iLayers]);
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("areasRatioFiles : "+iLevel+": "+areasRatioFiles[iLevel][iLayers].getAbsolutePath());
				}
				iLayers++;
			}
			iLevel++;	
		}

		fis=new FileInputStream(new File(workingDir,dataStorePropFile));
		dataStoreProp.load(fis);
					// where clause, 					set clause
		listOfMap=DataStoreHandler.select4UpdatePrepStat(dataStoreProp,limitSize,"status_code='TOF' and ignore=1","status_code='TLK'");
		if (LOGGER.isDebugEnabled()){
				LOGGER.debug("Now the list of map about the gaezFlowStats is updated with: (dataStoreProp, limitSize, where status_code='TOF', set status_code='TLK') ");
			}

		if (listOfMap!=null){
			
			if (LOGGER.isDebugEnabled()){
				LOGGER.debug("List of map size: "+listOfMap.size());
			}

			if (listOfMap.size()==0){
				// ////
				// forwarding event to the next action
				// dummy results
				if (LOGGER.isInfoEnabled()){
					LOGGER.info("No data to process, exit.");
				}
				return null;
			}
		}
		else {	
			final NullPointerException e=new NullPointerException("Unable to get results from the database");
			if (LOGGER.isErrorEnabled()){
				LOGGER.error(e.getLocalizedMessage(),e);
			}
			listenerForwarder.failed(e);
			throw e;
		}
	}catch (FileNotFoundException e) {
		if (LOGGER.isErrorEnabled()){
			LOGGER.error(e.getLocalizedMessage(),e);
		}
		listenerForwarder.failed(e);
		throw e;
	}catch (Throwable e) {
		if (LOGGER.isErrorEnabled()){
			LOGGER.error(e.getLocalizedMessage(),e);
		}
		listenerForwarder.failed(e);
		throw e;
	}
	finally {
		
		IOUtils.closeQuietly(fis);
	}

	final ExecutorService es = Executors.newFixedThreadPool(nCopyThreads);
	AggregatingRules aggrRules=null;

	try {
	
		// MOVE FILES LOCALLY
		File imageFile;
		File imagePath;
		// base name never change so store now (to rename rdc files)
		String imageBaseName;

		final List<FutureTask<File>> futureImageFileTaskList=new ArrayList();
		final List<FutureTask<File>> futureXmlFileTaskList=new ArrayList();
		
		final List<File> imageFileList=new ArrayList();
		// for each element of the listOfMap list (limitSize)
		int nImageSize=listOfMap.size();
		// contains index of successfully copied images
		final List imageIndex=new ArrayList(nImageSize);
		final int listIndex=-1;
		for (Map map : listOfMap){
			//index of the i'th image into the map element
			++listIndex;

			// check db input and start file copy
			String imagePathName=map.get("file_path_tif");
			if (imagePathName==null){
				String message= "Inconsistent image file path found into DB!";
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug(message);
				}
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
				continue;
			}
			imagePath=new File(inputDataDir,imagePathName);

			String imageFileName=map.get("file_name_tif")
			if (imageFileName==null){
				String message= "Inconsistent image file name found into DB!";
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug(message);
				}
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
				continue;
			}
			imageFile=new File(imagePath, imageFileName);
			imageBaseName=FilenameUtils.getBaseName(imageFileName);

			File xmlFile = new File(imagePath, imageFileName+".aux.xml");

			//copy xml file if exists
			if (xmlFile.exists())
			{
				final FutureTask<File> futureXmlFileTask = Copy.asynchFileCopyToNFS(es,xmlFile, new File(runningContextDir,imageFile.getName()+".aux.xml"), 180);
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("copy xmlFile : "+xmlFile.getAbsolutePath());
				}
				futureXmlFileTaskList.add(futureXmlFileTask);

			}
			// and copy tif too
			final FutureTask<File> futureImageFileTask = Copy.asynchFileCopyToNFS(es,imageFile, new File(runningContextDir,imageFile.getName()), 180);
			if (LOGGER.isDebugEnabled()){
				LOGGER.debug("copy imageFile : "+imageFile.getAbsolutePath());
			}
			futureImageFileTaskList.add(futureImageFileTask);

			// if here copy is succesfully started (no errors into DB)
			imageIndex.add(listIndex);
		}

		if (futureImageFileTaskList==null){
			final String message="problem copying file images";
			for (Map map : listOfMap){
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
			}
			//continue;
			final Exception e=new Exception(message);
			listenerForwarder.failed(e);
			if (LOGGER.isErrorEnabled()){
				LOGGER.error(e.getLocalizedMessage(),e);
			}
			throw e;
		}
		
		// list of future moves
		final List<AsynchMove> futureMoveList=new ArrayList();

		// method called for area and gauls added 18/11/2011
		aggrRules = new AggregatingRules(gaulsFiles, areasFiles);

		nodataAreas=aggrRules.getNoDataAreas();
		nodataGauls=aggrRules.getNoDataGauls();

		
		int nImage=-1;
		int nXml=-1;
		nImageSize=imageIndex.size();
		while (++nImage<nImageSize){
			final Map map = listOfMap.get(imageIndex.get(nImage));
			try {
				/* ----------------------------------------------------------------- */
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("This is the image number -----> : "+ imageIndex.get(nImage));
				}
				final FutureTask<File> future=futureImageFileTaskList.get(nImage);
				

				// wait for the i-th copy image file task
				imageFile=null;
				imageFile=future.get();

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("COPYIED imageFile: "+ imageFile);
				}
				// CHECKS for file
				if (imageFile==null){
					final String message="problem the file number:"+nImage+"does not exists";
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","SKO"); // throw e;
					continue;
				}
				if (!imageFile.exists()){
					final String message="problem the file "+imageFile.getAbsolutePath()+" does not exists";
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					map.put("status_msg",message);
					map.put("status_code","SKO"); // throw e;
					continue;
				}

				if(++nXml<futureXmlFileTaskList.size()){
					// wait for the i-th copy xml file task
					final FutureTask<File> futureXmlFileTask = futureXmlFileTaskList.get(nXml);
					xmlFile=futureXmlFileTask.get();
					// CHECKS for xml file [optional]
					if (xmlFile==null){
						final String message="problem the XML file number:"+nXml+" associated to the image "+imageFile.getAbsolutePath()+"does not exists";
						if (LOGGER.isErrorEnabled()){
							LOGGER.error(message);
						}
						map.put("status_msg",message);
						map.put("status_code","SKO"); // throw e;
						continue;
					}
				}
				

				final File gdalInfoTemplate=new File(workingDirName,template);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("GdalInfoTemplate FILE: "+ gdalInfoTemplate.getAbsolutePath());
				}

				//added several	FileType -------------> modified by Emanuele Notarnicola emanuele.notarnicola@geo-solutions.it

				final String fileType = map.get("file_type");
				final FileType type=FileType.toFileType(fileType);
				if (type==FileType.NOVALUE)
				{
					final String message="problem fileType do not exist";
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					map.put("status_msg", message)
					map.put("status_code", "SKO") 
					continue;
				}

				Map mapOut = null;
				final String file_rule = map.get ("file_rule");
				final Double noDataImage = AggregatingRules.getNoData(imageFile);

				AggregatingRules aggregatorRules = null;
  //aggregatorRules = new AggregatingRules(gaulsFiles, areasRationalized);
				final Double multiply = map.get("scalar");

				if (file_rule == "A"){
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(" *** Parameter accepted from this RuleA *** ");
						LOGGER.debug("type value 		------> " + type);
						LOGGER.debug("gdalInfoTemplate value  	------> " + gdalInfoTemplate);
						LOGGER.debug("runningContextDir 	------> " + runningContextDir);
						LOGGER.debug("areasFiles value 		------> " + areasFiles );
						LOGGER.debug("imageFile value 		------> " + imageFile);
// 						LOGGER.debug("noDataImageAreas value 	------> " + nodataAreas);
						LOGGER.debug("noDataImage value 	------> " + noDataImage);
						LOGGER.debug("gaulsFiles value 		------> " + gaulsFiles);
						LOGGER.debug("nodataGauls value 	------> " + nodataGauls);
					}
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("A possible file_type parameter accepted for this rule are: F(discrete), P4(percentage), C(continuous)  ");
					}
					
					mapOut = RuleA.ruleA(type,gdalInfoTemplate,runningContextDir,imageFile,areasFiles,gaulsFiles,noDataImage,nodataAreas,nodataGauls);
				
				} else if (file_rule == "B"){
					
					
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(" *** Parameter accepted from this RuleB *** ");
						LOGGER.debug(" type value        		------> " + type );
						LOGGER.debug(" multiply value    		------> " + multiply );
						LOGGER.debug(" runningContextDir 		------> " + runningContextDir );
						LOGGER.debug(" aggrRules value   		------> " + aggrRules );
						LOGGER.debug(" imageFile value   		------> " + imageFile );
						LOGGER.debug(" noDataImage value	  		------> " + noDataImage.doubleValue() );
						LOGGER.debug(" is ruleC	  			------> " + "false" );
						LOGGER.debug(" type value        		------> " + type.getClass() );
						LOGGER.debug(" multiply value    		------> " + multiply.getClass() );
						LOGGER.debug(" runningContextDir 		------> " + runningContextDir.getClass() );
						LOGGER.debug(" aggrRules value   		------> " + aggrRules.getClass());
						LOGGER.debug(" noDataImage value	  		------> " + noDataImage.getClass() );
						LOGGER.debug(" is ruleC ?	  		------> " + "false" );
						LOGGER.debug(" imageFile value   		------> " + imageFile.getClass());
						}
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("A possible file_type parameter accepted for this rule are: P4(percentage) , C(continuous int), C4(continuous int/float)  ");
					}
					final double[] nodatArray=new double[1];
					nodatArray[0]=noDataImage.doubleValue();
					mapOut = RuleB.ruleBC(type, false, multiply, runningContextDir,aggrRules, imageFile, nodatArray);


				} else if (file_rule == "C"){
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("*** Parameter accepted from this RuleC ***");
						LOGGER.debug("type value 		------> " + type);
						LOGGER.debug("multiply value 		------> " + multiply);
						LOGGER.debug("runningContextDir 	------> " + runningContextDir);
						LOGGER.debug("aggrRules value 		------> " + aggrRules);
						LOGGER.debug("imageFile value 		------> " + imageFile);
						LOGGER.debug("noDataImage 			------> " + noDataImage);
						LOGGER.debug("is ruleC	  		------> " + "true");
					}
					if (LOGGER.isInfoEnabled()) {
					LOGGER.info("A possible file_type parameter accepted for this rule are: C(continuous int), C4 (continuous int/float)  ");
					}
					
					final double[] nodatArray=new double[2];
					nodatArray[0]=noDataImage.doubleValue();
					nodatArray[1]=0;
					mapOut = RuleB.ruleBC(type, true, multiply, runningContextDir,aggrRules, imageFile, nodatArray);

				} else if (file_rule == "D") {

					if (LOGGER.isDebugEnabled()){
						LOGGER.debug("*** Parameter accepted from this RuleD ***");
						LOGGER.debug("aggregatorRules value 			------> " + aggrRules);
						LOGGER.debug("multiply value 			------> " + multiply);
						LOGGER.debug("runningContextDir 		------> " + runningContextDir);
						LOGGER.debug("noDataImage value 		    ------> " + noDataImage);
						LOGGER.debug("imageFile value 		------> " + imageFile);
					}
					// check if ratio files already exist
					if(!areasRatioFiles[0][0].exists()){
						AreaRationalizer.produce(areasRatioFiles, areasFiles,nodataAreas);
					}
					if(areasRatioFiles[0][0].exists()){
						aggregatorRules = new AggregatingRules(gaulsFiles, areasRatioFiles);
						if (aggregatorRules==null){
							final String message="problem aggregatorRules is null";	
							if (LOGGER.isErrorEnabled()){
								LOGGER.error(message);
							}
							map.put("status_msg",message);
							map.put("status_code","SKO"); 
							continue;
						}
					} else if (file_rule == null){
						final String message="ERROR: file_rule is null check this variable";
						if (LOGGER.isErrorEnabled()){
							LOGGER.error(message);
						}
						map.put("status_msg",message);
						map.put("status_code","SKO");
						continue;
					}

					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("A possible file_type parameter accepted for this rule are: C(continuous int), C4 (continuous int/float)  ");
					}
//AggregatingRules aggregatorRules = null;
//final double multiply = map.get("scalar");
//aggregatorRules = new AggregatingRules(gaulsFiles, areasRatioFiles);

					if (aggregatorRules==null){
						final String message="problem aggregatorRules is null";	
						map.put("status_msg",message);
						map.put("status_code","SKO"); // throw e;
						if (LOGGER.isErrorEnabled()){
							LOGGER.error(message);
						}
						continue;
					}
					mapOut = RuleD.ruleD(aggregatorRules, multiply, imageFile, noDataImage,runningContextDir);
				} else {
					final String message="problem rule "+file_rule+" method still not implemented";
					map.put("status_msg",message);
					map.put("status_code","SKO"); // throw e;
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					continue;
				}
				if (mapOut==null){
					final String message="problem mapOut is null";	
					map.put("status_msg",message);
					map.put("status_code","SKO"); // throw e;
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					continue;
				}

				final File csvOutputFinalDataDir=new File(outputFinalDataDirName,"CSV1"+File.separator+map.get("file_class"));
				final String csvOutputFinalDataDirName=csvOutputFinalDataDir.getAbsolutePath();

				csvOutputFinalDataDir.mkdirs();
				if (!csvOutputFinalDataDir.exists() || !csvOutputFinalDataDir.canWrite()){
					final String message="problem the final output data dir "+csvOutputFinalDataDirName+" do not exists or is not writeable";
					map.put("status_msg",message);
					map.put("status_code","SKO"); // throw e;
					if (LOGGER.isErrorEnabled()){
						LOGGER.error(message);
					}
					continue;
				}

// MOVE FILES TO NFS
				// once moved images we have to move csv
				AsynchMove moveCSV_L0 =new AsynchMove(map,mapOut.get("0_csv"), csvOutputFinalDataDir,es);
				futureMoveList.add(moveCSV_L0);
				moveCSV_L1 =new AsynchMove(map, mapOut.get("1_csv"), csvOutputFinalDataDir,es);
				futureMoveList.add(moveCSV_L1);

				// for discrete files only
				if (mapOut.get(RuleA.LEGEND_KEY)!=null)
				{
					// get the legend file also
					AsynchMove moveLegend = new AsynchMove(map, mapOut.get(RuleA.LEGEND_KEY), csvOutputFinalDataDir,es);
					futureMoveList.add(moveLegend);
				}

// MIN AND MAX + including no data
				final Map<String, Double> mapStat=GdalInfoExecutor.getMaxMinNoData(imageFile);
				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("----------DATA FROM DB-------------");
					LOGGER.debug("multiply 		------> " + multiply);
					LOGGER.debug("nodata  DB	------> " + map.get("no_data_value"));
					LOGGER.debug("min 		------> " + map.get("min_value"));
					LOGGER.debug("max 		------> " + map.get("max_value"));
				}

				map.put("no_data_value",mapStat.get(GdalInfoExecutor.NODATA_KEY));
				map.put("min_value",mapStat.get(GdalInfoExecutor.MIN_KEY*multiply));
				map.put("max_value",mapStat.get(GdalInfoExecutor.MAX_KEY*multiply));

				if (LOGGER.isDebugEnabled()){
					LOGGER.debug("---------- DATA FROM IMAGE -------------");
					LOGGER.debug("nodata  DB	------> " + map.get("no_data_value"));
					LOGGER.debug("min 		------> " + map.get("min_value"));
					LOGGER.debug("max 		------> " + map.get("max_value"));
					
				}

				// SET STATUS OK
			} catch (IOException ioe) {
				final String message="IOException " +ioe.getLocalizedMessage();
				if (LOGGER.isErrorEnabled()){
					LOGGER.error(message,ioe);
				}
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
				continue;

			} catch (Throwable t){
				final String message="Throwable " +t.getLocalizedMessage();
				if (LOGGER.isErrorEnabled()){
					LOGGER.error(message,t);
				}
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
				continue;
			}

		} //for

		final Iterator<AsynchMove> itmove=futureMoveList.iterator();
		while (itmove.hasNext())
		{
			final AsynchMove futureMove=itmove.next();
			final Map map= futureMove.getMap();
			if (futureMove.isMoved()==Boolean.TRUE)
			{
				if (LOGGER.isInfoEnabled()){
					LOGGER.info("Work completed -> "+futureMove.getDest());
				}
				map.put("status_code","SOK");
				map.put("status_msg",null);
			}
			else {
				final String message="problem in moving file: "+ futureMove.getSource() +" to destination: "+futureMove.getDest();
				if (LOGGER.isErrorEnabled()){
					LOGGER.error(message);
				}
				map.put("status_msg",message);
				map.put("status_code","SKO"); // throw e;
			}
		}

		listenerForwarder.progressing(100,"completed");
		listenerForwarder.completed();	
	} catch (Throwable t){
		final String message="problem " +t.getLocalizedMessage();
		if (LOGGER.isErrorEnabled()){
			LOGGER.error(message,t);
		}
		listenerForwarder.failed(t);
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

		// dispose images
		if (aggrRules!=null){
			try  {
				  aggrRules.dispose(false);
			}  catch (Exception e){
			  if (LOGGER.isErrorEnabled()){
				  LOGGER.error(e);
			  }  
			}  
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
