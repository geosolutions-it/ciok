/**
 * 
 */
package org.geoserver.wps.fao;

import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

class FlatMapConverter extends MapConverter {
    
    public FlatMapConverter(XStream xstream) {
        super(xstream.getMapper());
    }
    
    @Override
    public boolean canConvert(Class type) {
        return Map.class.isAssignableFrom(type);
    }
    
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
    
        Map<String, String> map = (Map<String, String>) source;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writer.startNode(entry.getKey());
            writer.setValue(entry.getValue());
            writer.endNode();
        }
    }
}