package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.db.DAppFile
import kotlin.test.Test
import kotlin.test.assertEquals

class AppFileStoreExtTest {
    @Test
    fun extFromFileName_prefersFilenameExtension() {
        assertEquals("properties", AppFileStore.extFromFileName("local.properties", ""))
        assertEquals("properties", AppFileStore.extFromFileName("local.properties", "application/octet-stream"))
        assertEquals("jpg", AppFileStore.extFromFileName("Photo.JPG", ""))
        assertEquals("gz", AppFileStore.extFromFileName("archive.tar.gz", ""))
    }

    @Test
    fun extFromFileName_emptyWhenNameHasNoExtAndMimeUnknown() {
        assertEquals("", AppFileStore.extFromFileName("README", ""))
        assertEquals("", AppFileStore.extFromFileName(".gitignore", ""))
        assertEquals("", AppFileStore.extFromFileName("", ""))
    }

    @Test
    fun toFidUri_usesRealPathFileName() {
        val dFile = DAppFile("aabbccdd")
        dFile.realPath = "aa/bb/aabbccdd.properties"
        assertEquals("fid:aabbccdd.properties", AppFileStore.toFidUri(dFile))
        dFile.realPath = "aa/bb/aabbccdd"
        assertEquals("fid:aabbccdd", AppFileStore.toFidUri(dFile))
    }
}
