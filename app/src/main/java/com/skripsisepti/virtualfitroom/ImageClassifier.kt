/*POSE ESTIMATION MOBILE - Septian Puji
*
* Aplikasi Mobile Implementasi Pose Estimation - Skeleton Tracking sebagai pengukur bagian tubuh
*
* Last Update - 28-05-20
*
* */

package com.skripsisepti.virtualfitroom

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode

/**
 * Classifies images with Tensorflow Lite.
 */
abstract class ImageClassifier
/** Initializes an `ImageClassifier`.  */
@Throws(IOException::class)
internal constructor(
  activity: Activity,
  val imageSizeX: Int, // Get the image size along the x axis.
  val imageSizeY: Int, // Get the image size along the y axis.
  private val modelPath: String, // Get the name of the model file stored in Assets.
  // Get the number of bytes that is used to store a single color channel value.
  numBytesPerChannel: Int
) {

  /* Preallocated buffers for storing image data in. */
  private val intValues = IntArray(imageSizeX * imageSizeY)

  /** An instance of the driver class to run model inference with Tensorflow Lite.  */
  protected var tflite: Interpreter? = null

  /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
  protected var imgData: ByteBuffer? = null

  var mPrintPointArray: Array<FloatArray>? = null


  init {
    tflite = Interpreter(loadModelFile(activity))
    imgData = ByteBuffer.allocateDirect(
        DIM_BATCH_SIZE
            * imageSizeX
            * imageSizeY
            * DIM_PIXEL_SIZE
            * numBytesPerChannel
    )
    imgData!!.order(ByteOrder.nativeOrder())
    //Log.d(TAG, "Created a Tensorflow Lite Image Classifier.")
  }

  /** Classifies a frame from the preview stream.  */
  @SuppressLint("LogNotTimber")
  fun classifyFrame(bitmap: Bitmap) {
    if (tflite == null) {
      Log.e(TAG, "Image classifier has not been initialized; Skipped.")
      return
    }

    /** Writes Image data into a `ByteBuffer`.
     * KARENA IMGDATA (DATA YANG DIPROSES) TYPENYA HARUS BYTE BUFFER
     * */
    if (imgData == null) {
      return
    }
    imgData!!.rewind()
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    // Convert the image to floating point.
    var pixel = 0
    for (i in 0 until imageSizeX) {
      for (j in 0 until imageSizeY) {
        val v = intValues[pixel++]
        addPixelValue(v)
      }
    }

    runInference()
  }


  /** Closes tflite to release resources.  */
  fun close() {
    tflite!!.close()
    tflite = null
  }

  /** Memory-map the model file in Assets.  */
  @Throws(IOException::class)
  private fun loadModelFile(activity: Activity): MappedByteBuffer {
    val fileDescriptor = activity.assets.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(MapMode.READ_ONLY, startOffset, declaredLength)
  }



  /**
   * Add pixelValue to byteBuffer.
   *
   * @param pixelValue
   */
  protected abstract fun addPixelValue(pixelValue: Int)

  /**
   * Read the probability value for the specified label This is either the original value as it was
   * read from the net's output or the updated value after the filter was applied.
   *
   * @param labelIndex
   * @return
   */
  protected abstract fun getProbability(labelIndex: Int): Float

  /**
   * Set the probability value for the specified label.
   *
   * @param labelIndex
   * @param value
   */
  protected abstract fun setProbability(
    labelIndex: Int,
    value: Number
  )

  /**
   * Get the normalized probability value for the specified label. This is the final value as it
   * will be shown to the user.
   *
   * @return
   */
  protected abstract fun getNormalizedProbability(labelIndex: Int): Float

  /**
   * Run inference using the prepared input in [.imgData]. Afterwards, the result will be
   * provided by getProbability().
   *
   *
   * This additional method is necessary, because we don't have a common base for different
   * primitive data types.
   */
  protected abstract fun runInference()

  companion object {

    /** Tag for the [Log].  */
    private const val TAG = "TfLiteCamera"

    /** Dimensions of inputs.  */
    private const val DIM_BATCH_SIZE = 1

    private const val DIM_PIXEL_SIZE = 3

  }
}
