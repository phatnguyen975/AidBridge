package com.drc.aidbridge.ui.base;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.viewbinding.ViewBinding;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * BaseFragment — parent class for all Fragments.
 * @param <VB> The specific ViewBinding type for the subclass Fragment.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    // Backing field to prevent memory leaks
    private VB _binding;
    // Safe alias for subclasses to use
    protected VB binding;

    private long lastNavigateAtMs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        _binding = inflateBinding(inflater, container);
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

    protected abstract VB inflateBinding(LayoutInflater inflater, @Nullable ViewGroup container);
    protected abstract void setupViews();
    protected abstract void observeViewModel();

    /**
     * Template hook for child fragments to control their loading UI.
     */
    protected void onLoadingStateChanged(boolean isLoading) {
    }

    /**
     * Shared observer for NetworkResultWrapper streams.
     * - Loading: delegate state handling to child fragments
     * - Success: invoke success callback
     * - Error: invoke error callback (or toast fallback)
     */
    protected <T> Observer<NetworkResultWrapper<T>> resultObserver(@NonNull OnSuccess<T> onSuccess) {
        return resultObserver(onSuccess, this::showToast);
    }

    protected <T> Observer<NetworkResultWrapper<T>> resultObserver(@NonNull OnSuccess<T> onSuccess,
                                                                   @NonNull OnError onError) {
        return result -> {
            if (result == null) {
                return;
            }

            boolean isLoading = result.isLoading();
            onLoadingStateChanged(isLoading);

            if (result.hasBeenHandled() && !isLoading) {
                return;
            }

            if (result.isSuccess()) {
                result.markAsHandled();
                T data = ((NetworkResultWrapper.Success<T>) result).data;
                onSuccess.handle(data);
            } else if (result.isError()) {
                result.markAsHandled();
                String message = ((NetworkResultWrapper.Error<T>) result).message;
                onError.handle(message != null ? message : getString(R.string.error_generic));
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

    /**
     * Shows a Snackbar at the top of the screen with custom styling.
     *
     * @param parentView The view to find a parent from for the Snackbar.
     * @param message The text to display in the Snackbar.
     * @param isError Whether the message represents an error (affects styling).
     */
    protected void showTopSnackbar(@NonNull View parentView,
                                   @NonNull String message,
                                   boolean isError) {
        View root = requireActivity().findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();

        int bgColor = ContextCompat.getColor(requireContext(), isError ? R.color.sos_red : R.color.safe_green);

        // Force-disable Material background tint so custom shape color is always visible.
        snackbar.setBackgroundTint(Color.TRANSPARENT);
        snackbarView.setBackgroundTintList(null);

        GradientDrawable background = new GradientDrawable();
        background.setColor(bgColor);
        background.setCornerRadius(getResources().getDimension(R.dimen.radius_sm));
        snackbarView.setBackground(background);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(5);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setTextColor(Color.WHITE);
        }

        int topMargin = getResources().getDimensionPixelSize(R.dimen.snackbar_top_margin);
        ViewGroup.LayoutParams layoutParams = snackbarView.getLayoutParams();

        if (layoutParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) layoutParams;
            params.gravity = Gravity.TOP;
            params.topMargin = topMargin;
            snackbarView.setLayoutParams(params);
        } else if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layoutParams;
            params.gravity = Gravity.TOP;
            params.topMargin = topMargin;
            snackbarView.setLayoutParams(params);
        }

        snackbar.show();
    }

    protected void applyActionLoadingState(@NonNull MaterialButton button,
                                           @Nullable View progressView,
                                           @Nullable TextView labelView,
                                           boolean isLoading,
                                           @StringRes int normalTextResId) {
        button.setEnabled(!isLoading);

        int buttonColor = ContextCompat.getColor(
            requireContext(),
            isLoading ? R.color.color_primary_variant : R.color.color_primary
        );
        button.setBackgroundTintList(ColorStateList.valueOf(buttonColor));

        int loadingTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int normalTextColor = ContextCompat.getColor(requireContext(), R.color.white);
        int effectiveTextColor = isLoading ? loadingTextColor : normalTextColor;

        if (progressView != null) {
            progressView.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (progressView instanceof ProgressBar) {
                ((ProgressBar) progressView).setIndeterminateTintList(
                    ColorStateList.valueOf(effectiveTextColor)
                );
            }
        }

        if (labelView != null) {
            labelView.setText(isLoading ? R.string.btn_loading : normalTextResId);
            labelView.setTextColor(effectiveTextColor);
        }
    }

    protected void clearInputFocusAndHideKeyboard() {
        if (!isAdded()) {
            return;
        }

        View currentFocus = requireActivity().getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }

        View rootView = getView();
        if (rootView != null) {
            rootView.clearFocus();
        }
    }

    /**
     * Gets the NavController associated with the fragment's view.
     *
     * @return The NavController, or null if not available.
     */
    @Nullable
    protected NavController getViewNavController() {
        View view = getView();
        if (view == null) {
            return null;
        }

        try {
            return Navigation.findNavController(view);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    /**
     * Gets the NavController associated with the fragment's host.
     *
     * @param hostId The ID of the host fragment.
     * @return The NavController, or null if not available.
     */
    @Nullable
    protected NavController getHostNavController(@IdRes int hostId) {
        if (!isAdded()) {
            return null;
        }

        try {
            return Navigation.findNavController(requireActivity(), hostId);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     * 
     * @param actionId The ID of the navigation action.
     */
    protected boolean navigateSafely(@IdRes int actionId) {
        return navigateSafely(actionId, null);
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param actionId The ID of the navigation action.
     * @param args Optional Bundle of arguments to pass to the destination.
     */
    protected boolean navigateSafely(@IdRes int actionId, @Nullable Bundle args) {
        NavController navController = getViewNavController();
        if (navController == null) {
            return false;
        }
        return navigateSafely(navController, actionId, args);
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param navController The NavController to use for navigation.
     * @param actionId The ID of the navigation action.
     * @return true if navigation was successful, false otherwise.
     */
    protected boolean navigateSafely(@NonNull NavController navController, @IdRes int actionId) {
        return navigateSafely(navController, actionId, null);
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param navController The NavController to use for navigation.
     * @param actionId The ID of the navigation action.
     * @param args Optional Bundle of arguments to pass to the destination.
     * @return true if navigation was successful, false otherwise.
     */
    protected boolean navigateSafely(@NonNull NavController navController,
                                     @IdRes int actionId,
                                     @Nullable Bundle args) {
        if (!canNavigateNow()) {
            return false;
        }

        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination == null || currentDestination.getAction(actionId) == null) {
            return false;
        }

        try {
            if (args != null) {
                navController.navigate(actionId, args);
            } else {
                navController.navigate(actionId);
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param destinationId The ID of the destination.
     * @return true if navigation was successful, false otherwise.
     */
    protected boolean navigateToDestinationSafely(@IdRes int destinationId) {
        NavController navController = getViewNavController();
        if (navController == null) {
            return false;
        }
        return navigateToDestinationSafely(navController, destinationId);
    }

    /**
     * Safely navigates to a destination using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param navController The NavController to use for navigation.
     * @param destinationId The ID of the destination.
     * @return true if navigation was successful, false otherwise.
     */
    protected boolean navigateToDestinationSafely(@NonNull NavController navController,
                                                  @IdRes int destinationId) {
        if (!canNavigateNow()) {
            return false;
        }

        if (navController.getGraph().findNode(destinationId) == null) {
            return false;
        }

        try {
            navController.navigate(destinationId);
            return true;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Safely pops the back stack using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @return true if the back stack was popped successfully, false otherwise.
     */
    protected boolean popBackStackSafely() {
        NavController navController = getViewNavController();
        if (navController == null) {
            return false;
        }

        try {
            return navController.popBackStack();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Safely pops the back stack using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param destinationId The ID of the destination to pop back to.
     * @param inclusive Whether to include the destination in the back stack.
     * @return true if the back stack was popped successfully, false otherwise.
     */
    protected boolean popBackStackSafely(@IdRes int destinationId, boolean inclusive) {
        NavController navController = getViewNavController();
        if (navController == null) {
            return false;
        }

        try {
            return navController.popBackStack(destinationId, inclusive);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Safely pops the back stack using the NavController, with checks to prevent crashes from invalid states or actions.
     *
     * @param navController The NavController to use for popping the back stack.
     * @param destinationId The ID of the destination to pop back to.
     * @param inclusive Whether to include the destination in the back stack.
     * @return true if the back stack was popped successfully, false otherwise.
     */
    protected boolean popBackStackSafely(@NonNull NavController navController,
                                         @IdRes int destinationId,
                                         boolean inclusive) {
        try {
            return navController.popBackStack(destinationId, inclusive);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Checks if navigation actions can be performed at the current time, with a debounce to prevent rapid repeated navigations.
     *
     * @return true if navigation can proceed, false if it should be blocked due to debounce.
     */
    private boolean canNavigateNow() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavigateAtMs < Constants.NAVIGATE_DEBOUNCE_MS) {
            return false;
        }
        lastNavigateAtMs = now;
        return true;
    }
}
