package com.github.edsergeev.expandabletextview

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.github.edsergeev.expandabletextview.Constants.Companion.COLLAPSED_MAX_LINES
import com.github.edsergeev.expandabletextview.Constants.Companion.DEFAULT_ANIM_DURATION
import com.github.edsergeev.expandabletextview.Constants.Companion.DEFAULT_ELLIPSIZED_TEXT
import com.github.edsergeev.expandabletextview.Constants.Companion.READ_LESS
import com.github.edsergeev.expandabletextview.Constants.Companion.READ_MORE

class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = R.attr.expandableTextView
) : AppCompatTextView(context, attrs, defStyleAttr),
    View.OnClickListener {

    private var mOriginalText: CharSequence? = ""
    private var mCollapsedLines = 0
    private var mReadMoreText: CharSequence = READ_MORE
    private var mReadLessText: CharSequence = READ_LESS
    private var isExpanded: Boolean = false
    private var mAnimationDuration = 0
    private var foregroundColor = 0
    private var initialText = ""
    private var isUnderlined = false
    private var mEllipsizeTextColor = 0

    private var visibleText: String? = null
    private var currentAnimation: Animator? = null
    private val expandStateListeners = mutableListOf<ExpandStateListener>()

    override fun onClick(v: View?) {
        toggle(expand = !isExpanded)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (initialText.isBlank()) {
            initialText = text.toString()
            visibleText = visibleText()

            setEllipsizedText(isExpanded)
            setForeground(isExpanded)
        }
    }

    fun expand() {
        post {
            if (isAttachedToWindow) {
                toggle(expand = true)
            }
        }
    }

    fun collapse() {
        post {
            if (isAttachedToWindow) {
                toggle(expand = false)
            }
        }
    }

    fun addExpandStateListener(listener: ExpandStateListener) {
        this.expandStateListeners.add(listener)
    }

    fun removeExpandStateListener(listener: ExpandStateListener) {
        this.expandStateListeners.remove(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimation?.cancel()
        currentAnimation = null
    }

    private fun toggle(expand: Boolean) {
        val visibleTextRef = visibleText ?: return
        if (visibleTextRef.isAllTextVisible()) {
            return
        }
        currentAnimation?.cancel()
        currentAnimation = null

        isExpanded = expand

        maxLines = if (!isExpanded) {
            mCollapsedLines
        } else {
            COLLAPSED_MAX_LINES
        }

        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        setEllipsizedText(isExpanded)

        expandStateListeners.forEach { it.onExpandStateChanged(expand) }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        mOriginalText = text
        super.setText(text, type)
    }

    fun setReadMoreText(readMore: String) {
        mReadMoreText = readMore
    }

    fun setReadLessText(readLess: String) {
        mReadLessText = readLess
    }

    //private functions
    init {
        context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView).apply {
            try {
                mCollapsedLines =
                    getInt(R.styleable.ExpandableTextView_collapsedLines, COLLAPSED_MAX_LINES)
                mAnimationDuration =
                    getInt(R.styleable.ExpandableTextView_animDuration, DEFAULT_ANIM_DURATION)
                mReadMoreText = getString(R.styleable.ExpandableTextView_readMoreText) ?: READ_MORE
                mReadLessText = getString(R.styleable.ExpandableTextView_readLessText) ?: READ_LESS
                foregroundColor =
                    getColor(R.styleable.ExpandableTextView_foregroundColor, Color.TRANSPARENT)
                isUnderlined = getBoolean(R.styleable.ExpandableTextView_isUnderlined, false)
                isExpanded = getBoolean(R.styleable.ExpandableTextView_isExpanded, false)
                mEllipsizeTextColor =
                    getColor(R.styleable.ExpandableTextView_ellipsizeTextColor, Color.BLUE)
            } finally {
                this.recycle()
            }
        }

        if (!isExpanded) {
            maxLines = mCollapsedLines
        }
        setOnClickListener(this)
    }

    private fun setEllipsizedText(isExpanded: Boolean) {
        val visibleTextRef = visibleText ?: return

        if (initialText.isBlank())
            return

        text =
            if (isExpanded || visibleTextRef.isAllTextVisible() || mCollapsedLines == COLLAPSED_MAX_LINES) {
                SpannableStringBuilder(initialText)
                    .append(DEFAULT_ELLIPSIZED_TEXT)
                    .append(mReadLessText.toString().span())
            } else {
                val endIndex =
                    if (visibleTextRef.length - (mReadMoreText.toString().length + DEFAULT_ELLIPSIZED_TEXT.length) < 0) visibleTextRef.length
                    else visibleTextRef.length - (mReadMoreText.toString().length + DEFAULT_ELLIPSIZED_TEXT.length)
                SpannableStringBuilder(visibleTextRef.substring(0, endIndex))
                    .append(DEFAULT_ELLIPSIZED_TEXT)
                    .append(mReadMoreText.toString().span())
            }
    }

    private fun visibleText(): String {
        try {
            var end = 0

            return if (mCollapsedLines < COLLAPSED_MAX_LINES) {
                for (i in 0 until mCollapsedLines) {
                    if (layout.getLineEnd(i) == 0) {
                        break
                    } else {
                        end = layout.getLineEnd(i)
                    }
                }
                initialText.substring(0, end - mReadMoreText.toString().length)
            } else {
                initialText
            }
        } catch (e: Exception) {
            return initialText
        }
    }

    private fun setForeground(isExpanded: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground =
                GradientDrawable(BOTTOM_TOP, intArrayOf(foregroundColor, Color.TRANSPARENT))
            foreground.alpha = if (isExpanded) {
                MIN_VALUE_ALPHA
            } else {
                MAX_VALUE_ALPHA
            }
        }
    }

    private fun animationSet(startHeight: Int, endHeight: Int): AnimatorSet {
        val textView = this
        return AnimatorSet().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                playTogether(
                    ObjectAnimator.ofInt(
                        textView,
                        ANIMATION_PROPERTY_MAX_HEIGHT,
                        startHeight,
                        endHeight
                    ),
                    ObjectAnimator.ofInt(
                        this@ExpandableTextView.foreground,
                        ANIMATION_PROPERTY_ALPHA,
                        foreground.alpha,
                        MAX_VALUE_ALPHA - foreground.alpha
                    )
                )
            }
        }
    }

    private fun String.isAllTextVisible(): Boolean = this == text

    private fun String.span(): SpannableString =
        SpannableString(this).apply {
            setSpan(
                ForegroundColorSpan(mEllipsizeTextColor),
                0,
                this.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (isUnderlined)
                setSpan(
                    UnderlineSpan(),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
        }

    interface ExpandStateListener {
        fun onExpandStateChanged(isExpanded: Boolean)
    }

    companion object {
        const val TAG = "ExpandableTextView"
        const val MAX_VALUE_ALPHA = 255
        const val MIN_VALUE_ALPHA = 0
        const val ANIMATION_PROPERTY_MAX_HEIGHT = "height"
        const val ANIMATION_PROPERTY_ALPHA = "alpha"
    }
}