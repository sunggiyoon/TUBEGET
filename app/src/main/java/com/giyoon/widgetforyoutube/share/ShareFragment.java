package com.giyoon.widgetforyoutube.share;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giyoon.widgetforyoutube.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class ShareFragment extends Fragment {
    // 참조객체 메모리 로드
    private ViewGroup rootView;
    private TextView current_path;
    private RecyclerView recyclerView;
    private ArrayList<String> list;
    private  ShareAdapter mAdapter;

    public ShareAdapter getAdapter(){
        return mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_share, container, false);


        initValue();
        initLayout();

        Button button_save = rootView.findViewById(R.id.folder_choose);
        Button button_cancel = rootView.findViewById(R.id.folder_cancel);
        FloatingActionButton fab = rootView.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                final EditText et = new EditText(getContext());
//                FrameLayout container = new FrameLayout(getContext());
//                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
//                et.setLayoutParams(params);
//                container.addView(et);
                final AlertDialog.Builder alt_bld = new AlertDialog.Builder(getContext());
                View view = LayoutInflater.from(getContext()).inflate(R.layout.dialogue_box,null,false);
                alt_bld.setView(view);
                final com.google.android.material.textfield.TextInputEditText editText
                        = view.findViewById(R.id.edittext_dialog_id);
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


//                if(et.getParent() != null){
//                    ((ViewGroup)et.getParent()).removeView(et);
//                }
//                alt_bld.setTitle(R.string.create_new_folder)
//                        .setMessage(R.string.create_new_folder_message)
//                        .setIcon(R.drawable.ic_create_new_folder_primary_54dp)
//                        .setCancelable(false)
//                        .setView(et)
//                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                String createPath = mAdapter.getCurrentPath()+"/"+et.getText().toString();
//                                File createFolder = new File(createPath);
//                                if(!createFolder.exists()){
//                                    createFolder.mkdir();
//                                }
//                                mAdapter.refresh();
//                            }
//                        });
//                final AlertDialog alertDialog = alt_bld.create();
//                alertDialog.show();
            }
        });



        // 버튼 동작 구현
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String savePath = mAdapter.getCurrentPath() + "/" + ShareActivity.mTitle;
                File saveFile = new File(savePath);
                if(!saveFile.exists()){
                    try {
                        //todo 파일명을 채널이름으로 변경해야함.
                        BufferedWriter bw = new BufferedWriter(new FileWriter(savePath));
                        bw.append(ShareActivity.mChannelInfo);
                        bw.append(ShareActivity.mUrl);
                        bw.close();
                        Toast.makeText(getContext(),R.string.channel_saved,Toast.LENGTH_LONG).show();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(getContext(),R.string.alert_file_exist,Toast.LENGTH_LONG).show();
                }
                ShareActivity activity = (ShareActivity) getActivity();
                activity.onSaveButtonClicked();
            }
        });

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(),R.string.cancel_message,Toast.LENGTH_LONG).show();
                ShareActivity activity = (ShareActivity) getActivity();
                activity.onCancelButtonClicked();
            }

        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.refresh();
    }

    private void initValue() {

    }

    private void initLayout(){
        current_path = rootView.findViewById(R.id.current_path);
        recyclerView = rootView.findViewById(R.id.share_items);
        File channelRootPath = new File(getContext().getFilesDir().getAbsolutePath()+"/channels");
        if(!channelRootPath.exists()){
            channelRootPath.mkdir();
        }
        mAdapter = new ShareAdapter(getContext(),channelRootPath.getAbsolutePath());

        current_path.setText(mAdapter.getCurrentPath());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        recyclerView.setAdapter(mAdapter);

    }

}
