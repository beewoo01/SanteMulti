package com.physiolab.sante.santemulti;

interface SaveFileListener {
    void onSuccess(int device);
    void onFail();
}
