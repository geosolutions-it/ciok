package org.geoserver.rest;

import javax.annotation.Resource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

@Resource
public class DummyClearCache extends AbstractClearCache {

    Logger LOGGER = Logger.getLogger(this.getClass());

    public DummyClearCache() {
    }

    // DELETE

    @Override
    public void deleteCube(String workspace, String layer, String catalog, String schema,
            String cube) {
        LOGGER.error("DELETE CUBE: "
                + ArrayUtils.toString(new String[] { workspace, layer, catalog, schema, cube }));
    }

    @Override
    public void deleteConnection(String workspace, String layer, String catalog, String schema,
            String cube, String connection) {
        LOGGER.error("DELETE CONNECTION: "
                + ArrayUtils.toString(new String[] { workspace, layer, catalog, schema, cube,
                        connection }));
    }

    // CLEAR

    @Override
    public void clearWorkspace(String workspace) {
        LOGGER.error("CLEAR WORKSPACE: " + ArrayUtils.toString(new String[] { workspace }));
    }

    @Override
    public void clearLayer(String workspace, String layer) {
        LOGGER.error("CLEAR LAYER: " + ArrayUtils.toString(new String[] { workspace, layer }));
    }

    @Override
    public void clearCube(String workspace, String layer, String catalog, String schema, String cube) {
        // TODO Auto-generated method stub
        LOGGER.error("CLEAR CUBE: "
                + ArrayUtils.toString(new String[] { workspace, layer, catalog, schema, cube}));
    }

    @Override
    public void clearConnection(String workspace, String layer, String catalog, String schema,
            String cube, String connection) {
        LOGGER.error("CLEAR CONNECTION: "
                + ArrayUtils.toString(new String[] { workspace, layer, catalog, schema, cube,
                        connection }));
    }

}
