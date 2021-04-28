/*POSE ESTIMATION MOBILE - Septian Puji
*
* Aplikasi Mobile Implementasi Pose Estimation - Skeleton Tracking sebagai pengukur bagian tubuh
*
* Last Update - 21-06-20

* */

package com.skripsisepti.virtualfitroom

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.theartofdev.edmodo.cropper.CropImage
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import kotlin.system.exitProcess

/**
 * 'Main' Activity class for the Camera app.
 */
class CameraActivity : AppCompatActivity() {

  private val mLoaderCallback = object : BaseLoaderCallback(this) {
    override fun onManagerConnected(status: Int) {
      when (status) {
        LoaderCallbackInterface.SUCCESS -> isOpenCVInit = true
        LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION -> {
        }
        LoaderCallbackInterface.INIT_FAILED -> {
        }
        LoaderCallbackInterface.INSTALL_CANCELED -> {
        }
        LoaderCallbackInterface.MARKET_ERROR -> {
        }
        else -> {
          super.onManagerConnected(status)
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (null == savedInstanceState) {
      fragmentManager
              .beginTransaction()
              .replace(R.id.container, Camera2BasicFragment.newInstance())
              .commit()
    }
    setContentView(R.layout.activity_camera)
    setSupportActionBar(findViewById(R.id.toolbar))
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    if (!OpenCVLoader.initDebug()) {
      OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
    } else {
      mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }
  }

  /**===========Pengatur Tombol Kembali (1 kali reset - 2 kali keluar========*/
  private var tbl = 2
  override fun onBackPressed() {
    Toast.makeText(this, "tekan 2 kali untuk keluar", Toast.LENGTH_SHORT).show()
    tbl -= 1
    if (tbl == 1){
      Camera2BasicFragment.selesai()
      Handler().postDelayed({
        tbl +=1
      }, 750)
    }
    else if (tbl == 0){
      finish()
      exitProcess(0)
    }
  }

  /**=============================MODE PILIH GAMBAR==========================*/
  private fun pilihGambar(){
    CropImage.activity()
            .setMinCropResultSize(300,400)
            .setMaxCropResultSize(1080,1440)
            .setAspectRatio(3,4)
            .setFixAspectRatio(true)
            .start(this)
  }

  /**===========================OPTION MENU================================**/
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_option, menu)
    super.onCreateOptionsMenu(menu)
    return true
  }
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val i: Intent
    return when (item.itemId) {
      R.id.galeri -> {
        pilihGambar()
        true
      }
      R.id.tabel_baju -> {
        i = Intent(this, TabelList::class.java)
        finish()
        startActivity(i)
        true
      }
      R.id.about -> {
        AlertDialog.Builder(this)
                .setTitle("Tentang Aplikasi")
                .setMessage("Virtual Fitting Room \nSeptian Puji Saputro \n11160910000038 \nSebagai Implementasi Skripsi \nAPLIKASI VIRTUAL FITTING ROOM MENGGUNAKAN METODE SKELETON TRACKING DAN EUCLIDEAN DISTANCE")
                .show()
        true
      }
      R.id.info -> {
        if(Camera2BasicFragment.layoutDev!!.visibility==View.VISIBLE){
          Camera2BasicFragment.layoutDev!!.visibility=View.GONE
          Camera2BasicFragment.layoutDataUtama!!.visibility=View.VISIBLE
        }
        else{
          Camera2BasicFragment.layoutDev!!.visibility=View.VISIBLE
        }
        true
      }
      R.id.quit -> {
        finish()
        exitProcess(0)
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  /**Hasil dari Ambil Gambar
   * Pilih gambar,
   * crop aspect rasio,
   * kirim bitmap ke Camera2BasicFragment,
   * proses DrawView, lalu ditampilkan */
  @SuppressLint("LogNotTimber")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
      val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
      if(resultCode == Activity.RESULT_OK){
        val uriToBitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, result.uri)
        Camera2BasicFragment.imagefile = uriToBitmap
        Camera2BasicFragment.viewImage!!.visibility = View.VISIBLE
        Camera2BasicFragment.ambilFoto!!.visibility = View.INVISIBLE
        Camera2BasicFragment.ambilFoto!!.isClickable = false
        Camera2BasicFragment.tombolSimpan!!.visibility = View.VISIBLE
        Camera2BasicFragment.tombolSimpan!!.isClickable = true
        Camera2BasicFragment.viewImage!!.setImageBitmap(uriToBitmap)
      }
      else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
        Toast.makeText(this, "Possible error is"+ result.error.toString(), Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    init {System.loadLibrary("opencv_java3")}
    @JvmStatic
    var isOpenCVInit = false
  }
}
