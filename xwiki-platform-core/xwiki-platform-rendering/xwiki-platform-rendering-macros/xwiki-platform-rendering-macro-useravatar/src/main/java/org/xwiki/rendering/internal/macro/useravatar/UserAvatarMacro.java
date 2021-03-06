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
 *
 */
package org.xwiki.rendering.internal.macro.useravatar;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.useravatar.UserAvatarMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Allows displaying the avatar for a specific user.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@Component("useravatar")
public class UserAvatarMacro extends AbstractMacro<UserAvatarMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Allows displaying the avatar for a specific user.";

    /**
     * Used to get the user avatar picture from his profile.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to get the default avatar picture when the user doesn't exist.
     */
    @Requirement
    private SkinAccessBridge skinAccessBridge;

    /**
     * Used to convert a user reference represented as a String (passed as a macro parameter by the user) to a
     * Document Reference.
     */
    @Requirement("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver; 

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    @Requirement("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    /**
     * Used to find out the current Wiki name.
     */
    @Requirement("current")
    private EntityReferenceValueProvider currentEntityReferenceValueProvider;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public UserAvatarMacro()
    {
        super("User Avatar", DESCRIPTION, UserAvatarMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(UserAvatarMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DocumentReference userReference = this.currentDocumentReferenceResolver.resolve(parameters.getUsername());

        // Find the avatar attachment name or null if not defined or an error happened when locating it
        String fileName = null;
        if (documentAccessBridge.exists(userReference)) {
            Object avatarProperty = documentAccessBridge.getProperty(userReference,
                new DocumentReference(this.currentEntityReferenceValueProvider.getDefaultValue(EntityType.WIKI),
                    "XWiki", "XWikiUsers"), "avatar");
            if (avatarProperty != null) {
                fileName = avatarProperty.toString(); 
            }
        } else {
            throw new MacroExecutionException("User ["
                + this.compactWikiEntityReferenceSerializer.serialize(userReference)
                + "] is not registered in this wiki");
        }

        ResourceReference imageReference;
        if (StringUtils.isBlank(fileName)) {
            imageReference = new ResourceReference(skinAccessBridge.getSkinFile("noavatar.png"), ResourceType.URL);
        } else {
            AttachmentReference attachmentReference = new AttachmentReference(fileName, userReference);
            imageReference = new ResourceReference(
                this.compactWikiEntityReferenceSerializer.serialize(attachmentReference), ResourceType.ATTACHMENT);
        }
        ImageBlock imageBlock = new ImageBlock(imageReference, false);

        imageBlock.setParameter("alt", "Picture of " + userReference.getName());
        imageBlock.setParameter("title", userReference.getName());

        if (parameters.getWidth() != null) {
            imageBlock.setParameter("width", String.valueOf(parameters.getWidth()));
        }

        if (parameters.getHeight() != null) {
            imageBlock.setParameter("height", String.valueOf(parameters.getHeight()));
        }

        return Collections.singletonList((Block) imageBlock);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }
}
