package com.drc.aidbridge.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.viewbinding.ViewBinding;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;

/**
 * BaseFragment — parent class for all Fragments.
 * @param <VB> The specific ViewBinding type for the subclass Fragment.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    // Backing field to prevent memory leaks
    private VB _binding;
    
    // Safe alias for subclasses to use
    protected VB binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        _binding = inflateBinding(inflater, container, false);
        binding = _binding;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null; // System clears memory
        binding = null;  // Prevent further access
    }

    protected abstract VB inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToRoot);
    protected abstract void setupViews();
    protected abstract void observeViewModel();

    /**
     * Optional progress view to toggle for loading states.
     * Fragments can override and return a ProgressBar from the layout.
     */
    @Nullable
    protected ProgressBar getLoadingView() {
        return null;
    }

    /**
     * Shared observer for NetworkResultWrapper streams.
     * - Loading: disable action view + show optional progress bar
     * - Success: re-enable action view + invoke success callback
     * - Error:   re-enable action view + invoke error callback (or toast fallback)
     */
    protected <T> Observer<NetworkResultWrapper<T>> resultObserver(@Nullable View actionView,
                                                                   @NonNull OnSuccess<T> onSuccess) {
        return resultObserver(actionView, onSuccess, this::showToast);
    }

    protected <T> Observer<NetworkResultWrapper<T>> resultObserver(@Nullable View actionView,
                                                                   @NonNull OnSuccess<T> onSuccess,
                                                                   @NonNull OnError onError) {
        return result -> {
            if (result == null || binding == null) {
                return;
            }

            ProgressBar loadingView = getLoadingView();
            boolean isLoading = result.isLoading();

            if (actionView != null) {
                actionView.setEnabled(!isLoading);
            }
            if (loadingView != null) {
                loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            if (result.isSuccess()) {
                T data = ((NetworkResultWrapper.Success<T>) result).data;
                onSuccess.handle(data);
            } else if (result.isError()) {
                String message = ((NetworkResultWrapper.Error<T>) result).message;
                onError.handle(message != null ? message : "Có lỗi xảy ra");
            }
        };
    }

    @FunctionalInterface
    protected interface OnSuccess<T> {
        void handle(@Nullable T data);
    }

    @FunctionalInterface
    protected interface OnError {
        void handle(@NonNull String message);
    }

    /**
     * Shows a short informational Toast message.
     *
     * @param message The text to display.
     */
    protected void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
