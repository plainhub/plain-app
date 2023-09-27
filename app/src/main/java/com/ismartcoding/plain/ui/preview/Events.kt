package com.ismartcoding.plain.ui.preview

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ViewerShowCastListEvent(val uri: String)

class ViewerDismissEvent

class ViewerInitEvent(val position: Int, val viewHolder: RecyclerView.ViewHolder)

class ViewerDragEvent(val view: View, val fraction: Float)

class ViewerRestoreEvent(val view: View, val fraction: Float)

class ViewerReleaseEvent(val view: View)
