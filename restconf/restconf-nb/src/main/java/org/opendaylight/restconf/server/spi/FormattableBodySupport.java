/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.server.spi;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javanet.staxutils.IndentingXMLStreamWriter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.restconf.api.FormatParameters;
import org.opendaylight.restconf.api.FormattableBody;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;

/**
 * Various methods supporting {@link FormattableBody}.
 */
@NonNullByDefault
public final class FormattableBodySupport {
    private static final XMLOutputFactory XML_FACTORY = XMLOutputFactory.newFactory();
    private static final String PRETTY_PRINT_INDENT = "  ";

    private FormattableBodySupport() {
        // Hidden on purpose
    }

    public static JsonWriter createJsonWriter(final OutputStream out, final FormatParameters format) {
        final var ret = JsonWriterFactory.createJsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        ret.setIndent(format.prettyPrint().value() ? PRETTY_PRINT_INDENT : "");
        return ret;
    }

    public static XMLStreamWriter createXmlWriter(final OutputStream out, final FormatParameters format)
            throws IOException {
        final var xmlWriter = createXmlWriter(out);
        return format.prettyPrint().value() ? new IndentingXMLStreamWriter(xmlWriter) : xmlWriter;
    }

    private static XMLStreamWriter createXmlWriter(final OutputStream out) throws IOException {
        try {
            return XML_FACTORY.createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new IOException(e);
        }
    }
}