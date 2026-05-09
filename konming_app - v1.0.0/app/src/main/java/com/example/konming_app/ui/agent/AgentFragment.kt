package com.example.konming_app.ui.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.konming_app.R

class AgentFragment : Fragment() {
    private lateinit var viewModel: AgentViewModel
    private lateinit var statusTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AgentViewModel::class.java]
        statusTextView = view.findViewById(R.id.status_text)

        viewModel.statusText.observe(viewLifecycleOwner) { text ->
            statusTextView.text = text
        }
    }
}