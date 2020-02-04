package com.giyoon.widgetforyoutube.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.giyoon.widgetforyoutube.R;
import com.google.api.services.youtube.model.Subscription;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class WidgetAdapter extends RecyclerView.Adapter<WidgetAdapter.ViewHolder> {

    private List<Subscription> mData;
    Context mContext;
    Bitmap bm;





    public Bitmap getBitmap(int position){

        final Subscription subscription = mData.get(position);

        Thread mThread = new Thread() {
            @Override
            public void run() {

                URL url = null;
                try {
                    url = new URL(subscription.getSnippet().getThumbnails().getDefault().getUrl());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                conn.setDoInput(true);
                try {
                    conn.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                InputStream is = null;
                try {
                    is = conn.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bm = BitmapFactory.decodeStream(is);

            }
        };

        mThread.start();

        try{
            mThread.join();

        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return bm;
    }

    protected WidgetAdapter(List<Subscription> list, Context context){
        mData = list;
        this.mContext = context;
    }

    public Subscription getmItems(int position){
        final Subscription subscription = mData.get(position);
        return subscription;

    }
    public interface OnItemClickListener {
        void onItemClick(View v, int pos);
    }

    private OnItemClickListener mListener = null;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imgView;
        TextView txtView;

        ViewHolder(View itemView){
            super(itemView);

            imgView = itemView.findViewById(R.id.imgview);
            txtView = itemView.findViewById(R.id.txtview);

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION){
                        mListener.onItemClick(v,pos);

                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public WidgetAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.widget_configure_item, parent, false);
        WidgetAdapter.ViewHolder vh = new WidgetAdapter.ViewHolder(view);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Subscription subscription = mData.get(position);

        Glide.with(mContext).load(subscription.getSnippet().getThumbnails().getDefault().getUrl())
                .apply(new RequestOptions().circleCrop())
                .into(holder.imgView);
        holder.txtView.setText(subscription.getSnippet().getTitle());

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

}
