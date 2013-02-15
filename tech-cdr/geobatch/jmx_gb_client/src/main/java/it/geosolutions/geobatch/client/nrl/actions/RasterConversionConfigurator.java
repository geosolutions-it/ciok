package it.geosolutions.geobatch.client.nrl.actions;

import it.geosolutions.geobatch.client.nrl.StatusMapper;
import it.geosolutions.geobatch.services.jmx.ConsumerManager;

import java.util.HashMap;
import java.util.Map;

public class RasterConversionConfigurator {

    /**
     * we could use tech-cdr
     * 
     * @param data
     * @return
     */
    public static Map<String, String> configureRasterConversion(Object[] data) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(ConsumerManager.SERVICE_ID_KEY, "ScriptingService");

        map.put(ConsumerManager.INPUT_KEY,
                StatusMapper.getDataString(data, StatusMapper.fileIndex, false));
        // TODO
        // map.put("dataTransferMethod","EXTERNAL");
        //
        // map.put("layerDescription",StatusMapper.checkData(data,StatusMapper.titleIndex,true));
        //
        // map.put("defaultStyle", StatusMapper.checkData(data,StatusMapper.styleIndex,false));

        return map;
    }

}
