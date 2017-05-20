package com.xiaoyi.yi360demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by jen on 21/05/2017.
 */

public class Settings extends Activity {

    private Spinner colormode, ev, fov, iso, meteringmode, resolution, quality, sharpness, shuttertime, whitebalance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_settings);
        colormode = (Spinner) findViewById(R.id.colormode);
        ev = (Spinner) findViewById(R.id.ev);
        fov = (Spinner) findViewById(R.id.fov);
        iso = (Spinner) findViewById(R.id.iso);
        meteringmode = (Spinner) findViewById(R.id.meteringmode);
        resolution = (Spinner) findViewById(R.id.resolution);
        quality = (Spinner) findViewById(R.id.quality);
        sharpness = (Spinner) findViewById(R.id.sharpness);
        shuttertime = (Spinner) findViewById(R.id.shuttertime);
        whitebalance = (Spinner) findViewById(R.id.whitebalance);
    }

    @Override
    public void updateSettings() {
        colormode.getSelectedItemPosition();
    }
}
