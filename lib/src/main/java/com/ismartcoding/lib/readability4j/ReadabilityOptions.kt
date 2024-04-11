package com.ismartcoding.lib.readability4j


internal class ReadabilityOptions(
    // Max number of nodes supported by this parser. Default: 0 (no limit)
    val maxElemsToParse: Int = 0,
    // The number of top candidates to consider when analysing how
    // tight the competition is among candidates.
    val nbTopCandidates: Int = 5,
    // The default number of words an article must have in order to return a result
    val wordThreshold: Int = 50,
)