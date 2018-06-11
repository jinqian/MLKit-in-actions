package fr.xebia.mlkitinactions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1000

        const val DEMO_MODE = "DEMO_MODE"

        const val DEMO_TEXT_RECOGNITION = 0
        const val DEMO_BARCODE_SCANING = 1
        const val DEMO_IMAGE_LABELLING = 2
        const val DEMO_LANDMARK_RECOGNITION = 3
        const val DEMO_CUSTOM_MODEL = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textRecognitionDemoBtn.setOnClickListener {
            // TODO
        }

        customModelDemoBtn.setOnClickListener {
            openDemo(DEMO_CUSTOM_MODEL)
        }
    }

    private fun openDemo(demoFlag: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {
                Toast.makeText(this, R.string.request_permission, Toast.LENGTH_LONG).show()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION)
            }
        } else {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra(DEMO_MODE, demoFlag)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startActivity(Intent(this, CameraActivity::class.java))
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
