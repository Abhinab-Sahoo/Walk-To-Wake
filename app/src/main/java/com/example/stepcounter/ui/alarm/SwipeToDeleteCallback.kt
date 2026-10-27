package com.example.stepcounter.ui.alarm

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import com.example.stepcounter.R
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(
    private val context: Context,
    private val onDeleteSwiped: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!
    private val background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(ContextCompat.getColor(context, R.color.swipe_delete_background))
        cornerRadius = 16f * context.resources.displayMetrics.density
    }

    override fun onMove(
        p0: RecyclerView,
        p1: RecyclerView.ViewHolder,
        p2: RecyclerView.ViewHolder
    ): Boolean { return false }

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) {
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onDeleteSwiped(position)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        if (dX < 0) {
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            background.draw(c)

            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + icon.intrinsicHeight
            val iconRight = itemView.right - iconMargin
            val iconLeft = iconRight - icon.intrinsicWidth

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            icon.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}