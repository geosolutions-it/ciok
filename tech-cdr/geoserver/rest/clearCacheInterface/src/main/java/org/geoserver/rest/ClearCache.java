package org.geoserver.rest;

import java.util.Map;

public interface ClearCache {

    public void clearWorkspace(String workspace, Map map);
    
    public Map getWorkspace(String workspace);
    
    public void clearLayer(String workspace, String layer, Map map);
    
    public Map getLayer(String workspace, String layer);
    
    public void clearCube(String workspace, String layer, String catalog, String schema, String cube, Map map);
    
    public Map getCube(String workspace, String layer, String catalog, String schema, String cube);
    
    public void clearConnection(String workspace, String layer, String catalog, String schema, String cube, String connection, Map map);
    
    public Map getConnection(String workspace, String layer, String catalog, String schema, String cube, String connection);

}
