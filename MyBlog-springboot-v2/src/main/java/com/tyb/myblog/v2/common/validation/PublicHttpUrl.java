package com.tyb.myblog.v2.common.validation;

import java.net.URI;

public final class PublicHttpUrl {

    private PublicHttpUrl() {
    }

    public static boolean isValid(URI uri) {
        return uri != null
                && uri.getHost() != null
                && !uri.getHost().isBlank()
                && uri.getUserInfo() == null
                && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()));
    }
}
