package com.example.calendar.ui.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.calendar.R;
import com.example.calendar.data.Meal;
import com.example.calendar.databinding.FragmentAiChatBinding;
import java.util.ArrayList;
import java.util.List;

public class AIChatFragment extends Fragment {
    private FragmentAiChatBinding binding;
    private AIChatViewModel viewModel;
    private ChatAdapter chatAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(AIChatViewModel.class);
        setupRecyclerView();
        setupSendButton();
        
        // Observe chat messages
        viewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null && !messages.isEmpty()) {
                chatAdapter.submitList(new ArrayList<>(messages));
                chatAdapter.notifyDataSetChanged();
                binding.chatRecyclerView.post(() -> 
                    binding.chatRecyclerView.smoothScrollToPosition(messages.size() - 1)
                );
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.chatRecyclerView.setAdapter(chatAdapter);
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setHasFixedSize(true);
    }

    private void setupSendButton() {
        binding.sendButton.setOnClickListener(v -> {
            String message = binding.messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
                binding.messageInput.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 