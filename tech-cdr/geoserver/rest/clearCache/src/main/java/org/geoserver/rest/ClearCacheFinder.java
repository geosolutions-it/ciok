package org.geoserver.rest;

import java.util.Map;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public class ClearCacheFinder extends MapResource {

    private final ClearCache clearCache;
    
//    private boolean initted=false;
    
//    @Autowired
//    private ApplicationContext context;

    public ClearCacheFinder(ClearCache clearCache) {
        this.clearCache=clearCache;
    }

    public ClearCacheFinder(Context context, Request request, Response response,ClearCache clearCache) {
        super(context, request, response);
        this.clearCache=clearCache;
    }

    @Override
    public Map getMap() throws Exception {
        Map map = null;
        String workspace = getAttribute("workspace");
        if (!isValid(workspace)) {
            throw new UnsupportedOperationException("Missing workspace parameter");
        } else {
            String layer = getAttribute("layer");
            if (!isValid(layer)) {
                // /{workspace}<br>
                map = clearCache.getWorkspace(workspace);
            } else {
                String catalog = getAttribute("catalog");
                if (!isValid(catalog)) {
                    // /{workspace}/{layer}<br>
                    map = clearCache.getLayer(workspace, layer);
                } else {
                    String schema = getAttribute("schema");
                    String cube = getAttribute("cube");
                    if (isValid(schema) && isValid(cube)) {
                        String connection = getAttribute("connection");
                        if (!isValid(connection)) {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}<br>
                            map = clearCache.getCube(workspace, layer, catalog, schema, cube);
                        } else {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}/{connection}
                            map = clearCache.getConnection(workspace, layer, catalog, schema, cube,
                                    connection);
                        }
                    } else {
                        throw new UnsupportedOperationException(
                                "Missing schema and cube parameters");
                    }
                }
            }
        }
        return map;
    }

    /**
     * 
     * /{workspace}<br>
     * /{workspace}/{layer}<br>
     * /{workspace}/{layer}/{catalog}/{schema}/{cube}<br>
     * /{workspace}/{layer}/{catalog}/{schema}/{cube}/{connection}<br>
     * 
     */
    @Override
    protected void postMap(Map map) throws Exception {
        String workspace = getAttribute("workspace");
        if (!isValid(workspace)) {
            throw new UnsupportedOperationException("Missing workspace parameter");
        } else {
            String layer = getAttribute("layer");
            if (!isValid(layer)) {
                // /{workspace}<br>
                clearCache.clearWorkspace(workspace, map);
            } else {
                String catalog = getAttribute("catalog");
                if (!isValid(catalog)) {
                    // /{workspace}/{layer}<br>
                    clearCache.clearLayer(workspace, layer, map);
                } else {
                    String schema = getAttribute("schema");
                    String cube = getAttribute("cube");
                    if (isValid(schema) && isValid(cube)) {
                        String connection = getAttribute("connection");
                        if (!isValid(connection)) {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}<br>
                            clearCache.clearCube(workspace, layer, catalog, schema, cube, map);
                        } else {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}/{connection}
                            clearCache.clearConnection(workspace, layer, catalog, schema, cube,
                                    connection, map);
                        }
                    } else {
                        throw new UnsupportedOperationException(
                                "Missing schema and cube parameters");
                    }
                }
            }
        }
    }

    @Override
    protected void putMap(Map map) throws Exception {
        postMap(map);
    }

    private static boolean isValid(String param) {
        if (param != null && !param.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
