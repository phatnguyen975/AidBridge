package com.drc.aidbridge.ui.base;

import androidx.lifecycle.ViewModel;

// import io.reactivex.rxjava3.disposables.CompositeDisposable;
// import io.reactivex.rxjava3.disposables.Disposable;

/**
 * BaseViewModel — parent class for all ViewModels.
 * Manages RxJava3 Disposables to prevent memory leaks for background streams (e.g., WebSocket).
 */
public abstract class BaseViewModel extends ViewModel {

    // private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * Adds an RxJava Disposable to be automatically cleared when ViewModel dies.
     */
    // protected void addDisposable(Disposable disposable) {
    //     compositeDisposable.add(disposable);
    // }

    @Override
    protected void onCleared() {
        super.onCleared();
        // if (!compositeDisposable.isDisposed()) {
        //     compositeDisposable.clear();
        // }
    }
}
