package com.giyoon.widgetforyoutube.share;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.giyoon.widgetforyoutube.R;

public class ShareFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_share, container, false);


        // 현재 경로 구현
        TextView current_path = rootView.findViewById(R.id.current_path);


        // 리스트뷰 구현
        initPath();

        // 버튼 동작 구현
        Button button_save = rootView.findViewById(R.id.folder_choose);
        Button button_cancel = rootView.findViewById(R.id.folder_cancel);

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(),R.string.cancel_message,Toast.LENGTH_LONG).show();
                ShareActivity activity = (ShareActivity) getActivity();
                activity.onCancelButonClicked();
            }

        });

        return rootView;
    }

    private void initPath(){
        //루트의 폴더를 보여준다.
    }

}
