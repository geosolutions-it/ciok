/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.geosolutions.geobatch.rasterprocessing;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geobatch.rasterprocessing.classifiedstats.DataFile;
import it.geosolutions.geobatch.rasterprocessing.classifiedstats.RasterClassifiedStatistics;
import it.geosolutions.geobatch.rasterprocessing.impl.GAEZStatsOutput;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jaitools.numeric.Statistic;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ETj (etj at geo-solutions.it)
 */
public class ClassifiedStatsAction extends BaseAction<FileSystemEvent> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClassifiedStatsAction.class);

    private final ClassifiedStatsConfiguration conf;
    
    // we're defaulting to GAEZ output, but it should be configurable
    private final ClassifiedStatsOutput output = new GAEZStatsOutput();

    public ClassifiedStatsAction(ClassifiedStatsConfiguration actionConfiguration) {
        super(actionConfiguration);
        conf = actionConfiguration;
    }

    /**
     * Encloses the params expected in the input file.
     * <p>
     * Output params should be handled externally, and used in a customizable output class.
     * Currently, we're hardcoding the output required for GAEZ.
     */
    public class ClassificationStatsParams {
        public static final String DATA = "data";
        public static final String DATAMULTIPLIER = "data_multiplier";
        public static final String CLASSIFICATION = "classification";
        public static final String OUTPUTPARAMS = "output";
        public static final String ATTRIB_NODATA = "nodata";

        public DataFile data;
        public DataFile dataMultiplier;
        public List<DataFile> classifiers;
    }


    public Queue<FileSystemEvent> execute(Queue<FileSystemEvent> ev) throws ActionException {
    	
        File file = ev.poll().getSource();
        ClassificationStatsParams params = parseParams(file); // may throw ActionEx if params not
                                                              // valid

        RasterClassifiedStatistics rcs = new RasterClassifiedStatistics();

        // some hacking for GAEZ here:
        EnumSet<Statistic> reqStats;
        if (params.classifiers.size() == 2) {
            reqStats = EnumSet.of(Statistic.SUM);
        } else if (params.classifiers.size() == 1) {
            reqStats = EnumSet.of(Statistic.MIN, Statistic.MAX, Statistic.RANGE, Statistic.MEAN,
                    Statistic.SDEV, Statistic.SUM);
        } else
            throw new ActionException(this, "Can't handle case with " + params.classifiers.size()
                    + " classifiers.");
        // furthermore we'll have to add the COUNT

        try {
            Map<MultiKey, List<Result>> result = rcs.execute(
                    this.conf.getComputationMode(),
                    params.data, params.dataMultiplier,
                    params.classifiers,
                    reqStats);
            
            output.writeResults(params, result);
            return ev;

        } catch (Exception ex) {
            throw new ActionException(this, "Error during stats computation", ex);
        }
    }


    /**
     * Parses the input file passed in the Event.
     */
    private ClassificationStatsParams parseParams(File paramFile) throws ActionException {
        ClassificationStatsParams ret = new ClassificationStatsParams();

        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(paramFile);
        } catch (Exception ex) {
            throw new ActionException(this, "Error reading param file " + paramFile, ex);
        }
        Element root = doc.detachRootElement();

        // parse data file        
        {
            Element dataElem = root.getChild(ClassificationStatsParams.DATA);
            if (dataElem == null)
                throw new ActionException(this, "Missing data param");
            String datafileName = dataElem.getTextTrim();
            if (datafileName == null)
                throw new ActionException(this, "Missing data file");
            File dataFile = new File(datafileName);
            if (!dataFile.canRead())
                throw new ActionException(this, "Error reading data file " + datafileName);
            ret.data = new DataFile(dataFile);
            String dataNodata = dataElem.getAttributeValue(ClassificationStatsParams.ATTRIB_NODATA);
            if(dataNodata != null)
                ret.data.setNoValue(new Double(dataNodata));
        }

        // parse datamultiplier file - optional
        {
            Element dataMultElem = root.getChild(ClassificationStatsParams.DATAMULTIPLIER);
            if(dataMultElem != null) {
                String datamultfileName = dataMultElem.getText();
                if (datamultfileName == null) {
                    throw new ActionException(this, "Missing data multiplier file");
                } else {
                    File datamultFile = new File(datamultfileName);
                    if (!datamultFile.canRead())
                        throw new ActionException(this, "Error reading data multiplier file " + datamultfileName);
                    ret.dataMultiplier = new DataFile(datamultFile);
                    
                    String multNodata = dataMultElem.getAttributeValue(ClassificationStatsParams.ATTRIB_NODATA);
                    if(multNodata != null)
                        ret.dataMultiplier.setNoValue(new Double(multNodata));
                }
            }
        }

        // parse classification files
        {
            List<Element> clasfElems = root.getChildren(ClassificationStatsParams.CLASSIFICATION);
            if (clasfElems.isEmpty())
                throw new ActionException(this, "Missing classifiers param");
            ret.classifiers = new ArrayList<DataFile>(clasfElems.size());
            for (Element clasfElem : clasfElems) {
                String clasfFileName = clasfElem.getText();
                File clasfFile = new File(clasfFileName);
                if (!clasfFile.canRead())
                    throw new ActionException(this, "Error reading classification file "
                            + clasfFileName);
                DataFile clasDFile = new DataFile(clasfFile);
                ret.classifiers.add(clasDFile);
                
                String classNodata = clasfElem.getAttributeValue(ClassificationStatsParams.ATTRIB_NODATA);
                if(classNodata != null)
                    clasDFile.setNoValue(new Double(classNodata));
            }
        }

        // parse output
        Element outputParams = root.getChild(ClassificationStatsParams.OUTPUTPARAMS);
        if (outputParams == null)
            throw new ActionException(this, "Missing output params");

        output.parseParams(outputParams);
        
        return ret;
    }

}
