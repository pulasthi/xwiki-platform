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
package org.xwiki.annotation.maintainer.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xwiki.annotation.maintainer.AnnotationMaintainer;
import org.xwiki.annotation.maintainer.MaintainerServiceException;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Event listener to listen to documents update events and update the annotations that are impacted by the document
 * <strong>content</strong> change, to update the selection and context to match the new document content. <br />
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component("document-content-annotation-updater")
public class DocumentContentAnnotationUpdateListener extends AbstractLogEnabled implements EventListener
{
    /**
     * Entity reference serializer, to serialize the modified document reference to send to the annotations service.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;

    /**
     * Annotations maintainer, to be invoked for the document content update.
     */
    @Requirement
    private AnnotationMaintainer maintainer;

    /**
     * Marks that there is currently an annotations update in progress so all the saves should not trigger a new update.
     * All document edits that take place because of updating the annotations for the current document shouldn't be
     * considered.
     */
    private volatile boolean isUpdating;

    /**
     * The events observed by this observation manager.
     */
    private final List<Event> eventsList = new ArrayList<Event>(Arrays.asList(new DocumentUpdatedEvent()));

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return eventsList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "DocumentContentAnnotationUpdateListener";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentModelBridge currentDocument = (DocumentModelBridge) source;

        DocumentModelBridge previousDocument = currentDocument.getOriginalDocument();

        // if it's not a modification triggered by the updates of the annotations while running the same annotation
        // maintainer, and the difference is in the content of the document
        if (!isUpdating && !previousDocument.getContent().equals(currentDocument.getContent())) {
            isUpdating = true;
            String content = currentDocument.getContent();
            String previousContent = previousDocument.getContent();
            // create the document reference
            EntityReference docReference =
                new EntityReference(currentDocument.getPageName(), EntityType.DOCUMENT, new EntityReference(
                    currentDocument.getSpaceName(), EntityType.SPACE, new EntityReference(
                        currentDocument.getWikiName(), EntityType.WIKI)));
            // maintain the document annotations
            try {
                maintainer.updateAnnotations(serializer.serialize(docReference), previousContent, content);
            } catch (MaintainerServiceException e) {
                getLogger().warn(e.getMessage(), e);
                // nothing else, just go further
            }
            isUpdating = false;
        }
    }
}
