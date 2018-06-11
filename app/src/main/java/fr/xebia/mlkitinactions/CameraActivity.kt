package fr.xebia.mlkitinactions

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (null == savedInstanceState) {
            val fragment = Camera2BasicFragment.newInstance()
            fragment.arguments = intent.extras
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
        }
    }
}
