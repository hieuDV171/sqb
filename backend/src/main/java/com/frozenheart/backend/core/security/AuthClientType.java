package com.frozenheart.backend.core.security;

/** Token transport selected by the server endpoint, not by device metadata. */
public enum AuthClientType {
    WEB, NATIVE, LEGACY_JSON
}
