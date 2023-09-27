package com.ismartcoding.lib.pdfviewer.exception

class PageRenderingException(val page: Int, cause: Throwable) : Exception(cause)
