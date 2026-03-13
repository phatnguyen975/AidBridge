package com.drc.aidbridge.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

/**
 * BaseActivity — parent class for all Activities.
 * @param <VB> The specific ViewBinding type for the subclass Activity.
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    protected VB binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
        observeViewModel();
    }

    protected abstract VB inflateBinding(LayoutInflater inflater);
    protected abstract void setupViews();
    protected abstract void observeViewModel();

    /**
     * Shows a short Toast message to the user.
     *
     * @param message The message string to display.
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
