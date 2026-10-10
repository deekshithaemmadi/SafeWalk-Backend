package com.anurag.itds.entity;

/**
 * The kind of help location. Values are stored as their {@link #name()} in the
 * database (see {@code @Enumerated(EnumType.STRING)} on {@link HelpLocation}).
 */
public enum HelpLocationType {
    HOSPITAL,
    POLICE,
    WOMEN_HELP
}
