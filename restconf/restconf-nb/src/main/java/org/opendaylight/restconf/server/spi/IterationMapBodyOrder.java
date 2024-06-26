/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.server.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

@NonNullByDefault
final class IterationMapBodyOrder extends MapBodyOrder {
    static final IterationMapBodyOrder INSTANCE = new IterationMapBodyOrder();

    private IterationMapBodyOrder() {
        // Hidden on purpose
    }

    @Override
    Iterable<DataContainerChild> orderBody(final MapEntryNode entry) {
        return entry.body();
    }
}