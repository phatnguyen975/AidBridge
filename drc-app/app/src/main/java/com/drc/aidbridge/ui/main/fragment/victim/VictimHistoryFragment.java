package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentVictimHistoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimHistoryViewModel;
import com.drc.aidbridge.utils.NetworkUtils;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimHistoryFragment extends BaseFragment<FragmentVictimHistoryBinding> {

    private static final String DETAIL_SHEET_TAG = "VictimHistoryDetailBottomSheet";

    private VictimHistoryViewModel viewModel;
    private VictimHistoryAdapter adapter;
    private int baseRecyclerBottomPadding;

    private boolean isScreenLoading = true;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isOfflineData = false;
    private boolean showRefreshSuccessNotice = false;

    @Override
    protected FragmentVictimHistoryBinding inflateBinding(LayoutInflater inflater,
                                                          ViewGroup container) {
        return FragmentVictimHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimHistoryViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupFilterDropdown();
        setupSwipeRefresh();

        binding.actTimeFilter.setText(getString(R.string.victim_history_filter_1h), false);
        renderEmptyState(false);
        isScreenLoading = true;
        viewModel.loadInitial(isNetworkAvailable());
    }

    @Override
    protected void observeViewModel() {
        viewModel.getHistoryResult().observe(getViewLifecycleOwner(), this::handleHistoryResult);
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecyclerView() {
        adapter = new VictimHistoryAdapter(model ->
            VictimHistoryDetailBottomSheet
                .newInstance(model)
                .show(getParentFragmentManager(), DETAIL_SHEET_TAG)
        );

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);
        baseRecyclerBottomPadding = binding.rvHistory.getPaddingBottom();

        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoading || isLastPage) {
                    return;
                }

                RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                if (!(manager instanceof LinearLayoutManager)) {
                    return;
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0) {
                    isScreenLoading = false;
                    viewModel.loadNextPage(isNetworkAvailable());
                }
            }
        });
    }

    private void setupFilterDropdown() {
        binding.actTimeFilter.setOnItemClickListener((parent, view, position, id) -> {
            adapter.clear();
            isLastPage = false;
            isOfflineData = false;
            isScreenLoading = true;
            showRefreshSuccessNotice = false;
            renderOfflineIndicator(false);
            renderEmptyState(false);
            viewModel.applyTimeRange(mapFilterCode(position), isNetworkAvailable());
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHistory.setOnRefreshListener(() -> {
            adapter.clear();
            isLastPage = false;
            isOfflineData = false;
            isScreenLoading = true;
            showRefreshSuccessNotice = true;
            renderOfflineIndicator(false);
            renderEmptyState(false);
            viewModel.refresh(isNetworkAvailable());
        });
    }

    private void handleHistoryResult(NetworkResultWrapper<VictimHistoryViewModel.HistoryUiPage> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            isLoading = true;
            updatePaginationLoading(true);
            return;
        }

        if (result.hasBeenHandled()) {
            return;
        }

        result.markAsHandled();
        isLoading = false;
        updatePaginationLoading(false);
        binding.swipeRefreshHistory.setRefreshing(false);

        if (result.isError()) {
            showRefreshSuccessNotice = false;
            String message = result.getMessage();
            showTopSnackbar(
                binding.getRoot(),
                message != null && !message.trim().isEmpty()
                    ? message
                    : getString(R.string.victim_history_load_error),
                true
            );
            renderOfflineIndicator(isOfflineData);
            renderEmptyState(isScreenLoading || adapter.getItemCount() == 0);
            return;
        }

        VictimHistoryViewModel.HistoryUiPage uiPage = result.getData();
        if (uiPage == null) {
            showRefreshSuccessNotice = false;
            renderOfflineIndicator(false);
            renderEmptyState(isScreenLoading || adapter.getItemCount() == 0);
            return;
        }

        if (!uiPage.isAppend()) {
            adapter.clear();
        }
        if (!uiPage.getItems().isEmpty()) {
            adapter.addItems(uiPage.getItems());
        }

        isLastPage = uiPage.isLastPage();
        isOfflineData = uiPage.isOfflineData();

        if (showRefreshSuccessNotice) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_history_refresh_success), false);
            showRefreshSuccessNotice = false;
        }

        renderOfflineIndicator(isOfflineData);
        renderEmptyState(adapter.getItemCount() == 0);
    }

    private void updatePaginationLoading(boolean show) {
        if (!show) {
            binding.initialLoadingProgress.setVisibility(View.GONE);
            binding.paginationProgress.setVisibility(View.GONE);
            setTemporaryBottomSpace(false);
            return;
        }

        boolean showBottomLoading = !isScreenLoading;
        binding.initialLoadingProgress.setVisibility(showBottomLoading ? View.GONE : View.VISIBLE);
        binding.paginationProgress.setVisibility(showBottomLoading ? View.VISIBLE : View.GONE);
        setTemporaryBottomSpace(showBottomLoading);
    }

    private void setTemporaryBottomSpace(boolean enabled) {
        int extraSpace = enabled ? getResources().getDimensionPixelSize(R.dimen.spacing_xxl) : 0;
        binding.rvHistory.setPaddingRelative(
            binding.rvHistory.getPaddingStart(),
            binding.rvHistory.getPaddingTop(),
            binding.rvHistory.getPaddingEnd(),
            baseRecyclerBottomPadding + extraSpace
        );
    }

    private void renderOfflineIndicator(boolean isOffline) {
        binding.tvOfflineIndicator.setVisibility(isOffline ? View.VISIBLE : View.GONE);
    }

    private void renderEmptyState(boolean isEmpty) {
        if (!isEmpty) {
            binding.tvEmptyState.setVisibility(View.GONE);
            return;
        }

        binding.tvEmptyState.setText(getString(
            isOfflineData
                ? R.string.victim_history_empty_offline
                : R.string.victim_history_empty
        ));
        binding.tvEmptyState.setVisibility(View.VISIBLE);
    }

    private String mapFilterCode(int position) {
        switch (position) {
            case 1:
                return "24h";
            case 2:
                return "7d";
            case 3:
                return "1m";
            case 4:
                return "all";
            case 0:
            default:
                return "1h";
        }
    }

    private boolean isNetworkAvailable() {
        return NetworkUtils.isConnected(requireContext());
    }
}
