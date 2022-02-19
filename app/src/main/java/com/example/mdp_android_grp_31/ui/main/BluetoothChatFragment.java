package com.example.mdp_android_grp_31.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdp_android_grp_31.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A placeholder fragment containing a simple view.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "CommsFragment";

    private PageViewModel pageViewModel;

    // Declaration Variable
    // Shared Preferences
    SharedPreferences sharedPreferences;

    private static TextView messageReceivedTextView;
    private EditText typeBoxEditText;

    StringBuilder messages;

    public static BluetoothChatFragment newInstance(int index) {
        BluetoothChatFragment fragment = new BluetoothChatFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);

        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));
        messages = new StringBuilder();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_comms2, container, false);


        ImageButton send;
        send = root.findViewById(R.id.messageButton);

        // Message Box
        messageReceivedTextView = (TextView) root.findViewById(R.id.messageReceivedTitleTextView);
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        typeBoxEditText = (EditText) root.findViewById(R.id.typeBoxEditText);

        // get shared preferences
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked sendTextBtn");
                String sentText = "" + typeBoxEditText.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentText);
                editor.commit();
                //messageReceivedTextView.append(sharedPreferences.getString("message", ""));
                messageReceivedTextView.append(sentText+"\n");
                typeBoxEditText.setText("");

                if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                }
                showLog("Exiting sendTextBtn");
            }
        });

        return root;
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    public static TextView getMessageReceivedTextView() {
        return messageReceivedTextView;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("receivedMessage");
            Log.d("QQQQ", text);
            messageReceivedTextView.append(text+"\n"+"\n");
        }
    };
}