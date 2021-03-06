/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.officeimporter.internal.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.xwiki.component.logging.Logger;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;

/**
 * Default implementation of {@link OpenOfficeConverter}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultOpenOfficeConverter implements OpenOfficeConverter
{
    /**
     * Converter provided by jodconverter library.
     */
    private OfficeDocumentConverter converter;

    /**
     * Working directory to be used when working with files.
     */
    private File workDir;

    /**
     * Used for logging.
     */
    private Logger logger;

    /**
     * Creates a new {@link DefaultOpenOfficeConverter} instance.
     * 
     * @param converter provided by jodconverter library.
     * @param workDir space for holding temporary file.
     * @param logger logging support.
     */
    public DefaultOpenOfficeConverter(OfficeDocumentConverter converter, File workDir, Logger logger)
    {
        this.converter = converter;
        this.workDir = workDir;
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, byte[]> convert(Map<String, InputStream> inputStreams, String inputFileName,
        String outputFileName) throws OpenOfficeConverterException
    {
        // Verify whether an input stream is present for the main input file.
        if (null == inputStreams.get(inputFileName)) {
            String message = "No input stream specified for main input file [%s].";
            throw new OpenOfficeConverterException(String.format(message, inputFileName));
        }

        OpenOfficeConverterFileStorage storage = null;
        try {
            // Prepare temporary storage.
            storage = new OpenOfficeConverterFileStorage(workDir, inputFileName, outputFileName);

            // Write out all the input streams.
            FileOutputStream fos = null;
            for (Map.Entry<String, InputStream> entry : inputStreams.entrySet()) {
                try {
                    File temp = new File(storage.getInputDir(), entry.getKey());
                    fos = new FileOutputStream(temp);
                    IOUtils.copy(entry.getValue(), fos);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }

            // Perform the conversion.
            converter.convert(storage.getInputFile(), storage.getOutputFile());

            // Collect all the output artifacts.
            Map<String, byte[]> result = new HashMap<String, byte[]>();
            FileInputStream fis = null;
            for (File file : storage.getOutputDir().listFiles()) {
                try {
                    fis = new FileInputStream(file);
                    result.put(file.getName(), IOUtils.toByteArray(fis));
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }

            return result;
        } catch (Exception ex) {
            throw new OpenOfficeConverterException("Error while performing conversion.", ex);
        } finally {
            if (!storage.cleanUp()) {
                logger.error("Could not cleanup temporary storage after conversion.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see OpenOfficeConverter#isMediaTypeSupported(String)
     */
    public boolean isMediaTypeSupported(String mediaType)
    {
        return converter.getFormatRegistry().getFormatByMediaType(mediaType) != null;
    }
}
