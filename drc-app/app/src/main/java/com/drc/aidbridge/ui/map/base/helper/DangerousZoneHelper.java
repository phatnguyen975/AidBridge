package com.drc.aidbridge.ui.map.base.helper;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.databinding.FragmentMapAdminBinding;
import com.drc.aidbridge.databinding.FragmentMapBaseBinding;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DangerousZoneHelper {

    private boolean isZonesVisible = true;
    
    public interface OnDangerousZoneListener {
        void onZoneSaved(@Nullable UUID id, String name, List<GeoPoint> points);
        void onDeleteZone(UUID id);
        void onEditModeChanged(boolean isEditing);
    }

    private final MapView mapView;
    private final FragmentMapAdminBinding adminBinding;
    private final FragmentMapBaseBinding baseBinding;
    private final OnDangerousZoneListener listener;

    private Polygon currentEditingPolygon;
    private List<GeoPoint> currentPoints = new ArrayList<>();
    private List<Marker> vertexMarkers = new ArrayList<>();
    private List<Marker> ghostMarkers = new ArrayList<>();
    private List<Polygon> permanentPolygons = new ArrayList<>();
    private Polygon activeEditingPolygonObject = null;
    private UUID currentEditingId = null;
    private boolean isEditing = false;

    public DangerousZoneHelper(@NonNull MapView mapView,
                              @NonNull FragmentMapAdminBinding adminBinding,
                              @NonNull FragmentMapBaseBinding baseBinding,
                              @NonNull OnDangerousZoneListener listener) {
        this.mapView = mapView;
        this.adminBinding = adminBinding;
        this.baseBinding = baseBinding;
        this.listener = listener;
        setupListeners();
        setupGlobalMapEvents();
    }

    private void setupGlobalMapEvents() {
        org.osmdroid.events.MapEventsReceiver mReceive = new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (isEditing) return false;
                
                for (Polygon poly : permanentPolygons) {
                    if (isPointInPolygon(p, poly.getActualPoints())) {
                        UUID id = null;
                        try {
                            id = UUID.fromString(poly.getId());
                        } catch (Exception ignored) {}
                        enterEditMode(id, poly.getTitle(), poly.getActualPoints(), poly);
                        permanentPolygons.remove(poly);
                        return true;
                    }
                }
                return false;
            }
        };

        org.osmdroid.views.overlay.MapEventsOverlay eventsOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(mReceive);
        mapView.getOverlayManager().add(eventsOverlay);
    }

    private void setupListeners() {
        adminBinding.btnManageDangerousZones.setOnClickListener(v -> enterEditMode());
        adminBinding.btnDrawingAddPoint.setOnClickListener(v -> addPointAtCrosshair());
        adminBinding.btnDrawingUndo.setOnClickListener(v -> undoLastPoint());
        adminBinding.btnDrawingCancel.setOnClickListener(v -> exitEditMode(false));
        adminBinding.btnDrawingSave.setOnClickListener(v -> exitEditMode(true));
        adminBinding.btnDrawingDeleteZone.setOnClickListener(v -> deleteCurrentZone());
    }

    private void deleteCurrentZone() {
        if (currentEditingId != null) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(mapView.getContext())
                    .setTitle("Xóa vùng nguy hiểm")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ vùng này không? Thao tác này không thể hoàn tác.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (listener != null) listener.onDeleteZone(currentEditingId);
                        exitEditMode(false);
                    })
                    .show();
        }
    }

    public void enterEditMode() {
        enterEditMode(null, "", new ArrayList<>(), null);
    }

    public void enterEditMode(@Nullable UUID id, String name, @NonNull List<GeoPoint> points, @Nullable Polygon existingPolygon) {
        isEditing = true;
        currentPoints = new ArrayList<>(points);
        activeEditingPolygonObject = existingPolygon;
        currentEditingId = id;
        
        adminBinding.etDrawingName.setText(name);
        
        if (existingPolygon != null) {
            mapView.getOverlayManager().remove(existingPolygon);
            adminBinding.btnDrawingDeleteZone.setVisibility(View.VISIBLE);
        } else {
            adminBinding.btnDrawingDeleteZone.setVisibility(View.GONE);
        }
        
        rebuildAllMarkers();
        updatePolygon();
        
        setBaseMapUIVisibility(View.GONE);
        adminBinding.cardRoutingMenu.setVisibility(View.GONE);
        adminBinding.btnToggleRoutingMenu.setVisibility(View.GONE);
        
        adminBinding.cardDrawingToolbar.setVisibility(View.VISIBLE);
        adminBinding.cardDrawingName.setVisibility(View.VISIBLE);
        adminBinding.ivDrawingCrosshair.setVisibility(View.VISIBLE);
        
        if (listener != null) listener.onEditModeChanged(true);
    }

    private void exitEditMode(boolean shouldSave) {
        if (shouldSave) {
            if (currentPoints.size() < 3) {
                Toast.makeText(mapView.getContext(), "Vùng phải có ít nhất 3 điểm", Toast.LENGTH_SHORT).show();
                return;
            }
            String name = adminBinding.etDrawingName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(mapView.getContext(), "Vui lòng nhập tên vùng", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (listener != null) listener.onZoneSaved(currentEditingId, name, new ArrayList<>(currentPoints));
        } else {
            if (activeEditingPolygonObject != null) {
                addPermanentPolygon(activeEditingPolygonObject);
            }
        }

        isEditing = false;
        activeEditingPolygonObject = null;
        currentEditingId = null;
        currentPoints.clear();
        clearMarkers();
        if (currentEditingPolygon != null) {
            mapView.getOverlayManager().remove(currentEditingPolygon);
            currentEditingPolygon = null;
        }

        setBaseMapUIVisibility(View.VISIBLE);
        adminBinding.cardRoutingMenu.setVisibility(View.VISIBLE);
        adminBinding.btnToggleRoutingMenu.setVisibility(View.VISIBLE);
        
        adminBinding.cardDrawingToolbar.setVisibility(View.GONE);
        adminBinding.cardDrawingName.setVisibility(View.GONE);
        adminBinding.ivDrawingCrosshair.setVisibility(View.GONE);
        
        if (listener != null) listener.onEditModeChanged(false);
        mapView.invalidate();
    }

    private void addPointAtCrosshair() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        currentPoints.add(center);
        updatePolygon();
        addVertexMarker(center);
    }

    private void undoLastPoint() {
        if (!currentPoints.isEmpty()) {
            currentPoints.remove(currentPoints.size() - 1);
            if (!vertexMarkers.isEmpty()) {
                mapView.getOverlayManager().remove(vertexMarkers.remove(vertexMarkers.size() - 1));
            }
            updatePolygon();
        }
    }

    private void updatePolygon() {
        if (currentEditingPolygon != null) {
            mapView.getOverlayManager().remove(currentEditingPolygon);
        }

        if (currentPoints.size() >= 2) {
            currentEditingPolygon = new Polygon();
            currentEditingPolygon.setPoints(currentPoints);
            currentEditingPolygon.setFillColor(Color.argb(75, 255, 87, 34));
            currentEditingPolygon.setStrokeColor(Color.parseColor("#FF5722"));
            currentEditingPolygon.setStrokeWidth(5f);
            mapView.getOverlayManager().add(currentEditingPolygon);
        }
        
        updateGhostMarkers();
        mapView.invalidate();
    }

    private void updateGhostMarkers() {
        for (Marker m : ghostMarkers) {
            mapView.getOverlayManager().remove(m);
        }
        ghostMarkers.clear();

        if (currentPoints.size() < 2) return;

        for (int i = 0; i < currentPoints.size(); i++) {
            GeoPoint p1 = currentPoints.get(i);
            GeoPoint p2 = currentPoints.get((i + 1) % currentPoints.size());
            
            if (!isEditing && i == currentPoints.size() - 1) break; 
            
            GeoPoint mid = new GeoPoint(
                    (p1.getLatitude() + p2.getLatitude()) / 2,
                    (p1.getLongitude() + p2.getLongitude()) / 2
            );

            Marker ghost = new Marker(mapView);
            ghost.setPosition(mid);
            ghost.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            ghost.setAlpha(0.5f);
            ghost.setIcon(mapView.getContext().getDrawable(com.drc.aidbridge.R.drawable.ic_vertex_dot));
            ghost.setInfoWindow(null);
            
            final int insertIndex = i + 1;
            ghost.setDraggable(true);
            ghost.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {}

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    currentPoints.add(insertIndex, marker.getPosition());
                    rebuildAllMarkers();
                    updatePolygon();
                }

                @Override
                public void onMarkerDragStart(Marker marker) {}
            });
            
            ghostMarkers.add(ghost);
            mapView.getOverlayManager().add(ghost);
        }
    }

    private void rebuildAllMarkers() {
        clearMarkers();
        for (GeoPoint p : currentPoints) {
            addVertexMarker(p);
        }
    }

    private void addVertexMarker(GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(mapView.getContext().getDrawable(com.drc.aidbridge.R.drawable.ic_vertex_dot));
        marker.setDraggable(true);
        marker.setInfoWindow(null);
        
        marker.setOnMarkerClickListener((m, mv) -> {
            int index = vertexMarkers.indexOf(m);
            if (index != -1 && currentPoints.size() > 0) {
                currentPoints.remove(index);
                rebuildAllMarkers();
                updatePolygon();
            }
            return true;
        });

        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
                int index = vertexMarkers.indexOf(marker);
                if (index != -1) {
                    currentPoints.set(index, marker.getPosition());
                    updatePolygon();
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                int index = vertexMarkers.indexOf(marker);
                if (index != -1) {
                    currentPoints.set(index, marker.getPosition());
                    rebuildAllMarkers();
                    updatePolygon();
                }
            }

            @Override
            public void onMarkerDragStart(Marker marker) {}
        });
        
        vertexMarkers.add(marker);
        mapView.getOverlayManager().add(marker);
        mapView.invalidate();
    }

    private boolean isPointInPolygon(GeoPoint point, List<GeoPoint> polygon) {
        if (polygon == null || polygon.size() < 3) return false;
        int i, j;
        boolean result = false;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            if ((polygon.get(i).getLatitude() > point.getLatitude()) != (polygon.get(j).getLatitude() > point.getLatitude()) &&
                    (point.getLongitude() < (polygon.get(j).getLongitude() - polygon.get(i).getLongitude()) * (point.getLatitude() - polygon.get(i).getLatitude()) / (polygon.get(j).getLatitude() - polygon.get(i).getLatitude()) + polygon.get(i).getLongitude())) {
                result = !result;
            }
        }
        return result;
    }

    public void addPermanentPolygon(Polygon polygon) {
        if (!permanentPolygons.contains(polygon)) {
            permanentPolygons.add(polygon);
        }
        if (isZonesVisible) {
            mapView.getOverlayManager().add(polygon);
        }
        updateZoneListUI();
        mapView.invalidate();
    }

    public void clearAllPermanentPolygons() {
        for (Polygon poly : permanentPolygons) {
            mapView.getOverlayManager().remove(poly);
        }
        permanentPolygons.clear();
        updateZoneListUI();
        mapView.invalidate();
    }

    public void toggleZonesVisibility() {
        isZonesVisible = !isZonesVisible;
        for (Polygon poly : permanentPolygons) {
            if (isZonesVisible) {
                if (!mapView.getOverlayManager().contains(poly)) {
                    mapView.getOverlayManager().add(poly);
                }
            } else {
                mapView.getOverlayManager().remove(poly);
            }
        }
        adminBinding.btnClearAllZones.setText(isZonesVisible ? "Ẩn tất cả vùng" : "Hiện tất cả vùng");
        mapView.invalidate();
    }

    private void updateZoneListUI() {
        adminBinding.layoutDangerousZonesList.removeAllViews();
        adminBinding.btnClearAllZones.setVisibility(permanentPolygons.isEmpty() ? View.GONE : View.VISIBLE);
        
        for (int i = 0; i < permanentPolygons.size(); i++) {
            Polygon poly = permanentPolygons.get(i);
            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(mapView.getContext());
            btn.setText(poly.getTitle() != null && !poly.getTitle().isEmpty() ? poly.getTitle() : "Vùng " + (i + 1));
            btn.setAllCaps(false);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.parseColor("#FF5722"));
            btn.setCornerRadius(24);
            btn.setPadding(32, 0, 32, 0);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);
            btn.setLayoutParams(params);
            
            btn.setOnClickListener(v -> {
                mapView.getController().animateTo(poly.getBounds().getCenterWithDateLine());
                UUID id = null;
                try {
                    id = UUID.fromString(poly.getId());
                } catch (Exception ignored) {}
                enterEditMode(id, poly.getTitle(), poly.getActualPoints(), poly);
                permanentPolygons.remove(poly);
                updateZoneListUI();
            });
            
            adminBinding.layoutDangerousZonesList.addView(btn);
        }
    }

    private void clearMarkers() {
        for (Marker m : vertexMarkers) {
            mapView.getOverlayManager().remove(m);
        }
        vertexMarkers.clear();
        for (Marker m : ghostMarkers) {
            mapView.getOverlayManager().remove(m);
        }
        ghostMarkers.clear();
        mapView.invalidate();
    }

    private void setBaseMapUIVisibility(int visibility) {
        if (baseBinding == null) return;
        
        if (baseBinding.fabFindHubs != null) baseBinding.fabFindHubs.setVisibility(visibility);
        if (baseBinding.fabRecenterCurrentLocation != null) baseBinding.fabRecenterCurrentLocation.setVisibility(visibility);
        if (baseBinding.btnSetStartPoint != null) baseBinding.btnSetStartPoint.setVisibility(visibility);
        if (baseBinding.btnSetEndPoint != null) baseBinding.btnSetEndPoint.setVisibility(visibility);
        if (baseBinding.fabOpenControlPanel != null) baseBinding.fabOpenControlPanel.setVisibility(visibility);
        if (baseBinding.cardTopOverview != null) baseBinding.cardTopOverview.setVisibility(visibility);
    }
}
