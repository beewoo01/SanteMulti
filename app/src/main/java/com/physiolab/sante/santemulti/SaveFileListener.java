package com.physiolab.sante.santemulti;

interface SaveFileListener {
    void onSuccess(int device, int percent);
    void onFail();
}
