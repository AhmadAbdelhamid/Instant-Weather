package com.example.instantweather.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.instantweather.R
import com.example.instantweather.databinding.ActivityMainBinding
import com.example.instantweather.ui.history.HistoryFragment
import com.example.instantweather.ui.home.HomeFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import me.ibrahimsn.lib.OnItemSelectedListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        binding.bottomNavBar.setOnItemSelectedListener(object: OnItemSelectedListener{
            override fun onItemSelect(pos: Int) {
                val HOME_FRAGMENT = 0
                val HISTORY_FRAGMENT = 1
                val CHART_FRAGMENT = 2
                val SETTINGS_FRAGMENT = 3
                when(pos){
                    HOME_FRAGMENT -> {
                        NavHostFragment.findNavController(fragment).navigate(R.id.homeFragment)
                    }
                    HISTORY_FRAGMENT -> {
                        NavHostFragment.findNavController(fragment).navigate(R.id.historyFragment)
                    }
                }
            }

        })
    }


}
