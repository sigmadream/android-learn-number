package com.sangkon.numbers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import kotlin.math.abs

class DrawingView(context: Context) : View(context) {

    data class Stroke(
        val path: Path,
        val color: Int,
        val strokeWidth: Float,
        val isEraser: Boolean = false,
    )

    private val strokes = mutableListOf<Stroke>()
    private var currentPath = Path()
    private var currentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var currentColor: Int = "#1E88E5".toColorInt()
    private var currentStrokeWidth: Float = 54f
    private var isEraserMode: Boolean = false

    private var targetNumber: Int = 0

    private val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    // Mask bitmap that defines the valid number track area (coloring-book style clipping)
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
    }

    // Transfer mode to clip drawn strokes inside the number mask
    private val maskCompositePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    // Guide centerline paint (single dashed centerline along the middle of the number track)
    private val guideCenterLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#B0BEC5".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(20f, 16f), 0f)
    }

    // Soft number background track fill so kids see the exact road/track to trace
    private val guideTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#EDF2F7".toColorInt()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Outer border for the number track to give it a crisp workbook look
    private val guideTrackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#CBD5E1".toColorInt()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Start dot paint (shows toddler where to begin tracing)
    private val startDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF7043".toColorInt()
        style = Paint.Style.FILL
    }
    private val startDotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // Canvas & bitmap for caching drawn strokes
    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    // Touch smoothing variables
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Sketchbook background border
    private val boardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E2E8F0".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val boardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val boardRect = RectF()

    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPaint.isAntiAlias = true
        currentPaint.color = currentColor
        currentPaint.style = Paint.Style.STROKE
        currentPaint.strokeJoin = Paint.Join.ROUND
        currentPaint.strokeCap = Paint.Cap.ROUND
        currentPaint.strokeWidth = currentStrokeWidth
        currentPaint.xfermode = null
    }

    fun setTargetNumber(number: Int) {
        targetNumber = number.coerceIn(0, 99)
        updateMask()
        clearCanvas()
    }

    fun setBrushColor(color: Int) {
        isEraserMode = false
        currentColor = color
        currentPaint.xfermode = null
        currentPaint.color = currentColor
        currentPaint.strokeWidth = currentStrokeWidth
    }

    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
        if (enabled) {
            currentPaint.xfermode = clearXfermode
            currentPaint.strokeWidth = 84f
        } else {
            currentPaint.xfermode = null
            currentPaint.color = currentColor
            currentPaint.strokeWidth = currentStrokeWidth
        }
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            redrawAllStrokes()
            invalidate()
        }
    }

    fun clearCanvas() {
        strokes.clear()
        currentPath.reset()
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    private fun redrawAllStrokes() {
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        for ((path, color, strokeWidth, isEraser) in strokes) {
            tempPaint.xfermode = if (isEraser) clearXfermode else null
            tempPaint.color = color
            tempPaint.strokeWidth = strokeWidth
            drawCanvas?.drawPath(path, tempPaint)
        }
    }

    /**
     * Builds a unified Single-Line Skeleton Path for digits 0~9.
     *
     * Every digit is defined inside the exact same vertical range [topY .. bottomY] and width [w],
     * guaranteeing 100% consistent height, proportions, and a single central dashed guide line!
     */
    private fun buildDigitSkeletonPath(
        digit: Char,
        centerX: Float,
        topY: Float,
        bottomY: Float,
        w: Float,
    ): Pair<Path, Pair<Float, Float>> {
        val path = Path()
        val h = bottomY - topY
        val left = centerX - w / 2f
        val right = centerX + w / 2f

        var startX = centerX
        var startY = topY

        when (digit) {
            '0' -> {
                val rect = RectF(left, topY, right, bottomY)
                path.addOval(rect, Path.Direction.CCW)
                startX = centerX
                startY = topY
            }
            '1' -> {
                // Korean textbook 1: gentle diagonal top tick + straight vertical center stem
                startX = centerX - w * 0.25f
                startY = topY + h * 0.18f
                path.moveTo(startX, startY)
                path.lineTo(centerX, topY)
                path.lineTo(centerX, bottomY)
            }
            '2' -> {
                // Clean textbook 2: top rounded arch -> smooth diagonal to bottom-left -> flat horizontal base
                val archRadiusY = h * 0.24f
                startX = left + w * 0.04f
                startY = topY + archRadiusY
                val arcRect = RectF(left, topY, right, topY + archRadiusY * 2f)
                path.arcTo(arcRect, 190f, 205f)
                path.lineTo(left, bottomY)
                path.lineTo(right, bottomY)
            }
            '3' -> {
                // Clean symmetric double-loop 3
                val upperBottom = topY + h * 0.48f
                startX = left + w * 0.06f
                startY = topY + h * 0.16f
                val topRect = RectF(left + w * 0.04f, topY, right - w * 0.02f, upperBottom)
                val botRect = RectF(left, upperBottom, right, bottomY)
                path.arcTo(topRect, 195f, 255f)
                path.arcTo(botRect, 270f, 255f)
            }
            '4' -> {
                // Open top 4 (Korean handwriting & textbook standard with open gap at the top):
                // 1st stroke: starts from top-left, goes down to mid-crossY, then horizontal right
                // 2nd stroke: separate vertical right stem with clear open gap between top endpoints
                val crossY = topY + h * 0.62f
                val leftStartInner = left + w * 0.08f
                val stemX = right - w * 0.22f

                // Stroke 1: Start at upper-left, draw down-left to crossY, then across
                startX = left + w * 0.18f
                startY = topY + h * 0.02f
                path.moveTo(startX, startY)
                path.lineTo(leftStartInner, crossY)
                path.lineTo(right + w * 0.06f, crossY)

                // Stroke 2: Clean vertical stem on the right with open space at the top
                path.moveTo(stemX, topY)
                path.lineTo(stemX, bottomY)
            }
            '5' -> {
                // Standard textbook 5: horizontal roof -> vertical drop -> round lower loop
                val loopTop = topY + h * 0.42f
                startX = right - w * 0.04f
                startY = topY
                path.moveTo(startX, startY)
                path.lineTo(left + w * 0.08f, topY)
                path.lineTo(left + w * 0.04f, loopTop + h * 0.04f)
                val loopRect = RectF(left, loopTop, right, bottomY)
                path.arcTo(loopRect, 230f, 275f)
            }
            '6' -> {
                // Smooth top curve flowing into round bottom loop
                val loopRadiusY = h * 0.27f
                val loopTop = bottomY - loopRadiusY * 2f
                startX = right - w * 0.16f
                startY = topY
                path.moveTo(startX, startY)
                path.quadTo(left - w * 0.02f, topY + h * 0.25f, left, bottomY - loopRadiusY)
                val loopRect = RectF(left, loopTop, right, bottomY)
                path.addOval(loopRect, Path.Direction.CW)
            }
            '7' -> {
                // Horizontal top bar -> clean diagonal stem down
                startX = left
                startY = topY
                path.moveTo(left, topY)
                path.lineTo(right, topY)
                path.lineTo(left + w * 0.25f, bottomY)
            }
            '8' -> {
                // Balanced figure-8 (top circle + bottom circle touching at midY)
                val splitY = topY + h * 0.47f
                val topRect = RectF(left + w * 0.05f, topY, right - w * 0.05f, splitY)
                val botRect = RectF(left, splitY, right, bottomY)
                path.addOval(topRect, Path.Direction.CCW)
                path.addOval(botRect, Path.Direction.CW)
                startX = centerX
                startY = topY
            }
            '9' -> {
                // Korean road-sign / textbook 9: circular top loop + curved hook tail down-left
                val loopRadiusY = h * 0.26f
                val loopCenterY = topY + loopRadiusY
                val loopRect = RectF(left, topY, right, topY + loopRadiusY * 2f)
                path.addOval(loopRect, Path.Direction.CCW)

                // Curved hook tail from right side of loop sweeping down-left to bottomY
                path.moveTo(right, loopCenterY)
                path.quadTo(
                    right + w * 0.04f,
                    topY + h * 0.72f,
                    centerX - w * 0.25f,
                    bottomY,
                )
                startX = right
                startY = loopCenterY
            }
        }

        return Pair(path, Pair(startX, startY))
    }

    /**
     * Renders the current number (0~99) with a unified single-line road track + center dashed guide.
     */
    private fun drawUnifiedNumber(
        canvas: Canvas,
        isMaskOnly: Boolean,
    ) {
        val numStr = targetNumber.toString()
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val isSingleDigit = numStr.length == 1
        val trackThickness = if (isSingleDigit) h * 0.135f else h * 0.105f

        // Exact top and bottom centerline coordinates so that with Round Cap (trackThickness / 2),
        // every single digit's outer top and outer bottom align to the exact pixel!
        val visualHeight = if (isSingleDigit) h * 0.56f else h * 0.44f
        val topY = (h - visualHeight) / 2f + trackThickness * 0.5f
        val bottomY = (h + visualHeight) / 2f - trackThickness * 0.5f
        val skeletonHeight = bottomY - topY

        val stdDigitWidth = skeletonHeight * 0.62f
        val oneDigitWidth = skeletonHeight * 0.36f
        val spacing = if (isSingleDigit) 0f else skeletonHeight * 0.26f

        val digitWidths = FloatArray(numStr.length) { i ->
            if (numStr[i] == '1') oneDigitWidth else stdDigitWidth
        }
        val totalWidth = digitWidths.sum() + spacing * (numStr.length - 1)
        var curX = (w - totalWidth) / 2f

        val paths = mutableListOf<Path>()
        val startPoints = mutableListOf<Pair<Float, Float>>()

        for (i in numStr.indices) {
            val ch = numStr[i]
            val dW = digitWidths[i]
            val centerX = curX + dW / 2f
            val (p, startPt) = buildDigitSkeletonPath(ch, centerX, topY, bottomY, dW)
            paths.add(p)
            startPoints.add(startPt)
            curX += dW + spacing
        }

        if (isMaskOnly) {
            maskPaint.strokeWidth = trackThickness
            for (p in paths) {
                canvas.drawPath(p, maskPaint)
            }
            return
        }

        // 1. Outer soft border around the track (gives workbook road feel)
        guideTrackBorderPaint.strokeWidth = trackThickness + 6f
        for (p in paths) {
            canvas.drawPath(p, guideTrackBorderPaint)
        }

        // 2. Inner light grey road track
        guideTrackPaint.strokeWidth = trackThickness
        for (p in paths) {
            canvas.drawPath(p, guideTrackPaint)
        }

        // 3. Single dashed centerline along the middle of the road!
        for (p in paths) {
            canvas.drawPath(p, guideCenterLinePaint)
        }

        // 4. Cute orange starting dot on each digit so toddlers know where to start tracing
        val dotRadius = trackThickness * 0.20f
        for ((sx, sy) in startPoints) {
            canvas.drawCircle(sx, sy, dotRadius, startDotPaint)
            canvas.drawCircle(sx, sy, dotRadius, startDotRingPaint)
        }
    }

    private fun updateMask() {
        val w = width
        val h = height
        if ((w <= 0) || (h <= 0)) return

        maskBitmap?.recycle()
        maskBitmap = createBitmap(w, h)
        maskCanvas = Canvas(maskBitmap!!)

        maskCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawUnifiedNumber(maskCanvas!!, isMaskOnly = true)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if ((w > 0) && (h > 0)) {
            canvasBitmap?.recycle()
            canvasBitmap = createBitmap(w, h)
            drawCanvas = Canvas(canvasBitmap!!)
            updateMask()
            redrawAllStrokes()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val margin = 28f
        boardRect.left = margin
        boardRect.top = margin
        boardRect.right = width - margin
        boardRect.bottom = height - margin

        // White rounded sketchbook board
        canvas.drawRoundRect(boardRect, 32f, 32f, boardFillPaint)
        canvas.drawRoundRect(boardRect, 32f, 32f, boardBorderPaint)

        // Draw unified road-style number track + single center dashed line + start dots
        drawUnifiedNumber(canvas, isMaskOnly = false)

        // Render user's crayon strokes masked cleanly inside the number road track
        canvasBitmap?.let { srcBitmap ->
            val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            canvas.drawBitmap(srcBitmap, 0f, 0f, bitmapPaint)
            canvas.drawPath(currentPath, currentPaint)

            // Restrict user drawings to the number track using DST_IN mask
            maskBitmap?.let { mBitmap ->
                canvas.drawBitmap(mBitmap, 0f, 0f, maskCompositePaint)
            }
            canvas.restoreToCount(saveCount)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(touchX, touchY)
                lastTouchX = touchX
                lastTouchY = touchY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(touchX - lastTouchX)
                val dy = abs(touchY - lastTouchY)
                if ((dx >= 4) || (dy >= 4)) {
                    currentPath.quadTo(
                        lastTouchX,
                        lastTouchY,
                        (touchX + lastTouchX) / 2f,
                        (touchY + lastTouchY) / 2f,
                    )
                    lastTouchX = touchX
                    lastTouchY = touchY
                }
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(touchX, touchY)
                performClick()

                val pWidth = if (isEraserMode) 84f else currentStrokeWidth
                strokes.add(
                    Stroke(
                        Path(currentPath),
                        currentColor,
                        pWidth,
                        isEraserMode,
                    ),
                )

                val bakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = if (isEraserMode) clearXfermode else null
                    color = currentColor
                    strokeWidth = pWidth
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                }
                drawCanvas?.drawPath(currentPath, bakePaint)
                currentPath.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }
}
