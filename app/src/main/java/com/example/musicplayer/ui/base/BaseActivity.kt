package com.example.musicplayer.ui.base


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar


open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .init()
    }


    protected fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
