package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.ui.activities.MainActivity

class ChatFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private var property: PropertyDTO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.updateTitle("Chat with Owner")

        rvMessages = view.findViewById(R.id.rvChatMessages)
        etMessage = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnChatSend)

        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        // Dummy data or actual chat logic would go here
        
        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Staff will reply soon!", android.widget.Toast.LENGTH_SHORT).show()
                etMessage.text.clear()
            }
        }
    }
}
