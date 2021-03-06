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
package org.xwiki.office.viewer.internal;

import java.util.Collections;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.office.viewer.OfficeViewerScriptService;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Default implementation of {@link OfficeViewerScriptService}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component("officeviewer")
public class DefaultOfficeViewerScriptService extends AbstractLogEnabled implements OfficeViewerScriptService
{
    /**
     * The key used to save on the execution context the exception caught during office document view.
     */
    private static final String OFFICE_VIEW_EXCEPTION = "officeView.caughtException";

    /**
     * The component used to view office documents.
     */
    @Requirement
    private OfficeViewer officeViewer;

    /**
     * The component used to retrieve the office document converter, which knows the supported media types.
     * 
     * @see #isMimeTypeSupported(String)
     */
    @Requirement
    private OpenOfficeManager officeManager;

    /**
     * Used to lookup various {@link BlockRenderer} implementations based on the output syntax.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Reference to the current execution context, used to save the exception caught during office document view.
     */
    @Requirement
    private Execution execution;

    /**
     * The component used to check access rights on the document holding the office attachment to be viewed.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to perform the XDOM transformations.
     */
    @Requirement
    private TransformationManager transformationManager;

    /**
     * {@inheritDoc}
     * 
     * @see OfficeViewerScriptService#getCaughtException()
     */
    public Exception getCaughtException()
    {
        return (Exception) execution.getContext().getProperty(OFFICE_VIEW_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficeViewerScriptService#view(AttachmentReference)
     */
    public String view(AttachmentReference attachmentReference)
    {
        Map<String, String> parameters = Collections.emptyMap();
        return view(attachmentReference, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficeViewerScriptService#view(AttachmentReference, Map)
     */
    public String view(AttachmentReference attachmentReference, Map<String, String> parameters)
    {
        // Clear previous caught exception.
        execution.getContext().removeProperty(OFFICE_VIEW_EXCEPTION);
        try {
            DocumentReference documentReference = attachmentReference.getDocumentReference();
            // Check whether current user has view rights on the document containing the attachment.
            if (!documentAccessBridge.isDocumentViewable(documentReference)) {
                throw new RuntimeException("Inadequate privileges.");
            }

            // Create the view and render the result.
            Syntax fromSyntax = documentAccessBridge.getDocument(documentReference).getSyntax();
            Syntax toSyntax = Syntax.XHTML_1_0;
            return render(officeViewer.createView(attachmentReference, parameters), fromSyntax, toSyntax);
        } catch (Exception e) {
            // Save caught exception.
            execution.getContext().setProperty(OFFICE_VIEW_EXCEPTION, e);
            getLogger().error("Failed to view office document: " + attachmentReference, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see OfficeViewerScriptService#isMimeTypeSupported(String)
     */
    public boolean isMimeTypeSupported(String mimeType)
    {
        OpenOfficeConverter converter = officeManager.getConverter();
        return converter != null && converter.isMediaTypeSupported(mimeType);
    }

    /**
     * Renders the given XDOM into specified syntax.
     * 
     * @param xdom the {@link XDOM} to be rendered
     * @param fromSyntax the syntax for which to perform the transformations
     * @param toSyntax expected output syntax
     * @return string holding the result of rendering
     * @throws Exception if an error occurs during rendering
     */
    private String render(XDOM xdom, Syntax fromSyntax, Syntax toSyntax) throws Exception
    {
        // Perform the transformations. This is required for office presentations which use the gallery macro to display
        // the slide images.
        TransformationContext context = new TransformationContext(xdom, fromSyntax);
        transformationManager.performTransformations(xdom, context);

        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = componentManager.lookup(BlockRenderer.class, toSyntax.toIdString());
        renderer.render(xdom, printer);
        return printer.toString();
    }
}
