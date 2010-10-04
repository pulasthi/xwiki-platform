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
package org.xwiki.officeimporter.internal.filter;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

/**
 * Converts usual xhtml links into xwiki compatible links. As an example, the link {@code <a href="foo">link</a>} will
 * be replaced by {@code <!--startwikilink:foo--><span class="wikiexternallink"><a
 * href="foo">link</a></span><!--stopwikilink-->}
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/link")
public class LinkFilter extends AbstractHTMLFilter
{
    /**
     * {@inheritDoc}
     */
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        List<Element> links =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_A}, new ElementSelector()
            {
                public boolean isSelected(Element element)
                {
                    return !element.getAttribute(ATTRIBUTE_HREF).equals("");
                }
            });
        for (Element link : links) {
            Node parent = link.getParentNode();
            Element span = document.createElement(TAG_SPAN);
            span.setAttribute(ATTRIBUTE_CLASS, "wikiexternallink");
            span.appendChild(link.cloneNode(true));
            parent.replaceChild(span, link);
            Comment beforeComment = document.createComment("startwikilink:" + link.getAttribute(ATTRIBUTE_HREF));
            Comment afterComment = document.createComment("stopwikilink");
            parent.insertBefore(beforeComment, span);
            parent.insertBefore(afterComment, span.getNextSibling());
        }
    }
}