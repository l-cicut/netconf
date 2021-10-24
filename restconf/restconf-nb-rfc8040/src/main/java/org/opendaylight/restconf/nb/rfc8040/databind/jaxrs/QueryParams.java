/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.databind.jaxrs;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.restconf.nb.rfc8040.utils.parser.ParserFieldsParameter.parseFieldsParameter;
import static org.opendaylight.restconf.nb.rfc8040.utils.parser.ParserFieldsParameter.parseFieldsPaths;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.common.errors.RestconfError;
import org.opendaylight.restconf.nb.rfc8040.ContentParam;
import org.opendaylight.restconf.nb.rfc8040.DepthParam;
import org.opendaylight.restconf.nb.rfc8040.FieldsParam;
import org.opendaylight.restconf.nb.rfc8040.FilterParam;
import org.opendaylight.restconf.nb.rfc8040.InsertParam;
import org.opendaylight.restconf.nb.rfc8040.NotificationQueryParams;
import org.opendaylight.restconf.nb.rfc8040.PointParam;
import org.opendaylight.restconf.nb.rfc8040.ReadDataParams;
import org.opendaylight.restconf.nb.rfc8040.StartTimeParam;
import org.opendaylight.restconf.nb.rfc8040.StopTimeParam;
import org.opendaylight.restconf.nb.rfc8040.WithDefaultsParam;
import org.opendaylight.restconf.nb.rfc8040.WriteDataParams;
import org.opendaylight.restconf.nb.rfc8040.legacy.QueryParameters;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;

@Beta
public final class QueryParams {
    private static final Set<String> ALLOWED_PARAMETERS = Set.of(ContentParam.uriName(), DepthParam.uriName(),
        FieldsParam.uriName(), WithDefaultsParam.uriName());
    private static final List<String> POSSIBLE_CONTENT = Arrays.stream(ContentParam.values())
        .map(ContentParam::paramValue)
        .collect(Collectors.toUnmodifiableList());
    private static final List<String> POSSIBLE_WITH_DEFAULTS = Arrays.stream(WithDefaultsParam.values())
        .map(WithDefaultsParam::paramValue)
        .collect(Collectors.toUnmodifiableList());

    private QueryParams() {
        // Utility class
    }

    public static @NonNull NotificationQueryParams newNotificationQueryParams(final UriInfo uriInfo) {
        StartTimeParam startTime = null;
        StopTimeParam stopTime = null;
        FilterParam filter = null;
        boolean skipNotificationData = false;

        for (Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            final String paramName = entry.getKey();
            final List<String> paramValues = entry.getValue();

            try {
                if (paramName.equals(StartTimeParam.uriName())) {
                    startTime = optionalParam(StartTimeParam::forUriValue, paramName, paramValues);
                    break;
                } else if (paramName.equals(StopTimeParam.uriName())) {
                    stopTime = optionalParam(StopTimeParam::forUriValue, paramName, paramValues);
                    break;
                } else if (paramName.equals(FilterParam.uriName())) {
                    filter = optionalParam(FilterParam::forUriValue, paramName, paramValues);
                } else if (paramName.equals("odl-skip-notification-data")) {
                    // FIXME: this should be properly encapsulated in SkipNotificatioDataParameter
                    skipNotificationData = Boolean.parseBoolean(optionalParam(paramName, paramValues));
                } else {
                    throw new RestconfDocumentedException("Bad parameter used with notifications: " + paramName,
                        ErrorType.PROTOCOL, ErrorTag. UNKNOWN_ATTRIBUTE);
                }
            } catch (IllegalArgumentException e) {
                throw new RestconfDocumentedException("Invalid " + paramName + " value: " + e.getMessage(), e);
            }
        }

        try {
            return NotificationQueryParams.of(startTime, stopTime, filter, skipNotificationData);
        } catch (IllegalArgumentException e) {
            throw new RestconfDocumentedException("Invalid query parameters: " + e.getMessage(), e);
        }
    }

    public static QueryParameters newQueryParameters(final ReadDataParams params,
            final InstanceIdentifierContext<?> identifier) {
        final var fields = params.fields();
        if (fields == null) {
            return QueryParameters.of(params);
        }

        return identifier.getMountPoint() != null
            ? QueryParameters.ofFieldPaths(params, parseFieldsPaths(identifier, fields.paramValue()))
                : QueryParameters.ofFields(params, parseFieldsParameter(identifier, fields.paramValue()));
    }

