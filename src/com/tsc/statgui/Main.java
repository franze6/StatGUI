package com.tsc.statgui;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        File config = new File("config\\config.json");
        float freq =0.1f;

        if(config.exists()) {
            MainForm mainForm = new MainForm(config.getAbsolutePath());
        }

    }
}
