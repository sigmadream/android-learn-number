package com.sangkon.numbers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var mainContainer: FrameLayout
    private lateinit var menuLayout: LinearLayout
    private lateinit var drawingLayout: LinearLayout
    private lateinit var countingLayout: LinearLayout
    private lateinit var mathLayout: LinearLayout

    // State
    private var currentScreen: Screen = Screen.MENU

    private enum class Screen {
        MENU, DRAWING, COUNTING, MATH
    }

    // Drawing Screen
    private lateinit var drawingView: DrawingView
    private var currentDrawingNumber = 0
    private var currentBrushColor: Int = "#1E88E5".toColorInt()
    private var isEraserActive: Boolean = false
    private lateinit var drawingNumberIndicator: TextView
    private lateinit var drawingNumberScrollView: HorizontalScrollView
    private lateinit var drawingNumberContainer: LinearLayout
    private val paletteChips = mutableListOf<MaterialCardView>()
    private lateinit var eraserBtnCard: MaterialCardView

    // Counting Screen
    private lateinit var countingStageLabel: TextView
    private lateinit var countingTitleText: TextView
    private lateinit var countingObjectContainer: FrameLayout
    private lateinit var countingOptionsContainer: LinearLayout
    private var currentCountTarget = 1
    private var isCountingProcessing = false

    private data class CountingTheme(
        val stageTitle: String,
        val questionText: String,
        val drawableRes: Int,
    )

    private val countingThemes = listOf(
        CountingTheme("사과 세기 놀이", "사과가 몇 개 있을까요?", R.drawable.ic_apple_kids),
        CountingTheme("반짝반짝 별 세기", "별이 몇 개 있을까요?", R.drawable.ic_star_kids),
        CountingTheme("부릉부릉 자동차 세기", "자동차가 몇 대 있을까요?", R.drawable.ic_car_kids),
    )

    // Math Screen
    private lateinit var mathQuestionText: TextView
    private lateinit var mathVisualContainer: LinearLayout
    private lateinit var mathOptionsContainer: LinearLayout
    private val mathLevelButtons = mutableListOf<MaterialCardView>()
    private var currentMathAnswer = 0
    private var currentMathLevel = 1
    private var isMathProcessing = false
    private var currentOp1 = 1
    private var currentOp2 = 1
    private var currentOpCode = 0
    private val currentMathOptions = mutableListOf<Int>()

    // Stars / Rewards
    private var starsEarned = 0
    private lateinit var starCounterText: TextView

    companion object {
        private const val KEY_SCREEN = "key_current_screen"
        private const val KEY_STARS = "key_stars_earned"
        private const val KEY_DRAWING_NUMBER = "key_drawing_number"
        private const val KEY_DRAWING_COLOR = "key_drawing_color"
        private const val KEY_DRAWING_ERASER = "key_drawing_eraser"
        private const val KEY_COUNTING_TARGET = "key_counting_target"
        private const val KEY_MATH_LEVEL = "key_math_level"
        private const val KEY_MATH_ANSWER = "key_math_answer"
        private const val KEY_MATH_OP1 = "key_math_op1"
        private const val KEY_MATH_OP2 = "key_math_op2"
        private const val KEY_MATH_OP_CODE = "key_math_op_code"
        private const val KEY_MATH_OPTIONS = "key_math_options"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainContainer = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_cream))
        }
        setContentView(mainContainer)

        // Safe insets handling: prevent status bar and navigation bar from overlapping buttons/top bars
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupMenu()
        setupDrawingScreen()
        setupCountingScreen()
        setupMathScreen()

        // Handle System Back Button
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    if (currentScreen != Screen.MENU) {
                        showMenu()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        // Restore or initialize state
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            showMenu()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SCREEN, currentScreen.name)
        outState.putInt(KEY_STARS, starsEarned)
        outState.putInt(KEY_DRAWING_NUMBER, currentDrawingNumber)
        outState.putInt(KEY_DRAWING_COLOR, currentBrushColor)
        outState.putBoolean(KEY_DRAWING_ERASER, isEraserActive)
        outState.putInt(KEY_COUNTING_TARGET, currentCountTarget)
        outState.putInt(KEY_MATH_LEVEL, currentMathLevel)
        outState.putInt(KEY_MATH_ANSWER, currentMathAnswer)
        outState.putInt(KEY_MATH_OP1, currentOp1)
        outState.putInt(KEY_MATH_OP2, currentOp2)
        outState.putInt(KEY_MATH_OP_CODE, currentOpCode)
        outState.putIntegerArrayList(KEY_MATH_OPTIONS, ArrayList(currentMathOptions))
    }

    private fun restoreState(savedState: Bundle) {
        starsEarned = savedState.getInt(KEY_STARS, 0)
        updateStarText()

        currentDrawingNumber = savedState.getInt(KEY_DRAWING_NUMBER, 0)
        currentBrushColor = savedState.getInt(KEY_DRAWING_COLOR, "#1E88E5".toColorInt())
        isEraserActive = savedState.getBoolean(KEY_DRAWING_ERASER, false)

        currentCountTarget = savedState.getInt(KEY_COUNTING_TARGET, 1)
        currentMathLevel = savedState.getInt(KEY_MATH_LEVEL, 1)
        currentMathAnswer = savedState.getInt(KEY_MATH_ANSWER, 0)
        currentOp1 = savedState.getInt(KEY_MATH_OP1, 1)
        currentOp2 = savedState.getInt(KEY_MATH_OP2, 1)
        currentOpCode = savedState.getInt(KEY_MATH_OP_CODE, 0)
        val savedOptions = savedState.getIntegerArrayList(KEY_MATH_OPTIONS)
        currentMathOptions.clear()
        if (savedOptions != null) {
            currentMathOptions.addAll(savedOptions)
        }

        val screenName = savedState.getString(KEY_SCREEN, Screen.MENU.name)
        when (screenName) {
            Screen.DRAWING.name -> {
                showDrawingScreen()
                if (isEraserActive) {
                    drawingView.setEraserMode(true)
                    highlightPaletteSelection(-1)
                } else {
                    drawingView.setBrushColor(currentBrushColor)
                }
            }
            Screen.COUNTING.name -> {
                currentScreen = Screen.COUNTING
                menuLayout.visibility = View.GONE
                drawingLayout.visibility = View.GONE
                countingLayout.visibility = View.VISIBLE
                mathLayout.visibility = View.GONE
                renderCountingProblem(currentCountTarget)
            }
            Screen.MATH.name -> {
                currentScreen = Screen.MATH
                menuLayout.visibility = View.GONE
                drawingLayout.visibility = View.GONE
                countingLayout.visibility = View.GONE
                mathLayout.visibility = View.VISIBLE
                updateMathLevelUI()
                if (currentMathOptions.isNotEmpty()) {
                    renderMathProblem(currentOp1, currentOp2, currentOpCode, currentMathAnswer, currentMathOptions)
                } else {
                    generateMathProblem()
                }
            }
            else -> showMenu()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics,
        ).toInt()
    }

    private fun updateStarText() {
        starCounterText.text = starsEarned.toString()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyBounceTouch(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            false
        }
    }

    // ==========================================
    // 1. MENU SCREEN
    // ==========================================
    private fun setupMenu() {
        menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_cream))
        }

        // Top Bar with Star Counter
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val starIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_star_badge)
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                setMargins(0, 0, dp(6), 0)
            }
        }
        starCounterText = TextView(this).apply {
            text = "0"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.coral_primary))
            typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(starIcon)
        topBar.addView(starCounterText)
        menuLayout.addView(topBar)

        // Title Header
        val headerIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_star_kids)
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                setMargins(0, dp(12), 0, dp(8))
            }
        }
        val titleText = TextView(this).apply {
            text = "신나는 숫자 놀이터"
            textSize = 30f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subTitle = TextView(this).apply {
            text = "재미있게 놀면서 숫자를 배워요!"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_gray))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(24))
        }

        menuLayout.addView(headerIcon)
        menuLayout.addView(titleText)
        menuLayout.addView(subTitle)

        // Menu Cards
        val cardDraw = createMenuCard(
            title = "숫자 따라 그리기",
            desc = "0부터 99까지 쓱쓱 따라 써봐요",
            iconRes = R.drawable.ic_brush,
            accentColor = ContextCompat.getColor(this, R.color.sky_blue),
        ) {
            showDrawingScreen()
        }

        val cardCount = createMenuCard(
            title = "개수 세기 놀이",
            desc = "귀여운 그림이 몇 개 있을까?",
            iconRes = R.drawable.ic_apple_kids,
            accentColor = ContextCompat.getColor(this, R.color.coral_primary),
        ) {
            showCountingScreen()
        }

        val cardMath = createMenuCard(
            title = "재미있는 덧셈 뺄셈",
            desc = "쉽고 재미있는 숫자 계산 퀴즈",
            iconRes = R.drawable.ic_star_kids,
            accentColor = ContextCompat.getColor(this, R.color.purple_soft),
        ) {
            showMathScreen()
        }

        menuLayout.addView(cardDraw)
        menuLayout.addView(cardCount)
        menuLayout.addView(cardMath)

        mainContainer.addView(
            menuLayout,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun createMenuCard(
        title: String,
        desc: String,
        iconRes: Int,
        accentColor: Int,
        onClick: () -> Unit,
    ): View {
        val card = MaterialCardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#EEF2F6".toColorInt()
            strokeWidth = dp(2)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(88),
            ).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }

        val iconBox = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(accentColor)
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                setMargins(0, 0, dp(16), 0)
            }
        }
        val iconImg = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        iconBox.addView(iconImg)

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tTitle = TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            typeface = Typeface.DEFAULT_BOLD
        }
        val tDesc = TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_gray))
            setPadding(0, dp(2), 0, 0)
        }
        textLayout.addView(tTitle)
        textLayout.addView(tDesc)

        content.addView(iconBox)
        content.addView(textLayout)
        card.addView(content)

        applyBounceTouch(card)
        card.setOnClickListener { onClick() }

        return card
    }

    // ==========================================
    // 2. DRAWING SCREEN (0~99 Support + Palette + Bottom Nav)
    // ==========================================
    private fun setupDrawingScreen() {
        drawingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_drawing))
            visibility = View.GONE
        }

        // Top Bar: Back button, current number indicator, Undo, Clear
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val btnBack = createRoundIconButton(R.drawable.ic_arrow_back) {
            showMenu()
        }

        drawingNumberIndicator = TextView(this).apply {
            text = "숫자 0"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnUndo = createRoundIconButton(R.drawable.ic_undo) {
            drawingView.undo()
        }
        val btnClear = createWideTextButton("지우기") {
            drawingView.clearCanvas()
        }

        topBar.addView(btnBack)
        topBar.addView(drawingNumberIndicator)
        topBar.addView(btnUndo)
        topBar.addView(btnClear)
        drawingLayout.addView(topBar)

        // Number Quick Selector Bar (0 to 99 Horizontal Scroll)
        drawingNumberScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(dp(16), 0, dp(16), dp(8))
            }
        }
        drawingNumberContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        populateNumberChips()
        drawingNumberScrollView.addView(drawingNumberContainer)
        drawingLayout.addView(drawingNumberScrollView)

        // Center Container: Drawing Canvas with Big Rectangular Left/Right Navigation Buttons
        val canvasContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                setMargins(dp(12), 0, dp(12), dp(8))
            }
        }

        drawingView = DrawingView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        canvasContainer.addView(drawingView)

        // Big Rectangular Navigation Buttons placed on bottom-left and bottom-right of the drawing area
        val btnPrev = createSquareNavButton("이전", "<") {
            if (currentDrawingNumber > 0) {
                selectDrawingNumber(currentDrawingNumber - 1)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(dp(76), dp(64)).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(16), 0, 0, dp(16))
            }
        }

        val btnNext = createSquareNavButton("다음", ">") {
            if (currentDrawingNumber < 99) {
                selectDrawingNumber(currentDrawingNumber + 1)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(dp(76), dp(64)).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(16), dp(16))
            }
        }

        canvasContainer.addView(btnPrev)
        canvasContainer.addView(btnNext)
        drawingLayout.addView(canvasContainer)

        // Bottom Color Palette & Tool Bar
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(6), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val colors = listOf(
            R.color.crayon_blue,
            R.color.crayon_red,
            R.color.crayon_yellow,
            R.color.crayon_green,
            R.color.crayon_purple,
            R.color.crayon_orange,
        )

        paletteChips.clear()
        colors.forEachIndexed { index, cRes ->
            val colorVal = ContextCompat.getColor(this, cRes)
            val colorChip = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(colorVal)
                strokeColor = if (index == 0) ContextCompat.getColor(this@MainActivity, R.color.text_dark) else Color.WHITE
                strokeWidth = if (index == 0) dp(3) else dp(2)
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    setMargins(dp(5), 0, dp(5), 0)
                }
                setOnClickListener {
                    isEraserActive = false
                    currentBrushColor = colorVal
                    drawingView.setBrushColor(colorVal)
                    highlightPaletteSelection(index)
                }
            }
            applyBounceTouch(colorChip)
            paletteChips.add(colorChip)
            bottomBar.addView(colorChip)
        }

        eraserBtnCard = createRoundIconButton(R.drawable.ic_eraser) {
            isEraserActive = true
            drawingView.setEraserMode(true)
            highlightPaletteSelection(-1)
        }.apply {
            (layoutParams as LinearLayout.LayoutParams).setMargins(dp(14), 0, 0, 0)
        }
        bottomBar.addView(eraserBtnCard)

        drawingLayout.addView(bottomBar)

        mainContainer.addView(
            drawingLayout,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun highlightPaletteSelection(selectedIndex: Int) {
        val activeStroke = ContextCompat.getColor(this, R.color.text_dark)
        paletteChips.forEachIndexed { idx, chip ->
            if (idx == selectedIndex) {
                chip.strokeColor = activeStroke
                chip.strokeWidth = dp(3)
            } else {
                chip.strokeColor = Color.WHITE
                chip.strokeWidth = dp(2)
            }
        }
        if (selectedIndex == -1) {
            eraserBtnCard.strokeColor = activeStroke
            eraserBtnCard.strokeWidth = dp(3)
        } else {
            eraserBtnCard.strokeColor = "#E0E0E0".toColorInt()
            eraserBtnCard.strokeWidth = dp(1)
        }
    }

    private fun populateNumberChips() {
        drawingNumberContainer.removeAllViews()
        for (i in 0..99) {
            val chip = MaterialCardView(this).apply {
                tag = i
                radius = dp(16).toFloat()
                cardElevation = dp(1).toFloat()
                setCardBackgroundColor(
                    if (i == currentDrawingNumber) {
                        ContextCompat.getColor(this@MainActivity, R.color.coral_primary)
                    } else {
                        Color.WHITE
                    },
                )
                strokeColor = "#E0E0E0".toColorInt()
                strokeWidth = dp(1)
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(38)).apply {
                    setMargins(dp(3), dp(2), dp(3), dp(2))
                }
            }
            val text = TextView(this).apply {
                this.text = i.toString()
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    if (i == currentDrawingNumber) {
                        Color.WHITE
                    } else {
                        ContextCompat.getColor(this@MainActivity, R.color.text_dark)
                    },
                )
                gravity = Gravity.CENTER
            }
            chip.addView(text)
            applyBounceTouch(chip)
            chip.setOnClickListener {
                selectDrawingNumber(i)
            }
            drawingNumberContainer.addView(chip)
        }
    }

    private fun selectDrawingNumber(num: Int) {
        currentDrawingNumber = num.coerceIn(0, 99)
        val labelText = "숫자 $currentDrawingNumber"
        drawingNumberIndicator.text = labelText
        drawingView.setTargetNumber(currentDrawingNumber)

        // Update chips visual
        for (i in 0 until drawingNumberContainer.childCount) {
            val chip = (drawingNumberContainer.getChildAt(i) as? MaterialCardView) ?: continue
            val numVal = (chip.tag as? Int) ?: continue
            val tv = chip.getChildAt(0) as? TextView
            if (numVal == currentDrawingNumber) {
                chip.setCardBackgroundColor(ContextCompat.getColor(this, R.color.coral_primary))
                tv?.setTextColor(Color.WHITE)
                drawingNumberScrollView.post {
                    drawingNumberScrollView.smoothScrollTo(chip.left - dp(120), 0)
                }
            } else {
                chip.setCardBackgroundColor(Color.WHITE)
                tv?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
        }
    }

    // ==========================================
    // 3. COUNTING SCREEN (Non-overlapping Objects + Multi-Theme)
    // ==========================================
    private fun setupCountingScreen() {
        countingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_counting))
            visibility = View.GONE
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        // Top Header
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val btnBack = createRoundIconButton(R.drawable.ic_arrow_back) {
            showMenu()
        }
        countingStageLabel = TextView(this).apply {
            text = "개수 세기 놀이"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            setPadding(dp(16), 0, 0, 0)
        }
        topBar.addView(btnBack)
        topBar.addView(countingStageLabel)
        countingLayout.addView(topBar)

        countingTitleText = TextView(this).apply {
            text = "사과가 몇 개 있을까요?"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.coral_primary))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
        }
        countingLayout.addView(countingTitleText)

        // Object Play Area Card
        val playCard = MaterialCardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#E0F2F1".toColorInt()
            strokeWidth = dp(2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                setMargins(0, dp(8), 0, dp(16))
            }
        }

        countingObjectContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        playCard.addView(countingObjectContainer)
        countingLayout.addView(playCard)

        // Options Container (4 Cards)
        countingOptionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        countingLayout.addView(countingOptionsContainer)

        mainContainer.addView(
            countingLayout,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun generateCountingProblem() {
        currentCountTarget = Random.nextInt(1, 10) // 1 to 9
        renderCountingProblem(currentCountTarget)
    }

    private fun renderCountingProblem(targetCount: Int) {
        isCountingProcessing = false
        currentCountTarget = targetCount
        val theme = countingThemes.random()

        countingStageLabel.text = theme.stageTitle
        countingTitleText.text = theme.questionText
        countingTitleText.setTextColor(ContextCompat.getColor(this, R.color.coral_primary))

        countingObjectContainer.removeAllViews()

        // Place objects cleanly in a 3x3 grid to guarantee no overlaps on any device
        val positions = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) {
            for (col in 0..2) {
                positions.add(Pair(row, col))
            }
        }
        positions.shuffle()

        var tappedCounter = 0

        countingObjectContainer.post {
            val containerW = countingObjectContainer.width
            val containerH = countingObjectContainer.height
            if ((containerW <= 0) || (containerH <= 0)) return@post

            val cellW = containerW / 3
            val cellH = containerH / 3
            val itemSize = dp(64)

            for (i in 0 until currentCountTarget) {
                val (r, c) = positions[i]
                val item = ImageView(this).apply {
                    setImageResource(theme.drawableRes)
                    layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
                    val jitterX = Random.nextInt(-dp(8), dp(8))
                    val jitterY = Random.nextInt(-dp(8), dp(8))
                    x = (c * cellW + (cellW - itemSize) / 2 + jitterX).toFloat()
                    y = (r * cellH + (cellH - itemSize) / 2 + jitterY).toFloat()
                }
                applyBounceTouch(item)
                item.setOnClickListener {
                    val animX = ObjectAnimator.ofFloat(item, "scaleX", 1f, 1.3f, 1f)
                    val animY = ObjectAnimator.ofFloat(item, "scaleY", 1f, 1.3f, 1f)
                    AnimatorSet().apply {
                        playTogether(animX, animY)
                        duration = 300
                        interpolator = OvershootInterpolator()
                        start()
                    }
                    if (!isCountingProcessing && item.tag == null) {
                        item.tag = true
                        tappedCounter++
                        val countFeedback = "$tappedCounter!"
                        countingTitleText.text = countFeedback
                    }
                }
                countingObjectContainer.addView(item)
            }
        }

        // 4 Options
        val options = mutableListOf(currentCountTarget)
        while (options.size < 4) {
            val w = Random.nextInt(1, 10)
            if (!options.contains(w)) options.add(w)
        }
        options.shuffle()

        countingOptionsContainer.removeAllViews()
        for (opt in options) {
            val optCard = createOptionButton(opt.toString()) {
                if (isCountingProcessing) return@createOptionButton
                if (opt == currentCountTarget) {
                    isCountingProcessing = true
                    starsEarned++
                    updateStarText()
                    countingTitleText.text = "🎉 정답이에요! 참 잘했어요!"
                    countingTitleText.setTextColor(ContextCompat.getColor(this, R.color.green_success))
                    countingTitleText.postDelayed(
                        {
                            generateCountingProblem()
                        },
                        1100,
                    )
                } else {
                    countingTitleText.text = "다시 한번 세어볼까요? 😊"
                    countingTitleText.setTextColor(ContextCompat.getColor(this, R.color.orange_accent))
                }
            }
            countingOptionsContainer.addView(optCard)
        }
    }

    // ==========================================
    // 4. MATH SCREEN (Visuals + Levels)
    // ==========================================
    private fun setupMathScreen() {
        mathLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_math))
            visibility = View.GONE
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        // Top Navigation Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val btnBack = createRoundIconButton(R.drawable.ic_arrow_back) {
            showMenu()
        }
        val levelSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        mathLevelButtons.clear()
        val levelNames = listOf("쉬움", "보통", "도전")
        levelNames.forEachIndexed { index, name ->
            val lvl = index + 1
            val btn = createWideTextButton(name) {
                currentMathLevel = lvl
                updateMathLevelUI()
                generateMathProblem()
            }
            mathLevelButtons.add(btn)
            levelSelector.addView(btn)
        }

        topBar.addView(btnBack)
        topBar.addView(levelSelector)
        mathLayout.addView(topBar)

        // Math Question Card
        val questionCard = MaterialCardView(this).apply {
            radius = dp(28).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#EDE7F6".toColorInt()
            strokeWidth = dp(2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                setMargins(0, dp(16), 0, dp(16))
            }
        }

        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        mathQuestionText = TextView(this).apply {
            text = "1 + 1 = ?"
            textSize = 50f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            gravity = Gravity.CENTER
        }
        cardContent.addView(mathQuestionText)

        // Visual Representation for Kids (e.g. 🍎🍎 + 🍎)
        mathVisualContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        cardContent.addView(mathVisualContainer)

        questionCard.addView(cardContent)
        mathLayout.addView(questionCard)

        // 4 Option Cards
        mathOptionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        mathLayout.addView(mathOptionsContainer)

        mainContainer.addView(
            mathLayout,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
    }

    private fun updateMathLevelUI() {
        mathLevelButtons.forEachIndexed { idx, card ->
            val tv = card.getChildAt(0) as? TextView
            if (idx + 1 == currentMathLevel) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.purple_soft))
                tv?.setTextColor(Color.WHITE)
            } else {
                card.setCardBackgroundColor(Color.WHITE)
                tv?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
        }
    }

    private fun generateMathProblem() {
        val prob = MathCore.generateProblem(currentMathLevel)
        currentOp1 = prob[0]
        currentOp2 = prob[1]
        currentOpCode = prob[2]
        currentMathAnswer = prob[3]

        currentMathOptions.clear()
        currentMathOptions.addAll(listOf(currentMathAnswer, prob[4], prob[5], prob[6]))
        currentMathOptions.shuffle()

        renderMathProblem(currentOp1, currentOp2, currentOpCode, currentMathAnswer, currentMathOptions)
    }

    private fun renderMathProblem(op1: Int, op2: Int, opCode: Int, answer: Int, options: List<Int>) {
        isMathProcessing = false
        currentMathAnswer = answer

        val opChar = when (opCode) {
            0 -> "+"
            1 -> "-"
            2 -> "×"
            3 -> "÷"
            else -> "+"
        }

        val questionStr = "$op1 $opChar $op2 = ?"
        mathQuestionText.text = questionStr
        mathQuestionText.setTextColor(ContextCompat.getColor(this, R.color.text_dark))

        // Visual helper for addition & subtraction when counts fit nicely on screen
        mathVisualContainer.removeAllViews()
        if ((opCode == 0 || opCode == 1) && op1 <= 6 && op2 <= 6) {
            addVisualIcons(op1)
            val signTv = TextView(this).apply {
                val signText = " $opChar "
                text = signText
                textSize = 26f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_gray))
                typeface = Typeface.DEFAULT_BOLD
            }
            mathVisualContainer.addView(signTv)
            addVisualIcons(op2)
        }

        mathOptionsContainer.removeAllViews()
        for (opt in options) {
            val btn = createOptionButton(opt.toString()) {
                if (isMathProcessing) return@createOptionButton
                if (opt == currentMathAnswer) {
                    isMathProcessing = true
                    starsEarned++
                    updateStarText()
                    mathQuestionText.text = "🎉 정답입니다!"
                    mathQuestionText.setTextColor(ContextCompat.getColor(this, R.color.green_success))
                    mathQuestionText.postDelayed(
                        {
                            generateMathProblem()
                        },
                        1100,
                    )
                } else {
                    mathQuestionText.text = "다시 생각해봐요! 🤔"
                    mathQuestionText.setTextColor(ContextCompat.getColor(this, R.color.orange_accent))
                }
            }
            mathOptionsContainer.addView(btn)
        }
    }

    private fun addVisualIcons(count: Int) {
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(count) {
            val img = ImageView(this).apply {
                setImageResource(R.drawable.ic_apple_kids)
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
            }
            group.addView(img)
        }
        mathVisualContainer.addView(group)
    }

    // ==========================================
    // REUSABLE KIDS UI COMPONENTS
    // ==========================================
    private fun createSquareNavButton(subLabel: String, arrowLabel: String, onClick: () -> Unit): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = ContextCompat.getColor(this@MainActivity, R.color.sky_blue)
            strokeWidth = dp(2)
            isClickable = true
            isFocusable = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val arrowTv = TextView(this).apply {
            text = arrowLabel
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sky_blue))
            gravity = Gravity.CENTER
        }

        val subTv = TextView(this).apply {
            text = subLabel
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            gravity = Gravity.CENTER
        }

        container.addView(arrowTv)
        container.addView(subTv)
        card.addView(container)

        applyBounceTouch(card)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun createRoundIconButton(iconRes: Int, onClick: () -> Unit): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#E0E0E0".toColorInt()
            strokeWidth = dp(1)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }
        val img = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(img)
        applyBounceTouch(card)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun createWideTextButton(label: String, onClick: () -> Unit): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#E0E0E0".toColorInt()
            strokeWidth = dp(1)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(40)).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        }
        val tv = TextView(this).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            gravity = Gravity.CENTER
        }
        card.addView(tv)
        applyBounceTouch(card)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun createOptionButton(label: String, onClick: () -> Unit): View {
        val card = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = "#E0E7EE".toColorInt()
            strokeWidth = dp(2)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                setMargins(dp(8), 0, dp(8), 0)
            }
        }
        val tv = TextView(this).apply {
            text = label
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            gravity = Gravity.CENTER
        }
        card.addView(tv)
        applyBounceTouch(card)
        card.setOnClickListener { onClick() }
        return card
    }

    // ==========================================
    // SCREEN NAVIGATION
    // ==========================================
    private fun showMenu() {
        currentScreen = Screen.MENU
        menuLayout.visibility = View.VISIBLE
        drawingLayout.visibility = View.GONE
        countingLayout.visibility = View.GONE
        mathLayout.visibility = View.GONE
    }

    private fun showDrawingScreen() {
        currentScreen = Screen.DRAWING
        menuLayout.visibility = View.GONE
        drawingLayout.visibility = View.VISIBLE
        countingLayout.visibility = View.GONE
        mathLayout.visibility = View.GONE
        selectDrawingNumber(currentDrawingNumber)
    }

    private fun showCountingScreen() {
        currentScreen = Screen.COUNTING
        menuLayout.visibility = View.GONE
        drawingLayout.visibility = View.GONE
        countingLayout.visibility = View.VISIBLE
        mathLayout.visibility = View.GONE
        generateCountingProblem()
    }

    private fun showMathScreen() {
        currentScreen = Screen.MATH
        menuLayout.visibility = View.GONE
        drawingLayout.visibility = View.GONE
        countingLayout.visibility = View.GONE
        mathLayout.visibility = View.VISIBLE
        updateMathLevelUI()
        generateMathProblem()
    }
}
