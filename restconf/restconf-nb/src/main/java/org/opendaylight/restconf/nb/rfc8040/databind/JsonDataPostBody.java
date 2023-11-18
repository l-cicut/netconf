/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.databind;

import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class JsonDataPostBody extends DataPostBody {
    public JsonDataPostBody(final InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public JsonOperationInputBody toOperationInput() {
        return new JsonOperationInputBody(acquireStream());
    }

    @Override
    public JsonChildBody toResource() {
        return new JsonChildBody(acquireStream());
    }
}
