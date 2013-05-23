package org.geoserver.rest;

import java.util.Map;

/**
 * 
 * Simple implementation of the {@link ClearCache} interface:
 * <ol>
 * <li>1. all the requests are converted in clear operations.</li>
 * <li>2. maps (returned and set) are all null)</li>
 * </ol>
 * 
 * @author carlo cancellieri - geosolutions SAS
 * 
 */

public abstract class AbstractClearCache implements ClearCache {

    public void clearWorkspace(String workspace, Map map) {
        clearWorkspace(workspace);
    }
    
    public void deleteWorkspace(String workspace) {
        clearWorkspace(workspace);
    }

    public Map getWorkspace(String workspace) {
        clearWorkspace(workspace);
        return null;
    }

    public void clearLayer(String workspace, String layer, Map map) {
        clearLayer(workspace, layer);
    }
    
    public void deleteLayer(String workspace, String layer) {
        clearLayer(workspace, layer);
    }

    public Map getLayer(String workspace, String layer) {
        clearLayer(workspace, layer);
        return null;
    }

    public void clearCube(String workspace, String layer, String catalog, String schema,
            String cube, Map map) {
        clearCube(workspace, layer, catalog, schema, cube);
    }
    
    public void deleteCube(String workspace, String layer, String catalog, String schema,
            String cube) {
        clearCube(workspace, layer, catalog, schema, cube);
    }

    public Map getCube(String workspace, String layer, String catalog, String schema, String cube) {
        clearCube(workspace, layer, catalog, schema, cube);
        return null;
    }

    public void clearConnection(String workspace, String layer, String catalog, String schema,
            String cube, String connection, Map map) {
        clearConnection(workspace, layer, catalog, schema, cube, connection);
    }
    
    public void deleteConnection(String workspace, String layer, String catalog, String schema,
            String cube, String connection) {
        clearConnection(workspace, layer, catalog, schema, cube, connection);
    }

    public Map getConnection(String workspace, String layer, String catalog, String schema,
            String cube, String connection) {
        clearConnection(workspace, layer, catalog, schema, cube, connection);
        return null;
    }

    public abstract void clearWorkspace(String workspace);

    public abstract void clearLayer(String workspace, String layer);

    public abstract void clearCube(String workspace, String layer, String catalog, String schema,
            String cube);

    public abstract void clearConnection(String workspace, String layer, String catalog,
            String schema, String cube, String connection);

}
