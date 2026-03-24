package com.example.lifegame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.lifegame.databinding.ActivityMainBinding
import com.example.lifegame.ui.widget.AttributeChangeManager
import com.example.lifegame.ui.widget.CelebrationManager
import com.example.lifegame.util.AttributeChangeBus
import com.example.lifegame.util.CelebrationBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var attributeChangeManager: AttributeChangeManager? = null
    private var celebrationManager: CelebrationManager? = null

    companion object {
        private var instance: MainActivity? = null
        
        fun getInstance(): MainActivity? = instance
        
        fun getAttributeChangeManager(): AttributeChangeManager? = instance?.attributeChangeManager
        
        fun getCelebrationManager(): CelebrationManager? = instance?.celebrationManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
        
        attributeChangeManager = AttributeChangeManager(this, binding.attributeChangeContainer)
        celebrationManager = CelebrationManager(this, binding.celebrationContainer)
        
        instance = this
        
        observeAttributeChanges()
        observeCelebrations()
    }
    
    private fun observeAttributeChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AttributeChangeBus.events.collect { event ->
                    attributeChangeManager?.showAttributeChangesNamed(event.changes)
                }
            }
        }
    }
    
    private fun observeCelebrations() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CelebrationBus.events.collect { event ->
                    celebrationManager?.showCelebration(event)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        attributeChangeManager?.destroy()
        celebrationManager?.destroy()
        if (instance == this) {
            instance = null
        }
    }
}
