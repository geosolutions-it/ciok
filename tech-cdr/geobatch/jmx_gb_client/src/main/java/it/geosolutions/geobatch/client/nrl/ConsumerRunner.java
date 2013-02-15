package it.geosolutions.geobatch.client.nrl;

import it.geosolutions.geobatch.services.jmx.ConsumerManager;
import it.geosolutions.geobatch.services.jmx.JMXAsynchConsumer;
import it.geosolutions.geobatch.services.jmx.JMXClientUtils;
import it.geosolutions.geobatch.services.jmx.JMXTaskRunner;
import it.geosolutions.tools.commons.generics.IntegerCaster;
import it.geosolutions.tools.commons.generics.SetComparator;
import it.geosolutions.tools.commons.reader.CSVReader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.RejectedExecutionException;

public class ConsumerRunner extends JMXTaskRunner<ConsumerManager> {

    private long delay;
    private final Set<Object[]> data;
    private final Map<String,String> commonEnv;

    public final static String CSV_FILE_KEY="CSV";
    
    /**
     * @param polling delay
     * @param connectionParams connection parameters
     * @throws Exception 
     */
    public ConsumerRunner(final Map<String,String> commonEnv) throws Exception {
        super();
        this.commonEnv=commonEnv;
        this.delay=JMXClientUtils.parsePollingDelay(commonEnv);
        final String csvFileName=commonEnv.get(CSV_FILE_KEY);
        if (csvFileName==null)
            throw new IllegalArgumentException("Unable to locate the "+CSV_FILE_KEY+" matching the CSV file name path");
        
        final File csv=new File(csvFileName); 
        
        data=CSVReader.readCsv(LOGGER, csv, ",", new SetComparator<Integer>(new IntegerCaster(), StatusMapper.keyIndex), false, false);
    }
    
    /**
     * using the passed environment builds a list of env to work on
     * 
     * @param service
     * @param input
     * @return
     * @throws Exception 
     * @throws IOException
     */
    @Override
    public int runTasks(CompletionService<ConsumerManager> cs) throws Exception {

        final Iterator<Object[]> it = data.iterator();
        int size = 0; // number of submitted tasks
        while (it.hasNext()) {

            // change config using naming convention (from the file name)
            final List<Map<String, String>> consumerConfiguration = StatusMapper.configureFlow(
                    LOGGER, it.next(), commonEnv);
            if (consumerConfiguration==null){
                if (LOGGER.isDebugEnabled())
                    LOGGER.error("Unable to parse the configuration");
                continue;
            }
            try {
                // submit the job
                cs.submit(new JMXAsynchConsumer(jmxConnector, serviceManager,
                        consumerConfiguration, delay));

                // work queue size
                ++size;

            } catch (RejectedExecutionException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.error(e.getLocalizedMessage(), e);
                else
                    LOGGER.error(e.getLocalizedMessage());
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.error(e.getLocalizedMessage(), e);
                else
                    LOGGER.error(e.getLocalizedMessage());
            }
        }
        return size;
        
    }

    public void run(final List<ConsumerManager> retSuccess,
            final List<ConsumerManager> retFail) throws Exception{
        
        run(this,commonEnv,retSuccess,retFail);
        
        // TODO WRITE INTO DATA
    }

}
