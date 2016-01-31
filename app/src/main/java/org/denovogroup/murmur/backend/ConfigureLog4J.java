/*
* Copyright (c) 2016, De Novo Group
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from this
* software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRES S OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package org.denovogroup.murmur.backend;


import android.os.Environment;

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;

import java.io.File;

public class ConfigureLog4J {

    public static final File LOG_DIRECTORY = Environment.getExternalStorageDirectory();
    public static final String LOG_FILE = "murmur.log";
    public static final String LOG_FILENAME = LOG_DIRECTORY + File.separator + LOG_FILE;
    public static final String LOG_LINE_PATTERN = "%d - [%t] - [%p::%c] - %m%n";
    public static final Level LOG_LEVEL = Level.DEBUG;
    public static final long FILE_SIZE = 6 * 1024 * 1024; //MB
    public static final int MAX_BACKUP_SIZE = 2; //Number of files

    public static void configure(boolean FreezeLogging) {
        final LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFilePattern(LOG_LINE_PATTERN);
        logConfigurator.setFileName(LOG_FILENAME);
        logConfigurator.setRootLevel(FreezeLogging ? Level.OFF : LOG_LEVEL);
        logConfigurator.setMaxFileSize(FILE_SIZE);
        logConfigurator.setMaxBackupSize(MAX_BACKUP_SIZE);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setResetConfiguration(FreezeLogging);
        try {
            logConfigurator.configure();
        } catch (Exception e){
            //log configuration not supported by device
        }
    }
}