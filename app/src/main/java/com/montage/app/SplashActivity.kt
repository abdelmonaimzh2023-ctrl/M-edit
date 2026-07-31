package com.montage.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.montage.app.ui.projects.ProjectsActivity

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

        // الأيقونة هي icon.png التي وضعناها سابقاً
        logoIcon.setImageResource(R.drawable.icon)

        // دوران الشعار
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_360)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        logoContainer.startAnimation(scaleUp)
        logoIcon.startAnimation(rotate)

        // وميض أخضر
        logoGlow.visibility = View.VISIBLE
        val glowAnim = AnimationUtils.loadAnimation(this, R.anim.glow_pulse)
        logoGlow.startAnimation(glowAnim)

        // ظهور النص
        titleText.postDelayed({
            titleText.visibility = View.VISIBLE
            titleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
        }, 800)

        subtitleText.postDelayed({
            subtitleText.visibility = View.VISIBLE
            subtitleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
        }, 1200)

        // شريط تقدم
        progressBar.postDelayed({
            progressBar.visibility = View.VISIBLE
        }, 1500)

        // الانتقال للمشاريع
        logoContainer.postDelayed({
            startActivity(Intent(this, ProjectsActivity::class.java))
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 3500)
    }
}
