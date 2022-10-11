package uz.jsoft.mediaplayer

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import uz.jsoft.mediaplayer.databinding.ActivityMainBinding
import uz.jsoft.mediaplayer.utils.extensions.hide
import uz.jsoft.mediaplayer.utils.extensions.show

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding ?: throw NullPointerException("View wasn't created")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)


        Log.d("TTTTTT", "onCreate: ")
        toggle = ActionBarDrawerToggle(this, binding.mainDrawer, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()


        binding.navView.setOnClickListener {
            when (it.id) {

            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun showAppBar() {
        try {
            binding.appbarLayout.show()
        } catch (e: Exception) {

        }
    }

    fun hideAppBar() {
        try {
            binding.appbarLayout.hide()
        } catch (e: Exception) {

        }
    }
}