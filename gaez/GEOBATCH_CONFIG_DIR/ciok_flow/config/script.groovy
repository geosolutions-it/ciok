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

// time parsing
import it.geosolutions.geobatch.tools.time.TimeParser;
import java.text.SimpleDateFormat;
import java.util.Date;

// FreeMarker
import it.geosolutions.geobatch.actions.freemarker.*;

// TaskExecutor
import it.geosolutions.geobatch.task.*;

//^^^manual^^^


    /**
     * Script Main "execute" function
     * @eventFileName
     **/
    public List execute(ScriptingConfiguration configuration, String eventFileName, ProgressListenerForwarder listenerForwarder) throws Exception {
	
        listenerForwarder.started();

        // ////////////////////////////////////////////////////////////////////
        // Initializing input variables from Flow configuration
        // ////////////////////////////////////////////////////////////////////
        final Map props = configuration.getProperties();
        
        // working dir
	FileBasedCatalogImpl catalog=(FileBasedCatalogImpl) CatalogHolder.getCatalog();
	File workingDir=Path.findLocation(configuration.getWorkingDirectory(),catalog.getBaseDirectory());
	//File workingDir=new File(eventFileName).getParentFile();

        String workingDirName = workingDir.getAbsolutePath();
        
	// results
	List results = new ArrayList();
	
        System.out.println("workingdir FILE: "+workingDirName);
        System.out.println("event FILE: "+eventFileName);

        // if the directory is complete move it
        try {
            /* ----------------------------------------------------------------- */
     
// search for needed files
	    Queue queue=new LinkedList();

// ----------------------- FreeMarker ----------------
System.out.println("----------------------- FreeMarker ----------------");

	FreeMarkerConfiguration fmc=new FreeMarkerConfiguration(configuration.getId(),"CIOK_freemarker",configuration.getDescription());
        // SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
        fmc.setWorkingDirectory(workingDirName);

// relative to the working dir
        fmc.setInput(props.get("TranslateTemplateInput"));
	
        fmc.setOutput("out");
        
        // SIMULATE THE EventObject on the queue 

        FileSystemEvent imageEvent=eventList.get(0);
	File imageFile=imageEvent.getSource();
	final String imageFileName=imageFile.getAbsolutePath();
	final String imageBaseName=FilenameUtils.getBaseName(imageFileName);
	final String imageExtension=FilenameUtils.getExtension(imageFileName);
	
	
        queue.add(imageEvent);
        
        FreeMarkerAction fma=new FreeMarkerAction(fmc);
        
        queue=fma.execute(eventList);

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


// ----------------------- FreeMarker ----------------
System.out.println("----------------------- FreeMarker ----------------");

	queue.clear();

// relative to the working dir
	fmc=new FreeMarkerConfiguration(configuration.getId(),"CIOK_freemarker",configuration.getDescription());
        // SIMULATE THE XML FILE CONFIGURATION OF THE ACTION
        fmc.setWorkingDirectory(workingDirName);

// relative to the working dir
        fmc.setInput(props.get("OverviewTemplateInput"));
        fmc.setOutput("out");
        
        // SIMULATE THE EventObject on the queue 

        queue.add(imageEvent);
        
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
// return
		queue.clear();
		File outFile=new File(imageFile.getParentFile(),imageBaseName+"_out."+imageExtension);
          	
		final String outputDataDirName = props.get("OutputDataDir");

System.out.println("OutputDataDir FILE: "+outputDataDirName);

		File outputDataDir=new File(outputDataDirName);
		if (!outputDataDir.exists() || !outputDataDir.canWrite()){
		    String message="::Briseide:: problem the output data dir "+outputDataDirName+" do not exists or is not writeable";
		    Exception e=new Exception(message);
		    listenerForwarder.failed(e);
		    throw e;
		}

		// copy to destination dir
		File destFile=Path.copyFileToNFS(outFile, new File(outputDataDir,imageBaseName+"."+imageExtension), 100);
		results.add(destFile.getAbsolutePath());
        }
	else {
		String message="::Briseide:: ouptup queue events from freemarker do not contain events...";
            	Exception e=new Exception(message);
            	listenerForwarder.failed(e);
            	throw e;
	}


System.out.println("DONE: FreeMarker");

// END -------------------- FreeMarker -----------------------


          listenerForwarder.progressing(100,"completed");
          listenerForwarder.completed();


        } catch (IOException ioe) {
            String message="::CIOK:: problem " +ioe.getLocalizedMessage();
            Exception e=new Exception(message);
            listenerForwarder.failed(e);
            throw e;
        }
        // ////
        // forwarding event to the next action
        return results;
    }
