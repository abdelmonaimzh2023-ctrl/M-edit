package com.montage.app

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoContainer = findViewById<FrameLayout>(R.id.logoContainer)
        val logoIcon = findViewById<ImageView>(R.id.splashLogoIcon)
        val logoGlow = findViewById<View>(R.id.logoGlow)
        val titleText = findViewById<TextView>(R.id.splashTitle)
        val subtitleText = findViewById<TextView>(R.id.splashSubtitle)
        val progressBar = findViewById<ProgressBar>(R.id.splashProgress)

        // 1. دوران الشعار مع ظهور
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_360)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        logoContainer.startAnimation(scaleUp)
        logoIcon.startAnimation(rotate)

        // 2. وميض على الحواف
        logoGlow.visibility = View.VISIBLE
        val glowAnim = AnimationUtils.loadAnimation(this, R.anim.glow_pulse)
        logoGlow.startAnimation(glowAnim)

        // 3. ظهور النص بعد تأخير
        titleText.postDelayed({
            titleText.visibility = View.VISIBLE
            titleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
        }, 800)

        subtitleText.postDelayed({
            subtitleText.visibility = View.VISIBLE
            subtitleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
        }, 1200)

        // 4. شريط تقدم متحرك
        progressBar.postDelayed({
            progressBar.visibility = View.VISIBLE
            val animator = ValueAnimator.ofInt(0, 100)
            animator.duration = 1800
            animator.addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            animator.start()
        }, 1500)

        // 5. الانتقال بعد 3.5 ثانية
        logoContainer.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 3500)
    }
}
