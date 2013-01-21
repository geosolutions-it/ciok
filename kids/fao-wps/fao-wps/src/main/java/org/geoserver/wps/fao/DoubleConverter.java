/**
 * 
 */
package org.geoserver.wps.fao;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

class DoubleConverter extends MapConverter {

    public DoubleConverter(XStream xstream) {
        super(xstream.getMapper());
    }
    
    @Override
    public boolean canConvert(Class type) {
        return Double.class.isAssignableFrom(type);
    }
    
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        DecimalFormat df = new DecimalFormat("0.######", new DecimalFormatSymbols(Locale.ENGLISH));
        writer.setValue(df.format((Double) source));
    }
}