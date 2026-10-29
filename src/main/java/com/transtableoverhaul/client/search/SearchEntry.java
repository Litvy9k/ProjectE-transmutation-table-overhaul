package com.transtableoverhaul.client.search;

import java.util.List;

/**
 * Lower-cased searchable text of one terminal entry.
 */
public interface SearchEntry {

    String name();

    String id();

    String modId();

    String modName();

    List<String> tags();

    String tooltip();
}
