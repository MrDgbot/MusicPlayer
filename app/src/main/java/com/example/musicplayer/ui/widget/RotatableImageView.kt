package com.example.musicplayer.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class RotatableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private var mRotateDegrees = 0f
    private var mPivotX = 0f
    private var mPivotY = 0f
    private var isAnimationRunning = false
    private var animator: Animator? = null

    private fun computePivotPoint() {
        mPivotX = width / 1.68f
        mPivotY = height / 4.9f
    }

    private fun endAnimation() {
        isAnimationRunning = false
    }

    override fun onDraw(canvas: Canvas) {
        computePivotPoint()
        canvas.save()
        canvas.rotate(mRotateDegrees, mPivotX, mPivotY)
        //  便于判断旋转中心
        //  Paint paint = new Paint();
        //  paint.setColor(Color.RED);
        //  canvas.drawCircle(mPivotX, mPivotY, 10, paint); // 这将绘制一个半径为10的小红点
        super.onDraw(canvas)
        canvas.restore()
    }

    fun rotate(degrees: Float) {
        mRotateDegrees += degrees
        invalidate() // 重新绘制
    }

    fun resetRotation() {
        mRotateDegrees = 0f
        invalidate() // 重新绘制
    }

    fun rotateWithAnimation(targetDegrees: Float) {
        if (isAnimationRunning) {
            animator?.end()
        }
        isAnimationRunning = true
        val startDegrees = mRotateDegrees
        val endDegrees = mRotateDegrees + targetDegrees
        computePivotPoint()
        animator = ValueAnimator.ofFloat(startDegrees, endDegrees).apply {
            duration = 1000 // 设置动画时长，例如1秒
            addUpdateListener { animation ->
                mRotateDegrees = animation.animatedValue as Float
                pivotX = mPivotX
                pivotY = mPivotY
                rotation = mRotateDegrees
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endAnimation()
                }
            })
        }
        animator?.start() // 开始动画
    }

    // 使用动画效果还原旋转
    fun resetRotationWithAnimation() {
        if (isAnimationRunning) {
            animator?.end()
        }
        isAnimationRunning = true
        val startDegrees = mRotateDegrees
        computePivotPoint()
        animator = ValueAnimator.ofFloat(startDegrees, 0f).apply {
            duration = 1000 // 设置动画时长，例如1秒
            addUpdateListener { animation ->
                mRotateDegrees = animation.animatedValue as Float
                pivotX = mPivotX
                pivotY = mPivotY
                rotation = mRotateDegrees
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endAnimation()
                }
            })
        }
        animator?.start()
    }

}