    /**
     * Parse parameters from URI request and check their types and values.
     *
     * @param uriInfo    URI info
     * @return {@link ReadDataParams}
     */
    public static @NonNull ReadDataParams newReadDataParams(final UriInfo uriInfo) {
        ContentParam content = ContentParam.ALL;
        DepthParam depth = null;
        FieldsParam fields = null;
        WithDefaultsParam withDefaults = null;
        boolean tagged = false;

        for (Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            final String paramName = entry.getKey();
            final List<String> paramValues = entry.getValue();

            if (paramName.equals(ContentParam.uriName())) {
                final String str = optionalParam(paramName, paramValues);
                if (str != null) {
                    content = RestconfDocumentedException.throwIfNull(ContentParam.forUriValue(str),
                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                        "Invalid content parameter: %s, allowed values are %s", str, POSSIBLE_CONTENT);
                }
            } else if (paramName.equals(DepthParam.uriName())) {
                final String str = optionalParam(paramName, paramValues);
                try {
                    depth = DepthParam.forUriValue(str);
                } catch (IllegalArgumentException e) {
                    throw new RestconfDocumentedException(e, new RestconfError(ErrorType.PROTOCOL,
                        ErrorTag.INVALID_VALUE, "Invalid depth parameter: " + str, null,
                        "The depth parameter must be an integer between 1 and 65535 or \"unbounded\""));
                }
            } else if (paramName.equals(FieldsParam.uriName())) {
                final String str = optionalParam(paramName, paramValues);
                if (str != null) {
                    try {
                        fields = FieldsParam.parse(str);
                    } catch (ParseException e) {
                        throw new RestconfDocumentedException(e, new RestconfError(ErrorType.PROTOCOL,
                            ErrorTag.INVALID_VALUE, "Invalid filds parameter: " + str));
                    }
                }
            } else if (paramName.equals(WithDefaultsParam.uriName())) {
                final String str = optionalParam(paramName, paramValues);
                if (str != null) {
                    final WithDefaultsParam val = WithDefaultsParam.forUriValue(str);
                    if (val == null) {
                        throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL,
                            ErrorTag.INVALID_VALUE, "Invalid with-defaults parameter: " + str, null,
                            "The with-defaults parameter must be a string in " + POSSIBLE_WITH_DEFAULTS));
                    }

                    switch (val) {
                        case REPORT_ALL:
                            withDefaults = null;
                            tagged = false;
                            break;
                        case REPORT_ALL_TAGGED:
                            withDefaults = null;
                            tagged = true;
                            break;
                        default:
                            withDefaults = val;
                            tagged = false;
                    }
                }
            } else {
                // FIXME: recognize pretty-print here
                throw new RestconfDocumentedException("Not allowed parameter for read operation: " + paramName,
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE);
            }
        }

        return ReadDataParams.of(content, depth, fields, withDefaults, tagged, false);
    }

    public static @NonNull WriteDataParams newWriteDataParams(final UriInfo uriInfo) {
        InsertParam insert = null;
        PointParam point = null;

        for (final Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            final String uriName = entry.getKey();
            final List<String> paramValues = entry.getValue();
            if (uriName.equals(InsertParam.uriName())) {
                final String str = optionalParam(uriName, paramValues);
                if (str != null) {
                    insert = InsertParam.forUriValue(str);
                    if (insert == null) {
                        throw new RestconfDocumentedException("Unrecognized insert parameter value '" + str + "'",
                            ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT);
                    }
                }
            } else if (PointParam.uriName().equals(uriName)) {
                final String str = optionalParam(uriName, paramValues);
                if (str != null) {
                    point = PointParam.forUriValue(str);
                }
            } else {
                throw new RestconfDocumentedException("Bad parameter for post: " + uriName,
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE);
            }
        }

        try {
            return WriteDataParams.of(insert, point);
        } catch (IllegalArgumentException e) {
            throw new RestconfDocumentedException("Invalid query parameters: " + e.getMessage(), e);
        }
    }

    @VisibleForTesting
    static @Nullable String optionalParam(final String name, final List<String> values) {
        switch (values.size()) {
            case 0:
                return null;
            case 1:
                return requireNonNull(values.get(0));
            default:
                throw new RestconfDocumentedException("Parameter " + name + " can appear at most once in request URI",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    private static <T> @Nullable T optionalParam(final Function<String, @NonNull T> factory, final String name,
            final List<String> values) {
        final String str = optionalParam(name, values);
        return str == null ? null : factory.apply(str);
    }
}
