package org.geoserver.rest;

import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public class ClearCacheFinder extends Finder {

    @Autowired
    private ClearCache clearCache;

    public ClearCacheFinder() {
    }
    
    public ClearCacheFinder(ClearCache clearCache) {
        this.clearCache = clearCache;
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        return new ClearCacheResource(getContext(), request, response, clearCache);
    }
}
