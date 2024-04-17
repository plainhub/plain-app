package com.ismartcoding.plain.ui.file

import android.content.Context
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.newPath
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch
import org.zeroturnaround.zip.ZipUtil
import java.io.File

object FilesBottomMenuHelper {
    fun onMenuItemClick(
        context: Context,
        dialog: FilesDialog,
        menuItem: MenuItem,
    ) {
        val binding = dialog.binding
        val list = binding.list
        val rv = list.rv
        val viewModel = dialog.viewModel
        when (menuItem.itemId) {
            R.id.share -> {
                rv.ensureSelect { items ->
                    ShareHelper.sharePaths(context, items.map { it.data.id }.toSet())
                }
            }
            R.id.cut -> {
                rv.ensureSelect { items ->
                    viewModel.cutFiles.clear()
                    viewModel.cutFiles.addAll(items.map { it.data as DFile })
                    dialog.showPasteAction()
                    rv.bindingAdapter.checkedAll(false)
                    viewModel.toggleMode.value = false
                }
            }
            R.id.copy -> {
                rv.ensureSelect { items ->
                    viewModel.copyFiles.clear()
                    viewModel.copyFiles.addAll(items.map { it.data as DFile })
                    dialog.showPasteAction()
                    rv.bindingAdapter.checkedAll(false)
                    viewModel.toggleMode.value = false
                }
            }
            R.id.rename -> {
                rv.ensureSelect { items ->
                    val m = items[0] as FileModel
                    val file = m.data
                    EditValueDialog(getString(R.string.rename), file.name, file.name) {
                        val name = this.binding.value.text
                        dialog.lifecycleScope.launch {
                            blockFormUI()
                            val oldName = file.name
                            val oldPath = file.path
                            val dstFile = withIO { FileHelper.rename(file.path, name) }
                            if (dstFile != null) {
                                withIO {
                                    MainApp.instance.scanFileByConnection(file.path)
                                    MainApp.instance.scanFileByConnection(dstFile)
                                }
                            }
                            dismiss()

                            m.title = name
                            file.name = name
                            file.path = file.path.replace("/$oldName", "/$name")
                            if (file.isDir) {
                                viewModel.breadcrumbs.find { b -> b.path == oldPath }?.let { b ->
                                    b.path = file.path
                                    b.name = name
                                    dialog.updateBreadcrumb()
                                }
                            }
                            m.notifyChange()
                            rv.bindingAdapter.checkedAll(false)
                        }
                    }.show()
                }
            }
            R.id.compress -> {
                rv.ensureSelect { items ->
                    dialog.lifecycleScope.launch {
                        DialogHelper.showLoading()
                        val m = items[0] as FileModel
                        val destFile = File(m.data.path + ".zip")
                        var destPath = destFile.path
                        if (destFile.exists()) {
                            destPath = destFile.newPath()
                        }
                        withIO {
                            ZipHelper.zip(items.map { m.data.path }, destPath)
                        }
                        DialogHelper.hideLoading()
                        rv.bindingAdapter.checkedAll(false)
                        binding.list.page.refresh()
                    }
                }
            }
            R.id.decompress -> {
                rv.ensureSelect { items ->
                    dialog.lifecycleScope.launch {
                        DialogHelper.showLoading()
                        val m = items[0] as FileModel
                        val destFile = File(m.data.path.removeSuffix(".zip"))
                        var destPath = destFile.path
                        if (destFile.exists()) {
                            destPath = destFile.newPath()
                        }
                        withIO {
                            ZipUtil.unpack(File(m.data.path), File(destPath))
                            MainApp.instance.scanFileByConnection(destPath)
                        }
                        DialogHelper.hideLoading()
                        rv.bindingAdapter.checkedAll(false)
                        binding.list.page.refresh()
                    }
                }
            }
            R.id.delete -> {
                rv.ensureSelect { items ->
                    DialogHelper.confirmToDelete {
                        dialog.lifecycleScope.launch {
                            val paths = items.map { it.data.id }.toSet()
                            DialogHelper.showLoading()
                            withIO {
                                paths.forEach {
                                    File(it).deleteRecursively()
                                }
                                MainApp.instance.scanFileByConnection(paths.toTypedArray())
                            }
                            DialogHelper.hideLoading()
                            rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.FILE, ActionType.DELETED, paths))
                        }
                    }
                }
            }
        }
    }
}
