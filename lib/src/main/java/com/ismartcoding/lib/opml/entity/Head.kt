package com.ismartcoding.lib.opml.entity

class Head(
    val title: String,
    val dateCreated: String,
    val dateModified: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val ownerId: String = "",
    val docs: String = "",
    val expansionState: List<Int> = arrayListOf(),
    val vertScrollState: Int? = null,
    val windowTop: Int? = null,
    val windowLeft: Int? = null,
    val windowBottom: Int? = null,
    val windowRight: Int? = null,
) {
    val expansionStateString: String
        get() {
            return expansionState.joinToString(", ")
        }
}
