/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * 
 * @author ETj <etj at geo-solutions.it>
 */
public class LoggingMonitor implements ProgressListener {

    private final Logger logger;

    private final Level level;

    public LoggingMonitor(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    public String getDescription() {
        return null;
    }

    public InternationalString getTask() {
        return null;
    }

    public void setTask(InternationalString task) {
        if (logger.isLoggable(level))
            logger.log(level, "setTask: " + task.toString());
    }

    public void setDescription(String description) {
        if (logger.isLoggable(level))
            logger.log(level, "setDescription: " + description.toString());
    }

    public void started() {
        if (logger.isLoggable(level))
            logger.log(level, "started.");
    }

    public void progress(float percent) {
        if (logger.isLoggable(level))
            logger.log(level, "progress: " + percent + "%");
    }

    public float getProgress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void complete() {
        if (logger.isLoggable(level))
            logger.log(level, "completed.");
    }

    public void dispose() {
        if (logger.isLoggable(level))
            logger.log(level, "dispose.");
    }

    public boolean isCanceled() {
        return false;
    }

    public void setCanceled(boolean cancel) {
        if (logger.isLoggable(level))
            logger.log(level, "setCanceled: " + cancel);
    }

    public void warningOccurred(String source, String location, String warning) {
        if (logger.isLoggable(level))
            logger.log(level, "warning: source:" + source + " location:" + location + " warning:"
                    + warning);
    }

    public void exceptionOccurred(Throwable exception) {
        if (logger.isLoggable(level))
            logger.log(level, "exception: " + exception.getMessage());
    }

}