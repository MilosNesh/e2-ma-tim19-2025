package com.example.habitgame.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.adapters.MessageAdapter;
import com.example.habitgame.model.Message;
import com.example.habitgame.model.MessageCallback;
import com.example.habitgame.model.MessageListCallback;
import com.example.habitgame.services.AllianceService;
import com.example.habitgame.services.MessageService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import com.google.firebase.Timestamp;


public class MessagesFragment extends Fragment {

    private EditText writeMessage;
    private ImageButton sentMessage;
    private ListView messages;
    private String allainceId, myEmail, username;
    private MessageService messageService;
    private MessageAdapter messageAdapter;

    public MessagesFragment() {
    }

    public static MessagesFragment newInstance(String param1, String param2) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        writeMessage = view.findViewById(R.id.write_message);
        sentMessage = view.findViewById(R.id.send_message);
        messages = view.findViewById(R.id.message_list);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        allainceId = sharedPreferences.getString("allianceId", "");
        myEmail = sharedPreferences.getString("email", null);
        username = sharedPreferences.getString("username", "");
        messageService = new MessageService();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(allainceId.equals(""))
            return;

        messageService.getAllByAlliance(allainceId, new MessageListCallback() {
            @Override
            public void onResult(List<Message> messageList) {
                if(messageList != null){
                    messageAdapter = new MessageAdapter(getContext(), messageList, myEmail);
                    messages.setAdapter(messageAdapter);
                }
            }
        });

        sentMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(allainceId.equals("")){
                    Toast.makeText(getContext(), "Ne mozete slati poruke jer niste u savezu", Toast.LENGTH_SHORT).show();
                    return;
                }
                Message message = new Message(writeMessage.getText().toString(), username, myEmail, new Timestamp(new Date()), allainceId);
                messageService.save(message, new MessageCallback() {
                    @Override
                    public void onResult(Message m) {
                        writeMessage.setText("");
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.mainContainer);
                        navController.navigate(R.id.messagesFragment);
                    }
                });
            }
        });
    }
}