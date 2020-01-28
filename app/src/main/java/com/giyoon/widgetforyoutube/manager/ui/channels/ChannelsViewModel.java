package com.giyoon.widgetforyoutube.manager.ui.channels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChannelsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ChannelsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}