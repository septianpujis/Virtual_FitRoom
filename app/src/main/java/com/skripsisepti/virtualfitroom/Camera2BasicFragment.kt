/**POSE ESTIMATION MOBILE - Septian Puji
*
* Aplikasi Mobile Implementasi Pose Estimation - Skeleton Tracking sebagai pengukur bagian tubuh
*
* halaman ini setara sama halaman main
*
* Last Update - 21-06-20
*
* task:
 * mulai uji coba
 *
*/

package com.skripsisepti.virtualfitroom

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.v13.app.FragmentCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.round

/**
 * Basic fragments for the Camera.
 */
@Suppress("NAME_SHADOWING")
class Camera2BasicFragment : Fragment(), FragmentCompat.OnRequestPermissionsResultCallback {

  private val lock = Any()
  private var runClassifier = false
  private var checkedPermissions = false
  private var textView: TextView? = null
  private var textureView: AutoFitTextureView? = null
  private var layoutFrame: AutoFitFrameLayout? = null
  private var drawView: DrawView? = null
  private var classifier: ImageClassifier? = null

  private var fabShow: View? = null

  private var genderSwitcher: Switch? = null

  //viewGroup detail 2 ada di bawah (companion object)
  private var panjangAEU: TextView? = null
  private var lebarABEU: TextView? = null
  private var panjangCEU: TextView? = null
  private var panjangDEU: TextView? = null
  private var panjangEEU: TextView? = null
  private var panjangAcm: TextView? = null
  private var lebarABcm: TextView? = null
  private var panjangCcm: TextView? = null
  private var panjangDcm: TextView? = null
  private var panjangEcm: TextView? = null


  private var teksLebarBadan: TextView? = null
  private var teksPanjangBadan: TextView? = null
  private var teksRekPolo: TextView? = null
  private var teksRekKemeja: TextView? = null
  private var valLebarBadan: TextView? = null
  private var valPanjangBadan: TextView? = null
  private var valRekPolo: TextView? = null
  private var valRekKemeja: TextView? = null

  private var leherDev: TextView? = null
  private var rshouDev: TextView? = null
  private var lshouDev: TextView? = null
  private var rhipDev: TextView? = null
  private var lhipDev: TextView? = null
  private var padanDev: TextView? = null
  private var ledanDev: TextView? = null

  /** [TextureView.SurfaceTextureListener] handles several lifecycle events on a [ ].*/
  private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

    override fun onSurfaceTextureAvailable(
      texture: SurfaceTexture,
      width: Int,
      height: Int
    ) {
      openCamera(width, height)
    }

    override fun onSurfaceTextureSizeChanged(
      texture: SurfaceTexture,
      width: Int,
      height: Int
    ) {
      configureTransform(width, height)
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
      return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
  }

  /** ID of the current [CameraDevice].*/
  private var cameraId: String? = null

  /**A [CameraCaptureSession] for camera preview.*/
  private var captureSession: CameraCaptureSession? = null

  /**A reference to the opened [CameraDevice].*/
  private var cameraDevice: CameraDevice? = null

  /**The [android.util.Size] of camera preview.*/
  private var previewSize: Size? = null

  /**[CameraDevice.StateCallback] is called when [CameraDevice] changes its state.*/
  private val stateCallback = object : CameraDevice.StateCallback() {

    override fun onOpened(currentCameraDevice: CameraDevice) {
      // This method is called when the camera is opened.  We start camera preview here.
      cameraOpenCloseLock.release()
      cameraDevice = currentCameraDevice
      createCameraPreviewSession()
    }

    override fun onDisconnected(currentCameraDevice: CameraDevice) {
      cameraOpenCloseLock.release()
      currentCameraDevice.close()
      cameraDevice = null
    }

    override fun onError(
      currentCameraDevice: CameraDevice,
      error: Int
    ) {
      cameraOpenCloseLock.release()
      currentCameraDevice.close()
      cameraDevice = null
      val activity = activity
      activity?.finish()
    }
  }

  /**An additional thread for running tasks that shouldn't block the UI.*/
  private var backgroundThread: HandlerThread? = null

  /**A [Handler] for running tasks in the background.*/
  private var backgroundHandler: Handler? = null

