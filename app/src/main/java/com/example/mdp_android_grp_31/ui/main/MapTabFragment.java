package com.example.mdp_android_grp_31.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.mdp_android_grp_31.MainActivity;
import com.example.mdp_android_grp_31.R;

import org.json.JSONException;

import java.util.ArrayList;

public class MapTabFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "MapFragment";

    private PageViewModel pageViewModel;

    SharedPreferences mapPref;
    private static SharedPreferences.Editor editor;

    Button resetMapBtn, updateButton, saveMapObstacle, loadMapObstacle;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    Switch manualAutoToggleBtn;
    GridMap gridMap;

    Switch dragSwitch;
    Switch changeObstacleSwitch;
    Spinner spinner_imageID;
    Spinner spinner_imageBearing;
    private static boolean autoUpdate = false;

    static String imageID;
    static String imageBearing;
    static boolean dragStatus;
    static boolean changeObstacleStatus;

    public static MapTabFragment newInstance(int index) {
        MapTabFragment fragment = new MapTabFragment();
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
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_map_config, container, false);

        gridMap = MainActivity.getGridMap();
        final DirectionFragment directionFragment = new DirectionFragment();

        resetMapBtn = root.findViewById(R.id.resetBtn);
        setStartPointToggleBtn = root.findViewById(R.id.startpointToggleBtn);
        setWaypointToggleBtn = root.findViewById(R.id.waypointToggleBtn);
        directionChangeImageBtn = root.findViewById(R.id.changeDirectionBtn);
        exploredImageBtn = root.findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = root.findViewById(R.id.addObstacleBtn);
        clearImageBtn = root.findViewById(R.id.clearImageBtn);
        updateButton = root.findViewById(R.id.updateMapBtn);
        saveMapObstacle = root.findViewById(R.id.saveBtn);
        loadMapObstacle = root.findViewById(R.id.loadBtn);

        dragSwitch = root.findViewById(R.id.dragSwitch);
        changeObstacleSwitch = root.findViewById(R.id.changeObstacleSwitch);
        spinner_imageID = root.findViewById(R.id.imageIDSpinner);
        spinner_imageBearing = root.findViewById(R.id.bearingSpinner);
        spinner_imageID.setEnabled(false);
        spinner_imageBearing.setEnabled(false);

        // Create an ArrayAdapter using the string array and a default spinner layout
        // Specify the layout to use when the list of choices appears
        // Apply the adapter to the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.imageID_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_imageID.setAdapter(adapter);

        // Repeat for imageBearing
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this.getContext(),
                R.array.imageBearing_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_imageBearing.setAdapter(adapter2);

        spinner_imageID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View v, int pos, long arg3) {
                imageID = a.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> a) { }
        });

        spinner_imageBearing.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View v, int pos, long arg3) {
                imageBearing = a.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> a) { }
        });

        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap();
            }
        });

        // switch for dragging
        dragSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Dragging is " + (isChecked ? "on" : "off"));
                dragStatus = isChecked;
                if (dragStatus == true) {
                    // disable imageID and imageBearing and disable setObstacle when drag is on
                    spinner_imageID.setEnabled(false);
                    spinner_imageBearing.setEnabled(false);
                    gridMap.setSetObstacleStatus(false);
                    changeObstacleSwitch.setChecked(false);
                }
            }
        });

        // switch for changing obstacle
        changeObstacleSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Changing Obstacle is " + (isChecked ? "on" : "off"));
                changeObstacleStatus = isChecked;
                if (changeObstacleStatus == true) {
                    // disable dragging, imageID and imageBearing and disable setObstacle
                    spinner_imageID.setEnabled(false);
                    spinner_imageBearing.setEnabled(false);
                    gridMap.setSetObstacleStatus(false);
                    dragSwitch.setChecked(false);
                }
            }
        });

        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("STARTING POINT"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL")
                        && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        saveMapObstacle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String getObsPos = "";
                mapPref = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = mapPref.edit();
                if(!mapPref.getString("maps", "").equals("")){
                    editor.putString("maps", "");
                    editor.commit();
                }
                getObsPos = GridMap.saveObstacleList();
                editor.putString("maps",getObsPos);
                editor.commit();
            }
        });

        loadMapObstacle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapPref = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                String obsPos = mapPref.getString("maps","");
                String[] obstaclePosition = obsPos.split("\\|");
                for (String s : obstaclePosition) {
                    String[] coords = s.split(",");
                    gridMap.setObstacleCoord(Integer.parseInt(coords[0])+1, Integer.parseInt(coords[1])+1);
                    String direction = "";
                    switch (coords[2]) {
                        case "N":
                            direction = "North";
                            break;
                        case "E":
                            direction = "East";
                            break;
                        case "W":
                            direction = "West";
                            break;
                        case "S":
                            direction = "South";
                            break;
                        default:
                            direction = "";
                    }
                    gridMap.imageBearings.get(Integer.parseInt(coords[1]))[Integer.parseInt(coords[0])] = direction;
                }
                gridMap.invalidate();
                showLog("Exiting Load Button");
            }
        });


        directionChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked directionChangeImageBtn");
                directionFragment.show(getActivity().getFragmentManager(),
                        "Direction Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });

        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");

                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    spinner_imageID.setEnabled(true);
                    spinner_imageBearing.setEnabled(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (gridMap.getSetObstacleStatus()) {
                    gridMap.setSetObstacleStatus(false);
                    spinner_imageID.setEnabled(false);
                    spinner_imageBearing.setEnabled(false);
                }

                changeObstacleSwitch.setChecked(false);
                dragSwitch.setChecked(false);
                showLog("obstacle status = " + gridMap.getSetObstacleStatus());
                showLog("Exiting obstacleImageBtn");
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked updateButton");

                gridMap.imageBearings.get(9)[5] = "South";
                gridMap.imageBearings.get(15)[15] = "South";
                gridMap.imageBearings.get(14)[7] = "West";
                gridMap.imageBearings.get(4)[15] = "West";
                gridMap.imageBearings.get(9)[12] = "East";
                gridMap.setObstacleCoord(5+1, 9+1);
                gridMap.setObstacleCoord(15+1, 15+1);
                gridMap.setObstacleCoord(7+1, 14+1);
                gridMap.setObstacleCoord(15+1, 4+1);
                gridMap.setObstacleCoord(12+1, 9+1);

                gridMap.invalidate();
                showLog("Exiting updateButton");
            }
        });
        return root;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}