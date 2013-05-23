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
public class ClearCacheResource extends MapResource {

    final ClearCache clearCache;

    public ClearCacheResource(Context context, Request request, Response response,
            ClearCache clearCache) {
        super(context, request, response);
        this.clearCache = clearCache;
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
                if (isValid(catalog)) {
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
                } else {
                    // /{workspace}/{layer}<br>
                    map = clearCache.getLayer(workspace, layer);
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
                if (isValid(catalog)) {
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
                } else {
                    // /{workspace}/{layer}<br>
                    clearCache.clearLayer(workspace, layer, map);
                }
            }
        }
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleDelete() {
        String workspace = getAttribute("workspace");
        if (!isValid(workspace)) {
            throw new UnsupportedOperationException("Missing workspace parameter");
        } else {
            String layer = getAttribute("layer");
            if (!isValid(layer)) {
                // /{workspace}<br>
                clearCache.deleteWorkspace(workspace);
            } else {
                String catalog = getAttribute("catalog");
                if (isValid(catalog)) {
                    String schema = getAttribute("schema");
                    String cube = getAttribute("cube");
                    if (isValid(schema) && isValid(cube)) {
                        String connection = getAttribute("connection");
                        if (!isValid(connection)) {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}<br>
                            clearCache.deleteCube(workspace, layer, catalog, schema, cube);
                        } else {
                            // /{workspace}/{layer}/{catalog}/{schema}/{cube}/{connection}
                            clearCache.deleteConnection(workspace, layer, catalog, schema, cube,
                                    connection);
                        }
                    } else {
                        throw new UnsupportedOperationException(
                                "Missing schema and cube parameters");
                    }
                } else {
                    // /{workspace}/{layer}<br>
                    clearCache.deleteLayer(workspace, layer);
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
