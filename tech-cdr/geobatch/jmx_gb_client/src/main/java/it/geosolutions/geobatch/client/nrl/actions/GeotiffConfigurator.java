package it.geosolutions.geobatch.client.nrl.actions;

import it.geosolutions.geobatch.client.nrl.StatusMapper;
import it.geosolutions.geobatch.services.jmx.ConsumerManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GeotiffConfigurator {

    public final static String TIFF_BASE_DIR_KEY = "TIFF_BASE_DIR";

    /**
     * private String crs;
     * 
     * private String envelope; // set BB comma separated
     * 
     * private String storeFilePrefix; // store name
     * 
     * private String configId;
     * 
     * private String datatype; // ?
     * 
     * private String layerName;
     * 
     * private String title;
     * 
     * private Boolean queryable=true;
     * 
     * private String layerDescription;
     * 
     * private String layerAbstract;
     * 
     * private String storeName;
     * 
     * private String defaultNamespace;
     * 
     * private String defaultNamespaceUri;
     * 
     * private String defaultStyle;
     * 
     * @param data
     * @param commonEnv
     * @return
     */
    public static Map<String, String> configureGeoTiffPublish(Object[] data,
            final Map<String, String> commonEnv) {
        Map<String, String> map = new HashMap<String, String>();

        map.put(ConsumerManager.SERVICE_ID_KEY, "GeotiffGeoServerService");

        map.put("geoserverPWD", commonEnv.get("geoserverPWD"));
        map.put("geoserverUID", commonEnv.get("geoserverUID"));
        map.put("geoserverURL", commonEnv.get("geoserverURL"));

        String baseTiffPath = commonEnv.get(TIFF_BASE_DIR_KEY);
        String tiffFileName;
        if (baseTiffPath != null && !baseTiffPath.isEmpty())
            tiffFileName = baseTiffPath + File.separator
                    + StatusMapper.getDataString(data, StatusMapper.fileIndex, false);
        else
            tiffFileName = StatusMapper.getDataString(data, StatusMapper.fileIndex, false);

        map.put(ConsumerManager.INPUT_KEY, tiffFileName);

        map.put("dataTransferMethod", "EXTERNAL");

        map.put("layerAbstract", StatusMapper.getDataString(data, StatusMapper.titleIndex, true));

        map.put("defaultStyle", StatusMapper.getDataString(data, StatusMapper.styleIndex, false));

        map.put("crs", commonEnv.get("crs"));
        map.put("defaultNamespace", commonEnv.get("defaultNamespace"));
        map.put("defaultNamespaceUri", commonEnv.get("defaultNamespaceUri"));

        return map;
    }

}
