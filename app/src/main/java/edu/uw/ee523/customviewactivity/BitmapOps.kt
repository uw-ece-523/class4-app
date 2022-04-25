package edu.uw.ee523.customviewactivity

import android.graphics.Bitmap

class BitmapOps {
    companion object {
        /**
         * The idea of a blur is that for every pixel in an image,
         * we average the value of the current pixel with the pixels
         * around it. The larger the radius, the more the blur.
         *
         */
        fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
            var sentBitmap = sentBitmap
            val width = Math.round(sentBitmap.width * scale)
            val height = Math.round(sentBitmap.height * scale)
            sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)

            val bitmap = sentBitmap.copy(sentBitmap.config, true)
            if (radius < 1) {
                return null
            }
            val w = bitmap.width
            val h = bitmap.height
            val pix = IntArray(w * h)
//        Log.e("pix", w.toString() + " " + h + " " + pix.size)
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)

            val width_max = w - 1
            val height_max = h - 1
            val totalNumPixels = w * h
            val div = radius + radius + 1

            // Create an array for each of red, green and blue
            val r = IntArray(totalNumPixels)
            val g = IntArray(totalNumPixels)
            val b = IntArray(totalNumPixels)

            // Keep track of the sums
            var red_sum: Int
            var green_sum: Int
            var blue_sum: Int
            var x: Int
            var y: Int

            var i: Int
            var curPixel: Int

            var yp: Int
            var yi: Int
            var yw: Int

            val vmin = IntArray(Math.max(w, h))
            var divsum = div + 1 shr 1 // shr = shift right
            divsum *= divsum

            val dv = IntArray(256 * divsum)
            i = 0
            while (i < 256 * divsum) {
                dv[i] = i / divsum
                i++
            }

            yi = 0
            yw = yi

            // Use a stack to keep track of pixels during the process
            val stack = Array(div) { IntArray(3) } // A stack of 3-element ints (R, G, B)
            var stackpointer: Int
            var stackstart: Int
            var sir: IntArray
            var rbs: Int

            val r1 = radius + 1
            var routsum: Int
            var goutsum: Int
            var boutsum: Int
            var rinsum: Int
            var ginsum: Int
            var binsum: Int

            // First, handle y
            y = 0
            while (y < h) {
                blue_sum = 0
                green_sum = 0
                red_sum = 0
                boutsum = 0
                goutsum = 0
                routsum = 0
                binsum = 0
                ginsum = 0
                rinsum = 0

                // Look at pixels in the radius
                i = -radius
                while (i <= radius) {
                    curPixel = pix[yi + Math.min(width_max, Math.max(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = curPixel and 0xff0000 shr 16 // Get the R val
                    sir[1] = curPixel and 0x00ff00 shr 8  // Get the G val
                    sir[2] = curPixel and 0x0000ff        // Get the B val
                    rbs =
                        r1 - Math.abs(i)  // The closer to the edge of the radius, the less it contributes
                    red_sum += sir[0] * rbs
                    green_sum += sir[1] * rbs
                    blue_sum += sir[2] * rbs
                    if (i > 0) { // "going in" vs "going out" of the pixel
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    i++
                }
                stackpointer = radius
                x = 0
                while (x < w) {
                    r[yi] = dv[red_sum]
                    g[yi] = dv[green_sum]
                    b[yi] = dv[blue_sum]
                    red_sum -= routsum
                    green_sum -= goutsum
                    blue_sum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]
                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]
                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, width_max)
                    }
                    curPixel = pix[yw + vmin[x]]
                    sir[0] = curPixel and 0xff0000 shr 16
                    sir[1] = curPixel and 0x00ff00 shr 8
                    sir[2] = curPixel and 0x0000ff
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    red_sum += rinsum
                    green_sum += ginsum
                    blue_sum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]
                    yi++
                    x++
                }
                yw += w
                y++
            }

            x = 0
            while (x < w) {
                blue_sum = 0
                green_sum = 0
                red_sum = 0
                boutsum = 0
                goutsum = 0
                routsum = 0
                binsum = 0
                ginsum = 0
                rinsum = 0

                yp = -radius * w
                i = -radius
                while (i <= radius) {
                    yi = Math.max(0, yp) + x
                    sir = stack[i + radius]
                    sir[0] = r[yi]
                    sir[1] = g[yi]
                    sir[2] = b[yi]
                    rbs = r1 - Math.abs(i)
                    red_sum += r[yi] * rbs
                    green_sum += g[yi] * rbs
                    blue_sum += b[yi] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    if (i < height_max) {
                        yp += w
                    }
                    i++
                }
                yi = x
                stackpointer = radius
                y = 0
                while (y < h) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] =
                        -0x1000000 and pix[yi] or (dv[red_sum] shl 16) or (dv[green_sum] shl 8) or dv[blue_sum]
                    red_sum -= routsum
                    green_sum -= goutsum
                    blue_sum -= boutsum
                    stackstart = stackpointer - radius + div

                    sir = stack[stackstart % div]
                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]
                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, height_max) * w
                    }
                    curPixel = x + vmin[y]
                    sir[0] = r[curPixel]
                    sir[1] = g[curPixel]
                    sir[2] = b[curPixel]
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    red_sum += rinsum
                    green_sum += ginsum
                    blue_sum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]
                    yi += w
                    y++
                }
                x++
            }
//        Log.e("pix", w.toString() + " " + h + " " + pix.size)
            bitmap.setPixels(pix, 0, w, 0, 0, w, h)
            return bitmap
        }
    }
}