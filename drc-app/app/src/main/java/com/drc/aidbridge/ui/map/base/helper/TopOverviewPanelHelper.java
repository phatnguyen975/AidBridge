package com.drc.aidbridge.ui.map.base.helper;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentMapBaseBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class TopOverviewPanelHelper {

    public interface StateListener {
        void onStateChanged(boolean isExpanded);
    }

    private static final float TOP_OVERVIEW_HALF_EXPANDED_RATIO = 0.62f;

    private boolean isTopOverviewExpanded = false;
    private boolean isTopOverviewAutoHiddenForRoute = false;

    @Nullable
    private BottomSheetBehavior<View> topOverviewBottomSheetBehavior;
    private int defaultTopOverviewPeekHeightPx;
    private int minTopOverviewPeekHeightPx;
    private int currentTopOverviewCollapsedPeekHeightPx;

    private int topOverviewBaseBottomMarginPx;
    private int topOverviewBaseFabBottomMarginPx;
    private int topOverviewBaseRecenterFabBottomMarginPx;

    private int topOverviewSystemTopInsetPx;
    private int topOverviewSystemBottomInsetPx;

    private StateListener listener;

    public void setupTopOverviewBottomSheet(@NonNull FragmentMapBaseBinding binding,
            @NonNull StateListener listener) {
        this.listener = listener;

        try {
            topOverviewBottomSheetBehavior = BottomSheetBehavior.from(binding.cardTopOverview);
            defaultTopOverviewPeekHeightPx = binding.cardTopOverview.getResources()
                    .getDimensionPixelSize(R.dimen.volunteer_map_sheet_peek_height);
            minTopOverviewPeekHeightPx = binding.cardTopOverview.getResources()
                    .getDimensionPixelSize(R.dimen.volunteer_map_sheet_collapsed_min_height);
            currentTopOverviewCollapsedPeekHeightPx = defaultTopOverviewPeekHeightPx;

            ViewGroup.LayoutParams cardParams = binding.cardTopOverview.getLayoutParams();
            if (cardParams instanceof ViewGroup.MarginLayoutParams) {
                topOverviewBaseBottomMarginPx = ((ViewGroup.MarginLayoutParams) cardParams).bottomMargin;
            }

            ViewGroup.LayoutParams fabParams = binding.fabOpenControlPanel.getLayoutParams();
            if (fabParams instanceof ViewGroup.MarginLayoutParams) {
                topOverviewBaseFabBottomMarginPx = ((ViewGroup.MarginLayoutParams) fabParams).bottomMargin;
            }

            ViewGroup.LayoutParams recenterFabParams = binding.fabRecenterCurrentLocation.getLayoutParams();
            if (recenterFabParams instanceof ViewGroup.MarginLayoutParams) {
                topOverviewBaseRecenterFabBottomMarginPx = ((ViewGroup.MarginLayoutParams) recenterFabParams).bottomMargin;
            }

            topOverviewBottomSheetBehavior.setHideable(true);
            topOverviewBottomSheetBehavior.setSkipCollapsed(true);
            topOverviewBottomSheetBehavior.setDraggable(true);
            topOverviewBottomSheetBehavior.setFitToContents(true);
            topOverviewBottomSheetBehavior.setHalfExpandedRatio(TOP_OVERVIEW_HALF_EXPANDED_RATIO);
            topOverviewBottomSheetBehavior.setPeekHeight(0);
            topOverviewBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState != BottomSheetBehavior.STATE_EXPANDED
                            && newState != BottomSheetBehavior.STATE_HIDDEN) {
                        return;
                    }
                    isTopOverviewExpanded = newState == BottomSheetBehavior.STATE_EXPANDED;
                    if (TopOverviewPanelHelper.this.listener != null) {
                        TopOverviewPanelHelper.this.listener.onStateChanged(isTopOverviewExpanded);
                    }
                    applyTopOverviewState(binding);
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });

            installTopOverviewInsetsHandling(binding);

            if (topOverviewBottomSheetBehavior != null) {
                applyResponsiveTopOverviewLayout(binding);
                topOverviewBottomSheetBehavior.setState(
                        isTopOverviewExpanded
                                ? BottomSheetBehavior.STATE_EXPANDED
                                : BottomSheetBehavior.STATE_HIDDEN);
                updateOpenControlFabVisibility(binding);
            }
        } catch (Exception e) {
            topOverviewBottomSheetBehavior = null;
        }
    }

    public void toggleTopOverviewPanel(@NonNull FragmentMapBaseBinding binding) {
        isTopOverviewAutoHiddenForRoute = false;
        isTopOverviewExpanded = !isTopOverviewExpanded;
        if (listener != null) {
            listener.onStateChanged(isTopOverviewExpanded);
        }
        applyTopOverviewState(binding);
    }

    public void applyTopOverviewState(@NonNull FragmentMapBaseBinding binding) {
        try {
            if (isTopOverviewAutoHiddenForRoute) {
                binding.cardTopOverview.setVisibility(View.GONE);
                updateOpenControlFabVisibility(binding);
                return;
            }

            binding.cardTopOverview.setVisibility(View.VISIBLE);
            binding.layoutTopOverviewContent.setVisibility(View.VISIBLE);
            binding.btnToggleTopPanel.setRotation(isTopOverviewExpanded ? 0f : 180f);

            if (topOverviewBottomSheetBehavior != null) {
                applyResponsiveTopOverviewLayout(binding);

                int targetState = isTopOverviewExpanded
                        ? BottomSheetBehavior.STATE_EXPANDED
                        : BottomSheetBehavior.STATE_HIDDEN;
                if (topOverviewBottomSheetBehavior.getState() != targetState) {
                    topOverviewBottomSheetBehavior.setState(targetState);
                }
            }
            updateOpenControlFabVisibility(binding);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void setTopOverviewAutoHiddenForRoute(@NonNull FragmentMapBaseBinding binding, boolean autoHide) {
        this.isTopOverviewAutoHiddenForRoute = autoHide;
        if (autoHide) {
            this.isTopOverviewExpanded = false;
            if (listener != null) {
                listener.onStateChanged(false);
            }
            binding.layoutTopOverviewContent.setVisibility(View.GONE);
            binding.btnToggleTopPanel.setRotation(180f);
            binding.cardTopOverview.setVisibility(View.GONE);

            if (topOverviewBottomSheetBehavior != null
                    && topOverviewBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                topOverviewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        } else {
            binding.cardTopOverview.setVisibility(View.VISIBLE);
            applyTopOverviewState(binding);
        }
        updateOpenControlFabVisibility(binding);
    }

    public void updateOpenControlFabVisibility(@NonNull FragmentMapBaseBinding binding) {
        try {
            boolean isNavOrSimActive = binding.cardNavigationHud.getVisibility() == View.VISIBLE;
            if (isNavOrSimActive || isTopOverviewExpanded) {
                binding.fabOpenControlPanel.setVisibility(View.GONE);
                binding.btnSetStartPoint.setVisibility(View.GONE);
                binding.btnSetEndPoint.setVisibility(View.GONE);
            } else {
                binding.fabOpenControlPanel.setVisibility(View.VISIBLE);
                binding.btnSetStartPoint.setVisibility(View.VISIBLE);
                binding.btnSetEndPoint.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public void openTopOverviewPanelFromFab(@NonNull FragmentMapBaseBinding binding) {
        isTopOverviewAutoHiddenForRoute = false;
        isTopOverviewExpanded = true;
        if (listener != null) {
            listener.onStateChanged(true);
        }
        applyTopOverviewState(binding);
    }

    public void ensureControlPanelAvailable(@NonNull FragmentMapBaseBinding binding) {
        if (isTopOverviewAutoHiddenForRoute) {
            setTopOverviewAutoHiddenForRoute(binding, false);
        }
        if (!isTopOverviewExpanded) {
            isTopOverviewExpanded = true;
            if (listener != null) {
                listener.onStateChanged(true);
            }
            applyTopOverviewState(binding);
        }
    }

    public boolean isExpanded() {
        return isTopOverviewExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isTopOverviewExpanded = expanded;
    }

    public boolean isAutoHidden() {
        return isTopOverviewAutoHiddenForRoute;
    }

    private void installTopOverviewInsetsHandling(@NonNull FragmentMapBaseBinding binding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topOverviewSystemTopInsetPx = systemBarsInsets.top;
            topOverviewSystemBottomInsetPx = systemBarsInsets.bottom;
            applyResponsiveTopOverviewLayout(binding);
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private void applyResponsiveTopOverviewLayout(@NonNull FragmentMapBaseBinding binding) {
        if (topOverviewBottomSheetBehavior == null) {
            return;
        }

        // Card margin is now fixed in XML to be flush with bottom
        /*
        ViewGroup.LayoutParams cardParams = binding.cardTopOverview.getLayoutParams();
        if (cardParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) cardParams;
            int targetBottomMargin = topOverviewBaseBottomMarginPx + topOverviewSystemBottomInsetPx;
            if (marginParams.bottomMargin != targetBottomMargin) {
                marginParams.bottomMargin = targetBottomMargin;
                binding.cardTopOverview.setLayoutParams(marginParams);
            }
        }
        */

        ViewGroup.LayoutParams fabParams = binding.fabOpenControlPanel.getLayoutParams();
        if (fabParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) fabParams;
            int targetFabBottomMargin = topOverviewBaseFabBottomMarginPx + topOverviewSystemBottomInsetPx;
            if (marginParams.bottomMargin != targetFabBottomMargin) {
                marginParams.bottomMargin = targetFabBottomMargin;
                binding.fabOpenControlPanel.setLayoutParams(marginParams);
            }
        }

        ViewGroup.LayoutParams recenterFabParams = binding.fabRecenterCurrentLocation.getLayoutParams();
        if (recenterFabParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) recenterFabParams;
            int targetRecenterFabBottomMargin = topOverviewBaseRecenterFabBottomMarginPx
                    + topOverviewSystemBottomInsetPx;
            if (marginParams.bottomMargin != targetRecenterFabBottomMargin) {
                marginParams.bottomMargin = targetRecenterFabBottomMargin;
                binding.fabRecenterCurrentLocation.setLayoutParams(marginParams);
            }
        }
    }
}