  /**An [ImageReader] that handles image capture.*/
  private var imageReader: ImageReader? = null

  /**[CaptureRequest.Builder] for the camera preview*/
  private var previewRequestBuilder: CaptureRequest.Builder? = null

  /**[CaptureRequest] generated by [.previewRequestBuilder*/
  private var previewRequest: CaptureRequest? = null

  /**A [Semaphore] to prevent the app from exiting before closing the camera.*/
  private val cameraOpenCloseLock = Semaphore(1)

  /**A [CameraCaptureSession.CaptureCallback] that handles events related to capture.*/
  private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureProgressed(
      session: CameraCaptureSession,
      request: CaptureRequest,
      partialResult: CaptureResult
    ) {
    }

    override fun onCaptureCompleted(
      session: CameraCaptureSession,
      request: CaptureRequest,
      result: TotalCaptureResult
    ) {
    }
  }

  private val requiredPermissions: Array<String> get() {
      val activity = activity
      return try {
        val info = activity
            .packageManager
            .getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        val ps = info.requestedPermissions
        if (ps != null && ps.isNotEmpty()) {
          ps
        } else {
          arrayOf()
        }
      } catch (e: Exception) {
        arrayOf()
      }

    }

  /**Takes photos and classify them periodically.*/
  private val periodicClassify = object : Runnable {
    override fun run() {
      synchronized(lock) {
        if (runClassifier) {
          classifyInput()
          cariPanjang()
        }
      }
      backgroundHandler!!.post(this)
    }
  }

  private fun showToast(text: String) {
    val activity = activity
    activity?.runOnUiThread {
      textView!!.text = text
      drawView!!.invalidate()
    }
  }

  /**Perhitungan jarak antar titik*/
  @SuppressLint("SetTextI18n")
  private fun cariPanjang(){
    val activity = activity

    /**IMPORT INPUTAN (TITIK KOORDINAT), DEKLARASI TITIK YANG DIBUTUHKAN*/
    val posisiX = drawView!!.getPosX()
    val posisiY = drawView!!.getPosY()
    val neckX = ceil(posisiX[1]);val neckY = ceil(posisiY[1])
    val lShoulderX = ceil(posisiX[2]); val lShoulderY = ceil(posisiY[2])
    val rShoulderX = ceil(posisiX[5]); val rShoulderY = ceil(posisiY[5])
    val lHipX = ceil(posisiX[8]); val lHipY = ceil(posisiY[8])
    val rHipX = ceil(posisiX[11]); val rHipY = ceil(posisiY[11])

    val skala = 8.2

    /**PERHITUNGAN Euclidean Dist.*/
    val pjgnApx = sqrt((neckX - lShoulderX).pow(2) + (neckY - lShoulderY).pow(2))
    val pjgnBpx = sqrt((neckX - rShoulderX).pow(2) + (neckY - rShoulderY).pow(2))
    val pjgnCpx = sqrt((neckX - lHipX).pow(2) + (neckY - lHipY).pow(2))
    val pjgnDpx = sqrt((lHipX - rHipX).pow(2) + (lHipY - rHipY).pow(2))

    val lebarBadanpx = pjgnApx + pjgnBpx
    val panjangBadanpx = sqrt(pjgnCpx.pow(2) - (pjgnDpx/2).pow(2))

    val lebarBadanCm = (lebarBadanpx)/skala
    val panjangBadanCm = (panjangBadanpx)/skala

    activity?.runOnUiThread {
      /** PERINTAH TAMPIL LAYOUT DETAIL 2 (Px ke Cm) */
      panjangAEU!!.text = round(pjgnApx).toString()
      lebarABEU!!.text = round(lebarBadanpx).toString()
      panjangCEU!!.text = round(pjgnCpx).toString()
      panjangEEU!!.text = round(panjangBadanpx).toString()
      panjangAcm!!.text = round((pjgnApx)/ skala).toString()
      lebarABcm!!.text = round(lebarBadanCm).toString()
      panjangCcm!!.text = round((pjgnCpx)/ skala).toString()
      panjangEcm!!.text = round(panjangBadanCm).toString()

      /** PERINTAH TAMPIL LAYOUT DATA UTAMA */
      valLebarBadan!!.text = round(lebarBadanCm).toString() + " cm"
      valPanjangBadan!!.text = round(panjangBadanCm).toString() + " cm"
      rekomendasi(genderSwitcher!!.isChecked,"polo", lebarBadanCm, panjangBadanCm)
      rekomendasi(genderSwitcher!!.isChecked,"kemeja", lebarBadanCm, panjangBadanCm)

      /** PERINTAH TAMPIL LAYOUT DEV (BAB 5) */
      leherDev!!.text = "($neckX, $neckY)"
      rshouDev!!.text = "($rShoulderX, $rShoulderY)"
      lshouDev!!.text = "($lShoulderX, $lShoulderY)"
      rhipDev!!.text = "($rHipX, $rHipY)"
      lhipDev!!.text = "($lHipX, $lHipY)"
      padanDev!!.text = round(panjangBadanpx).toString()
      ledanDev!!.text = round(lebarBadanpx).toString()

      drawView!!.invalidate()
    }
  }

  private fun rekomendasi(mode: Boolean, jenis: String, lebar: Double, panjang: Double){
    when (mode) {
        false /*Male*/ -> {
            when {
                jenis=="polo" && (panjang in 67F..69F || lebar in 48F..50F) -> valRekPolo!!.setText(R.string.size_s)
                jenis=="polo" && (panjang in 69F..71F || lebar in 50F..53F) -> valRekPolo!!.setText(R.string.size_m)
                jenis=="polo" && (panjang in 71F..73F || lebar in 53F..55F) -> valRekPolo!!.setText(R.string.size_l)
                jenis=="polo" && (panjang in 73F..75F || lebar in 56F..58F) -> valRekPolo!!.setText(R.string.size_xl)
                jenis=="polo" && (panjang in 75F..77F || lebar in 58F..60F) -> valRekPolo!!.setText(R.string.size_xxl)
                jenis=="polo" && (panjang in 77F..79F || lebar in 61F..63F) -> valRekPolo!!.setText(R.string.size_xxxl)
                jenis=="kemeja" && (panjang in 67F..69F || lebar in 49F..51F) -> valRekKemeja!!.setText(R.string.size_s)
                jenis=="kemeja" && (panjang in 70F..72F || lebar in 52F..55F) -> valRekKemeja!!.setText(R.string.size_m)
                jenis=="kemeja" && (panjang in 73F..75F || lebar in 55F..57F) -> valRekKemeja!!.setText(R.string.size_l)
                jenis=="kemeja" && (panjang in 75F..77F || lebar in 57F..59F) -> valRekKemeja!!.setText(R.string.size_xl)
                jenis=="kemeja" && (panjang in 77F..79F || lebar in 59F..61F) -> valRekKemeja!!.setText(R.string.size_xxl)
                jenis=="kemeja" && (panjang in 78F..80F || lebar in 61F..63F) -> valRekKemeja!!.setText(R.string.size_xxxl)
                else -> {
                    valRekPolo!!.setText(R.string.size_non)
                    valRekKemeja!!.setText(R.string.size_non)
                }
            }
        }
        true /*Female*/ -> {
            when {
                jenis=="polo" && (panjang in 63F..65F || lebar in 43F..45F) -> valRekPolo!!.setText(R.string.size_s)
                jenis=="polo" && (panjang in 66F..69F || lebar in 45F..47F) -> valRekPolo!!.setText(R.string.size_m)
                jenis=="polo" && (panjang in 69F..71F || lebar in 47F..49F) -> valRekPolo!!.setText(R.string.size_l)
                jenis=="polo" && (panjang in 72F..73F || lebar in 49F..51F) -> valRekPolo!!.setText(R.string.size_xl)
                jenis=="polo" && (panjang in 74F..76F || lebar in 51F..53F) -> valRekPolo!!.setText(R.string.size_xxl)
                jenis=="polo" && (panjang in 76F..78F || lebar in 53F..55F) -> valRekPolo!!.setText(R.string.size_xxxl)
                jenis=="kemeja" && (panjang in 63F..65F || lebar in 45F..47F) -> valRekKemeja!!.setText(R.string.size_s)
                jenis=="kemeja" && (panjang in 65F..67F || lebar in 47F..49F) -> valRekKemeja!!.setText(R.string.size_m)
                jenis=="kemeja" && (panjang in 67F..69F || lebar in 49F..51F) -> valRekKemeja!!.setText(R.string.size_l)
                jenis=="kemeja" && (panjang in 70F..72F || lebar in 51F..53F) -> valRekKemeja!!.setText(R.string.size_xl)
                jenis=="kemeja" && (panjang in 72F..74F || lebar in 53F..55F) -> valRekKemeja!!.setText(R.string.size_xxl)
                jenis=="kemeja" && (panjang in 74F..76F || lebar in 55F..57F) -> valRekKemeja!!.setText(R.string.size_xxxl)
                else -> {
                    valRekPolo!!.setText(R.string.size_non)
                    valRekKemeja!!.setText(R.string.size_non)
                }
            }

        }
    }
  }

  /**Layout the preview and buttons.*/
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_camera2_basic, container, false)
  }

  /** Pengendali button, layout, dan view (onCreate).*/
  @SuppressLint("SimpleDateFormat", "LogNotTimber")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    textureView = view.findViewById(R.id.texture)
    textView = view.findViewById(R.id.text)
    layoutFrame = view.findViewById(R.id.layout_frame)
    drawView = view.findViewById(R.id.drawview)
    viewImage = view.findViewById(R.id.image_view)

    /**KOLEKSI variabel di layout dev (dibutuhkan di bab 5)*/
    layoutDev = view.findViewById(R.id.layout_dev)
    leherDev = view.findViewById(R.id.neck)
    rshouDev = view.findViewById(R.id.rsho)
    lshouDev = view.findViewById(R.id.lsho)
    rhipDev = view.findViewById(R.id.rhip)
    lhipDev = view.findViewById(R.id.lhip)
    padanDev = view.findViewById(R.id.padan)
    ledanDev = view.findViewById(R.id.ledan)

    /**KOLEKSI variabel di layout detail 2 (Panjang awal (eu + cm)*/
    layoutDetail2 = view.findViewById(R.id.layout_detail_2)

    panjangAEU = view.findViewById(R.id.pjng_a_eu)
    lebarABEU = view.findViewById(R.id.lbr_bdn_eu)
    panjangCEU = view.findViewById(R.id.pjng_c_eu)
    panjangDEU = view.findViewById(R.id.pjng_d_eu)
    panjangEEU = view.findViewById(R.id.pjng_bdn_eu)

    panjangAcm = view.findViewById(R.id.pjng_a_cm)
    lebarABcm = view.findViewById(R.id.lbr_bdn_cm)
    panjangCcm = view.findViewById(R.id.pjng_c_cm)
    panjangDcm = view.findViewById(R.id.pjng_d_cm)
    panjangEcm = view.findViewById(R.id.pjng_bdn_cm)

    /**KOLEKSI variabel di layout detail 3 (genderswitch + rek value)*/
    layoutDataUtama = view.findViewById(R.id.layout_data_utama)
    teksLebarBadan = view.findViewById(R.id.teks_lbr_badan)
    teksPanjangBadan = view.findViewById(R.id.teks_pjg_badan)
    teksRekPolo = view.findViewById(R.id.teks_rek_polo)
    teksRekKemeja = view.findViewById(R.id.teks_rek_kemeja)
    valLebarBadan = view.findViewById(R.id.val_lbr_badan)
    valPanjangBadan = view.findViewById(R.id.val_pjg_badan)
    valRekPolo = view.findViewById(R.id.val_rek_polo)
    valRekKemeja = view.findViewById(R.id.val_rek_kemeja)

    fabShow = view.findViewById(R.id.btn_show)
    fabShow!!.setOnClickListener{ view ->
      if (layoutDataUtama!!.visibility == View.VISIBLE){
        Snackbar.make(view, "Menyembunyikan Detail masukan", Snackbar.LENGTH_SHORT)
                .setAction("Aksi", null).show()
        layoutDataUtama!!.visibility = View.GONE
      }else{
        Snackbar.make(view, "Menampilkan Detail masukan", Snackbar.LENGTH_SHORT)
                .setAction("Aksi", null).show()
        layoutDataUtama!!.visibility = View.VISIBLE
        layoutDetail2!!.visibility = View.GONE
        layoutDev!!.visibility = View.GONE
      }
    }

    ambilFoto = view.findViewById(R.id.btn_shoot)
    ambilFoto!!.setOnClickListener{
      imagefile = textureView!!.bitmap

      viewImage!!.setImageBitmap(imagefile)
      viewImage!!.visibility = View.VISIBLE
      ambilFoto!!.visibility = View.INVISIBLE
      ambilFoto!!.isClickable = false
      tombolSimpan!!.isClickable = true
      tombolSimpan!!.visibility = View.VISIBLE
    }

    tombolSimpan = view.findViewById(R.id.btn_simpan)
    tombolSimpan!!.setOnClickListener{
      val sdf = SimpleDateFormat("dd-MM_HH-mm-ss")
      val currentDateandTime = sdf.format(Date())
      val fileName = Environment.getExternalStorageDirectory().path +
              "/Download/Hasil Kamera " + currentDateandTime + ".PNG"
      try {
        val canvas = Canvas(imagefile!!)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(255,255,255)
        paint.textSize = 40F
        canvas.drawText("Lebar : " + valLebarBadan!!.text as String, 40F, 40F, paint)
        canvas.drawText("Panjang : " + valPanjangBadan!!.text as String, 40F, 90F, paint)
        canvas.drawText("Rek. Polo : " + valRekPolo!!.text as String, 40F, 140F, paint)
        canvas.drawText("Rek. Kemeja : " + valRekKemeja!!.text as String, 40F, 190F, paint)

        imagefile!!.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(fileName))

        Snackbar.make(view, "Gambar $fileName Berhasil Disimpan", Snackbar.LENGTH_SHORT)
                .setAction("Aksi", null).show()
      } catch (e: IOException){
        e.printStackTrace()
      }
      tombolSimpan!!.isClickable = false
      tombolSimpan!!.visibility = View.INVISIBLE
    }



    genderSwitcher = view.findViewById(R.id.mSwitch)
    genderSwitcher!!.setOnCheckedChangeListener { _, isChecked ->
      if(isChecked){
        genderSwitcher!!.setThumbResource(R.drawable.ic_female_symbol)

        teksLebarBadan!!.setTextColor(resources.getColor(R.color.color_female, null))
        teksPanjangBadan!!.setTextColor(resources.getColor(R.color.color_female, null))
        teksRekPolo!!.setTextColor(resources.getColor(R.color.color_female, null))
        teksRekKemeja!!.setTextColor(resources.getColor(R.color.color_female, null))
        valLebarBadan!!.setTextColor(resources.getColor(R.color.color_female, null))
        valPanjangBadan!!.setTextColor(resources.getColor(R.color.color_female, null))
        valRekPolo!!.setTextColor(resources.getColor(R.color.color_female, null))
        valRekKemeja!!.setTextColor(resources.getColor(R.color.color_female, null))

        Snackbar.make(view, "Rekomendasi ukuran dipindah ke FEMALE", Snackbar.LENGTH_SHORT)
                .setAction("Aksi", null).show()
      }else{
        genderSwitcher!!.setThumbResource(R.drawable.ic_male_symbol_2239)

        teksLebarBadan!!.setTextColor(resources.getColor(R.color.color_male, null))
        teksPanjangBadan!!.setTextColor(resources.getColor(R.color.color_male, null))
        teksRekPolo!!.setTextColor(resources.getColor(R.color.color_male, null))
        teksRekKemeja!!.setTextColor(resources.getColor(R.color.color_male, null))
        valLebarBadan!!.setTextColor(resources.getColor(R.color.color_male, null))
        valPanjangBadan!!.setTextColor(resources.getColor(R.color.color_male, null))
        valRekPolo!!.setTextColor(resources.getColor(R.color.color_male, null))
        valRekKemeja!!.setTextColor(resources.getColor(R.color.color_male, null))


        Snackbar.make(view, "Rekomendasi ukuran dipindah ke MALE", Snackbar.LENGTH_SHORT)
                .setAction("Aksi", null).show()
      }
    }
  }

  /**MEMUAT MODEL DAN LABEL (Image Clasifier)*/
  @SuppressLint("LogNotTimber")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    try {
      classifier = ImageClassifierFloatInception.create(activity)
      if (drawView != null)
        drawView!!.setImgSize(classifier!!.imageSizeX, classifier!!.imageSizeY)
    } catch (e: IOException) {
      Log.e(TAG, "Failed to initialize an image classifier.", e)
    }

    startBackgroundThread()
  }

  override fun onResume() {
    super.onResume()
    startBackgroundThread()
    if (textureView!!.isAvailable)
      openCamera(textureView!!.width, textureView!!.height)
    else
      textureView!!.surfaceTextureListener = surfaceTextureListener
  }
  override fun onPause() {
    closeCamera()
    stopBackgroundThread()
    super.onPause()
  }
  override fun onDestroy() {
    classifier!!.close()
    super.onDestroy()
  }

  /**Sets up member variables related to camera.
   *
   * @param width  The width of available size for camera preview
   * @param height The height of available size for camera preview
   */
  @SuppressLint("LogNotTimber")
  private fun setUpCameraOutputs(width: Int, height: Int) {
    val activity = activity
    val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      for (cameraId in manager.cameraIdList) {
        val characteristics = manager.getCameraCharacteristics(cameraId)

        // We don't use a front facing camera in this sample.
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue
        }

        val map =
          characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

        // // For still image captures, we use the largest available size.
        val largest = Collections.max(
            listOf(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea()
        )
        imageReader = ImageReader.newInstance(
            largest.width, largest.height, ImageFormat.JPEG, /*maxImages*/ 2
        )

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        val displayRotation = activity.windowManager.defaultDisplay.rotation

        /* Orientation of the camera sensor */
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        var swappedDimensions = false
        when (displayRotation) {
          Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) {
            swappedDimensions = true
          }
          Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) {
            swappedDimensions = true
          }
          else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
        }

        val displaySize = Point()
        activity.windowManager.defaultDisplay.getSize(displaySize)
        var rotatedPreviewWidth = width
        var rotatedPreviewHeight = height
        var maxPreviewWidth = displaySize.x
        var maxPreviewHeight = displaySize.y

        if (swappedDimensions) {
          rotatedPreviewWidth = height
          rotatedPreviewHeight = width
          maxPreviewWidth = displaySize.y
          maxPreviewHeight = displaySize.x
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
          maxPreviewWidth = MAX_PREVIEW_WIDTH
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
          maxPreviewHeight = MAX_PREVIEW_HEIGHT
        }

        previewSize = chooseOptimalSize(
            map.getOutputSizes(SurfaceTexture::class.java),
            rotatedPreviewWidth,
            rotatedPreviewHeight,
            maxPreviewWidth,
            maxPreviewHeight,
            largest
        )

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
          layoutFrame!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
          textureView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
          drawView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
        } else {
          layoutFrame!!.setAspectRatio(previewSize!!.height, previewSize!!.width)
          textureView!!.setAspectRatio(previewSize!!.height, previewSize!!.width)
          drawView!!.setAspectRatio(previewSize!!.height, previewSize!!.width)
        }

        this.cameraId = cameraId
        return
      }
    } catch (e: CameraAccessException) {
      Log.e(TAG, "Failed to access Camera", e)
    } catch (e: NullPointerException) {
      // Currently an NPE is thrown when the Camera2API is used but not supported on the
      // device this code runs.
      ErrorDialog.newInstance(getString(R.string.camera_error))
          .show(childFragmentManager, FRAGMENT_DIALOG)
    }

  }

  /**
   * Opens the camera specified by [Camera2BasicFragment.cameraId].
   */
  @SuppressLint("MissingPermission", "LogNotTimber")
  private fun openCamera(width: Int, height: Int) {
    if (!checkedPermissions && !allPermissionsGranted()) {
      FragmentCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
      return
    } else {
      checkedPermissions = true
    }
    setUpCameraOutputs(width, height)
    configureTransform(width, height)
    val activity = activity
    val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw RuntimeException("Time out waiting to lock camera opening.")
      }
      manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
    } catch (e: CameraAccessException) {
      Log.e(TAG, "Failed to open Camera", e)
    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera opening.", e)
    }
  }

  private fun allPermissionsGranted(): Boolean {
    for (permission in requiredPermissions) {
      if (ContextCompat.checkSelfPermission(
              activity, permission
          ) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }
    return true
  }
  override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String>,grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  /**Closes the current [CameraDevice]*/
  private fun closeCamera() {
    try {
      cameraOpenCloseLock.acquire()
      if (null != captureSession) {
        captureSession!!.close()
        captureSession = null
      }
      if (null != cameraDevice) {
        cameraDevice!!.close()
        cameraDevice = null
      }
      if (null != imageReader) {
        imageReader!!.close()
        imageReader = null
      }
    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera closing.", e)
    } finally {
      cameraOpenCloseLock.release()
    }
  }

  /**Starts a background thread and its [Handler].*/
  private fun startBackgroundThread() {
    backgroundThread = HandlerThread(HANDLE_THREAD_NAME)
    backgroundThread!!.start()
    backgroundHandler = Handler(backgroundThread!!.looper)
    synchronized(lock) {
      runClassifier = true
    }
    backgroundHandler!!.post(periodicClassify)
  }

  /**Stops the background thread and its [Handler].*/
  @SuppressLint("LogNotTimber")
  private fun stopBackgroundThread() {
    backgroundThread!!.quitSafely()
    try {
      backgroundThread!!.join()
      backgroundThread = null
      backgroundHandler = null
      synchronized(lock) {
        runClassifier = false
      }
    } catch (e: InterruptedException) {
      Log.e(TAG, "Interrupted when stopping background thread", e)
    }

  }

  /** Creates a new [CameraCaptureSession] for camera preview.
   *
   * INTI TextureView Tuh DISINIIIII*/
  @SuppressLint("LogNotTimber")
  private fun createCameraPreviewSession() {
    try {
      val texture = textureView!!.surfaceTexture!!

      // We configure the size of default buffer to be the size of camera preview we want.
      texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

      // This is the output Surface we need to start preview.
      val surface = Surface(texture)

      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
      previewRequestBuilder!!.addTarget(surface)

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice!!.createCaptureSession(
          listOf(surface),
          object : CameraCaptureSession.StateCallback() {

            @SuppressLint("LogNotTimber")
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
              // The camera is already closed
              if (null == cameraDevice) {
                return
              }

              // When the session is ready, we start displaying the preview.
              captureSession = cameraCaptureSession
              try {
                // Auto focus should be continuous for camera preview.
                previewRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )

                // Finally, we start displaying the camera preview.
                previewRequest = previewRequestBuilder!!.build()
                captureSession!!.setRepeatingRequest(
                    previewRequest!!, captureCallback, backgroundHandler
                )
              } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to set up config to capture Camera", e)
              }
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
              showToast("Failed")
            }
          }, null
      )
    } catch (e: CameraAccessException) {
      Log.e(TAG, "Failed to preview Camera", e)
    }
  }

  /**
   * Configures the necessary [android.graphics.Matrix] transformation to `textureView`. This
   * method should be called after the camera preview size is determined in setUpCameraOutputs and
   * also the size of `textureView` is fixed.
   *
   * @param viewWidth  The width of `textureView`
   * @param viewHeight The height of `textureView`
   */
  private fun configureTransform(viewWidth: Int,viewHeight: Int) {
    val activity = activity
    if (null == textureView || null == previewSize || null == activity) {
      return
    }
    val rotation = activity.windowManager.defaultDisplay.rotation
    val matrix = Matrix()
    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
    val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
    val centerX = viewRect.centerX()
    val centerY = viewRect.centerY()
    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
      matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
      val scale = (viewHeight.toFloat() / previewSize!!.height).coerceAtLeast(viewWidth.toFloat() / previewSize!!.width)
      matrix.postScale(scale, scale, centerX, centerY)
      matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
    } else if (Surface.ROTATION_180 == rotation) {
      matrix.postRotate(180f, centerX, centerY)
    }
    textureView!!.setTransform(matrix)
  }

  /**Classifies a frame from the preview stream.*/
  @SuppressLint("LogNotTimber")
  private fun classifyInput() {
    if (classifier == null || activity == null || cameraDevice == null) {
      showToast("Invalid (Tidak Terdeteksi).")
      return
    }
    val bitmap: Bitmap = if(imagefile == null){
      textureView!!.getBitmap(classifier!!.imageSizeX, classifier!!.imageSizeY)
    }else{
      Bitmap.createScaledBitmap(imagefile!!, classifier!!.imageSizeX, classifier!!.imageSizeY, false)
    }
    classifier!!.classifyFrame(bitmap)                                                           //PERINTAH MEMPROSES BITMAP KE HEATMAP
    bitmap.recycle()
    drawView!!.setDrawPoint(classifier!!.mPrintPointArray!!, 0.5f)                         //PERINTAH MENGGAMBAR (DRAWVIEW) dari hasil classifyFrame (mPrintPointArray)

  }

  /** Compares two `Size`s based on their areas.*/
  private class CompareSizesByArea : Comparator<Size> {
    override fun compare(
      lhs: Size,
      rhs: Size
    ): Int {
      // We cast here to ensure the multiplications won't overflow
      return java.lang.Long.signum(
          lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
      )
    }
  }

  /** Shows an error message dialog.*/
  class ErrorDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
      val activity = activity
      return AlertDialog.Builder(activity)
          .setMessage(arguments.getString(ARG_MESSAGE))
          .setPositiveButton(
              android.R.string.ok
          ) { _, _ -> activity.finish() }
          .create()
    }

    companion object {

      private const val ARG_MESSAGE = "message"

      fun newInstance(message: String): ErrorDialog {
        val dialog = ErrorDialog()
        val args = Bundle()
        args.putString(ARG_MESSAGE, message)
        dialog.arguments = args
        return dialog
      }
    }
  }

  companion object {
    var viewImage: ImageView? = null
    var imagefile: Bitmap? = null
    var ambilFoto: View? = null
    var tombolSimpan: View? = null
    var layoutDetail2: ViewGroup? = null
    var layoutDev: ViewGroup? = null
    var layoutDataUtama: ViewGroup? = null

    private const val TAG = "Virtual Fitting Room"
    private const val FRAGMENT_DIALOG = "dialog"
    private const val HANDLE_THREAD_NAME = "CameraBackground"
    private const val PERMISSIONS_REQUEST_CODE = 1

    /**Max preview width that is guaranteed by Camera2 API*/
    private const val MAX_PREVIEW_WIDTH = 1920
    /**Max preview height that is guaranteed by Camera2 API*/
    private const val MAX_PREVIEW_HEIGHT = 1080
    /**
     * Resizes image.
     *
     *
     * Attempting to use too large a preview size could  exceed the camera bus' bandwidth limitation,
     * resulting in gorgeous previews but the storage of garbage capture data.
     *
     *
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that is
     * at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size, and
     * whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    @SuppressLint("LogNotTimber")
    private fun chooseOptimalSize(
      choices: Array<Size>,
      textureViewWidth: Int,
      textureViewHeight: Int,
      maxWidth: Int,
      maxHeight: Int,
      aspectRatio: Size
    ): Size {

      // Collect the supported resolutions that are at least as big as the preview Surface
      val bigEnough = ArrayList<Size>()
      // Collect the supported resolutions that are smaller than the preview Surface
      val notBigEnough = ArrayList<Size>()
      val w = aspectRatio.width
      val h = aspectRatio.height
      for (option in choices) {
        if (option.width <= maxWidth
            && option.height <= maxHeight
            && option.height == option.width * h / w
        ) {
          if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
            bigEnough.add(option)
          } else {
            notBigEnough.add(option)
          }
        }
      }

      // Pick the smallest of those big enough. If there is no one big enough, pick the
      // largest of those not big enough.
      return when {
        bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
        notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
        else -> {
          Log.e(TAG, "Couldn't find any suitable preview size")
          choices[0]
        }
      }
    }

    fun newInstance(): Camera2BasicFragment {
      return Camera2BasicFragment()
    }

    fun selesai() {
      imagefile = null
      viewImage!!.visibility = View.GONE
      ambilFoto!!.visibility = View.VISIBLE
      ambilFoto!!.isClickable = true
        tombolSimpan!!.visibility = View.GONE
        tombolSimpan!!.isClickable = false
    }
  }
}
