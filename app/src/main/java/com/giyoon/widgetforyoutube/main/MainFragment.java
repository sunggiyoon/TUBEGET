package com.giyoon.widgetforyoutube.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giyoon.widgetforyoutube.R;
import com.giyoon.widgetforyoutube.widget.WidgetConfigure;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class MainFragment extends Fragment implements MainActivity.onKeyBackPressedListener,  View.OnClickListener, Updater {
    // 참조객체 메모리 로드
    private ViewGroup rootView;
    private RecyclerView recyclerView;
    private MainAdapter mAdapter;
    private TextView mPath;


    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton fab, fab_create_foler, fab_get_channel_from_search, fab_get_channel_from_subscription;


    @Override
    public void pathUpdater(String path) {
        mPath.setText(path);
    }

    @Override
    public void onBack(){
        if (!mAdapter.getCurrentPath().equals(mAdapter.getRootPath())) {
            mAdapter.prevPath();
        } else {
            MainActivity activity = (MainActivity) getActivity();
            activity.setOnKeyBackPressedListener(null);
            activity.onBackPressed();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.setOnKeyBackPressedListener(this);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);
        mPath = rootView.findViewById(R.id.main_filepath);

        initValue();
        initLayout();

        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab_create_foler = (FloatingActionButton) rootView.findViewById(R.id.fab_create_folder);
        fab_get_channel_from_search = (FloatingActionButton) rootView.findViewById(R.id.fab_get_channel_from_search);
        fab_get_channel_from_subscription = (FloatingActionButton) rootView.findViewById(R.id.fab_get_channel_from_subscription);

        fab.setOnClickListener(this);
        fab_create_foler.setOnClickListener(this);
        fab_get_channel_from_search.setOnClickListener(this);
        fab_get_channel_from_subscription.setOnClickListener(this);



        return rootView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                anim();
                break;
            case R.id.fab_create_folder:
                anim();
                final AlertDialog.Builder alt_bld = new AlertDialog.Builder(getContext());
                View view = LayoutInflater.from(getContext()).inflate(R.layout.dialogue_box,null,false);
                alt_bld.setView(view);
                final com.google.android.material.textfield.TextInputEditText editText
                        = view.findViewById(R.id.edittext_dialog_id);
                editText.setSingleLine(true);
                editText.setLines(1);
                final Button button_save
                        = view.findViewById(R.id.confirm_button);
                final Button button_cancel
                        = view.findViewById(R.id.cancel_button);

                final AlertDialog dialog = alt_bld.create();

                button_save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String createPath = mAdapter.getCurrentPath()+"/"+editText.getText().toString();
                        File createFolder = new File(createPath);
                        if(!createFolder.exists()){
                            createFolder.mkdir();
                        }
                        mAdapter.refresh();
                        dialog.dismiss();
                    }
                });

                button_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                break;
            //case R.id.fab_get_channel_from_search:
                //anim();

                //break;
            case R.id.fab_get_channel_from_subscription:
                anim();

                Intent intent = new Intent(getContext(), WidgetConfigure.class);
                intent.putExtra("from","main");
                startActivityForResult(intent,1001);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode  == -1){
            if(requestCode == 1001){
                String mChannelInfo = data.getStringExtra("mChannelInfo");
                String mThumbnailUri = data.getStringExtra("mThumbnailUri");
                String mTitle = data.getStringExtra("mTitle");

                String savePath = mAdapter.getCurrentPath() + "/" + mTitle;
                File saveFile = new File(savePath);


                if(!saveFile.exists()){
                    try {
                        //todo 파일명을 채널이름으로 변경해야함.
                        BufferedWriter bw = new BufferedWriter(new FileWriter(savePath));
                        bw.append(mChannelInfo);
                        bw.newLine();
                        bw.append(mThumbnailUri);
                        bw.close();
                        Toast.makeText(getContext(),R.string.channel_saved,Toast.LENGTH_LONG).show();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(getContext(),R.string.alert_file_exist,Toast.LENGTH_LONG).show();
                }

            }else{
                Toast.makeText(getContext(), R.string.fail_to_add_channel,Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getContext(),R.string.channel_add_canceled,Toast.LENGTH_SHORT).show();
        }

        mAdapter.refresh();
    }

    public void anim() {

        if (isFabOpen) {
            fab_create_foler.startAnimation(fab_close);
            //fab_get_channel_from_search.startAnimation(fab_close);
            fab_get_channel_from_subscription.startAnimation(fab_close);
            fab_create_foler.setClickable(false);
            //fab_get_channel_from_search.setClickable(false);
            fab_get_channel_from_subscription.setClickable(false);
            isFabOpen = false;
        } else {
            fab_create_foler.startAnimation(fab_open);
            //fab_get_channel_from_search.startAnimation(fab_open);
            fab_get_channel_from_subscription.startAnimation(fab_open);
            fab_create_foler.setClickable(true);
            //fab_get_channel_from_search.setClickable(true);
            fab_get_channel_from_subscription.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.refresh();
    }

    private void initValue() {

    }

    private void initLayout(){
        recyclerView = rootView.findViewById(R.id.main_items);
        File channelRootPath = new File(getContext().getFilesDir().getAbsolutePath()+"/channels");
        if(!channelRootPath.exists()){
            channelRootPath.mkdir();
        }
        mAdapter = new MainAdapter(getContext(),channelRootPath.getAbsolutePath(), this);


        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        recyclerView.setAdapter(mAdapter);

    }




}
