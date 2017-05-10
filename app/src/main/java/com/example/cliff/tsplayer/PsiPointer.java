package com.example.cliff.tsplayer;

import android.util.Log;

/**
 * Created by CLIFF on 2017/5/10.
 */

public class PsiPointer {
    public static final String TAG = "Cliff";

    public int pointer_field; // 8 bits, ISO13818-1 Table 2-24, program specific information pointer

    public PsiPointer(){
        pointer_field = 0;
    }

    void printPsiPointer(){
        Log.i(TAG, String.format("Pointer field            = %d", pointer_field));
    }
}
