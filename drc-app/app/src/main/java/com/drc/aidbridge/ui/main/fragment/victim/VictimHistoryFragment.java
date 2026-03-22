package com.drc.aidbridge.ui.main.fragment.victim;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimHistoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimHistoryFragment extends BaseFragment<FragmentVictimHistoryBinding> {

    private VictimHistoryAdapter adapter;

    private int currentPage = 1;
    private String currentFilter = "1H";
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected FragmentVictimHistoryBinding inflateBinding(android.view.LayoutInflater inflater,
                                                          android.view.ViewGroup container) {
        return FragmentVictimHistoryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupToolbar();
        setupRecyclerView();
        setupFilterDropdown();
        loadData();
    }

    @Override
    protected void observeViewModel() {
        /*
         * TODO: API integration block.
         *
         * viewModel.getHistoryResult().observe(getViewLifecycleOwner(), result -> {
         *     if (result == null) {
         *         return;
         *     }
         *
         *     if (result.isLoading()) {
         *         isLoading = true;
         *         binding.paginationProgress.setVisibility(View.VISIBLE);
         *         return;
         *     }
         *
         *     if (result.hasBeenHandled()) {
         *         return;
         *     }
         *
         *     if (result.isSuccess()) {
         *         result.markAsHandled();
         *         isLoading = false;
         *         binding.paginationProgress.setVisibility(View.GONE);
         *
         *         HistoryPageResponse page = ((NetworkResultWrapper.Success<HistoryPageResponse>) result).data;
         *         if (page == null || page.getItems() == null || page.getItems().isEmpty()) {
         *             isLastPage = true;
         *             return;
         *         }
         *
         *         // Append data for pagination instead of replacing the entire list.
         *         List<VictimHistoryAdapter.HistoryModel> mapped = mapper.map(page.getItems());
         *         adapter.addItems(mapped);
         *         isLastPage = !page.hasNextPage();
         *         return;
         *     }
         *
         *     if (result.isError()) {
         *         result.markAsHandled();
         *         isLoading = false;
         *         binding.paginationProgress.setVisibility(View.GONE);
         *         String message = ((NetworkResultWrapper.Error<HistoryPageResponse>) result).message;
         *         showToast(message != null ? message : getString(R.string.error_generic));
         *     }
         * });
         */
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());
    }

    private void setupRecyclerView() {
        adapter = new VictimHistoryAdapter(model -> {
            VictimHistoryDetailBottomSheet bottomSheet = new VictimHistoryDetailBottomSheet();
            bottomSheet.show(getParentFragmentManager(), "VictimHistoryDetailBottomSheet");
        });

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoading || isLastPage) {
                    return;
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) {
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    currentPage++;
                    loadData();
                }
            }
        });
    }

    private void setupFilterDropdown() {
        String[] filterItems = new String[]{
                getString(R.string.victim_history_filter_1h),
                getString(R.string.victim_history_filter_24h),
                getString(R.string.victim_history_filter_7d),
                getString(R.string.victim_history_filter_1m),
                getString(R.string.victim_history_filter_all)
        };

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                filterItems
        );

        binding.actvFilter.setAdapter(filterAdapter);
        binding.actvFilter.setText(getString(R.string.victim_history_filter_1h), false);

        binding.actvFilter.setOnItemClickListener((parent, view, position, id) -> {
            adapter.clear();
            currentPage = 1;
            isLastPage = false;
            currentFilter = mapFilterCode(position);
            loadData();
        });
    }

    private String mapFilterCode(int position) {
        switch (position) {
            case 1:
                return "24H";
            case 2:
                return "7D";
            case 3:
                return "1M";
            case 4:
                return "ALL";
            case 0:
            default:
                return "1H";
        }
    }

    private void loadData() {
        if (isLoading || isLastPage) {
            return;
        }

        isLoading = true;
        binding.paginationProgress.setVisibility(android.view.View.VISIBLE);

        List<VictimHistoryAdapter.HistoryModel> pageData = generateDummyData(currentPage, currentFilter);
        if (pageData.isEmpty()) {
            isLastPage = true;
        } else {
            adapter.addItems(pageData);
            if (currentPage >= 4) {
                isLastPage = true;
            }
        }

        isLoading = false;
        binding.paginationProgress.setVisibility(android.view.View.GONE);
    }

    private List<VictimHistoryAdapter.HistoryModel> generateDummyData(int page, String filter) {
        List<VictimHistoryAdapter.HistoryModel> list = new ArrayList<>();

        String statusPending = getString(R.string.victim_history_status_pending);
        String statusProcessing = getString(R.string.victim_history_status_processing);
        String statusCompleted = getString(R.string.victim_history_status_completed);

        list.add(new VictimHistoryAdapter.HistoryModel(
                "REQ-" + page + "-01",
                getString(R.string.victim_history_title_sos_self),
                statusPending,
            VictimHistoryAdapter.HistoryModel.STATUS_PENDING,
                getString(R.string.victim_history_dummy_date_1),
                getString(R.string.victim_history_dummy_location_1, filter),
                VictimHistoryAdapter.TYPE_SOS_SELF
        ));

        list.add(new VictimHistoryAdapter.HistoryModel(
                "REQ-" + page + "-02",
                getString(R.string.victim_history_title_supply),
                statusProcessing,
            VictimHistoryAdapter.HistoryModel.STATUS_PROCESSING,
                getString(R.string.victim_history_dummy_date_2),
                getString(R.string.victim_history_dummy_location_2, filter),
                VictimHistoryAdapter.TYPE_SUPPLY
        ));

        list.add(new VictimHistoryAdapter.HistoryModel(
                "REQ-" + page + "-03",
                getString(R.string.victim_history_title_sos_relative),
                statusCompleted,
            VictimHistoryAdapter.HistoryModel.STATUS_COMPLETED,
                getString(R.string.victim_history_dummy_date_3),
                getString(R.string.victim_history_dummy_location_3, filter),
                VictimHistoryAdapter.TYPE_SOS_RELATIVE
        ));

        list.add(new VictimHistoryAdapter.HistoryModel(
                "REQ-" + page + "-04",
                getString(R.string.victim_history_title_supply),
                statusPending,
            VictimHistoryAdapter.HistoryModel.STATUS_PENDING,
                getString(R.string.victim_history_dummy_date_4),
                getString(R.string.victim_history_dummy_location_4, filter),
                VictimHistoryAdapter.TYPE_SUPPLY
        ));

        list.add(new VictimHistoryAdapter.HistoryModel(
                "REQ-" + page + "-05",
                getString(R.string.victim_history_title_sos_self),
                statusProcessing,
            VictimHistoryAdapter.HistoryModel.STATUS_PROCESSING,
                getString(R.string.victim_history_dummy_date_5),
                getString(R.string.victim_history_dummy_location_5, filter),
                VictimHistoryAdapter.TYPE_SOS_SELF
        ));

        return list;
    }
}
