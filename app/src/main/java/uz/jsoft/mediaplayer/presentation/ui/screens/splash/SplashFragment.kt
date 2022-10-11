package uz.jsoft.mediaplayer.presentation.ui.screens.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import uz.jsoft.mediaplayer.databinding.FragmentSplashBinding
import uz.jsoft.mediaplayer.utils.extensions.hideAppBar

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding: FragmentSplashBinding
        get() = _binding ?: throw NullPointerException("View wasn't created")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentSplashBinding.inflate(layoutInflater)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideAppBar()

        Handler(Looper.myLooper()!!).postDelayed({
            checkScreen()
        }, 1500)

    }

    private fun checkScreen() {
        findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToMusicListFragment())

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}