package it.geosolutions.geobatch.client.nrl;

import it.geosolutions.geobatch.client.nrl.actions.GeotiffConfigurator;
import it.geosolutions.geobatch.services.jmx.ConsumerManager;
import it.geosolutions.tools.commons.generics.IntegerCaster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * build the flows using actions configurators starting from the passed data, this is highly dependent from the used CSV
 * 
 * @author cancellieri
 * 
 */
public class StatusMapper {

    // 0 1 2 3 4 5 6
    // ,2,23,gn_23_airtmp_mn.tif,gn_23_airtmp_mn,geonetwork_23_airtmp_mn_style,Mean monthly air temperature of Africa for the month of January
    public final static String keyIndex_KEY = "keyIndex";

    public static int keyIndex = 2;

    public final static String statusIndex_KEY = "statusIndex";

    public static int statusIndex = 1;

    public final static String tiffIndex_KEY = "fileIndex";

    public static int fileIndex = 3;

    public final static String styleIndex_KEY = "styleIndex";

    public static int styleIndex = 5;

    public final static String titleIndex_KEY = "titleIndex";

    public static int titleIndex = 6;

    public final Properties prop;

    public StatusMapper(Properties prop) {
        this.prop = prop;
        // org.springframework.beans.BeanUtils. //todo
    }

    /**
     * STATUS Label 1 convert 2 retilize 3 to publish on GS 4 published on GS 5 to link in GN 6 linked in GN 7 published on local GS 8 publishd on MS
     * - error 9 10 locked
     * 
     * @param LOGGER
     * @param data
     * @return
     */
    public static List<Map<String, String>> configureFlow(Logger LOGGER, Object[] data, final Map<String,String> commonEnv) {
        
        IntegerCaster caster = new IntegerCaster();
        Integer status = null;
        if (checkSize(data, statusIndex)){
            status = caster.cast(data[statusIndex]);
            if (status == null && LOGGER != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("unable to cast data at index: " + statusIndex + " from data: "
                            + Arrays.toString(data));
                }
                return null;
            }
        } else {
            return null;
        }

//        List<Map<String, String>> flowConfiguration;
        switch (status) {
        // STATUS Label
        // ///////////////////////
        // 1 convert
        case 1:
            // 2 retilize
        case 2:
            // 1 & 2 can be performed using tech-cdr flow
            break;
        // ///////////////////////
        // 3 to publish on GS
        case 3:
            return Collections.singletonList(GeotiffConfigurator.configureGeoTiffPublish(data,commonEnv));
        // ///////////////////////
        // 4 published on GS
        case 4:
            // do nothing
            break;
        // ///////////////////////
        // 5 to link in GN
        case 5:
            // do nothing
            break;
        // ///////////////////////
        // 6 linked in GN
        case 6:
            // do nothing
            break;
        // ///////////////////////
        // 7 published on local GS
        case 7:
            // do nothing
            break;
        // ///////////////////////
        // 8 publishd on MS
        case 8:
            // do nothing
            break;
        // ///////////////////////
        // 9 error
        case 9:
            // do nothing
            break;
        // ///////////////////////
        // 10 locked
        case 10:
            // do nothing
            break;
        // ///////////////////////
        default:
            if (LOGGER != null && LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unrecognized status number at index: " + statusIndex + " from data: "
                        + Arrays.toString(data));
            }
        }
        return null;
    }

    
    public static boolean checkSize(Object[] data, int index){
        if (index<0 || index>data.length){
            return false;
        }
        return true;
    }
    
    public static String getDataString(Object[] data, int index, boolean failIgnore)throws IllegalArgumentException{
        if (!checkSize(data, index)){
            throw new IllegalArgumentException("wrong index is: "+index);
        }
        String input=(String)data[index];
        if (input == null || input.isEmpty() && !failIgnore)
            throw new IllegalArgumentException("null data string");
        return input;
    }
 }
