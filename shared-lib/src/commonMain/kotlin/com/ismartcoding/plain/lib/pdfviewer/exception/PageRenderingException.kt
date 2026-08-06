package com.ismartcoding.plain.lib.pdfviewer.exception

class PageRenderingException(val page: Int, cause: Throwable) : Exception(cause)
