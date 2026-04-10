package com.drc.aidbridge.ui.map.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentMapVolunteerBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMapFragment extends BaseFragment<FragmentMapVolunteerBinding> implements OnMapReadyCallback {

	private GoogleMap mMap;
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

	@Nullable
	@Override
	protected FragmentMapVolunteerBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
		return FragmentMapVolunteerBinding.inflate(inflater, container, false);
	}

	@Override
	protected void setupViews() {
		// 1. Tìm và khởi tạo SupportMapFragment từ layout
		// Lưu ý: Đảm bảo trong fragment_map_volunteer.xml bạn đã có một <fragment> hoặc
		// <FragmentContainerView> với ID là R.id.map
		SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
				.findFragmentById(R.id.map);

		if (mapFragment != null) {
			mapFragment.getMapAsync(this);
		}
	}

	@Override
	public void onMapReady(@NonNull GoogleMap googleMap) {
		mMap = googleMap;

		// 2. Kích hoạt tính năng vị trí hiện tại
		enableMyLocation();
	}

	private void enableMyLocation() {
		// Kiểm tra quyền ACCESS_FINE_LOCATION
		if (ActivityCompat.checkSelfPermission(requireContext(),
				Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

			// Nếu chưa có quyền, yêu cầu người dùng cấp quyền
			requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
					LOCATION_PERMISSION_REQUEST_CODE);
			return;
		}

		if (mMap != null) {
			// 3. Hiển thị dấu chấm xanh vị trí hiện tại và nút "My Location"
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setMyLocationButtonEnabled(true);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				enableMyLocation();
			} else {
				Toast.makeText(requireContext(), "Bạn cần cấp quyền vị trí để xem bản đồ", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void observeViewModel() {
		// Xử lý dữ liệu từ ViewModel sau này (ví dụ: danh sách các ca cứu trợ)
	}
}