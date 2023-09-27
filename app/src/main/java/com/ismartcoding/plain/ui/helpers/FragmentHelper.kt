package com.ismartcoding.plain.ui.helpers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object FragmentHelper {
    fun <VB : ViewBinding> createBinding(
        dialog: Fragment,
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB {
        var parametrizedType: ParameterizedType? = null
        var genericSuperClass: Type? = dialog.javaClass.genericSuperclass
        while (parametrizedType == null) {
            if (genericSuperClass is ParameterizedType) {
                parametrizedType = genericSuperClass
            } else {
                genericSuperClass = (genericSuperClass as Class<*>).genericSuperclass
            }
        }

        val vbType = parametrizedType.actualTypeArguments[0]
        val vbClass = vbType as Class<VB>
        return vbClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java,
        ).invoke(null, inflater, container, false) as VB
    }
}
