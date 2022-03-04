package ru.krirll.optimalmaps.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import ru.krirll.optimalmaps.databinding.FragmentMapBinding
import java.lang.RuntimeException

class MapFragment : Fragment() {

    private var _viewBinding: FragmentMapBinding? = null
    private val viewBinding: FragmentMapBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentMapBinding == null")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentMapBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
    }

    //map settings
    private fun initMap() {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        viewBinding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK) //standard kind of map
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(DEFAULT_ZOOM)
        }
    }

    //clean view binding
    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    companion object {
        private const val DEFAULT_ZOOM = 5.0
    }

}