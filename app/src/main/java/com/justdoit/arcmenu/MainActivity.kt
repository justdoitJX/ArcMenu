package com.justdoit.arcmenu

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawables = intArrayOf(R.mipmap.sheet_main_activity_start_live_icon,
            R.mipmap.sheet_main_activity_start_live_icon,
            R.mipmap.sheet_main_activity_start_live_icon)
        val itemCount = drawables.size
        for (i in 0 until itemCount) {
            val item = ImageView(this).apply {
                setImageResource(drawables[i])
            }
            arcMenu.addItem(item) {
                when (i) {
                    0 -> Toast.makeText(this, "0", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "1", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "2", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //布局复杂的时候动画会有问题，这样可以解决
        arcMenu.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                arcMenu.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val layoutParams = RelativeLayout.LayoutParams(resources.getDimension(R.dimen.dp750_650).toInt(), resources.getDimension(R.dimen.dp750_650).toInt())
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                arcMenu.layoutParams = layoutParams
            }
        })

        arcMenu.onControl = {
            Toast.makeText(this, "click", Toast.LENGTH_SHORT).show()
        }
    }
}
