package com.giyoon.widgetforyoutube.share;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.giyoon.widgetforyoutube.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class ShareFragment extends Fragment {
    // 참조객체 메모리 로드
    private ViewGroup rootView;
    private RecyclerView recyclerView;
    private  ShareAdapter mAdapter;

    public ShareAdapter getmAdapter(){
        return  mAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_share, container, false);


        initValue();
        initLayout();

        Button button_save = rootView.findViewById(R.id.folder_choose);
        Button button_cancel = rootView.findViewById(R.id.folder_cancel);



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
                        bw.newLine();
                        bw.append(ShareActivity.mThumbnailUrl);
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
        recyclerView = rootView.findViewById(R.id.share_items);
        File channelRootPath = new File(getContext().getFilesDir().getAbsolutePath()+"/channels");
        if(!channelRootPath.exists()){
            channelRootPath.mkdir();
        }
        mAdapter = new ShareAdapter(getContext(),channelRootPath.getAbsolutePath());

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        recyclerView.setAdapter(mAdapter);

    }

}
