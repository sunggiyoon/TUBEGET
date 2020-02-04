package com.giyoon.widgetforyoutube.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.giyoon.widgetforyoutube.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.giyoon.widgetforyoutube.share.ShareAdapter.deleteFolder;
import static com.giyoon.widgetforyoutube.share.ShareAdapter.deleteFile;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    /**
     * {@link Context}
     */
    private Context mContext;
    /**
     * 리스트 아이템
     */
    private ArrayList<File> mList;
    /**
     * 최상위 경로
     */
    private String mRootFolderPath;
    /**
     * 현재 경로
     */
    private String mCurrentPath;

    /**
     * 상위 경로
     */
    private String mUpperPath;
    /**
     * 프래그먼트 함수 콜백
     */
    private Updater mCallback;

    //아이템 뷰를 저장하는 뷰홀더 클래스.
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, View.OnClickListener{

        ImageView imageView;
        TextView textView;

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem edit = menu.add(Menu.NONE,1001,1, R.string.edit);
            MenuItem delete = menu.add(Menu.NONE,1002,2,R.string.delete);
            edit.setOnMenuItemClickListener(onContextMenu);
            delete.setOnMenuItemClickListener(onContextMenu);
        }

        @Override
        public void onClick(View v) {
            File file = mList.get(getAdapterPosition());
            String fullPath = mList.get(getAdapterPosition()).getPath();
            String path = mList.get(getAdapterPosition()).getName();
            if(file.isFile()) {
                //파일을 선택했을 떄
                try {
                    FileReader fr = new FileReader(file);
                    ArrayList<String> data = new ArrayList<String>();
                    String line = null;
                    BufferedReader bufferedReader = new BufferedReader(fr);
                    while ((line = bufferedReader.readLine()) != null) {
                        data.add(line);
                    }
                    String mChannelInfo = data.get(0);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mChannelInfo)).setPackage("com.google.android.youtube");
                    mContext.startActivity(intent);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                if (fullPath.equals(mUpperPath)) {
                    prevPath();
                } else {
                    nextPath(path);
                }
            }
        }

        ViewHolder(View itemView){
            super(itemView);
            imageView = itemView.findViewById(R.id.item_icon);
            textView = itemView.findViewById(R.id.item_name);

            // 아이템 클릭 이벤트 처리.
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        private final MenuItem.OnMenuItemClickListener onContextMenu = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case 1001:
                        if(mList.get(getAdapterPosition()).isDirectory()){
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            View view = LayoutInflater.from(mContext).inflate(R.layout.edit_box, null, false);
                            builder.setView(view);
                            final Button buttonSubmit = (Button) view.findViewById(R.id.button_dialog_submit);
                            final EditText editTextID = (EditText) view.findViewById(R.id.edittext_dialog_id);

                            // 6. 해당 줄에 입력되어 있던 데이터를 불러와서 다이얼로그에 보여줍니다.
                            editTextID.setText(mList.get(getAdapterPosition()).getName());

                            final AlertDialog dialog = builder.create();
                            buttonSubmit.setOnClickListener(new View.OnClickListener() {

                                // 7. 수정 버튼을 클릭하면 현재 UI에 입력되어 있는 내용으로
                                public void onClick(View v) {
                                    File newFile = new File(mList.get(getAdapterPosition()).getParent(),editTextID.getText().toString());
                                    mList.get(getAdapterPosition()).renameTo(newFile);

                                    // 8. 어댑터에서 RecyclerView에 반영하도록 합니다.
                                    refresh();
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        }else{
                            Toast.makeText(mContext,R.string.cannot_change,Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case 1002:
                        if(mList.get(getAdapterPosition()).isDirectory()) {
                            deleteFolder(mList.get(getAdapterPosition()).getAbsolutePath());
                            refresh();
                        }else{
                            deleteFile(mList.get(getAdapterPosition()).getAbsolutePath());
                            refresh();
                        }
                        break;
                }
                return true;
            }
        };
    }

    //생성자에서 데이터 리스트 객체를 전달받음.
    MainAdapter(Context context, String rootFolderPath, Updater listener){

        this.mRootFolderPath = rootFolderPath;
        this.mContext = context;
        this.mList = new ArrayList<File>();
        this.mCurrentPath = null;
        this.mCallback = listener;

        init();

    }

    //아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.items_fragment, parent, false);
        MainAdapter.ViewHolder vh = new MainAdapter.ViewHolder(view);

        return vh;
    }


    //position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
    @Override
    public void onBindViewHolder(@NonNull MainAdapter.ViewHolder holder, int position) {
        // 상위 폴더와 하위폴더 구분
        File item = mList.get(position);
        if (item.isDirectory()){
            if(item.getPath().equals(mUpperPath)){
                holder.imageView.setImageResource(R.drawable.ic_folder_primary_54dp);
                holder.textView.setText("..");
            }else{
                holder.imageView.setImageResource(R.drawable.ic_folder_open_primary_54dp);
                holder.textView.setText(item.getName());
            }
        }else{
            try {
                FileReader fileReader = new FileReader(item);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String line01 = bufReader.readLine();
                String line02 = bufReader.readLine();
                Uri mThumbnailUri = Uri.parse(line02);

                Glide.with(mContext).load(mThumbnailUri)
                        .apply(new RequestOptions().circleCrop())
                        .into(holder.imageView);
                holder.textView.setText(item.getName());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //전체 데이터 갯수 리턴
    @Override
    public int getItemCount() {
        return mList.size();
    }

    public boolean init(){
        File rootFolder = new File(mRootFolderPath);
        if(rootFolder.isDirectory() == false) {
            Toast.makeText(mContext, "Not Directory",Toast.LENGTH_LONG).show();
            return false;
        }

        File[] fileList = rootFolder.listFiles();

        if (fileList == null){
            Toast.makeText(mContext, "Could not fine list", Toast.LENGTH_LONG).show();
            return false;
        }

        for (int i = 0 ; i<fileList.length ; i++) {
            mList.add(fileList[i]);
            Collections.sort(mList, new Comparator<File>(){
                @Override
                public int compare(File o1, File o2) {
                    // 폴더 먼저 정렬
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    boolean b = o1.toString().compareToIgnoreCase(o2.toString()) < 0;
                    if (o1.isDirectory() && o2.isDirectory()) {
                        // 알파벳 순으로 정렬
                        if(b){
                            return -1;
                        }else{
                            return 0;
                        }
                    }
                    if (o1.isFile() && o2.isFile()) {
                        // 알파벳 순으로 정렬
                        if(b){
                            return -1;
                        }else{
                            return 0;
                        }
                    }
                    return 1;
                }
            });
        }
        updateCurrentPath();
        updateUpperPath();
        this.mCallback.pathUpdater(mCurrentPath.substring(mRootFolderPath.length()));
        return true;
    }


    public void refresh(){
        File file = new File(mCurrentPath);
        if (file.isDirectory() == false){
            Toast.makeText(mContext,"Not Directory", Toast.LENGTH_SHORT).show();
            return;
        }
        File[] fileList = file.listFiles();
        mList.clear();
        if(this.mCurrentPath != this.mRootFolderPath && mCurrentPath.length() > this.mRootFolderPath.length() ) {
            File upperFolder = new File(mUpperPath);
            this.mList.add(upperFolder);
        }
        for(int i = 0; i < fileList.length; i++){
            this.mList.add(fileList[i]);
        }
        Collections.sort(mList, new Comparator<File>(){
            @Override
            public int compare(File o1, File o2) {
                // 폴더 먼저 정렬
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                boolean b = o1.toString().compareToIgnoreCase(o2.toString()) < 0;
                if (o1.isDirectory() && o2.isDirectory()) {
                    // 알파벳 순으로 정렬
                    if(b){
                        return -1;
                    }else{
                        return 0;
                    }
                }
                if (o1.isFile() && o2.isFile()) {
                    // 알파벳 순으로 정렬
                    if(b){
                        return -1;
                    }else{
                        return 0;
                    }
                }
                return 1;
            }
        });
        this.mCallback.pathUpdater(mCurrentPath.substring(mRootFolderPath.length()));
        this.notifyDataSetChanged();
    }

    public String getCurrentPath(){
        return mCurrentPath;
    }

    public String getRootPath() { return mRootFolderPath; }

    public void updateUpperPath(){
        mUpperPath = mCurrentPath.substring(0,mCurrentPath.lastIndexOf("/"));
    }

    public void prevPath(){

        int lastSlashPosition = mCurrentPath.lastIndexOf("/");
        String prevPath = mCurrentPath.substring(0,lastSlashPosition);
        updateCurrentPathTo(prevPath);
        this.mCallback.pathUpdater(mCurrentPath.substring(mRootFolderPath.length()));
    }

    public void nextPath(String str){

        String nextPath = mCurrentPath+"/"+str;
        updateCurrentPathTo(nextPath);
        this.mCallback.pathUpdater(mCurrentPath.substring(mRootFolderPath.length()));
    }
    public void updateCurrentPath(){
        mCurrentPath = mRootFolderPath;
    }
    public void updateCurrentPathTo(String str){
        this.mCurrentPath = str;
        updateUpperPath();


        File file = new File(str);
        if (file.isDirectory() == false){
            Toast.makeText(mContext,"Not Directory", Toast.LENGTH_SHORT).show();
            return;
        }
        File[] fileList = file.listFiles();
        mList.clear();
        if(this.mCurrentPath != this.mRootFolderPath && mCurrentPath.length() > this.mRootFolderPath.length() ) {
            File upperFolder = new File(mUpperPath);
            this.mList.add(upperFolder);
        }
        for(int i = 0; i < fileList.length; i++){
            this.mList.add(fileList[i]);
        }
        Collections.sort(mList, new Comparator<File>(){
            @Override
            public int compare(File o1, File o2) {
                // 폴더 먼저 정렬
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                boolean b = o1.toString().compareToIgnoreCase(o2.toString()) < 0;
                if (o1.isDirectory() && o2.isDirectory()) {
                    // 알파벳 순으로 정렬
                    if(b){
                        return -1;
                    }else{
                        return 0;
                    }
                }
                if (o1.isFile() && o2.isFile()) {
                    // 알파벳 순으로 정렬
                    if(b){
                        return -1;
                    }else{
                        return 0;
                    }
                }
                return 1;
            }
        });
        this.notifyDataSetChanged();
    }

}
