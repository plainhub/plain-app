package com.ismartcoding.plain.ui.helpers.mergeimages

import java.util.*

object CombineNineRect {
    fun generateCombineBitmapEntity(
        combineWidth: Int,
        combineHeight: Int,
        count: Int,
    ): List<CombineBitmapEntity> {
        val mCRC = generateColumnRowCountByCount(count)
        var mBitmapEntity: CombineBitmapEntity?
        val perBitmapWidth =
            (
                (combineWidth - 1 * 2 * mCRC.columns) /
                    mCRC.columns
            ).toFloat()
        val topDownDelta: Float = combineHeight - mCRC.rows * (perBitmapWidth + CombineBitmapEntity.devide * 2)
        val mList: MutableList<CombineBitmapEntity> = LinkedList<CombineBitmapEntity>()
        for (row in 0 until mCRC.rows) {
            for (column in 0 until mCRC.columns) {
                mBitmapEntity = CombineBitmapEntity()
                mBitmapEntity.y = 1 + topDownDelta / 2 + row * 2 + (
                    row
                        * perBitmapWidth
                )
                mBitmapEntity.x = 1 + column * 2 + column * perBitmapWidth
                mBitmapEntity.height = perBitmapWidth
                mBitmapEntity.width = mBitmapEntity.height
                mList.add(mBitmapEntity)
            }
        }
        when (count) {
            3 -> {
                mList.removeAt(0)
                modifyListWhenCountThree(mList)
            }
            5 -> {
                mList.removeAt(0)
                modifyListWhenCountFive(mList)
            }
            7 -> {
                mList.removeAt(0)
                mList.removeAt(0)
                modifyListWhenCountSeven(mList)
            }
            8 -> {
                mList.removeAt(0)
                modifyListWhenCountEight(mList)
            }
        }
        return mList
    }

    private fun modifyListWhenCountThree(list: List<CombineBitmapEntity>) {
        list[0].x = (list[1].x + list[2].x) / 2
    }

    private fun modifyListWhenCountFive(list: List<CombineBitmapEntity>) {
        list[0].x = (list[3].x + list[2].x) / 2
        list[1].x = (list[1].x + list[3].x) / 2
    }

    private fun modifyListWhenCountSeven(list: List<CombineBitmapEntity>) {
        list[0].x = (list[1].x + list[2].x + list[3].x) / 3
    }

    private fun modifyListWhenCountEight(list: List<CombineBitmapEntity>) {
        list[0].x = (list[2].x + list[3].x) / 2
        list[1].x = (list[3].x + list[4].x) / 2
    }

    private fun generateColumnRowCountByCount(count: Int): ColumnRowCount {
        return when (count) {
            2 -> ColumnRowCount(1, 2, count)
            3, 4 -> ColumnRowCount(2, 2, count)
            5, 6 -> ColumnRowCount(2, 3, count)
            7, 8, 9 -> ColumnRowCount(3, 3, count)
            else -> ColumnRowCount(1, 1, count)
        }
    }

    private class ColumnRowCount(var rows: Int, var columns: Int, var count: Int) {
        override fun toString(): String {
            return (
                "ColumnRowCount [rows=" + rows + ", columns=" + columns +
                    ", count=" + count + "]"
            )
        }
    }
}
